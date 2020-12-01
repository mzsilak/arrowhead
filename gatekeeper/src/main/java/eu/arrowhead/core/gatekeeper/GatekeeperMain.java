/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.gatekeeper;

import eu.arrowhead.common.ArrowheadMain;
import eu.arrowhead.common.DatabaseManager;
import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ArrowheadCloud;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.Broker;
import eu.arrowhead.common.database.OwnCloud;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.misc.CoreSystemService;
import eu.arrowhead.common.misc.GetCoreSystemServicesTask;
import eu.arrowhead.common.misc.NeedsCoreSystemService;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.TypeSafeProperties;
import eu.arrowhead.common.web.ArrowheadCloudApi;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;
import java.util.Timer;
import java.util.TimerTask;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLContextConfigurator.GenericStoreException;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class GatekeeperMain implements NeedsCoreSystemService {

  public static TimerTask getServicesTask;

  static boolean IS_SECURE;
  static boolean USE_GATEWAY;
  static String SERVICE_REGISTRY_URI;
  static SSLContext outboundClientContext;
  static SSLContext outboundServerContext;
  static final int TIMEOUT;
  static final String GATEKEEPER_SERVICE_URI = "gatekeeper";

  private static String INBOUND_BASE_URI;
  private static String OUTBOUND_BASE_URI;
  private static String BASE64_PUBLIC_KEY;
  private static HttpServer inboundServer;
  private static HttpServer outboundServer;
  private static String ORCHESTRATOR_URI;
  private static String AUTH_CONTROL_URI;
  private static String[] GATEWAY_CONSUMER_URI;
  private static String[] GATEWAY_PROVIDER_URI;

  private static final TypeSafeProperties props;
  private static final Logger log = LogManager.getLogger(GatekeeperMain.class.getName());

  private static final String GET_CORE_SYSTEM_URLS_ERROR_MESSAGE = "The Gatekeeper core system has not acquireq the addresses of the "
      + "Authorization, Orchestrator and Gateway core systems yet from the Service Registry. Wait 15 seconds and retry your request";

  static {
    props = Utility.getProp();
    DatabaseManager.init();
    USE_GATEWAY = props.getBooleanProperty("use_gateway", false);
    TIMEOUT = props.getIntProperty("timeout", 30000);
  }

  private GatekeeperMain() {
    List<String> serviceDefs = new ArrayList<>(Arrays.asList(CoreSystemService.AUTH_CONTROL_SERVICE.getServiceDef(),
                                                             CoreSystemService.GW_CONSUMER_SERVICE.getServiceDef(),
                                                             CoreSystemService.GW_PROVIDER_SERVICE.getServiceDef(),
                                                             CoreSystemService.ORCH_SERVICE.getServiceDef()));
    getServicesTask = new GetCoreSystemServicesTask(this, serviceDefs);
    Timer timer = new Timer();
    timer.schedule(getServicesTask, 15L * 1000L, 60L * 60L * 1000L); //15 sec delay, 1 hour period
  }

  public static void main(String[] args) throws IOException {
    System.out.println("Working directory: " + System.getProperty("user.dir"));
    DatabaseManager.init();

    String internalAddress = props.getProperty("internal_address", "0.0.0.0");
    String externalAddress = props.getProperty("external_address", "0.0.0.0");
    int internalInsecurePort = props.getIntProperty("internal_insecure_port", 8446);
    int internalSecurePort = props.getIntProperty("internal_secure_port", 8447);
    int externalInsecurePort = props.getIntProperty("external_insecure_port", 8448);
    int externalSecurePort = props.getIntProperty("external_secure_port", 8449);

    String srAddress = props.getProperty("sr_address", "0.0.0.0");
    int srInsecurePort = props.getIntProperty("sr_insecure_port", 8442);
    int srSecurePort = props.getIntProperty("sr_secure_port", 8443);

    boolean daemon = false;
    List<String> alwaysMandatoryProperties = Arrays.asList("db_user", "db_password", "db_address");
    for (String arg : args) {
      switch (arg) {
        case "-daemon":
          daemon = true;
          System.out.println("Starting server as daemon!");
          break;
        case "-d":
          System.setProperty("debug_mode", "true");
          System.out.println("Starting server in debug mode!");
          break;
        case "-tls":
          IS_SECURE = true;
          break;
      }
    }

    final DatabaseManager dm = DatabaseManager.getInstance();
    try {
      Utility.getOwnCloud(IS_SECURE);
    } catch (DataNotFoundException e) {
      System.out.println("Own cloud not found, creating it...");
      String gatekeeperKeystorePath = props.getProperty("gatekeeper_keystore");
      String gatekeeperKeystorePass = props.getProperty("gatekeeper_keystore_pass");
      final String[] serverCN = getServerCN(gatekeeperKeystorePath, gatekeeperKeystorePass, false).split("\\.");
      final ArrowheadCloud cloud = new ArrowheadCloud(serverCN[2], serverCN[1] + (IS_SECURE ? "" : "-insecure"), externalAddress,
                                                      IS_SECURE ? externalSecurePort : externalInsecurePort, GATEKEEPER_SERVICE_URI,
                                                      IS_SECURE ? getAuthBase64(gatekeeperKeystorePath, gatekeeperKeystorePass) : null, IS_SECURE);
      final OwnCloud ownCloud = new OwnCloud(cloud);
      dm.save(cloud, ownCloud);
    }

    if (dm.getAll(Broker.class, null).isEmpty() &&
        props.getBooleanProperty("public_brokers", false)) {
      dm.save(
          new Broker("arrowhead-relay.tmit.bme.hu", 5672, false),
          new Broker("arrowhead-relay.tmit.bme.hu", 5671, true),
          new Broker("arrowhead-relay2.tmit.bme.hu", 5672, false),
          new Broker("arrowhead-relay2.tmit.bme.hu", 5671, true));
    }

    if (IS_SECURE) {
      List<String> allMandatoryProperties = new ArrayList<>(alwaysMandatoryProperties);
      allMandatoryProperties.addAll(Arrays.asList("gatekeeper_keystore", "gatekeeper_keystore_pass", "gatekeeper_keypass", "cloud_keystore",
                                                  "cloud_keystore_pass", "cloud_keypass", "master_arrowhead_cert"));
      Utility.checkProperties(props.stringPropertyNames(), allMandatoryProperties);
      INBOUND_BASE_URI = Utility.getUri(internalAddress, internalSecurePort, null, IS_SECURE, true);
      OUTBOUND_BASE_URI = Utility.getUri(externalAddress, externalSecurePort, null, IS_SECURE, true);
      SERVICE_REGISTRY_URI = Utility.getUri(srAddress, srSecurePort, "serviceregistry", IS_SECURE, true);
      inboundServer = startSecureServer(INBOUND_BASE_URI, true);
      outboundServer = startSecureServer(OUTBOUND_BASE_URI, false);
      useSRService(true);
    } else {
      Utility.checkProperties(props.stringPropertyNames(), alwaysMandatoryProperties);
      INBOUND_BASE_URI = Utility.getUri(internalAddress, internalInsecurePort, null, IS_SECURE, true);
      OUTBOUND_BASE_URI = Utility.getUri(externalAddress, externalInsecurePort, null, IS_SECURE, true);
      SERVICE_REGISTRY_URI = Utility.getUri(srAddress, srInsecurePort, "serviceregistry", IS_SECURE, true);
      inboundServer = startServer(INBOUND_BASE_URI, true);
      outboundServer = startServer(OUTBOUND_BASE_URI, false);
      useSRService(true);
    }
    Utility.setServiceRegistryUri(SERVICE_REGISTRY_URI);
    new GatekeeperMain();

    if (daemon) {
      System.out.println("In daemon mode, process will terminate for TERM signal...");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Received TERM signal, shutting down...");
        shutdown();
      }));
    } else {
      System.out.println("Type \"stop\" to shutdown Gatekeeper Server...");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input = "";
      while (!input.equals("stop")) {
        input = br.readLine();
      }
      br.close();
      shutdown();
    }
  }

  private static String getAuthBase64(String keystorePath, String keystorePass) {
    KeyStore keyStore = SecurityUtils.loadKeyStore(keystorePath, keystorePass);
    X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
    return Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
  }

  private static HttpServer startServer(final String url, final boolean inbound) {
    final ResourceConfig config = new ResourceConfig();
    if (inbound) {
      config.registerClasses(GatekeeperInboundResource.class, ArrowheadCloudApi.class);
    } else {
      config.registerClasses(GatekeeperApi.class, GatekeeperOutboundResource.class, ArrowheadCloudApi.class);
    }
    config.packages("eu.arrowhead.common.exception", "eu.arrowhead.common.json", "eu.arrowhead.common.filter", "eu.arrowhead.core.gatekeeper.filter");
    config.packages("io.swagger.v3.jaxrs2.integration.resources");
    if (Boolean.valueOf(System.getProperty("debug_mode", "false")))
      config.register(new LoggingFeature(
          org.apache.logging.log4j.jul.LogManager.getLogManager().getLogger(GatekeeperMain.class.getName()),
          LoggingFeature.Verbosity.PAYLOAD_ANY
      ));

    URI uri = UriBuilder.fromUri(url).build();
    try {
      final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, config, false);
      configureServer(server);
      server.start();
      if (inbound) {
        log.info("Started inbound server at: " + url);
        System.out.println("Started insecure inbound server at: " + url);
      } else {
        log.info("Started outbound server at: " + url);
        System.out.println("Started insecure outbound server at: " + url);
      }
      return server;
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError("Make sure you gave a valid address in the config file! (Assignable to this JVM and not in use already)",
                                          e);
    }
  }

  private static HttpServer startSecureServer(final String url, final boolean inbound) {
    final ResourceConfig config = new ResourceConfig();
    if (inbound) {
      config.registerClasses(GatekeeperInboundResource.class, ArrowheadCloudApi.class);
    } else {
      config.registerClasses(GatekeeperApi.class, GatekeeperOutboundResource.class, ArrowheadCloudApi.class);
    }
    config.packages("eu.arrowhead.common.exception", "eu.arrowhead.common.json", "eu.arrowhead.common.filter", "eu.arrowhead.core.gatekeeper.filter");
    config.packages("io.swagger.v3.jaxrs2.integration.resources");
    if (Boolean.valueOf(System.getProperty("debug_mode", "false")))
      config.register(new LoggingFeature(
          org.apache.logging.log4j.jul.LogManager.getLogManager().getLogger(GatekeeperMain.class.getName()),
          LoggingFeature.Verbosity.PAYLOAD_ANY
      ));

    String gatekeeperKeystorePath = props.getProperty("gatekeeper_keystore");
    String gatekeeperKeystorePass = props.getProperty("gatekeeper_keystore_pass");
    String gatekeeperKeyPass = props.getProperty("gatekeeper_keypass");
    String cloudKeystorePath = props.getProperty("cloud_keystore");
    String cloudKeystorePass = props.getProperty("cloud_keystore_pass");
    String cloudKeyPass = props.getProperty("cloud_keypass");
    String masterArrowheadCertPath = props.getProperty("master_arrowhead_cert");

    SSLContext serverContext;
    if (inbound) {
      serverContext = SecurityUtils.createMasterSSLContext(cloudKeystorePath, cloudKeystorePass, cloudKeyPass, masterArrowheadCertPath);
      config.property("server_common_name", getServerCN(cloudKeystorePath, cloudKeystorePass, true));

      SSLContextConfigurator clientConfig = new SSLContextConfigurator();
      clientConfig.setKeyStoreFile(gatekeeperKeystorePath);
      clientConfig.setKeyStorePass(gatekeeperKeystorePass);
      clientConfig.setKeyPass(gatekeeperKeyPass);
      clientConfig.setTrustStoreFile(cloudKeystorePath);
      clientConfig.setTrustStorePass(cloudKeystorePass);
      SSLContext clientContext;
      try {
        clientContext = clientConfig.createSSLContext(true);
      } catch (GenericStoreException e) {
        log.fatal("Internal client SSL Context is not valid, check the certificate or the config files!");
        throw new AuthException("Internal client SSL Context is not valid, check the certificate or the config files!", e);
      }
      Utility.setSSLContext(clientContext);
    } else {
      SSLContextConfigurator serverConfig = new SSLContextConfigurator();
      serverConfig.setKeyStoreFile(gatekeeperKeystorePath);
      serverConfig.setKeyStorePass(gatekeeperKeystorePass);
      serverConfig.setKeyPass(gatekeeperKeyPass);
      serverConfig.setTrustStoreFile(cloudKeystorePath);
      serverConfig.setTrustStorePass(cloudKeystorePass);
      try {
        serverContext = serverConfig.createSSLContext(true);
      } catch (GenericStoreException e) {
        log.fatal("External server SSL Context is not valid, check the certificate or the config files!");
        throw new AuthException("External server SSL Context is not valid, check the certificate or the config files!", e);
      }
      outboundServerContext = serverContext;
      config.property("server_common_name", getServerCN(gatekeeperKeystorePath, gatekeeperKeystorePass, false));

      outboundClientContext = SecurityUtils.createMasterSSLContext(cloudKeystorePath, cloudKeystorePass, cloudKeyPass, masterArrowheadCertPath);
    }

    URI uri = UriBuilder.fromUri(url).build();
    try {
      final HttpServer server = GrizzlyHttpServerFactory
          .createHttpServer(uri, config, true, new SSLEngineConfigurator(serverContext).setClientMode(false).setNeedClientAuth(true), false);
      configureServer(server);
      server.start();
      if (inbound) {
        log.info("Started inbound server at: " + url);
        System.out.println("Started secure inbound server at: " + url);
      } else {
        log.info("Started outbound server at: " + url);
        System.out.println("Started secure outbound server at: " + url);
      }
      return server;
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError("Make sure you gave a valid address in the config file! (Assignable to this JVM and not in use already)",
                                          e);
    }
  }

  private static void configureServer(HttpServer server) {
    //Add swagger UI to the server
    final HttpHandler httpHandler = new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "/swagger/");
    server.getServerConfiguration().addHttpHandler(httpHandler, "/api");
    //Allow message payload for GET and DELETE requests - ONLY to provide custom error message for them
    server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
  }

  private static String getHostAddress(final URI uri)
  {
    String address = uri.getHost();
    if("0.0.0.0".equals(address))
    {
      try {
        address = Utility.getIpAddress();
      } catch (SocketException e) {
        // noop
      }
    }
    return address;
  }

  private static void useSRService(boolean registering) {
    final URI uri = UriBuilder.fromUri(OUTBOUND_BASE_URI).build();
    final boolean isSecure = uri.getScheme().equals("https");
    final String interfaceName = isSecure ? "HTTP-SECURE-JSON" : "HTTP-INSECURE-JSON";
    final ArrowheadSystem gkSystem = new ArrowheadSystem("gatekeeper", getHostAddress(uri), uri.getPort(),
                                                         BASE64_PUBLIC_KEY);
    ArrowheadService gsdService = new ArrowheadService(Utility.createSD(CoreSystemService.GSD_SERVICE.getServiceDef(), isSecure),
                                                       Collections.singleton(interfaceName), null);
    ArrowheadService icnService = new ArrowheadService(Utility.createSD(CoreSystemService.ICN_SERVICE.getServiceDef(), isSecure),
                                                       Collections.singleton(interfaceName), null);
    if (isSecure) {
      gsdService.setServiceMetadata(ArrowheadMain.secureServerMetadata);
      icnService.setServiceMetadata(ArrowheadMain.secureServerMetadata);
    }

    //Preparing the payload
    ServiceRegistryEntry gsdEntry = new ServiceRegistryEntry(gsdService, gkSystem, "gatekeeper/init_gsd");
    ServiceRegistryEntry icnEntry = new ServiceRegistryEntry(icnService, gkSystem, "gatekeeper/init_icn");

    if (registering) {
      try {
        Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("register").build().toString(), "POST", gsdEntry);
      } catch (ArrowheadException e) {
        if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
          Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("remove").build().toString(), "PUT", gsdEntry);
          Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("register").build().toString(), "POST", gsdEntry);
        } else {
          throw new ArrowheadException("GSD service registration failed.", e);
        }
      }
      try {
        Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("register").build().toString(), "POST", icnEntry);
      } catch (ArrowheadException e) {
        if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
          Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("remove").build().toString(), "PUT", icnEntry);
          Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("register").build().toString(), "POST", icnEntry);
        } else {
          throw new ArrowheadException("ICN service registration failed.", e);
        }
      }
    } else {
      Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("remove").build().toString(), "PUT", gsdEntry);
      Utility.sendRequest(UriBuilder.fromUri(SERVICE_REGISTRY_URI).path("remove").build().toString(), "PUT", icnEntry);
      System.out.println("Gatekeeper services deregistered.");
    }
  }

  @Override
  public void getCoreSystemServiceURIs(Map<String, String[]> uriMap) {
    for (Entry<String, String[]> entry : uriMap.entrySet()) {
      switch (entry.getKey()) {
        case "AuthorizationControl":
          AUTH_CONTROL_URI = entry.getValue()[0];
          break;
        case "ConnectToConsumer":
          GATEWAY_CONSUMER_URI = entry.getValue();
          break;
        case "ConnectToProvider":
          GATEWAY_PROVIDER_URI = entry.getValue();
          break;
        case "OrchestrationService":
          ORCHESTRATOR_URI = entry.getValue()[0];
          break;
        default:
          break;
      }
    }
    System.out.println("Core system URLs acquired/updated.");
  }

  private static String getServerCN(String certPath, String certPass, boolean inbound) {
    if (certPath == null || certPass == null) {
      throw new ArrowheadException("Server certificate path or password is missing, can not acquire server common name!");
    }

    KeyStore keyStore = SecurityUtils.loadKeyStore(certPath, certPass);
    X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
    BASE64_PUBLIC_KEY = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
    String serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
    if (inbound && !SecurityUtils.isTrustStoreCNArrowheadValid(serverCN)) {
      log.fatal("Server CN is not compliant with the Arrowhead cert structure.");
      throw new AuthException(
          "Server CN ( " + serverCN + ") is not compliant with the Arrowhead cert structure, since it does not have 4 parts, or does not "
              + "end with arrowhead.eu.");
    } else if (!inbound && !SecurityUtils.isKeyStoreCNArrowheadValid(serverCN)) {
      log.fatal("Server CN is not compliant with the Arrowhead cert structure");
      throw new AuthException(
          "Server CN ( " + serverCN + ") is not compliant with the Arrowhead cert structure, since it does not have 5 parts, or does not "
              + "end with arrowhead.eu.");
    }

    log.info("Certificate of the secure server: " + serverCN);
    return serverCN;
  }

  private static void shutdown() {
    if (inboundServer != null) {
      log.info("Stopping server at: " + INBOUND_BASE_URI);
      inboundServer.shutdownNow();
    }
    if (outboundServer != null) {
      log.info("Stopping server at: " + OUTBOUND_BASE_URI);
      outboundServer.shutdown();
      useSRService(false);
    }
    DatabaseManager.closeSessionFactory();
    System.out.println("Gatekeeper Server stopped");
    System.exit(0);
  }

  static String getOrchestratorUri() {
    if (ORCHESTRATOR_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return ORCHESTRATOR_URI;
  }

  static String getAuthControlUri() {
    if (AUTH_CONTROL_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return AUTH_CONTROL_URI;
  }

  static String[] getGatewayConsumerUri() {
    if (GATEWAY_CONSUMER_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return GATEWAY_CONSUMER_URI;
  }

  static String[] getGatewayProviderUri() {
    if (GATEWAY_PROVIDER_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return GATEWAY_PROVIDER_URI;
  }
}

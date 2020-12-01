/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common;

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
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLContextConfigurator.GenericStoreException;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.misc.CoreSystem;
import eu.arrowhead.common.misc.CoreSystemService;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.TypeSafeProperties;
import eu.arrowhead.common.opcua.ArrowheadOpcUaServer;
//import eu.arrowhead.common.opcua.ArrowheadOpcUaServer;

public abstract class ArrowheadMain {

  public static final List<String> dbFields = Collections
      .unmodifiableList(Arrays.asList("db_user", "db_password", "db_address"));
  public static final List<String> certFields = Collections
      .unmodifiableList(Arrays.asList("keystore", "keystorepass", "keypass", "truststore", "truststorepass"));
  public static final Map<String, String> secureServerMetadata = Collections.singletonMap("security", "certificate");
  public static final String SWAGGER_PATH = "/api";
  public static final String OPENAPI_PATH = "/openapi.json";

  protected String srBaseUri;
  protected boolean isSecure = false;
  protected String baseUri;
  protected final TypeSafeProperties props = Utility.getProp();

  private boolean requestClientCertificate = true;
  private boolean daemon = false;
  private CoreSystem coreSystem;
  private HttpServer server;
  protected ArrowheadOpcUaServer arrowheaduaserver;
  private String base64PublicKey;
  private int registeringTries = 1;

  private static final Logger log = LogManager.getLogger(ArrowheadMain.class.getName());

  {
    DatabaseManager.init();
  }

  protected void init(CoreSystem coreSystem, String[] args, Set<Class<?>> classes, String[] packages) {
    System.out.println("Working directory: " + System.getProperty("user.dir"));
    log.info("Working directory: " + System.getProperty("user.dir"));
    packages = addSwaggerToPackages(packages);
    this.coreSystem = coreSystem;

    //Read in command line arguments
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
          System.setProperty("is_secure", "true");
          isSecure = true;
          Utility.setSecure(true);
          break;
      }
    }

		// Get the URL where the web-server will bind to
		String address = props.getProperty("address", "0.0.0.0");
		int port = isSecure ? props.getIntProperty("secure_port", coreSystem.getSecurePort())
				: props.getIntProperty("insecure_port", coreSystem.getInsecurePort());
		baseUri = Utility.getUri(address, port, null, isSecure, true);

		// Start the web-server
		if (isSecure) {
			List<String> allMandatoryProperties = new ArrayList<>(coreSystem.getAlwaysMandatoryFields());
			allMandatoryProperties.addAll(coreSystem.getSecureMandatoryFields());
			Utility.checkProperties(props.stringPropertyNames(), allMandatoryProperties);
			startSecureServer(classes, packages);
		} else {
			Utility.checkProperties(props.stringPropertyNames(), coreSystem.getAlwaysMandatoryFields());
			startServer(classes, packages);
		}

    //Register the core system services to the Service Registry
    if (!coreSystem.equals(CoreSystem.SERVICE_REGISTRY_DNS) && !coreSystem.equals(CoreSystem.SERVICE_REGISTRY_SQL)) {
      String srAddress = props.getProperty("sr_address", "0.0.0.0");
      int srPort = isSecure ? props.getIntProperty("sr_secure_port", CoreSystem.SERVICE_REGISTRY_SQL.getSecurePort())
                            : props
                       .getIntProperty("sr_insecure_port", CoreSystem.SERVICE_REGISTRY_SQL.getInsecurePort());
      srBaseUri = Utility.getUri(srAddress, srPort, "serviceregistry", isSecure, true);
      Utility.setServiceRegistryUri(srBaseUri);
      useSRService(true);
    }
    else
    {
      srBaseUri = Utility.getUri(address, port, "serviceregistry", isSecure, false);
      Utility.setServiceRegistryUri(srBaseUri);
      useSRService(true);
    }
  }

	protected void listenForInput() {
		log.info(coreSystem + " startup completed.");
		if (daemon) {
			System.out.println("In daemon mode, process will terminate for TERM signal...");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Received TERM signal, shutting down...");
				shutdown();
			}));
		} else {
			System.out.println("Type \"stop\" to shutdown " + coreSystem.name() + " Server...");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input = "";
			try {
				while (!input.equals("stop")) {
					input = br.readLine();
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			shutdown();
		}
	}

  private void startServer(Set<Class<?>> classes, String[] packages) {
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(classes);
    config.packages(packages);
    if (Boolean.valueOf(System.getProperty("debug_mode", "false")))
      config.register(new LoggingFeature(
          org.apache.logging.log4j.jul.LogManager.getLogManager().getLogger(this.getClass().getName()),
          LoggingFeature.Verbosity.PAYLOAD_ANY
      ));

		URI uri = UriBuilder.fromUri(baseUri).build();
		try {
			server = GrizzlyHttpServerFactory.createHttpServer(uri, config, false);
			configureServer(server);
			server.start();
			log.info("Started server at: " + baseUri);
			System.out.println("Started insecure server at: " + baseUri);
		} catch (IOException | ProcessingException e) {
			throw new ServiceConfigurationError(
					"Make sure you gave a valid address in the config file! (Assignable to this JVM and not in use already)",
					e);
		}
	}

  protected void startSecureServer(Set<Class<?>> classes, String[] packages) {
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(classes);
    config.packages(packages);
    if (Boolean.valueOf(System.getProperty("debug_mode", "false")))
      config.register(new LoggingFeature(
          org.apache.logging.log4j.jul.LogManager.getLogManager().getLogger(this.getClass().getName()),
          LoggingFeature.Verbosity.PAYLOAD_ANY
      ));

		String keystorePath = props.getProperty("keystore");
		String keystorePass = props.getProperty("keystorepass");
		String keyPass = props.getProperty("keypass");
		String truststorePath = props.getProperty("truststore");
		String truststorePass = props.getProperty("truststorepass");

		SSLContextConfigurator sslCon = new SSLContextConfigurator();
		sslCon.setKeyStoreFile(keystorePath);
		sslCon.setKeyStorePass(keystorePass);
		sslCon.setKeyPass(keyPass);
		sslCon.setTrustStoreFile(truststorePath);
		sslCon.setTrustStorePass(truststorePass);
		SSLContext sslContext;
		try {
			sslContext = sslCon.createSSLContext(true);
		} catch (GenericStoreException e) {
			log.fatal("SSL Context is not valid, check the certificate or the config files!");
			throw new AuthException("SSL Context is not valid, check the certificate or the config files!", e);
		}
		Utility.setSSLContext(sslContext);

		KeyStore keyStore = SecurityUtils.loadKeyStore(keystorePath, keystorePass);
		X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
		base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
		System.out.println("Server PublicKey Base64: " + base64PublicKey);
		String serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
		if (!SecurityUtils.isKeyStoreCNArrowheadValid(serverCN)) {
			log.fatal("Server CN is not compliant with the Arrowhead cert structure");
			throw new AuthException("Server CN ( " + serverCN
					+ ") is not compliant with the Arrowhead cert structure, since it does not have 5 "
					+ "parts, or does not end with" + " \"arrowhead.eu\"");
		}
		log.info("Certificate of the secure server: " + serverCN);
		config.property("server_common_name", serverCN);

    URI uri = UriBuilder.fromUri(baseUri).build();
    try {
      // instead of "need" we only "want" a client certificate. we will verify the certificate in the filter
      server = GrizzlyHttpServerFactory.createHttpServer(uri, config, true,
                                                         new SSLEngineConfigurator(sslCon).setClientMode(false)
                                                                                          .setWantClientAuth(requestClientCertificate),
                                                         false);
      configureServer(server);
      server.start();
      log.info("Started server at: " + baseUri);
      System.out.println("Started secure server at: " + baseUri);
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError(
          "Make sure you gave a valid address in the config file! (Assignable to this JVM and not in use already)", e);
    }
  }

  private void configureServer(HttpServer server) {
    //Add swagger UI to the server
    final HttpHandler httpHandler = new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "/swagger/");
    server.getServerConfiguration().addHttpHandler(httpHandler, SWAGGER_PATH);
    //Allow message payload for GET and DELETE requests - ONLY to provide custom error message for them
    server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
  }

	private void shutdown() {
		useSRService(false);
		DatabaseManager.closeSessionFactory();
		if (server != null) {
			log.info("Stopping server at: " + baseUri);
			server.shutdownNow();
		}
		System.out.println(coreSystem + " Server stopped");
		System.exit(0);
	}
	
	protected void startUaServer (String endpoint) {
		try {
			int port = props.getIntProperty("opcua_port", coreSystem.getOpcUaPort());
			arrowheaduaserver = ArrowheadOpcUaServer.getInstance(port, endpoint);
			arrowheaduaserver.startup().get();
			final CompletableFuture<Void> future = new CompletableFuture<>();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));
			// future.get();
			System.out.println("Starting an opc ua server at " + endpoint + ":" + port);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
  private void useSRService(boolean registering) {
    //Preparing the payload
    final URI uri = UriBuilder.fromUri(baseUri).build();
    final boolean isSecure = uri.getScheme().equals("https");
    final String interfaceName = isSecure ? "HTTP-SECURE-JSON" : "HTTP-INSECURE-JSON";

    String hostAddress = uri.getHost();
    if("0.0.0.0".equals(uri.getHost()))
    {
      try {
        hostAddress = Utility.getIpAddress();
      } catch (SocketException e) {
        hostAddress = "127.0.0.1";
      }
    }

    final ArrowheadSystem provider = new ArrowheadSystem(coreSystem.name(), hostAddress, uri.getPort(),
                                                         base64PublicKey);
		for (CoreSystemService service : coreSystem.getServices()) {
			ArrowheadService providedService = new ArrowheadService(Utility.createSD(service.getServiceDef(), isSecure),
					Collections.singleton(interfaceName), null);
			if (isSecure) {
				providedService.setServiceMetadata(ArrowheadMain.secureServerMetadata);
			}
			ServiceRegistryEntry srEntry = new ServiceRegistryEntry(providedService, provider, service.getServiceURI());

      if (registering) {
        try {
          Utility.sendRequest(UriBuilder.fromUri(srBaseUri).path("register").build().toString(), "POST", srEntry);
        } catch (ArrowheadException e) {
          if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
            Utility.sendRequest(UriBuilder.fromUri(srBaseUri).path("remove").build().toString(), "PUT", srEntry);
            Utility.sendRequest(UriBuilder.fromUri(srBaseUri).path("register").build().toString(), "POST", srEntry);
          } else if (e.getExceptionType() == ExceptionType.UNAVAILABLE) {
            System.out.println("Service Registry is unavailable at the moment, retrying in 15 seconds...");
            try {
              Thread.sleep(15000);
              if (registeringTries == 3) {
                throw e;
              } else {
                registeringTries++;
                //noinspection ConstantConditions
                useSRService(registering);
              }
            } catch (InterruptedException e1) {
              e1.printStackTrace();
            }
          } else {
            throw new ArrowheadException(service.getServiceDef() + " service registration failed.", e);
          }
        }
        registeringTries = 1;
      } else {
        Utility.sendRequest(UriBuilder.fromUri(srBaseUri).path("remove").build().toString(), "PUT", srEntry);
      }
    }

  }
  
  protected void setRequestClientCertificate(final boolean requestClientCertificate)
    {
      this.requestClientCertificate = requestClientCertificate;
    }
  
  private String[] addSwaggerToPackages(String[] packages) {
	     packages = Arrays.copyOf(packages, packages.length + 1);
	     packages[packages.length - 1] = "io.swagger.v3.jaxrs2.integration.resources";
	     return packages;
	   }


	public ArrowheadOpcUaServer getArrowheadOpcUaServer() {
		return arrowheaduaserver;
	}
}

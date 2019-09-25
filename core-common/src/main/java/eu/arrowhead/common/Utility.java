/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.arrowhead.common.database.ArrowheadCloud;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.NeighborCloud;
import eu.arrowhead.common.database.OwnCloud;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.DnsException;
import eu.arrowhead.common.exception.DuplicateEntryException;
import eu.arrowhead.common.exception.ErrorMessage;
import eu.arrowhead.common.exception.UnavailableServerException;
import eu.arrowhead.common.json.JacksonJsonProviderAtRest;
import eu.arrowhead.common.messages.ServiceQueryForm;
import eu.arrowhead.common.messages.ServiceQueryResult;
import eu.arrowhead.common.misc.CoreSystemService;
import eu.arrowhead.common.misc.TypeSafeProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public final class Utility {

  private static Client client = createClient(null);
  private static Client sslClient;
  private static SSLContext sslContext;
  private static String SR_QUERY_URI;

  private static final ObjectMapper mapper = JacksonJsonProviderAtRest.getMapper();
  private static final Logger log = Logger.getLogger(Utility.class.getName());
  private static final HostnameVerifier allHostsValid = (hostname, session) -> {
    // Decide whether to allow the connection...
    return true;
  };

  private static final String DEFAULT_CONF = "default.conf";
  private static final String DEFAULT_CONF_DIR = "config" + File.separator + "default.conf";
  private static final String APP_CONF = "app.conf";
  private static final String APP_CONF_DIR = "config" + File.separator + "app.conf";

  private Utility() throws AssertionError {
    throw new AssertionError("Arrowhead Common:Utility is a non-instantiable class");
  }

  private static Client createClient(SSLContext context) {
    ClientConfig configuration = new ClientConfig();
    configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
    configuration.property(ClientProperties.READ_TIMEOUT, 30000);

    Client client;
    if (context != null) {
      client = ClientBuilder.newBuilder().sslContext(context).withConfig(configuration).hostnameVerifier(allHostsValid)
                            .build();
    } else {
      client = ClientBuilder.newClient(configuration);
    }
    client.register(JacksonJsonProviderAtRest.class);
    return client;
  }

  public static void setSSLContext(SSLContext context) {
    sslContext = context;
    sslClient = createClient(sslContext);
  }

  public static void setServiceRegistryUri(String uri) {
    if (uri == null) {
      throw new AssertionError("Arrowhead Common:Utility has no Service Registry URL.");
    }
    SR_QUERY_URI = UriBuilder.fromUri(uri).path("query").build().toString();
  }

  public static <T> Response sendRequest(String uri, String method, T payload, SSLContext givenContext) {
    log.info("Sending " + method + " request to: " + uri);

    boolean isSecure = false;
    if (uri == null) {
      log.error("sendRequest received null uri");
      throw new NullPointerException(
          "send (HTTP) request method received null URL. This most likely means the invoking Core System could not "
              + "fetch the service"
              + " of another Core System from the Service Registry!");
    }
    if (uri.startsWith("https")) {
      isSecure = true;
    }

    if (isSecure && sslClient == null) {
      throw new AuthException(
          "SSL Context is not set, but secure request sending was invoked. An insecure module can not send requests "
              + "to secure modules.",
          Status.UNAUTHORIZED.getStatusCode());
    }
    Client usedClient = isSecure ? givenContext != null ? createClient(givenContext) : sslClient : client;

    Builder request = usedClient.target(UriBuilder.fromUri(uri).build()).request()
                                .header("Content-type", "application/json");
    Response response; // will not be null after the switch-case
    try {
      switch (method) {
        case "GET":
          response = request.get();
          break;
        case "POST":
          response = request.post(Entity.json(payload));
          break;
        case "PUT":
          response = request.put(Entity.json(payload));
          break;
        case "DELETE":
          response = request.delete();
          break;
        default:
          throw new NotAllowedException("Invalid method type was given to the Utility.sendRequest() method");
      }
    } catch (ProcessingException e) {
      if (e.getCause().getMessage().contains("PKIX path")) {
        log.error("The system at " + uri + " is not part of the same certificate chain of trust!");
        throw new AuthException("The system at " + uri + " is not part of the same certificate chain of trust!",
                                Status.UNAUTHORIZED.getStatusCode(), e);
      } else {
        log.error("UnavailableServerException occurred at " + uri, e);
        throw new UnavailableServerException("Could not get any response from: " + uri,
                                             Status.SERVICE_UNAVAILABLE.getStatusCode(), e);
      }
    }

    // If the response status code does not start with 2 the request was not successful
    if (!(response.getStatusInfo().getFamily() == Family.SUCCESSFUL)) {
      handleException(response, uri);
    }

    return response;
  }

  public static <T> Response sendRequest(String uri, String method, T payload) {
    return sendRequest(uri, method, payload, null);
  }

  //TODO option for async request sending which can be used by the event handler

  private static void handleException(Response response, String uri) {
    //The response body has to be extracted before the stream closes
    String errorMessageBody = toPrettyJson(null, response.getEntity());
    if (errorMessageBody == null || errorMessageBody.equals("null")) {
      response.bufferEntity();
      errorMessageBody = response.readEntity(String.class);
      if (errorMessageBody.length() > 250) {
        errorMessageBody = errorMessageBody.substring(0, 250);
      }
    }

    ErrorMessage errorMessage;
    try {
      errorMessage = response.readEntity(ErrorMessage.class);
    } catch (RuntimeException e) {
      log.error("Unknown reason for RuntimeException at the sendRequest() method.", e);
      log.info("Request failed, response status code: " + response.getStatus());
      log.info("Request failed, response body: " + errorMessageBody);
      throw new RuntimeException("Unknown error occurred at " + uri + ". Check log for possibly more information.", e);
    }
    if (errorMessage == null) {
      log.error("Unknown reason for RuntimeException at the sendRequest() method.");
      log.info("Request failed, response status code: " + response.getStatus());
      log.info("Request failed, response body: " + errorMessageBody);
      throw new RuntimeException("Unknown error occurred at " + uri + ". Check log for possibly more information.");
    } else if (errorMessage.getExceptionType() == null) {
      log.info("Request failed, response status code: " + response.getStatus());
      log.info("Request failed, response body: " + errorMessageBody);
      throw new RuntimeException("Unknown error occurred at " + uri + ". Check log for possibly more information.");
    } else {
      log.error("Request returned with " + errorMessage.getExceptionType().toString() + ": " + errorMessage
          .getErrorMessage());
      switch (errorMessage.getExceptionType()) {
        case ARROWHEAD:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                       errorMessage.getOrigin());
        case AUTH:
          throw new AuthException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                  errorMessage.getOrigin());
        case BAD_MEDIA_TYPE:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case BAD_METHOD:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                       errorMessage.getOrigin());
        case BAD_PAYLOAD:
          throw new BadPayloadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                        errorMessage.getOrigin());
        case BAD_URI:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                       errorMessage.getOrigin());
        case DATA_NOT_FOUND:
          throw new DataNotFoundException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                          errorMessage.getOrigin());
        case DNS_SD:
          throw new DnsException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(), errorMessage.getOrigin());
        case DUPLICATE_ENTRY:
          throw new DuplicateEntryException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                            errorMessage.getOrigin());
        case GENERIC:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                       errorMessage.getOrigin());
        case JSON_PROCESSING:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                       errorMessage.getOrigin());
        case UNAVAILABLE:
          throw new UnavailableServerException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(),
                                               errorMessage.getOrigin());
      }
    }
  }

  public static String getUri(String address, int port, String serviceURI, boolean isSecure, boolean serverStart) {
    if (address == null) {
      log.error("Address can not be null (Utility:getUri throws NPE)");
      throw new NullPointerException("Address can not be null (Utility:getUri throws NPE)");
    }

    UriBuilder ub = UriBuilder.fromPath("").host(address);
    if (isSecure) {
      ub.scheme("https");
    } else {
      ub.scheme("http");
    }
    if (port > 0) {
      ub.port(port);
    }
    if (serviceURI != null) {
      ub.path(serviceURI);
    }

    String url = ub.toString();
    try {
      new URI(url);
    } catch (URISyntaxException e) {
      if (serverStart) {
        throw new ServiceConfigurationError(
            url + " is not a valid URL to start a HTTP server! Please fix the address field in the properties file.");
      } else {
        log.error("Bad URL components passed to getUri() method");
        throw new ArrowheadException(url + " is not a valid URL!");
      }
    }

    log.info("Utility:getUri returning this: " + url);
    return url;
  }

  public static Optional<String[]> getServiceInfo(String serviceId) {
    ArrowheadService service = sslContext == null ? new ArrowheadService(createSD(serviceId, false),
                                                                         Collections.singleton("HTTP-INSECURE-JSON"),
                                                                         null)
                                                  : new ArrowheadService(createSD(serviceId, true),
                                                                         Collections.singleton("HTTP-SECURE-JSON"),
                                                                         ArrowheadMain.secureServerMetadata);
    ServiceQueryForm sqf = new ServiceQueryForm(service, true, false);
    Response response = sendRequest(SR_QUERY_URI, "PUT", sqf);
    ServiceQueryResult result = response.readEntity(ServiceQueryResult.class);
    if (result != null && result.isValid()) {
      ServiceRegistryEntry entry = result.getServiceQueryData().get(0);
      ArrowheadSystem coreSystem = entry.getProvider();
      boolean isSecure = false;
      if (!entry.getProvidedService().getServiceMetadata().isEmpty()) {
        isSecure = entry.getProvidedService().getServiceMetadata().containsKey("security");
      }
      String serviceURI = getUri(coreSystem.getAddress(), coreSystem.getPort(), entry.getServiceURI(), isSecure, false);
      if (serviceId.equals(CoreSystemService.GW_CONSUMER_SERVICE.getServiceDef()) || serviceId
          .equals(CoreSystemService.GW_PROVIDER_SERVICE.getServiceDef())) {
        return Optional.of(new String[]{serviceURI, coreSystem.getSystemName(), coreSystem.getAddress(),
            coreSystem.getAuthenticationInfo()});
      }
      return Optional.of(new String[]{serviceURI});
    }
    return Optional.empty();
  }

  public static List<String> getNeighborCloudURIs(boolean isSecure) {
    List<NeighborCloud> cloudList = new ArrayList<>(DatabaseManager.getInstance().getAll(NeighborCloud.class, null));

    List<String> uriList = new ArrayList<>();
    for (NeighborCloud cloud : cloudList) {
      if (isSecure == cloud.getCloud().isSecure()) {
        uriList.add(getUri(cloud.getCloud().getAddress(), cloud.getCloud().getPort(),
                           cloud.getCloud().getGatekeeperServiceURI(), cloud.getCloud().isSecure(), false));
      }
    }

    return uriList;
  }

  public static ArrowheadCloud getOwnCloud(boolean isSecure) {
    List<OwnCloud> cloudList = DatabaseManager.getInstance().getAll(OwnCloud.class, null);
    if (cloudList.isEmpty()) {
      log.error("Utility:getOwnCloud not found in the database.");
      throw new DataNotFoundException(
          "Own Cloud information not found in the database. This information is needed for the Gatekeeper System.",
          Status.NOT_FOUND.getStatusCode());
    }
    if (cloudList.size() > 2) {
      log.warn("own_cloud table should NOT have more than 2 rows.");
    }
    if (isSecure) {
      for (OwnCloud cloud : cloudList) {
        if (cloud.getCloud().isSecure()) {
          return cloud.getCloud();
        }
      }
      log.error("Utility:getOwnCloud finds no secure own cloud!");
      throw new DataNotFoundException("Could not find secure own cloud information in the database!",
                                      Status.NOT_FOUND.getStatusCode());
    } else {
      for (OwnCloud cloud : cloudList) {
        if (!cloud.getCloud().isSecure()) {
          return cloud.getCloud();
        }
      }
      log.error("Utility:getOwnCloud finds no insecure own cloud!");
      throw new DataNotFoundException("Could not find insecure own cloud information in the database!",
                                      Status.NOT_FOUND.getStatusCode());
    }
  }

  public static String stripEndSlash(String uri) {
    if (uri != null && uri.endsWith("/")) {
      return uri.substring(0, uri.length() - 1);
    }
    return uri;
  }

  public static String getRequestPayload(InputStream is) {
    StringBuilder sb = new StringBuilder();
    String line;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
    } catch (UnsupportedEncodingException e) {
      log.fatal("getRequestPayload ISReader has unsupported charset set!");
      throw new AssertionError(
          "getRequestPayload InputStreamReader has unsupported character set! Code needs to be changed!", e);
    } catch (IOException e) {
      log.error("IOException while reading the request payload");
      throw new RuntimeException("IOException occured while reading an incoming request payload", e);
    }

    if (!sb.toString().isEmpty()) {
      String payload = toPrettyJson(sb.toString(), null);
      return payload != null ? payload : "";
    } else {
      return "";
    }
  }

  public static String toPrettyJson(String jsonString, Object obj) {
    try {
      if (jsonString != null) {
        jsonString = jsonString.trim();
        if (jsonString.startsWith("{")) {
          Object tempObj = mapper.readValue(jsonString, Object.class);
          return mapper.writeValueAsString(tempObj);
        } else {
          Object[] tempObj = mapper.readValue(jsonString, Object[].class);
          return mapper.writeValueAsString(tempObj);
        }
      }
      if (obj != null) {
        return mapper.writeValueAsString(obj);
      }
    } catch (IOException e) {
      throw new ArrowheadException(
          "Jackson library threw IOException during JSON serialization! Wrapping it in RuntimeException. Exception "
              + "message: " + e.getMessage(), e);
    }
    return null;
  }

  public static <T> T fromJson(String json, Class<T> parsedClass) {
    try {
      return mapper.readValue(json, parsedClass);
    } catch (IOException e) {
      throw new ArrowheadException("Jackson library threw exception during JSON parsing!", e);
    }
  }

  public static String createSD(String baseSD, boolean isSecure) {
    if (isSecure) {
      return "Secure" + baseSD;
    } else {
      return "Insecure" + baseSD;
    }
  }

  public static TypeSafeProperties getProp(String fileName) {
    TypeSafeProperties prop = new TypeSafeProperties();
    try {
      File file;
      if (new File(fileName).exists()) {
        file = new File(fileName);
      } else {
        file = new File("config" + File.separator + fileName);
      }
      FileInputStream inputStream = new FileInputStream(file);
      prop.load(inputStream);
    } catch (FileNotFoundException ex) {
      throw new ServiceConfigurationError(
          fileName + " file not found, make sure you have the correct working directory "
              + "set! (directory where the config folder can be found)", ex);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return prop;
  }

  public static TypeSafeProperties getProp() {
    TypeSafeProperties prop = new TypeSafeProperties();

    try {
      if (Files.isReadable(Paths.get(DEFAULT_CONF))) {
        prop.load(new FileInputStream(DEFAULT_CONF));
      } else if (Files.isReadable(Paths.get(DEFAULT_CONF_DIR))) {
        prop.load(new FileInputStream(DEFAULT_CONF_DIR));
      } else {
        throw new ServiceConfigurationError(
            "default.conf file not found in the working directory! (" + System.getProperty("user.dir") + ")");
      }

      if (Files.isReadable(Paths.get(APP_CONF))) {
        prop.load(new FileInputStream(APP_CONF));
      } else if (Files.isReadable(Paths.get(APP_CONF_DIR))) {
        prop.load(new FileInputStream(APP_CONF_DIR));
      }
    } catch (IOException e) {
      throw new AssertionError("File loading failed...", e);
    }

    //If MySQL based JDBC URLs are used, we append the system default time zone to the URL
    //This is for a bug fix with certain MySQL JDBC driver versions: https://github.com/arrowhead-f/core-java/issues/30
    String timeZoneQueryParam = "serverTimezone=" + ZoneId.systemDefault().getId();

    String dbAddress = prop.getProperty("db_address");
    if (dbAddress != null && dbAddress.contains("mysql")) {
      if (dbAddress.contains("?")) {
        dbAddress = dbAddress + "&" + timeZoneQueryParam;
      } else {
        dbAddress = dbAddress + "?" + timeZoneQueryParam;
      }
      prop.setProperty("db_address", dbAddress);
    }

    String logAddress = prop.getProperty("log4j.appender.DB.URL");
    if (logAddress != null && logAddress.contains("mysql")) {
      if (logAddress.contains("?")) {
        logAddress = logAddress + "&" + timeZoneQueryParam;
      } else {
        logAddress = logAddress + "?" + timeZoneQueryParam;
      }
      prop.setProperty("log4j.appender.DB.URL", logAddress);
    }

    return prop;
  }

  public static void checkProperties(Set<String> propertyNames, List<String> mandatoryProperties) {
    if (mandatoryProperties == null || mandatoryProperties.isEmpty()) {
      return;
    }
    //Arrays.asList() returns immutable lists, so we have to copy it first
    List<String> properties = new ArrayList<>(mandatoryProperties);
    if (!propertyNames.containsAll(mandatoryProperties)) {
      properties.removeIf(propertyNames::contains);
      throw new ServiceConfigurationError("Missing field(s) from config file: " + properties.toString());
    }
  }

  /* If needed, this method can be used to get the IPv4 address of the host machine. Public point-to-point IP
  addresses are prioritized over private
    (site local) IP addresses */
  @SuppressWarnings("unused")
  public static String getIpAddress() throws SocketException {
    List<InetAddress> addresses = new ArrayList<>();

    Enumeration e = NetworkInterface.getNetworkInterfaces();
    while (e.hasMoreElements()) {
      NetworkInterface inf = (NetworkInterface) e.nextElement();
      Enumeration ee = inf.getInetAddresses();
      while (ee.hasMoreElements()) {
        addresses.add((InetAddress) ee.nextElement());
      }
    }

    addresses = addresses.stream().filter(current -> !current.getHostAddress().contains(":"))
                         .filter(current -> !current.isLoopbackAddress())
                         .filter(current -> !current.isMulticastAddress())
                         .filter(current -> !current.isLinkLocalAddress()).collect(Collectors.toList());
    if (addresses.isEmpty()) {
      throw new SocketException("No valid addresses left after filtering");
    }
    for (InetAddress address : addresses) {
      if (!address.isSiteLocalAddress()) {
        return address.getHostAddress();
      }
    }
    return addresses.get(0).getHostAddress();
  }

  public static <T> boolean isBeanValid(T bean) {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<T>> violations = validator.validate(bean);
    return violations.isEmpty();
  }

  @SuppressWarnings("WeakerAccess")
  public static Throwable getExceptionRootCause(Throwable e) {
    Throwable cause = null;
    Throwable result = e;

    while (null != (cause = result.getCause()) && (result != cause)) {
      result = cause;
    }
    return result;
  }
}

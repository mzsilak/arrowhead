/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.onboarding;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.messages.OrchestrationResponse;
import eu.arrowhead.common.messages.ServiceQueryForm;
import eu.arrowhead.common.messages.ServiceQueryResult;
import eu.arrowhead.common.messages.ServiceRequestForm;
import eu.arrowhead.common.misc.CoreSystem;
import eu.arrowhead.common.misc.CoreSystemService;
import eu.arrowhead.common.misc.TypeSafeProperties;
import eu.arrowhead.core.certificate_authority.model.CertificateSigningRequest;
import eu.arrowhead.core.certificate_authority.model.CertificateSigningResponse;
import eu.arrowhead.core.onboarding.model.ServiceEndpoint;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

public class OnboardingService {

  // TODO CA does not expose a service ?!
  private static final String PROPERTY_SR_IP = "sr_address";
  private static final String PROPERTY_CA_IP = "ca_address";
  private static final String PROPERTY_UNKNOWN_ENABLED = "enable_unknown";
  private static final String PROPERTY_SHARED_KEY_ENABLED = "enable_shared_key";
  private static final String PROPERTY_CERTIFICATE_ENABLED = "enable_certificate";
  private static final String PROPERTY_SHARED_KEY = "shared_key";

  private final TypeSafeProperties props;
  private String caBaseUri;
  private String srBaseUri;

  private final Logger log = LogManager.getLogger(OnboardingService.class);

  public OnboardingService() {
    props = Utility.getProp();

    caBaseUri = Utility.getUri(getCaIp(), Utility.isSecure() ? CoreSystem.CERTIFICATE_AUTHORITY.getSecurePort()
                                                             : CoreSystem.CERTIFICATE_AUTHORITY.getInsecurePort(), "ca",
                               Utility.isSecure(), false);

    srBaseUri = Utility.getUri(getSrIp(), Utility.isSecure() ? CoreSystem.SERVICE_REGISTRY_SQL.getSecurePort()
                                                             : CoreSystem.SERVICE_REGISTRY_SQL.getInsecurePort(),
                               CoreSystemService.SERVICE_LOOKUP_SERVICE.getServiceURI(), Utility.isSecure(), false);
  }

  private String getCaIp() {
    return props.getProperty(PROPERTY_CA_IP, "127.0.0.1");
  }

  private String getSrIp() {
    return props.getProperty(PROPERTY_SR_IP, "127.0.0.1");
  }

  public boolean isUnknownAllowed() {
    return props.getBooleanProperty(PROPERTY_UNKNOWN_ENABLED, false);
  }

  public boolean isSharedKeyAllowed() {
    return props.getBooleanProperty(PROPERTY_SHARED_KEY_ENABLED, false);
  }

  public boolean isCertificateAllowed() {
    return props.getBooleanProperty(PROPERTY_CERTIFICATE_ENABLED, true);
  }

  public KeyPair generateKeyPair(final String algorithm, int keySize) throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
    keyPairGenerator.initialize(keySize, new SecureRandom());
    return keyPairGenerator.generateKeyPair();
  }

  public CertificateSigningResponse createAndSignCertificate(final String name, final KeyPair keyPair)
    throws IOException, OperatorCreationException {
    final String cloudName = getCloudname(caBaseUri);
    final String encodedCert = prepareAndCreateCSR(name + "." + cloudName, keyPair);
    return sendCsr(new CertificateSigningRequest(encodedCert));
  }

  private String getCloudname(final String caUri) {
    Response caResponse = Utility.sendRequest(caUri, "GET", null);
    String ret = caResponse.readEntity(String.class);
    caResponse.close();
    return ret;
  }

  private String prepareAndCreateCSR(final String name, final KeyPair keyPair)
    throws IOException, OperatorCreationException {

    final X500Name x500Name = new X500Name("CN=" + name);
    final JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(x500Name,
                                                                                                  keyPair.getPublic());
    final JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
    final ContentSigner contentSigner = contentSignerBuilder.build(keyPair.getPrivate());
    final PKCS10CertificationRequest csr = builder.build(contentSigner);
    return Base64.getEncoder().encodeToString(csr.getEncoded());
  }

  private CertificateSigningResponse sendCsr(final CertificateSigningRequest csr) {
    log.info("sending csr to CA (POST)...");
    final Response caResponse = Utility.sendRequest(caBaseUri, "POST", csr);
    CertificateSigningResponse ret = caResponse.readEntity(CertificateSigningResponse.class);
    caResponse.close();
    return ret;
  }

  public CertificateSigningResponse signCertificate(final JcaPKCS10CertificationRequest providedCsr)
    throws IOException {
    final String encodedCert = Base64.getEncoder().encodeToString(providedCsr.getEncoded());
    return sendCsr(new CertificateSigningRequest(encodedCert));
  }

  public ServiceEndpoint[] getEndpoints() throws URISyntaxException {
    // first we need the endpoint of the orchestration service

    //You can put any additional metadata you look for in a Service here (key-value pairs)
    final Map<String, String> metadata = new HashMap<>();
    metadata.put("security", "certificate");

    final Set<String> interfaces =
      Utility.isSecure() ? Collections.singleton("HTTP-SECURE-JSON") : Collections.singleton("HTTP-INSECURE-JSON");

    log.info("Getting publish service endpoints of orchestration service...");
    final ArrowheadService orchestration = compileService(CoreSystemService.ORCH_SERVICE, interfaces, metadata);
    final ServiceRegistryEntry orchSRE = sendServiceLookupRequest(orchestration);

    final String orchServiceFullURI = Utility
      .getUri(orchSRE.getProvider().getAddress(), orchSRE.getProvider().getPort(), orchSRE.getServiceURI(),
              Utility.isSecure(), false);

    log.info(String.format("Orch service full URI: %s", orchServiceFullURI));


        /*
      ArrowheadService: serviceDefinition (name), interfaces, metadata
      Interfaces: supported message formats (e.g. JSON, XML, JSON-SenML), a potential provider has to have at least 1
       match,
      so the communication between consumer and provider can be facilitated.
     */

    log.info("Getting publish service endpoints of registries...");

    ArrowheadService servDevReg = compileService(CoreSystemService.DEVICE_REGISTRY_SERVICE, interfaces, metadata);
    ArrowheadService servSysReg = compileService(CoreSystemService.SYSTEM_REGISTRY_SERVICE, interfaces, metadata);
    ArrowheadService servSerReg = compileService(CoreSystemService.SERVICE_REGISTRY_SERVICE, interfaces, metadata);

    //Some of the orchestrationFlags the consumer can use, to influence the orchestration process
    final Map<String, Boolean> orchestrationFlags = new HashMap<>();
    orchestrationFlags.put("overrideStore", true);

    final ServiceRequestForm devRegSRF = compileSRF(servDevReg, orchestrationFlags);
    final ServiceRequestForm sysRegSRF = compileSRF(servSysReg, orchestrationFlags);
    final ServiceRequestForm serRegSRF = compileSRF(servSerReg, orchestrationFlags);

    log.info("sending orchestration requests (devreg, sysreg and serreg services)...");

    String devregServiceURI = sendOrchestrationRequest(orchServiceFullURI, devRegSRF, false);
    String sysregServiceURI = sendOrchestrationRequest(orchServiceFullURI, sysRegSRF, false);
    String serregServiceURI = sendOrchestrationRequest(orchServiceFullURI, serRegSRF, true);

    log.info(String.format("dev reg service endpoint: %s", devregServiceURI));
    log.info(String.format("sys reg service endpoint: %s", sysregServiceURI));
    log.info(String.format("ser reg service endpoint: %s", serregServiceURI));

    final List<ServiceEndpoint> endpoints = new ArrayList<>();

    if (devregServiceURI != null) {
      endpoints.add(new ServiceEndpoint(CoreSystemService.DEVICE_REGISTRY_SERVICE, new URI(devregServiceURI)));
    }

    if (sysregServiceURI != null) {
      endpoints.add(new ServiceEndpoint(CoreSystemService.SYSTEM_REGISTRY_SERVICE, new URI(sysregServiceURI)));
    }

    if (serregServiceURI != null) {
      endpoints.add(new ServiceEndpoint(CoreSystemService.SERVICE_REGISTRY_SERVICE, new URI(serregServiceURI)));
    }

    return endpoints.toArray(new ServiceEndpoint[0]);
  }

  private ArrowheadService compileService(final CoreSystemService service, final Set<String> interfaces,
                                          final Map<String, String> metadata) {
    final String serviceDefinition = Utility.createSD(service.getServiceDef(), Utility.isSecure());
    return new ArrowheadService(serviceDefinition, interfaces, metadata);

  }

  public boolean isKeyValid(final String providedKey) {
    final String sharedKey = props.getProperty(PROPERTY_SHARED_KEY, null);

    if (sharedKey == null || providedKey == null) {
      return false;
    }

    return sharedKey.equals(providedKey);
  }

  //code taken from client-java consumer code
  private ServiceRequestForm compileSRF(ArrowheadService arrowheadService, Map<String, Boolean> orchestrationFlags) {


    /*
      ArrowheadSystem: systemName, (address, port, authenticationInfo)
      Since this Consumer skeleton will not receive HTTP requests (does not provide any services on its own),
      the address, port and authenticationInfo fields can be set to anything.
      SystemName can be an arbitrarily chosen name, which makes sense for the use case.
     */
    String hostAddress = props.getProperty("address", "0.0.0.0");
    if ("0.0.0.0".equals(hostAddress)) {
      try {
        hostAddress = Utility.getIpAddress();
      } catch (SocketException e) {
        // noop
      }
    }

    int port = Utility.isSecure() ? props.getIntProperty("secure_port", CoreSystem.ONBOARDING.getSecurePort())
                                  : props.getIntProperty("insecure_port", CoreSystem.ONBOARDING.getInsecurePort());

    //TODO: systemName as constant (?)
    ArrowheadSystem consumer = new ArrowheadSystem(CoreSystem.ONBOARDING.name(), hostAddress, port, "null");
    //You can put any additional metadata you look for in a Service here (key-value pairs)
//        Map<String, String> metadata = new HashMap<>();
//        metadata.put("unit", "celsius");
//        if (isSecure) {
    //This is a mandatory metadata when using TLS, do not delete it
//        metadata.put("security", "certificate");
//        }
        /*
      ArrowheadService: serviceDefinition (name), interfaces, metadata
      Interfaces: supported message formats (e.g. JSON, XML, JSON-SenML), a potential provider has to have at least 1
       match,
      so the communication between consumer and provider can be facilitated.
     */
//        ArrowheadService servDevReg = new ArrowheadService(Utility.createSD(CoreSystemService.DEVICE_REGISTRY_SERVICE
//        .getServiceDef(), true), Collections
//            .singleton("HTTP-SECURE-JSON"), metadata);

    //Some of the orchestrationFlags the consumer can use, to influence the orchestration process
//        Map<String, Boolean> orchestrationFlags = new HashMap<>();

//        orchestrationFlags.put("overrideStore", true);

    ServiceRequestForm srf = new ServiceRequestForm.Builder(consumer).requestedService(arrowheadService)
                                                                     .orchestrationFlags(orchestrationFlags).build();

    log.info("Orchestration srf:" + Utility.toPrettyJson(null, srf));

    return srf;
  }

  private ServiceRegistryEntry sendServiceLookupRequest(final ArrowheadService service) {

    final int maxRetries = 3;
    int retries = 0;

    final ServiceQueryForm serviceQueryForm = new ServiceQueryForm(service, true, false);

    log.info("Searching for \"" + service.getServiceDefinition() + "\" on service registry");

    while (retries < maxRetries) {
      try {
        // send lookup request to the service registry
        final Response response = Utility.sendRequest(srBaseUri, "PUT", serviceQueryForm);

        // verify that there is no HTTP error
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          final ServiceQueryResult lookupResult = response.readEntity(ServiceQueryResult.class);
          response.close();

          // check if the entity is valid and actually found a service
          if (lookupResult.isValid() && !lookupResult.getServiceQueryData().isEmpty()) {
            final List<ServiceRegistryEntry> serviceQueryData = lookupResult.getServiceQueryData();
            return serviceQueryData.get(0);
          }
        }
      } catch (Exception e) {
        log.error(e.getMessage());
      }

      try {
        retries++;
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        // noop
      }
    }

    throw new DataNotFoundException("Unable to find " + service.getServiceDefinition());
  }

  private String sendOrchestrationRequest(final String orchestrationURI, final ServiceRequestForm srf,
                                          final boolean mandatory) {
    final int maxRetries = 3;
    int retries = 0;
    String error = null;

    while (retries < maxRetries) {
      try {
        //Sending a POST request to the orchestrator (URL, method, payload)
        final Response postResponse = Utility.sendRequest(orchestrationURI, "POST", srf);

        // verify that there is no HTTP error
        if (postResponse.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          //Parsing the orchestrator response
          OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
          log.debug("Orchestration Response payload: " + Utility.toPrettyJson(null, orchResponse));
          postResponse.close();

          if (!orchResponse.getResponse().isEmpty()) {

            String serviceURI = Utility.getUri(orchResponse.getResponse().get(0).getProvider().getAddress(),
                                               orchResponse.getResponse().get(0).getProvider().getPort(),
                                               orchResponse.getResponse().get(0).getServiceURI(), Utility.isSecure(),
                                               false);

            log.info(String.format("Retrieved service URI from orchestrator: %s", serviceURI));

            return serviceURI;
          }
        }
      } catch (Exception e) {
        error = e.getMessage();
        log.warn(error);
      }

      try {
        retries++;
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        // noop
      }
    }

    final String errorMessage = String
      .format("Orchestration for %s failed: %s", srf.getRequestedService().getServiceDefinition(), error);
    if (mandatory) {
      log.fatal(errorMessage);
      throw new ArrowheadException(errorMessage);
    } else {
      log.error(errorMessage);
      return null;
    }
  }
}

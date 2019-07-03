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
import eu.arrowhead.common.messages.OrchestrationResponse;
import eu.arrowhead.common.messages.ServiceRequestForm;
import eu.arrowhead.common.misc.CoreSystem;
import eu.arrowhead.common.misc.CoreSystemService;
import eu.arrowhead.common.misc.TypeSafeProperties;
import eu.arrowhead.core.certificate_authority.model.CertificateSigningRequest;
import eu.arrowhead.core.certificate_authority.model.CertificateSigningResponse;
import eu.arrowhead.core.onboarding.model.ServiceEndpoint;
import java.io.IOException;
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
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.apache.log4j.Logger;

public class OnboardingService {

    // TODO CA does not expose a service ?!
    private static final String PROPERTY_CA_IP = "ca_url";
    private static final String PROPERTY_UNKNOWN_ENABLED = "enable_unknown";
    private static final String PROPERTY_SHARED_KEY_ENABLED = "enable_shared_key";
    private static final String PROPERTY_CERTIFICATE_ENABLED = "enable_certificate";
    private static final String PROPERTY_SHARED_KEY = "shared_key";

    private final TypeSafeProperties props;
    private final String caUri;

    private final Logger log = Logger.getLogger(OnboardingService.class.getName());

    public OnboardingService() {
        props = Utility.getProp();
//        caUri = Utility.getUri(getCaIp(), CoreSystem.CERTIFICATE_AUTHORITY.getInsecurePort(), "ca", false, false);
        caUri = Utility.getUri(getCaIp(), CoreSystem.CERTIFICATE_AUTHORITY.getSecurePort(), "ca", true, false);
    }

    public String getCaIp() {
        return props.getProperty(PROPERTY_CA_IP, "127.0.0.1");
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
        final String cloudName = getCloudname(caUri);
        final String encodedCert = prepareAndCreateCSR(name + "." + cloudName, keyPair);
        return sendCsr(new CertificateSigningRequest(encodedCert));
    }

    private String getCloudname(final String caUri) {
        Response caResponse = Utility.sendRequest(caUri, "GET", null);
        return caResponse.readEntity(String.class);
    }

    private String prepareAndCreateCSR(final String name, final KeyPair keyPair)
        throws IOException, OperatorCreationException {

        final X500Name x500Name = new X500Name("CN=" + name);
        final JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(x500Name, keyPair.getPublic());
        final JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
        final ContentSigner contentSigner = contentSignerBuilder.build(keyPair.getPrivate());
        final PKCS10CertificationRequest csr = builder.build(contentSigner);
        return Base64.getEncoder().encodeToString(csr.getEncoded());
    }

    private CertificateSigningResponse sendCsr(final CertificateSigningRequest csr) {
        log.info("sending csr to CA (POST)...");
        final Response caResponse = Utility.sendRequest(caUri, "POST", csr);
        return caResponse.readEntity(CertificateSigningResponse.class);
    }

    public CertificateSigningResponse signCertificate(final JcaPKCS10CertificationRequest providedCsr)
        throws IOException {
        final String encodedCert = Base64.getEncoder().encodeToString(providedCsr.getEncoded());
        return sendCsr(new CertificateSigningRequest(encodedCert));
    }

    public ServiceEndpoint[] getEndpoints() throws URISyntaxException {

        log.info("Getting publish service endpoints of device registry and system registry...");

        final String orchServiceURI = CoreSystemService.ORCH_SERVICE.getServiceURI();
        final String orcServiceDef = CoreSystemService.ORCH_SERVICE.getServiceDef();

        log.info(String.format("Resolved orchestrator service uri %s, def: %s", orchServiceURI, orcServiceDef));

        log.info(String.format("Orch service number: %d", CoreSystem.ORCHESTRATOR.getServices().size()));

        final ServiceEndpoint sepOrch = new ServiceEndpoint(CoreSystem.ORCHESTRATOR, new URI(orchServiceURI));

        log.info(String.format("Orch service ep: %s", sepOrch.getUri().toString()));

        final String orchServiceFullURI = Utility.getUri("0.0.0.0", CoreSystem.ORCHESTRATOR.getSecurePort(), orchServiceURI, true, false);

        log.info(String.format("Orch service full URI: %s", orchServiceFullURI));

        //You can put any additional metadata you look for in a Service here (key-value pairs)
        Map<String, String> metadata = new HashMap<>();
        metadata.put("security", "certificate");

        /*
      ArrowheadService: serviceDefinition (name), interfaces, metadata
      Interfaces: supported message formats (e.g. JSON, XML, JSON-SenML), a potential provider has to have at least 1 match,
      so the communication between consumer and provider can be facilitated.
     */
        ArrowheadService servDevReg = new ArrowheadService(Utility.createSD(CoreSystemService.DEVICE_REG_SERVICE.getServiceDef(), true), Collections
            .singleton("HTTP-SECURE-JSON"), metadata);

        ArrowheadService servSysReg =
            new ArrowheadService(Utility.createSD(CoreSystemService.SYS_REG_SERVICE.getServiceDef(), true), Collections
            .singleton("HTTP-SECURE-JSON"), metadata);

        //Some of the orchestrationFlags the consumer can use, to influence the orchestration process

        Map<String, Boolean> orchestrationFlags = new HashMap<>();
        orchestrationFlags.put("overrideStore", true);

        final ServiceRequestForm devRegSRF = compileSRF(servDevReg, orchestrationFlags);
        final ServiceRequestForm sysRegSRF = compileSRF(servSysReg, orchestrationFlags);

        log.info("sending orchestration requests (devreg and sysreg services)...");

        String devregServiceURI = sendOrchestrationRequest(orchServiceFullURI, devRegSRF);
        String sysregServiceURI = sendOrchestrationRequest(orchServiceFullURI, sysRegSRF);


        log.info(String.format("dev reg service endpoint: %s", devregServiceURI));
        log.info(String.format("sys reg service endpoint: %s", sysregServiceURI));

        final List<ServiceEndpoint> endpoints = new ArrayList<>();

        endpoints.add(new ServiceEndpoint(CoreSystem.DEVICE_REGISTRY, new URI(devregServiceURI)));
        endpoints.add(new ServiceEndpoint(CoreSystem.SYSTEM_REGISTRY, new URI(sysregServiceURI)));

        return endpoints.toArray(new ServiceEndpoint[0]);

    }

    public boolean isKeyValid(final String providedKey) {
        final String sharedKey = props.getProperty(PROPERTY_SHARED_KEY, null);

        if (sharedKey == null || providedKey == null) {
            return false;
        }

        return sharedKey.equals(providedKey);
    }

    //code taken from client-java consumer code
    private ServiceRequestForm compileSRF(ArrowheadService arrowheadService, Map<String, Boolean> orchestrationFlags){


    /*
      ArrowheadSystem: systemName, (address, port, authenticationInfo)
      Since this Consumer skeleton will not receive HTTP requests (does not provide any services on its own),
      the address, port and authenticationInfo fields can be set to anything.
      SystemName can be an arbitrarily chosen name, which makes sense for the use case.
     */
    //TODO: systemName as constant (?)
        ArrowheadSystem consumer = new ArrowheadSystem(CoreSystem.ONBOARDING.name(), props.getProperty("address", "0.0.0.0"),
                                                       props.getIntProperty("secure_port", CoreSystem.ONBOARDING.getSecurePort()),
                                                       "null");
        //You can put any additional metadata you look for in a Service here (key-value pairs)
//        Map<String, String> metadata = new HashMap<>();
//        metadata.put("unit", "celsius");
//        if (isSecure) {
        //This is a mandatory metadata when using TLS, do not delete it
//        metadata.put("security", "certificate");
//        }
        /*
      ArrowheadService: serviceDefinition (name), interfaces, metadata
      Interfaces: supported message formats (e.g. JSON, XML, JSON-SenML), a potential provider has to have at least 1 match,
      so the communication between consumer and provider can be facilitated.
     */
//        ArrowheadService servDevReg = new ArrowheadService(Utility.createSD(CoreSystemService.DEVICE_REG_SERVICE.getServiceDef(), true), Collections
//            .singleton("HTTP-SECURE-JSON"), metadata);

        //Some of the orchestrationFlags the consumer can use, to influence the orchestration process
//        Map<String, Boolean> orchestrationFlags = new HashMap<>();

//        orchestrationFlags.put("overrideStore", true);

        ServiceRequestForm srf =
            new ServiceRequestForm.Builder(consumer).requestedService(arrowheadService).orchestrationFlags(orchestrationFlags).build();

        log.info("Orchestration srf:" + Utility.toPrettyJson(null, srf));

        return srf;
    }

    private String sendOrchestrationRequest(String orchestrationURI, ServiceRequestForm srf){
        //Sending a POST request to the orchestrator (URL, method, payload)
        Response postResponse = Utility.sendRequest(orchestrationURI, "POST", srf);
        //Parsing the orchestrator response
        OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
        System.out.println("Orchestration Response payload: " + Utility.toPrettyJson(null, orchResponse));
        if (orchResponse.getResponse().isEmpty()) {
//            throw new ArrowheadException("Orchestrator returned with 0 Orchestration Forms!");

            log.info(String.format("Orchestration for %s failed", srf.getRequestedService().getServiceDefinition()));

        }

        //Getting the first provider from the response
//        ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
//        String serviceURI =
//            orchResponse.getResponse().get(0).getProvider().getAddress() + orchResponse.getResponse().get(0).getProvider().getPort() + orchResponse.getResponse().get(0).getServiceURI() + orchResponse.getResponse().get(0).getService().getServiceDefinition();
        String serviceURI =
            Utility.getUri(orchResponse.getResponse().get(0).getProvider().getAddress(),
                           orchResponse.getResponse().get(0).getProvider().getPort(),
                           orchResponse.getResponse().get(0).getServiceURI(), true, false);

        log.info(String.format("Retrieved service URI from orchestrator: %s", serviceURI));


        return serviceURI;

    }
}

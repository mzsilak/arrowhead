package eu.arrowhead.common.opcua;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_X509;

import java.io.File;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedHttpsCertificateBuilder;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.slf4j.LoggerFactory;

public class ArrowheadOpcUaServer {

	private static ArrowheadOpcUaServer arrowheadOpcUaserver;

	public static ArrowheadOpcUaServer getInstance(int port, String endpoint) {
		if (arrowheadOpcUaserver == null) {
			synchronized (ArrowheadOpcUaServer.class) {
				try {
					arrowheadOpcUaserver = new ArrowheadOpcUaServer(port, endpoint);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return arrowheadOpcUaserver;
	}

	private static final int HTTPS_BIND_PORT = 8443;

	static {

		// Required for SecurityPolicy.Aes256_Sha256_RsaPss
		Security.addProvider(new BouncyCastleProvider());
	}

	private final OpcUaServer server;
	private UaNodeContext nodeContext;
	private UaNodeManager nodeManager;

	private ArrowheadOpcUaServer(int port, String endpoint) throws Exception {
		File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");
		if (!securityTempDir.exists() && !securityTempDir.mkdirs()) {
			throw new Exception("unable to create security temp dir: " + securityTempDir);
		}
		LoggerFactory.getLogger(getClass()).info("security temp dir: {}", securityTempDir.getAbsolutePath());

		KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

		DefaultCertificateManager certificateManager = new DefaultCertificateManager(loader.getServerKeyPair(),
				loader.getServerCertificateChain());

		File pkiDir = securityTempDir.toPath().resolve("pki").toFile();
		DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);
		LoggerFactory.getLogger(getClass()).info("pki dir: {}", pkiDir.getAbsolutePath());

		DefaultCertificateValidator certificateValidator = new DefaultCertificateValidator(trustListManager);

		KeyPair httpsKeyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

		SelfSignedHttpsCertificateBuilder httpsCertificateBuilder = new SelfSignedHttpsCertificateBuilder(httpsKeyPair);
		httpsCertificateBuilder.setCommonName(HostnameUtil.getHostname());
		HostnameUtil.getHostnames("0.0.0.0").forEach(httpsCertificateBuilder::addDnsName);
//		X509Certificate httpsCertificate = httpsCertificateBuilder.build();

		UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(true, authChallenge -> {
			String username = authChallenge.getUsername();
			String password = authChallenge.getPassword();

			boolean userOk = "user".equals(username) && "password1".equals(password);
			boolean adminOk = "admin".equals(username) && "password2".equals(password);

			return userOk || adminOk;
		});

		X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);

		// If you need to use multiple certificates you'll have to be smarter than this.
		X509Certificate certificate = certificateManager.getCertificates().stream().findFirst()
				.orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError, "no certificate found"));

		// The configured application URI must match the one in the certificate(s)
		String applicationUri = CertificateUtil.getSanUri(certificate)
				.orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError,
						"certificate is missing the application URI"));

		Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations(certificate, endpoint, port);

		OpcUaServerConfig serverConfig = OpcUaServerConfig.builder().setApplicationUri(applicationUri)
				.setApplicationName(LocalizedText.english("Arrowhead OPC UA "+ endpoint.toUpperCase() + " Server"))
				.setEndpoints(endpointConfigurations)
				.setBuildInfo(new BuildInfo("urn:arrowhead:opcua-sr-server", "arrowhead", "arrowhead opcua server",
						OpcUaServer.SDK_VERSION, "", DateTime.now()))
				.setCertificateManager(certificateManager).setTrustListManager(trustListManager)
				.setCertificateValidator(certificateValidator).setHttpsKeyPair(httpsKeyPair)
//				.setHttpsCertificate(httpsCertificate)
				.setIdentityValidator(new CompositeValidator(identityValidator, x509IdentityValidator))
				.setProductUri("urn:arrowhead:opcua-sr-server").build();

		server = new OpcUaServer(serverConfig);

		ArrowheadOpcUaNamespace namespace = new ArrowheadOpcUaNamespace(server,
				"urn:arrowhead:namespace");
		namespace.startup();
		nodeContext = namespace.getNamespaceNodeContext();
		nodeManager = namespace.getNamespaceNodeManager();
	}

	private Set<EndpointConfiguration> createEndpointConfigurations(X509Certificate certificate, String endpoint, int port) {
		Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

		List<String> bindAddresses = newArrayList();
		bindAddresses.add("0.0.0.0");

		Set<String> hostnames = new LinkedHashSet<>();
		hostnames.add(HostnameUtil.getHostname());
		hostnames.addAll(HostnameUtil.getHostnames("0.0.0.0"));

		for (String bindAddress : bindAddresses) {
			for (String hostname : hostnames) {
				EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder().setBindAddress(bindAddress)
						.setHostname(hostname).setPath("/" + endpoint).setCertificate(certificate).addTokenPolicies(
								USER_TOKEN_POLICY_ANONYMOUS, USER_TOKEN_POLICY_USERNAME, USER_TOKEN_POLICY_X509);

				EndpointConfiguration.Builder noSecurityBuilder = builder.copy().setSecurityPolicy(SecurityPolicy.None)
						.setSecurityMode(MessageSecurityMode.None);

				endpointConfigurations.add(buildTcpEndpoint(port, noSecurityBuilder));
//				endpointConfigurations.add(buildHttpsEndpoint(noSecurityBuilder));

				// TCP Basic256Sha256 / SignAndEncrypt
//				endpointConfigurations
//						.add(buildTcpEndpoint(port, builder.copy().setSecurityPolicy(SecurityPolicy.Basic256Sha256)
//								.setSecurityMode(MessageSecurityMode.SignAndEncrypt)));

				// HTTPS Basic256Sha256 / Sign (SignAndEncrypt not allowed for HTTPS)
//				endpointConfigurations.add(buildHttpsEndpoint(builder.copy()
//						.setSecurityPolicy(SecurityPolicy.Basic256Sha256).setSecurityMode(MessageSecurityMode.Sign)));

				/*
				 * It's good practice to provide a discovery-specific endpoint with no security.
				 * It's required practice if all regular endpoints have security configured.
				 *
				 * Usage of the "/discovery" suffix is defined by OPC UA Part 6:
				 *
				 * Each OPC UA Server Application implements the Discovery Service Set. If the
				 * OPC UA Server requires a different address for this Endpoint it shall create
				 * the address by appending the path "/discovery" to its base address.
				 */

				EndpointConfiguration.Builder discoveryBuilder = builder.copy().setPath("/" + endpoint + "/discovery")
						.setSecurityPolicy(SecurityPolicy.None).setSecurityMode(MessageSecurityMode.None);

				endpointConfigurations.add(buildTcpEndpoint(port, discoveryBuilder));
//				endpointConfigurations.add(buildHttpsEndpoint(discoveryBuilder));
			}
		}

		return endpointConfigurations;
	}

	private static EndpointConfiguration buildTcpEndpoint(int port, EndpointConfiguration.Builder base) {
		return base.copy().setTransportProfile(TransportProfile.TCP_UASC_UABINARY).setBindPort(port).build();
	}

	private static EndpointConfiguration buildHttpsEndpoint(EndpointConfiguration.Builder base) {
		return base.copy().setTransportProfile(TransportProfile.HTTPS_UABINARY).setBindPort(HTTPS_BIND_PORT).build();
	}

	public OpcUaServer getServer() {
		return server;
	}

	public UaNodeContext getNodeContext() {
		return nodeContext;
	}

	public UaNodeManager getNodeManager() {
		return nodeManager;
	}

	public CompletableFuture<OpcUaServer> startup() {
		return server.startup();
	}

	public CompletableFuture<OpcUaServer> shutdown() {
		return server.shutdown();
	}

	public void addMethodNodeInNamespace(UaMethodNode methodNode, UaFolderNode folderNode,
			AbstractMethodInvocationHandler methodClass) {

		methodNode.setProperty(UaMethodNode.InputArguments, methodClass.getInputArguments());
		methodNode.setProperty(UaMethodNode.OutputArguments, methodClass.getOutputArguments());
		methodNode.setInvocationHandler(methodClass);

		nodeManager.addNode(methodNode);

		methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent,
				folderNode.getNodeId().expanded(), false));

		nodeManager.addNode(methodNode);

		folderNode.addReference(new Reference(folderNode.getNodeId(), Identifiers.HasComponent,
				methodNode.getNodeId().expanded(), true));

		methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent,
				folderNode.getNodeId().expanded(), false));

	}

	public UaMethodNode addMethodNode(UShort namespaceIndex, UaFolderNode folderNode, String name) {
		UaMethodNode newMethodNode = UaMethodNode.builder(nodeContext).setNodeId(new NodeId(namespaceIndex, name))
				.setBrowseName(new QualifiedName(namespaceIndex, name)).setDisplayName(new LocalizedText(null, name))
				.setDescription(LocalizedText.english("")).build();

		return newMethodNode;
	};

	public UaFolderNode addFolder(UShort namespaceIndex, String name) {
		NodeId folderNodeId = new NodeId(namespaceIndex, name);

		UaFolderNode folderNode = new UaFolderNode(nodeContext, folderNodeId, new QualifiedName(namespaceIndex, name),
				LocalizedText.english(name));

		nodeManager.addNode(folderNode);

		folderNode.addReference(
				new Reference(Identifiers.ObjectsFolder, Identifiers.Organizes, folderNodeId.expanded(), true));

		return folderNode;
	}

}
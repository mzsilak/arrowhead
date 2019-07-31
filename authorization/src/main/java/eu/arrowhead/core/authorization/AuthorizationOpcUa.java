package eu.arrowhead.core.authorization;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import eu.arrowhead.common.opcua.ArrowheadOpcUaServer;
import eu.arrowhead.core.authorization.opcua.IsCloudAuthorized;
import eu.arrowhead.core.authorization.opcua.IsSystemAuthorized;
import eu.arrowhead.core.authorization.opcua.TokenGeneration;

public class AuthorizationOpcUa {
	private int namespaceIndex;

	public AuthorizationOpcUa(ArrowheadOpcUaServer server) {
		namespaceIndex = ArrayUtils.indexOf(server.getNodeContext().getNamespaceTable().toArray(),
				"urn:arrowhead:namespace");
		UaFolderNode authorizationFolder = server.addFolder(UShort.valueOf(namespaceIndex), "Authorization");
		UaMethodNode tokenGeneration = server.addMethodNode(UShort.valueOf(namespaceIndex), authorizationFolder,
				"tokenGeneration");
		server.addMethodNodeInNamespace(tokenGeneration, authorizationFolder, new TokenGeneration(tokenGeneration));
		UaMethodNode isSystemAuthorized = server.addMethodNode(UShort.valueOf(namespaceIndex), authorizationFolder,
				"isSystemAuthorized");
		server.addMethodNodeInNamespace(isSystemAuthorized, authorizationFolder,
				new IsSystemAuthorized(isSystemAuthorized));
		UaMethodNode isCloudAuthorized = server.addMethodNode(UShort.valueOf(namespaceIndex), authorizationFolder,
				"isCloudAuthorized");
		server.addMethodNodeInNamespace(isCloudAuthorized, authorizationFolder,
				new IsCloudAuthorized(isSystemAuthorized));
	}

}

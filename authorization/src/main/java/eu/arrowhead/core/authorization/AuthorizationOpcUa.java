package eu.arrowhead.core.authorization;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import eu.arrowhead.common.opcua.ArrowheadOpcUaServer;
import eu.arrowhead.core.authorization.opcua.AddSystemToAuthorized;

public class AuthorizationOpcUa {
	private int namespaceIndex;

	public AuthorizationOpcUa(ArrowheadOpcUaServer server) {
		namespaceIndex = ArrayUtils.indexOf(server.getNodeContext().getNamespaceTable().toArray(),
				"urn:arrowhead:namespace");
		UaFolderNode authorizationFolder = server.addFolder(UShort.valueOf(namespaceIndex), "Authorization");
		UaMethodNode addSystemToAuthorized = server.addMethodNode(UShort.valueOf(namespaceIndex), authorizationFolder,
				"addSystemToAuthorized");
		server.addMethodNodeInNamespace(addSystemToAuthorized, authorizationFolder, new AddSystemToAuthorized(addSystemToAuthorized));
	}
}

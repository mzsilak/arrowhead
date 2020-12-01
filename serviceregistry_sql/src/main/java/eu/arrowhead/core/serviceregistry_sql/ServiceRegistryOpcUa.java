package eu.arrowhead.core.serviceregistry_sql;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import eu.arrowhead.common.opcua.ArrowheadOpcUaServer;
import eu.arrowhead.core.serviceregistry_sql.opcua.QueryMethod;
import eu.arrowhead.core.serviceregistry_sql.opcua.RegisterMethod;
import eu.arrowhead.core.serviceregistry_sql.opcua.RemoveMethod;

public class ServiceRegistryOpcUa {
    private int namespaceIndex;

    public ServiceRegistryOpcUa(ArrowheadOpcUaServer server) {
        namespaceIndex = ArrayUtils.indexOf(server.getNodeContext().getNamespaceTable().toArray(),
                "urn:arrowhead:namespace");
        UaFolderNode srFolder = server.addFolder(UShort.valueOf(namespaceIndex), "ServiceRegistry");
        UaMethodNode register = server.addMethodNode(UShort.valueOf(namespaceIndex), srFolder, "register");
        server.addMethodNodeInNamespace(register, srFolder, new RegisterMethod(register));
        UaMethodNode query = server.addMethodNode(UShort.valueOf(namespaceIndex), srFolder, "query");
        server.addMethodNodeInNamespace(query, srFolder, new QueryMethod(query));
        UaMethodNode remove = server.addMethodNode(UShort.valueOf(namespaceIndex), srFolder, "remove");
        server.addMethodNodeInNamespace(remove, srFolder, new RemoveMethod(remove));
    }

}

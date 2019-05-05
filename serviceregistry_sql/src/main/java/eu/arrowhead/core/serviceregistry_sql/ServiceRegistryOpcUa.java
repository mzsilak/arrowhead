package eu.arrowhead.core.serviceregistry_sql;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.serviceregistry_sql.opcua.Query;
import eu.arrowhead.core.serviceregistry_sql.opcua.Register;
import eu.arrowhead.core.serviceregistry_sql.opcua.Remove;

public class ServiceRegistryOpcUa {
    private UaNodeContext nodeContext;
    private UaNodeManager nodeManager;
    private int namespaceIndex;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    ServiceRegistryOpcUa(UaNodeManager nodeManager, UaNodeContext server) {
        this.nodeContext = server;
        this.nodeManager = nodeManager;
        namespaceIndex = ArrayUtils.indexOf(server.getNamespaceTable().toArray(),
                "urn:arrowhead:service-registry:namespace");
        addRegistryNodes();
    }

    private void addRegistryNodes() {
        NodeId folderNodeId = new NodeId(namespaceIndex, "ServiceRegistry");

        UaFolderNode folderNode = new UaFolderNode(nodeContext, folderNodeId,
                new QualifiedName(namespaceIndex, "ServiceRegistry"), LocalizedText.english("ServiceRegistry"));

        nodeManager.addNode(folderNode);

        folderNode.addReference(
                new Reference(Identifiers.ObjectsFolder, Identifiers.Organizes, folderNodeId.expanded(), true));

        addMethodNode(folderNode);
    }

    private void addMethodNode(UaFolderNode folderNode) {
        UaMethodNode register = UaMethodNode.builder(nodeContext)
                .setNodeId(new NodeId(namespaceIndex, "ServiceRegistry/register"))
                .setBrowseName(new QualifiedName(namespaceIndex, "register"))
                .setDisplayName(new LocalizedText(null, "register")).setDescription(LocalizedText.english("")).build();

        UaMethodNode remove = UaMethodNode.builder(nodeContext)
                .setNodeId(new NodeId(namespaceIndex, "ServiceRegistry/remove"))
                .setBrowseName(new QualifiedName(namespaceIndex, "remove"))
                .setDisplayName(new LocalizedText(null, "remove")).setDescription(LocalizedText.english("")).build();

        UaMethodNode query = UaMethodNode.builder(nodeContext)
                .setNodeId(new NodeId(namespaceIndex, "ServiceRegistry/query"))
                .setBrowseName(new QualifiedName(namespaceIndex, "query"))
                .setDisplayName(new LocalizedText(null, "query")).setDescription(LocalizedText.english("")).build();

        methodNodeInNamespace(register, folderNode, new Register(register));
        methodNodeInNamespace(remove, folderNode, new Remove(remove));
        methodNodeInNamespace(query, folderNode, new Query(query));
    }

    private void methodNodeInNamespace(UaMethodNode methodNode, UaFolderNode folderNode, Object methodClass) {

        // methodNode.setProperty(UaMethodNode.InputArguments,
        // methodNode.getInputArguments());
        // methodNode.setProperty(UaMethodNode.OutputArguments,
        // methodNode.getOutputArguments());
        // methodNode.setInvocationHandler(methodNode);

        nodeManager.addNode(methodNode);

        folderNode.addReference(new Reference(folderNode.getNodeId(), Identifiers.HasComponent,
                methodNode.getNodeId().expanded(), true));

        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent,
                folderNode.getNodeId().expanded(), false));

    }
}

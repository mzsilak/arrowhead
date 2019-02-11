package eu.arrowhead.core.serviceregistry_sql;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.serviceregistry_sql.opcua.Query;
import eu.arrowhead.core.serviceregistry_sql.opcua.Register;
import eu.arrowhead.core.serviceregistry_sql.opcua.Remove;

public class ServiceRegistryOpcUa {
    private OpcUaServer server;
    private int namespaceIndex;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    ServiceRegistryOpcUa(OpcUaServer server) {
        this.server = server;
        namespaceIndex = ArrayUtils.indexOf(server.getNamespaceManager().getNamespaceTable().toArray(),
                "urn:arrowhead:service-registry:namespace");
        addRegistryNodes();
    }

    private void addRegistryNodes() {
        try {
            NodeId folderNodeId = new NodeId(namespaceIndex, "ServiceRegistry");

            UaFolderNode folderNode = new UaFolderNode(server.getNodeMap(), folderNodeId,
                    new QualifiedName(namespaceIndex, "ServiceRegistry"), LocalizedText.english("ServiceRegistry"));

            server.getNodeMap().addNode(folderNode);

            server.getUaNamespace().addReference(Identifiers.ObjectsFolder, Identifiers.Organizes, true,
                    folderNodeId.expanded(), NodeClass.Object);

            addMethodNode(folderNode);
        } catch (UaException e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
    }

    private void addMethodNode(UaFolderNode folderNode) {
        UaMethodNode register = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ServiceRegistry/register"))
                .setBrowseName(new QualifiedName(namespaceIndex, "register"))
                .setDisplayName(new LocalizedText(null, "register")).setDescription(LocalizedText.english("")).build();

        UaMethodNode remove = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ServiceRegistry/remove"))
                .setBrowseName(new QualifiedName(namespaceIndex, "remove"))
                .setDisplayName(new LocalizedText(null, "remove")).setDescription(LocalizedText.english("")).build();

        UaMethodNode query = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ServiceRegistry/query"))
                .setBrowseName(new QualifiedName(namespaceIndex, "query"))
                .setDisplayName(new LocalizedText(null, "query")).setDescription(LocalizedText.english("")).build();

        methodNodeInNamespace(register, folderNode, new Register());
        methodNodeInNamespace(remove, folderNode, new Remove());
        methodNodeInNamespace(query, folderNode, new Query());
    }

    private void methodNodeInNamespace(UaMethodNode methodNode, UaFolderNode folderNode, Object methodClass) {
        try {
            AnnotationBasedInvocationHandler invocationHandler = AnnotationBasedInvocationHandler
                    .fromAnnotatedObject(server.getNodeMap(), methodClass);

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);

            folderNode.addReference(new Reference(folderNode.getNodeId(), Identifiers.HasComponent,
                    methodNode.getNodeId().expanded(), methodNode.getNodeClass(), true));

            methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent,
                    folderNode.getNodeId().expanded(), folderNode.getNodeClass(), false));
        } catch (Exception e) {
            logger.error("Error creating method.", e);
        }
    }
}

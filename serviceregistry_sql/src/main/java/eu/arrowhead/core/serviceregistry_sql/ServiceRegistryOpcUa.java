package eu.arrowhead.core.serviceregistry_sql;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.serviceregistry_sql.opcua.Query;
import eu.arrowhead.core.serviceregistry_sql.opcua.Register;
import eu.arrowhead.core.serviceregistry_sql.opcua.Remove;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

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
        UaMethodNode register = UaMethodNode.builder(nodeContext).setNodeId(new NodeId(namespaceIndex, "register"))
                .setBrowseName(new QualifiedName(namespaceIndex, "register"))
                .setDisplayName(new LocalizedText(null, "register")).setDescription(LocalizedText.english("")).build();

        UaMethodNode remove = UaMethodNode.builder(nodeContext)
                .setNodeId(new NodeId(namespaceIndex, "remove"))
                .setBrowseName(new QualifiedName(namespaceIndex, "remove"))
                .setDisplayName(new LocalizedText(null, "remove")).setDescription(LocalizedText.english("")).build();

        UaMethodNode query = UaMethodNode.builder(nodeContext)
                .setNodeId(new NodeId(namespaceIndex, "query"))
                .setBrowseName(new QualifiedName(namespaceIndex, "query"))
                .setDisplayName(new LocalizedText(null, "query")).setDescription(LocalizedText.english("")).build();

        methodNodeInNamespace(register, folderNode, new Register(register));
        methodNodeInNamespace(remove, folderNode, new Remove(remove));
        methodNodeInNamespace(query, folderNode, new Query(query));
    }

    private void methodNodeInNamespace(UaMethodNode methodNode, UaFolderNode folderNode,
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

//    private void addScalarNodes(UaFolderNode rootNode) {
//        UaFolderNode scalarTypesFolder = new UaFolderNode(nodeContext,
//                new NodeId(namespaceIndex, "HelloWorld/ScalarTypes"), new QualifiedName(namespaceIndex, "ScalarTypes"),
//                LocalizedText.english("ScalarTypes"));
//
//        nodeManager.addNode(scalarTypesFolder);
//        rootNode.addOrganizes(scalarTypesFolder);
//
//        String name = "hw";
//        NodeId typeId = Identifiers.Boolean;
//        Variant variant = new Variant(false);
//
//        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(nodeContext)
//                .setNodeId(new NodeId(namespaceIndex, "HelloWorld/ScalarTypes/" + name))
//                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
//                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
//                .setBrowseName(new QualifiedName(namespaceIndex, name)).setDisplayName(LocalizedText.english(name)).setDataType(typeId)
//                .setTypeDefinition(Identifiers.BaseDataVariableType).build();
//
//        node.setValue(new DataValue(variant));
//
////        node.setAttributeDelegate(new ValueLoggingDelegate());
//
//        nodeManager.addNode(node);
//        scalarTypesFolder.addOrganizes(node);
//    }
}

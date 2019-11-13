package eu.arrowhead.core.orchestrator;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import eu.arrowhead.common.opcua.ArrowheadOpcUaServer;
import eu.arrowhead.core.orchestrator.opcua.DeleteAllMethod;
import eu.arrowhead.core.orchestrator.opcua.OrchestrationMethod;
import eu.arrowhead.core.orchestrator.opcua.StoreMethod;

public class OrchestratorOpcUa {
	private int namespaceIndex;

	public OrchestratorOpcUa(ArrowheadOpcUaServer server) {
		namespaceIndex = ArrayUtils.indexOf(server.getNodeContext().getNamespaceTable().toArray(),
				"urn:arrowhead:namespace");
		UaFolderNode orchestratorFolder = server.addFolder(UShort.valueOf(namespaceIndex), "Orchestrator");
		UaMethodNode store = server.addMethodNode(UShort.valueOf(namespaceIndex), orchestratorFolder, "store");
		server.addMethodNodeInNamespace(store, orchestratorFolder, new StoreMethod(store));
		
		UaMethodNode orchestration = server.addMethodNode(UShort.valueOf(namespaceIndex), orchestratorFolder, "orchestration");
		server.addMethodNodeInNamespace(orchestration, orchestratorFolder, new OrchestrationMethod(orchestration));
		
		UaMethodNode deleteAll = server.addMethodNode(UShort.valueOf(namespaceIndex), orchestratorFolder, "deleteAll");
		server.addMethodNodeInNamespace(deleteAll, orchestratorFolder, new DeleteAllMethod(store));
		
		
	}

}

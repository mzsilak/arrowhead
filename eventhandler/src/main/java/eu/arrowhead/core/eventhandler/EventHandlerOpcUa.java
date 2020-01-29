package eu.arrowhead.core.eventhandler;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import eu.arrowhead.common.opcua.ArrowheadOpcUaServer;
import eu.arrowhead.core.eventhandler.opcua.PublishMethod;
import eu.arrowhead.core.eventhandler.opcua.SusbcribeMethod;
import eu.arrowhead.core.eventhandler.opcua.UnsubscribeMethod;

public class EventHandlerOpcUa {
	private int namespaceIndex;

	public EventHandlerOpcUa(ArrowheadOpcUaServer server) {
		namespaceIndex = ArrayUtils.indexOf(server.getNodeContext().getNamespaceTable().toArray(),
				"urn:arrowhead:namespace");
		UaFolderNode eventHandlerFolder = server.addFolder(UShort.valueOf(namespaceIndex), "EventHandler");
		UaMethodNode subscribeMethod = server.addMethodNode(UShort.valueOf(namespaceIndex), eventHandlerFolder, "subscribe");
		server.addMethodNodeInNamespace(subscribeMethod, eventHandlerFolder, new SusbcribeMethod(subscribeMethod));
		
		UaMethodNode unsubscribeMethod = server.addMethodNode(UShort.valueOf(namespaceIndex), eventHandlerFolder, "unsubscribe");
		server.addMethodNodeInNamespace(unsubscribeMethod, eventHandlerFolder, new UnsubscribeMethod(unsubscribeMethod));
		
		UaMethodNode publishMethod = server.addMethodNode(UShort.valueOf(namespaceIndex), eventHandlerFolder, "publish");
		server.addMethodNodeInNamespace(publishMethod, eventHandlerFolder, new PublishMethod(publishMethod));
	}
	
}

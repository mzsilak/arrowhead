package eu.arrowhead.common.opcua;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.AddressSpaceFilter;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArrowheadOpcUaNamespace extends ManagedNamespace {

    public static final String NAMESPACE_URI = "urn:arrowhead:service-registry:namespace";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionModel subscriptionModel;

    public ArrowheadOpcUaNamespace(OpcUaServer server, String namespaceUri) {
        super(server, namespaceUri);

        subscriptionModel = new SubscriptionModel(server, this);
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public AddressSpaceFilter getFilter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void browse(BrowseContext arg0, ViewDescription arg1, NodeId arg2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getReferences(BrowseContext arg0, ViewDescription arg1, NodeId arg2) {
        // TODO Auto-generated method stub
        
    }
    
    public UaNodeContext getNodeContext() {
        return this.getNodeContext();
    }

    public UaNodeManager getNodeManager() {
        return this.getNodeManager();
    }

}
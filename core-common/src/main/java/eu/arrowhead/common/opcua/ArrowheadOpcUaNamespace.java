/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package eu.arrowhead.common.opcua;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArrowheadOpcUaNamespace extends ManagedNamespace {

    static final String NAMESPACE_URI = "urn:arrowhead:opcua";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionModel subscriptionModel;

    ArrowheadOpcUaNamespace(OpcUaServer server, String namespaceUri) {
        super(server, namespaceUri);

        subscriptionModel = new SubscriptionModel(server, this);
    }

    @Override
    protected void onStartup() {
        super.onStartup();
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

    public UaNodeContext getNamespaceNodeContext() {
        // TODO Auto-generated method stub
        return getNodeContext();
    }

    public UaNodeManager getNamespaceNodeManager() {
        // TODO Auto-generated method stub
        return getNodeManager();
    }

}
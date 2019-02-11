package eu.arrowhead.core.serviceregistry_sql.opcua;
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.common.database.ServiceRegistryEntry;

public class Remove {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(InvocationContext context,

            @UaInputArgument(name = "ServiceRegistryEntry", description = "ServiceRegistryEntry") ServiceRegistryEntry entry) {
        logger.debug("Invoking remove() method of Object '{}'", context.getObjectNode().getBrowseName().getName());
    }
}

package eu.arrowhead.core.serviceregistry_sql.opcua;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.common.messages.ServiceQueryForm;

public class Query {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(InvocationContext context,

            @UaInputArgument(name = "ServiceQueryForm", description = "ServiceQueryForm") ServiceQueryForm queryForm) {
        logger.debug("Invoking register() method of Object '{}'", context.getObjectNode().getBrowseName().getName());
    }
}

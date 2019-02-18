package eu.arrowhead.core.serviceregistry_sql.opcua;

import java.io.IOException;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.arrowhead.common.exception.DuplicateEntryException;
import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class Register extends ServiceRegistryResource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(InvocationContext context,

            @UaInputArgument(name = "ServiceRegistryEntry", description = "ServiceRegistryEntry") String entry,
            @UaOutputArgument(name = "Result", description = "Call result") Out<String> out) throws UaException {
        logger.debug("Invoking register() method of Object '{}'", context.getObjectNode().getBrowseName().getName());
        try {
            try {
                registerGeneric(new ServiceRegistryOpcUaHelper().sreFromJsonString(entry));
                out.set("Success");
            } catch (DuplicateEntryException dee) {
                out.set("DuplicateEntryException");
            }
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

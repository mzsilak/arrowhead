package eu.arrowhead.core.serviceregistry_sql.opcua;
import java.io.IOException;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class Remove extends ServiceRegistryResource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(InvocationContext context,

            @UaInputArgument(name = "ServiceRegistryEntry", description = "ServiceRegistryEntry") String entry) {
        logger.debug("Invoking remove() method of Object '{}'", context.getObjectNode().getBrowseName().getName());
        try {
            removeGeneric(new ServiceRegistryOpcUaHelper().sreFromJsonString(entry));
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

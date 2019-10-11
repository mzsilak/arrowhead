package eu.arrowhead.core.serviceregistry_sql.opcua;

import java.io.IOException;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.arrowhead.common.opcua.OpcUaHelper;
import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class Remove extends AbstractMethodInvocationHandler {
    public Remove(UaMethodNode node) {
        super(node);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final Argument SR_ENTRY = new Argument("sr_entry", Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("ServiceRegistryEntry"));

    @Override
    public Argument[] getInputArguments() {
        return new Argument[] { SR_ENTRY };
    }

    @Override
    public Argument[] getOutputArguments() {
        return null;
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoking remove() method of Object '{}'", invocationContext.getObjectId());
        try {
            new ServiceRegistryResource().removeGeneric(
                    new OpcUaHelper().sreFromJsonString(inputValues[0].getValue().toString()));
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Variant[] { new Variant(null) };
    }
}

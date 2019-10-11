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

import eu.arrowhead.common.exception.DuplicateEntryException;
import eu.arrowhead.common.opcua.OpcUaHelper;
import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class Register extends AbstractMethodInvocationHandler {
    public Register(UaMethodNode node) {
        super(node);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final Argument SR_ENTRY = new Argument("sr_entry", Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("ServiceRegistryEntry"));

    public static final Argument RESULT = new Argument("result", Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Call result"));

    @Override
    public Argument[] getInputArguments() {
        return new Argument[] { SR_ENTRY };
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[] { RESULT };
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        String out = "";
        logger.debug("Invoking register() method of Object '{}'", invocationContext.getObjectId());
        try {
            try {
                new ServiceRegistryResource().registerGeneric(
                        new OpcUaHelper().sreFromJsonString(inputValues[0].getValue().toString()));
                out = "Success";
            } catch (DuplicateEntryException dee) {
                out = "DuplicateEntryException";
            }
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Variant[] { new Variant(out) };
    }
}

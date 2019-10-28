package eu.arrowhead.core.serviceregistry_sql.opcua;

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

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class RegisterMethod extends AbstractMethodInvocationHandler {
    public RegisterMethod(UaMethodNode node) {
        super(node);
    }

    private static final Logger log = LoggerFactory.getLogger(RegisterMethod.class.getName());

    public static final Argument SR_ENTRY = new Argument("sr_entry", Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("ServiceRegistryEntry"));

    @Override
    public Argument[] getInputArguments() {
        return new Argument[] { SR_ENTRY };
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[0];
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		log.debug("Invoking register() method of Object '{}'", invocationContext.getObjectId());

		try {
			new ServiceRegistryResource().registerService(Utility.fromJson(inputValues[0].getValue().toString(), ServiceRegistryEntry.class));	
		} catch (ArrowheadException e) {
			 log.info("The register process found an exception {} ", e);
		}

        return new Variant[0];
    }
}

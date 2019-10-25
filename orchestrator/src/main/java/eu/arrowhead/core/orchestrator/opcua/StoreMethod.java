package eu.arrowhead.core.orchestrator.opcua;

import java.util.Arrays;

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
import eu.arrowhead.common.database.OrchestrationStore;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.core.orchestrator.api.StoreApi;

public class StoreMethod extends AbstractMethodInvocationHandler {
	private static final Logger logger = LoggerFactory.getLogger(StoreMethod.class.getName());

	public StoreMethod(UaMethodNode node) {
		super(node);
	}

	public static final Argument OrchestrationStoreInput = new Argument("OrchestrationStoreInput", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("OrchestrationStoreInput"));

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { OrchestrationStoreInput };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		logger.debug("Invoking store() method of Object '{}'", invocationContext.getObjectId());
		
		try {
			new StoreApi().addStoreEntriesGeneric(Arrays.asList(Utility.fromJson(inputValues[0].getValue().toString(), OrchestrationStore[].class)));
		}catch (ArrowheadException e) {
			logger.debug("Error storing orchestration rule: {}", e);
		}
		return new Variant[0];
	}
}

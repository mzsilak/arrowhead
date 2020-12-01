package eu.arrowhead.core.eventhandler.opcua;

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
import eu.arrowhead.common.database.EventFilter;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.core.eventhandler.EventHandlerResource;

public class UnsubscribeMethod extends AbstractMethodInvocationHandler {
	private static final Logger logger = LoggerFactory.getLogger(UnsubscribeMethod.class.getName());

	public UnsubscribeMethod(UaMethodNode node) {
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
		logger.debug("Invoking unsubcribe() method of Object '{}'", invocationContext.getObjectId());
		
		try {
			new EventHandlerResource().unsubscribe(Utility.fromJson(inputValues[0].getValue().toString(), EventFilter.class));
		}catch (ArrowheadException e) {
			logger.debug("Error unsubcribing: {}", e);
		}
		return new Variant[0];
	}
}

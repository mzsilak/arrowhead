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

public class SusbcribeMethod extends AbstractMethodInvocationHandler {
	private static final Logger logger = LoggerFactory.getLogger(SusbcribeMethod.class.getName());

	public SusbcribeMethod(UaMethodNode node) {
		super(node);
	}

	public static final Argument EventFilter = new Argument("EventFilter", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("OrchestrationStoreInput"));

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { EventFilter };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		logger.debug("Invoking subcribe() method of Object '{}'", invocationContext.getObjectId());
		
		try {
			new EventHandlerResource().subscribe(Utility.fromJson(inputValues[0].getValue().toString(), EventFilter.class));
		}catch (ArrowheadException e) {
			logger.debug("Error subcribing: {}", e);
		}
		return new Variant[0];
	}
}

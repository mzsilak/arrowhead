package eu.arrowhead.core.orchestrator.opcua;

import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.core.orchestrator.api.StoreApi;

public class DeleteAllMethod extends AbstractMethodInvocationHandler {
	private static final Logger logger = LoggerFactory.getLogger(DeleteAllMethod.class.getName());

	public DeleteAllMethod(UaMethodNode node) {
		super(node);
	}

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		logger.debug("Invoking deleteAll() method of Object '{}'", invocationContext.getObjectId());
		
		try {
			new StoreApi().deleteAllEntries();
		}catch (ArrowheadException e) {
			logger.debug("Error storing orchestration rule: {}", e);
		}
		return new Variant[0];
	}
}

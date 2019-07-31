package eu.arrowhead.core.orchestrator.opcua;

import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

public class Orchestration extends AbstractMethodInvocationHandler{

	public Orchestration(UaMethodNode node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Argument[] getInputArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Argument[] getOutputArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		// TODO Auto-generated method stub
		return null;
	}

}

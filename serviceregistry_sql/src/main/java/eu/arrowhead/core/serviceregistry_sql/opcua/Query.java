package eu.arrowhead.core.serviceregistry_sql.opcua;

import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Query extends AbstractMethodInvocationHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Query(UaMethodNode node) {
        super(node);
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

package eu.arrowhead.core.authorization.opcua;

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
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.messages.IntraCloudAuthEntry;
import eu.arrowhead.core.authorization.AuthorizationApi;

public class AddSystemToAuthorized extends AbstractMethodInvocationHandler{
	private static final Logger log = LoggerFactory.getLogger(AddSystemToAuthorized.class.getName());

	public AddSystemToAuthorized(UaMethodNode node) {
		super(node);
	}

	public static final Argument IntraCloudAuthEntry = new Argument("IntraCloudAuthEntry", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("IntraCloudAuthEntry"));

	public static final Argument ResponseStatus = new Argument("ResponseStatus", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("ResponseStatus"));

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { IntraCloudAuthEntry };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[0];
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		log.debug("Invoking mgm/intracloud() method of Object '{}'", invocationContext.getObjectId());

		try {
			new AuthorizationApi().addSystemToAuthorized(Utility.fromJson(inputValues[0].getValue().toString(), IntraCloudAuthEntry.class));	
		} catch (ArrowheadException e) {
			 log.info("The mgm/intracloud process found an exception {} ", e);
		}

        return new Variant[0];
	}
}

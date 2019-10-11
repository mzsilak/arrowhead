package eu.arrowhead.core.authorization.opcua;

import java.io.IOException;

import javax.ws.rs.core.Response;

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
import eu.arrowhead.core.authorization.AuthorizationApi;

public class AddSystemToAuthorized extends AbstractMethodInvocationHandler{
	private final Logger logger = LoggerFactory.getLogger(getClass());

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
		return new Argument[] { ResponseStatus };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		Response out = null;
		logger.debug("Invoking query() method of Object '{}'", invocationContext.getObjectId());
		try {
			out = new AuthorizationApi().addSystemToAuthorizedGeneric(
					new OpcUaHelper().icaeFromJsonString(inputValues[0].getValue().toString()));
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String res = out.getStatusInfo().getReasonPhrase();

		return new Variant[] { new Variant(res) };
	}
}

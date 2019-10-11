package eu.arrowhead.core.serviceregistry_sql.opcua;

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

import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.messages.ServiceQueryResult;
import eu.arrowhead.common.opcua.OpcUaHelper;
import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class Query extends AbstractMethodInvocationHandler {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public Query(UaMethodNode node) {
		super(node);
	}

	public static final Argument SQ_FORM = new Argument("sq_form", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("ServiceQueryForm"));

	public static final Argument SR_ENTRY = new Argument("result", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("ServiceRegistryEntry"));

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { SQ_FORM };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { SR_ENTRY };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		Response out = null;
		logger.debug("Invoking query() method of Object '{}'", invocationContext.getObjectId());
		try {
			out = new ServiceRegistryResource().queryGeneric(
					new OpcUaHelper().sqfFromJsonString(inputValues[0].getValue().toString()));
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String res = "";
		for (ServiceRegistryEntry entry : ((ServiceQueryResult) (out.getEntity())).getServiceQueryData()) {
			res += entry.toString() + " ";
		}

		return new Variant[] { new Variant(res) };
	}
}

package eu.arrowhead.core.serviceregistry_sql.opcua;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.json.JacksonJsonProviderAtRest;
import eu.arrowhead.common.messages.ServiceQueryForm;
import eu.arrowhead.common.messages.ServiceQueryResult;
import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class QueryMethod extends AbstractMethodInvocationHandler {
	private static final Logger log = LoggerFactory.getLogger(QueryMethod.class.getName());

	public QueryMethod(UaMethodNode node) {
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
		Response out =  Response.status(Status.OK).entity(new ServiceQueryResult()).build();
		log.debug("Invoking query() method of Object '{}'", invocationContext.getObjectId());

		try {
			out = new ServiceRegistryResource().queryGeneric(Utility.fromJson(inputValues[0].getValue().toString(), ServiceQueryForm.class));	
		} catch (DataNotFoundException e) {
			 log.info("The orchestration process didn't find a rule. Originally treated as an exception, but not here, because, well, it's not an exception. ");
		}

		try {
			return new Variant[] { new Variant(JacksonJsonProviderAtRest.getMapper().writeValueAsString(out.getEntity())) };
		} catch (JsonProcessingException e) {
			log.error("There was a problem converting the orchestration response into JSON");
		}
		
		return new Variant[] { new Variant("{ \"serviceQueryData\" : []}") };  //default empty response
		
		
		
		
	}
}

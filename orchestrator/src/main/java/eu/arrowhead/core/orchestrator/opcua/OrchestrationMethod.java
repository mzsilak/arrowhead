package eu.arrowhead.core.orchestrator.opcua;

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
import eu.arrowhead.common.messages.ServiceRequestForm;
import eu.arrowhead.core.orchestrator.OrchestratorResource;

public class OrchestrationMethod extends AbstractMethodInvocationHandler {
	private static final Logger log = LoggerFactory.getLogger(OrchestrationMethod.class.getName());

	public OrchestrationMethod(UaMethodNode node) {
		super(node);
	}

	public static final Argument OrchestrationForm = new Argument("OrchestrationForm", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("OrchestrationInput"));

	public static final Argument OrchestrationResponse = new Argument("OrchestrationResponse", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("OrchestrationResponse"));

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { OrchestrationForm };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { OrchestrationResponse };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		Response out =  Response.status(Status.OK).entity(new eu.arrowhead.common.messages.OrchestrationResponse()).build();
		log.debug("Invoking orchestration() method of Object '{}'", invocationContext.getObjectId());

		try {
			out = new OrchestratorResource()
					.orchestrationProcess(Utility.fromJson(inputValues[0].getValue().toString(), ServiceRequestForm.class));	
		} catch (DataNotFoundException e) {
			 log.info("The orchestration process didn't find a rule. Originally treated as an exception, but not here, because, well, it's not an exception. ");
		}

		try {
			return new Variant[] { new Variant(JacksonJsonProviderAtRest.getMapper().writeValueAsString(out.getEntity())) };
		} catch (JsonProcessingException e) {
			log.error("There was a problem converting the orchestration response into JSON");
		}
		
		return new Variant[] { new Variant("{ \"response\" : []}") };  //default empty response
	}
}

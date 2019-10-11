package eu.arrowhead.core.orchestrator.opcua;

import java.io.IOException;
import java.util.List;

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

import eu.arrowhead.common.database.OrchestrationStore;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.messages.ServiceQueryResult;
import eu.arrowhead.common.opcua.OpcUaHelper;
import eu.arrowhead.core.orchestrator.api.StoreApi;

public class Store extends AbstractMethodInvocationHandler {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public Store(UaMethodNode node) {
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
		logger.debug("Invoking query() method of Object '{}'", invocationContext.getObjectId());
		try {
			new StoreApi().addStoreEntriesGeneric(
					new OpcUaHelper().orchestrationStoreListFromJsonString(inputValues[0].getValue().toString()));
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}

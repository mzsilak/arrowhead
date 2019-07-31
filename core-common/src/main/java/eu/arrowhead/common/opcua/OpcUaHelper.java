package eu.arrowhead.common.opcua;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.database.ArrowheadCloud;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.OrchestrationStore;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.messages.IntraCloudAuthEntry;
import eu.arrowhead.common.messages.ServiceQueryForm;

public class OpcUaHelper {
	public ServiceRegistryEntry sreFromJsonString(String json)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(json);
		List<String> interfaces = mapper.readValue(actualObj.get("providedService").get("interfaces").toString(),
				new TypeReference<List<String>>() {
				});
		Set<String> interfaceSet = new HashSet<String>();
		for (String s : interfaces) {
			interfaceSet.add(s);
		}
		TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
		};
		Map<String, String> metadata = mapper
				.readValue(actualObj.get("providedService").get("serviceMetadata").toString(), typeRef);
		ArrowheadService ahService = new ArrowheadService(
				actualObj.get("providedService").get("serviceDefinition").toString().replaceAll("\"", ""), interfaceSet,
				metadata);
		String systemName = actualObj.get("provider").get("systemName").toString().replaceAll("\"", "");
		String address = actualObj.get("provider").get("address").toString().replaceAll("\"", "");
		Integer port = actualObj.get("provider").get("port").asInt();
		String authenticationInfo = actualObj.get("provider").get("authenticationInfo").toString().replaceAll("\"", "");
		ArrowheadSystem ahSystem = new ArrowheadSystem(systemName, address, port, authenticationInfo);

		ServiceRegistryEntry sre = new ServiceRegistryEntry(ahService, ahSystem,
				actualObj.get("serviceURI").toString().replaceAll("\"", ""));
		return sre;
	}

	public ServiceQueryForm sqfFromJsonString(String json)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(json);
		List<String> interfaces = mapper.readValue(actualObj.get("service").get("interfaces").toString(),
				new TypeReference<List<String>>() {
				});
		Set<String> interfaceSet = new HashSet<String>();
		for (String s : interfaces) {
			interfaceSet.add(s);
		}
		TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
		};
		Map<String, String> metadata = mapper.readValue(actualObj.get("service").get("serviceMetadata").toString(),
				typeRef);
		ArrowheadService ahService = new ArrowheadService(
				actualObj.get("service").get("serviceDefinition").toString().replaceAll("\"", ""), interfaceSet,
				metadata);
		Boolean metadataSearch = actualObj.get("metadataSearch").asBoolean();
		Boolean pingProviders = actualObj.get("pingProviders").asBoolean();

		ServiceQueryForm sqf = new ServiceQueryForm(ahService, metadataSearch, pingProviders);
		return sqf;
	}

	public List<OrchestrationStore> orchestrationStoreListFromJsonString(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(json);

		List<OrchestrationStore> orchestrationStoresList = new ArrayList<OrchestrationStore>();
		for (JsonNode node : actualObj) {
			String serviceDefinition = node.get("service").get("serviceDefinition").toString().replaceAll("\"", "");
			List<String> interfaces = mapper.readValue(node.get("service").get("interfaces").toString(),
					new TypeReference<List<String>>() {
					});
			Set<String> interfaceSet = new HashSet<String>();
			for (String s : interfaces) {
				interfaceSet.add(s);
			}
			TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
			};
			Map<String, String> metadata = mapper.readValue(node.get("service").get("serviceMetadata").toString(),
					typeRef);
			ArrowheadService service = new ArrowheadService(serviceDefinition, interfaceSet, metadata);

			String consumerSystemName = node.get("consumer").get("systemName").toString().replaceAll("\"", "");
			String consumerAddress = node.get("consumer").get("address").toString().replaceAll("\"", "");
			Integer consumerPort = node.get("consumer").get("port").asInt();
			String consumerAuthenticationInfo = node.get("consumer").get("authenticationInfo") == null ? ""
					: node.get("consumer").get("authenticationInfo").toString().replaceAll("\"", "");
			ArrowheadSystem consumer = new ArrowheadSystem(consumerSystemName, consumerAddress, consumerPort,
					consumerAuthenticationInfo);

			String providerSystemName = node.get("providerSystem").get("systemName").toString().replaceAll("\"", "");
			String providerAddress = node.get("providerSystem").get("address").toString().replaceAll("\"", "");
			Integer providerPort = node.get("providerSystem").get("port").asInt();
			String providerAuthenticationInfo = node.get("providerSystem").get("authenticationInfo") == null ? ""
					: node.get("providerSystem").get("authenticationInfo").toString().replaceAll("\"", "");
			ArrowheadSystem providerSystem = new ArrowheadSystem(providerSystemName, providerAddress, providerPort,
					providerAuthenticationInfo);

			int priority = node.get("priority").asInt();

			boolean defaultEntry = node.get("defaultEntry").asBoolean();

			String name = node.get("name").textValue();

			OrchestrationStore os = new OrchestrationStore(service, consumer, providerSystem, null, priority,
					defaultEntry, name, null, null, null, null);
			orchestrationStoresList.add(os);
		}
		return orchestrationStoresList;
	}

	public IntraCloudAuthEntry icaeFromJsonString(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(json);

		String systemName = actualObj.get("consumer").get("systemName").toString().replaceAll("\"", "");
		String address = actualObj.get("consumer").get("address").toString().replaceAll("\"", "");
		Integer port = actualObj.get("consumer").get("port").asInt();
		String authenticationInfo = actualObj.get("consumer").get("authenticationInfo") == null ? ""
				: actualObj.get("consumer").get("authenticationInfo").toString().replaceAll("\"", "");
		ArrowheadSystem consumer = new ArrowheadSystem(systemName, address, port, authenticationInfo);

		List<ArrowheadSystem> providersList = new ArrayList<ArrowheadSystem>();
		for (JsonNode node : actualObj.get("providerList")) {
			systemName = node.get("systemName").toString().replaceAll("\"", "");
			address = node.get("address").toString().replaceAll("\"", "");
			port = node.get("port").asInt();
			authenticationInfo = node.get("authenticationInfo") == null ? ""
					: node.get("authenticationInfo").toString().replaceAll("\"", "");
			providersList.add(new ArrowheadSystem(systemName, address, port, authenticationInfo));
		}

		List<ArrowheadService> servicesList = new ArrayList<ArrowheadService>();
		for (JsonNode node : actualObj.get("serviceList")) {
			String serviceDefinition = node.get("serviceDefinition").toString().replaceAll("\"", "");
			servicesList.add(new ArrowheadService(serviceDefinition, null, null));
		}
		IntraCloudAuthEntry icae = new IntraCloudAuthEntry(consumer, providersList, servicesList);
		return icae;
	}
}

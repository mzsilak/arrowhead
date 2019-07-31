package eu.arrowhead.core.serviceregistry_sql.opcua;

import java.io.IOException;
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

import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.messages.ServiceQueryForm;

public class ServiceRegistryOpcUaHelper {
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
        Map<String, String> metadata = mapper
                .readValue(actualObj.get("service").get("serviceMetadata").toString(), typeRef);
        ArrowheadService ahService = new ArrowheadService(
                actualObj.get("service").get("serviceDefinition").toString().replaceAll("\"", ""), interfaceSet,
                metadata);
        Boolean metadataSearch = actualObj.get("metadataSearch").asBoolean();
        Boolean pingProviders = actualObj.get("pingProviders").asBoolean();
        
        ServiceQueryForm sqf = new ServiceQueryForm(ahService, metadataSearch, pingProviders);
        return sqf;
    }
}

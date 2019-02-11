package eu.arrowhead.core.serviceregistry_sql.opcua;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.core.serviceregistry_sql.ServiceRegistryResource;

public class Register extends ServiceRegistryResource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(InvocationContext context,

            @UaInputArgument(name = "ServiceRegistryEntry", description = "ServiceRegistryEntry") String entry) {
        logger.debug("Invoking register() method of Object '{}'", context.getObjectNode().getBrowseName().getName());
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode actualObj = mapper.readTree(entry);
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
                    actualObj.get("providedService").get("serviceDefinition").toString().replaceAll("\"", ""), interfaceSet, metadata);
            
            //"provider":{"systemName":"systemTestName","address":"localhost","port":8090,"authenticationInfo":""}
            String systemName = actualObj.get("provider").get("systemName").toString().replaceAll("\"", "");
            String address = actualObj.get("provider").get("address").toString().replaceAll("\"", "");
            Integer port = actualObj.get("provider").get("port").asInt();
            String authenticationInfo = actualObj.get("provider").get("authenticationInfo").toString().replaceAll("\"", "");
            ArrowheadSystem ahSystem = new ArrowheadSystem(systemName, address, port, authenticationInfo);
            
            ServiceRegistryEntry sre = new ServiceRegistryEntry(ahService, ahSystem, actualObj.get("serviceURI").toString().replaceAll("\"", ""));
            registerGeneric(sre);
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // ServiceRegistryEntry sre = new ServiceRegistryEntry(as,
        // actualObj.get("provider"), actualObj.get("serviceURI");
        // registerGeneric(sre);
    }
}

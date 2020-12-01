/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.orchestrator.api;

import eu.arrowhead.common.DatabaseManager;
import eu.arrowhead.common.database.ArrowheadCloud;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.OrchestrationStore;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.messages.OrchestrationStorePriorities;
import eu.arrowhead.common.messages.OrchestrationStoreQuery;
import eu.arrowhead.core.orchestrator.StoreService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("orchestrator/mgmt/store")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StoreApi {

  private final HashMap<String, Object> restrictionMap = new HashMap<>();
  private static final Logger log = LogManager.getLogger(StoreApi.class.getName());
  private static final DatabaseManager dm = DatabaseManager.getInstance();

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getIt() {
    return "orchestrator/mgmt/store got it";
  }

  /**
   * Returns an Orchestration Store entry from the database specified by the database generated id.
   *
   * @return OrchestrationStore
   */
  @GET
  @Path("{id}")
  public Response getStoreEntry(@PathParam("id") long id) {

    restrictionMap.put("id", id);
    OrchestrationStore entry = dm.get(OrchestrationStore.class, restrictionMap);
    if (entry == null) {
      log.info("getStoreEntry throws DataNotFoundException");
      throw new DataNotFoundException("Requested store entry was not found in the database.");
    } else {
      log.info("getStoreEntry returns a store entry.");
      return Response.ok(entry).build();
    }
  }

  /**
   * Returns all the entries of the Orchestration Store.
   *
   * @return List<OrchestrationStore>
   */
  @GET
  @Path("all")
  public List<OrchestrationStore> getAllStoreEntries() {

    List<OrchestrationStore> store = dm.getAll(OrchestrationStore.class, restrictionMap);
    if (store.isEmpty()) {
      log.info("getAllStoreEntries throws DataNotFoundException.");
      throw new DataNotFoundException("The Orchestration Store is empty.");
    }

    Collections.sort(store);
    log.info("getAllStoreEntries successfully returns.");
    return store;
  }

  /**
   * Returns all the default entries of the Orchestration Store.
   *
   * @return List<OrchestrationStore>
   */
  @GET
  @Path("all_default")
  public List<OrchestrationStore> getDefaultStoreEntries() {
    restrictionMap.put("defaultEntry", true);
    List<OrchestrationStore> store = dm.getAll(OrchestrationStore.class, restrictionMap);
    if (store.isEmpty()) {
      log.info("getDefaultStoreEntries throws DataNotFoundException.");
      throw new DataNotFoundException("Default Orchestration Store entries were not found.");
    }

    Collections.sort(store);
    log.info("getDefaultStoreEntries successfully returns.");
    return store;
  }

  /**
   * Returns the Orchestration Store entries from the database specified by the consumer (and the service).
   *
   * @return List<OrchestrationStore>
   *
   * @throws BadPayloadException, DataNotFoundException
   */
  @PUT
  public Response getStoreEntries(@Valid OrchestrationStoreQuery query) {
    List<OrchestrationStore> store;
    if (query.getRequestedService() == null) {
      store = StoreService.getDefaultStoreEntries(query.getRequesterSystem());
    } else if (query.getRequesterSystem() == null) {
      store = StoreService.getStoreEntries(query.getRequestedService());
    } else {
      store = StoreService.getStoreEntries(query.getRequesterSystem(), query.getRequestedService());
    }

    Collections.sort(store);
    GenericEntity<List<OrchestrationStore>> entity = new GenericEntity<List<OrchestrationStore>>(store) {
    };
    log.info("getStoreEntries successfully returns.");
    return Response.ok(entity).build();
  }

  /**
   * Adds a list of Orchestration Store entries to the database. Elements which would throw BadPayloadException are
   * being skipped. The returned list
   * only contains the elements which were saved in the process.
   *
   * @return List<OrchestrationStore>
   */

  @POST
  public List<OrchestrationStore> addStoreEntries(@Valid List<OrchestrationStore> storeEntries) {
	  List<OrchestrationStore> store = new ArrayList<>();
	    for (OrchestrationStore entry : storeEntries) {
	      entry.validateCrossParameterConstraints();
	      restrictionMap.clear();
	      restrictionMap.put("systemName", entry.getConsumer().getSystemName());
	      restrictionMap.put("address", entry.getConsumer().getAddress());
	      restrictionMap.put("port", entry.getConsumer().getPort());
	      ArrowheadSystem consumer = dm.get(ArrowheadSystem.class, restrictionMap);
	      if (consumer == null) {
	        consumer = dm.save(entry.getConsumer());
	      }

	      restrictionMap.clear();
	      restrictionMap.put("serviceDefinition", entry.getService().getServiceDefinition());
	      ArrowheadService service = dm.get(ArrowheadService.class, restrictionMap);
	      if (service == null) {
	        service = dm.save(entry.getService());
	      }

	      restrictionMap.clear();
	      restrictionMap.put("systemName", entry.getProviderSystem().getSystemName());
	      restrictionMap.put("address", entry.getProviderSystem().getAddress());
	      restrictionMap.put("port", entry.getProviderSystem().getPort());
	      ArrowheadSystem providerSystem = dm.get(ArrowheadSystem.class, restrictionMap);
	      if (providerSystem == null) {
	        providerSystem = dm.save(entry.getProviderSystem());
	      }

	      ArrowheadCloud providerCloud = null;
	      if (entry.getProviderCloud() != null) {
	        restrictionMap.clear();
	        restrictionMap.put("operator", entry.getProviderCloud().getOperator());
	        restrictionMap.put("cloudName", entry.getProviderCloud().getCloudName());
	        providerCloud = dm.get(ArrowheadCloud.class, restrictionMap);
	        if (providerCloud == null) {
	          providerCloud = dm.save(entry.getProviderCloud());
	        }
	      }

	      restrictionMap.clear();
	      restrictionMap.put("consumer", consumer);
	      restrictionMap.put("service", service);
	      restrictionMap.put("priority", entry.getPriority());
	      restrictionMap.put("defaultEntry", entry.isDefaultEntry());
	      OrchestrationStore storeEntry = dm.get(OrchestrationStore.class, restrictionMap);
	      if (storeEntry == null) {
	        // Merge the service metadata map to the store attributes map, duplicate keys are handled with concatenated
	        // values
	        entry.getService().getServiceMetadata()
	             .forEach((k, v) -> entry.getAttributes().merge(k, v, (v1, v2) -> String.join(", ", v1, v2)));
	        // Create the new Store Entry with the transactional objects
	        storeEntry = new OrchestrationStore(service, consumer, providerSystem, providerCloud, entry.getPriority(),
	                                            entry.isDefaultEntry(), entry.getName(), LocalDateTime.now(),
	                                            entry.getInstruction(), entry.getAttributes(), null);
	        storeEntry = dm.save(storeEntry);
	        store.add(storeEntry);
	      }
	    }

	    log.info("addStoreEntries successfully returns. List size: " + store.size());
	    return store;
  }
  
  /**
   * Toggles the <tt>defaultEntry</tt> boolean for the Orchestration Store entry specified by the id field.
   *
   * @return OrchestrationStore
   *
   * @throws DataNotFoundException, BadPayloadException
   */
  @GET
  @Path("default/{id}")
  public Response toggleIsDefault(@PathParam("id") long id) {

    restrictionMap.put("id", id);
    OrchestrationStore entry = dm.get(OrchestrationStore.class, restrictionMap);
    if (entry == null) {
      log.info("toggleIsDefault throws DataNotFoundException.");
      throw new DataNotFoundException("Orchestration Store entry with this id was not found in the database.");
    } else if (entry.getProviderCloud() != null && !entry.isDefaultEntry()) {
      log.info("toggleIsDefault throws BadPayloadException.");
      throw new BadPayloadException("Only intra-cloud store entries can be set as default entries.");
    } else {
      entry.setDefaultEntry(!entry.isDefaultEntry());
      dm.merge(entry);
      log.info("toggleIsDefault successfully returns.");
      return Response.ok(entry).build();
    }
  }

  /**
   * @return OrchestrationStore
   *
   * @throws BadPayloadException, DataNotFoundException
   */
  @PUT
  @Path("update/{id}")
  public Response updateStoreEntry(@PathParam("id") long id, @Valid OrchestrationStore updatedEntry) {
    updatedEntry.validateCrossParameterConstraints();
    OrchestrationStore entry = dm.get(OrchestrationStore.class, id).orElseThrow(
        () -> new DataNotFoundException("OrchestrationStore entry not found with id: " + id));
    entry.updateEntryWith(updatedEntry);
    entry = dm.merge(entry);
    log.info("updateStoreEntry successfully returns.");
    return Response.ok().entity(entry).build();
  }

  /**
   * Deletes the Orchestration Store entry with the id specified by the path parameter. Returns 200 if the delete is
   * successful, 204 (no content) if
   * the entry was not in the database to begin with.
   */
  @DELETE
  @Path("{id}")
  public Response deleteEntry(@PathParam("id") long id) {
    return dm.get(OrchestrationStore.class, id).map(entry -> {
      dm.delete(entry);
      log.info("deleteStoreEntry successfully returns.");
      return Response.ok().build();
    }).<DataNotFoundException>orElseThrow(() -> {
      log.info("deleteStoreEntry had no effect.");
      throw new DataNotFoundException("OrchestrationStore entry not found with id: " + id);
    });
  }
  
  /**
   * Deletes all Orchestration Store entries. Returns always OK
   */
  @DELETE
  @Path("all")
  public Response deleteAllEntries() {
	  List<OrchestrationStore> allStore = getAllStoreEntries();
	  for(OrchestrationStore current : allStore) {
		  deleteEntry(current.getId().longValue());
	  }
	  return Response.ok().build();
  }

  /**
   * Deletes the Orchestration Store entries from the database specified by the consumer. Returns 200 if the delete
   * is successful, 204 (no content) if
   * no matching entries were in the database to begin with.
   */
  @DELETE
  @Path("consumerId/{systemId}")
  public Response deleteEntries(@PathParam("systemId") long systemId) {
    restrictionMap.put("id", systemId);
    ArrowheadSystem consumer = dm.get(ArrowheadSystem.class, restrictionMap);
    if (consumer == null) {
      return Response.noContent().build();
    }

    restrictionMap.clear();
    restrictionMap.put("consumer", consumer);
    List<OrchestrationStore> store = dm.getAll(OrchestrationStore.class, restrictionMap);
    if (store.isEmpty()) {
      log.info("deleteEntries had no effect.");
      return Response.noContent().build();
    } else {
      for (OrchestrationStore entry : store) {
        dm.delete(entry);
      }

      log.info("deleteEntries successfully returns.");
      return Response.ok().build();
    }
  }

  @PUT
  @Path("priorities")
  public Response updatePriorities(@Valid OrchestrationStorePriorities prioritiesMap) {
    Set<Long> IDs = prioritiesMap.getPriorities().keySet();
    List<OrchestrationStore> storeList = dm.get(OrchestrationStore.class, IDs);
    dm.delete(storeList.toArray());

    for (OrchestrationStore entry : storeList) {
      int newPriority = prioritiesMap.getPriorities().get(entry.getId());
      entry.setPriority(newPriority);
    }
    dm.save(storeList.toArray());
    return Response.ok().entity(storeList).build();
  }

}

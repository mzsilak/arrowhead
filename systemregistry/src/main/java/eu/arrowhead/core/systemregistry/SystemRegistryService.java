/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.systemregistry;

import eu.arrowhead.common.DatabaseManager;
import eu.arrowhead.common.database.ArrowheadDevice;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.SystemRegistryEntry;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.misc.registry_interfaces.RegistryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response.Status;

public class SystemRegistryService implements RegistryService<SystemRegistryEntry> {

	private final DatabaseManager databaseManager;

  public SystemRegistryService() {
		databaseManager = DatabaseManager.getInstance();
	}

	public SystemRegistryEntry lookup(final long id) throws EntityNotFoundException, ArrowheadException {
		final SystemRegistryEntry returnValue;

		try {
			Optional<SystemRegistryEntry> optional = databaseManager.get(SystemRegistryEntry.class, id);
			returnValue = optional.orElseThrow(() -> {
				return new DataNotFoundException("The requested entity does not exist", Status.NOT_FOUND.getStatusCode());
			});
		} catch (final ArrowheadException e) {
			throw e;
		} catch (Exception e) {
			throw new ArrowheadException(e.getMessage(), Status.NOT_FOUND.getStatusCode(), e);
		}

		return returnValue;
	}

	public SystemRegistryEntry publish(final SystemRegistryEntry entity) throws ArrowheadException {
		final SystemRegistryEntry returnValue;

		try {
			entity.setProvidedSystem(resolve(entity.getProvidedSystem()));
			entity.setProvider(resolve(entity.getProvider()));
			returnValue = databaseManager.save(entity);
		} catch (final ArrowheadException e) {
			throw e;
		} catch (Exception e) {
			throw new ArrowheadException(e.getMessage(), Status.NOT_FOUND.getStatusCode(), e);
		}

		return returnValue;
	}

	public SystemRegistryEntry unpublish(final SystemRegistryEntry entity) throws EntityNotFoundException, ArrowheadException {
		final SystemRegistryEntry returnValue;

		try {
			if(entity.getProvidedSystem().getId() == null)
			{
				final Map<String, Object> restrictions = new HashMap<>();
				ArrowheadSystem system = entity.getProvidedSystem();
				restrictions.put("systemName", system.getSystemName());
				restrictions.put("address", system.getAddress());
				restrictions.put("port", system.getPort());

				List<ArrowheadSystem> systemList = databaseManager.getAll(ArrowheadSystem.class, restrictions);

				for(ArrowheadSystem sys : systemList)
				{
					databaseManager.delete(sys);
				}
			}
			else
			{
				databaseManager.delete(entity.getProvidedSystem());
			}
			databaseManager.delete(entity);
			returnValue = entity;
		} catch (final ArrowheadException e) {
			throw e;
		} catch (Exception e) {
			throw new ArrowheadException(e.getMessage(), Status.NOT_FOUND.getStatusCode(), e);
		}
		return returnValue;
	}

	protected ArrowheadSystem resolve(final ArrowheadSystem providedSystem) {
		final ArrowheadSystem returnValue;

		if (providedSystem.getId() != null) {
			Optional<ArrowheadSystem> optional = databaseManager.get(ArrowheadSystem.class, providedSystem.getId());
			if(optional.isPresent())
			{
				returnValue = optional.get();
			}
			else
			{
				returnValue = databaseManager.save(providedSystem);
			}

		} else {
			returnValue = databaseManager.save(providedSystem);
		}

		return returnValue;
	}

	protected ArrowheadDevice resolve(final ArrowheadDevice provider) {
		final ArrowheadDevice returnValue;

		if (provider.getId() != null) {
			Optional<ArrowheadDevice> optional = databaseManager.get(ArrowheadDevice.class, provider.getId());
			if(optional.isPresent())
			{
				returnValue = optional.get();
			}
			else
			{
				returnValue = databaseManager.save(provider);
			}
		} else {
			returnValue = databaseManager.save(provider);
		}

		return returnValue;
	}
}

/*
 * Copyright (c) 2011, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.dnssdjava;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Unicast {@link DnsSDBrowser} implementation backed by dnsjava.
 *
 * @author Daniel Nilsson
 */
class UnicastDnsSDBrowser implements DnsSDBrowser {


  private static final Logger logger = Logger.getLogger(UnicastDnsSDBrowser.class.getName());

  private static final Name SERVICES_DNSSD_UDP = Name.fromConstantString("_services._dns-sd._udp");

  private final List<Name> browserDomains;

  /**
   * Create a new UnicastDnsSDBrowser.
   *
   * @param browserDomains list of domain names to browse.
   */
  UnicastDnsSDBrowser(List<Name> browserDomains) {
    this.browserDomains = browserDomains;
    logger.log(Level.INFO, "Created DNS-SD Browser for domains: {0}", browserDomains);
  }

  //@Override

  public ServiceData getServiceData(ServiceName service) {
    Name serviceName = service.toDnsName();
    Lookup lookup = new Lookup(serviceName, Type.SRV);
    Record[] records = lookup.run();
    if (records == null || records.length == 0) {
      return null;
    }
    ServiceData data = new ServiceData();
    data.setName(service);
    for (Record record : records) {
      if (record instanceof SRVRecord) {
        //
        SRVRecord srv = (SRVRecord) record;
        data.setHost(srv.getTarget().toString());
        data.setPort(srv.getPort());
        break;
      }
    }
    lookup = new Lookup(serviceName, Type.TXT);
    records = lookup.run();
    if (records == null || records.length == 0) {
      return data;
    }
    for (Record record : records) {
      if (record instanceof TXTRecord) {
        //  Handle multiple TXT records as different variants of same service
        TXTRecord txt = (TXTRecord) record;
        for (Object o : txt.getStrings()) {
          String string = (String) o;     // Safe cast
          int i = string.indexOf('=');
          String key;
          String value;
          if (i == 0 || string.isEmpty()) {
            continue;   // Invalid empty key, should be ignored
          } else if (i > 0) {
            key = string.substring(0, i).toLowerCase();
            value = string.substring(i + 1);
          } else {
            key = string;
            value = null;
          }
          if (!data.getProperties().containsKey(key)) {   // Ignore all but the first
            data.getProperties().put(key, value);
          }
        }
        break;
      }
    }
    return data;
  }

  //@Override

  public Collection<ServiceName> getServiceInstances(ServiceType type) {
    List<ServiceName> results = new ArrayList<>();
    for (Name domain : browserDomains) {
      results.addAll(getServiceInstances(type, domain));
    }
    return results;
  }


  public Collection<ServiceType> getServiceTypes() {
    Set<ServiceType> results = new HashSet<>();
    for (Name domain : browserDomains) {
      results.addAll(getServiceTypes(domain));
    }
    return results;
  }

  /**
   * Get the service types from a single domain.
   *
   * @param domainName the domain to browse.
   *
   * @return a list of service types.
   */

  private List<ServiceType> getServiceTypes(Name domainName) {
    try {
      List<ServiceType> results = new ArrayList<>();
      Lookup lookup = new Lookup(Name.concatenate(SERVICES_DNSSD_UDP, domainName), Type.PTR);
      Record[] records = lookup.run();
      if (records != null) {
        for (Record record : records) {
          if (record instanceof PTRRecord) {
            PTRRecord ptr = (PTRRecord) record;
            Name name = ptr.getTarget();
            try {
              String type = name.getLabelString(0);
              String transport = name.getLabelString(1);
              results.add(new ServiceType(type, transport));
            } catch (IllegalArgumentException e) {
              logger.warning("Invalid service type " + name + ": " + e.getMessage());
            }
          }
        }
      }
      return results;
    } catch (NameTooLongException ex) {
      throw new IllegalArgumentException("Too long name: " + domainName, ex);
    }
  }

  /**
   * Get all service names of a service type in a single domain. If the specified type has subtypes then only instances registered under any of those
   * are returned.
   *
   * @param type the service type.
   * @param domainName the domain to browse.
   *
   * @return a list of service names.
   */

  private List<ServiceName> getServiceInstances(ServiceType type, Name domainName) {
    if (type.getSubtypes().isEmpty()) {
      List<ServiceName> results = new ArrayList<>();
      getServiceInstances(type.toDnsString(), domainName, results);
      return results;
    } else {
      Set<ServiceName> results = new HashSet<>();
      for (String subtype : type.toDnsStringsWithSubtype()) {
        getServiceInstances(subtype, domainName, results);
      }
      return new ArrayList<>(results);
    }
  }

  /**
   * Get all service names of a specific type in a single domain.
   *
   * @param type the service type as a string, including transport and subtype (if any).
   * @param domainName the domain to browse.
   * @param results a collection to put found service names into.
   */
  private void getServiceInstances(String type, Name domainName, Collection<ServiceName> results) {
    try {
      Name typeDomainName = Name.fromString(type, domainName);
      Lookup lookup = new Lookup(typeDomainName, Type.PTR);
      Record[] records = lookup.run();
      if (records != null) {
        for (Record record : records) {
          if (record instanceof PTRRecord) {
            PTRRecord ptr = (PTRRecord) record;
            Name name = ptr.getTarget();
            try {
              results.add(ServiceName.fromDnsName(name));
            } catch (IllegalArgumentException e) {
              logger.warning("Invalid service instance " + name + ": " + e.getMessage());
            }
          }
        }
      }
    } catch (TextParseException ex) {
      throw new IllegalArgumentException("Invalid type: " + type, ex);
    }
  }

}

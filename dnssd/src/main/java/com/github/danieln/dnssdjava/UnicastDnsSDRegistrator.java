/*
 * Copyright (c) 2011, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.dnssdjava;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbill.DNS.Address;
import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.Update;


/**
 * Unicast {@link DnsSDRegistrator} implementation backed by dnsjava.
 *
 * @author Daniel Nilsson
 */
class UnicastDnsSDRegistrator implements DnsSDRegistrator {

  private static final Logger logger = Logger.getLogger(UnicastDnsSDBrowser.class.getName());

  private static final Name DNSUPDATE_UDP = Name.fromConstantString("_dns-update._udp");
  private static final Name SERVICES_DNSSD_UDP = Name.fromConstantString("_services._dns-sd._udp");

  private final Name registrationDomain;

  private final Resolver resolver;
  private final Name servicesName;

  private int timeToLive = 60;
  private String localHostname;

  UnicastDnsSDRegistrator(Name registrationDomain, InetSocketAddress resolverSocaddr) throws UnknownHostException {
    try {
      this.registrationDomain = registrationDomain;
      this.resolver = setDirectResolver(resolverSocaddr);
      this.servicesName = Name.concatenate(SERVICES_DNSSD_UDP, registrationDomain);
      logger.log(Level.INFO, "Created DNS-SD Registrator for domain {0}", registrationDomain);
    } catch (NameTooLongException e) {
      throw new IllegalArgumentException("Domain name too long: " + registrationDomain, e);
    }

  }

  private Resolver setDirectResolver(InetSocketAddress socaddr) {
    SimpleResolver simpleResolver;

    try {
      simpleResolver = new SimpleResolver();
      simpleResolver.setAddress(socaddr);
      return simpleResolver;
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

    return null;

  }

  /**
   * Create a UnicastDnsSDRegistrator.
   *
   * @param registrationDomain the registration domain.
   *
   * @throws UnknownHostException if the DNS server name for the domain failed to resolve.
   */
  UnicastDnsSDRegistrator(Name registrationDomain) throws UnknownHostException {
    try {
      this.registrationDomain = registrationDomain;
      this.resolver = findUpdateResolver(registrationDomain);
      this.servicesName = Name.concatenate(SERVICES_DNSSD_UDP, registrationDomain);
      logger.log(Level.INFO, "Created DNS-SD Registrator for domain {0}", registrationDomain);
    } catch (NameTooLongException e) {
      throw new IllegalArgumentException("Domain name too long: " + registrationDomain, e);
    }
  }

  /**
   * Create a DNS {@link Resolver} to handle updates to the given domain.
   *
   * @param domain the domain for which updates will be generated.
   *
   * @return a Resolver configured with the DNS server that handles zone for that domain.
   *
   * @throws UnknownHostException if the DNS server name for the domain failed to resolve.
   */

  private Resolver findUpdateResolver(Name domain) throws UnknownHostException {
    SimpleResolver simpleResolver = null;
    try {
      Lookup lookup = new Lookup(Name.concatenate(DNSUPDATE_UDP, domain), Type.SRV);
      Record[] records = lookup.run();
      if (records != null) {
        for (Record record : records) {
          if (record instanceof SRVRecord) {
            SRVRecord srv = (SRVRecord) record;
            simpleResolver = new SimpleResolver();
            InetAddress addr = Address.getByName(srv.getTarget().toString());
            InetSocketAddress socaddr = new InetSocketAddress(addr, srv.getPort());
            logger.log(Level.INFO, "Using DNS server {0} to perform updates.", socaddr);
            simpleResolver.setAddress(socaddr);
            break;
          }
        }
      }
    } catch (NameTooLongException e) {
      logger.log(Level.WARNING, "Failed to lookup update DNS server", e);
    }
    if (simpleResolver == null) {
      simpleResolver = new SimpleResolver();
    }
    return simpleResolver;
  }


  public ServiceName makeServiceName(String name, ServiceType type) {
    return new ServiceName(name, type, registrationDomain.toString());
  }

  public String getLocalHostName() throws UnknownHostException {
    if (localHostname == null) {
      List<String> names = new ArrayList<>();
      names.addAll(DomainUtil.getComputerHostNames());
      names.add(InetAddress.getLocalHost().getCanonicalHostName());
      names.add(InetAddress.getLocalHost().getHostName());
      for (String name : names) {
        if (!name.startsWith("localhost") && !name.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
          localHostname = name.endsWith(".") ? name : (name + ".");
          break;
        }
      }
    }
    if (localHostname == null) {
      throw new UnknownHostException();
    }
    return localHostname;
  }

  public int getTimeToLive() {
    return timeToLive;
  }

  public void setTimeToLive(int ttl) {
    timeToLive = ttl;
  }

  public void setTSIGKey(String name, String algorithm, String key) {
    if (name != null && algorithm != null && key != null) {
      resolver.setTSIGKey(new TSIG(algorithm, name, key));
    } else {
      resolver.setTSIGKey(null);
    }
  }

  public boolean registerService(ServiceData serviceData) throws DnsSDException {
    try {
      ServiceName serviceName = serviceData.getName();
      Name dnsName = serviceName.toDnsName();
      Name typeName = new Name(serviceName.getType().toDnsString(), registrationDomain);
      List<Name> subtypes = new ArrayList<>(serviceName.getType().getSubtypes().size());
      for (String subtype : serviceName.getType().toDnsStringsWithSubtype()) {
        subtypes.add(new Name(subtype, registrationDomain));
      }
      Name target = new Name(serviceData.getHost());
      List<String> strings = new ArrayList<>();
      for (Map.Entry<String, String> entry : serviceData.getProperties().entrySet()) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.getKey());
        if (entry.getValue() != null) {
          sb.append('=').append(entry.getValue());
        }
        strings.add(sb.toString());
      }
      if (strings.isEmpty()) {
        // Must not be empty
        strings.add("");
      }
      Update update = new Update(registrationDomain);
      update.absent(dnsName);
      update.add(new PTRRecord(servicesName, DClass.IN, timeToLive, typeName));
      update.add(new PTRRecord(typeName, DClass.IN, timeToLive, dnsName));
      for (Name subtype : subtypes) {
        update.add(new PTRRecord(subtype, DClass.IN, timeToLive, dnsName));
      }
      update.add(new SRVRecord(dnsName, DClass.IN, timeToLive, 0, 0, serviceData.getPort(), target));
      update.add(new TXTRecord(dnsName, DClass.IN, timeToLive, strings));
      System.out.print("update= ");
      System.out.println(update.toString());

      Message response = resolver.send(update);
      switch (response.getRcode()) {
        case Rcode.NOERROR:
          flushCache(update);
          return true;
        case Rcode.YXDOMAIN:  // Prerequisite failed, the service already exists.
          return false;
        default:
          throw new DnsSDException("Server returned error code: " + Rcode.string(response.getRcode()));
      }
    } catch (TextParseException ex) {
      throw new IllegalArgumentException("Invalid service data: " + serviceData, ex);
    } catch (IOException ex) {
      throw new DnsSDException("Failed to send DNS update to server", ex);
    }
  }

  /**
   * Flush all records related to the update from the default cache.
   *
   * @param update the update to flush.
   */
  private static void flushCache(Update update) {
    Cache cache = Lookup.getDefaultCache(DClass.IN);
    Record[] records = update.getSectionArray(Section.UPDATE);
    for (Record rec : records) {
      logger.log(Level.FINE, "Flush name {0} due to update: {1}", new Object[]{rec.getName(), rec});
      cache.flushName(rec.getName());
    }
  }

  public boolean unregisterService(ServiceName serviceName) throws DnsSDException {
    try {
      Name dnsName = serviceName.toDnsName();
      Name typeName = new Name(serviceName.getType().toDnsString(), registrationDomain);
      List<Name> subtypes = new ArrayList<>(serviceName.getType().getSubtypes().size());
      for (String subtype : serviceName.getType().toDnsStringsWithSubtype()) {
        subtypes.add(new Name(subtype, registrationDomain));
      }
      Update update = new Update(registrationDomain);
      update.present(dnsName);
      update.delete(new PTRRecord(typeName, DClass.IN, timeToLive, dnsName));
      for (Name subtype : subtypes) {
        update.delete(new PTRRecord(subtype, DClass.IN, timeToLive, dnsName));
      }
      update.delete(dnsName);
      Message response = resolver.send(update);
      switch (response.getRcode()) {
        case Rcode.NOERROR:
          flushCache(update);
          break;
        case Rcode.NXDOMAIN:  // Prerequisite failed, the service doesn't exist.
          return false;
        default:
          throw new DnsSDException("Server returned error code: " + Rcode.string(response.getRcode()));
      }
      // Remove the service type if there are no instances left
      update = new Update(registrationDomain);
      update.absent(typeName);
      update.delete(new PTRRecord(servicesName, DClass.IN, timeToLive, typeName));
      response = resolver.send(update);
      switch (response.getRcode()) {
        case Rcode.NOERROR:
          flushCache(update);
          logger.log(Level.FINE, "Removed service type record {0}", typeName);
          break;
        case Rcode.YXDOMAIN:  // Prerequisite failed, service instances exists
          logger.log(Level.FINE, "Did not remove service type record {0}, instances left.", typeName);
          break;
        default:
          logger.log(Level.WARNING, "Failed to remove service type {0}, server returned status {1}",
                     new Object[]{typeName, Rcode.string(response.getRcode())});
      }
      return true;
    } catch (TextParseException ex) {
      throw new IllegalArgumentException("Invalid service name: " + serviceName, ex);
    } catch (IOException ex) {
      throw new DnsSDException("Failed to send DNS update to server", ex);
    }
  }
}

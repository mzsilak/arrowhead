/*
 *  Copyright (c) 2018 Jens Eliasson, Luleå University of Technology
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.core.datamanager;

import eu.arrowhead.common.DatabaseManager;
import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.messages.SigMLMessage;
import eu.arrowhead.common.messages.SenMLMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.log4j.Logger;

final class ProxyService {

  private static final Logger log = Logger.getLogger(DataManagerResource.class.getName());
  //private static final DatabaseManager dm = DatabaseManager.getInstance();

  private static List<ProxyElement> endpoints = new ArrayList<>();

  static ProxyElement getEndpoint(String name) {
    Iterator<ProxyElement> epi = endpoints.iterator();

    while (epi.hasNext()) {
      ProxyElement curpe = epi.next();
      System.out.println("Found endpoint: " + curpe.name);
      if (name.equals(curpe.name)) {
        return curpe;
      }
    }

    return null;
  }

  static boolean updateEndpoint(String name, Vector<SenMLMessage> msg) {
    Iterator<ProxyElement> epi = endpoints.iterator();

    while (epi.hasNext()) {
      ProxyElement pe = epi.next();
      if (name.equals(pe.name)) {
	System.out.println("Found endpoint: " + pe.name);
	pe.msg = msg; //.get(0);
	System.out.println("Updating with: " + msg.toString());
        return true;
      }
    }
    return false;
  }

  static boolean updateEndpoint(String name, SigMLMessage msg) {
    Iterator<ProxyElement> epi = endpoints.iterator();

    while (epi.hasNext()) {
      ProxyElement pe = epi.next();
      if (name.equals(pe.name)) {
	System.out.println("Found endpoint: " + pe.name);
	pe.msg = msg.sml;
	System.out.println("Updating with: " + msg.sml.toString());
        return true;
      }
    }
    return false;
  }

  static SenMLMessage fetchEndpoint(String name) {
    Iterator<ProxyElement> epi = endpoints.iterator();

    while (epi.hasNext()) {
      ProxyElement pe = epi.next();
      if (name.equals(pe.name)) {
	System.out.println("Found endpoint: " + pe.name);
        return null; //pe.msg;
      }
    }
    System.out.println("Endpoint: " + name + " not found");
    return null;
  }

  static boolean addEndpoint(ProxyElement e) {
    endpoints.add(e);
    return true;
  }

  static boolean deleteEndpoint(String name) { //XXX: do not support this now right now
    return false;
  }


  
}

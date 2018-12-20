/*
 *  Copyright (c) 2018 Jens Eliasson, Luleå University of Technology
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.core.datamanager;

public class ArrowheadSystem {
  String name;
  int id;

 
  public ArrowheadSystem(String name, int id) {
    this.name = new String(name);
    this.id = id; 
  }

  public String getName() {
    return this.name;
  }

  public int getId() {
    return this.id;
  }

}

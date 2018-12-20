/*
 *  Copyright (c) 2018 Jens Eliasson, Luleå University of Technology
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.core.datamanager;

import java.util.Vector;
import eu.arrowhead.common.messages.SigMLMessage;
import eu.arrowhead.common.messages.SenMLMessage;

public class ProxyElement {

  public String name = null;
  public int p = -1;	      // p is valid when using SigmL, set to -1 if data is SenML
  public Vector<SenMLMessage> msg = null;

  public ProxyElement(String name) {
    this.name = new String(name);
    this.p = -1;
    this.msg = null;
  }


  /**
   * @fn public ProxyElement(String name, Vector<SenMLMessage> senml)
   * @brief creates a new ProxyElement from a SenML message
   */
  public ProxyElement(String name, Vector<SenMLMessage> senml) {
    this.name = new String(name);
    this.p = -1;
    this.msg = senml;
  }


  /**
   * @fn public ProxyElement(String name, SigMLMessage sigml)
   * @brief creates a new ProxyElement from a SigML message
   */
  public ProxyElement(String name, SigMLMessage sigml) {
    this.name = new String(name);
    this.p = sigml.getP();
    this.msg = sigml.getSenML();
  }

}


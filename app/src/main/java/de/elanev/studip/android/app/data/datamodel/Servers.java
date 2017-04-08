/*
 * Copyright (c) 2016 ELAN e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package de.elanev.studip.android.app.data.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

/**
 * @author joern
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Servers {
  @JsonProperty("servers") private ArrayList<Server> servers;

  public Servers() {
  }

  public Servers(ArrayList<Server> servers) {
    this.servers = servers;
  }

  @JsonProperty("servers") public ArrayList<Server> getServers() {
    return servers;
  }

  @JsonProperty("servers") public void setServers(ArrayList<Server> servers) {
    this.servers = servers;
  }
}

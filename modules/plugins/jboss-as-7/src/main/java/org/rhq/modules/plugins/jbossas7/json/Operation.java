/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.modules.plugins.jbossas7.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Operation to run on the server
 * @author Heiko W. Rupp
 */
public class Operation {

    private String operation;
    @JsonProperty
    private List<PROPERTY_VALUE> address = Collections.emptyList();
    private Map<String,String> additionalProperties;


    public Operation(String operation, List<PROPERTY_VALUE> address, NameValuePair payload) {
        this.operation = operation;
        this.address = address;
        additionalProperties = new HashMap<String,String>(2);
        additionalProperties.put("name",payload.name);
        additionalProperties.put("value",payload.value);

    }

    public Operation(String operation, List<PROPERTY_VALUE> address, Map<String,String> payload) {
        this.operation = operation;
        this.address = address;
        this.additionalProperties = payload;
    }

    public Operation(String operation, List<PROPERTY_VALUE> address, String key, String value) {
        this.operation = operation;
        this.address = address;
        additionalProperties = new HashMap<String,String>(1);
        additionalProperties.put(key,value);

    }

    public Operation(String operation, List<PROPERTY_VALUE> address) {
        this.operation = operation;
        this.address = address;
        additionalProperties = Collections.emptyMap();

    }

    public Operation() {
        // needed for Jackson
    }

    @JsonAnySetter
    public void addAdditionalProperty(String key, String value) {
        if (additionalProperties == null)
            additionalProperties = new HashMap<String, String>();
        additionalProperties.put(key,value);
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonAnyGetter
    public Map<String,String> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonIgnore
    public String getName() {
       return getProperty("name");
    }

    @JsonIgnore
    public String getValue() {
       return getProperty("value");
    }

    private String getProperty(String key) {
            if (additionalProperties.containsKey(key))
            return additionalProperties.get(key);
        else
            return null;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<PROPERTY_VALUE> getAddress() {
        return address;
    }

    public void setAddress(List<PROPERTY_VALUE> address) {
        this.address = address;
    }

    public List<PROPERTY_VALUE> addToAddress(PROPERTY_VALUE component) {
        if (address==null)
            address = new ArrayList<PROPERTY_VALUE>();
        address.add(component);
        return address;
    }

}


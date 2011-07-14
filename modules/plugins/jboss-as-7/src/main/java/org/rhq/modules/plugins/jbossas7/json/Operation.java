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
    private Map<String,Object> additionalProperties;


    public Operation(String operation, List<PROPERTY_VALUE> address, NameValuePair payload) {
        this.operation = operation;
        this.address = address;
        additionalProperties = new HashMap<String,Object>(2);
        additionalProperties.put("name",payload.name);
        additionalProperties.put("value",payload.value);

    }

    public Operation(String operation, List<PROPERTY_VALUE> address, Map<String,Object> payload) {
        this.operation = operation;
        this.address = address;
        this.additionalProperties = payload;
    }

    public Operation(String operation, List<PROPERTY_VALUE> address, String key, Object value) {
        this.operation = operation;
        this.address = address;
        additionalProperties = new HashMap<String,Object>(1);
        additionalProperties.put(key,value);

    }

    public Operation(String operation, List<PROPERTY_VALUE> address) {
        this.operation = operation;
        this.address = address;

    }

    public Operation(String operation, String addressKey, String addressValue) {
        this.operation = operation;
        List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>(1);
        address.add(new PROPERTY_VALUE(addressKey, addressValue));
        this.address = address;
    }

    public Operation(String operation, Address address) {
        this.operation = operation;
        this.address = address.path;
    }

    public Operation() {
        // needed for Jackson
    }

    @JsonAnySetter
    public void addAdditionalProperty(String key, Object value) {
        if (additionalProperties == null)
            additionalProperties = new HashMap<String, Object>();
        additionalProperties.put(key,value);
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonAnyGetter
    public Map<String,Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonIgnore
    public String getName() {
       return (String) getProperty("name");
    }

    @JsonIgnore
    public String getValue() {
       return (String) getProperty("value");
    }

    private Object getProperty(String key) {
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


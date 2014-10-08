/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.json;

import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Operation to run on the server
 * @author Heiko W. Rupp
 */
public class Operation {

    private String operation;
    @JsonProperty(value = "address")
    private List<PROPERTY_VALUE> _address;
    @JsonIgnore
    Address address;
    private Map<String, Object> additionalProperties;
    @JsonProperty(value = "operation-headers")
    @JsonSerialize(include = NON_NULL)
    OperationHeaders operationHeaders;

    public Operation(String operation, String addressKey, String addressValue) {
        this.operation = operation;
        this.address = new Address(addressKey, addressValue);
        this._address = address.path;
        additionalProperties = new HashMap<String, Object>();
    }

    public Operation(String operation, Address address) {
        this.operation = operation;
        additionalProperties = new HashMap<String, Object>();
        if (address != null && address.path != null) {
            this.address = address;
            this._address = address.path;
        } else {
            _address = Collections.emptyList();
        }
    }

    public Operation(String operation, Address address, Map<String, Object> additionalProperties) {
        this(operation, address);
        this.additionalProperties = additionalProperties;
    }

    public Operation() {
        // needed for Jackson
    }

    @JsonAnySetter
    public void addAdditionalProperty(String key, Object value) {
        if (additionalProperties == null)
            additionalProperties = new HashMap<String, Object>();
        additionalProperties.put(key, value);
    }

    @SuppressWarnings("unused")
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
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

    @JsonProperty
    public String getOperation() {
        return operation;
    }

    @JsonIgnore
    public Address getAddress() {
        if (address == null) {
            address = new Address(_address);
        }
        return address;
    }

    @JsonIgnore
    public void allowResourceServiceRestart() {
        if (operationHeaders == null) {
            operationHeaders = new OperationHeaders();
        }
        operationHeaders.allowResourceServiceRestart = true;
    }

    @Override
    public String toString() {
        return "Operation{" + "operation='" + operation + '\'' + ", address=" + address + ", additionalProperties="
            + additionalProperties + '}';
    }

    private static class OperationHeaders {
        @JsonProperty("allow-resource-service-restart")
        boolean allowResourceServiceRestart;
    }
}

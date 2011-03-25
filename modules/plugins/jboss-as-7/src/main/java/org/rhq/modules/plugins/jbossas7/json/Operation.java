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
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * Operation to run on the server
 * @author Heiko W. Rupp
 */
public class Operation {

    public Operation(String operation, List<PROPERTY_VALUE> address, NameValuePair payload) {
        this.operation = operation;
        this.address = address;
        this.name = payload.name;
        this.value = payload.value;
    }

    public Operation() {
        // needed for Jackson
    }

    private String operation;
    @JsonProperty
    private List<PROPERTY_VALUE> address = Collections.emptyList();

    private String name;
    private String value;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /*
        [localhost:9999 /subsystem=web/connector=http] :write-attribute(name=socket-binding,value=jndi)
    yield JSON to send:
    {
        "operation" : "write-attribute",
        "name" : "socket-binding",
        "value" : "jndi",
        "address" : [
            {
                "PROPERTY_VALUE" : {
                    "subsystem" : "web"
                }
            },
            {
                "PROPERTY_VALUE" : {
                    "connector" : "http"
                }
            }
        ]
    }
    */

//    @JsonValue
//    public String toString() {
//        StringBuilder b = new StringBuilder();
//        b.append('{');
//        b.append("\"operation\":\"").append(operation).append("\",");
//        b.append("\"address\":");
//        if (address!=null && !address.isEmpty())
//            b.append(address);
//        else
//            b.append("[]");
//        b.append(',');
//        b.append(payload);
//        b.append("}");
//        return b.toString();
//    }
}


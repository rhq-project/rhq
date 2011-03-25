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
package org.rhq.modules.plugins.jbossas7;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.NameValuePair;
import org.rhq.modules.plugins.jbossas7.json.Operation;

/**
 * @author Heiko W. Rupp
 */
@Test
public class OperationJsonTest {


    public void operationSerDeserTest() throws Exception{

        List<PROPERTY_VALUE> address=new ArrayList<PROPERTY_VALUE>(2);
        PROPERTY_VALUE part = new PROPERTY_VALUE("subsystem","web");
        address.add(part);
        part = new PROPERTY_VALUE("connector","http");
        address.add(part);

        NameValuePair payload = new NameValuePair("socket-binding","jndi");
        Operation operation = new Operation("write-attribute",address,payload);


        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);

        System.out.println(operation);
        System.out.println(result);

        Operation op = mapper.readValue(result,Operation.class);
        assert op.getOperation().equals(operation.getOperation()) : "Operation is " + op.getOperation();
        assert op.getName().equals("socket-binding") : "attribute name  is " + op.getName();
        assert op.getValue().equals("jndi") : "attribute value  is " + op.getValue();

    }

    public void propertyValueDeserTest() throws Exception {

        String json = "{\"myKey\":\"myValue\"}";

        ObjectMapper mapper = new ObjectMapper();
        PROPERTY_VALUE pv = mapper.readValue(json,PROPERTY_VALUE.class);

        assert pv.getKey().equals("myKey") : "Key is " + pv.getKey();
        assert pv.getValue().equals("myValue"): "Value is " + pv.getValue();
    }
}

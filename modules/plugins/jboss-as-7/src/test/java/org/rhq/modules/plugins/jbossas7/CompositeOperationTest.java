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
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Test composite operations
 * @author Heiko W. Rupp
 */
@Test
public class CompositeOperationTest {

    public void serializationTest() throws Exception {

        List<PROPERTY_VALUE> address=new ArrayList<PROPERTY_VALUE>(3);
        PROPERTY_VALUE part = new PROPERTY_VALUE("subsystem","web");
        address.add(part);
        part = new PROPERTY_VALUE("connector","http");
        address.add(part);
        part = new PROPERTY_VALUE("profile","default");
        address.add(part);

        CompositeOperation op = new CompositeOperation();

        Operation step1 = new Operation("add", address);
        op.addStep(step1);

        Operation step2 = new Operation("deploy", address);
        op.addStep(step2);

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(op);

        System.out.println(result);

        assert result.contains("http");

    }

    public void serializationTest2() throws Exception {


        List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
        String TEST_WAR = "test.war";
        deploymentsAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        Operation step1 = new Operation("add",deploymentsAddress);
        String bytes_value = "123";
        step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", bytes_value));
        step1.addAdditionalProperty("name", TEST_WAR);

        List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
        serverGroupAddress.add(new PROPERTY_VALUE("server-group","main-server-group"));
        serverGroupAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        Operation step2 = new Operation("add",serverGroupAddress,"enabled","true");


        Operation step3 = new Operation("remove",serverGroupAddress);

        Operation step4 = new Operation("remove",deploymentsAddress);

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);
        cop.addStep(step2);
        cop.addStep(step3);
        cop.addStep(step4);

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(cop);
        System.out.println(result);
        System.out.flush();

        JsonNode node = mapper.readTree(result);

        assert node.has("operation");
        assert !node.get("operation").isNull() : "Operations was null, but should not be " + result;
        assert node.get("operation").getTextValue().equals("composite") : node;

    }
}

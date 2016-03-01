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
package org.rhq.modules.plugins.wildfly10;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.PROPERTY_VALUE;

/**
 * Test composite operations
 * @author Heiko W. Rupp
 */
@Test(groups = "unit")
public class CompositeOperationTest {

    public void serializationTest() throws Exception {

        Address address= new Address();
        address.add("subsystem","web");
        address.add("connector","http");
        address.add("profile","default");

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


        Address deploymentsAddress = new Address();
        String TEST_WAR = "test.war";
        deploymentsAddress.add("deployment", TEST_WAR);
        Operation step1 = new Operation("add",deploymentsAddress);
        String bytes_value = "123";
        step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", bytes_value));
        step1.addAdditionalProperty("name", TEST_WAR);

        Address serverGroupAddress = new Address();
        serverGroupAddress.add("server-group","main-server-group");
        serverGroupAddress.add("deployment", TEST_WAR);
        Operation step2 = new Operation("add",serverGroupAddress);
        step2.addAdditionalProperty("enabled","true");


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

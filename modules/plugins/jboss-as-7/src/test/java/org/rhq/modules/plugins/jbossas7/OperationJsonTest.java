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

package org.rhq.modules.plugins.jbossas7;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

/**
 * @author Heiko W. Rupp
 */
@Test(groups = "unit")
public class OperationJsonTest {

    public void operationSerDeserTest() throws Exception {

        Address address = new Address();
        address.add("subsystem", "web");
        address.add("connector", "http");

        Operation operation = new WriteAttribute(address, "socket-binding", "jndi");

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);
        Operation op = mapper.readValue(result, Operation.class);
        assertNotSame(op, null);
        assertNotSame(op.getOperation(), null, "op.operation was null!");
        assertEquals(op.getOperation(), operation.getOperation(), "Operation is " + op.getOperation());
        assertNotSame(op.getName(), null, "op.getName is null");
        assertEquals("socket-binding", op.getName(), "attribute name  is " + op.getName() + " and not 'socket-binding'");
        assertEquals(op.getValue(), "jndi", "attribute value  is " + op.getValue());
        assertSame(op.getAddress().size(), 2, "Address did not contain 2 parts, but " + op.getAddress().size());

    }

    public void propertyValueDeserTest() throws Exception {

        String json = "{\"myKey\":\"myValue\"}";

        ObjectMapper mapper = new ObjectMapper();
        PROPERTY_VALUE pv = mapper.readValue(json, PROPERTY_VALUE.class);

        assertEquals(pv.getKey(), "myKey", "Key is " + pv.getKey());
        assertEquals(pv.getValue(), "myValue", "Value is " + pv.getValue());
    }

    public void anyPayloadTest() throws Exception {

        Address address = new Address();
        address.add("/server-group", "newOne");

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("profile", "default");
        props.put("someBool", true);

        Operation operation = new Operation("add", address, props);
        operation.addAdditionalProperty("foo", "bar");

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);

        assertFalse(result.contains("name"), "Result contains a name property but should not, " + result);
        assertFalse(result.contains("null"), "Result contains null values but should not, " + result);

        Operation op = mapper.readValue(result, Operation.class);
        assertEquals(op.getOperation(), operation.getOperation(), "Operation is " + op.getOperation());
        assertTrue(op.getAdditionalProperties().containsKey("someBool"), "Key someBool not found ");
        Object someBool = op.getAdditionalProperties().get("someBool");
        assertTrue((Boolean) someBool, "someBool was not true");

    }

    public void operationHeadersTest() throws Exception {
        Address address = new Address();
        address.add("/server-group", "newOne");

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("profile", "default");
        props.put("someBool", true);

        Operation operation = new Operation("add", address, props);
        operation.addAdditionalProperty("foo", "bar");

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);

        assertFalse(result.contains("operation-headers"),
            "Result contains a operation-headers property but should not, " + result);

        operation.allowResourceServiceRestart();

        result = mapper.writeValueAsString(operation);

        assertTrue(result.contains("operation-headers"),
            "Result does not contain a operation-headers property but should, " + result);
    }

    public void addPropsTest() throws Exception {
        Address address = new Address();
        address.add("/server-group", "newOne");

        Operation operation = new Operation("add", address);
        operation.addAdditionalProperty("foo", "bar");

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);

        assertTrue(result.contains("\"foo\":\"bar\"".replace(" ", "")),
            "Result does not contain property \"foo\":\"bar\", " + result);
    }

    public void simpleResult() throws Exception {

        String resultString = "{\"outcome\" : \"success\", \"result\" : \"no metrics available\"}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);

        assertNotSame(result, null);
        assertEquals(result.getOutcome(), "success");
        assertTrue(result.isSuccess());
    }

    public void simpleResult2() throws Exception {

        String resultString = "{\"outcome\" : \"success\", \"result\" : \"DISABLED\"}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);

        assertNotSame(result, null);
        assertEquals(result.getOutcome(), "success");
        assertTrue(result.isSuccess());
    }

    public void simpleResultWithFailure() throws Exception {

        String resultString = "{\"outcome\" : \"failed\", \"failure-description\" : [{ \"java.util.NoSuchElementException\" : \"No child 'profile' exists\" }]}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);

        assertNotSame(result, null);
        assertEquals(result.getOutcome(), "failed");
        assertFalse(result.isSuccess());

        assertSame(result.getResult(), null);
        assertNotSame(result.getFailureDescription(), null);
        //        assertSame(result.getFailureDescription().size(), 1);
    }

    public void complexResult1() throws Exception {

        String resultString = " {\"outcome\" : \"success\", \"result\" : {\"alias\" : [\"example.com\"], \"access-log\" : null, \"rewrite\" : null}}";

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);

        assertNotSame(result, null);
        assertEquals(result.getOutcome(), "success");
        assertTrue(result.isSuccess());
        assertSame(result.getResult().size(), 3);
        String rewrite = (String) result.getResult().get("rewrite");
        assertSame(rewrite, null);

        @SuppressWarnings("unchecked")
        List<String> aliases = (List<String>) result.getResult().get("alias");
        assertNotSame(aliases, null);
        assertSame(aliases.size(), 1);
        assertEquals(aliases.get(0), "example.com");
    }

    public void arrayResult1() throws Exception {

        String resultString = "{\"outcome\":\"success\",\"result\":[\"standard-sockets\",\"messaging-sockets\"],\"response-headers\":null, \"rolled-back\" : false}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);

        assertNotSame(result, null);
        assertEquals(result.getOutcome(), "success");
        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<String> stringList = (List<String>) result.getResult();
        assertSame(stringList.size(), 2);
        assertEquals(stringList.get(0), "standard-sockets");
        assertEquals(stringList.get(1), "messaging-sockets");
        assertFalse(result.isRolledBack());

    }

    public void rolledBack() throws Exception {

        String resultString = "{\"outcome\":\"failed\", \"rolled-back\" : true}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);

        assertNotSame(result, null);
        assertEquals(result.getOutcome(), "failed");
        assertFalse(result.isSuccess());
        assertTrue(result.isRolledBack());

    }

    public void needsNoReload() throws Exception {

        String resultString = "{\"outcome\":\"failed\", \"rolled-back\" : true}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);

        assertNotSame(result, null);
        assertFalse(result.isReloadRequired());
    }

    public void needsReload1() throws Exception {

        String resultString = "{\n" + "    \"outcome\":\"success\",\n" + "    \"result\":{\n"
            + "        \"enabled\":\"true\"\n" + "    },\n" + "    \"response-headers\":{\n"
            + "        \"process-state\":\"reload-required\"\n" + "    }\n" + "}\n";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);

        assertNotSame(result, null);
        assertTrue(result.isReloadRequired());
    }

    public void complexResult2() throws Exception {

        String resultString = "{\n"
            + "  \"outcome\" : \"failed\",\n"
            + "  \"result\" : {\n"
            + "    \"server-groups\" : {\n"
            + "      \"main-server-group\" : {\n"
            + "        \"server-one\" : {\n"
            + "          \"host\" : \"local\",\n"
            + "          \"response\" : {\n"
            + "            \"outcome\" : \"success\",\n"
            + "            \"result\" : null,\n"
            + "            \"compensating-operation\" : {\n"
            + "              \"operation\" : \"add\",\n"
            + "              \"address\" : [ {\n"
            + "                \"subsystem\" : \"jms\"\n"
            + "              }, {\n"
            + "                \"queue\" : \"flubbr\"\n"
            + "              } ],\n"
            + "              \"durable\" : \"true\",\n"
            + "              \"entries\" : [ \"PropertySimple[id=0, name=entries, value=flubbr, override=null]\" ]\n"
            + "            }\n"
            + "          }\n"
            + "        },\n"
            + "        \"server-two\" : {\n"
            + "          \"host\" : \"local\",\n"
            + "          \"response\" : {\n"
            + "            \"outcome\" : \"success\",\n"
            + "            \"result\" : null,\n"
            + "            \"compensating-operation\" : {\n"
            + "              \"operation\" : \"add\",\n"
            + "              \"address\" : [ {\n"
            + "                \"subsystem\" : \"jms\"\n"
            + "              }, {\n"
            + "                \"queue\" : \"flubbr\"\n"
            + "              } ],\n"
            + "              \"durable\" : \"true\",\n"
            + "              \"entries\" : [ \"PropertySimple[id=0, name=entries, value=flubbr, override=null]\" ]\n"
            + "            }\n"
            + "          }\n"
            + "        },\n"
            + "        \"server-demo\" : {\n"
            + "          \"host\" : \"local\",\n"
            + "          \"response\" : {\n"
            + "            \"outcome\" : \"failed\",\n"
            + "            \"failure-description\" : \"No handler for add at address [\\n    (\\\"host\\\" => \\\"local\\\"),\\n    (\\\"server\\\" => \\\"server-demo\\\"),\\n    (\\\"subsystem\\\" => \\\"jms\\\"),\\n    (\\\"queue\\\" => \\\"flubbr\\\")\\n]\"\n"
            + "          }\n" + "        }\n" + "      }\n" + "    }\n" + "  },\n"
            + "  \"failure-description\" : \"Operation was not applied successfully to any servers\"\n" + "}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        assertFalse(result.isSuccess(), "Result should be 'failed', but was not");
        assertTrue(result.getFailureDescription().startsWith("Operation was not applied successfully to any servers"));

        assertTrue(result.getResult().containsKey("server-groups"));
        @SuppressWarnings("unchecked")
        Map<String, Object> sgs = (Map<String, Object>) result.getResult().get("server-groups");
        assertTrue(sgs.containsKey("main-server-group"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mainSg = (Map<String, Object>) sgs.get("main-server-group");
        assertSame(mainSg.size(), 3, "Main server group does not have 3 servers, but " + mainSg.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> s3 = (Map<String, Object>) mainSg.get("server-demo");
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) s3.get("response");
        assertNotSame(response, null);

    }

}

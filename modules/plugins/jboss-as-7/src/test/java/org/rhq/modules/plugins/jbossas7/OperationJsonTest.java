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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

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

        Operation operation = new WriteAttribute(new Address(address),"socket-binding","jndi");


        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);

        Operation op = mapper.readValue(result,Operation.class);
        assert op.getOperation().equals(operation.getOperation()) : "Operation is " + op.getOperation();
        assert op.getName().equals("socket-binding") : "attribute name  is " + op.getName();
        assert op.getValue().equals("jndi") : "attribute value  is " + op.getValue();
        assert op.getAddress().size()==2 : "Address did not contain 2 parts, but " + op.getAddress().size();


    }

    public void propertyValueDeserTest() throws Exception {

        String json = "{\"myKey\":\"myValue\"}";

        ObjectMapper mapper = new ObjectMapper();
        PROPERTY_VALUE pv = mapper.readValue(json,PROPERTY_VALUE.class);

        assert pv.getKey().equals("myKey") : "Key is " + pv.getKey();
        assert pv.getValue().equals("myValue"): "Value is " + pv.getValue();
    }

    public void anyPayloadTest() throws Exception {

        List<PROPERTY_VALUE> address=new ArrayList<PROPERTY_VALUE>(2);
        PROPERTY_VALUE part = new PROPERTY_VALUE("/server-group","newOne");
        address.add(part);

        Map<String,Object> props = new HashMap<String, Object>();
        props.put("profile","default");
        props.put("someBool",true);

        Operation operation = new Operation("add",address,props);
        operation.addAdditionalProperty("foo","bar");

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);
        System.out.println(result);

        assert !result.contains("name") : "Result contains a name property but should not : " + result;
        assert !result.contains("null") : "Result contains null values but should not : " + result;

        Operation op = mapper.readValue(result,Operation.class);
        assert op.getOperation().equals(operation.getOperation()) : "Operation is " + op.getOperation();
        assert op.getAdditionalProperties().containsKey("someBool") : "Key someBool not found ";
        Object someBool = op.getAdditionalProperties().get("someBool");
        assert Boolean.valueOf((Boolean) someBool) : "someBool was not true";

    }

    public void addPropsTest() throws Exception {
        List<PROPERTY_VALUE> address=new ArrayList<PROPERTY_VALUE>(2);
        PROPERTY_VALUE part = new PROPERTY_VALUE("/server-group","newOne");
        address.add(part);

        Operation operation = new Operation("add",address);
        operation.addAdditionalProperty("foo","bar");

        ObjectMapper mapper = new ObjectMapper();

        String result = mapper.writeValueAsString(operation);
        System.out.println(result);

    }

    public void simpleResult() throws Exception {

        String resultString = "{\"outcome\" : \"success\", \"result\" : \"no metrics available\"}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString,Result.class);

        assert result != null;
        assert result.getOutcome().equals("success");
        assert result.isSuccess();
    }

    public void simpleResult2() throws Exception {

        String resultString = "{\"outcome\" : \"success\", \"result\" : \"DISABLED\"}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString,Result.class);

        assert result != null;
        assert result.getOutcome().equals("success");
        assert result.isSuccess();
    }
    public void simpleResultWithFailure() throws Exception {

        String resultString = "{\"outcome\" : \"failed\", \"failure-description\" : [{ \"java.util.NoSuchElementException\" : \"No child 'profile' exists\" }]}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString,Result.class);

        assert result != null;
        assert result.getOutcome().equals("failed");
        assert !result.isSuccess();

        assert result.getResult() == null;
        assert result.getFailureDescription() != null;
//        assert result.getFailureDescription().size() == 1;
    }

    public void complexResult1() throws Exception {

        String resultString = " {\"outcome\" : \"success\", \"result\" : {\"alias\" : [\"example.com\"], \"access-log\" : null, \"rewrite\" : null}}";

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);

        assert result != null;
        assert result.getOutcome().equals("success");
        assert result.isSuccess();
        assert result.getResult().size()==3;
        String rewrite = (String) result.getResult().get("rewrite");
        assert rewrite == null;

        List<String> aliases = (List<String>) result.getResult().get("alias");
        assert aliases != null;
        assert aliases.size()==1;
        assert aliases.get(0).equals("example.com");
    }

    public void arrayResult1() throws Exception {

        String resultString = "{\"outcome\":\"success\",\"result\":[\"standard-sockets\",\"messaging-sockets\"],\"response-headers\":null, \"rolled-back\" : false}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString,Result.class);

        assert result != null;
        assert result.getOutcome().equals("success");
        assert result.isSuccess();
        List<String> stringList = (List<String>) result.getResult();
        assert stringList.size()==2;
        assert stringList.get(0).equals("standard-sockets");
        assert stringList.get(1).equals("messaging-sockets");
        assert !result.isRolledBack();

    }

    public void rolledBack() throws Exception {

        String resultString = "{\"outcome\":\"failed\", \"rolled-back\" : true}";

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString,Result.class);

        assert result != null;
        assert result.getOutcome().equals("failed");
        assert !result.isSuccess();
        assert result.isRolledBack();

    }


    public void complexResult2() throws Exception {


        String resultString =
                "{\n" +
                        "  \"outcome\" : \"failed\",\n" +
                        "  \"result\" : {\n" +
                        "    \"server-groups\" : {\n" +
                        "      \"main-server-group\" : {\n" +
                        "        \"server-one\" : {\n" +
                        "          \"host\" : \"local\",\n" +
                        "          \"response\" : {\n" +
                        "            \"outcome\" : \"success\",\n" +
                        "            \"result\" : null,\n" +
                        "            \"compensating-operation\" : {\n" +
                        "              \"operation\" : \"add\",\n" +
                        "              \"address\" : [ {\n" +
                        "                \"subsystem\" : \"jms\"\n" +
                        "              }, {\n" +
                        "                \"queue\" : \"flubbr\"\n" +
                        "              } ],\n" +
                        "              \"durable\" : \"true\",\n" +
                        "              \"entries\" : [ \"PropertySimple[id=0, name=entries, value=flubbr, override=null]\" ]\n" +
                        "            }\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"server-two\" : {\n" +
                        "          \"host\" : \"local\",\n" +
                        "          \"response\" : {\n" +
                        "            \"outcome\" : \"success\",\n" +
                        "            \"result\" : null,\n" +
                        "            \"compensating-operation\" : {\n" +
                        "              \"operation\" : \"add\",\n" +
                        "              \"address\" : [ {\n" +
                        "                \"subsystem\" : \"jms\"\n" +
                        "              }, {\n" +
                        "                \"queue\" : \"flubbr\"\n" +
                        "              } ],\n" +
                        "              \"durable\" : \"true\",\n" +
                        "              \"entries\" : [ \"PropertySimple[id=0, name=entries, value=flubbr, override=null]\" ]\n" +
                        "            }\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"server-demo\" : {\n" +
                        "          \"host\" : \"local\",\n" +
                        "          \"response\" : {\n" +
                        "            \"outcome\" : \"failed\",\n" +
                        "            \"failure-description\" : \"No handler for add at address [\\n    (\\\"host\\\" => \\\"local\\\"),\\n    (\\\"server\\\" => \\\"server-demo\\\"),\\n    (\\\"subsystem\\\" => \\\"jms\\\"),\\n    (\\\"queue\\\" => \\\"flubbr\\\")\\n]\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"failure-description\" : \"Operation was not applied successfully to any servers\"\n" +
                        "}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        assert !result.isSuccess() : "Result should be 'failed', but was not";
        assert result.getFailureDescription().startsWith("Operation was not applied successfully to any servers");

        assert result.getResult().containsKey("server-groups");
        Map<String,Object> sgs = (Map<String, Object>) result.getResult().get("server-groups");
        assert sgs.containsKey("main-server-group");
        Map<String,Object> mainSg = (Map<String, Object>) sgs.get("main-server-group");
        assert mainSg.size()==3 : "Main server group does not have 3 servers, but " + mainSg.size();
        Map<String,Object> s3 = (Map<String, Object>) mainSg.get("server-demo");
        Map<String,Object> response = (Map<String, Object>) s3.get("response");
        assert response!=null;

    }

}

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Test uploading and deploying to the domain
 * To use it, a server in <b>domain mode</b> must be up and have it's
 * <b>DomainController</b> running <b>locally</b> and
 * the UPLOAD_FILE must point to a valid archive in the resources directory.
 * @author Heiko W. Rupp
 */
@Test
public class UploadAndDeployTest {

    static final String TEST_WAR = "test.war";
    private static final String UPLOAD_FILE = "test-simple.war";
    private static final String DC_HOST = "localhost";
    private static final int DC_HTTP_PORT = 9990;

    @Test(timeOut = 60*1000L, enabled = true)
    public void testUploadOnly() throws Exception {

        String bytes_value = prepare();

        assert bytes_value != null;

        System.out.println("sha: " + bytes_value);
        assert bytes_value.equals("7jgpMVmynfxpqp8UDleKLmtgbrA=");

    }

    @Test(timeOut = 60*1000L, enabled = true)
    public void testDoubleUploadOnly() throws Exception {

        String bytes_value = prepare();
        String bytes_value2 = prepare();

        assert bytes_value != null;
        assert bytes_value2 != null;
        assert bytes_value.equals(bytes_value2);

        assert bytes_value.equals("7jgpMVmynfxpqp8UDleKLmtgbrA=");
    }

    @Test(timeOut = 60*1000L, enabled = true)
    public void testUploadIndividualSteps() throws Exception {

        String bytes_value = prepare();

        System.out.println("sha: " + bytes_value);

        System.out.println();
        ASConnection connection = new ASConnection(DC_HOST, DC_HTTP_PORT);

        List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
        deploymentsAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        Operation op = new Operation("add",deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",bytes_value));
        content.add(contentValues);
        op.addAdditionalProperty("content",content);
        op.addAdditionalProperty("name", TEST_WAR); // this needs to be unique per upload
        op.addAdditionalProperty("runtime-name", TEST_WAR);
        System.out.flush();
        JsonNode ret = connection.executeRaw(op);
        op = null;
        System.out.println("Add to /deploy done " + ret);
        System.out.flush();

        assert ret.toString().contains("success") : ret;


        List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
        serverGroupAddress.add(new PROPERTY_VALUE("server-group", "main-server-group"));
        serverGroupAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));

        Operation attach = new Operation("add",serverGroupAddress);//,"enabled","true");
//        deploy.addAdditionalProperty("runtime-name", TEST_WAR);
        System.out.flush();
        ret = connection.executeRaw(attach);
        System.out.println("Add to server group done: " + ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "add to sg was no success " + ret.getTextValue();


        Operation deploy = new Operation("deploy",serverGroupAddress);
        Result depRes = connection.execute(deploy);

        assert depRes.isSuccess() : "Deploy went wrong: " + depRes.getFailureDescription();


        Thread.sleep(500);

        Operation undeploy = new Operation("undeploy",serverGroupAddress);
        depRes = connection.execute(undeploy);

        assert depRes.isSuccess() : "Undeploy went wrong: " + depRes.getFailureDescription();
        undeploy = null;

        // Now tear down stuff again

        Operation unattach = new Operation("remove",serverGroupAddress);
        ret = connection.executeRaw(unattach);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from sg was no success " + ret.getTextValue();


        // remove from domain

        Operation remove = new Operation("remove",deploymentsAddress);
        ret = connection.executeRaw(remove);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from domain was no success " + ret.getTextValue();

        System.out.flush();

    }

    @Test(timeOut = 60*1000L, enabled = true)
    public void testUploadComposite() throws Exception {

        String bytes_value = prepare();

        System.out.println("Prepare done");
        System.out.flush();

        List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
        deploymentsAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        Operation step1 = new Operation("add",deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",bytes_value));
        content.add(contentValues);
        step1.addAdditionalProperty("content", content);
        step1.addAdditionalProperty("name", TEST_WAR); // this needs to be unique per upload


        List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
        serverGroupAddress.add(new PROPERTY_VALUE("server-group","main-server-group"));
        serverGroupAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        Operation step2 = new Operation("add",serverGroupAddress);// ,"enabled","true");
        Operation step2a = new Operation("deploy",serverGroupAddress);


        Operation step3 = new Operation("undeploy",serverGroupAddress);
        Operation step3a = new Operation("remove",serverGroupAddress);

        Operation step4 = new Operation("remove",deploymentsAddress);

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);
        cop.addStep(step2);
        cop.addStep(step2a);


        ASConnection connection = new ASConnection(DC_HOST, DC_HTTP_PORT);
        JsonNode ret = connection.executeRaw(cop);
        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite deploy was no success " + ret.getTextValue();

        Thread.sleep(1000);

        cop = new CompositeOperation();
        cop.addStep(step3);
        cop.addStep(step3a);
        cop.addStep(step4);
        ret = connection.executeRaw(cop);

        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite remove was no success " + ret.getTextValue();


    }

    private String prepare() throws IOException {
        ASUploadConnection conn = new ASUploadConnection(DC_HOST, DC_HTTP_PORT);
        OutputStream os = conn.getOutputStream("test.war");



        InputStream fis = getClass().getClassLoader().getResourceAsStream(UPLOAD_FILE);
        final byte[] buffer = new byte[1024];
        int numRead = 0;

        while(numRead > -1) {
            numRead = fis.read(buffer);
            if(numRead > 0) {
                os.write(buffer,0,numRead);
            }
        }
        fis.close();
        JsonNode node = conn.finishUpload();
        System.out.println(node);
        assert node != null : "No result from upload - node was null";
        assert node.has("outcome") : "No outcome from upload";
        String outcome = node.get("outcome").getTextValue();
        assert outcome.equals("success") : "Upload was no success" + outcome;

        JsonNode resultNode = node.get("result");
        return resultNode.get("BYTES_VALUE").getTextValue();
    }
}

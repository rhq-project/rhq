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
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Test uploading and deploying to the domain
 * To use it, domain server must be up and running locally and
 * the UPLOAD_FILE must point to a valid archive in the resources directory.
 * @author Heiko W. Rupp
 */
@Test
public class UploadAndDeployTest {

    static final String TEST_WAR = "test.war";
    private static final String UPLOAD_FILE = "test-simple.war";

    public void testUploadIndividualSteps() throws Exception {

        String bytes_value = prepare();

        System.out.println("sha: " + bytes_value);

        System.out.println();
        ASConnection connection = new ASConnection("localhost",9990);

        List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
        deploymentsAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        Operation op = new Operation("add",deploymentsAddress);
        op.addAdditionalProperty("hash",new PROPERTY_VALUE("BYTES_VALUE",bytes_value));
        op.addAdditionalProperty("name", TEST_WAR); // this needs to be separate per upload
        op.addAdditionalProperty("runtime-name", TEST_WAR);
        System.out.flush();
        JsonNode ret = connection.execute(op);
        System.out.println("Add to /deploy done " + ret);
        System.out.flush();

        assert ret.toString().contains("success") : ret;


        List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
        serverGroupAddress.add(new PROPERTY_VALUE("server-group","main-server-group"));
        serverGroupAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        op.addAdditionalProperty("runtime-name", TEST_WAR);
        Operation deploy = new Operation("add",serverGroupAddress,"enabled","true");
        System.out.flush();
        ret = connection.execute(deploy);
        System.out.println("Add to server group done: " + ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valied " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "add to sg was no success " + ret.getTextValue();


        // Now teat down stuff again
/*

        Operation undeploy = new Operation("remove",serverGroupAddress);
        ret = connection.execute(undeploy);

        assert ret.has("outcome") : "Ret not valied " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from sg was no success " + ret.getTextValue();


        // remove from domain

        Operation remove = new Operation("remove",deploymentsAddress);
        ret = connection.execute(remove);

        assert ret.has("outcome") : "Ret not valied " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from domain was no success " + ret.getTextValue();

        System.out.flush();

*/
    }

    @Test(enabled = false)
    public void testUploadComposite() throws Exception {

        String bytes_value = prepare();


        List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
        deploymentsAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));
        Operation step1 = new Operation("add",deploymentsAddress);
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


        ASConnection connection = new ASConnection("localhost",9990);
        JsonNode ret = connection.execute(cop);
        System.out.println(ret);
        System.out.flush();

        Thread.sleep(1000);

        cop = new CompositeOperation();
        cop.addStep(step3);
        cop.addStep(step4);
        ret = connection.execute(cop);

        System.out.println(ret);
        System.out.flush();


    }

    private String prepare() throws IOException {
        ASUploadConnection conn = new ASUploadConnection();
        OutputStream os = conn.getOutputStream("test1.war");



        InputStream fis = getClass().getClassLoader().getResourceAsStream(UPLOAD_FILE);
        int b;
        while ((b = fis.read())!=-1) {
            os.write(b);
        }
        fis.close();
        JsonNode node = conn.finishUpload();
        System.out.println(node);
        assert node.has("outcome") : "No outcome from upload";
        String outcome = node.get("outcome").getTextValue();
        assert outcome.equals("success") : "Upload was no success" + outcome;

        JsonNode resultNode = node.get("result");
        return resultNode.get("BYTES_VALUE").getTextValue();
    }
}

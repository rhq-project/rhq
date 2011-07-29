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
package org.rhq.modules.integrationTests.jbossas7plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Remove;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Test uploading and deploying to the domain
 * To use it, a server in <b>domain mode</b> must be up and have it's
 * <b>DomainController</b> running <b>locally</b> and
 * the UPLOAD_FILE must point to a valid archive in the resources directory.
 * @author Heiko W. Rupp
 */
@Test(enabled = UploadAndDeployTest.isEnabled)
public class UploadAndDeployTest extends AbstractIntegrationTest {

    private  String TEST_WAR = "test-simple.war";

    protected static final boolean isEnabled = true;

    @Test(timeOut = 60*1000L, enabled=isEnabled)
    public void testUploadOnly() throws Exception {

        String bytes_value = uploadToAs(TEST_WAR);

        assert bytes_value != null;

        System.out.println("sha: " + bytes_value);
        assert bytes_value.equals("7jgpMVmynfxpqp8UDleKLmtgbrA=");

    }

    @Test(timeOut = 60*1000L, enabled=isEnabled)
    public void testDoubleUploadOnly() throws Exception {

        String bytes_value = uploadToAs(TEST_WAR);
        String bytes_value2 = uploadToAs(TEST_WAR);

        assert bytes_value != null;
        assert bytes_value2 != null;
        assert bytes_value.equals(bytes_value2);

        assert bytes_value.equals("7jgpMVmynfxpqp8UDleKLmtgbrA=");
    }

    @Test(timeOut = 60*1000L,enabled=isEnabled)
    public void testUploadIndividualSteps() throws Exception {

        String bytes_value = uploadToAs(TEST_WAR);

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

    // Test for AS7-853
    @Test(timeOut = 60*1000L,enabled = isEnabled)
    public void testUploadIndividualSteps2() throws Exception {

        String bytes_value = uploadToAs(TEST_WAR);

        System.out.println("sha: " + bytes_value);

        System.out.println();
        ASConnection connection = new ASConnection(DC_HOST, DC_HTTP_PORT);

/*
        Address deploymentsAddress = new Address();
        deploymentsAddress.add("deployment", TEST_WAR);
        Operation op = new Operation("add",deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",bytes_value));
        content.add(contentValues);
        op.addAdditionalProperty("content",content);
        op.addAdditionalProperty("name", TEST_WAR); // this needs to be unique per upload
        op.addAdditionalProperty("runtime-name", TEST_WAR);
        System.out.flush();
*/
        Operation op = addDeployment(TEST_WAR,bytes_value);
        JsonNode ret = connection.executeRaw(op);
        op = null;
        System.out.println("Add to /deploy done " + ret);
        System.out.flush();

        assert ret.toString().contains("success") : ret;


        List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
        serverGroupAddress.add(new PROPERTY_VALUE("server-group", "main-server-group"));
        serverGroupAddress.add(new PROPERTY_VALUE("deployment", TEST_WAR));

        Operation attach = new Operation("add",serverGroupAddress,"enabled",true);
        System.out.flush();
        ret = connection.executeRaw(attach);
        System.out.println("Add to server group done: " + ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "add to sg was no success " + ret.getTextValue();


        Result depRes;// = connection.execute(deploy);

        Thread.sleep(500);

        Operation undeploy = new Operation("undeploy",serverGroupAddress);
        depRes = connection.execute(undeploy);

        assert depRes.isSuccess() : "Undeploy went wrong: " + depRes.getFailureDescription();
        undeploy = null;

        // Now tear down stuff again

        Operation unattach = new Remove(serverGroupAddress);
        ret = connection.executeRaw(unattach);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from sg was no success " + ret.getTextValue();


        // remove from domain

        Operation remove = new Remove("deployment",TEST_WAR);
        ret = connection.executeRaw(remove);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from domain was no success " + ret.getTextValue();

        System.out.flush();

    }

    @Test(timeOut = 60*1000L,enabled = isEnabled)
    public void testUploadComposite() throws Exception {

        String bytes_value = uploadToAs(TEST_WAR);

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

    /**
     * Test uploading to domain only, but not to a server group
     * @throws Exception if anything goes wrong.
     */
    @Test(timeOut = 60*1000L,enabled = isEnabled)
    public void testUploadComposite2() throws Exception {

        String bytes_value = uploadToAs(TEST_WAR);

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


        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);


        ASConnection connection = new ASConnection(DC_HOST, DC_HTTP_PORT);
        JsonNode ret = connection.executeRaw(cop);
        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite deploy was no success " + ret.getTextValue();

        // Wait for AS to settle
        Thread.sleep(1000);


        // Now undeploy again to clean up

        cop = new CompositeOperation();
        Operation step4 = new Operation("remove",deploymentsAddress);

        cop.addStep(step4);
        ret = connection.executeRaw(cop);

        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite remove was no success " + ret.getTextValue();


    }

    /**
     * Test the real API code for uploading - case 1: just upload to /deployment
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testUploadViaCreateChild1() throws Exception {

        BaseComponent bc = new BaseComponent();
        bc.setConnection(getASConnection());
        bc.setPath("");
        ResourceType rt = new ResourceType();
        rt.setName("Deployment");
        Resource resource = new Resource("deployment="+TEST_WAR,TEST_WAR,rt); // TODO resource key?
        ResourceContext context = new ResourceContext(resource,null,null,null,null,null,null,null,null,null,null,null);
        bc.start(context);

        String bytes_value = uploadToAs(TEST_WAR);

        ResourcePackageDetails details = new ResourcePackageDetails(new PackageDetailsKey(TEST_WAR,"1.0","deployment","all"));
        CreateResourceReport report = new CreateResourceReport(TEST_WAR,rt,new Configuration(), new Configuration(),details);
        try {
            report = bc.runDeploymentMagicOnServer(report,TEST_WAR,TEST_WAR,bytes_value);
            assert report != null;
            assert report.getErrorMessage()==null: "Report contained an unexpected error: " + report.getErrorMessage();
            assert report.getStatus()!=null : "Report did not contain a status";
            assert report.getStatus()== CreateResourceStatus.SUCCESS : "Status was no success";
            assert report.getResourceName().equals(TEST_WAR);
            assert report.getResourceKey().equals("deployment=" + TEST_WAR);
        } finally {
            Remove r =new Remove("deployment",TEST_WAR);
            getASConnection().execute(r);
        }

    }

    /**
     * Test the real API code for uploading - case 2: upload to /deployment and a server group
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testUploadViaCreateChild2() throws Exception {

        BaseComponent bc = new BaseComponent();
        bc.setConnection(getASConnection());
        bc.setPath("server-group=main-server-group");
        ResourceType rt = new ResourceType();
        rt.setName("Deployment");
        Resource resource = new Resource("server-group=main-server-group",TEST_WAR,rt);
        ResourceContext context = new ResourceContext(resource,null,null,null,null,null,null,null,null,null,null,null);
        bc.start(context);

        String bytes_value = uploadToAs(TEST_WAR);

        ResourcePackageDetails details = new ResourcePackageDetails(new PackageDetailsKey(TEST_WAR,"1.0","deployment","all"));
        CreateResourceReport report = new CreateResourceReport(TEST_WAR,rt,new Configuration(), new Configuration(),details);
        try {
            report = bc.runDeploymentMagicOnServer(report,TEST_WAR,TEST_WAR,bytes_value);
            assert report != null;
            assert report.getErrorMessage()==null: "Report contained an unexpected error: " + report.getErrorMessage();
            assert report.getStatus()!=null : "Report did not contain a status";
            assert report.getStatus()== CreateResourceStatus.SUCCESS : "Status was no success";
            assert report.getResourceName().equals(TEST_WAR);
            assert report.getResourceKey().equals("server-group=main-server-group,deployment=" + TEST_WAR) : "Resource key was wrong";
        } finally {
            Address sgd = new Address("server-group","main-server-group");
            sgd.add("deployment", TEST_WAR);
            Remove r =new Remove(sgd);
            getASConnection().execute(r);
            r =new Remove("deployment",TEST_WAR);
            getASConnection().execute(r);
        }

    }
}

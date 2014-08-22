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

package org.rhq.modules.plugins.jbossas7.itest.nonpc;

import static org.rhq.modules.plugins.jbossas7.test.util.ASConnectionFactory.getDomainControllerASConnection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
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
 *
 * @author Heiko W. Rupp
 */
@Test(enabled = UploadAndDeployTest.isEnabled)
public class UploadAndDeployTest extends AbstractIntegrationTest {

    private String TEST_WAR_PATH = "itest/test-simple.war";
    private String TEST_WAR_FILE_NAME = new File(TEST_WAR_PATH).getName();

    protected static final boolean isEnabled = true;

    @Test(timeOut = 60 * 1000L, enabled = isEnabled)
    public void testUploadOnly() throws Exception {
        String bytes_value = uploadToAs(TEST_WAR_PATH);

        assert bytes_value != null;

        System.out.println("sha: " + bytes_value);
        assert bytes_value.equals("7jgpMVmynfxpqp8UDleKLmtgbrA=");
    }

    @Test(timeOut = 60 * 1000L, enabled = isEnabled)
    public void testDoubleUploadOnly() throws Exception {
        String bytes_value = uploadToAs(TEST_WAR_PATH);
        String bytes_value2 = uploadToAs(TEST_WAR_PATH);

        assert bytes_value != null;
        assert bytes_value2 != null;
        assert bytes_value.equals(bytes_value2);

        assert bytes_value.equals("7jgpMVmynfxpqp8UDleKLmtgbrA=");
    }

    @Test(timeOut = 60 * 1000L, enabled = isEnabled)
    public void testUploadIndividualSteps() throws Exception {
        String bytes_value = uploadToAs(TEST_WAR_PATH);

        System.out.println("sha: " + bytes_value);

        System.out.println();
        ASConnection connection = getDomainControllerASConnection();

        Address deploymentsAddress = new Address("deployment", TEST_WAR_FILE_NAME);
        Operation op = new Operation("add", deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", bytes_value));
        content.add(contentValues);
        op.addAdditionalProperty("content", content);
        op.addAdditionalProperty("name", TEST_WAR_FILE_NAME); // this needs to be unique per upload
        op.addAdditionalProperty("runtime-name", TEST_WAR_FILE_NAME);
        System.out.flush();
        JsonNode ret = connection.executeRaw(op);
        System.out.println("Add to /deploy done " + ret);
        System.out.flush();

        assert ret.toString().contains("success") : ret;

        Address serverGroupAddress = new Address();
        serverGroupAddress.add("server-group", "main-server-group");
        serverGroupAddress.add("deployment", TEST_WAR_FILE_NAME);

        Operation attach = new Operation("add", serverGroupAddress);//,"enabled","true");
        //        deploy.addAdditionalProperty("runtime-name", TEST_WAR);
        System.out.flush();
        ret = connection.executeRaw(attach);
        System.out.println("Add to server group done: " + ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "add to sg was no success " + ret.getTextValue();

        Operation deploy = new Operation("deploy", serverGroupAddress);
        Result depRes = connection.execute(deploy);

        assert depRes.isSuccess() : "Deploy went wrong: " + depRes.getFailureDescription();

        Thread.sleep(500);

        Operation undeploy = new Operation("undeploy", serverGroupAddress);
        depRes = connection.execute(undeploy);

        assert depRes.isSuccess() : "Undeploy went wrong: " + depRes.getFailureDescription();

        // Now tear down stuff again

        Operation unattach = new Operation("remove", serverGroupAddress);
        ret = connection.executeRaw(unattach);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from sg was no success "
            + ret.getTextValue();

        // remove from domain

        Operation remove = new Operation("remove", deploymentsAddress);
        ret = connection.executeRaw(remove);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from domain was no success "
            + ret.getTextValue();

        System.out.flush();
    }

    // Test for AS7-853
    @Test(timeOut = 60 * 1000L, enabled = isEnabled)
    public void testUploadIndividualSteps2() throws Exception {
        String bytes_value = uploadToAs(TEST_WAR_PATH);

        System.out.println("sha: " + bytes_value);

        System.out.println();
        ASConnection connection = getDomainControllerASConnection();

        Operation op = addDeployment(TEST_WAR_FILE_NAME, bytes_value);
        JsonNode ret = connection.executeRaw(op);

        System.out.println("Add to /deploy done " + ret);
        System.out.flush();

        assert ret.toString().contains("success") : ret;

        Address serverGroupAddress = new Address();
        serverGroupAddress.add("server-group", "main-server-group");
        serverGroupAddress.add("deployment", TEST_WAR_FILE_NAME);

        Operation attach = new Operation("add", serverGroupAddress);
        attach.addAdditionalProperty("enabled", true);
        System.out.flush();
        ret = connection.executeRaw(attach);
        System.out.println("Add to server group done: " + ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "add to sg was no success " + ret.getTextValue();

        Result depRes;// = connection.execute(deploy);

        Thread.sleep(500);

        Operation undeploy = new Operation("undeploy", serverGroupAddress);
        depRes = connection.execute(undeploy);

        assert depRes.isSuccess() : "Undeploy went wrong: " + depRes.getFailureDescription();

        // Now tear down stuff again

        Operation unattach = new Remove(serverGroupAddress);
        ret = connection.executeRaw(unattach);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from sg was no success "
            + ret.getTextValue();

        // remove from domain

        Operation remove = new Remove("deployment", TEST_WAR_FILE_NAME);
        ret = connection.executeRaw(remove);

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "remove from domain was no success "
            + ret.getTextValue();

        System.out.flush();
    }

    @Test(timeOut = 60 * 1000L, enabled = isEnabled)
    public void testUploadComposite() throws Exception {
        String bytes_value = uploadToAs(TEST_WAR_PATH);

        System.out.println("Prepare done");
        System.out.flush();

        Address deploymentsAddress = new Address();
        deploymentsAddress.add("deployment", TEST_WAR_FILE_NAME);
        Operation step1 = new Operation("add", deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", bytes_value));
        content.add(contentValues);
        step1.addAdditionalProperty("content", content);
        step1.addAdditionalProperty("name", TEST_WAR_FILE_NAME); // this needs to be unique per upload

        Address serverGroupAddress = new Address();
        serverGroupAddress.add("server-group", "main-server-group");
        serverGroupAddress.add("deployment", TEST_WAR_FILE_NAME);
        Operation step2 = new Operation("add", serverGroupAddress);// ,"enabled","true");
        Operation step2a = new Operation("deploy", serverGroupAddress);

        Operation step3 = new Operation("undeploy", serverGroupAddress);
        Operation step3a = new Operation("remove", serverGroupAddress);

        Operation step4 = new Operation("remove", deploymentsAddress);

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);
        cop.addStep(step2);
        cop.addStep(step2a);

        ASConnection connection = getDomainControllerASConnection();
        JsonNode ret = connection.executeRaw(cop);
        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite deploy was no success "
            + ret.getTextValue();

        Thread.sleep(1000);

        cop = new CompositeOperation();
        cop.addStep(step3);
        cop.addStep(step3a);
        cop.addStep(step4);
        ret = connection.executeRaw(cop);

        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite remove was no success "
            + ret.getTextValue();
    }

    /**
     * Test uploading to domain only, but not to a server group
     * @throws Exception if anything goes wrong.
     */
    @Test(timeOut = 60 * 1000L, enabled = isEnabled)
    public void testUploadComposite2() throws Exception {
        String bytes_value = uploadToAs(TEST_WAR_PATH);

        System.out.println("Prepare done");
        System.out.flush();

        Address deploymentsAddress = new Address("deployment", TEST_WAR_FILE_NAME);
        Operation step1 = new Operation("add", deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", bytes_value));
        content.add(contentValues);
        step1.addAdditionalProperty("content", content);
        step1.addAdditionalProperty("name", TEST_WAR_FILE_NAME); // this needs to be unique per upload

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);

        ASConnection connection = getDomainControllerASConnection();
        JsonNode ret = connection.executeRaw(cop);
        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite deploy was no success "
            + ret.getTextValue();

        // Wait for AS to settle
        Thread.sleep(1000);

        // Now undeploy again to clean up

        cop = new CompositeOperation();
        Operation step4 = new Operation("remove", deploymentsAddress);

        cop.addStep(step4);
        ret = connection.executeRaw(cop);

        System.out.println(ret);
        System.out.flush();

        assert ret.has("outcome") : "Ret not valid " + ret.toString();
        assert ret.get("outcome").getTextValue().equals("success") : "Composite remove was no success "
            + ret.getTextValue();
    }

    /**
     * Test the real API code for uploading - case 1: just upload to /deployment
     * @throws Exception if anything goes wrong.
     */
    public void testUploadViaCreateChild1() throws Exception {
        BaseComponent bc = new BaseComponent();
        bc.setPath("");
        ResourceType rt = new ResourceType();
        rt.setName("Deployment");
        Resource resource = new Resource("deployment=" + TEST_WAR_FILE_NAME, TEST_WAR_FILE_NAME, rt); // TODO resource key?
        resource.setUuid(UUID.randomUUID().toString());
        StandaloneASComponent parentComponent = new StandaloneASComponent();
        parentComponent.setConnection(getDomainControllerASConnection());
        ResourceContext context = new ResourceContext(resource, parentComponent, null, null, null, null, null, null,
            null, null, null, null, null, null);
        bc.start(context);

        String bytes_value = uploadToAs(TEST_WAR_PATH);

        ResourcePackageDetails details = new ResourcePackageDetails(new PackageDetailsKey(TEST_WAR_FILE_NAME, "1.0",
            "deployment", "all"));
        CreateResourceReport report = new CreateResourceReport(TEST_WAR_FILE_NAME, rt, new Configuration(),
            new Configuration(), details);
        try {
            report = bc.runDeploymentMagicOnServer(report, TEST_WAR_FILE_NAME, TEST_WAR_FILE_NAME, bytes_value);
            assert report != null;
            assert report.getErrorMessage() == null : "Report contained an unexpected error: "
                + report.getErrorMessage();
            assert report.getStatus() != null : "Report did not contain a status";
            assert report.getStatus() == CreateResourceStatus.SUCCESS : "Status was no success";
            assert report.getResourceName().equals(TEST_WAR_FILE_NAME);
            assert report.getResourceKey().equals("deployment=" + TEST_WAR_FILE_NAME);
        } finally {
            Remove r = new Remove("deployment", TEST_WAR_FILE_NAME);
            getDomainControllerASConnection().execute(r);
        }
    }

    /**
     * Test the real API code for uploading - case 2: upload to /deployment and a server group
     * @throws Exception if anything goes wrong.
     */
    public void testUploadViaCreateChild2() throws Exception {
        BaseComponent bc = new BaseComponent();
        bc.setPath("server-group=main-server-group");
        ResourceType rt = new ResourceType();
        rt.setName("Deployment");
        Resource resource = new Resource("server-group=main-server-group", TEST_WAR_FILE_NAME, rt);
        resource.setUuid(UUID.randomUUID().toString());
        StandaloneASComponent parentComponent = new StandaloneASComponent();
        parentComponent.setConnection(getDomainControllerASConnection());
        ResourceContext context = new ResourceContext(resource, parentComponent, null, null, null, null, null, null,
            null, null, null, null, null, null);
        bc.start(context);

        String bytes_value = uploadToAs(TEST_WAR_PATH);

        ResourcePackageDetails details = new ResourcePackageDetails(new PackageDetailsKey(TEST_WAR_FILE_NAME, "1.0",
            "deployment", "all"));
        CreateResourceReport report = new CreateResourceReport(TEST_WAR_FILE_NAME, rt, new Configuration(),
            new Configuration(), details);
        try {
            report = bc.runDeploymentMagicOnServer(report, TEST_WAR_FILE_NAME, TEST_WAR_FILE_NAME, bytes_value);
            assert report != null;
            assert report.getErrorMessage() == null : "Report contained an unexpected error: "
                + report.getErrorMessage();
            assert report.getStatus() != null : "Report did not contain a status";
            assert report.getStatus() == CreateResourceStatus.SUCCESS : "Status was no success";
            assert report.getResourceName().equals(TEST_WAR_FILE_NAME);
            assert report.getResourceKey().equals("server-group=main-server-group,deployment=" + TEST_WAR_FILE_NAME) : "Resource key was wrong";
        } finally {
            Address sgd = new Address("server-group", "main-server-group");
            sgd.add("deployment", TEST_WAR_FILE_NAME);
            Remove r = new Remove(sgd);
            getDomainControllerASConnection().execute(r);
            r = new Remove("deployment", TEST_WAR_FILE_NAME);
            getDomainControllerASConnection().execute(r);
        }
    }

}

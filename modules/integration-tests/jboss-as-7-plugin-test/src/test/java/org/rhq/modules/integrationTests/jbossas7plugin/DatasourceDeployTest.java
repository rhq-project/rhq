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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.DatasourceComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Remove;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

/**
 * Integration test for deploying datasources and installing jdbc drivers
 * @author Heiko W. Rupp
 */
@Test(enabled = UploadAndDeployTest.isEnabled)
public class DatasourceDeployTest extends AbstractIntegrationTest {
    private static final String POSTGRES = "postgres";

//    private final Log log = LogFactory.getLog(DatasourceDeployTest.class);

    private String DRIVER = "postgresql-9.0-801.jdbc4.jar";

    public void uploadOnly() throws Exception {

        String sha1 = uploadToAs(DRIVER);
        assert sha1!=null;

    }

    public void deployToDomain() throws Exception {
        String sha = uploadToAs(DRIVER);
        Operation addDeployment = addDeployment(DRIVER,sha);

        ASConnection conn = getASConnection();

        Result res = conn.execute(addDeployment);
        assert res != null;
        assert res.isSuccess() : " Was not able to add the uploaded file " + DRIVER + " to /deployment";

        Operation op = new ReadChildrenNames(null,"deployment");
        res = conn.execute(op);

        assert res != null;
        assert res.isSuccess();
        List<String> result = (List<String>) res.getResult();
        assert result.contains(DRIVER): "Driver not found in deployments";

        cleanupDomainDeployment(conn);
    }

    /*
     * This test is to make sure that server-one is indeed part of
     * server-group=main-server-group. Purely to check that the assumption
     * about as7 is still correct.
     */
    public void AS7DomainAssumptions() {

        ASConnection connection = getASConnection();
        Operation op = new ReadResource("host","master");
        Result res = connection.execute(op);
        assert res!=null;
        assert res.isSuccess();

        Address address = new Address();
        address.add("host", "master");
        address.add("server-config","server-one");
        op = new ReadResource(address);
        ComplexResult cres = connection.executeComplex(op);
        assert cres!=null;
        assert cres.isSuccess();
        Map<String,Object> result = cres.getResult();
        assert result.containsKey("name");
        assert result.get("name").equals("server-one");
        assert result.containsKey("group");
        assert result.get("group").equals("main-server-group");

        Address defautProfile = new Address("profile","default");
        op = new ReadResource(address);
        res = connection.execute(op);
        assert res!=null;
        assert res.isSuccess() : "Did not find a 'default' profile";

    }


    public void deployDriverToServerGroup() throws Exception {

        ASConnection conn = getASConnection();

        uploadDriverToDomain(conn);
        Result res;

        Address sgAddress = addDriverToMainServerGroup(conn);

        Operation op;
        // Now try to see if this ended up in server-one
        Address address = new Address();
        address.add("host","master");
        address.add("server","server-one");
        address.add("subsystem","datasources");
        op = new Operation("installed-drivers-list",address);
        res = conn.execute(op);
        assert res != null;
        assert res.isSuccess();

        List<Map<String,Object>> list = (List<Map<String, Object>>) res.getResult();
        assert !list.isEmpty();
        boolean found = false;
        for (Map<String,Object> map : list) {
            assert map.containsKey("driver-name");
            if (!map.get("driver-name").equals(DRIVER))
                continue;
            found=true;
            assert map.containsKey("deployment-name");
            assert map.get("deployment-name").equals(DRIVER);
        }
        assert found : "Did not find the driver";


        // Now clean up
        cleanupSGDeployment(conn,sgAddress);
        cleanupDomainDeployment(conn);
    }

    public void createDatasource() throws Exception {

        ASConnection conn = getASConnection();
        uploadDriverToDomain(conn);
        Result res;
        Address sgAddress = addDriverToMainServerGroup(conn);
        Operation op;

        // Now create the data source in the profile, that main-server-group is using.

        Address dsAddress = createDatasource(conn, false);

        System.out.println("Deployed new datasource at " + dsAddress.toString());

        Thread.sleep(1000L); // give some time to settle

        // clean up

        cleanupDatasource(conn,dsAddress);
        cleanupSGDeployment(conn,sgAddress);
        cleanupDomainDeployment(conn);

    }

    public void createXADatasource() throws Exception {

        ASConnection conn = getASConnection();
        uploadDriverToDomain(conn);
        Result res;
        Address sgAddress = addDriverToMainServerGroup(conn);
        Operation op;

        // Now create the data source in the profile, that main-server-group is using.

        Address dsAddress = createDatasource(conn, true);

        System.out.println("Deployed new xa-datasource at " + dsAddress.toString());

        Thread.sleep(1000L); // give some time to settle

        // clean up

        cleanupDatasource(conn,dsAddress);
        cleanupSGDeployment(conn,sgAddress);
        cleanupDomainDeployment(conn);
    }


    public void updateDatasource() throws Exception {
        ASConnection conn = getASConnection();
        uploadDriverToDomain(conn);
        Result res;
        Address sgAddress = addDriverToMainServerGroup(conn);
        Operation op;

        // Now create the data source in the profile, that main-server-group is using.

        Address dsAddress = createDatasource(conn, false);

        System.out.println("Deployed new datasource at " + dsAddress.toString());

        Thread.sleep(1000L); // give some time to settle

        op = new WriteAttribute(dsAddress,"max-pool-size",20);
        res = conn.execute(op);
        assert res != null;
        assert res.isSuccess(): "Updating the max-pool-size did not work: " + res.getFailureDescription();

        cleanupDatasource(conn,dsAddress);
        cleanupSGDeployment(conn,sgAddress);
        cleanupDomainDeployment(conn);

    }

    public void deployDatasourceViaOperation() throws Exception {
        ASConnection conn = getASConnection();
        uploadDriverToDomain(conn);
        Address sgAddress = addDriverToMainServerGroup(conn);

        // Now create the data source in the profile, that main-server-group is using.

        String name = "myTestDS";

        DatasourceComponent dc = new DatasourceComponent();
        dc.setPath("profile=default,subsystem=datasources");
        dc.setConnection(conn);

        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("name",name));
        parameters.put(new PropertySimple("driver-name",DRIVER));
        parameters.put(new PropertySimple("pool-name","pgPool"));
        parameters.put(new PropertySimple("connection-url","jdbc:postgresql:foo@localhost:5432"));
        parameters.put(new PropertySimple("jndi-name","postgresDS"));
        OperationResult operationResult = dc.invokeOperation("addDatasource",parameters);
        assert operationResult != null;
        assert operationResult.getSimpleResult()!=null;
        assert operationResult.getErrorMessage()==null;

        Address dsAddress = new Address();
        dsAddress.add("profile","default");
        dsAddress.add("subsystem","datasources");
        dsAddress.add("data-source",name);


        System.out.println("Deployed new datasource at " + dsAddress.toString());

        Thread.sleep(1000L); // give some time to settle

        cleanupDatasource(conn, dsAddress);
        cleanupSGDeployment(conn, sgAddress);
        cleanupDomainDeployment(conn);


    }

    public void deployXADatasourceViaOperation() throws Exception {
        ASConnection conn = getASConnection();
        uploadDriverToDomain(conn);
        Result res;
        Address sgAddress = addDriverToMainServerGroup(conn);
        Operation op;

        // Now create the data source in the profile, that main-server-group is using.

        String name = "myTestDS";

        DatasourceComponent dc = new DatasourceComponent();
        dc.setPath("profile=default,subsystem=datasources");
        dc.setConnection(conn);

        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("name",name));
        parameters.put(new PropertySimple("driver-name",DRIVER));
        parameters.put(new PropertySimple("pool-name","pgPool"));
        parameters.put(new PropertySimple("connection-url","jdbc:postgresql:foo@localhost:5432"));
        parameters.put(new PropertySimple("jndi-name","postgresDS"));
        parameters.put(new PropertySimple("xa-data-source-class","org.postgres.XA.driver"));
        OperationResult operationResult = dc.invokeOperation("addXADatasource",parameters);
        assert operationResult != null;
        assert operationResult.getSimpleResult()!=null ;
        assert operationResult.getErrorMessage()==null;

        Address dsAddress = new Address();
        dsAddress.add("profile","default");
        dsAddress.add("subsystem","datasources");
        dsAddress.add("xa-data-source",name);


        System.out.println("Deployed new xa-datasource at " + dsAddress.toString());

        Thread.sleep(1000L); // give some time to settle

        cleanupDatasource(conn, dsAddress);
        cleanupSGDeployment(conn, sgAddress);
        cleanupDomainDeployment(conn);


    }

    public void flushDSviaOperation() throws Exception {
        // This test executes the "fall through" operation in the DatasourceComponent

        String theOperation = "flush-all-connection-in-pool";

        ASConnection conn = getASConnection();
        uploadDriverToDomain(conn);
        Address sgAddress = addDriverToMainServerGroup(conn);

        // Now create the data source in the profile, that main-server-group is using.
        Address dsAddress = createDatasource(conn, false);

        System.out.println("Deployed new datasource at " + dsAddress.toString());

        Thread.sleep(1000L); // give some time to settle

        // Now invoke the operation
        DatasourceComponent dc = new DatasourceComponent();
        dc.path="profile=default,subsystem=datasources,data-source=" + POSTGRES;
        dc.connection = conn;

        Configuration parameters = new Configuration();
        // Operation takes no parameters
        OperationResult operationResult = dc.invokeOperation(theOperation,parameters);
        assert operationResult != null;
        assert operationResult.getSimpleResult()!=null;
        assert operationResult.getErrorMessage()==null;


        cleanupDatasource(conn,dsAddress);
        cleanupSGDeployment(conn,sgAddress);
        cleanupDomainDeployment(conn);

    }

    private void cleanupDomainDeployment(ASConnection conn) {
        Operation op;
        Result res;
        Address deployment = new Address("deployment",DRIVER);
        op = new Remove(deployment);
        res = conn.execute(op);
        assert res != null;
        assert res.isSuccess() : "Could not remove driver from /deployment";
    }

    private void cleanupSGDeployment(ASConnection conn, Address sgAddress) {
        Operation op;Result res;
        op = new Remove(sgAddress);
        res = conn.execute(op);
        assert res != null;
        assert res.isSuccess() : "Could not remove driver from server group @ " + sgAddress;
    }

    private void cleanupDatasource(ASConnection conn, Address dsAddress) {
        Operation op;Result res;
        op = new Remove(dsAddress);
        res = conn.execute(op);
        assert res != null;
        assert res.isSuccess() : "Could not remove datasource from profile @ " + dsAddress;
    }

    private Address createDatasource(ASConnection conn, boolean isXa) {
        Operation op;
        Result res;
        Address dsAddress = new Address("profile","default");
        dsAddress.add("subsystem","datasources");
        if (isXa)
            dsAddress.add("xa-data-source", POSTGRES);
        else
            dsAddress.add("data-source", POSTGRES);

        op = new Operation("add",dsAddress);
        op.addAdditionalProperty("driver-name",DRIVER);
        op.addAdditionalProperty("jndi-name","postgresDS");
        op.addAdditionalProperty("pool-name","pgPool");
        op.addAdditionalProperty("connection-url","jdbc:postgresql://127.0.0.1:5432/rhqdev");
        if (isXa) {
            op.addAdditionalProperty("xa-data-source-class","org.postgresql.xa.PGXADataSource");
            Map<String,String> map = new HashMap<String, String>(1); // TODO AS7-1209
            map.put("key","value");
            op.addAdditionalProperty("xa-data-source-properties",map);
        }

        res = conn.execute(op);
        assert res != null;
        assert res.isSuccess() : "Could not add driver to profile default: " + res.getFailureDescription();
        return dsAddress;
    }

    private Address addDriverToMainServerGroup(ASConnection conn) {
        Result res;// then add the driver to the server-group we want to have the DS on
        Address sgAddress = new Address();
        sgAddress.add("server-group", "main-server-group");
        sgAddress.add("deployment", DRIVER);

        Operation op = new Operation("add",sgAddress);
        op.addAdditionalProperty("enabled",true);
        res = conn.execute(op);
        assert res != null;
        assert res.isSuccess() : "Was not able to add the driver to the serer-group: " + res.getFailureDescription();
        return sgAddress;
    }

    private void uploadDriverToDomain(ASConnection conn) throws IOException {
        // first upload driver and add to /deployment
        String sha = uploadToAs(DRIVER);
        Operation addDeployment = addDeployment(DRIVER,sha);
        Result res = conn.execute(addDeployment);
        assert res != null;
        assert res.isSuccess() : " Was not able to add the uploaded file " + DRIVER + " to /deployment";
    }

}

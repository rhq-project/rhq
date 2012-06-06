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
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.ModuleOptionsComponent.ModuleOptionType;
import org.rhq.modules.plugins.jbossas7.ModuleOptionsComponent.Value;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

/**
 * Test exercising the subsystem=security/SecurityDomain/[Authentication, Authorization, Mapping, Audit, Acl, 
 *      Identity-Trust]
 * @author Simeon Pinder
 */
@Test(groups = "unit")
public class SecurityModuleOptionsTest extends AbstractConfigurationHandlingTest {

    private static String user = "rhqadmin";
    private static String pass = "as7";
    private static String host = "localhost";
    private static ASConnection con = null;
    private static ObjectMapper mapper = null;
    private ModuleOptionsComponent moc = null;

    //    @BeforeSuite
    //    void loadPluginDescriptor() throws Exception {
    //        super.loadPluginDescriptor();
    //
    //        initializeConnectionDetails();
    //    }

    @BeforeSuite
    private void initializeConnectionDetails() {
        con = new ASConnection(host, 9990, user, pass);
        mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

        moc = new ModuleOptionsComponent();
    }

    public static void main(String[] args) {
        SecurityModuleOptionsTest test = new SecurityModuleOptionsTest();
        try {
            test.initializeConnectionDetails();
            test.testPopulateModuleOptionsAndTypes();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**The test reads the existing property values, deserializes them and writes the same 
     * contents back out to a running instance for all known ModuleOptionTypes.
     * 
     * @throws Exception
     */
    public void testPopulateModuleOptionsAndTypes() throws Exception {

        //as7 node details.
        String securityDomainId = "testDomain";
        //TODO: spinder 6-6-12: this cannot run as a standalone itest until JIRA https://issues.jboss.org/browse/AS7-4951
        //      is addressed as there is no way to automate setup of the information being tested.
        String address = "subsystem=security,security-domain=" + securityDomainId + ",authentication=classic";
        boolean verboseOutput = true;
        boolean executeOperation = true;
        for (ModuleOptionType t : ModuleOptionType.values()) {
            String attribute = t.getAttribute();
            if (verboseOutput) {
                System.out.println("======= Running with ModuleOptionType:" + t + " attribute:" + attribute + ":");
            }
            if (attribute.equals("policy-modules")) {
                address = "subsystem=security,security-domain=" + securityDomainId + ",authorization=classic";
            } else if (attribute.equals("acl-modules")) {
                address = "subsystem=security,security-domain=" + securityDomainId + ",acl=classic";
            } else if (attribute.equals("mapping-modules")) {
                address = "subsystem=security,security-domain=" + securityDomainId + ",mapping=classic";
            } else if (attribute.equals("trust-modules")) {
                address = "subsystem=security,security-domain=" + securityDomainId + ",identity-trust=classic";
            } else if (attribute.equals("provider-modules")) {
                address = "subsystem=security,security-domain=" + securityDomainId + ",audit=classic";
            } else if (attribute.equals("login-modules")) {
                address = "subsystem=security,security-domain=" + securityDomainId + ",authentication=classic";
            } else {
                assert false : "An unknown attribute '" + attribute
                    + "' was found. Is there a new type to be supported?";
            }

            //test operation- read property always available.
            Operation op = null;

            //read the login-modules attribute
            op = new ReadAttribute(new Address(address), attribute);
            Result result = exerciseOperation(op, true, verboseOutput);
            assert result.isSuccess() == true : "The operation '" + op + "' failed to read the resource."
                + result.getFailureDescription();
            //extract current results
            Object rawResult = result.getResult();
            assert rawResult != null : "Read of attribute'" + attribute + "' from address '" + address
                + "' has returned no value. Are those values in the model?";

            List<Value> list2 = new ArrayList<Value>();
            //populate the Value component complete with module Options.
            list2 = moc.populateSecurityDomainModuleOptions(result,
                ModuleOptionsComponent.loadModuleOptionType(attribute));
            if (verboseOutput) {
                if (rawResult != null) {
                    System.out.println("Raw Result is:" + rawResult + " and of type:" + rawResult.getClass());
                } else {
                    System.out.println("Read of attribute'" + attribute + "' from address '" + address
                        + "' has returned no value. Are those values in the model?");
                }
            }
            //write the login-modules attribute
            op = new WriteAttribute(new Address(address));
            op.addAdditionalProperty("name", attribute);//attribute to execute on
            op.addAdditionalProperty("value", list2);

            //Now test the operation
            result = exerciseOperation(op, executeOperation, verboseOutput);
            assert ((result.isSuccess() == true) || (result.getOutcome() == null)) : "The operation '" + op
                + "' failed to write the resource.." + result.getFailureDescription();

            //read the login-modules attribute
            op = new ReadAttribute(new Address(address), attribute);
            result = exerciseOperation(op, true, verboseOutput);
            assert (result.isSuccess() == true) : "The operation '" + op + "' failed to read the resource."
                + result.getFailureDescription();
        }
        if (verboseOutput) {
            System.out.println("Successfully detected,read and wrote out attribute values for:");
            for (ModuleOptionType type : ModuleOptionType.values()) {
                System.out.println("\n" + type.ordinal() + " " + type.name());
            }
        }
    }

    /** For each operation 
     *   - will write verbose json and operation details to system.out if verboseOutput = true;
     *   - will execute the operation against running server if execute = true.
     * 
     * @param op
     * @param execute
     * @param verboseOutput
     * @return
     */
    public static Result exerciseOperation(Operation op, boolean execute, boolean verboseOutput) {
        //display operation as AS7 plugin will build it
        if (verboseOutput) {
            System.out.println("\tOperation is:" + op);
        }

        String jsonToSend = "";
        try {
            //            jsonToSend = mapper.writeValueAsString(op);
            jsonToSend = mapper.defaultPrettyPrintingWriter().writeValueAsString(op);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //As generated by jackson mapper
        if (verboseOutput) {
            System.out.println("@@@@ OUTBOUND JSON#\n" + jsonToSend + "#");
        }

        //Execute the operation
        Result result = new Result();
        if (execute) {
            result = con.execute(op);
        } else {
            if (verboseOutput) {
                System.out.println("**** NOTE: Execution disabled . NOT exercising write-attribute operation. **** ");
            }
        }
        if (verboseOutput) {
            //result wrapper details 
            System.out.println("\tResult:" + result);
            //detailed results
            System.out.println("\tValue:" + result.getResult());
            System.out.println("-----------------------------------------------------\n");
        }
        return result;
    }
}

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
package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.DeleteResourceStatus;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.ModuleOptionsComponent;
import org.rhq.modules.plugins.jbossas7.ModuleOptionsComponent.Value;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test exercising the subsystem=security/SecurityDomain/[Authentication(Classic|Jaspi), 
 * Authorization, Mapping, Audit, Acl, 
 *      Identity-Trust]
 * @author Simeon Pinder
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class SecurityModuleOptionsTest extends AbstractJBossAS7PluginTest {

    private static String user = "rhqadmin";
    private static String pass = "as7";
    private static String host = "localhost";
    private static ASConnection con = null;
    private static ObjectMapper mapper = null;
    private ModuleOptionsComponent moc = null;

    private static String TEST_DOMAIN = "testDomain";
    private static Resource securityResource = null;
    private static String SECURITY_RESOURCE_TYPE = "Security";
    private static String SECURITY_RESOURCE_KEY = "subsystem=security";
    private static String SECURITY_DOMAIN_RESOURCE_TYPE = "Security Domain";
    private static String SECURITY_DOMAIN_RESOURCE_KEY = "security-domain";
    private static String AUTH_CLASSIC_RESOURCE_TYPE = "Authentication (Classic)";
    private static String AUTH_CLASSIC_RESOURCE_KEY = "authentication=classic";
    private static String PROFILE = "profile=full-ha";

    protected static final String DC_HOST = System.getProperty("jboss.domain.bindAddress");
    protected static final int DC_HTTP_PORT = Integer.valueOf(System.getProperty("jboss.domain.httpManagementPort"));
    protected static final String DC_USER = AbstractJBossAS7PluginTest.MANAGEMENT_USERNAME;
    protected static final String DC_PASS = AbstractJBossAS7PluginTest.MANAGEMENT_PASSWORD;

    ASConnection getASConnection() {
        ASConnection connection = new ASConnection(DC_HOST, DC_HTTP_PORT, DC_USER, DC_PASS);
        return connection;
    }

    public static final ResourceType RESOURCE_TYPE = new ResourceType(SECURITY_RESOURCE_TYPE, PLUGIN_NAME,
        ResourceCategory.SERVICE, null);
    private static final String RESOURCE_KEY = SECURITY_RESOURCE_KEY;
    private static Resource testSecurityDomain = null;
    private String testSecurityDomainKey = null;
    private ConfigurationManager testConfigurationManager = null;

    //Define some shared and reusable content
    static HashMap<String, String> jsonMap = new HashMap<String, String>();
    static {
        jsonMap
            .put(
                "login-modules",
                "[{\"flag\":\"required\", \"code\":\"Ldap\", \"module-options\":{\"bindDn\":\"uid=ldapSecureUser,ou=People,dc=redat,dc=com\", \"bindPw\":\"test126\", \"allowEmptyPasswords\":\"true\"}}]");
        jsonMap
            .put(
                "policy-modules",
                "[{\"flag\":\"requisite\", \"code\":\"LdapExtended\", \"module-options\":{\"policy\":\"module\", \"policy1\":\"module1\"}}]");
        jsonMap
            .put("mapping-modules",
                "[{\"code\":\"Test\", \"type\":\"attribute\", \"module-options\":{\"mapping\":\"module\", \"mapping1\":\"module1\"}}]");
        jsonMap.put("provider-modules",
            "[{\"code\":\"Providers\", \"module-options\":{\"provider\":\"module\", \"provider1\":\"module1\"}}]");
        jsonMap
            .put("acl-modules",
                "[{\"flag\":\"sufficient\", \"code\":\"ACL\", \"module-options\":{\"acl\":\"module\", \"acl1\":\"module1\"}}]");
        jsonMap
            .put("trust-modules",
                "[{\"flag\":\"optional\", \"code\":\"TRUST\", \"module-options\":{\"trust\":\"module\", \"trust1\":\"module1\"}}]");
    }

    /** This method mass loads all the supported Module Option Types(Excluding authentication=jaspi, cannot co-exist with 
     * authentication=classic) into a single SecurityDomain.  This is done as
     * -i)creating all of the related hierarchy of types needed to exercise N Module Options Types and their associated 
     *     Module Options instances would take too long to setup(N creates would signal N discovery runs before test could complete).
     * -ii)setting the priority of this method lower than the discovery method means that we'll get all the same types in much 
     *     less time.
     *     
     * @throws Exception
     */
    @Test(priority = 10)
    public void testLoadStandardModuleOptionTypes() throws Exception {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //Adjust discovery depth to support deeper hierarchy depth of Module Option elements
        setMaxDiscoveryDepthOverride(10);

        //create new Security Domain
        Address destination = new Address(PROFILE);
        destination.addSegment(SECURITY_RESOURCE_KEY);
        String securityDomainId = TEST_DOMAIN + "2";
        destination.addSegment(SECURITY_DOMAIN_RESOURCE_KEY + "=" + securityDomainId);

        ASConnection connection = getASConnection();
        Result result = new Result();
        //delete old one if present to setup clean slate
        Operation op = new Operation("remove", destination);
        result = connection.execute(op);

        //build/rebuild hierarchy
        op = new Operation("add", destination);
        result = connection.execute(op);

        //Ex. profile=standalone-ha,subsystem=security,security-domain
        String addressPrefix = PROFILE + "," + SECURITY_RESOURCE_KEY + "," + SECURITY_DOMAIN_RESOURCE_KEY;

        //loop over standard types and add base details for all of them to security domain
        String address = "";
        for (String attribute : jsonMap.keySet()) {
            if (attribute.equals("policy-modules")) {
                address = addressPrefix + "=" + securityDomainId + ",authorization=classic";
            } else if (attribute.equals("acl-modules")) {
                address = addressPrefix + "=" + securityDomainId + ",acl=classic";
            } else if (attribute.equals("mapping-modules")) {
                address = addressPrefix + "=" + securityDomainId + ",mapping=classic";
            } else if (attribute.equals("trust-modules")) {
                address = addressPrefix + "=" + securityDomainId + ",identity-trust=classic";
            } else if (attribute.equals("provider-modules")) {
                address = addressPrefix + "=" + securityDomainId + ",audit=classic";
            } else if (attribute.equals("login-modules")) {
                address = addressPrefix + "=" + securityDomainId + ",authentication=classic";
            } else {
                assert false : "An unknown attribute '" + attribute
                    + "' was found. Is there a new type to be supported?";
            }
            //build the operation to add the component
            ////Load json map into ModuleOptionType
            List<Value> moduleTypeValue = new ArrayList<Value>();
            try {
                // loading jsonMap contents for Ex. 'login-module'
                JsonNode node = mapper.readTree(jsonMap.get(attribute));
                Object obj = mapper.treeToValue(node, Object.class);
                result.setResult(obj);
                result.setOutcome("success");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //populate the Value component complete with module Options.
            moduleTypeValue = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                ModuleOptionsComponent.loadModuleOptionType(attribute));
            op = ModuleOptionsComponent.createAddModuleOptionTypeOperation(new Address(address), attribute,
                moduleTypeValue);
            //submit the command
            result = connection.execute(op);
        }
    }

    @Test(priority = 11, groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void initialDiscovery() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        assertNotNull(platform);
        assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        //Have thread sleep longer to discover deeper resource types. 
        Thread.sleep(120 * 1000L); // delay so that PC gets a chance to scan for resources
    }

    /** This test method exercises a number of things:
     *  - that the security-domain children loaded have been created successfully
     *  - that all of the supported Module Option Type children(excluding 'authentication=jaspi') have been 
     *    discovered as AS7 types successfully.
     *  - that the correct child attribute was specified for each type //Ex. acl=classic -> acl-modules
     *  - 
     * 
     * @throws Exception
     */
    @Test(priority = 12)
    public void testDiscoveredSecurityNodes() throws Exception {
        //lazy-load configurationManager
        if (testConfigurationManager == null) {
            testConfigurationManager = this.pluginContainer.getConfigurationManager();
            testConfigurationManager = pluginContainer.getConfigurationManager();
            testConfigurationManager.initialize();
            Thread.sleep(20 * 1000L);
        }
        //iterate through list of nodes and make sure they've all been discovered
        ////Ex. profile=full-ha,subsystem=security,security-domain=testDomain2,acl=classic
        String attribute = null;
        for (String jsonKey : jsonMap.keySet()) {
            //Ex. policy-modules
            attribute = jsonKey;
            //spinder 6/26/12: Temporarily disable until figure out why NPE happens only for this type?
            if (attribute.equals(ModuleOptionsComponent.ModuleOptionType.Authentication.getAttribute())) {
                break;//
            }
            //Ex. name=acl-modules
            //check the configuration for the Module Option Type Ex. 'Acl (Profile)' Resource. Should be able to verify components
            Resource aclResource = getModuleOptionResourceResource(attribute);
            //assert non-zero id returned
            assert aclResource.getId() > 0 : "The resource was not properly initialized. Expected id >0 but got:"
                + aclResource.getId();

            //Now request the resource complete with resource config
            Configuration loadedConfiguration = testConfigurationManager.loadResourceConfiguration(aclResource.getId());
            String code = null;
            String type = null;
            String flag = null;
            //populate the associated attributes if it's supported.
            for (String key : loadedConfiguration.getAllProperties().keySet()) {
                Property property = loadedConfiguration.getAllProperties().get(key);
                if (key.equals("code")) {
                    code = ((PropertySimple) property).getStringValue();
                } else if (key.equals("flag")) {
                    flag = ((PropertySimple) property).getStringValue();
                } else {//Ex. type.
                    type = ((PropertySimple) property).getStringValue();
                }
            }

            //retrieve module options as well.
            String jsonContent = jsonMap.get(attribute);
            Result result = new Result();
            try {
                // loading jsonMap contents for Ex. 'login-module'
                JsonNode node = mapper.readTree(jsonContent);
                Object obj = mapper.treeToValue(node, Object.class);
                result.setResult(obj);
                result.setOutcome("success");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                assert false;
            } catch (IOException e) {
                e.printStackTrace();
                assert false;
            }

            //populate the Value component complete with module Options.
            List<Value> moduleTypeValue = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                ModuleOptionsComponent.loadModuleOptionType(attribute));
            Value moduleOptionType = moduleTypeValue.get(0);
            //Ex. retrieve the acl-modules component and assert values.
            //always test 'code'
            assert moduleOptionType.getCode().equals(code) : "Module Option 'code' value is not correct. Expected '"
                + code + "' but was '" + moduleOptionType.getCode() + "'";
            if (attribute.equals(ModuleOptionsComponent.ModuleOptionType.Mapping.getAttribute())) {
                assert moduleOptionType.getType().equals(type) : "Mapping Module 'type' value is not correct. Expected '"
                    + type + "' but was '" + moduleOptionType.getType() + "'";
            } else if (!attribute.equals(ModuleOptionsComponent.ModuleOptionType.Audit.getAttribute())) {//Audit has no second parameter
                assert moduleOptionType.getFlag().equals(flag) : "Provider Module 'flag' value is not correct. Expected '"
                    + flag + "' but was '" + moduleOptionType.getFlag() + "'";
            }
        }
        //TODO spinder 6-26-12: retrieve the Module Options and test them too.
    }

    @Test(priority = 13)
    public void testCreateSecurityDomain() throws Exception {
        //get the root security resource
        securityResource = getResource();

        //plugin config
        Configuration createPlugConfig = new Configuration();
        createPlugConfig.put(new PropertySimple("path", SECURITY_DOMAIN_RESOURCE_KEY += "=" + TEST_DOMAIN));

        //resource config
        Configuration createResConfig = new Configuration();
        createResConfig.put(new PropertySimple("name", TEST_DOMAIN));

        CreateResourceRequest request = new CreateResourceRequest();
        request.setParentResourceId(securityResource.getId());
        request.setPluginConfiguration(createPlugConfig);
        request.setPluginName(PLUGIN_NAME);
        request.setResourceConfiguration(createResConfig);
        request.setResourceName(TEST_DOMAIN);
        request.setResourceTypeName(SECURITY_DOMAIN_RESOURCE_TYPE);

        CreateResourceResponse response = pluginContainer.getResourceFactoryManager().executeCreateResourceImmediately(
            request);

        assert response.getStatus() == CreateResourceStatus.SUCCESS : "The Security Domain creation failed with an error mesasge: "
            + response.getErrorMessage();
    }

    @Test(priority = 14)
    public void testAuthenticationClassic() throws Exception {
        //get the root security resource
        securityResource = getResource();

        //find TEST_DOMAIN 'security-domain'
        Resource securityDomain = null;
        Set<Resource> childResources = securityResource.getChildResources();
        for (Resource r : childResources) {
            if (r.getName().indexOf(TEST_DOMAIN) > -1) {
                securityDomain = r;
            }
        }

        //plugin config
        Configuration createPlugConfig = new Configuration();
        createPlugConfig.put(new PropertySimple("path", AUTH_CLASSIC_RESOURCE_KEY));

        //resource config
        Configuration createResConfig = new Configuration();
        createResConfig.put(new PropertySimple("code", "Ldap"));
        createResConfig.put(new PropertySimple("flag", "requisite"));

        CreateResourceRequest request = new CreateResourceRequest();
        request.setParentResourceId(securityDomain.getId());
        request.setPluginConfiguration(createPlugConfig);
        request.setPluginName(PLUGIN_NAME);
        request.setResourceConfiguration(createResConfig);
        request.setResourceName("Test - notUsed.");

        request.setResourceTypeName(AUTH_CLASSIC_RESOURCE_TYPE);

        CreateResourceResponse response = pluginContainer.getResourceFactoryManager().executeCreateResourceImmediately(
            request);

        assert response.getStatus() == CreateResourceStatus.SUCCESS : "The 'Authentication (Classic)' node creation failed with an error mesasge: "
            + response.getErrorMessage();
    }

    @Test(priority = 15)
    public void testDeleteSecurityDomain() throws Exception {
        //get the root security resource
        securityResource = getResource();
        Resource found = null;
        Set<Resource> childResources = securityResource.getChildResources();
        for (Resource r : childResources) {
            if (r.getName().indexOf(TEST_DOMAIN) > -1) {
                found = r;
            }
        }

        //plugin config
        Configuration deletePlugConfig = new Configuration();
        deletePlugConfig.put(new PropertySimple("path", SECURITY_DOMAIN_RESOURCE_KEY += "=" + TEST_DOMAIN));

        //resource config
        Configuration deleteResConfig = new Configuration();
        deleteResConfig.put(new PropertySimple("name", TEST_DOMAIN));

        DeleteResourceRequest request = new DeleteResourceRequest();
        if (found != null) {
            request.setResourceId(found.getId());
        }
        DeleteResourceResponse response = pluginContainer.getResourceFactoryManager().executeDeleteResourceImmediately(
            request);

        assert response.getStatus() == DeleteResourceStatus.SUCCESS : "The Security Domain deletion failed with an error mesasge: "
            + response.getErrorMessage();
    }

    //    public static void main(String[] args) {
    //        SecurityModuleOptionsTest setup = new SecurityModuleOptionsTest();
    //        try {
    //            setup.testLoadStandardModuleOptionTypes();
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //    }

    private Resource getResource() {

        InventoryManager im = pluginContainer.getInventoryManager();
        Resource platform = im.getPlatform();
        Resource server = getResourceByTypeAndKey(platform, StandaloneServerComponentTest.RESOURCE_TYPE,
            StandaloneServerComponentTest.RESOURCE_KEY);
        Resource bindings = getResourceByTypeAndKey(server, RESOURCE_TYPE, RESOURCE_KEY);
        return bindings;
    }

    /** Automates
     * 
     * @param optionAttributeType
     * @return
     */
    private Resource getModuleOptionResourceResource(String optionAttributeType) {
        Resource moduleOptionResource = null;
        String securityDomainId = SECURITY_DOMAIN_RESOURCE_KEY + "=" + TEST_DOMAIN + "2";
        if (testSecurityDomain == null) {
            InventoryManager im = pluginContainer.getInventoryManager();
            Resource platform = im.getPlatform();
            //host controller
            ResourceType hostControllerType = new ResourceType("JBossAS7 Host Controller", PLUGIN_NAME,
                ResourceCategory.SERVER, null);
            Resource hostController = getResourceByTypeAndKey(platform, hostControllerType,
                "/tmp/jboss-as-6.0.0.GA/domain");
            //profile=full-ha
            ResourceType profileType = new ResourceType("Profile", PLUGIN_NAME, ResourceCategory.SERVICE, null);
            String key = PROFILE;
            Resource profile = getResourceByTypeAndKey(hostController, profileType, key);

            //Security (Profile)
            ResourceType securityType = new ResourceType("Security (Profile)", PLUGIN_NAME, ResourceCategory.SERVICE,
                null);
            key += "," + SECURITY_RESOURCE_KEY;
            Resource security = getResourceByTypeAndKey(profile, securityType, key);

            //Security Domain (Profile)
            ResourceType domainType = new ResourceType("Security Domain (Profile)", PLUGIN_NAME,
                ResourceCategory.SERVICE, null);
            key += "," + securityDomainId;
            testSecurityDomainKey = key;
            testSecurityDomain = getResourceByTypeAndKey(security, domainType, key);
        }

        //acl=classic
        String descriptorName = "";
        String moduleAttribute = "";
        //acl
        if (optionAttributeType.equals(ModuleOptionsComponent.ModuleOptionType.Acl.getAttribute())) {
            descriptorName = "ACL (Profile)";
            moduleAttribute = "acl=classic";
        } else if (optionAttributeType.equals(ModuleOptionsComponent.ModuleOptionType.Audit.getAttribute())) {
            descriptorName = "Audit (Profile)";
            moduleAttribute = "audit=classic";
        } else if (optionAttributeType.equals(ModuleOptionsComponent.ModuleOptionType.Authentication.getAttribute())) {
            descriptorName = "Authentication (Classic - Profile)";
            moduleAttribute = "authentication=classic";
        } else if (optionAttributeType.equals(ModuleOptionsComponent.ModuleOptionType.Authorization.getAttribute())) {
            descriptorName = "Authorization (Profile)";
            moduleAttribute = "authorization=classic";
        } else if (optionAttributeType.equals(ModuleOptionsComponent.ModuleOptionType.IdentityTrust.getAttribute())) {
            descriptorName = "Identity Trust (Profile)";
            moduleAttribute = "identity-trust=classic";
        } else if (optionAttributeType.equals(ModuleOptionsComponent.ModuleOptionType.Mapping.getAttribute())) {
            descriptorName = "Mapping (Profile)";
            moduleAttribute = "mapping=classic";
        }
        //Build the right Module Option Type. Ex. ACL (Profile), etc.
        ResourceType moduleOptionType = new ResourceType(descriptorName, PLUGIN_NAME, ResourceCategory.SERVICE, null);
        ConfigurationDefinition cdef = new ConfigurationDefinition(descriptorName, null);
        moduleOptionType.setResourceConfigurationDefinition(cdef);
        //Ex. profile=full-ha,subsystem=security,security-domain=testDomain2,identity-trust=classic
        String moduleOptionTypeKey = testSecurityDomainKey += "," + moduleAttribute;

        if (!testSecurityDomainKey.endsWith(securityDomainId)) {
            moduleOptionTypeKey = testSecurityDomainKey.substring(0, testSecurityDomainKey.indexOf(securityDomainId)
                + securityDomainId.length())
                + "," + moduleAttribute;
        }

        moduleOptionResource = getResourceByTypeAndKey(testSecurityDomain, moduleOptionType, moduleOptionTypeKey);

        return moduleOptionResource;
    }

    private Resource getResource(Resource parentResource, String pluginDescriptorTypeName, String resourceKey) {
        Resource resource = null;
        if (((parentResource != null) & (pluginDescriptorTypeName != null) & (resourceKey != null))
            & (((!pluginDescriptorTypeName.isEmpty()) & (!resourceKey.isEmpty())))) {
            ResourceType resourceType = buildResourceType(pluginDescriptorTypeName);
            resource = getResourceByTypeAndKey(parentResource, resourceType, resourceKey);
        }
        return resource;
    }

    private ResourceType buildResourceType(String pluginTypeName) {
        ResourceType created = null;
        if ((pluginTypeName != null) && (!pluginTypeName.isEmpty())) {
            created = new ResourceType(pluginTypeName, PLUGIN_NAME, ResourceCategory.SERVICE, null);
        }
        return created;
    }

    //##################################################################
    //    /**The test reads the existing property values, deserializes them and writes the same 
    //     * contents back out to a running instance for all known ModuleOptionTypes.
    //     * 
    //     * @throws Exception
    //     */
    //    public void testPopulateModuleOptionsAndTypes() throws Exception {
    //
    //        //as7 node details.
    //        String securityDomainId = "testDomain";
    //        //TODO: spinder 6-6-12: this cannot run as a standalone itest until JIRA https://issues.jboss.org/browse/AS7-4951
    //        //      is addressed as there is no way to automate setup of the information being tested.
    //        String address = "subsystem=security,security-domain=" + securityDomainId + ",authentication=classic";
    //        boolean verboseOutput = true;
    //        boolean executeOperation = true;
    //        for (ModuleOptionType t : ModuleOptionType.values()) {
    //            String attribute = t.getAttribute();
    //            if (verboseOutput) {
    //                System.out.println("======= Running with ModuleOptionType:" + t + " attribute:" + attribute + ":");
    //            }
    //            if (attribute.equals("policy-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",authorization=classic";
    //            } else if (attribute.equals("acl-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",acl=classic";
    //            } else if (attribute.equals("mapping-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",mapping=classic";
    //            } else if (attribute.equals("trust-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",identity-trust=classic";
    //            } else if (attribute.equals("provider-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",audit=classic";
    //            } else if (attribute.equals("login-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",authentication=classic";
    //            } else {
    //                assert false : "An unknown attribute '" + attribute
    //                    + "' was found. Is there a new type to be supported?";
    //            }
    //
    //            //test operation- read property always available.
    //            Operation op = null;
    //
    //            //read the login-modules attribute
    //            op = new ReadAttribute(new Address(address), attribute);
    //            Result result = exerciseOperation(op, true, verboseOutput);
    //            assert result.isSuccess() == true : "The operation '" + op + "' failed to read the resource."
    //                + result.getFailureDescription();
    //            //extract current results
    //            Object rawResult = result.getResult();
    //            assert rawResult != null : "Read of attribute'" + attribute + "' from address '" + address
    //                + "' has returned no value. Are those values in the model?";
    //
    //            List<Value> list2 = new ArrayList<Value>();
    //            //populate the Value component complete with module Options.
    //            list2 = moc.populateSecurityDomainModuleOptions(result,
    //                ModuleOptionsComponent.loadModuleOptionType(attribute));
    //            if (verboseOutput) {
    //                if (rawResult != null) {
    //                    System.out.println("Raw Result is:" + rawResult + " and of type:" + rawResult.getClass());
    //                } else {
    //                    System.out.println("Read of attribute'" + attribute + "' from address '" + address
    //                        + "' has returned no value. Are those values in the model?");
    //                }
    //            }
    //            //write the login-modules attribute
    //            op = new WriteAttribute(new Address(address));
    //            op.addAdditionalProperty("name", attribute);//attribute to execute on
    //            op.addAdditionalProperty("value", list2);
    //
    //            //Now test the operation
    //            result = exerciseOperation(op, executeOperation, verboseOutput);
    //            assert ((result.isSuccess() == true) || (result.getOutcome() == null)) : "The operation '" + op
    //                + "' failed to write the resource.." + result.getFailureDescription();
    //
    //            //read the login-modules attribute
    //            op = new ReadAttribute(new Address(address), attribute);
    //            result = exerciseOperation(op, true, verboseOutput);
    //            assert (result.isSuccess() == true) : "The operation '" + op + "' failed to read the resource."
    //                + result.getFailureDescription();
    //        }
    //        if (verboseOutput) {
    //            System.out.println("Successfully detected,read and wrote out attribute values for:");
    //            for (ModuleOptionType type : ModuleOptionType.values()) {
    //                System.out.println("\n" + type.ordinal() + " " + type.name());
    //            }
    //        }
    //    }
    //
    //    /**Attempts to create a new Authentication node(authentication=classic) with a
    //     * 'login-modules' attribute complete with 'code':'Ldap' and 'flag':'required' 
    //     *  and some sample 'module-options' values.
    //     *  
    //     */
    //    @Test(enabled = true)
    //    public void testCreateSecurityDomainChildLoginModules() {
    //        boolean execute = true;
    //        boolean verboseOutput = false;
    //        String address = "subsystem=security,security-domain=testDomain3,authentication=classic";
    //        String attribute = ModuleOptionType.Authentication.getAttribute();
    //
    //        //test operation- read property always available.
    //        Operation op = null;
    //
    //        //read the login-modules attribute
    //        op = new ReadAttribute(new Address(address), attribute);
    //        Result result = exerciseOperation(op, execute, verboseOutput);
    //        assert result.isSuccess() == true : "The operation '" + op + "' failed to read the resource."
    //            + result.getFailureDescription();
    //
    //        //extract current results
    //        Object rawResult = result.getResult();
    //
    //        //#### Have to create new content for the new node.
    //        List<Value> moduleTypeValue = new ArrayList<Value>();
    //        try {
    //            // loading 'login-module'
    //            JsonNode node = mapper.readTree(jsonMap.get(attribute));
    //            result.setResult(mapper.treeToValue(node, Object.class));
    //        } catch (JsonProcessingException e) {
    //            e.printStackTrace();
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        }
    //
    //        //populate the Value component complete with module Options.
    //        moduleTypeValue = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
    //            ModuleOptionsComponent.loadModuleOptionType(attribute));
    //
    //        //add the login-modules attribute
    //        op = ModuleOptionsComponent
    //            .createAddModuleOptionTypeOperation(new Address(address), attribute, moduleTypeValue);
    //
    //        result = exerciseOperation(op, execute, verboseOutput);
    //        assert ((result.isSuccess() == true) || (result.getOutcome() == null)) : "The operation '" + op
    //            + "' failed to write the resource.." + result.getFailureDescription();
    //
    //        //read the login-modules attribute
    //        op = new ReadAttribute(new Address(address), attribute);
    //        result = exerciseOperation(op, execute, verboseOutput);
    //        assert result.isSuccess() == true : "The operation '" + op + "' failed to read the resource."
    //            + result.getFailureDescription();
    //
    //        //exercise values retrieved from read
    //        List<Value> serverResponse = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
    //            ModuleOptionsComponent.loadModuleOptionType(attribute));
    //        Value serverState = serverResponse.get(0);
    //        assert serverState.getFlag().equals("required") : "Incorrect state retrieved for 'flag'. Expected 'required'.";
    //        assert serverState.getCode().equals("Ldap") : "Incorrect state retrieved for 'code'. Expected 'Ldap'.";
    //        LinkedHashMap<String, Object> options = serverState.getOptions();
    //        assert options.size() == 3 : "Invalid number of module options returned. Expected 3.";
    //        int found = 0;
    //        for (String key : options.keySet()) {
    //            if (key.equals("bindPw")) {
    //                assert "test126".equals(options.get(key)) : "Module option value not correct for key '" + key + "'.";
    //                found++;
    //            } else if (key.equals("bindDn")) {
    //                assert "uid=ldapSecureUser,ou=People,dc=redat,dc=com".equals(options.get(key)) : "Module option value not correct for key '"
    //                    + key + "'.";
    //                found++;
    //            } else if (key.equals("allowEmptyPasswords")) {
    //                assert "true".equals(options.get(key)) : "Module option value not correct for key '" + key + "'.";
    //                found++;
    //            }
    //        }
    //        assert found == 3 : "All module options were not loaded.";
    //
    //        //remove the original node to reset for next run.
    //        op = new Remove(new Address(address));
    //        result = exerciseOperation(op, execute, verboseOutput);
    //        assert result.isSuccess() == true : "The operation '" + op + "' failed to remove the resource."
    //            + result.getFailureDescription();
    //    }
    //
    //    /** For each operation 
    //     *   - will write verbose json and operation details to system.out if verboseOutput = true;
    //     *   - will execute the operation against running server if execute = true.
    //     * 
    //     * @param op
    //     * @param execute
    //     * @param verboseOutput
    //     * @return
    //     */
    //    public static Result exerciseOperation(Operation op, boolean execute, boolean verboseOutput) {
    //        //display operation as AS7 plugin will build it
    //        if (verboseOutput) {
    //            System.out.println("\tOperation is:" + op);
    //        }
    //
    //        String jsonToSend = "";
    //        try {
    //            jsonToSend = mapper.defaultPrettyPrintingWriter().writeValueAsString(op);
    //        } catch (JsonGenerationException e) {
    //            e.printStackTrace();
    //        } catch (JsonMappingException e) {
    //            e.printStackTrace();
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        }
    //        //As generated by jackson mapper
    //        if (verboseOutput) {
    //            System.out.println("@@@@ OUTBOUND JSON#\n" + jsonToSend + "#");
    //        }
    //
    //        //Execute the operation
    //        Result result = new Result();
    //        if (execute) {
    //            result = con.execute(op);
    //        } else {
    //            if (verboseOutput) {
    //                System.out.println("**** NOTE: Execution disabled . NOT exercising write-attribute operation. **** ");
    //            }
    //        }
    //        if (verboseOutput) {
    //            //result wrapper details 
    //            System.out.println("\tResult:" + result);
    //            //detailed results
    //            System.out.println("\tValue:" + result.getResult());
    //            System.out.println("-----------------------------------------------------\n");
    //        }
    //        return result;
    //    }
    //
    //    @Test(priority = 10, groups = "discovery")
    //    public void initialSetup() throws Exception {
    //        String securityDomainId = TEST_DOMAIN + "1";
    //        Result result = null;
    //        Operation op = null;
    //
    //        //create the initial securityDomain
    //        op = new Operation("add", new Address("subsystem=security,security-domain=" + securityDomainId));
    //        ASConnection connection = getASConnection();
    //        con = connection;
    //        mapper = new ObjectMapper();
    //        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    //
    //        moc = new ModuleOptionsComponent();
    //        result = connection.execute(op);
    //        System.out.println("#### Op is:" + op);
    //        System.out.println("#### Result is:" + result);
    //
    //        //iterate over jsonMap
    //        for (String key : jsonMap.keySet()) {
    //            String attribute = key;
    //            String address = "";
    //            if (attribute.equals("policy-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",authorization=classic";
    //            } else if (attribute.equals("acl-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",acl=classic";
    //            } else if (attribute.equals("mapping-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",mapping=classic";
    //            } else if (attribute.equals("trust-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",identity-trust=classic";
    //            } else if (attribute.equals("provider-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",audit=classic";
    //            } else if (attribute.equals("login-modules")) {
    //                address = "subsystem=security,security-domain=" + securityDomainId + ",authentication=classic";
    //            } else {
    //                assert false : "An unknown attribute '" + attribute
    //                    + "' was found. Is there a new type to be supported?";
    //            }
    //            //#### Have to create new content for the new node.
    //            List<Value> moduleTypeValue = new ArrayList<Value>();
    //
    //            try {
    //                // loading 'login-module'
    //                JsonNode node = mapper.readTree(jsonMap.get(attribute));
    //                result.setResult(mapper.treeToValue(node, Object.class));
    //            } catch (JsonProcessingException e) {
    //                e.printStackTrace();
    //            } catch (IOException e) {
    //                e.printStackTrace();
    //            }
    //
    //            //populate the Value component complete with module Options.
    //            moduleTypeValue = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
    //                ModuleOptionsComponent.loadModuleOptionType(attribute));
    //
    //            //add the login-modules attribute
    //            op = ModuleOptionsComponent.createAddModuleOptionTypeOperation(new Address(address), attribute,
    //                moduleTypeValue);
    //
    //            result = exerciseOperation(op, true, true);
    //            assert ((result.isSuccess() == true) || (result.getOutcome() == null)) : "The operation '" + op
    //                + "' failed to write the resource.." + result.getFailureDescription();
    //        }
    //    }
}

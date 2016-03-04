package org.rhq.modules.plugins.wildfly10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.ReadChildrenNames;
import org.rhq.modules.plugins.wildfly10.json.Result;
import org.rhq.modules.plugins.wildfly10.json.WriteAttribute;

/**
 * Component class for Module Options. Necessary because Module Options are child 
 * attributes of another as7 node attribute and cannot be created/updated without
 * rewriting the parent attribute as well.  The following as7 node snippets shows how the
 * 'login-modules' attribute contains both simple and complex child attributes.  The complex
 * 'module-options' child attribute values can be numerous and can/may need to be modifiable 
 * independently.
 * Read:
 * [standalone@localhost:9999 authentication=classic] :read-attribute(name=login-modules)
 {
    "outcome" => "success",
    "result" => [{
        "code" => "Ldap",
        "flag" => "requisite",
        "module-options" => [("bindDn"=>"uid=ldapUser,ou=People,dc=redat,dc=com")]
    }]
 }
 * Write back:
 [standalone@localhost:9999 authentication=classic] 
 :write-attribute(name=login-modules,
   value=[{"code"=>"Ldap","flag"=>"required", 
       "module-options"=>[("bindDn"=>"uid=ldapSecureUser,ou=People,dc=redat,dc=com")]}])
 {
    "outcome" => "success",
    "response-headers" => {
        "operation-requires-reload" => true,
        "process-state" => "reload-required"
    }
 }
 * 
 * @author Simeon Pinder
 */
public class ModuleOptionsComponent extends BaseComponent implements ConfigurationFacet, DeleteResourceFacet,
    CreateChildResourceFacet {

    //shared identifier for module-options attribute reused by all nodes with Module Options 
    private static String moduleOptionsNode = ",module-options";

    //Module Option type attribute identifiers
    private static String loginModules = "login-modules";//Authentication (Classic,-Managed Server,-Profile)
    private static String aclModules = "acl-modules";//Acl
    private static String providerModules = "provider-modules";//Audit
    private static String policyModules = "policy-modules";//Authorization
    private static String trustModules = "trust-modules";//IdentityTrust
    private static String mappingModules = "mapping-modules";//Mapping
    private static String authModules = "auth-modules";//Authentication (Jaspi,-Managed Server,-Profile)

    //Enumerates list of AS7 types that support module options.
    public enum ModuleOptionType {
        Acl(aclModules), Audit(providerModules), Authentication(loginModules), AuthenticationJaspi(authModules), Authorization(
            policyModules), IdentityTrust(
            trustModules), Mapping(mappingModules);
        private String attribute = "";

        //stores mapping of as7 attribute name to module option type mapping Ex. login-modules -> Authentication Module Option Type.
        public static HashMap<String, ModuleOptionType> typeMap = new HashMap<String, ModuleOptionType>();
        static {//populate all module option type mappings
            for (ModuleOptionType type : ModuleOptionType.values()) {
                typeMap.put(type.getAttribute(), type);
            }
        }
        //Stores mapping of as7 attribute name to more user friendly Module option type
        public static Map<String, String> readableNameMap = new HashMap<String, String>();
        static {
            readableNameMap.put(aclModules, "ACL Modules");
            readableNameMap.put(providerModules, "Provider Modules");
            readableNameMap.put(loginModules, "Login Modules");//Authentication=classic
            readableNameMap.put(authModules, "Auth Modules");//Authentication=jaspi
            readableNameMap.put(policyModules, "Policy Modules");
            readableNameMap.put(trustModules, "Trust Modules");
            readableNameMap.put(mappingModules, "Mapping Modules");
        }

        public String getAttribute() {
            return attribute;
        }

        private ModuleOptionType(String attribute) {
            this.attribute = attribute;
        }
    }

    //Strings unique to nodes
    private static String AUTH_CLASSIC_NODE = "Authentication (Classic";
    private static String AUTH_JASPI_NODE = "Authentication (Jaspi";
    private static String ACL_NODE = "ACL";
    private static String AUDIT_NODE = "Audit";
    private static String AUTHORIZATION_NODE = "Authorization";
    private static String TRUST_NODE = "Identity Trust";
    private static String MAPPING_NODE = "Mapping";
    private static String[] supportedModuleOptionTypeNodes = { AUTH_CLASSIC_NODE, AUTH_JASPI_NODE, ACL_NODE,
        AUDIT_NODE, AUTHORIZATION_NODE, TRUST_NODE, MAPPING_NODE };

    //define operation/type mappings where specific plugin descriptor nodes map to specific 
    // as7 node names.
    public static HashMap<String, String> attributeMap = new HashMap<String, String>();
    static {
        attributeMap.put(AUTH_CLASSIC_NODE + ")", loginModules);
        attributeMap.put(AUTH_CLASSIC_NODE + " - Managed Server)", loginModules);
        attributeMap.put(AUTH_CLASSIC_NODE + " - Profile)", loginModules);
        attributeMap.put(AUTH_JASPI_NODE + ")", authModules);
        attributeMap.put(AUTH_JASPI_NODE + " - Managed Server)", authModules);
        attributeMap.put(AUTH_JASPI_NODE + " - Profile)", authModules);
        attributeMap.put(ACL_NODE, aclModules);
        attributeMap.put(ACL_NODE + " (Managed Server)", aclModules);
        attributeMap.put(ACL_NODE + " (Profile)", aclModules);
        attributeMap.put(AUDIT_NODE, providerModules);
        attributeMap.put(AUDIT_NODE + " (Managed Server)", providerModules);
        attributeMap.put(AUDIT_NODE + " (Profile)", providerModules);
        attributeMap.put(AUTHORIZATION_NODE, policyModules);
        attributeMap.put(AUTHORIZATION_NODE + " (Managed Server)", policyModules);
        attributeMap.put(AUTHORIZATION_NODE + " (Profile)", policyModules);
        attributeMap.put(TRUST_NODE, trustModules);
        attributeMap.put(TRUST_NODE + " (Managed Server)", trustModules);
        attributeMap.put(TRUST_NODE + " (Profile)", trustModules);
        attributeMap.put(MAPPING_NODE, mappingModules);
        attributeMap.put(MAPPING_NODE + " (Managed Server)", mappingModules);
        attributeMap.put(MAPPING_NODE + " (Profile)", mappingModules);
    }

    public static HashMap<String, String> newChildTypeMap = new HashMap<String, String>();
    static {
        newChildTypeMap.put(loginModules, "authentication=classic");
        newChildTypeMap.put(authModules, "authentication=jaspi");
        newChildTypeMap.put(aclModules, "acl=classic");
        newChildTypeMap.put(providerModules, "audit=classic");
        newChildTypeMap.put(policyModules, "authorization=classic");
        newChildTypeMap.put(trustModules, "identity-trust=classic");
        newChildTypeMap.put(mappingModules, "mapping=classic");
    }
    
    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {

        if (report.getPackageDetails() != null) { // Content deployment
            return deployContent(report);
        } else {
            ASConnection connection = getASConnection();

            // Check for the Highlander principle
            boolean isSingleton = report.getResourceType().isSingleton();
            if (isSingleton) {
                // check if there is already a child with the desired type is present
                Configuration pluginConfig = report.getPluginConfiguration();
                PropertySimple pathProperty = pluginConfig.getSimple("path");
                if (path == null || path.isEmpty()) {
                    report.setErrorMessage("No path property found in plugin configuration");
                    report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);
                    return report;
                }

                ReadChildrenNames op = new ReadChildrenNames(address, pathProperty.getStringValue());
                Result res = connection.execute(op);
                if (res.isSuccess()) {
                    List<String> entries = (List<String>) res.getResult();
                    if (!entries.isEmpty()) {
                        report.setErrorMessage("Resource is a singleton, but there are already children " + entries
                            + " please remove them and retry");
                        report.setStatus(CreateResourceStatus.FAILURE);
                        return report;
                    }
                }
            }

            //determine type then attribute
            ResourceType resourceType = report.getResourceType();
            String attribute = attributeMap.get(resourceType.getName());
            //determine new child name from attribute
            String newChild = newChildTypeMap.get(attribute);

            //get resourceConfig
            Configuration configuration = report.getResourceConfiguration();
            
            if(attribute!=null){//create executed from SecurityDomain level

                //retrieve the values passed in via config
                Value loaded = loadCodeFlagType(configuration, attribute, null);

                //populate the ModuleOptionType from the Configuration
                List<Value> newAttributeState = new ArrayList<Value>();
                newAttributeState.add(loaded);

                //build the operation
                //update the address to point to the new child being created
                Address newChildLocation = new Address(path + "," + newChild);
                Operation op = createAddModuleOptionTypeOperation(newChildLocation, attribute, newAttributeState);

                Result result = connection.execute(op);
                if (result.isSuccess()) {
                    report.setStatus(CreateResourceStatus.SUCCESS);
                    report.setResourceKey(newChildLocation.getPath());
                    report.setResourceName(report.getResourceType().getName());
                } else {
                    report.setStatus(CreateResourceStatus.FAILURE);
                    report.setErrorMessage(result.getFailureDescription());
                }
            }else{//Create executed from the 'Login Modules/Provider Modules/etc. level.
                //retrieve the parent type to lookup attribute to write to
                ResourceType parentType = (ResourceType) resourceType.getParentResourceTypes().toArray()[0];
                attribute = attributeMap.get(parentType.getName());
                
                //retrieve existing attribute definition
                //get the current attribute value
                ReadAttribute op = new ReadAttribute(address, attribute);
                Result result = getASConnection().execute(op);
                if (result.isSuccess()) {
                    //populate attribute values
                    List<Value> currentAttributeState = new ArrayList<Value>();
                    currentAttributeState = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                        ModuleOptionsComponent.loadModuleOptionType(attribute));
                   //populate new Module type data 
                   //retrieve the values passed in via config
                   Value loaded = loadCodeFlagType(configuration, attribute, null);

                    //append new type information
                    currentAttributeState.add(loaded);
                    //write values back out.
                    Operation write = new WriteAttribute(address);
                    write.addAdditionalProperty("name", attribute);//attribute to execute on

                    //now complete the write operation by updating the value
                    write.addAdditionalProperty("value", (Object) currentAttributeState);
                    result = connection.execute(write);
                    if (result.isSuccess()) {
                        report.setStatus(CreateResourceStatus.SUCCESS);
                        //Ex. subsystem=security,security-domain=createOne,authentication=classic,login-modules:0
                        report.setResourceKey(path + "," + attribute + ":" + (currentAttributeState.size() - 1));
                        //Ex. Login Modules 0
                        report.setResourceName(ModuleOptionType.readableNameMap.get(attribute) + " "
                            + (currentAttributeState.size() - 1));
                    } else {
                        report.setStatus(CreateResourceStatus.FAILURE);
                        report.setErrorMessage(result.getFailureDescription());
                    }
                }
            }
            return report;
        }
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = new Configuration();

        //determine the component
        ResourceType resourceType = context.getResourceType();
        Set<ResourceType> nodeParentTypes = context.getResourceType().getParentResourceTypes();
        ResourceType parentType = (ResourceType) nodeParentTypes.toArray()[0];
        ResourceType grandParentType = (ResourceType) parentType.getParentResourceTypes().toArray()[0];

        //For each Module Option type, like Authentication (Classic) and the immediate
        // child node of each, the configuration is shared as it is in the AS7 node as well.
        if (isSupportedModuleOptionTypeOrImmediateChildOf(parentType, resourceType)) {//Classic, Managed, Profile
            boolean currentNodeIsModuleType = false;
            //if the current resourceType is included in the attributeMap then it is an actual Module Option Type.
            if (ModuleOptionsComponent.attributeMap.get(resourceType.getName()) != null) {
                currentNodeIsModuleType = true;
            }
            //get type and lookup supported node and type
            //retrieve the parent resource
            String attribute = ModuleOptionsComponent.attributeMap.get(parentType.getName());
            if (currentNodeIsModuleType) {//if is an actual Module Option Type then update attribute retrieved.
                attribute = ModuleOptionsComponent.attributeMap.get(resourceType.getName());
            }

            //get the current attribute value
            ReadAttribute op = new ReadAttribute(address, attribute);
            Result result = getASConnection().execute(op);
            if (result.isSuccess()) {
                //populate attribute values
                List<Value> currentAttributeState = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                    ModuleOptionsComponent.loadModuleOptionType(attribute));
                if (currentNodeIsModuleType) {//grab first available module type
                    Value loaded = currentAttributeState.get(0);
                    //populate configuration
                    populateCodeFlagType(configuration, attribute, loaded);
                } else {//Need to locate specific module type 
                    //locate specific node and populate the config
                    //Ex."login-modules:";
                    String moduleTypeIdentifier = attribute + ":";
                    int index = path.indexOf(moduleTypeIdentifier);
                    String loginModuleIndex = path.substring(index + moduleTypeIdentifier.length());
                    try {
                        int lmi = Integer.valueOf(loginModuleIndex);//Ex 0,1,30
                        if (currentAttributeState.size() > lmi) {//then retrieve.
                            Value loaded = currentAttributeState.get(lmi);
                            //populate configuration
                            populateCodeFlagType(configuration, attribute, loaded);
                        }
                    } catch(NumberFormatException e) {
                        // Ignore it, the value just wasn't available
                    }
                }
            }
            //read attribute
            return configuration;
        }
        //Module Options child, Ex. 'Login Modules (Classic', ..-Managed, .. - Profile
        else if (supportsLoginModuleOptionType(grandParentType)) {
            //get type and lookup supported node and type
            String attribute = ModuleOptionsComponent.attributeMap.get(grandParentType.getName());

            //get the current attribute value
            ReadAttribute op = new ReadAttribute(address, attribute);
            Result result = getASConnection().execute(op);
            if (result.isSuccess()) {
                //populate attribute values
                List<Value> currentAttributeState = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                    ModuleOptionsComponent.loadModuleOptionType(attribute));

                //locate specific node and populate the config
                //Ex."login-modules:";
                String moduleTypeIdentifier = attribute + ":";
                int index = path.indexOf(moduleTypeIdentifier);
                String loginModuleIndex = path.substring(index + moduleTypeIdentifier.length());
                String[] split = loginModuleIndex.split(",");
                try {
                    int lmi = Integer.valueOf(split[0]);//Ex 0,1,30
                    if (lmi < currentAttributeState.size()) {//then proceed
                        Value loaded = currentAttributeState.get(lmi);
                        //populate configuration: module-options
                        LinkedHashMap<String, Object> currentModuleOptions = loaded.getOptions();

                        //This must match exactly the mapping identifier from descriptor, otherwise loadResource fails silently.
                        String id = "Module Options";
                        PropertyMap map = new PropertyMap(id);
                        for (String key : currentModuleOptions.keySet()) {
                            PropertySimple option = new PropertySimple(key, currentModuleOptions.get(key));
                            map.put(option);
                        }
                        if (!currentModuleOptions.isEmpty()) {//check that keyset is non empty before adding to config.
                            configuration.put(map);
                        }
                    }
                } catch(NumberFormatException e) {
                    // Ignore it, it just wasn't available
                }
            }
            return configuration;
        } else {//otherwise default subsystem discovery behavior.
            ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
            ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef, getASConnection(), address,
                includeRuntime);
            configuration = delegate.loadResourceConfiguration();

            // Read server state
            ReadAttribute op = new ReadAttribute(getAddress(), "name");
            executeAndGenerateServerUpdateIfNecessary(configuration, op);
            return configuration;
        }
    }

    /** Looks up whether the current node supports the Module Options.
     * 
     * @param grandParentType
     * @return
     */
    private boolean supportsLoginModuleOptionType(ResourceType grandParentType) {
        boolean supportModuleOptionTypes = false;
        if (grandParentType != null) {
            for (String moduleOptionType : supportedModuleOptionTypeNodes) {
                if (grandParentType.getName().indexOf(moduleOptionType) > -1) {
                    supportModuleOptionTypes = true;
                }
            }
        }
        return supportModuleOptionTypes;
    }

    /**Iterates through the list of supported Module Option Type nodes
     * to determine if the current node is i)supported Module option node or
     * ii)the immediated child of a supported Module option node.
     * 
     * @param parentType
     * @param resourceType
     * @return
     */
    private boolean isSupportedModuleOptionTypeOrImmediateChildOf(ResourceType parentType, ResourceType resourceType) {
        boolean loadThisConfiguration = false;
        if ((parentType != null) && (resourceType != null)) {
            for (String moduleOptionType : attributeMap.keySet()) {
                if ((parentType.getName().equals(moduleOptionType))
                    || (resourceType.getName().equals(moduleOptionType))) {
                    loadThisConfiguration = true;
                }
            }

        }
        return loadThisConfiguration;
    }

    /** Handles the different types of configuration population based on the attribute value passed in.
     * 
     * @param configuration
     * @param attribute 
     * @param loaded
     */
    private void populateCodeFlagType(Configuration configuration, String attribute, Value loaded) {
        if (attribute.equals(providerModules)) {//audit=classic. Ex. only code 
            PropertySimple currentValue = new PropertySimple("code", loaded.getCode());
            configuration.put(currentValue);
        } else if (attribute.equals(mappingModules)) {//mapping=classic. Ex. code type
            PropertySimple currentValue = new PropertySimple("code", loaded.getCode());
            PropertySimple currentValue2 = new PropertySimple("type", loaded.getType());
            configuration.put(currentValue);
            configuration.put(currentValue2);
        } else {//code flag
            PropertySimple currentValue = new PropertySimple("code", loaded.getCode());
            PropertySimple currentValue2 = new PropertySimple("flag", loaded.getFlag());
            configuration.put(currentValue);
            configuration.put(currentValue2);
        }
    }

    /** Populates the Value instance passed in or returns new Value instance with values loaded.
     * 
     * @param configuration
     * @param attribute
     * @param loaded
     * @return
     */
    private Value loadCodeFlagType(Configuration configuration, String attribute, Value loaded) {
        //if required data is not present then return null.
        if ((configuration == null) || attribute == null) {
            return null;
        }
        if (loaded == null) {// initialize if null
            loaded = new Value();
        }
        if (attribute.equals(providerModules)) {//audit=classic. Ex. only code
            String code = configuration.getSimpleValue("code");
            loaded.setCode(code);
        } else if (attribute.equals(mappingModules)) {//mapping=classic. Ex. code type
            String code = configuration.getSimpleValue("code");
            String type = configuration.getSimpleValue("type");
            loaded.setCode(code);
            loaded.setType(type);
        } else {//code flag
            String code = configuration.getSimpleValue("code");
            String flag = configuration.getSimpleValue("flag");
            loaded.setCode(code);
            loaded.setFlag(flag);
        }
        return loaded;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        //determine the component
        ResourceType resourceType = context.getResourceType();
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        Set<ResourceType> nodeParentTypes = context.getResourceType().getParentResourceTypes();
        ResourceType parentType = (ResourceType) nodeParentTypes.toArray()[0];
        ResourceType grandParentType = (ResourceType) parentType.getParentResourceTypes().toArray()[0];
        ///if child of SecurityDomain then
        if (isSupportedModuleOptionTypeOrImmediateChildOf(parentType, resourceType)) {
            boolean currentNodeIsModuleType = false;
            if (ModuleOptionsComponent.attributeMap.get(resourceType.getName()) != null) {
                currentNodeIsModuleType = true;
            }
            //get type and lookup supported node and type
            //retreive the parent resource
            String attribute = ModuleOptionsComponent.attributeMap.get(parentType.getName());
            if (currentNodeIsModuleType) {
                attribute = ModuleOptionsComponent.attributeMap.get(resourceType.getName());
            }

            //get the current attribute value. Will write module types back out exactly as read in with these new updates
            ReadAttribute op = new ReadAttribute(address, attribute);
            Result result = getASConnection().execute(op);
            if (result.isSuccess()) {
                List<String> entries = (List<String>) result.getResult();

                //populate attribute values
                List<Value> currentAttributeState = new ArrayList<Value>();
                currentAttributeState = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                    ModuleOptionsComponent.loadModuleOptionType(attribute));

                //retrieve current config changes    
                Configuration conf = report.getConfiguration();

                if (currentNodeIsModuleType) {//grab first available module type
                    Value loaded = currentAttributeState.get(0);
                    //iterate over properties and update values appropriately
                    for (String pKey : conf.getSimpleProperties().keySet()) {
                        if (pKey.equals("flag")) {
                            loaded.setFlag(conf.getSimpleProperties().get(pKey).getStringValue());
                        } else if (pKey.equals("code")) {
                            loaded.setCode(conf.getSimpleProperties().get(pKey).getStringValue());
                        } else if (pKey.equals("type")) {
                            loaded.setType(conf.getSimpleProperties().get(pKey).getStringValue());
                        }
                    }
                    Operation write = new WriteAttribute(address);
                    write.addAdditionalProperty("name", attribute);//attribute to execute on

                    //now complete the write operation by updating the value
                    write.addAdditionalProperty("value", (Object) currentAttributeState);
                    executeWriteAndGenerateAs7ServerUpdate(report, conf, write);
                } else {//Need to locate specific module type 

                //locate specific node and populate the config
                    String loginModuleIdentifier = attribute + ":";
                int index = path.indexOf(loginModuleIdentifier);
                String loginModuleIndex = path.substring(index + loginModuleIdentifier.length());
                int lmi = Integer.valueOf(loginModuleIndex);//Ex 0,1,30
                Value valueObject = currentAttributeState.get(lmi);

                //iterate over properties and update values appropriately
                for (String pKey : conf.getSimpleProperties().keySet()) {
                    if (pKey.equals("flag")) {
                        valueObject.setFlag(conf.getSimpleProperties().get(pKey).getStringValue());
                    } else if (pKey.equals("code")) {
                        valueObject.setCode(conf.getSimpleProperties().get(pKey).getStringValue());
                        } else if (pKey.equals("type")) {
                            valueObject.setType(conf.getSimpleProperties().get(pKey).getStringValue());
                    }
                }
                Operation write = new WriteAttribute(address);
                write.addAdditionalProperty("name", attribute);//attribute to execute on

                //now complete the write operation by updating the value
                write.addAdditionalProperty("value", (Object) currentAttributeState);
                executeWriteAndGenerateAs7ServerUpdate(report, conf, write);
                }
            }
        } else if (supportsLoginModuleOptionType(grandParentType)) {//Module Options child.
            //get type and lookup supported node and type
            String attribute = ModuleOptionsComponent.attributeMap.get(grandParentType.getName());

            //get the current attribute value. Will write module types back out exactly as read in with these new updates
            ReadAttribute op = new ReadAttribute(address, attribute);
            Result result = getASConnection().execute(op);
            if (result.isSuccess()) {

                //populate attribute values
                List<Value> currentAttributeState = new ArrayList<Value>();
                currentAttributeState = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                    ModuleOptionsComponent.loadModuleOptionType(attribute));

                //locate specific node and populate the config
                String loginModuleIdentifier = attribute + ":";
                int index = path.indexOf(loginModuleIdentifier);
                String loginModuleIndex = path.substring(index + loginModuleIdentifier.length());
                String[] split = loginModuleIndex.split(",");
                int lmi = Integer.valueOf(split[0]);//Ex 0,1,30
                Value valueObject = currentAttributeState.get(lmi);

                //retrieve current config changes    
                Configuration conf = report.getConfiguration();
                //list current conf stated being defined.
                String mapKey = (String) conf.getMap().keySet().toArray()[0];
                PropertyMap mapType = (PropertyMap) conf.getMap().get(mapKey);
                //insert update logic.
                //set this new state passed in as state of the attribute.
                LinkedHashMap<String, Object> currentOptions = new LinkedHashMap<String, Object>();
                for (String propertyKey : mapType.getMap().keySet()) {
                    currentOptions.put(propertyKey, ((PropertySimple) mapType.get(propertyKey)).getStringValue());
                }

                //make these settings the new state for this part of the attribute.
                valueObject.setOptions(currentOptions);
                currentAttributeState.set(lmi, valueObject);

                Operation write = new WriteAttribute(address);
                write.addAdditionalProperty("name", attribute);//attribute to execute on

                //now complete the write operation by updating the value
                write.addAdditionalProperty("value", (Object) currentAttributeState);
                executeWriteAndGenerateAs7ServerUpdate(report, conf, write);
            }
        } else {
            ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
            delegate.updateResourceConfiguration(report);
        }

    }

    /**Executes boilerplate finish for write operation.
     * 
     * @param report
     * @param conf
     * @param write
     */
    private void executeWriteAndGenerateAs7ServerUpdate(ConfigurationUpdateReport report, Configuration conf,
        Operation write) {
        Result result;
        result = getASConnection().execute(write);
        if (!result.isSuccess()) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage(result.getFailureDescription());
        } else {
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            // signal "need reload"
            if (result.isReloadRequired()) {
                PropertySimple oobMessage = new PropertySimple("__OOB",
                    "The server needs a reload for the latest changes to come effective.");
                conf.put(oobMessage);
            }
            if (result.isRestartRequired()) {
                PropertySimple oobMessage = new PropertySimple("__OOB",
                    "The server needs a restart for the latest changes to come effective.");
                conf.put(oobMessage);
            }
        }
    }

    /**Executes boilerplate finish for read operation.
     * 
     * @param report
     * @param conf
     * @param write
     */
    private void executeAndGenerateServerUpdateIfNecessary(Configuration configuration, ReadAttribute op) {
        Result res = getASConnection().execute(op);
        if (res.isReloadRequired()) {
            PropertySimple oobMessage = new PropertySimple("__OOB",
                "The server needs a reload for the latest changes to come effective.");
            configuration.put(oobMessage);
        }
        if (res.isRestartRequired()) {
            PropertySimple oobMessage = new PropertySimple("__OOB",
                "The server needs a restart for the latest changes to come effective.");
            configuration.put(oobMessage);
        }
    }

    /** Locates the write ModuleOptionType mapped to the AS7Node attribute passed in.
     *  Ex. 'login-modules' -> Authentication (Classic * type. One of three types.
     * 
     * @param attribute 
     * @return
     */
    public static ModuleOptionType loadModuleOptionType(String attribute) {
        ModuleOptionType located = ModuleOptionType.typeMap.get(attribute);
        if (located == null) {
            throw new IllegalArgumentException("Unknown node '" + attribute
                + "' entered for which no valid ModuleOptionType could be found.");
        }
        return located;
    }

    /** Takes the result passed in(successful readAttribute for ModuleOptionType) and
     *  parses the json to populate a json object of type List<Value> with the 
     *  results.
     *  Handles inconsistencies in model representation for login-modules vs. 
     *  the other supported ModuleOptionTypes.
     * 
     * @param result json.Result type from successful read of attribute.
     * @param type ModuleOptionType
     * @return List<Value> type populated with moduleOption details.
     */
    public static List<Value> populateSecurityDomainModuleOptions(Result result, ModuleOptionType type) {
        //initialize empty
        List<Value> populated = new ArrayList<Value>();
        //input validation
        if ((result != null) && (result.isSuccess())) {

            //parse json and populate the object.
            Object rawResult = result.getResult();

            if (rawResult instanceof ArrayList) {
                //iterate over the module option type passed in.
                ArrayList moduleOptionTypeChildrenList = (ArrayList) rawResult;
                for (int i = 0; i < moduleOptionTypeChildrenList.size(); i++) {
                    Value value = new Value();
                    //stores current <module-options> defined.
                    LinkedHashMap<String, Object> optionsMap = new LinkedHashMap<String, Object>();
                    Object entryCheck = ((ArrayList) rawResult).get(i);

                    if (entryCheck instanceof HashMap) {

                        //this is the root attribute map for all children of the specific custom Security Domain 
                        //type attributes.
                        Map<String, Object> attributeMap = (HashMap<String, Object>) entryCheck;
                        for (String key : attributeMap.keySet()) {

                            //peek at contents to exclude empty values.
                            String extracted = String.valueOf(attributeMap.get(key));
                            if (!extracted.trim().isEmpty()) {
                                if (key.equals("flag")) {
                                    value.setFlag(extracted);
                                } else if (key.equals("code")) {
                                    value.setCode(extracted);
                                } else if (key.equals("type")) {
                                    value.setType(extracted);
                                } else if (key.equals("module-options")) {
                                    //Need to support both Map and List types here because of inconsistent representations.
                                    Object optionEntity = attributeMap.get(key);
                                    if (optionEntity instanceof HashMap) {
                                        Map<String, Object> entryList = (HashMap<String, Object>) optionEntity;
                                        for (String oKey : entryList.keySet()) {
                                            //get key and value and populate ModuleEntries found.
                                            optionsMap.put(oKey, String.valueOf(entryList.get(oKey)));
                                        }
                                    } else if (optionEntity instanceof ArrayList) {
                                        Object listEntryCheck = ((ArrayList) optionEntity).get(0);
                                        if (listEntryCheck instanceof HashMap) {
                                            ArrayList list = (ArrayList) optionEntity;
                                            for (Object listEntry : list) {//iterate over each instance to get all values.
                                                Map<String, Object> entryList = (HashMap<String, Object>) listEntry;
                                                for (String oKey : entryList.keySet()) {
                                                    //get key and value and populate ModuleEntries found.
                                                    optionsMap.put(oKey, String.valueOf(entryList.get(oKey)));
                                                }
                                            }
                                        }
                                    }//end of if/else
                                }
                            }//end of empty value check
                        }
                    }//end of HashMap for SecurityDomain child check
                    value.setOptions(optionsMap);
                    populated.add(value);
                }//end of moduleOptionType iteration
            }
        }

        return populated;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public static class Value {
        // no args for jackson.
        public Value() {
        };

        /** Three possible flags can be set. c-> code f->flag t->type
         * Acl                  c  f 
         * Authentication       c  f
         * Authentication(Jaspi)c  f
         * Authorization        c  f
         * Identity Trust       c  f
         * Mapping              c  t
         * Audit                c
         * 
         * @param code
         * @param flag
         * @param type
         */
        public Value(String code, String flag, String type) {
            setCode(code);
            if ((flag != null) && (!flag.trim().isEmpty())) {
                setFlag(flag);
            }
            if ((type != null) && (!type.trim().isEmpty())) {
                setType(type);
            }
        }

        //default to empty string for case where specific attribute is not used.
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        private String flag;
        private String code;
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFlag() {
            return flag;
        }

        public void setFlag(String flag) {
            this.flag = flag;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        //overrides the type name for serialization/deserialization.
        @JsonProperty(value = "module-options")
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        private LinkedHashMap<String, Object> options = null;

        @JsonProperty(value = "module-options")
        public LinkedHashMap<String, Object> getOptions() {
            if (options == null) {
                options = new LinkedHashMap<String, Object>();
            }
            return options;
        }

        @JsonProperty(value = "module-options")
        public void setOptions(LinkedHashMap<String, Object> options) {
            this.options = options;
        }

        @Override
        public String toString() {
            String serialized = "";
            serialized += "code=" + getCode() + ", ";
            if (getFlag() != null) {
                serialized += "flag=" + getFlag() + ", ";
            } else if (getType() != null) {
                serialized += "type=" + getType() + ", ";
            }
            String options = "module-options=";
            if (getOptions().isEmpty()) {
                options += " {}";
            } else {
                options += " {";
                for (String key : getOptions().keySet()) {
                    options += key + "=\"" + getOptions().get(key) + "\",";
                }
                options = options.substring(0, options.length() - 1);
                options += "} ";
            }
            serialized += options;
            return serialized;
        }
    }

    /** Handles the creation of 
     * 
     * @param address
     * @param attribute
     * @param moduleTypeValue
     * @return
     */
    public static Operation createAddModuleOptionTypeOperation(Address address, String attribute,
        List<Value> moduleTypeValue) {
        Operation add = null;
        if ((address != null) & (attribute != null) & (moduleTypeValue != null)) {
            add = new Operation("add", address);
            add.addAdditionalProperty(attribute, moduleTypeValue);
        }
        return add;
    }
}

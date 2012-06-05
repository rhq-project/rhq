package org.rhq.modules.plugins.jbossas7;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

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

    private static String moduleOptionsNode = ",module-options";

    public enum ModuleOptionType {
        Authentication("login-modules"), Authorization("policy-modules"), Mapping("mapping-modules"), Audit(
            "provider-modules");
        private String attribute = "";
        public static HashMap<String, ModuleOptionType> typeMap = new HashMap<String, ModuleOptionType>();
        static {
            for (ModuleOptionType type : ModuleOptionType.values()) {
                typeMap.put(type.getAttribute(), type);
            }
        }

        public String getAttribute() {
            return attribute;
        }

        private ModuleOptionType(String attribute) {
            this.attribute = attribute;
        }
    }

    //define operation/type mappings
    private static HashMap<String, String> attributeMap = new HashMap<String, String>();
    static {
        //TODO: this type structure is not right. Fix!!
        attributeMap.put("Module Options (Classic)", "login-modules");
        attributeMap.put("Authentication (Classic)", "login-modules");
        //TODO: spinder 6/2/12 add all of the supported parent types.
    }

    @Override
    public void deleteResource() throws Exception {
        super.deleteResource();
    }

    /** Executed from parent type.
     *  When creating a new Module-option we need to:
     *  i)read the existing definition
     *  ii)parse the new configuration to generate new module-option commands
     *  iii)execute the as7 operation to create these new options.
     */
    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);

        ASConnection connection = getASConnection();
        ResourceType resourceType = report.getResourceType();
        ConfigurationDefinition configDef = resourceType.getResourceConfigurationDefinition();

        //retreive the parent resource
        String attribute = attributeMap.get(resourceType.getName());

        //read the current configuration
        Configuration newConfiguration = report.getResourceConfiguration();

        //get the current attribute value
        ReadAttribute op = new ReadAttribute(address, attribute);
        Result result = connection.execute(op);
        if (result.isSuccess()) {
            List<String> entries = (List<String>) result.getResult();

            //populate attribute values
            List<Value> currentAttributeState = new ArrayList<Value>();
            currentAttributeState = populateSecurityDomainModuleOptions(result,
                loadModuleOptionType(attribute));

            Operation write = new WriteAttribute(address);
            write.addAdditionalProperty("name", attribute);//attribute to execute on
            //update the write attribute list
            ///get the option list
            Value valueObject = currentAttributeState.get(0);
            LinkedHashMap<String, Object> optionsList = valueObject.getOptions();
            //if new module options are added, detect and add here
            if (newConfiguration.getAllProperties().size() > 0) {
                for (String key : newConfiguration.getAllProperties().keySet()) {
                    ///add to the option list
                    optionsList.put(key, newConfiguration.getAllProperties().get(key));
                }
                ///update the attribute state to be written out
                valueObject.setOptions(optionsList);
            }
            //now complete the write operation by updating the value
            write.addAdditionalProperty("value", (Object) currentAttributeState);

            String stringIfy = "";
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                stringIfy = mapper.writeValueAsString(write);
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //test write back out
            result = connection.execute(write);

            if (result.isSuccess()) {
                report.setStatus(CreateResourceStatus.SUCCESS);
                //TODO: spinder 6/2/12 set this correctly
                //                report.setResourceKey(createAddress.getPath());
                report.setResourceKey(address.getPath());
                report.setResourceName(report.getUserSpecifiedResourceName());
            } else {
                report.setStatus(CreateResourceStatus.FAILURE);
                report.setErrorMessage(result.getFailureDescription());
            }
            return report;
        }

        CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, connection, address);
        return delegate.createResource(report);
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration loaded = new Configuration();
        //load the configuration from parent node
        String currentPath = address.getPath();
        Set<ResourceType> pTypes = context.getResourceType().getParentResourceTypes();
        ResourceType parentType = (ResourceType) pTypes.toArray()[0];

        // Read server state
        //        ReadAttribute op = new ReadAttribute(getAddress(), "name");
        //TODO: spinder 6/4/12 We need to look this up from type passed in.
        String attribute = "login-modules";
        attribute = attributeMap.get(parentType.getName());
        ReadAttribute op = new ReadAttribute(new Address(getAddress()), attribute);
        Result result = getASConnection().execute(op);
        if (result.isSuccess()) {
            //get property Definition 
            ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
            List<PropertyDefinition> ungrouped = configDef.getNonGroupedProperties();
            PropertyDefinition pdef = ungrouped.get(0);
            //populate attribute values
            List<Value> currentAttributeState = new ArrayList<Value>();
            currentAttributeState = populateSecurityDomainModuleOptions(result, loadModuleOptionType(attribute));
            LinkedHashMap<String, Object> moduleOptions = currentAttributeState.get(0).getOptions();
            PropertyList loadedConfigProp = new PropertyList(pdef.getName());

            for (String key : moduleOptions.keySet()) {
                loaded.getProperties().add(new PropertySimple(key, moduleOptions.get(key)));
            }
        }
        return loaded;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);
    }

    private static ModuleOptionType loadModuleOptionType(String attribute) {
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
    private List<Value> populateSecurityDomainModuleOptions(Result result, ModuleOptionType type) {
        //initialize empty
        List<Value> populated = new ArrayList<Value>();
        //stores current <module-options> defined.
        LinkedHashMap<String, Object> optionsMap = new LinkedHashMap<String, Object>();
        //input validation
        if ((result != null) && (result.isSuccess())) {

            Value value = new Value();
            //parse json and populate the object.
            Object rawResult = result.getResult();

            if (rawResult instanceof ArrayList) {
                //retrieve first entry to do type check
                Object entryCheck = ((ArrayList) rawResult).get(0);
                if (entryCheck instanceof HashMap) {
                    //this is the root attribute map for all children of the custom Security Domain type attributes.
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
                }
            }

            value.setOptions(optionsMap);
            populated.add(value);
        }

        return populated;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public static class Value {
        // no args for json.
        public Value() {
        };

        /** Three possible flags can be set. 
         * Authentication       c  f
         * Authorization        c  f
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
        private LinkedHashMap<String, Object> options = new LinkedHashMap<String, Object>();

        public LinkedHashMap<String, Object> getOptions() {
            return options;
        }

        public void setOptions(LinkedHashMap<String, Object> options) {
            this.options = options;
        }
    }
}

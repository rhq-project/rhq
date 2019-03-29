package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Component for the naming subsystem
 *
 * @author Heiko W. Rupp
 */
public class NamingComponent extends BaseComponent implements OperationFacet {
    private static final String BINDING_TYPE = "Binding";
    private static final String ENVIRONMENT = "environment";

    class BindingCreateResourceDelegate extends CreateResourceDelegate {

        public BindingCreateResourceDelegate(ConfigurationDefinition configDef, ASConnection connection, Address address) {
            super(configDef, connection, address);
        }

        @Override
        public CreateResourceReport createResource(CreateResourceReport report) {
            report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);
            Address createAddress = new Address(this.address);
            String path = report.getPluginConfiguration().getSimpleValue("path", "");
            String resourceName;
            if (!path.contains("=")) {
                //this is not a singleton subsystem
                //resources like  example=test1 and example=test2 can be created
                resourceName = report.getUserSpecifiedResourceName();
            } else {
                //this is a request to create a true singleton subsystem
                //both the path and the name are set at resource level configuration
                resourceName = path.substring(path.indexOf('=') + 1);
                path = path.substring(0, path.indexOf('='));
            }
            createAddress.add(path, resourceName);
            Operation op = new Operation("add", createAddress);
            for (Property prop : report.getResourceConfiguration().getProperties()) {
                AbstractMap.SimpleEntry<String, ?> entry = null;

                boolean isEntryEligible = true;
                if (prop.getName().equals(ENVIRONMENT)) {
                    PropertyList propertyList = (PropertyList) prop;
                    PropertyDefinitionList propertyDefinition = this.configurationDefinition
                            .getPropertyDefinitionList(propertyList.getName());

                    if (!propertyDefinition.isRequired() && propertyList.getList().size() == 0) {
                        isEntryEligible = false;
                    } else {
                        List<Property> embeddedProps = propertyList.getList();
                        Map<String, Object> results = new HashMap<String, Object>();
                        for (Property inner : embeddedProps) {
                            PropertyMap propMap = (PropertyMap) inner;
                            PropertySimple propName = ((PropertySimple) propMap.get("name"));
                            PropertySimple propValue = ((PropertySimple) propMap.get("value"));
                            results.put(propName.getStringValue(), propValue.getStringValue());
                        }
                        entry = new SimpleEntry<String, Map<String, Object>>(prop.getName(), results);
                        isEntryEligible = true;
                    }
                } else if (prop instanceof PropertySimple) {
                    PropertySimple propertySimple = (PropertySimple) prop;
                    PropertyDefinitionSimple propertyDefinition = this.configurationDefinition
                            .getPropertyDefinitionSimple(propertySimple.getName());

                    if (propertyDefinition == null
                            || (!propertyDefinition.isRequired() && propertySimple.getStringValue() == null)) {
                        isEntryEligible = false;
                    } else {
                        entry = preparePropertySimple(propertySimple, propertyDefinition);
                    }
                } else if (prop instanceof PropertyList) {
                    PropertyList propertyList = (PropertyList) prop;
                    PropertyDefinitionList propertyDefinition = this.configurationDefinition
                            .getPropertyDefinitionList(propertyList.getName());

                    if (!propertyDefinition.isRequired() && propertyList.getList().size() == 0) {
                        isEntryEligible = false;
                    } else {
                        entry = preparePropertyList(propertyList, propertyDefinition);
                    }
                } else if (prop instanceof PropertyMap) {
                    PropertyMap propertyMap = (PropertyMap) prop;
                    PropertyDefinitionMap propertyDefinition = this.configurationDefinition
                            .getPropertyDefinitionMap(propertyMap.getName());

                    if (!propertyDefinition.isRequired() && propertyMap.getMap().size() == 0) {
                        isEntryEligible = false;
                    } else {
                        entry = preparePropertyMap(propertyMap, propertyDefinition);
                        isEntryEligible = !((Map<String, Object>) entry.getValue()).isEmpty();
                    }
                }

                if (isEntryEligible) {
                    op.addAdditionalProperty(entry.getKey(), entry.getValue());
                }
            }

            Result result = this.connection.execute(op);
            if (result.isSuccess()) {
                report.setStatus(CreateResourceStatus.SUCCESS);
                report.setResourceKey(createAddress.getPath());
                report.setResourceName(report.getUserSpecifiedResourceName());
            } else {
                report.setStatus(CreateResourceStatus.FAILURE);
                report.setErrorMessage(result.getFailureDescription());
            }

            return report;
        }
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        if (report.getResourceType().getName().equals(BINDING_TYPE)) {
            ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition();
            ASConnection connection = getASConnection();
            BindingCreateResourceDelegate delegate = new BindingCreateResourceDelegate(configDef, connection, address);
            return delegate.createResource(report);
        }
        return super.createResource(report);
    }

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        OperationResult result = new OperationResult();

        Operation operation;
        if (name.equals("jndi-view")) {
            Address address = new Address(path);
            operation = new Operation("jndi-view",address);
            Result res = getASConnection().execute(operation);

            if (!res.isSuccess()) {
                result.setErrorMessage(res.getFailureDescription());
                return result;
            }

            Configuration config = result.getComplexResults();

            Map<String,Map> contexts = (Map<String, Map>) res.getResult();

            String entryName = "java: contexts";
            Map<String,Map> javaContexts = contexts.get(entryName);
            PropertyList javaContextsMap = fillMap(javaContexts, "java-contexts", false);
            config.put(javaContextsMap);
            entryName = "applications";
            Map<String,Map> applications = contexts.get(entryName);
            PropertyList applicationsMap = fillMap(applications, entryName, true);
            config.put(applicationsMap);

            return result;
        }

        return super.invokeOperation(name,parameters);
    }

    private PropertyList fillMap(Map<String, Map> contexts, String name, boolean isAppCtx) {
        PropertyList pl = new PropertyList(name);
        if (contexts == null) { // e.g. no applications deployed
            return pl;
        }

        for (String entry : contexts.keySet() ) {
            Map<String,Object> map = contexts.get(entry);
            if (map==null)
                continue;

            for (String innerkey : map.keySet()) {
                PropertyMap pm = new PropertyMap(name);
                pm.put(new PropertySimple("context",entry));
                pm.put(new PropertySimple("name",innerkey));
                Object val = map.get(innerkey);
                if (val!=null) {
                    if (isAppCtx) {
                        Map<String,Object> cvmap = (Map<String, Object>) val;
                        if (cvmap.containsKey("AppName")) {
                            Map<String,String> nameMap = (Map<String, String>) cvmap.get("AppName");
                            pm.put(new PropertySimple("value", nameMap.get("value")));
                        }
                        else
                            pm.put(new PropertySimple("value",val.toString() ));
                    }
                    else {
                        Map<String,String> cvmap = (Map<String, String>) val;
                        String clazz = cvmap.get("class-name");
                        String value = cvmap.get("value");
                        pm.put(new PropertySimple("value",clazz + "="+ value));
                    }
                }
                pl.add(pm);
            }
        }

        return pl;
    }
}

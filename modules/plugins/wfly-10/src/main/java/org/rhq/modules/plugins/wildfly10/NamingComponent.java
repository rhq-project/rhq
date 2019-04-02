package org.rhq.modules.plugins.wildfly10;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Component for the naming subsystem
 * @author Heiko W. Rupp
 */
public class NamingComponent extends BaseComponent implements OperationFacet {
    private static final String BINDING_TYPE = "Binding";
    private static final String ENVIRONMENT = "environment";
    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition().copy();
        if (report.getResourceType().getName().equals(BINDING_TYPE)) {
            Configuration resourceConfiguration = report.getResourceConfiguration();
            PropertyList prop = (PropertyList)resourceConfiguration.get(ENVIRONMENT);
            if (prop != null) {
                List<Property> embeddedProps = prop.getList();

                /* Modify definition on-fly*/
                Map<String, PropertyDefinition> definitions = configDef.getPropertyDefinitions();
                PropertyDefinitionList envDefinition = (PropertyDefinitionList)definitions.get(ENVIRONMENT);
                PropertyDefinitionMap envDefinitionMap = new PropertyDefinitionMap(envDefinition.getName(),
                        envDefinition.getDescription(), envDefinition.isRequired());
                definitions.put(ENVIRONMENT, envDefinitionMap);

                /* Modify resource configuration on-fly*/
                PropertyMap environmentMap = new PropertyMap();
                environmentMap.setName(ENVIRONMENT);
                Map<String, Property> results = new HashMap<String, Property>();
                for (Property inner : embeddedProps) {
                    PropertyMap propMap = (PropertyMap) inner;
                    PropertySimple propName = ((PropertySimple) propMap.get("name"));
                    PropertySimple propValue = ((PropertySimple) propMap.get("value"));
                    if (propName != null && propValue != null ) {
                        PropertySimple propS = new PropertySimple();
                        propS.setName(propName.getStringValue());
                        propS.setValue(propValue.getStringValue());
                        results.put(propName.getStringValue(), propS);
                        // Add to "ghost" definition
                        envDefinitionMap.put(new PropertyDefinitionSimple(propName.getStringValue(),
                                "", true, PropertySimpleType.STRING));
                    }

                }
                environmentMap.setMap(results);
                resourceConfiguration.put(environmentMap);
            }
            ASConnection connection = getASConnection();
            CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, connection, address);
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

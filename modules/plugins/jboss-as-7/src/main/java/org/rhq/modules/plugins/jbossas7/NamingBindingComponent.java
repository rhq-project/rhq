package org.rhq.modules.plugins.jbossas7;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.modules.plugins.jbossas7.ConfigurationWriteDelegate;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.core.domain.configuration.Property;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;
/**
 * Component for the naming subsystem
 * @author Ruben Vargas
 */
public class NamingBindingComponent extends BaseComponent<BaseComponent<?>> {

    private static final String ENVIRONMENT = "environment";

    class BindingConfigurationWriteDelegate extends ConfigurationWriteDelegate {


        public BindingConfigurationWriteDelegate(ConfigurationDefinition configDef, ASConnection connection, Address
                address) {
            super(configDef.copy(), connection, address);
            this.configurationDefinition.getPropertyDefinitions().remove(ENVIRONMENT);
        }

        @Override
        protected CompositeOperation updateGenerateOperationFromProperties(Configuration conf, Address address) {
            CompositeOperation cop = super.updateGenerateOperationFromProperties(conf, address);
            /* Update special enviroment var */
            PropertyList prop = (PropertyList)conf.get(ENVIRONMENT);
            List<Object> values = new ArrayList<Object>();
            List<Property> embeddedProps = prop.getList();
            Map<String, Object> results = new HashMap<String, Object>();
            for (Property inner : embeddedProps) {
                PropertyMap propMap = (PropertyMap) inner;
                PropertySimple propName = ((PropertySimple) propMap.get("name"));
                PropertySimple propValue = ((PropertySimple) propMap.get("value"));
                results.put(propName.getStringValue(), propValue.getStringValue());
            }
            WriteAttribute writeAttribute = new WriteAttribute(address, ENVIRONMENT, results);
            cop.addStep(writeAttribute);
            return cop;
        }
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition().copy();
        ConfigurationWriteDelegate delegate = new BindingConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition().copy();
        /* Get and remove enviroment property */
        configDef.getPropertyDefinitions().remove(ENVIRONMENT);

        /* Load other properties*/
        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef, getASConnection(), address,
                includeRuntime);
        Configuration configuration = delegate.loadResourceConfiguration();

        Operation op = new Operation("whoami", getAddress());
        Result res = getASConnection().execute(op);

        if (!res.isSuccess()) {
            op = new Operation("read-resource", getAddress());
            res = getASConnection().execute(op);
        }
        includeOOBMessages(res, configuration);

        /* Load environment property*/
        ReadAttribute readAttributeOp = new ReadAttribute(address, ENVIRONMENT);
        PropertyList list = new PropertyList(ENVIRONMENT);
        configuration.put(list);
        Result envOpResult = getASConnection().execute(readAttributeOp);

        if (envOpResult.isSuccess()) {
            Map<String, Object> results = (Map<String, Object>)envOpResult.getResult();
            if (results != null) {
                for(Map.Entry<String, Object> entry: results.entrySet()) {
                    PropertyMap map = new PropertyMap(ENVIRONMENT);
                    map.put(new PropertySimple("name", entry.getKey()));
                    map.put(new PropertySimple("value", entry.getValue()));
                    list.add(map);
                }
            }
        }
        return configuration;
    }

}
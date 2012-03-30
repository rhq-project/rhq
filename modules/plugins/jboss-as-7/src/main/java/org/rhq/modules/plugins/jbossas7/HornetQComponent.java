package org.rhq.modules.plugins.jbossas7;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for HornetQ related stuff
 * @author Heiko W. Rupp
 */
public class HornetQComponent extends BaseComponent {

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {

        String targetTypeName = report.getResourceType().getName();
        String resourceName = report.getUserSpecifiedResourceName();
        Configuration rc = report.getResourceConfiguration();
        Address targetAddress = new Address(getPath());

        CreationType targetType = CreationType.getForName(targetTypeName);
        if (targetType==null)
            throw new IllegalArgumentException("Type " + targetTypeName + " not yet supported");

        targetAddress.add(targetType.as7name,resourceName);
        List<String> entries;

        Result res;
        Operation op = new Operation("read-operation-description",targetAddress);
        op.addAdditionalProperty("name","add");
        ComplexResult cres = getASConnection().executeComplex(op);
        Map<String,Map<String,Object>> definitions;
        if (cres.isSuccess()) {
            definitions = (Map<String, Map<String, Object>>) cres.getResult().get("request-properties");
        }
        else {
            definitions = Collections.emptyMap();
        }

        op = new Operation("add",targetAddress);
        switch (targetType) {
            case JMS_QUEUE:
                entries = getEntriesPropertyFromConfig(rc);
                op.addAdditionalProperty("entries",entries);

                processSimpleProperties(op,rc, definitions);

/*
                String durableProp = rc.getSimpleValue("durable","false");
                op.addAdditionalProperty("durable",Boolean.valueOf(durableProp));

                PropertySimple selectorProp = (PropertySimple) rc.get("selector");
                if (selectorProp!=null && selectorProp.getStringValue()!=null && !selectorProp.getStringValue().isEmpty())
                    op.addAdditionalProperty("selector",selectorProp.getStringValue());
*/

                break;
            case JMS_TOPIC:
                entries = getEntriesPropertyFromConfig(rc);
                op.addAdditionalProperty("entries",entries);

                break;

            case CONNECTION_FACTORY:
                entries = getEntriesPropertyFromConfig(rc);
                op.addAdditionalProperty("entries",entries);

                processSimpleProperties(op,rc,definitions);

                // now handle the connector
                PropertyMap connectorMap = (PropertyMap) rc.get("connector:collapsed");
                Map<String,String> map = new HashMap<String, String>(1);
                String key = connectorMap.getSimpleValue("name:0", null);
                String value = connectorMap.getSimpleValue("backup:1", null);
                if (key==null) {
                    report.setErrorMessage("No connector name given");
                    report.setStatus(CreateResourceStatus.FAILURE);
                    return report;
                }
                map.put(key, value);
                op.addAdditionalProperty("connector",map);

                break;
        }


        res = getASConnection().execute(op);
        if (res.isSuccess()) {
            report.setResourceKey(targetAddress.getPath());
            report.setResourceName(resourceName);
            report.setStatus(CreateResourceStatus.SUCCESS);
        } else {
            report.setErrorMessage(res.getFailureDescription());
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setException(res.getRhqThrowable());
        }
        return report;
    }


    /**
     * Add the data from the simple properties as additional properties to the passed operation
     * @param op Operation
     * @param rc configuration with the properties to use
     * @param definitions AS7 metadata
     */
    private void processSimpleProperties(Operation op, Configuration rc, Map<String, Map<String, Object>> definitions) {
        Map<String,PropertySimple> properties = rc.getSimpleProperties();
        for (PropertySimple ps : properties.values()) {

            Map<String,Object> def = definitions.get(ps.getName());
            String value = ps.getStringValue();

            if (value!=null) {
                Object o = getValueAndType(value,def);
                op.addAdditionalProperty(ps.getName(),o);
            }
        }
    }

    private Object getValueAndType(String value, Map<String, Object> def) {

        Object ret;
        DataType dt = getTypeFromProps(def);
        switch (dt) {
            case BOOLEAN:
                ret = Boolean.valueOf(value);
                break;
            case INT:
                ret = Integer.valueOf(value);
                break;
            case LONG:
                ret = Long.valueOf(value);
                break;
            case BIG_DECIMAL:
                ret = Double.valueOf(value);
                break;
            case DOUBLE:
                ret = Double.valueOf(value);
                break;

            default:
                ret = value;
        }

        return ret;
    }

    private DataType getTypeFromProps(Map<String, Object> props) {
        Map<String,String> tMap = (Map<String, String>) props.get("type");
        if (tMap==null)
            return DataType.OBJECT;

        String type = tMap.get("TYPE_MODEL_VALUE");
        DataType ret = DataType.valueOf(type);

        return ret;
    }

    /**
     * Get the JNDI entries
     * @param rc Configuration parameters
     * @return List of string entries
     */
    private List<String> getEntriesPropertyFromConfig(Configuration rc) {
        PropertyList entriesListProp = (PropertyList) rc.get("entries");
        List<Property> psl = entriesListProp.getList();
        List<String> entries = new ArrayList<String>(psl.size());
        for (Property p :psl) {
            PropertySimple ps = (PropertySimple) p;
            entries.add(ps.getStringValue());
        }
        return entries;
    }

    /**
     * Supported types for resource creation
     */
    enum CreationType {
        JMS_QUEUE("JMS Queue","jms-queue"),
        JMS_TOPIC("JMS Topic","jms-topic"),
        CONNECTION_FACTORY("Connection-Factory","connection-factory")
        ;

        private String descriptorName;
        private String as7name;

        /**
         * Create a Type
         * @param descriptorName type name as it is listed in the plugin descriptor
         * @param as7name the type by which as7 knows it.
         */
        CreationType(String descriptorName, String as7name) {

            this.descriptorName = descriptorName;
            this.as7name = as7name;
        }

        public static CreationType getForName(String targetTypeName) {
            EnumSet<CreationType> set = EnumSet.allOf(CreationType.class);
            for (CreationType t : set) {
                if (t.descriptorName.equals(targetTypeName))
                    return t;
            }
            return null;
        }
    }

    public enum DataType {

        STRING(false,"string"),
        INT(true,"integer"),
        BOOLEAN(false,"boolean"),
        LONG(true,"long"),
        BIG_DECIMAL(true,"long"),
        OBJECT(false,"-object-"),
        LIST(false,"-list-"),
        DOUBLE(true,"long")
        ;

        private boolean numeric;
        private String rhqName;

        private DataType(boolean numeric, String rhqName) {
            this.numeric = numeric;
            this.rhqName = rhqName;
        }

        public boolean isNumeric() {
            return numeric;
        }
    }

}

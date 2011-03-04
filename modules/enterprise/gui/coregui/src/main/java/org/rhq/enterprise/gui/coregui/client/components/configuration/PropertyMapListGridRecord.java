package org.rhq.enterprise.gui.coregui.client.components.configuration;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

import java.util.List;

/**
 * A ListGridRecord representation of an RHQ PropertyMap.
 *
 * @author Ian Springer
 */
public class PropertyMapListGridRecord extends ListGridRecord {

    private AbstractPropertyMap propertyMap;
    private int index;
    private List<PropertyDefinition> memberPropertyDefinitions;

    public PropertyMapListGridRecord(AbstractPropertyMap propertyMap, int index,
                                     List<PropertyDefinition> memberPropertyDefinitions) {
        this.propertyMap = propertyMap;
        this.index = index;
        this.memberPropertyDefinitions = memberPropertyDefinitions;

        refresh();
    }

    public AbstractPropertyMap getPropertyMap() {
        return propertyMap;
    }

    public int getIndex() {
        return index;
    }

    public void refresh() {
        for (PropertyDefinition subDef : memberPropertyDefinitions) {
            PropertyDefinitionSimple subDefSimple = (PropertyDefinitionSimple) subDef;
            String propertyName = subDefSimple.getName();
            PropertySimple propertySimple = propertyMap.getSimple(propertyName);

            if ((propertySimple != null) && (propertySimple.getStringValue() != null)) {
                switch (((PropertyDefinitionSimple) subDef).getType()) {
                    case BOOLEAN:
                        setAttribute(propertyName, propertySimple.getBooleanValue());
                        break;
                    case INTEGER:
                        setAttribute(propertyName, propertySimple.getIntegerValue());
                        break;
                    case FLOAT:
                        setAttribute(propertyName, propertySimple.getFloatValue());
                        break;
                    case DOUBLE:
                        setAttribute(propertyName, propertySimple.getDoubleValue());
                        break;
                    default:
                        setAttribute(propertyName, propertySimple.getStringValue());
                        break;
                }
            } else {
                setAttribute(propertyName, (String) null);
            }
        }
    }

}

/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.components.configuration;

import java.util.Map;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;

/**
 * @author Ian Springer
 */
public class PropertyValueChangeEvent {

    private Property property;
    private PropertyDefinition propertyDefinition;
    private boolean invalidPropertySetChanged;
    private Map<String, String> invalidPropertyNames;

    public PropertyValueChangeEvent(Property property, PropertyDefinition propertyDefinition, boolean invalidPropertySetChanged,
                                    Map<String, String> invalidPropertyNames) {
        this.property = property;
        this.propertyDefinition = propertyDefinition;
        this.invalidPropertySetChanged = invalidPropertySetChanged;
        this.invalidPropertyNames = invalidPropertyNames;
    }

    public Property getProperty() {
        return property;
    }

    public PropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

    public boolean isInvalidPropertySetChanged() {
        return invalidPropertySetChanged;
    }

    public Map<String, String> getInvalidPropertyNames() {
        return invalidPropertyNames;
    }

}

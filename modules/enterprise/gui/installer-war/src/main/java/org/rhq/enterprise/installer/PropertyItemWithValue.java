/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.installer;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PropertyItemWithValue {
    private PropertyItem itemDefinition;
    private String value;

    public PropertyItemWithValue(PropertyItem itemDefinition, String value) {
        setItemDefinition(itemDefinition);
        setValue(value);
    }

    public PropertyItem getItemDefinition() {
        return itemDefinition;
    }

    public void setItemDefinition(PropertyItem itemDefinition) {
        this.itemDefinition = itemDefinition;
    }

    public String getValue() {
        return value;
    }

    /**
     * Sets the property's value.  If the given value is null or empty,
     * a default will be assigned depending on  the property's type.
     * 
     * @param value the new property value
     */
    public void setValue(String value) {
        if ((value == null) || (value.trim().length() <= 0)) {
            Class<?> propertyType = this.itemDefinition.getPropertyType();

            if (Boolean.class.isAssignableFrom(propertyType)) {
                value = Boolean.FALSE.toString();
            } else if (Number.class.isAssignableFrom(propertyType)) {
                value = "0";
            } else if (InetAddress.class.isAssignableFrom(propertyType)) {
                try {
                    InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    value = "127.0.0.1";
                }
            } else {
                value = "";
            }
        }

        this.value = value;
    }

    /**
     * Sets the property's value. No matter what value is, it will
     * be stored as-is.  See {@link #setValue(String)} for different behavior.
     * 
     * @param value the new value
     */
    public void setRawValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.itemDefinition.getPropertyName() + "=" + this.value;
    }
}
/*
 * Jopr Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas5.adapter.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

import org.jboss.metatype.api.values.MetaValue;

/**
 * A base class for {@link PropertySimple} <-> ???MetaValue adapters.
 *
 * @author Ian Springer
 */
public abstract class AbstractPropertySimpleAdapter implements PropertyAdapter<PropertySimple, PropertyDefinitionSimple>
{
    private final Log log = LogFactory.getLog(this.getClass());

    public PropertySimple convertToProperty(MetaValue metaValue, PropertyDefinitionSimple propDefSimple)
    {
        PropertySimple propSimple = new PropertySimple(propDefSimple.getName(), null);
        populatePropertyFromMetaValue(propSimple, metaValue, propDefSimple);
        return propSimple;
    }

    public void populateMetaValueFromProperty(PropertySimple propSimple, MetaValue metaValue,
                                              PropertyDefinitionSimple propDefSimple)
    {
        if (metaValue == null) {
            throw new IllegalArgumentException("MetaValue to be populated is null.");
        }
        String value = (propSimple != null) ? propSimple.getStringValue() : null;
        if (value == null && metaValue.getMetaType().isPrimitive()) {
            // For primitive types, if we set the value to null, the Profile Service will set the value to 0, which
            // is not good, so for such properties, we make sure to define a default value in the plugin descriptor, so
            // we can explicitly set the value to that default, rather than null, here.
            value = propDefSimple.getDefaultValue();
            if (value == null) {
                log.warn("Plugin error: Managed property [" + propDefSimple.getName()
                        + "] has a primitive type, but the corresponding RHQ property definition does not provide a default value.");
            }
        }
        setInnerValue(value, metaValue, propDefSimple);
    }

    protected abstract void setInnerValue(String propSimpleValue, MetaValue metaValue, PropertyDefinitionSimple propDefSimple);
}

/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.metatype.api.values.CollectionValue;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.MetaValue;

/**
 * Utility methods for converting various Profile Service objects into Strings for debugging purposes.
 *
 * @author Ian Springer
 */
public abstract class DebugUtils
{
    public static String convertPropertiesToString(ManagedComponent managedComponent)
    {
        StringBuilder buf = new StringBuilder();
        String componentTypeName = managedComponent.getType().getSubtype() + " " + managedComponent.getType().getType();
        buf.append("Properties for [").append(componentTypeName).append("] ManagedComponent [");
        buf.append(managedComponent.getName()).append("]:\n");
        buf.append(convertPropertiesToString(managedComponent.getProperties()));
        return buf.toString();
    }

    public static String convertPropertiesToString(DeploymentTemplateInfo template)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Properties for DeploymentTemplateInfo [").append(template.getName()).append("]:\n");
        buf.append(convertPropertiesToString(template.getProperties()));
        return buf.toString();
    }

    public static String convertMetaValueToString(MetaValue metaValue)
    {
        StringBuilder buffer = new StringBuilder();
        convertMetaValueToString(metaValue, buffer, true, 1);
        return buffer.toString();
    }

    public static String convertPropertiesToString(Map<String, ManagedProperty> managedProps)
    {
        StringBuilder buf = new StringBuilder();
        List<ManagedProperty> props = new ArrayList<ManagedProperty>(managedProps.values());
        Collections.sort(props, new ManagedPropertyComparator()); // sort by name
        try {
            for (ManagedProperty managedProperty : props) {
                if (managedProperty.isMandatory())
                    buf.append("* ");
                else
                    buf.append("  ");
                buf.append("name=").append(managedProperty.getName());
                if (!managedProperty.getName().equals(managedProperty.getMappedName()))
                    buf.append(", mappedName=").append(managedProperty.getMappedName());
                EnumSet<ViewUse> viewUses = ManagedComponentUtils.getViewUses(managedProperty);
                buf.append(", viewUses=").append(viewUses);
                buf.append(", readOnly=").append(managedProperty.isReadOnly());
                buf.append(", mandatory=").append(managedProperty.isMandatory());
                buf.append(", removed=").append(managedProperty.isRemoved());
                MetaValue value = managedProperty.getValue();
                if (value == null)
                    buf.append(", type=").append(managedProperty.getMetaType());
                buf.append(", value=").append(convertMetaValueToString(value));
            }
        } catch (Exception e) {
            buf.append(" ... Failed to convert properties to string: " + e.getMessage());
        }
        return buf.toString();
    }

    private static void convertMetaValueToString(MetaValue metaValue, StringBuilder buffer, boolean indentFirstLine,
                                                 int indentLevel)
    {
        if (indentFirstLine)
            for (int i = 0; i < indentLevel; i++) buffer.append("  ");
        if (metaValue == null)
        {
            buffer.append("<<<null>>>\n"); // make it stand out a bit
        }
        else if (metaValue.getMetaType().isCollection())
        {
            CollectionValue collectionValue = (CollectionValue)metaValue;
            buffer.append(collectionValue).append("\n");
            /*for (int i = 0; i < indentLevel; i++) buffer.append("  ");
            buffer.append("Elements:\n");
            indentLevel++;
            for (MetaValue elementMetaValue : collectionValue.getElements())
                convertMetaValueToString(elementMetaValue, buffer, true, indentLevel);*/
        }
        else if (metaValue.getMetaType().isComposite())
        {
            CompositeValue compositeValue = (CompositeValue)metaValue;
            buffer.append(compositeValue).append("\n");
            /*for (int i = 0; i < indentLevel; i++) buffer.append("  ");
            buffer.append("Items:\n");
            indentLevel++;
            for (String key : compositeValue.getMetaType().keySet()) {
                for (int i = 0; i < indentLevel; i++) buffer.append("  ");
                buffer.append(key).append("=");
                convertMetaValueToString(compositeValue.get(key), buffer, false, indentLevel);
            }*/
        }
        else
        {
            buffer.append(metaValue).append("\n");
        }
    }

    private static class ManagedPropertyComparator implements Comparator<ManagedProperty>
    {
        /**
         * Use viewUse as primary sort field and name as secondary sort field.
         */
        public int compare(ManagedProperty prop1, ManagedProperty prop2)
        {
            ViewUse prop1ViewUse = getPrimaryViewUse(prop1);
            ViewUse prop2ViewUse = getPrimaryViewUse(prop2);
            if (prop1ViewUse == null)
                return (prop2ViewUse == null) ? 0 : -1;
            if (prop2ViewUse == null)
                return 1;
            int result = prop1ViewUse.name().compareTo(prop2ViewUse.name());
            if (result == 0)
                result = prop1.getName().compareTo(prop2.getName()); // break the tie
            return result;
        }

        @Nullable
        private static ViewUse getPrimaryViewUse(ManagedProperty managedProperty)
        {
            ViewUse viewUse;
            if (managedProperty.hasViewUse(ViewUse.CONFIGURATION))
                viewUse = ViewUse.CONFIGURATION;
            else if (managedProperty.hasViewUse(ViewUse.RUNTIME))
                viewUse = ViewUse.RUNTIME;
            else if (managedProperty.hasViewUse(ViewUse.STATISTIC))
                viewUse = ViewUse.STATISTIC;
            else
                viewUse = null;
            return viewUse;
        }
    }

    private DebugUtils()
    {
    }
}

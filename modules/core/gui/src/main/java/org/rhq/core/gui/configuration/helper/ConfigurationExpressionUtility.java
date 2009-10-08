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
package org.rhq.core.gui.configuration.helper;

import java.util.LinkedList;

import javax.el.ValueExpression;

import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ian Springer
 */
public class ConfigurationExpressionUtility
{
    public static ValueExpression createValueExpressionForPropertyDefiniton(
            String configurationDefinitionExpressionString,
            PropertyDefinition propertyDefinition) {
        StringBuilder stringBuilder = new StringBuilder("#{");
        stringBuilder.append(FacesExpressionUtility.unwrapExpressionString(configurationDefinitionExpressionString));
        LinkedList<PropertyDefinition> propertyDefinitionHierarchy = getPropertyDefinitionHierarchy(propertyDefinition);
        for (PropertyDefinition subPropertyDefinition : propertyDefinitionHierarchy)
        {
            PropertyDefinition parentPropertyDefinition = getParentPropertyDefinition(subPropertyDefinition);
            stringBuilder.append(".");
            if (parentPropertyDefinition == null || parentPropertyDefinition instanceof PropertyDefinitionMap)
            {
                // top-level property or map member property
                stringBuilder.append("propertyDefinitions['").append(subPropertyDefinition.getName()).append("']");
            } else {
                // list member property
                stringBuilder.append("memberDefinition");
            }
        }
        stringBuilder.append("}");
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(stringBuilder.toString(),
                PropertyDefinition.class);
        return valueExpression;
    }

    public static LinkedList<PropertyDefinition> getPropertyDefinitionHierarchy(PropertyDefinition propertyDefinition)
    {
        LinkedList<PropertyDefinition> propertyHierarchy = new LinkedList<PropertyDefinition>();
        PropertyDefinition parentPropertyDefinition = propertyDefinition;
        while ((parentPropertyDefinition = getParentPropertyDefinition(parentPropertyDefinition)) != null)
            propertyHierarchy.addFirst(parentPropertyDefinition);
        propertyHierarchy.add(propertyDefinition);
        return propertyHierarchy;
    }

    @Nullable
    public static PropertyDefinition getParentPropertyDefinition(PropertyDefinition property) {
        PropertyDefinition parentPropertyDefinition;
        if (property.getParentPropertyListDefinition() != null)
            parentPropertyDefinition = property.getParentPropertyListDefinition();
        else if (property.getParentPropertyMapDefinition() != null)
            parentPropertyDefinition = property.getParentPropertyMapDefinition();
        else
            parentPropertyDefinition = null;
        return parentPropertyDefinition;
    }
}

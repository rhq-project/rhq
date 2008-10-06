 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.gui.util;

import java.util.LinkedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;

/*
 * Class used to generate identifiers based on a configuration Property.
 *
 * @author Ian Springer @author Charles Crouch
 */
public class PropertyIdGeneratorUtility {
    private static final String ID_PREFIX = "jon_prop-";
    private static final String ID_DELIMITER = "_";

    /**
     * Generates a unique identifier based on the specified Property. It will look at any parents/grandparents etc. the
     * property has and take those into account too. The configuration itself will also be taken into account, so the
     * identifier will be unique across multiple configurations.
     *
     * @param  property a JON configuration property
     * @param  index    the property's index if its parent is a PropertyList, null otherwise
     *
     * @return a unique identifier based on the specified Property
     */
    public static String getIdentifier(@NotNull
    Property property, @Nullable
    Integer index) {
        String suffix = null;
        return getIdentifier(property, index, suffix);
    }

    /**
     * Generates a unique identifier based on the specified Property. It will look at any parents/grandparents etc. the
     * property has and take those into account too. The configuration itself will also be taken into account, so the
     * identifier will be unique across multiple configurations.
     *
     * @param  property a JON configuration property
     * @param  index    the property's index if its parent is a PropertyList, null otherwise
     * @param  suffix   a suffix to append to the returned id; if null, no suffix will be appended
     *
     * @return a unique identifier based on the specified Property
     */
    public static String getIdentifier(@NotNull
    Property property, @Nullable
    Integer index, @Nullable
    String suffix) {
        //noinspection ConstantConditions
        if (property == null) {
            throw new IllegalArgumentException("Property parameter cannot be null.");
        }

        LinkedList<Property> propertyHierarchy = new LinkedList<Property>();
        Property parentProperty = property;
        while ((parentProperty = getParentProperty(parentProperty)) != null) {
            propertyHierarchy.addFirst(parentProperty);
        }

        propertyHierarchy.add(property);

        StringBuilder identifier = new StringBuilder(ID_PREFIX);

        //noinspection ConstantConditions
        Configuration configuration = propertyHierarchy.getFirst().getConfiguration();

        // NOTE: Use the Configuration's id rather than its hash code, since the id will always be unique across multiple
        //       configs and will not change if the Configuration is modified.
        // TODO (embedded): id will always be zero - perhaps the id could be set to a unique identifier for the entity to
        //                  which the Configuration applies (e.g. for a Resource Configuration, the Resource's path).
        identifier.append(configuration.getId());
        for (Property propertyNode : propertyHierarchy) {
            // NOTE: Use the hash code of the property name rather than the hash code of the property itself, since for
            //       lists and maps, this will be much more performant and just as effective for uniquely identifying a
            //       list or map within a given configuration.
            identifier.append(ID_DELIMITER).append(propertyNode.getName().hashCode());
            if (propertyNode.getParentList() != null) {
                if (index == null) {
                    throw new IllegalStateException("Property " + property
                        + " has a list in its ancestry, but no index was provided.");
                } else {
                    identifier.append(ID_DELIMITER).append(index);
                }
            }
        }

        if (suffix != null) {
            identifier.append(suffix);
        }

        return identifier.toString();
    }

    @Nullable
    private static Property getParentProperty(Property property) {
        Property parentProperty;
        if (property.getParentList() != null) {
            parentProperty = property.getParentList();
        } else if (property.getParentMap() != null) {
            parentProperty = property.getParentMap();
        } else {
            parentProperty = null;
        }

        return parentProperty;
    }
}
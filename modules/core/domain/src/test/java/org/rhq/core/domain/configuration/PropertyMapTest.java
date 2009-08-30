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

package org.rhq.core.domain.configuration;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class PropertyMapTest {

    @Test
    public void deepCopyShouldCopySimpleFields() {
        PropertyMap original = createPropertyMap();
        PropertyMap copy = original.deepCopy();

        assertNotSame(copy, original, "The copy should not reference the original object");

        assertEquals(copy.getName(), original.getName(), "Failed to copy the name property");
        assertEquals(copy.getErrorMessage(), original.getErrorMessage(), "Failed to copy the errorMessage property");
    }

    @Test
    public void deepCopyShouldNotCopyIdField() {
        PropertyMap original = createPropertyMap();
        PropertyMap copy = original.deepCopy();

        assertFalse(copy.getId() == original.getId(), "The original id property should not be copied.");
    }

    @Test
    public void deepCopyShouldNotCopyReferenceOfUnderlyingMap() {
        PropertyMap original = createPropertyMap();
        PropertyMap copy = original.deepCopy();

        assertNotSame(copy.getMap(), original.getMap(), "The values in the underlying map should be copied, not the reference to the map itself");
    }

    @Test
    public void deepCopyShouldCopyProperty() {
        PropertyMap original = createPropertyMap();

        PropertySimple simpleProperty = new PropertySimple("simeplProperty", "Simple Property");
        original.put(simpleProperty);

        PropertyMap copy = original.deepCopy();

        assertEquals(copy.getMap().size(), original.getMap().size(), "Failed to copy simple property contained in original property map");

        assertNotSame(
            copy.getMap().get(simpleProperty.getName()),
            original.getMap().get(simpleProperty.getName()),
            "Properties in the map should be copied by value as opposed to just copying the references"
        );
    }

    @Test
    public void deepCopyShouldSetParentOfCopiedProperty() {
        PropertyMap original = createPropertyMap();

        PropertySimple simpleProperty = new PropertySimple("simpleProperty", "Simple Property");
        original.put(simpleProperty);

        PropertyMap copy = original.deepCopy();

        assertSame(
            copy.get(simpleProperty.getName()).getParentMap(),
            copy,
            "The parentMap property of copied properties should be set to the new PropertyMap"
        );
    }

    private PropertyMap createPropertyMap() {
        PropertyMap map = new PropertyMap("mapProperty");
        map.setId(1);
        map.setErrorMessage("error message");

        // This is done to ensure that the underlying map is initialized.
        map.getMap();

        return map;
    }

}

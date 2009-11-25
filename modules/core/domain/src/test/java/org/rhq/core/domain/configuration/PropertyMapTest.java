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
    public void deepCopyShouldCopyAllSimpleFields() {
        PropertyMap original = createPropertyMap();
        PropertyMap copy = original.deepCopy(true);

        assertNotSame(copy, original, "The copy should not reference the original object");

        assertEquals(copy.getId(), original.getId(), "Failed to copy the id property");
        assertEquals(copy.getName(), original.getName(), "Failed to copy the name property");
        assertEquals(copy.getErrorMessage(), original.getErrorMessage(), "Failed to copy the errorMessage property");
    }

    @Test
    public void deepCopyShouldNotCopyIdFieldWhenFlagIsFalse() {
        PropertyMap original = createPropertyMap();
        PropertyMap copy = original.deepCopy(false);

        assertNotSame(copy, original, "The copy should not reference the original object");

        assertFalse(copy.getId() == original.getId(), "The original id property should not be copied.");
        assertEquals(copy.getName(), original.getName(), "Failed to copy the name property");
        assertEquals(copy.getErrorMessage(), original.getErrorMessage(), "Failed to copy the errorMessage property");
    }

    @Test
    public void deepCopyShouldCopyPropertyWhenIdIncluded() {
        PropertyMap original = createPropertyMap();

        PropertySimple simpleProperty = new PropertySimple("simeplProperty", "Simple Property");
        original.put(simpleProperty);

        PropertyMap copy = original.deepCopy(true);

        assertEquals(copy.getMap().size(), original.getMap().size(), "Failed to copy simple property contained in original property map");

        assertNotSame(
            copy.getMap().get(simpleProperty.getName()),
            original.getMap().get(simpleProperty.getName()),
            "Properties in the map should be copied by value as opposed to just copying the references"
        );
    }

    @Test
    public void deepCopyShouldCopyPropertyWhenIdNotIncluded() {
        PropertyMap original = createPropertyMap();

        PropertySimple simpleProperty = new PropertySimple("simeplProperty", "Simple Property");
        original.put(simpleProperty);

        PropertyMap copy = original.deepCopy(false);

        assertEquals(copy.getMap().size(), original.getMap().size(), "Failed to copy simple property contained in original property map");

        assertNotSame(
            copy.getMap().get(simpleProperty.getName()),
            original.getMap().get(simpleProperty.getName()),
            "Properties in the map should be copied by value as opposed to just copying the references"
        );
    }

    @Test
    public void deepCopyShouldSetParentOfCopiedPropertyWhenIdIncluded() {
        PropertyMap original = createPropertyMap();

        PropertySimple simpleProperty = new PropertySimple("simpleProperty", "Simple Property");
        original.put(simpleProperty);

        PropertyMap copy = original.deepCopy(true);

        assertSame(
            copy.get(simpleProperty.getName()).getParentMap(),
            copy,
            "The parentMap property of copied properties should be set to the new PropertyMap"
        );
    }

    @Test
    public void deepCopyShouldSetParentOfCopiedPropertyWhenIdNotIncluded() {
        PropertyMap original = createPropertyMap();

        PropertySimple simpleProperty = new PropertySimple("simpleProperty", "Simple Property");
        original.put(simpleProperty);

        PropertyMap copy = original.deepCopy(false);

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

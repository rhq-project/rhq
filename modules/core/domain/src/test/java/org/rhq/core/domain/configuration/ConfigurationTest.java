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

/**
 * This class has tests for Configuration just like org.rhq.core.domain.configuration.test.ConfigurationTest. These
 * tests however are just plain, vanilla unit tests whereas the tests in org.rhq.core.domain.configuration.test.ConfigurationTest
 * are slower, longer running integration tests; hence, the separation.
 */
public class ConfigurationTest {

    @Test
    public void deepCopyWithoutProxiesShouldNotReturnReferenceToOriginalObject() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopyWithoutProxies();

        assertNotSame(copy, original, "Expected a reference to a new Configuration object, not the original object being copied");
    }

    @Test
    public void deepCopyWithoutProxiesShouldCopySimpleFields() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopyWithoutProxies();

        assertEquals(copy.getNotes(), original.getNotes(), "Failed to copy the notes property");
        assertEquals(copy.getVersion(), original.getVersion(), "Failed to copy version property");
    }

    @Test
    public void deepCopyWithoutProxiesShouldNotCopyIdProperty() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopyWithoutProxies();

        assertFalse(copy.getId() == original.getId(), "The original id property should not be copied");
    }

    @Test
    public void deepCopyWithoutProxiesShouldCopyProperties() {
        Configuration original = createConfiguration();
        original.put(new PropertySimple("simpleProperty", "Simple Property"));

        Configuration copy = original.deepCopyWithoutProxies();

        assertNotSame(copy.getProperties(), original.getProperties(), "The properties property should not refer to the properties in the original object");
        assertEquals(copy.getProperties(), original.getProperties(), "Failed to copy the contents of the properties collection");
    }

    @Test
    public void deepCopyWithoutProxiesShouldNotReturnCopyReferenceOfOriginalProperty() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopyWithoutProxies();

        assertNotSame(copy.get(propertyName), original.get(propertyName), "Expected a refernce to a new property, not the original property being copied");
    }

    @Test
    public void deepCopyWithoutProxiesShouldSetParentReferenceOfCopiedProperties() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopyWithoutProxies();

        assertSame(
            copy.get(propertyName).getConfiguration(),
            copy,
            "The reference to the parent configuration should point to the newly copied configuration, not the original configuration"
        );
    }

    private Configuration createConfiguration() {
        Configuration config = new Configuration();
        config.setId(1);
        config.setNotes("notes");
        config.setVersion(1L);

        // make sure properties property is initialized
        config.getMap();

        return config;
    }

}

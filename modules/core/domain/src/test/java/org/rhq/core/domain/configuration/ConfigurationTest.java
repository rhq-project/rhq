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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Random;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;

/**
 * This class has tests for Configuration just like org.rhq.core.domain.configuration.test.ConfigurationTest. These
 * tests however are just plain, vanilla unit tests whereas the tests in org.rhq.core.domain.configuration.test.ConfigurationTest
 * are slower, longer running integration tests; hence, the separation.
 */
public class ConfigurationTest {

    @Test
    public void deepCopyShouldNotReturnReferenceToOriginalObjectWhenKeepingIds() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopy(true);

        assertNotSame(copy, original,
            "Expected a reference to a new Configuration object, not the original object being copied");
    }

    @Test
    public void deepCopyShouldNotReturnReferenceToOriginalObjectWhenNotKeepingIds() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopy(false);

        assertNotSame(copy, original,
            "Expected a reference to a new Configuration object, not the original object being copied");
    }

    @Test
    public void deepCopyShouldCopyAllSimpleFieldsWhenKeepingIds() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopy(true);

        assertEquals(copy.getId(), original.getId(), "Failed to copy the id property");
        assertEquals(copy.getNotes(), original.getNotes(), "Failed to copy the notes property");
        assertEquals(copy.getVersion(), original.getVersion(), "Failed to copy version property");
    }

    @Test
    public void deepCopyShouldNotCopyIdWhenNotKeepingIds() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopy(false);

        assertFalse(copy.getId() == original.getId(), "The original id property should not be copied");
        assertEquals(copy.getNotes(), original.getNotes(), "Failed to copy the notes property");
        assertEquals(copy.getVersion(), original.getVersion(), "Failed to copy version property");
    }

    @Test
    public void deepCopyShouldCopyPropertiesWhenKeepingIds() {
        Configuration original = createConfiguration();
        original.put(new PropertySimple("simpleProperty", "Simple Property"));

        Configuration copy = original.deepCopy(true);

        assertNotSame(copy.getProperties(), original.getProperties(),
            "The properties property should not refer to the properties in the original object");
        assertEquals(copy.getProperties(), original.getProperties(),
            "Failed to copy the contents of the properties collection");
    }

    @Test
    public void deepCopyShouldCopyPropertiesWhenNotKeepingIds() {
        Configuration original = createConfiguration();
        original.put(new PropertySimple("simpleProperty", "Simple Property"));

        Configuration copy = original.deepCopy(false);

        assertNotSame(copy.getProperties(), original.getProperties(),
            "The properties property should not refer to the properties in the original object");
        assertEquals(copy.getProperties(), original.getProperties(),
            "Failed to copy the contents of the properties collection");
    }

    @Test
    public void deepCopyShouldNotReturnCopyReferenceOfOriginalPropertyWhenKeepingIds() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopy(true);

        assertNotSame(copy.get(propertyName), original.get(propertyName),
            "Expected a refernce to a new property, not the original property being copied");
    }

    @Test
    public void deepCopyShouldNotReturnCopyReferenceOfOriginalPropertyWhenNotKeepingIds() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopy(false);

        assertNotSame(copy.get(propertyName), original.get(propertyName),
            "Expected a refernce to a new property, not the original property being copied");
    }

    @Test
    public void deepCopyShouldSetParentReferenceOfCopiedPropertiesWhenKeepingIds() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopy(true);

        assertSame(
            copy.get(propertyName).getConfiguration(),
            copy,
            "The reference to the parent configuration should point to the newly copied configuration, not the original configuration");
    }

    @Test
    public void deepCopyShouldSetParentReferenceOfCopiedPropertiesWhenNotKeepingIds() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopy(false);

        assertSame(
            copy.get(propertyName).getConfiguration(),
            copy,
            "The reference to the parent configuration should point to the newly copied configuration, not the original configuration");
    }

    @Test
    public void deepCopyShouldCopyRawConfigurationsWhenKeepingIds() {
        Configuration original = createConfiguration();
        original.addRawConfiguration(createRawConfiguration("/tmp/foo"));

        Configuration copy = original.deepCopy(true);

        assertNotSame(copy.getRawConfigurations(), original.getRawConfigurations(),
            "The rawConfigurations property should not refer to the original rawConfigurations of the copied object.");

        assertRawConfigurationsEquals(copy.getRawConfigurations(), original.getRawConfigurations(),
            "Failed to copy rawConfigurations property.");
    }

    @Test
    public void deepCopyShouldCopyRawConfigurationsWhenNotKeepingIds() {
        Configuration original = createConfiguration();
        original.addRawConfiguration(createRawConfiguration("/tmp/foo"));

        Configuration copy = original.deepCopy(false);

        assertNotSame(copy.getRawConfigurations(), original.getRawConfigurations(),
            "The rawConfigurations property should not refer to the original rawConfigurations of the copied object.");

        assertRawConfigurationsEquals(copy.getRawConfigurations(), original.getRawConfigurations(),
            "Failed to copy rawConfigurations property.");
    }

    void assertRawConfigurationsEquals(Set<RawConfiguration> actual, Set<RawConfiguration> expected, String msg) {
        assertEquals(actual.size(), expected.size(), msg
            + " -- The rawConfigurations set has the wrong number of elements.");
        for (RawConfiguration rawConfig : expected) {
            assertTrue(actual.contains(rawConfig), msg + " -- Failed to find " + rawConfig);
        }
    }

    @Test
    public void deepCopyShouldSetParentReferenceOfCopiedRawConfigurationsWhenKeepingIds() {
        Configuration original = createConfiguration();
        original.addRawConfiguration(createRawConfiguration("/tmp/foo"));

        Configuration copy = original.deepCopy(true);
        RawConfiguration copiedRawConfig = getCopiedRawConfiguration(copy);

        assertSame(
            copiedRawConfig.getConfiguration(),
            copy,
            "The reference to the parent configuration should point to the newly copied configuration, not the original configuration.");
    }

    @Test
    public void deepCopyShouldSetParentReferenceOfCopiedRawConfigurationsWhenNotKeepingIds() {
        Configuration original = createConfiguration();
        original.addRawConfiguration(createRawConfiguration("/tmp/foo"));

        Configuration copy = original.deepCopy(false);
        RawConfiguration copiedRawConfig = getCopiedRawConfiguration(copy);

        assertSame(
            copiedRawConfig.getConfiguration(),
            copy,
            "The reference to the parent configuration should point to the newly copied configuration, not the original configuration.");
    }

    private RawConfiguration getCopiedRawConfiguration(Configuration config) {
        for (RawConfiguration rawConfig : config.getRawConfigurations()) {
            return rawConfig;
        }
        return null;
    }

    @Test
    public void equalsShouldBeFalseWhenArgumentIsNull() {
        Configuration config = new Configuration();

        assertFalse(config.equals(null), "equals() should be false for null argument.");
    }

    @Test
    public void equalsShouldBeFalseWhenArgumentIsNotAConfiguration() {
        Configuration config = new Configuration();

        assertFalse(config.equals(new Object()), "equals should return false when argument is not a "
            + Configuration.class.getSimpleName());
    }

    @Test
    public void equalsShouldBeReflexive() {
        Configuration config = new Configuration();

        assertTrue(config.equals(config), "equals() should be reflexive.");
    }

    @Test
    public void equalsAndHashCodeShouldBeSymmetricWhenBothConfigurationsAreEmpty() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();

        assertTrue(c1.equals(c2) && c2.equals(c1), "equals() should be true and symmetric when both configs are empty.");
        assertEquals(c1.hashCode(), c2.hashCode(), "hashCodes should be the same when equals() returns true.");
    }

    @Test
    public void equalsAndHashCodeShouldBeSymmetricWhenConfigsHaveEqualStructuredAndNoRaw() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));

        assertTrue(c1.equals(c2) && c2.equals(c1),
            "equals() should be true and symmetric when structured configs are equal and there are no raw configs.");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void equalsAndHashCodeShouldBeSymmetricWhenConfigsHaveEqualRawAndNoStructured() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        assertTrue(c1.equals(c2) && c2.equals(c1),
            "equals() should be true and symmetric when raw configs are equal and there are no structured configs.");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void equalsAndHashCodeShouldBeTransitiveWhenConfigurationsAreEmpty() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();
        Configuration c3 = new Configuration();

        assertTrue(c1.equals(c2) && c2.equals(c3), "equals() should be true when configs are empty.");
        assertTrue(c1.equals(c3), "equals should be transitive.");

        assertEquals(c1.hashCode(), c3.hashCode(), "hashCodes should be the same when equals returns true.");
    }

    @Test
    public void equalsAndHashCodeShouldBeTransitiveWhenConfigsHaveEqualStructuredAndNoRaw() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));

        Configuration c3 = new Configuration();
        c3.put(new PropertySimple("foo", "bar"));

        assertTrue(c1.equals(c2) && c2.equals(c3),
            "equals() should be true when structured configs are equal and there are no raw configs.");
        assertTrue(c1.equals(c3), "equals should be transitive.");

        assertEquals(c1.hashCode(), c3.hashCode(), "hashCodes should be the same when equals() returns true.");
    }

    @Test
    public void equalsAndHashCodeShouldBeTransitiveWhenConfigsHaveEqualRawAndNoStructured() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        Configuration c3 = new Configuration();
        c3.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        assertTrue(c1.equals(c2) && c2.equals(c3),
            "equals() should be true when raw configs are equal and there are no structured configs.");
        assertTrue(c1.equals(c3), "equals should be transitive.");

        assertEquals(c1.hashCode(), c3.hashCode(), "hashCodes should be the same when equals() returns true.");
    }

    @Test
    public void equalsShouldBeFalseWhenOneConfigHasStructuredAndTheOtherDoesNot() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));

        Configuration c2 = new Configuration();

        assertFalse(c1.equals(c2), "equals() should be false when one config has structured and the other does not");
    }

    @Test
    public void equalsShouldBeFalseWhenBothHaveStructuredButNotRaw() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));

        assertFalse(c1.equals(c2), "equals() should be false when one config has raw and the other does not.");
    }

    @Test
    public void equalsShouldBeFalseWhenBothHaveRawButNotStructured() {
        RawConfiguration r1 = createRawConfiguration("/tmp/foo");
        Configuration c1 = new Configuration();
        c1.addRawConfiguration(r1);
        c1.put(new PropertySimple("foo", "bar"));

        RawConfiguration r2 = createCopyOfRawConfiguration(r1);
        Configuration c2 = new Configuration();
        c2.addRawConfiguration(r2);

        assertFalse(c1.equals(c2), "equals() should be false when one config has structured and the other does not.");
    }

    @Test
    public void equalsShouldBeFalseWhenStructuredAreUnequalAndRawsAreEqual() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("bar", "foo"));
        c2.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        assertFalse(c1.equals(c2), "equals() should be false when structured configs are not equal.");
    }

    @Test
    public void equalsShouldBeFalseWhenStructuredAreEqualAndRawAreUnequal() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));
        c1.addRawConfiguration(createRawConfiguration("/tmp/foo"));

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));
        c2.addRawConfiguration(createRawConfiguration("/tmp/bar"));

        assertFalse(c1.equals(c2), "equals() should be false when raw configs are not equal.");
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

    private RawConfiguration createRawConfiguration(String path) {
        byte[] bytes = new byte[10];

        Random random = new Random();
        random.nextBytes(bytes);

        RawConfiguration rawConfig = new RawConfiguration();
        rawConfig.setPath(path);
        String contents = new String(bytes);
        String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(contents);
        rawConfig.setContents(contents, sha256);

        return rawConfig;
    }

    private RawConfiguration createCopyOfRawConfiguration(RawConfiguration rawConfig) {
        RawConfiguration copy = new RawConfiguration();
        copy.setPath(rawConfig.getPath());
        String contents = rawConfig.getContents();
        String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(contents);
        copy.setContents(contents, sha256);
        copy.setConfiguration(rawConfig.getConfiguration());

        return copy;
    }

}

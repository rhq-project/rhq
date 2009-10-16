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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.testng.annotations.Test;

public class RawConfigurationTest {

    @Test
    public void getContentsShouldReturnADeepCopyOfArray() {
        RawConfiguration rawConfig = new RawConfiguration();
        rawConfig.setContents(getBytes());

        byte[] contents = rawConfig.getContents();
        contents[0] = 5;

        assertEquals(
            rawConfig.getContents(),
            getBytes(),
            "The contents property should only be mutable through setContents(). Therefore getContents() must return a copy of the underlying array."
        ); 
    }

    @Test
    public void setContentsShouldUpdateContentsArrayWithCopyOfSpecifiedValue() {
        byte[] bytes = getBytes();

        RawConfiguration rawConfig = new RawConfiguration();
        rawConfig.setContents(bytes);

        bytes[0] = 9;

        assertEquals(
            rawConfig.getContents(),
            getBytes(),
            "setContents() should update the underlying array to refer to a copy of the incoming array to enforce the contents property being mutable only through setContents()."
        );
    }

    byte[] getBytes() {
        return new byte[] {1, 2, 3};
    }

    @Test
    public void sha256ShouldChangeWhenContentsChange() throws Exception {
        RawConfiguration rawConfig = new RawConfiguration();
        rawConfig.setContents(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        String actualSha256 = rawConfig.getSha256();

        String expectedSha256 = calculateSHA256(rawConfig.getContents());

        assertEquals(actualSha256, expectedSha256, "Failed to calculate the SHA-256 correctly.");

        byte[] contents = rawConfig.getContents();
        contents[4] = 15;
        rawConfig.setContents(contents);

        actualSha256 = rawConfig.getSha256();

        expectedSha256 = calculateSHA256(rawConfig.getContents());

        assertEquals(actualSha256, expectedSha256, "Failed to update sha256 when contents property changes");
    }

    String calculateSHA256(byte[] data) throws DecoderException {
        return DigestUtils.sha256Hex(data);
    }

    @Test
    public void verifyEqualsWhenObjectIsNull() {
        RawConfiguration rawConfig = new RawConfiguration();

        assertFalse(rawConfig.equals(null), "equals() should return false when incoming object is null.");
    }

    @Test
    public void verifyEqualsWhenObjectIsNotARawConfiguration() {
        RawConfiguration rawConfig = new RawConfiguration();

        assertFalse(rawConfig.equals(new Object()), "equals() should return false when incoming object is not a " +
            RawConfiguration.class.getSimpleName());
    }

    @Test
    public void verifyEqualsWhenNeitherRawConfigurationHasContentsOrPathSet() {
        RawConfiguration r1 = new RawConfiguration();
        RawConfiguration r2 = new RawConfiguration();

        assertFalse(r1.equals(r2), "equals() should return false when contents and path is null for both raw configs");
    }

    @Test
    public void verifyEqualsWhenRawConfigurationArgHasNullContents() {
        RawConfiguration r1 = new RawConfiguration();
        r1.setContents(getBytes());

        RawConfiguration r2 = new RawConfiguration();

        assertFalse(r1.equals(r2), "equals() should return false when contents is null for one of the objects.");
    }

    @Test
    public void verifyEqualsWhenRawConfigurationArgHasNullPath() {
        RawConfiguration r1 = new RawConfiguration();
        r1.setContents(getBytes());
        r1.setPath("/tmp/foo");

        RawConfiguration r2 = new RawConfiguration();
        r2.setContents(getBytes());

        assertFalse(r1.equals(r2), "equals() should return false when one of the raw configs does not have its paths set");
    }

    @Test
    public void verifyEqualsIsReflexive() {
        RawConfiguration rawConfig = new RawConfiguration();

        assertTrue(rawConfig.equals(rawConfig), "equals() should be reflexive.");
    }

    @Test
    public void verifyEqualsAndHashCodeAreSymmetricWhenPathIsNull() {
        RawConfiguration r1 = new RawConfiguration();
        r1.setContents(getBytes());

        RawConfiguration r2 = new RawConfiguration();
        r2.setContents(getBytes());

        assertTrue(r1.equals(r2), "equals() should be true when contents are the same and path is null for both.");
        assertTrue(r2.equals(r1), "equals() should be symmetric.");

        assertEquals(r1.hashCode(), r2.hashCode(), "hashCode() should be the same for two objects that symmetric.");
    }

    @Test
    public void verifyEqualsAndHashCodeAreSymmetricWhenPathIsNotNull() {
        RawConfiguration r1 = new RawConfiguration();
        r1.setContents(getBytes());
        r1.setPath("/tmp/foo");

        RawConfiguration r2 = new RawConfiguration();
        r2.setContents(getBytes());
        r2.setPath("/tmp/foo");

        assertTrue(r1.equals(r2), "equals() should be true when contents and paths are the same.");
        assertTrue(r2.equals(r1), "equals() should be symmetric.");

        assertEquals(r1.hashCode(), r2.hashCode(), "hashCode() should be the same for two objects that symmetric.");
    }

    @Test
    public void verifyEqualsAndHashCodeTransitiveWhenPathIsNull() {
        RawConfiguration r1 = new RawConfiguration();
        r1.setContents(getBytes());

        RawConfiguration r2 = new RawConfiguration();
        r2.setContents(getBytes());

        RawConfiguration r3 = new RawConfiguration();
        r3.setContents(getBytes());

        assertTrue(r1.equals(r2) && r2.equals(r3), "equals() should be true when contents are the same and paths are null.");
        assertTrue(r1.equals(r3), "equals() should be transitive when contents are the same and paths are null.");

        assertEquals(r1.hashCode(), r3.hashCode(), "hashCode() should be the same for r1 and r3 via transitivity.");
    }

    @Test
    public void verifyEqualsAndHashCodeTransitiveWhenPathIsNotNull() {
        RawConfiguration r1 = new RawConfiguration();
        r1.setContents(getBytes());
        r1.setPath("/tmp/foo");

        RawConfiguration r2 = new RawConfiguration();
        r2.setContents(getBytes());
        r2.setPath("/tmp/foo");

        RawConfiguration r3 = new RawConfiguration();
        r3.setContents(getBytes());
        r3.setPath("/tmp/foo");

        assertTrue(r1.equals(r2) && r2.equals(r3), "equals() should be true when contents and paths are the same.");
        assertTrue(r1.equals(r3), "equals() should be transitive when contents and paths are the same.");

        assertEquals(r1.hashCode(), r3.hashCode(), "hashCode() should be the same for r1 and r3 via transitivity.");
    }
}

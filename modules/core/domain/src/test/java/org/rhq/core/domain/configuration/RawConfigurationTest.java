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
import static org.testng.Assert.assertTrue;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;

public class RawConfigurationTest {

    @Test
    public void sha256ShouldChangeWhenContentsChange() throws Exception {
        RawConfiguration rawConfig = new RawConfiguration();
        setContentsAndSha256(rawConfig, "contents");

        String actualSha256 = rawConfig.getSha256();

        String expectedSha256 = calculateSHA256(rawConfig.getContents());

        assertEquals(actualSha256, expectedSha256, "Failed to calculate the SHA-256 correctly.");

        String newContents = "new contents";
        setContentsAndSha256(rawConfig, newContents);

        actualSha256 = rawConfig.getSha256();

        expectedSha256 = calculateSHA256(rawConfig.getContents());

        assertEquals(actualSha256, expectedSha256, "Failed to update sha256 when contents property changes");
    }

    String calculateSHA256(String string) throws DecoderException {
        return DigestUtils.sha256Hex(string);
    }

    @Test
    public void verifyEqualsWhenObjectIsNull() {
        RawConfiguration rawConfig = new RawConfiguration();

        assertFalse(rawConfig.equals(null), "equals() should return false when incoming object is null.");
    }

    @Test
    public void verifyEqualsWhenObjectIsNotARawConfiguration() {
        RawConfiguration rawConfig = new RawConfiguration();

        assertFalse(rawConfig.equals(new Object()), "equals() should return false when incoming object is not a "
            + RawConfiguration.class.getSimpleName());
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
        setContentsAndSha256(r1, "contents");

        RawConfiguration r2 = new RawConfiguration();

        assertFalse(r1.equals(r2), "equals() should return false when contents is null for one of the objects.");
    }

    @Test
    public void verifyEqualsWhenRawConfigurationArgHasNullPath() {
        RawConfiguration r1 = new RawConfiguration();
        setContentsAndSha256(r1, "contents");
        r1.setPath("/tmp/foo");

        RawConfiguration r2 = new RawConfiguration();
        setContentsAndSha256(r2, "contents");

        assertFalse(r1.equals(r2),
            "equals() should return false when one of the raw configs does not have its paths set");
    }

    @Test
    public void verifyEqualsIsReflexive() {
        RawConfiguration rawConfig = new RawConfiguration();

        assertTrue(rawConfig.equals(rawConfig), "equals() should be reflexive.");
    }

    @Test
    public void verifyEqualsAndHashCodeAreSymmetricWhenPathIsNull() {
        RawConfiguration r1 = new RawConfiguration();
        setContentsAndSha256(r1, "contents");

        RawConfiguration r2 = new RawConfiguration();
        setContentsAndSha256(r2, "contents");

        assertTrue(r1.equals(r2), "equals() should be true when contents are the same and path is null for both.");
        assertTrue(r2.equals(r1), "equals() should be symmetric.");

        assertEquals(r1.hashCode(), r2.hashCode(), "hashCode() should be the same for two objects that symmetric.");
    }

    @Test
    public void verifyEqualsAndHashCodeAreSymmetricWhenPathIsNotNull() {
        RawConfiguration r1 = new RawConfiguration();
        setContentsAndSha256(r1, "contents");
        r1.setPath("/tmp/foo");

        RawConfiguration r2 = new RawConfiguration();
        setContentsAndSha256(r2, "contents");
        r2.setPath("/tmp/foo");

        assertTrue(r1.equals(r2), "equals() should be true when contents and paths are the same.");
        assertTrue(r2.equals(r1), "equals() should be symmetric.");

        assertEquals(r1.hashCode(), r2.hashCode(), "hashCode() should be the same for two objects that symmetric.");
    }

    @Test
    public void verifyEqualsAndHashCodeTransitiveWhenPathIsNull() {
        String contents = "contents";

        RawConfiguration r1 = new RawConfiguration();
        setContentsAndSha256(r1, contents);

        RawConfiguration r2 = new RawConfiguration();
        setContentsAndSha256(r2, contents);

        RawConfiguration r3 = new RawConfiguration();
        setContentsAndSha256(r3, contents);

        assertTrue(r1.equals(r2) && r2.equals(r3),
            "equals() should be true when contents are the same and paths are null.");
        assertTrue(r1.equals(r3), "equals() should be transitive when contents are the same and paths are null.");

        assertEquals(r1.hashCode(), r3.hashCode(), "hashCode() should be the same for r1 and r3 via transitivity.");
    }

    @Test
    public void verifyEqualsAndHashCodeTransitiveWhenPathIsNotNull() {
        String contents = "contents";

        RawConfiguration r1 = new RawConfiguration();
        setContentsAndSha256(r1, contents);
        r1.setPath("/tmp/foo");

        RawConfiguration r2 = new RawConfiguration();
        setContentsAndSha256(r2, contents);
        r2.setPath("/tmp/foo");

        RawConfiguration r3 = new RawConfiguration();
        setContentsAndSha256(r3, contents);
        r3.setPath("/tmp/foo");

        assertTrue(r1.equals(r2) && r2.equals(r3), "equals() should be true when contents and paths are the same.");
        assertTrue(r1.equals(r3), "equals() should be transitive when contents and paths are the same.");

        assertEquals(r1.hashCode(), r3.hashCode(), "hashCode() should be the same for r1 and r3 via transitivity.");
    }

    @Test
    public void deepCopyShouldCopyPathAndIdWhenIdIncluded() {
        RawConfiguration original = new RawConfiguration();
        original.setPath("/tmp/foo");
        original.setId(-1);

        RawConfiguration copy = original.deepCopy(true);

        assertEquals(copy.getId(), copy.getId(), "Failed to copy the id property.");
        assertEquals(copy.getPath(), original.getPath(), "Failed to copy the path property.");
    }

    @Test
    public void deepCopyShouldCopyPathAndIdWhenIdNotIncluded() {
        RawConfiguration original = new RawConfiguration();
        original.setPath("/tmp/foo");
        original.setId(-1);

        RawConfiguration copy = original.deepCopy(false);

        assertFalse(copy.getId() == original.getId(), "The original id property should not be copied.");
        assertEquals(copy.getPath(), original.getPath(), "Failed to copy the path property.");
    }

    @Test
    public void deepCopyShouldCopyContentsWhenCopyingId() {
        RawConfiguration original = new RawConfiguration();
        setContentsAndSha256(original, "contents");

        RawConfiguration copy = original.deepCopy(true);

        assertEquals(original.getContents(), copy.getContents(), "Failed to copy the contents property");
        assertEquals(copy.getContents(), original.getContents(), "Failed to copy contents property.");
    }

    @Test
    public void deepCopyShouldCopyContentsWhenNotCopyingId() {
        RawConfiguration original = new RawConfiguration();
        setContentsAndSha256(original, "contents");

        RawConfiguration copy = original.deepCopy(false);

        assertEquals(original.getContents(), copy.getContents(), "Failed to copy the contents property");
        assertEquals(copy.getContents(), original.getContents(), "Failed to copy contents property.");
    }

    private void setContentsAndSha256(RawConfiguration rc, String contents) {
        String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(contents);
        rc.setContents(contents, sha256);
    }
}

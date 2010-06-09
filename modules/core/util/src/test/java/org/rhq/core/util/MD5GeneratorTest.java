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
package org.rhq.core.util;

import org.testng.annotations.Test;

/**
 * Tests MD5 generation.
 *
 * @author John Mazzitelli
 */
@Test
public class MD5GeneratorTest {
    /**
     * Tests generating the MD5 of a String.
     */
    public void testString() {
        String md5 = MessageDigestGenerator.getDigestString("calculate MD5 of this String!\n");
        assert md5.equals("ac98d9c00ea0d821cd757b0f3c628c99") : "Invalid MD5 was " + md5;

        String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256)
            .calcDigestString("calculate SHA256 of this String!");
        assert sha256.equals("10ee77d3c1ccf45c15c360ecc4f8847136d64b594b0eedc4dd2e67bde4ec8100") : "Invalid SHA256 was "
            + sha256;

    }

    public void testEmpty() {
        MessageDigestGenerator md5 = new MessageDigestGenerator();
        assert md5.getDigestString().equals("d41d8cd98f00b204e9800998ecf8427e"); // empty data results in this MD5
    }
}
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

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
        //StringUtils stringUtils = new StringUtils();
//        byte[] shaBytes = new Hex().encode(data);
//        return DigestUtils.shaHex(shaBytes);
        //byte[] shaBytes = DigestUtils.sha(data);
        //return DigestUtils.sha
        return DigestUtils.sha256Hex(data);
//        Base64 base64 = new Base64();
//        return base64.encode(shaBytes).toString();
//        char[] chars = Hex.encodeHex(shaBytes);
//
//        return new String(chars);
    }

    byte[] getFileBytes() throws Exception {
        return FileUtils.readFileToByteArray(new File("/home/jsanda/test.txt"));
    }

}

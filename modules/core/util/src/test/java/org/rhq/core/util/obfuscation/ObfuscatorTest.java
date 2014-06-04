/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

package org.rhq.core.util.obfuscation;

import java.io.ByteArrayOutputStream;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Lukas Krejci
 */
@Test
public class ObfuscatorTest {

    public void testCanDecodePassword() throws Exception {
        testEncodeDecode("0_[");
    }

    public void testDecodeLongData() throws Exception {
        byte[] testBytes = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ `1234567890-=~!@#$%^&*()_+ []\\{}|;':\",./<>?\n"
            .getBytes();
        ByteArrayOutputStream fullTestBytes = new ByteArrayOutputStream();
        for (int i = 0; i < 500; i++) {
            fullTestBytes.write(testBytes);
        }
        testEncodeDecode(fullTestBytes.toString());
    }

    /**
     * @link https://issues.jboss.org/browse/SECURITY-344
     * @throws Exception
     */
    public void testResilienceAgainstZeroPadding() throws Exception {
        testEncodeDecode("dv");
    }

    /**
     * @link https://issues.jboss.org/browse/SECURITY-563
     * @throws Exception
     */
    public void testResilienceAgainstMinusOnePadding() throws Exception {
        testEncodeDecode("aan2o1Y%");
    }

    private void testEncodeDecode(String pass) throws Exception {
        //System.out.println("Testing obfuscation of pass:\n" + pass);
        String encoded = Obfuscator.encode(pass);
        String decoded = Obfuscator.decode(encoded);

        Assert.assertEquals(decoded, pass, "The password doesn't match after decoding it.");
    }
}

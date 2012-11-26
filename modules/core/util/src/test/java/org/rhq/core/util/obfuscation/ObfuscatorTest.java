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
        String encoded = Obfuscator.encode(pass);
        String decoded = Obfuscator.decode(encoded);

        Assert.assertEquals(decoded, pass, "The password doesn't match after decoding it.");
    }
}

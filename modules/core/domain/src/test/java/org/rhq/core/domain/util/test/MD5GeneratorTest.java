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
package org.rhq.core.domain.util.test;

import org.testng.annotations.Test;

import org.rhq.core.domain.util.MD5Generator;

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
        String md5 = MD5Generator.getDigestString("calculate MD5 of this String!\n");
        assert md5.equals("ac98d9c00ea0d821cd757b0f3c628c99") : "Invalid MD5 was " + md5;
    }

    public void testEmpty() {
        MD5Generator md5 = new MD5Generator();
        assert md5.getDigestString().equals("d41d8cd98f00b204e9800998ecf8427e"); // empty data results in this MD5
    }
}
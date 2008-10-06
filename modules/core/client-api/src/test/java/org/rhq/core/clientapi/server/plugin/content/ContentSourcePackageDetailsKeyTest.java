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
package org.rhq.core.clientapi.server.plugin.content;

import org.testng.annotations.Test;

@Test
public class ContentSourcePackageDetailsKeyTest {
    public void testEqualsHashcode() {
        ContentSourcePackageDetailsKey key1;
        ContentSourcePackageDetailsKey key2;

        key1 = new ContentSourcePackageDetailsKey("name1", "ver1", "pt1", "arch1", "rt1", "plugin1");
        key2 = new ContentSourcePackageDetailsKey("name1", "ver1", "pt1", "arch1", "rt1", "plugin1");
        assert key1.equals(key2);
        assert key2.equals(key1);
        assert key1.hashCode() == key2.hashCode();

        key2 = new ContentSourcePackageDetailsKey("BLAH", "ver1", "pt1", "arch1", "rt1", "plugin1");
        assert !key1.equals(key2);
        assert !key2.equals(key1);
        assert key1.hashCode() != key2.hashCode();

        key2 = new ContentSourcePackageDetailsKey("name1", "BLAH", "pt1", "arch1", "rt1", "plugin1");
        assert !key1.equals(key2);
        assert !key2.equals(key1);
        assert key1.hashCode() != key2.hashCode();

        key2 = new ContentSourcePackageDetailsKey("name1", "ver1", "BLAH", "arch1", "rt1", "plugin1");
        assert !key1.equals(key2);
        assert !key2.equals(key1);
        assert key1.hashCode() != key2.hashCode();

        key2 = new ContentSourcePackageDetailsKey("name1", "ver1", "pt1", "BLAH", "rt1", "plugin1");
        assert !key1.equals(key2);
        assert !key2.equals(key1);
        assert key1.hashCode() != key2.hashCode();

        key2 = new ContentSourcePackageDetailsKey("name1", "ver1", "pt1", "arch1", "BLAH", "plugin1");
        assert !key1.equals(key2);
        assert !key2.equals(key1);
        assert key1.hashCode() != key2.hashCode();

        key2 = new ContentSourcePackageDetailsKey("name1", "ver1", "pt1", "arch1", "rt1", "BLAH");
        assert !key1.equals(key2);
        assert !key2.equals(key1);
        assert key1.hashCode() != key2.hashCode();
    }
}
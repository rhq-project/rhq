/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.apache.maven.artifact.versioning;

import org.testng.annotations.Test;

@Test
public class ComparableVersionTest {
    public void testSimpleVersionCompare() {
        ComparableVersion ver1;
        ComparableVersion ver2;

        ver1 = new ComparableVersion("1.0");
        ver2 = new ComparableVersion("1.1");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.0");
        ver2 = new ComparableVersion("2.0");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.0.0");
        ver2 = new ComparableVersion("1.0.1");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;
    }

    public void testSnapshotVersionCompare() {
        ComparableVersion ver1;
        ComparableVersion ver2;

        ver1 = new ComparableVersion("1.0-SNAPSHOT");
        ver2 = new ComparableVersion("1.1-SNAPSHOT");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1-SNAPSHOT");
        ver2 = new ComparableVersion("1.1.0.RC1");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1-SNAPSHOT");
        ver2 = new ComparableVersion("1.1.0.GA");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1-SNAPSHOT");
        ver2 = new ComparableVersion("1.1.0.GA");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1.1-SNAPSHOT");
        ver2 = new ComparableVersion("1.1.1.GA");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1.1-SNAPSHOT");
        ver2 = new ComparableVersion("1.1.0.GA");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) > 0; // ver1 is after ver2!
        assert ver2.compareTo(ver1) < 0; // ver1 is after ver2!
    }

    public void testRCVersionCompare() {
        ComparableVersion ver1;
        ComparableVersion ver2;

        ver1 = new ComparableVersion("1.0.0.RC1");
        ver2 = new ComparableVersion("1.1.0.RC1");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1.0.RC1");
        ver2 = new ComparableVersion("1.1.0.RC2");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1.0.RC1");
        ver2 = new ComparableVersion("1.1.0.RC10");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1.0.RC1");
        ver2 = new ComparableVersion("1.1.0.GA");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) < 0;
        assert ver2.compareTo(ver1) > 0;

        ver1 = new ComparableVersion("1.1.1.RC1");
        ver2 = new ComparableVersion("1.1.0.GA");
        assert !ver1.equals(ver2);
        assert !ver2.equals(ver1);
        assert ver1.compareTo(ver1) == 0;
        assert ver2.compareTo(ver2) == 0;
        assert ver1.compareTo(ver2) > 0; // ver1 is after ver2!
        assert ver2.compareTo(ver1) < 0; // ver1 is after ver2!
    }
}

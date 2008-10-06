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

import junit.framework.TestCase;

public class OSGiVersionComparatorTest extends TestCase {
    public OSGiVersionComparatorTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'net.hyperic.hq.ui.action.resource.common.patch.JBossONVersionComparator.compare(Object, Object)'
     */
    public void testCompare() {
        OSGiVersionComparator comparator = new OSGiVersionComparator();

        assertTrue("1.2 > 1.1", comparator.compare("1.2", "1.1") > 0);
        assertTrue("1.1 < 1.2", comparator.compare("1.1", "1.2") < 0);
        assertTrue("1.0 = 1.0", comparator.compare("1.0", "1.0") == 0);
        assertTrue("9.9 < 9.22", comparator.compare("9.9", "9.22") < 0);
        assertTrue("9.9 < 10.2", comparator.compare("9.9", "10.2") < 0);
        assertTrue("9 < 10", comparator.compare("9", "10") < 0);
        assertTrue("1.2.13.BETA > 1.2.13.ALPHA", comparator.compare("1.2.13.BETA", "1.2.13.ALPHA") > 0);
        assertTrue("4.0.4.GA_CP_2006_06 > 4.0.4.GA", comparator.compare("4.0.4.GA_CP_2006_06", "4.0.4.GA") > 0);
    }

    public void testInvalidComparisons() {
        OSGiVersionComparator comparator = new OSGiVersionComparator();
        try {
            assertTrue("EA > 1.2", comparator.compare("EA", "1.2") > 0);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        ;
        try {
            assertTrue("1.2.alpha < 1.2.beta", comparator.compare("1.2.alpha", "1.2.beta") < 0);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        ;
        try {
            assertTrue("1.2.3.4.5 < 1.2.3.4.6", comparator.compare("1.2.3.4.5", "1.2.3.4.6") < 0);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        ;
    }
}
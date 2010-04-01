/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.core.util;

import org.testng.annotations.Test;

@Test
public class NumberUtilTest {
    public void testAutoIncrementVersion() {
        assertAutoIncrementVersion("1", "2");
        assertAutoIncrementVersion("9", "10");
        assertAutoIncrementVersion("1.0", "1.1");
        assertAutoIncrementVersion("2.0", "2.1");
        assertAutoIncrementVersion("2.5", "2.6");
        assertAutoIncrementVersion("2.5.0", "2.5.1");
        assertAutoIncrementVersion("1.0-1", "1.0-2");
        assertAutoIncrementVersion("1.5-29", "1.5-30");
        assertAutoIncrementVersion("3.5.GA_49", "3.5.GA_50");
        assertAutoIncrementVersion("My Wiki 9 v 1.2.3", "My Wiki 9 v 1.2.4");
    }

    private void assertAutoIncrementVersion(String old, String expectedNew) {
        String actualNew = NumberUtil.autoIncrementVersion(old);
        assert expectedNew.equals(actualNew) : "Incrementing [" + old + "] was not [" + expectedNew
            + "] but instead was [" + actualNew + "]";
    }
}

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.util;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Thomas Ssegismont
 */
public class OSGiVersionTest {

    @DataProvider(name = "testSplit")
    public Object[][] testSplitData() {
        return new Object[][] { { "3.2.1.GA" }, { "...54n;dsq.." }, { ".44" }, { "44." }, { "" }, { "." }, { ".." },
            { "..." }, { "...555" }, { "5555..." }, { "555" }, { "    " }, { " ksjd kqkjksljdkl qsjlkmj" } };
    }

    @Test(dataProvider = "testSplit")
    public void testSplit(String input) {
        assertEquals(OSGiVersion.split(input), input.split("\\."));
    }

}

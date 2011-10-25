/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.resource.group.test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import org.rhq.enterprise.server.resource.group.LDAPStringUtil;

/**
 * Test various methods of the {@link LDAPStringUtil} class.
 * 
 * @author loleary
 * @since 4.0.1
 *
 */
public class LDAPStringUtilTest {

    /**
     * Test method for {@link org.rhq.core.domain.util.LDAPStringUtil#encodeForFilter(java.lang.String)}.
     */
    @Test
    public void testEncodeForFilter() {
        assertEquals(LDAPStringUtil.encodeForFilter("\\, (, ), and *"), "\\5c, \\28, \\29, and \\2a",
            "Failed to encode \"\\, (, ), and *\"");
        assertEquals(LDAPStringUtil.encodeForFilter("A " + '\0' + " empty value"), "A \\00 empty value",
            "Failed to encode \"A <null> empty value\"");
        assertEquals(LDAPStringUtil.encodeForFilter("A " + '\u00E9' + " value"), "A \\c3\\a9 value",
            "Failed to encode \"A <letter e with acute> value\"");
    }

}

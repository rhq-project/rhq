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
package org.rhq.enterprise.communications.command.param;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.Test;

/**
 * Tests Parameter.
 *
 * @author John Mazzitelli
 */
@Test
public class ParameterTest {
    /**
     * Tests constructors.
     *
     * @throws Exception
     */
    public void testParameterConstructors() throws Exception {
        Parameter p1;
        Parameter p2;
        ParameterDefinition def1;
        ParameterDefinition def2;

        p1 = new Parameter(null, null);
        assertNull(p1.getValue());
        assertNull(p1.getDefinition());

        p2 = new Parameter(null, null);
        assertEquals(p1, p2);

        // test copy constructor with all nulls in the parameter to copy
        p2 = new Parameter(p1);
        assertNotSame(p1, p2);
        assertEquals(p1, p2);

        def1 = new ParameterDefinition("one", "java.lang.String", true, true, true, "");
        p1 = new Parameter(def1, null);
        assertFalse(p1.equals(p2));
        assertNotNull(p1.getDefinition());

        def2 = new ParameterDefinition("one", "java.lang.StringBuffer", false, false, false, "desc");
        p2 = new Parameter(def2, null);
        assertEquals(p1.getDefinition(), p2.getDefinition()); // just def names are compared in equals()
        assertTrue(p1.equals(p2)); // the values are compared as well as defs

        def2 = new ParameterDefinition("two", "java.lang.String", true, true, true, "");
        p2 = new Parameter(def2, null);
        assertFalse(p1.getDefinition().equals(p2.getDefinition()));
        assertFalse(p1.equals(p2)); // stupid, we know if the defs are different, the params themselves are not equal

        def1 = new ParameterDefinition("param", "java.lang.String", true, true, true, "");
        def2 = new ParameterDefinition("param", "java.lang.String", true, true, true, "");
        p1 = new Parameter(def1, null);
        p2 = new Parameter(def2, null);
        assertEquals(p1, p2);

        p1 = new Parameter(def1, "hello world!");
        p2 = new Parameter(def2, "hello world!");
        assertEquals(p1, p2);

        p2 = new Parameter(p1);
        assertNotSame(p1, p2);
        assertEquals(p1, p2);
    }

    /**
     * Tests dirty flag.
     */
    public void testIsDirty() {
        Parameter p = new Parameter(null, null);
        assertFalse(p.isDirty());
        p.setValue("boo");
        assertTrue(p.isDirty());
        p.setValue(null);
        assertTrue(p.isDirty()); // even though its back to its original value, its still considered dirty
    }
}
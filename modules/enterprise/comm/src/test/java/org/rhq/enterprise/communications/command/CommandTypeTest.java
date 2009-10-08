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
package org.rhq.enterprise.communications.command;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

/**
 * Tests CommandType.
 *
 * @author John Mazzitelli
 */
@Test
public class CommandTypeTest {
    /**
     * Test toString and how its return value can be passed into constructor.
     */
    public void testToStringAndConstructor() {
        CommandType type;

        // without specifying a version, it defaults to 1
        type = new CommandType("cmd");
        assertEquals(1, type.getVersion());
        assertEquals("cmd", type.toString());
        assertEquals(type, new CommandType(type.toString()));

        type = new CommandType("cmd 111");
        assertEquals(111, type.getVersion());
        assertEquals("cmd v111", type.toString());
        assertEquals(type, new CommandType(type.toString()));

        type = new CommandType("cmd v222");
        assertEquals(222, type.getVersion());
        assertEquals("cmd v222", type.toString());
        assertEquals(type, new CommandType(type.toString()));

        // [mazz] I'm not sure what a version of 0 should denote - seems like it should be something like "use the latest version"
        // but for now, there is no special semantics to a command's version 0 - its just another version like all the others
        type = new CommandType("cmd 0");
        assertEquals(0, type.getVersion());
        assertEquals("cmd v0", type.toString());
        assertEquals(type, new CommandType(type.toString()));
    }

    /**
     * Tests the constructor that takes a single string argument.
     */
    public void testStringConstructor() {
        CommandType type;

        type = new CommandType("cmd");
        assertEquals("cmd", type.getName());
        assertEquals(1, type.getVersion());

        type = new CommandType("cmd 1");
        assertEquals("cmd", type.getName());
        assertEquals(1, type.getVersion());

        type = new CommandType("a.cmd.name   512");
        assertEquals("a.cmd.name", type.getName());
        assertEquals(512, type.getVersion());

        type = new CommandType("cmd v1");
        assertEquals("cmd", type.getName());
        assertEquals(1, type.getVersion());

        type = new CommandType("  a.cmd.name   v512  ");
        assertEquals("a.cmd.name", type.getName());
        assertEquals(512, type.getVersion());

        type = new CommandType("cmd V2");
        assertEquals("cmd", type.getName());
        assertEquals(2, type.getVersion());

        try {
            type = new CommandType("cmd  v");
            fail("constructor must fail - the version number was missing");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            type = new CommandType("cmd 1b");
            fail("constructor must fail - the version number was invalid");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            type = new CommandType("cmd v1 boo");
            fail("constructor must fail - there was extraneous characters in the argument");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            type = new CommandType("cmd v 1");
            fail("constructor must fail - the v prefix must appear adjacent to the version number");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            new CommandType((String) null);
            fail("constructor must not be able to accept a null");
        } catch (IllegalArgumentException ignore) {
        }
    }

    /**
     * testGetName
     */
    public void testGetName() {
        CommandType type = new CommandType("cmd1", 1);
        assertEquals("cmd1", type.getName());
    }

    /**
     * testHashCode
     */
    public void testHashCode() {
        CommandType type1 = new CommandType("cmd1", 1);
        CommandType type1dup = new CommandType("cmd1", 1);
        CommandType type2 = new CommandType("cmd2", 1);
        CommandType type2v2 = new CommandType("cmd2", 2);

        assertTrue(type1.hashCode() == type1dup.hashCode());
        assertTrue(type1.hashCode() != type2.hashCode());
        assertTrue(type2.hashCode() != type2v2.hashCode());
    }

    /**
     * testEqualsObject
     */
    public void testEqualsObject() {
        CommandType type1 = new CommandType("cmd1", 1);
        CommandType type1dup = new CommandType("cmd1", 1);
        CommandType type1v2 = new CommandType("cmd1", 2);
        CommandType type2 = new CommandType("cmd2", 2);

        assertTrue(type1.equals(type1));
        assertTrue(type1.equals(type1dup));
        assertFalse(type1.equals(type1v2));
        assertFalse(type1v2.equals(type1));
        assertFalse(type1.equals(type2));
        assertFalse(type2.equals(type1));
    }

    /**
     * testCompareTo
     */
    public void testCompareTo() {
        CommandType type1 = new CommandType("cmd1", 1);
        CommandType type1dup = new CommandType("cmd1", 1);
        CommandType type1v2 = new CommandType("cmd1", 2);
        CommandType type1v3 = new CommandType("cmd1", 3);
        CommandType type2 = new CommandType("cmd2", 2);

        // must ensure sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for all x and y.
        // (This implies that x.compareTo(y) must throw an exception iff
        // y.compareTo(x) throws an exception.)
        assertTrue(1 == type1v2.compareTo(type1));
        assertTrue(-1 == type1.compareTo(type1v2));

        try {
            type1v2.compareTo(type2);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }

        try {
            type2.compareTo(type1v2);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }

        // must ensure that the relation is transitive:
        // (x.compareTo(y) > 0 && y.compareTo(z) > 0 implies x.compareTo(z) > 0
        assertTrue(1 == type1v3.compareTo(type1v2));
        assertTrue(1 == type1v2.compareTo(type1));
        assertTrue(1 == type1v3.compareTo(type1));

        assertTrue(-1 == type1.compareTo(type1v2));
        assertTrue(-1 == type1v2.compareTo(type1v3));
        assertTrue(-1 == type1.compareTo(type1v3));

        // must ensure that x.compareTo(y)==0 implies that sgn(x.compareTo(z)) == sgn(y.compareTo(z)),
        // for all z.
        assertTrue(0 == type1.compareTo(type1dup));
        assertTrue(type1.compareTo(type1v3) == type1dup.compareTo(type1v3));
        assertTrue(type1.compareTo(type1v2) == type1dup.compareTo(type1v2));

        // It is strongly recommended, but not strictly required that
        // (x.compareTo(y)==0) == (x.equals(y))
        assertTrue(0 == type1.compareTo(type1dup));
        assertTrue(type1.equals(type1dup));
        assertTrue(0 != type1.compareTo(type1v3));
        assertTrue(!type1.equals(type1v3));

        try {
            type1.compareTo(null);
            fail("Should have thrown a ClassCastException");
        } catch (ClassCastException cce) {
        }

        try {
            type1.compareTo(new Integer(1));
            fail("Should have thrown a ClassCastException");
        } catch (ClassCastException cce) {
        }
    }
}
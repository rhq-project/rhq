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
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.command.param.InvalidParameterValueException;
import org.rhq.enterprise.communications.command.param.NoParameterDefinitionsException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;

/**
 * Tests the abstract command functionality command to all commands.
 *
 * @author John Mazzitelli
 */
@Test
public class CommandTest {
    private SimpleTestCommand m_allParamTest;
    private NoParamTestCommand m_noParamTest;
    private ParamsTestCommand m_someParamTest;

    /**
     * Set up tests.
     */
    @BeforeMethod
    protected void setUp() {
        m_allParamTest = new SimpleTestCommand();
        m_noParamTest = new NoParamTestCommand();
        m_someParamTest = new ParamsTestCommand();
    }

    /**
     * Clean up tests.
     */
    @AfterMethod
    protected void tearDown() {
        m_allParamTest = null;
        m_noParamTest = null;
        m_someParamTest = null;
    }

    /**
     * testRemoveParameters
     */
    public void testRemoveParameters() {
        m_allParamTest.setParameterValue("foo", "bar");
        m_allParamTest.setParameterValue("wot", "gorilla?");
        assertTrue(m_allParamTest.getParameterValues().size() == 2);
        m_allParamTest.removeParameterValues();
        assertTrue(m_allParamTest.getParameterValues().size() == 0);

        m_allParamTest.setParameterValue("wot", "gorilla?");
        assertTrue(m_allParamTest.getParameterValues().size() == 1);
        m_allParamTest.removeParameterValues();
        assertTrue(m_allParamTest.getParameterValues().size() == 0);
    }

    /**
     * testGetCommandType
     */
    public void testGetCommandType() {
        assertEquals("test", m_allParamTest.getCommandType().getName());
    }

    /**
     * testIsCommandInResponse, testSetCommandInResponse
     */
    public void testIsCommandInResponse() {
        assertFalse(m_allParamTest.isCommandInResponse()); // default is false
        m_allParamTest.setCommandInResponse(true);
        assertTrue(m_allParamTest.isCommandInResponse());
        m_allParamTest.setCommandInResponse(false);
        assertFalse(m_allParamTest.isCommandInResponse());
    }

    /**
     * testAllowAnyParameter
     */
    public void testAllowAnyParameter() {
        assertTrue(m_allParamTest.allowAnyParameter());
        assertFalse(m_noParamTest.allowAnyParameter());
        assertFalse(m_someParamTest.allowAnyParameter());
    }

    /**
     * testGetParameterDefinition
     */
    public void testGetParameterDefinition() {
        try {
            m_someParamTest.getParameterDefinition(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        } catch (NoParameterDefinitionsException npde) {
            fail("Should have thrown IllegalArgumentException, not " + npde);
        }

        try {
            m_allParamTest.getParameterDefinition("string");
            fail("Should have thrown NoParameterDefinitionsException");
        } catch (NoParameterDefinitionsException npde) {
        }

        try {
            ParameterDefinition def = m_someParamTest.getParameterDefinition("notvalid");
            assertNull(def);

            def = m_someParamTest.getParameterDefinition("int");
            assertEquals("int", def.getName());
            assertEquals(Integer.class.getName(), def.getType());
            assertFalse(def.isRequired());
            assertTrue(def.isNullable());
            assertTrue(def.isValidValue(new Integer(Integer.MAX_VALUE)));
            assertFalse(def.isValidValue("0"));

            def = m_someParamTest.getParameterDefinition("string");
            assertEquals("string", def.getName());
            assertEquals(String.class.getName(), def.getType());
            assertTrue(def.isRequired());
            assertFalse(def.isNullable());
            assertTrue(def.isValidValue("my string"));
            assertFalse(def.isValidValue(new Integer(0)));
        } catch (Exception e) {
            fail("Should not have thrown exception: " + e);
        }
    }

    /**
     * testGetParameterDefinitions
     */
    public void testGetParameterDefinitions() {
        try {
            m_allParamTest.getParameterDefinitions();
            fail("Should have thrown NoParameterDefinitionsException");
        } catch (IllegalArgumentException iae) {
            fail("Should have thrown NoParameterDefinitionsException, not " + iae);
        } catch (NoParameterDefinitionsException npde) {
        }

        try {
            ParameterDefinition[] def = m_someParamTest.getParameterDefinitions();
            assertEquals(4, def.length);
            def = m_noParamTest.getParameterDefinitions();
            assertEquals(0, def.length);
        } catch (Exception e) {
            fail("Should not have thrown exception: " + e);
        }
    }

    /**
     * testGetParameterValue, testSetParameterValue
     */
    public void testGetParameterValue() {
        try {
            m_allParamTest.getParameterValue(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }

        assertFalse(m_allParamTest.hasParameterValue("string"));
        assertNull(m_allParamTest.getParameterValue("string"));

        m_allParamTest.setParameterValue("string", "hello");
        assertTrue(m_allParamTest.hasParameterValue("string"));
        assertEquals("hello", m_allParamTest.getParameterValue("string"));

        m_allParamTest.setParameterValue("string", null);
        assertTrue(m_allParamTest.hasParameterValue("string"));
        assertNull(m_allParamTest.getParameterValue("string"));

        // note that no parameter validation checking is performed
        m_allParamTest.setParameterValue("string", new Integer(1));
        assertEquals(new Integer(1), m_allParamTest.getParameterValue("string"));
    }

    /**
     * testHasParameterValue
     */
    public void testHasParameterValue() {
        assertFalse(m_allParamTest.hasParameterValue("string"));
        assertNull(m_allParamTest.getParameterValue("string"));

        m_allParamTest.setParameterValue("string", "hello");
        assertTrue(m_allParamTest.hasParameterValue("string"));
        assertNotNull(m_allParamTest.getParameterValue("string"));

        m_allParamTest.setParameterValue("string", null);
        assertTrue(m_allParamTest.hasParameterValue("string"));
        assertNull(m_allParamTest.getParameterValue("string"));

        m_allParamTest.removeParameterValue("string");
        assertFalse(m_allParamTest.hasParameterValue("string"));
        assertNull(m_allParamTest.getParameterValue("string"));
    }

    /**
     * testSetParameterValue
     */
    public void testSetParameterValue() {
        try {
            m_allParamTest.setParameterValue(null, "");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }
    }

    /**
     * testRemoveParameterValue
     */
    public void testRemoveParameterValue() {
        try {
            m_allParamTest.removeParameterValue(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }

        m_noParamTest.removeParameterValue("string");
        m_someParamTest.removeParameterValue("string");

        m_someParamTest.setParameterValue("string", "hello");
        assertEquals("hello", m_someParamTest.getParameterValue("string"));
        m_someParamTest.setParameterValue("string", null);
        assertNull(m_someParamTest.getParameterValue("string"));
    }

    /**
     * testCheckParameterValidity
     */
    public void testCheckParameterValidity() {
        // no allowed any params
        m_noParamTest.setParameterValue("string", "hello");

        try {
            m_noParamTest.checkParameterValidity(false);
            fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
        }

        try {
            m_noParamTest.checkParameterValidity(true);
            fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
        }

        // allowed all params
        m_allParamTest.setParameterValue("hello", "world");
        m_allParamTest.checkParameterValidity(false);
        m_allParamTest.checkParameterValidity(true);

        // allowed some params
        m_someParamTest.setParameterValue("string", "hello");
        m_someParamTest.setParameterValue("int", null);
        m_someParamTest.setParameterValue("long", new Long(0L));
        m_someParamTest.setParameterValue("object", null);
        m_someParamTest.checkParameterValidity(false);
        m_someParamTest.checkParameterValidity(true);

        // string is required
        m_someParamTest.removeParameterValue("string");
        try {
            m_someParamTest.checkParameterValidity(false);
            fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
        }

        try {
            m_someParamTest.checkParameterValidity(true);
            fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
        }

        // long is not nullable
        m_someParamTest.setParameterValue("string", "hello");
        m_someParamTest.setParameterValue("long", null);
        try {
            m_someParamTest.checkParameterValidity(false);
            fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
        }

        try {
            m_someParamTest.checkParameterValidity(true);
            fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
        }

        // convert types
        Long max = new Long(Long.MAX_VALUE);
        m_someParamTest.setParameterValue("long", max.toString());
        assertEquals(String.class, m_someParamTest.getParameterValue("long").getClass());
        m_someParamTest.checkParameterValidity(true); // convert the string to long
        assertEquals(Long.class, m_someParamTest.getParameterValue("long").getClass());
        assertEquals(max, m_someParamTest.getParameterValue("long"));
    }

    /**
     * testConvertParameters
     */
    public void testConvertParameters() {
        // not allowed any params
        m_noParamTest.setParameterValue("string", "hello");

        m_noParamTest.convertParameters();
        assertEquals("hello", m_noParamTest.getParameterValue("string"));

        // allowed all params
        m_allParamTest.setParameterValue("hello", "world");
        m_allParamTest.convertParameters();
        assertEquals("world", m_allParamTest.getParameterValue("hello"));

        // allowed some params
        m_someParamTest.setParameterValue("string", new StringBuffer("hello"));
        m_someParamTest.setParameterValue("int", "1");
        m_someParamTest.setParameterValue("long", "0");
        m_someParamTest.setParameterValue("object", new StringBuffer("boo"));
        m_someParamTest.convertParameters();
        assertEquals("hello", m_someParamTest.getParameterValue("string"));
        assertEquals(new Integer(1), m_someParamTest.getParameterValue("int"));
        assertEquals(new Long(0), m_someParamTest.getParameterValue("long"));
        assertEquals(new StringBuffer("boo").toString(), "" + m_someParamTest.getParameterValue("object"));
    }
}
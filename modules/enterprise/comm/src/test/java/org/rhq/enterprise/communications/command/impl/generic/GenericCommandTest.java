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
package org.rhq.enterprise.communications.command.impl.generic;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.param.InvalidParameterValueException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;

/**
 * Tests the ability to modify the custom command's metadata.
 *
 * @author John Mazzitelli
 */
@Test
public class GenericCommandTest {
    private GenericCommand m_cmd;

    /**
     * Set up test.
     */
    @BeforeMethod
    protected void setUp() {
        m_cmd = new GenericCommand();
    }

    /**
     * Clean up test.
     */
    @AfterMethod
    protected void tearDown() {
        m_cmd = null;
    }

    /**
     * Tests the ability to dynamically define metadata for a command.
     *
     * @throws Exception
     */
    public void testDynamicDefinition() throws Exception {
        GenericCommand gc = new GenericCommand(new CommandType("foo", 123), new ParameterDefinition[] {
            new ParameterDefinition("string", "java.lang.String", true, false, false, ""),
            new ParameterDefinition("int", "java.lang.Integer", true, true, false, "") });

        assertEquals(new CommandType("foo", 123), gc.getCommandType());
        assertEquals(2, gc.getParameterDefinitions().length);
        try {
            gc.checkParameterValidity(true);
            fail("should have failed - the parameters are required");
        } catch (InvalidParameterValueException e) {
            // to be expected - the parameters are required
        }

        // put in the parameters and verify the check is OK now
        gc.setParameterValue("string", "hello");
        gc.setParameterValue("int", "987");
        gc.checkParameterValidity(true);
        assertEquals("hello", gc.getParameterValue("string"));
        assertEquals(new Integer(987), gc.getParameterValue("int"));
    }

    /**
     * testSetCommandType
     */
    public void testSetCommandType() {
        CommandType oldType = m_cmd.getCommandType();
        CommandType newType = new CommandType("newtesttype", 123);

        m_cmd.setCommandType(newType);

        assertFalse(oldType.equals(m_cmd.getCommandType()));
        assertEquals("newtesttype", m_cmd.getCommandType().getName());
        assertEquals(123, m_cmd.getCommandType().getVersion());
    }

    /**
     * testSetParameterDefinitions
     */
    public void testSetParameterDefinitions() {
        // asserting this to true means the defs were initially null
        // this means any parameter name/value pair is valid
        assertTrue(m_cmd.allowAnyParameter());

        m_cmd.setParameterValue("string", "hello");
        m_cmd.setParameterValue("int", null);
        m_cmd.checkParameterValidity(false);

        // restrict our command by not allowing any parameters
        m_cmd.setParameterDefinitions(new ParameterDefinition[0]);
        try {
            m_cmd.checkParameterValidity(false);
            fail("Should not have allowed any parameters - InvalidParameterValueException should have been thrown");
        } catch (InvalidParameterValueException e) {
        }

        // restrict our command to only a certain set of parameters
        m_cmd.setParameterDefinitions(new ParameterDefinition[] {
            new ParameterDefinition("string", "java.lang.String", true, false, false, ""),
            new ParameterDefinition("int", "java.lang.Integer", true, true, false, "") });
        m_cmd.checkParameterValidity(false);

        // change nullable field
        m_cmd.setParameterDefinitions(new ParameterDefinition[] {
            new ParameterDefinition("string", "java.lang.String", true, false, false, ""),
            new ParameterDefinition("int", "java.lang.Integer", true, false, false, "") });
        try {
            m_cmd.checkParameterValidity(false);
            fail("Should not have allowed a null int param - InvalidParameterValueException should have been thrown");
        } catch (InvalidParameterValueException e) {
        }

        // add a new required field
        m_cmd.setParameterDefinitions(new ParameterDefinition[] {
            new ParameterDefinition("another", "java.lang.String", true, false, false, ""),
            new ParameterDefinition("string", "java.lang.String", true, false, false, ""),
            new ParameterDefinition("int", "java.lang.Integer", true, true, false, "") });
        try {
            m_cmd.checkParameterValidity(false);
            fail("Should have required 'another' param - InvalidParameterValueException should have been thrown");
        } catch (InvalidParameterValueException e) {
        }

        // change that new required field to optional
        m_cmd.setParameterDefinitions(new ParameterDefinition[] {
            new ParameterDefinition("another", "java.lang.String", false, false, false, ""),
            new ParameterDefinition("string", "java.lang.String", true, false, false, ""),
            new ParameterDefinition("int", "java.lang.Integer", true, true, false, "") });
        m_cmd.checkParameterValidity(false);

        // change back to allowing all params
        m_cmd.setParameterDefinitions(null);
        assertTrue(m_cmd.allowAnyParameter());
        m_cmd.checkParameterValidity(false);
    }
}
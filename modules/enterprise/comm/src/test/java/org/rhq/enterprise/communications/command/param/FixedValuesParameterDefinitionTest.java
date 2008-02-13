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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

/**
 * Tests FixedValuesParameterDefinition.
 *
 * @author John Mazzitelli
 */
@Test
public class FixedValuesParameterDefinitionTest {
    /**
     * Tests to ensure constructor calls make the proper checks for valid inputs.
     */
    public void testFixedValuesBadConstructorCalls() {
        final String INTEGER_TYPE = Integer.class.getName();
        Object[] allowed;
        List<Object> allowedList;

        try {
            allowed = null;
            new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, allowed, "");
            fail("Should not allow null for the fixed, allowed array");
        } catch (IllegalArgumentException iae) {
        }

        try {
            allowed = new Object[0];
            new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, allowed, "");
            fail("Should not allow empty array for the fixed, allowed array");
        } catch (IllegalArgumentException iae) {
        }

        try {
            allowedList = null;
            new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, allowedList, "");
            fail("Should not allow null for the fixed, allowed List");
        } catch (IllegalArgumentException iae) {
        }

        try {
            allowedList = new ArrayList<Object>();
            new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, allowedList, "");
            fail("Should not allow empty List for the fixed, allowed List");
        } catch (IllegalArgumentException iae) {
        }

        try {
            allowed = new Object[] { "a-string-not-integer" };
            new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, allowed, "");
            fail("Should not allow an allowed value that does not match the type of parameter");
        } catch (IllegalArgumentException iae) {
        }

        try {
            allowed = new Object[] { "a-string-not-integer" };
            new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, Arrays.asList(allowed), "");
            fail("Should not allow an allowed value that does not match the type of parameter");
        } catch (IllegalArgumentException iae) {
        }

        try {
            allowed = new Object[] { "hello" };
            new FixedValuesParameterDefinition("foo", Integer[].class.getName(), true, false, false, allowed, "");
            fail("Should not allow array types if using fixed values param defs");
        } catch (IllegalArgumentException iae) {
        }
    }

    /**
     * Tests to ensure values are valid appropriately.
     */
    public void testFixedValuesValidity() {
        final String INTEGER_TYPE = Integer.class.getName();
        final String STRING_TYPE = String.class.getName();
        Object[] allowed;
        FixedValuesParameterDefinition def;

        allowed = new Object[] { new Integer(0) };
        def = new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, allowed, "");
        assertIsValid(def, new Integer(0));
        assertIsNotValid(def, new Integer(1));
        assertIsNotValid(def, "0");
        assertIsValid(def, def.convertObject("0"));

        allowed = new Object[] { new String("hello"), new String("world") };
        def = new FixedValuesParameterDefinition("foo", STRING_TYPE, true, false, false, allowed, "");
        assertIsValid(def, new String("hello"));
        assertIsValid(def, new String("world"));
        assertIsNotValid(def, new String("foo"));

        allowed = new Object[] { new String("1"), new String("2") };
        def = new FixedValuesParameterDefinition("foo", INTEGER_TYPE, true, false, false, allowed, "");
        assertIsValid(def, new Integer(1));
        assertIsValid(def, new Integer(2));
        assertIsNotValid(def, new Integer(0));
        assertIsNotValid(def, new String("1"));
    }

    /**
     * Asserts that the given object is valid using the given definition.
     *
     * @param def           the definition
     * @param objToValidate the object to try to validate
     */
    private void assertIsValid(FixedValuesParameterDefinition def, Object objToValidate) {
        if (!def.isValidValue(objToValidate)) {
            fail("Should have been able to validate - object was not valid.");
        }
    }

    /**
     * Asserts that the given object is not valid using the given definition.
     *
     * @param def           the definition
     * @param objToValidate the object to try to validate (which should not be valid)
     */
    private void assertIsNotValid(FixedValuesParameterDefinition def, Object objToValidate) {
        if (def.isValidValue(objToValidate)) {
            fail("Should not have been able to validate - but object was valid.");
        }
    }
}
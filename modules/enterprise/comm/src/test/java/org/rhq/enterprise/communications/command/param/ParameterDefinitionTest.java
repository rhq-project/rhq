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

import java.util.Collection;
import java.util.NoSuchElementException;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.NoParamTestCommand;
import org.rhq.enterprise.communications.command.ParamsTestCommand;

/**
 * Tests ParameterDefinition and its iterators.
 *
 * @author John Mazzitelli
 */
@Test
public class ParameterDefinitionTest {
    /**
     * Tests the hidden flag setting.
     */
    public void testHidden() {
        ParameterDefinition def;

        def = new ParameterDefinition("foo", String.class.getName(), true, false, false, "");
        assertFalse(def.isHidden());
        def = new ParameterDefinition("foo", String.class.getName(), true, false, true, "");
        assertTrue(def.isHidden());
    }

    /**
     * Tests converting objects to the proper parameter definition type.
     *
     * @throws Exception
     */
    public void testConversion() throws Exception {
        final String STRING_TYPE = String.class.getName();
        final String INTEGER_TYPE = Integer.class.getName();
        final String INTEGER_ARR_TYPE = Integer[].class.getName();
        final String INTEGER_ARR2_TYPE = Integer[][].class.getName();
        final String LONG_ARR3_TYPE = Long[][][].class.getName();

        ParameterDefinition def;

        // trivial cases
        def = new ParameterDefinition("foo", STRING_TYPE, true, false, false, "");
        assertConversionSuccess(def, new String("hello"), new String("hello"));

        def = new ParameterDefinition("foo", INTEGER_TYPE, true, false, false, "");
        assertConversionSuccess(def, new Integer(777), new Integer(777));

        // convert from string to integer
        def = new ParameterDefinition("foo", INTEGER_TYPE, true, false, false, "");
        assertConversionSuccess(def, "123", new Integer(123));

        // can convert a null if def is nullable
        def = new ParameterDefinition("foo", INTEGER_TYPE, true, true, false, "");
        assertNull(def.convertObject(null));
        def = new ParameterDefinition("foo", INTEGER_ARR_TYPE, true, true, false, "");
        assertNull(def.convertObject(null));
        def = new ParameterDefinition("foo", INTEGER_ARR2_TYPE, true, true, false, "");
        assertNull(def.convertObject(null));

        // can NOT convert a null if def is not nullable
        def = new ParameterDefinition("foo", INTEGER_TYPE, true, false, false, "");
        assertConversionFailure(def, null);
        def = new ParameterDefinition("foo", INTEGER_ARR_TYPE, true, false, false, "");
        assertConversionFailure(def, null);
        def = new ParameterDefinition("foo", INTEGER_ARR2_TYPE, true, false, false, "");
        assertConversionFailure(def, null);

        // convert 1-dimension arrays
        def = new ParameterDefinition("foo", INTEGER_ARR_TYPE, true, false, false, "");
        assertArrayConversionSuccess(def, new String[] { "1", "2", "111", "222" }, new Integer[] { new Integer(1),
            new Integer(2), new Integer(111), new Integer(222) });

        // tokenizable string
        def = new ParameterDefinition("foo", INTEGER_ARR_TYPE, true, false, false, "");
        assertArrayConversionSuccess(def, "123,456,789,0", new Integer[] { new Integer(123), new Integer(456),
            new Integer(789), new Integer(0) });

        // tokenizable string with delimiter explicitly defined
        def = new ParameterDefinition("foo", INTEGER_ARR_TYPE, true, false, false, "");
        assertArrayConversionSuccess(def, "#123#456#789#0", new Integer[] { new Integer(123), new Integer(456),
            new Integer(789), new Integer(0) });

        // convert multi-dimension arrays
        def = new ParameterDefinition("foo", INTEGER_ARR2_TYPE, true, false, false, "");
        assertArrayConversionSuccess(def, new String[][] { { "11", "12", "13", "14" }, { "21", "22", "23", "24" } },
            new Integer[][] { { new Integer(11), new Integer(12), new Integer(13), new Integer(14) },
                { new Integer(21), new Integer(22), new Integer(23), new Integer(24) } });

        // note that you can define different delimiters to denote multi-dimensional arrays
        assertArrayConversionSuccess(def, ",|11|12|13|14,|21|22|23|24", new Integer[][] {
            { new Integer(11), new Integer(12), new Integer(13), new Integer(14) },
            { new Integer(21), new Integer(22), new Integer(23), new Integer(24) } });

        def = new ParameterDefinition("foo", LONG_ARR3_TYPE, true, false, false, "");
        assertArrayConversionSuccess(def, new String[][][] { { { "111", "112" }, { "121", "122" }, { "131", "132" } },
            { { "211", "212" }, { "221", "222" }, { "231", "232" } },
            { { "311", "312" }, { "321", "322" }, { "331", "332" } } }, new Long[][][] {
            { { new Long(111), new Long(112) }, { new Long(121), new Long(122) }, { new Long(131), new Long(132) } },
            { { new Long(211), new Long(212) }, { new Long(221), new Long(222) }, { new Long(231), new Long(232) } },
            { { new Long(311), new Long(312) }, { new Long(321), new Long(322) }, { new Long(331), new Long(332) } } });

        assertArrayConversionSuccess(
            def,
            "_#|111|112#|121|122#|131|132_#|211|212#|221|222#|231|232_#|311|312#|321|322#|331|332",
            new Long[][][] {
                { { new Long(111), new Long(112) }, { new Long(121), new Long(122) }, { new Long(131), new Long(132) } },
                { { new Long(211), new Long(212) }, { new Long(221), new Long(222) }, { new Long(231), new Long(232) } },
                { { new Long(311), new Long(312) }, { new Long(321), new Long(322) }, { new Long(331), new Long(332) } } });

        // funky use of delimiters, but just making sure it works; typically you would use the same delimiters for each dimension
        assertArrayConversionSuccess(
            def,
            "^&*111*112&*121*122&*131*132^#!211!212#!221!222#!231!232^%-311-312%-321-322%-331-332",
            new Long[][][] {
                { { new Long(111), new Long(112) }, { new Long(121), new Long(122) }, { new Long(131), new Long(132) } },
                { { new Long(211), new Long(212) }, { new Long(221), new Long(222) }, { new Long(231), new Long(232) } },
                { { new Long(311), new Long(312) }, { new Long(321), new Long(322) }, { new Long(331), new Long(332) } } });
    }

    /**
     * testOptionalIterator
     *
     * @throws Exception
     */
    public void testOptionalIterator() throws Exception {
        OptionalParameterDefinitionIterator iter;
        Command cmd;
        ParameterDefinition[] defs;

        iter = new OptionalParameterDefinitionIterator((Collection<ParameterDefinition>) null);

        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }

        defs = null;
        iter = new OptionalParameterDefinitionIterator(defs);

        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }

        // there are four definitions but only 3 are optional
        cmd = new ParamsTestCommand();
        defs = cmd.getParameterDefinitions();

        iter = new OptionalParameterDefinitionIterator(defs);
        for (int i = 0; i < 3; i++) {
            assertNotNull(iter.next());
            if ((i + 1) < 3) {
                assertTrue(iter.hasNext());
            }
        }

        assertFalse(iter.hasNext());

        // there are no definitions
        cmd = new NoParamTestCommand();
        defs = cmd.getParameterDefinitions();

        iter = new OptionalParameterDefinitionIterator(defs);
        assertFalse(iter.hasNext());
    }

    /**
     * testRequiredIterator
     *
     * @throws Exception
     */
    public void testRequiredIterator() throws Exception {
        RequiredParameterDefinitionIterator iter;
        Command cmd;
        ParameterDefinition[] defs;

        iter = new RequiredParameterDefinitionIterator((Collection<ParameterDefinition>) null);

        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }

        defs = null;
        iter = new RequiredParameterDefinitionIterator(defs);

        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }

        // there are four definitions but only 1 is required
        cmd = new ParamsTestCommand();
        defs = cmd.getParameterDefinitions();

        iter = new RequiredParameterDefinitionIterator(defs);
        assertTrue(iter.hasNext());
        assertNotNull(iter.next());
        assertFalse(iter.hasNext());

        // there are no definitions
        cmd = new NoParamTestCommand();
        defs = cmd.getParameterDefinitions();

        iter = new RequiredParameterDefinitionIterator(defs);
        assertFalse(iter.hasNext());
    }

    /**
     * Recursively tests multi-dimensional arrays for each element being equal.
     *
     * @param arr1 array to test against arr2 (must not be <code>null</code>)
     * @param arr2 array to test against arr1 (must not be <code>null</code>)
     */
    private void assertArrayEquals(Object[] arr1, Object[] arr2) {
        if ((arr1 == null) || (arr2 == null)) {
            fail("arrays to compare must not be null - arr1=[" + arr1 + "] arr2=[" + arr2 + "]");
        }

        if (arr1.length != arr2.length) {
            fail("array lengths do not match - arr1.length=[" + arr1.length + "] arr2.length=[" + arr2.length + "]");
        }

        for (int i = 0; i < arr1.length; i++) {
            if (arr1[i].getClass().isArray()) {
                if (arr2[i].getClass().isArray()) {
                    // recursively call ourselves until we get down to the actual elements to test
                    assertArrayEquals((Object[]) arr1[i], (Object[]) arr2[i]);
                } else {
                    fail("one array does not match the dimensions as the other - arr1=[" + arr1 + "] arr2=[" + arr2
                        + "]");
                }
            } else {
                assertEquals(arr1[i], arr2[i]);
            }
        }

        return;
    }

    /**
     * Asserts that the given object array can be converted using the given definition.
     *
     * @param def            the definition
     * @param arrayToConvert
     * @param arrayConverted
     */
    private void assertArrayConversionSuccess(ParameterDefinition def, Object arrayToConvert, Object[] arrayConverted) {
        try {
            Object[] actualArrayConverted = (Object[]) def.convertObject(arrayToConvert);

            assertArrayEquals(arrayConverted, actualArrayConverted);
        } catch (InvalidParameterValueException ipve) {
            fail("Should have been able to convert: " + ipve);
        }
    }

    /**
     * Asserts that the given object can be converted using the given definition.
     *
     * @param def          the definition
     * @param objToConvert the object to try to convert (should be able to be converted).
     * @param objConverted the object that should be the new converted value
     */
    private void assertConversionSuccess(ParameterDefinition def, Object objToConvert, Object objConverted) {
        try {
            assertEquals(objConverted, def.convertObject(objToConvert));
        } catch (InvalidParameterValueException ipve) {
            fail("Should have been able to convert: " + ipve);
        }
    }

    /**
     * Asserts that the given object cannot be converted using the given definition.
     *
     * @param def the definition
     * @param obj the object to try to convert (but should not be able to be converted).
     */
    private void assertConversionFailure(ParameterDefinition def, Object obj) {
        try {
            def.convertObject(obj);
            fail("Should have thrown an InvalidParameterValueException");
        } catch (InvalidParameterValueException ipve) {
        }
    }
}
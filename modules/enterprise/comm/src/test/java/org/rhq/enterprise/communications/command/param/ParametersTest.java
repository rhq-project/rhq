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
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.util.StreamUtil;

/**
 * Tests the Parameters collection.
 *
 * @author John Mazzitelli
 */
@Test
public class ParametersTest {
    private ParameterDefinition m_def1;
    private ParameterDefinition m_def2;
    private ParameterDefinition m_def3;
    private ParameterDefinition m_def4;

    private Parameter m_param1;
    private Parameter m_param2;
    private Parameter m_param3;
    private Parameter m_param4;

    private Parameters m_params;

    /**
     * Set up some attributes.
     */
    @BeforeMethod
    protected void setUp() {
        m_def1 = new ParameterDefinition("one", String.class.getName(), true, "1 description"); // internal/hidden
        m_def2 = new ParameterDefinition("two", String.class.getName(), true, "2 description"); // internal/hidden
        m_def3 = new ParameterDefinition("three", String.class.getName(), false, "3 description"); // public/not hidden
        m_def4 = new ParameterDefinition("four", String.class.getName(), false, "4 description"); // public/not hidden

        m_param1 = new Parameter(m_def1, "111");
        m_param2 = new Parameter(m_def2, "222");
        m_param3 = new Parameter(m_def3, "333");
        m_param4 = new Parameter(m_def4, "444");

        m_params = new ParametersImpl();
    }

    /**
     * Cleans up test.
     */
    @AfterMethod
    protected void tearDown() {
        m_params = null;
    }

    /**
     * Test the rendering stuff.
     */
    public void testRendering() {
        ParameterRenderingInformation ri = new ParameterRenderingInformation("test-key", "test-desc");
        ParameterDefinition def = new ParameterDefinition("test", String.class.getName(), false, "a description", ri);
        Parameter param = new Parameter(def, "boo");

        assertNotNull(param.getDefinition().getRenderingInfo());

        param.getDefinition().getRenderingInfo().applyResourceBundle(
            ResourceBundle.getBundle(this.getClass().getName()));

        assertEquals("boo", param.getValue());
        assertEquals("title-here", param.getDefinition().getRenderingInfo().getLabel());
        assertEquals("wot gorilla?", param.getDefinition().getRenderingInfo().getDescription());

        // make sure we can serialize a parameter while using ResourceBundle (which itself is not serializable)
        Parameter serialized = ((Parameter) (StreamUtil.deserialize(StreamUtil.serialize(param))));
        assertEquals("boo", serialized.getValue());
        assertEquals("title-here", param.getDefinition().getRenderingInfo().getLabel());
        assertEquals("wot gorilla?", serialized.getDefinition().getRenderingInfo().getDescription());
    }

    /**
     * Tests getting just public or internal parameters.
     */
    public void testPublicInternalParameters() {
        m_params.add(m_param1);
        m_params.add(m_param2);
        m_params.add(m_param3);
        m_params.add(m_param4);

        Parameters internalParams = m_params.getInternalParameters();
        Parameters publicParams = m_params.getPublicParameters();

        assertEquals(2, internalParams.size());
        assertEquals(2, publicParams.size());

        assertTrue(internalParams.contains("one"));
        assertTrue(internalParams.contains(m_param1));
        assertTrue(internalParams.contains("two"));
        assertTrue(internalParams.contains(m_param2));
        assertFalse(internalParams.contains("three"));
        assertFalse(internalParams.contains(m_param3));
        assertFalse(internalParams.contains("four"));
        assertFalse(internalParams.contains(m_param4));

        assertFalse(publicParams.contains("one"));
        assertFalse(publicParams.contains(m_param1));
        assertFalse(publicParams.contains("two"));
        assertFalse(publicParams.contains(m_param2));
        assertTrue(publicParams.contains("three"));
        assertTrue(publicParams.contains(m_param3));
        assertTrue(publicParams.contains("four"));
        assertTrue(publicParams.contains(m_param4));

        Object[] objectArray;
        Object[] retObjectArray;

        objectArray = new Object[2];
        retObjectArray = publicParams.toArray(objectArray);
        assertSame(retObjectArray, objectArray);
        assertEquals(retObjectArray[0], publicParams.getParameter("three"));
        assertEquals(retObjectArray[1], publicParams.getParameter("four"));

        objectArray = new Object[3];
        retObjectArray = publicParams.toArray(objectArray);
        assertSame(retObjectArray, objectArray);
        assertEquals(retObjectArray[0], publicParams.getParameter("three"));
        assertEquals(retObjectArray[1], publicParams.getParameter("four"));
        assertNull(retObjectArray[2]);

        objectArray = new Object[50];
        retObjectArray = publicParams.toArray(objectArray);
        assertSame(retObjectArray, objectArray);
        assertEquals(retObjectArray[0], publicParams.getParameter("three"));
        assertEquals(retObjectArray[1], publicParams.getParameter("four"));
        assertNull(retObjectArray[2]);

        objectArray = new Object[1];
        retObjectArray = publicParams.toArray(objectArray);
        assertNotSame(retObjectArray, objectArray);
        assertEquals(2, retObjectArray.length);
        assertEquals(retObjectArray[0], publicParams.getParameter("three"));
        assertEquals(retObjectArray[1], publicParams.getParameter("four"));

        assertNotNull(internalParams.getParameter("one"));
        assertNotNull(internalParams.getParameter("two"));
        assertNull(internalParams.getParameter("three"));
        assertNull(internalParams.getParameter("four"));

        assertNull(publicParams.getParameter("one"));
        assertNull(publicParams.getParameter("two"));
        assertNotNull(publicParams.getParameter("three"));
        assertNotNull(publicParams.getParameter("four"));

        int iterations = 0;
        for (Iterator iter = publicParams.iterator(); iter.hasNext(); iterations++) {
            Parameter param = (Parameter) iter.next();
            assertNotNull(param);
            assertFalse(param.getDefinition().isHidden());
        }

        assertEquals(2, iterations);

        iterations = 0;
        for (Iterator iter = internalParams.iterator(); iter.hasNext(); iterations++) {
            Parameter param = (Parameter) iter.next();
            assertNotNull(param);
            assertTrue(param.getDefinition().isHidden());
        }

        assertEquals(2, iterations);

        // our parameters impl returns a reference to the actual collection, not a copy
        // so we want to test to make sure that's true
        assertSame(internalParams.getParameter("one"), m_params.getParameter("one"));
        assertSame(internalParams.getParameter("two"), m_params.getParameter("two"));
        assertSame(publicParams.getParameter("three"), m_params.getParameter("three"));
        assertSame(publicParams.getParameter("four"), m_params.getParameter("four"));

        assertSame(internalParams.getParameterValue("one"), m_params.getParameterValue("one"));
        assertSame(internalParams.getParameterValue("two"), m_params.getParameterValue("two"));
        assertSame(publicParams.getParameterValue("three"), m_params.getParameterValue("three"));
        assertSame(publicParams.getParameterValue("four"), m_params.getParameterValue("four"));

        String oneValue = "changedOne";
        Parameter one = internalParams.getParameter("one");
        one.setValue(oneValue);
        assertSame(one, internalParams.getParameter("one"));
        assertSame(oneValue, internalParams.getParameterValue("one"));
        assertSame(internalParams.getParameter("one"), m_params.getParameter("one"));
        assertSame(internalParams.getParameterValue("one"), m_params.getParameterValue("one"));

        String threeValue = "changedThree";
        Parameter three = publicParams.getParameter("three");
        three.setValue(threeValue);
        assertSame(three, publicParams.getParameter("three"));
        assertSame(threeValue, publicParams.getParameterValue("three"));
        assertSame(publicParams.getParameter("three"), m_params.getParameter("three"));
        assertSame(publicParams.getParameterValue("three"), m_params.getParameterValue("three"));

        ParameterDefinition def5;
        Parameter param5;
        String fiveValue = "555";
        def5 = new ParameterDefinition("newFive", String.class.getName(), true, "5 description"); // internal/hidden
        param5 = new Parameter(def5, fiveValue);
        assertNull(internalParams.getParameter("newFive"));
        internalParams.add(param5);
        assertSame(param5, internalParams.getParameter("newFive"));
        assertSame(fiveValue, internalParams.getParameterValue("newFive"));
        assertSame(internalParams.getParameter("newFive"), m_params.getParameter("newFive"));
        assertSame(internalParams.getParameterValue("newFive"), m_params.getParameterValue("newFive"));
        internalParams.remove(param5);
        assertNull(internalParams.getParameter("newFive"));
        assertNull(m_params.getParameter("newFive"));

        try {
            internalParams.add(m_param3);
            fail("Should not have been able to add a non-hidden parameter to the internal only parameter collection");
        } catch (IllegalArgumentException ok) {
        }

        ParameterDefinition def6;
        Parameter param6;
        String sixValue = "600";
        def6 = new ParameterDefinition("newSix", String.class.getName(), false, "6 description"); // public/not hidden
        param6 = new Parameter(def6, sixValue);
        assertNull(publicParams.getParameter("newSix"));
        publicParams.add(param6);
        assertSame(param6, publicParams.getParameter("newSix"));
        assertSame(sixValue, publicParams.getParameterValue("newSix"));
        assertSame(publicParams.getParameter("newSix"), m_params.getParameter("newSix"));
        assertSame(publicParams.getParameterValue("newSix"), m_params.getParameterValue("newSix"));
        publicParams.remove(param6);
        assertNull(publicParams.getParameter("newSix"));
        assertNull(m_params.getParameter("newSix"));

        try {
            publicParams.add(m_param1);
            fail("Should not have been able to add a hidden parameter to the public, non-hidden only parameter collection");
        } catch (IllegalArgumentException ok) {
        }

        // test the copy constructor, removeAll, isEmpty, size, allAll
        ParametersImpl copy = new ParametersImpl(internalParams);

        assertEquals(4, m_params.size());
        assertEquals(2, internalParams.size());
        assertFalse(internalParams.isEmpty());
        assertTrue(internalParams.removeAll(Arrays.asList(internalParams.toArray())));
        assertTrue(internalParams.isEmpty());
        assertEquals(2, m_params.size());
        assertEquals(0, internalParams.size());

        assertTrue(internalParams.addAll(copy));
        assertEquals(4, m_params.size());
        assertEquals(2, internalParams.size());

        try {
            publicParams.addAll(copy);
            fail("Should not have been able to add all the internal parameters to the public params collection");
        } catch (IllegalArgumentException ok) {
        }
    }

    /**
     * Tests to make sure the order you put the items in are the same order they come out.
     */
    public void testSorting() {
        m_params.add(m_param1);
        m_params.add(m_param2);
        m_params.add(m_param3);

        Parameter[] array;

        array = m_params.toArray(new Parameter[m_params.size()]);
        assertEquals(3, array.length);
        assertEquals(m_param1, array[0]);
        assertEquals(m_param2, array[1]);
        assertEquals(m_param3, array[2]);

        array = (Parameter[]) m_params.toArray();
        assertEquals(3, array.length);
        assertEquals(m_param1, array[0]);
        assertEquals(m_param2, array[1]);
        assertEquals(m_param3, array[2]);

        // remove the middle one and make sure the order is still as expected
        m_params.remove(m_param2);
        array = (Parameter[]) m_params.toArray();
        assertEquals(2, array.length);
        assertEquals(m_param1, array[0]);
        assertEquals(m_param3, array[1]);

        // add the one back in (this time it should go on the end) and make sure iterator is OK
        m_params.add(m_param2);
        int i = 0;
        String[] expectedNames = new String[] { m_param1.getDefinition().getName(), m_param3.getDefinition().getName(),
            m_param2.getDefinition().getName() };

        for (Iterator iter = m_params.iterator(); iter.hasNext(); i++) {
            Parameter attrib = (Parameter) iter.next();
            assertEquals(expectedNames[i], attrib.getDefinition().getName());
        }
    }

    /**
     * Tests the non-Collection APIs of get/set.
     *
     * @throws Exception
     */
    public void testGetSet() throws Exception {
        m_params.add(m_param1);
        assertEquals(m_param1, m_params.getParameter(m_param1.getDefinition().getName()));
        assertEquals(m_param1.getDefinition(), m_params.getParameterDefinition(m_param1.getDefinition().getName()));
        assertEquals(m_param1.getValue(), m_params.getParameterValue(m_param1.getDefinition().getName()));

        m_params.setParameterValue(m_param1.getDefinition().getName(), "boo");
        assertEquals("boo", m_params.getParameterValue(m_param1.getDefinition().getName()));
        assertSame(m_param1.getValue(), m_params.getParameterValue(m_param1.getDefinition().getName()));
        assertSame(m_param1.getDefinition(), m_params.getParameterDefinition(m_param1.getDefinition().getName()));
        assertSame(m_param1, m_params.getParameter(m_param1.getDefinition().getName()));

        // see that the copy-constructor really does copy the attribute wrapper itself (not the value or definition)
        Parameters dup = new ParametersImpl(m_params);
        assertEquals("boo", dup.getParameterValue(m_param1.getDefinition().getName()));
        assertSame(m_param1.getValue(), dup.getParameterValue(m_param1.getDefinition().getName()));
        assertSame(m_param1.getDefinition(), dup.getParameterDefinition(m_param1.getDefinition().getName()));
        assertNotSame(m_param1, dup.getParameter(m_param1.getDefinition().getName()));

        // make sure changing in one doesn't change in the other
        m_params.setParameterValue(m_param1.getDefinition().getName(), "NOTboo");
        assertEquals("boo", dup.getParameterValue(m_param1.getDefinition().getName()));
        assertEquals("NOTboo", m_params.getParameterValue(m_param1.getDefinition().getName()));
        dup.setParameterValue(m_param1.getDefinition().getName(), "boo-two");
        assertEquals("boo-two", dup.getParameterValue(m_param1.getDefinition().getName()));
        assertEquals("NOTboo", m_params.getParameterValue(m_param1.getDefinition().getName()));

        m_params.setParameterValue(m_param1.getDefinition().getName(), null);
        assertNull(m_params.getParameterValue(m_param1.getDefinition().getName()));

        assertNull(m_params.getParameter("doesNotExist"));
        assertNull(m_params.getParameter(null));

        try {
            m_params.getParameterDefinition("doesNotExist");
            fail("should have thrown NoSuchElementException");
        } catch (InvalidParameterDefinitionException e) {
        }

        try {
            m_params.getParameterDefinition(null);
            fail("should have thrown NoSuchElementException");
        } catch (InvalidParameterDefinitionException e) {
        }

        try {
            m_params.getParameterValue("doesNotExist");
            fail("should have thrown NoSuchElementException");
        } catch (InvalidParameterDefinitionException e) {
        }

        try {
            m_params.setParameterValue("doesNotExist", "boo");
            fail("should have thrown NoSuchElementException");
        } catch (InvalidParameterDefinitionException e) {
        }
    }

    /**
     * Test method for 'Parameters.Parameters(Parameters)'
     */
    public void testParametersParameters() {
        m_params.add(m_param1);

        Parameters col2 = new ParametersImpl(m_params);

        assertEquals(m_params.size(), col2.size());
        assertEquals(m_params, col2);

        // quick test of equals and hash code
        assertFalse(m_params.equals("not a collection"));
        assertFalse(m_params.equals(null));
        assertTrue(m_params.hashCode() == col2.hashCode());
    }

    /**
     * Test method for 'Parameters.size()'
     */
    public void testSize() {
        assertEquals(0, m_params.size());
        m_params.add(m_param1);
        assertEquals(1, m_params.size());
        m_params.add(m_param2);
        assertEquals(2, m_params.size());
        m_params.add(m_param1); // dup
        assertEquals(2, m_params.size());
    }

    /**
     * Test method for 'Parameters.clear()'
     */
    public void testClear() {
        m_params.add(m_param1);
        m_params.add(m_param2);
        assertEquals(2, m_params.size());
        m_params.clear();
        assertEquals(0, m_params.size());
        m_params.clear();
        assertEquals(0, m_params.size());
    }

    /**
     * Test method for 'Parameters.isEmpty()'
     */
    public void testIsEmpty() {
        assertTrue(m_params.isEmpty());
        m_params.add(m_param1);
        assertFalse(m_params.isEmpty());
        m_params.clear();
        assertTrue(m_params.isEmpty());
    }

    /**
     * Test method for 'Parameters.add(Object)'
     */
    public void testAddObject() {
        Parameter p = m_param1;

        assertTrue(m_params.add(p));
        assertEquals(1, m_params.size());
        assertTrue(m_params.add(m_param2));
        assertEquals(2, m_params.size());

        try {
            m_params.add(null);
            fail("should have failed with a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test method for 'Parameters.contains(Object)'
     */
    public void testContainsObject() {
        Parameter p = m_param1;

        assertTrue(m_params.add(p));
        assertTrue(m_params.add(m_param2));
        assertTrue(m_params.contains(p));
        assertTrue(m_params.contains(m_param2));
        assertFalse(m_params.contains(m_param3));
        assertFalse(m_params.contains(m_param3));

        assertTrue(m_params.contains(m_param1.getDefinition().getName()));
        assertTrue(m_params.contains(m_param2.getDefinition().getName()));
        assertFalse(m_params.contains(m_param3.getDefinition().getName()));
        assertFalse(m_params.contains(m_param4.getDefinition().getName()));

        try {
            m_params.contains(new StringBuffer("not a valid action attribute"));
            fail("should have failed with a ClassCastException");
        } catch (ClassCastException e) {
        }

        try {
            m_params.contains((Object) null);
            fail("should have failed with a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test method for 'Parameters.remove(Object)'
     */
    public void testRemoveObject() {
        Parameter p = m_param1;

        assertTrue(m_params.add(p));
        assertTrue(m_params.add(m_param2));
        assertTrue(m_params.add(m_param3));
        assertEquals(3, m_params.size());

        assertTrue(m_params.contains(m_param1));
        assertTrue(m_params.remove(p));
        assertFalse(m_params.contains(m_param1));
        assertFalse(m_params.remove(p));

        assertEquals(2, m_params.size());
        assertTrue(m_params.remove(m_param2));
        assertEquals(1, m_params.size());

        assertTrue(m_params.remove(m_param3.getDefinition().getName()));
        assertFalse(m_params.remove(m_param3.getDefinition().getName()));
        assertEquals(0, m_params.size());

        try {
            m_params.remove(new StringBuffer("not a valid action attribute"));
            fail("should have failed with a ClassCastException");
        } catch (ClassCastException e) {
        }

        try {
            m_params.remove((Object) null);
            fail("should have failed with a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test method for 'Parameters.addAll(Collection)'
     */
    public void testAddAll() {
        List<Parameter> list = new ArrayList<Parameter>();
        list.add(m_param1);
        list.add(m_param2);
        list.add(m_param3);

        assertEquals(0, m_params.size());
        assertTrue(m_params.addAll(list));
        assertEquals(list.size(), m_params.size());

        try {
            m_params.addAll(null);
            fail("should have failed with a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test method for 'Parameters.containsAll(Collection)'
     */
    public void testContainsAll() {
        List<Parameter> list = new ArrayList<Parameter>();
        list.add(m_param1);
        list.add(m_param2);
        list.add(m_param3);

        assertEquals(0, m_params.size());
        assertTrue(m_params.addAll(list));
        assertEquals(list.size(), m_params.size());

        assertTrue(m_params.containsAll(list));
        list.add(m_param4);
        assertFalse(m_params.containsAll(list));

        try {
            m_params.containsAll(null);
            fail("should have failed with a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test method for 'Parameters.removeAll(Collection)'
     */
    public void testRemoveAll() {
        m_params.add(m_param1);
        m_params.add(m_param2);
        m_params.add(m_param3);
        m_params.add(m_param4);

        List<Parameter> list = new ArrayList<Parameter>();
        list.add(m_param1);
        list.add(m_param2);

        assertEquals(4, m_params.size());
        assertTrue(m_params.removeAll(list));
        assertEquals(2, m_params.size());

        assertFalse(m_params.contains(m_param1));
        assertFalse(m_params.contains(m_param2));
        assertTrue(m_params.contains(m_param3));
        assertTrue(m_params.contains(m_param4));

        assertFalse(m_params.removeAll(list));

        try {
            m_params.removeAll(null);
            fail("should have failed with a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test method for 'Parameters.retainAll(Collection)'
     */
    public void testRetainAll() {
        m_params.add(m_param1);
        m_params.add(m_param2);
        m_params.add(m_param3);
        m_params.add(m_param4);

        List<Parameter> list = new ArrayList<Parameter>();
        list.add(m_param1);
        list.add(m_param2);

        assertEquals(4, m_params.size());
        assertTrue(m_params.retainAll(list));
        assertEquals(2, m_params.size());

        assertTrue(m_params.contains(m_param1));
        assertTrue(m_params.contains(m_param2));
        assertFalse(m_params.contains(m_param3));
        assertFalse(m_params.contains(m_param4));

        try {
            m_params.retainAll(null);
            fail("should have failed with a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test method for 'Parameters.iterator()'
     */
    public void testIterator() {
        m_params.add(m_param1);

        for (Iterator iter = m_params.iterator(); iter.hasNext();) {
            Parameter attr = (Parameter) iter.next();
            assertEquals(m_param1, attr);
            assertEquals(m_param1.getValue(), attr.getValue());
        }
    }

    /**
     * Test method for 'Parameters.toArray(Object[])'
     */
    public void testToArrayObjectArray() {
        m_params.add(m_param1);
        m_params.add(m_param2);
        Parameter[] array = m_params.toArray(new Parameter[m_params.size()]);
        assertEquals(2, array.length);

        array = (Parameter[]) m_params.toArray();
        assertEquals(2, array.length);
    }
}
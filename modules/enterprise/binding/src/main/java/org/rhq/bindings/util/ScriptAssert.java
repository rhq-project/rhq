/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.bindings.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.script.ScriptEngine;

/**
 * This class provides the assertion functions to the scripts.
 * It is heavily inspired by and in part copied from org.testng.Assert class.
 * <p>
 * We specifically do not use the TestNG class here so that we avoid a runtime
 * dependency on TestNG.
 */
public class ScriptAssert {

    private ScriptEngine scriptEngine;

    //borrowed from TestNG
    private static String format(Object actual, Object expected, String message) {
        String formatted = "";
        if (null != message) {
            formatted = message + " ";
        }

        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }

    private void failNotEquals(Object actual, Object expected, String message) {
        fail(format(actual, expected, message));
    }
    
    private void failAssertNoEqual(Object[] actual, Object[] expected, String message, String defaultMessage) {
        if (message != null) {
            fail(message);
        } else {
            fail(defaultMessage);
        }
    }
    
    public ScriptAssert() {
        
    }
    
    public ScriptAssert(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @NoTopLevelIndirection
    public void init(ScriptEngine engine) {
        this.scriptEngine = engine;
    }
    
    public void assertTrue(boolean condition, String msg) {
        if (!condition) {
            failNotEquals(Boolean.valueOf(condition), Boolean.TRUE, msg);
        }
    }

    public void assertTrue(boolean condition) {
        assertTrue(condition, null);
    }

    public void assertFalse(boolean condition, String msg) {
        if (condition) {
            failNotEquals(Boolean.valueOf(condition), Boolean.FALSE, msg);
        }
    }

    public void assertFalse(boolean condition) {
        assertFalse(condition, null);
    }

    public void fail(String msg, Throwable throwable) {
        throw new ScriptAssertionException(msg, throwable);
    }

    public void fail(String msg) {
        throw new ScriptAssertionException(msg);
    }

    public void fail() {
        throw new ScriptAssertionException();
    }

    public void assertEquals(Object actual, Object expected, String msg) {
        if ((expected == null) && (actual == null)) {
            return;
        }
        if (expected != null) {
            if (expected.getClass().isArray()) {
                assertArrayEquals(actual, expected, msg);
                return;
            } else if (expected.equals(actual)) {
                return;
            }
        }
        failNotEquals(actual, expected, msg);
    }
    
    /**
     * <b>COPIED FROM TESTNG</b>
     * Asserts that two objects are equal. It they are not, an AssertionError,
     * with given message, is thrown.
     * @param actual the actual value
     * @param expected the expected value (should be an non-null array value)
     * @param message the assertion error message
     */
    private void assertArrayEquals(Object actual, Object expected, String message) {
        //is called only when expected is an array
        if (actual.getClass().isArray()) {
            int expectedLength = Array.getLength(expected);
            if (expectedLength == Array.getLength(actual)) {
                for (int i = 0; i < expectedLength; i++) {
                    Object _actual = Array.get(actual, i);
                    Object _expected = Array.get(expected, i);
                    try {
                        assertEquals(_actual, _expected);
                    } catch (AssertionError ae) {
                        failNotEquals(actual, expected, message == null ? "" : message + " (values as index " + i
                            + " are not the same)");
                    }
                }
                //array values matched
                return;
            } else {
                failNotEquals(Array.getLength(actual), expectedLength, message == null ? "" : message
                    + " (Array lengths are not the same)");
            }
        }
        failNotEquals(actual, expected, message);
    }    

    public void assertEquals(Object actual, Object expected) {
        assertEquals(actual, expected, null);
    }

    public void assertNotNull(Object object) {
        assertNotNull(object, null);
    }

    public void assertNotNull(Object object, String msg) {
        if (object == null) {
            String message = "";
            if (msg != null) {
                message += msg + " ";
            }
            
            message = "expected the object to not be null";
            fail(message);
        }
    }

    public void assertNull(Object object) {
        assertNull(object, null);
    }

    public void assertNull(Object object, String msg) {
        if (object != null) {
            failNotEquals(object, null, msg);
        }
    }

    public void assertSame(Object actual, Object expected, String msg) {
        if (actual != expected) {
            failNotEquals(actual, expected, msg);
        }
    }

    public void assertSame(Object actual, Object expected) {
        assertSame(actual, expected, null);
    }

    public void assertNotSame(Object actual, Object expected, String msg) {
        if (actual == expected) {
            String formatted = "";
            if (msg != null) {
                formatted = msg + " ";
            }
            fail(formatted + "expected not same with:<" + expected + "> but was same:<" + actual + ">");
        }
    }

    public void assertNotSame(Object actual, Object expected) {
        assertNotSame(actual, expected, null);
    }

    public void assertEquals(Collection<?> actual, Collection<?> expected) {
        assertEquals(actual, expected, null);
    }

    public void assertEquals(Collection<?> actual, Collection<?> expected, String msg) {
        if(actual == expected) {
            return;
          }

          if (actual == null || expected == null) {
            if (msg != null) {
              fail(msg);
            } else {
              fail("Collections not equal: expected: " + expected + " and actual: " + actual);
            }
          }

          assertEquals(actual.size(), expected.size(), msg + ": lists don't have the same size");

          Iterator<?> actIt = actual.iterator();
          Iterator<?> expIt = expected.iterator();
          int i = -1;
          while(actIt.hasNext() && expIt.hasNext()) {
            i++;
            Object e = expIt.next();
            Object a = actIt.next();
            String explanation = "Lists differ at element [" + i + "]: " + e + " != " + a;
            String errorMessage = msg == null ? explanation : msg + ": " + explanation;

            assertEquals(a, e, errorMessage);
          }
    }

    /* These are no longer needed in Rhino 1.7R3, because native javascript arrays implement
     * the collection interfaces.
     */
    /*    
        public void assertEquals(Object[] actual, Object[] expected, String msg) {
            if (actual == expected) {
                return;
            }

            if ((actual == null && expected != null) || (actual != null && expected == null)) {
                if (msg != null) {
                    fail(msg);
                } else {
                    fail("Arrays not equal: " + Arrays.toString(expected) + " and " + Arrays.toString(actual));
                }
            }
            assertEquals(Arrays.asList(actual), Arrays.asList(expected), msg);
        }

        public void assertEquals(Object[] actual, Object[] expected) {
            assertEquals(actual, expected, null);
        }

    */

    public void assertEqualsNoOrder(Object[] actual, Object[] expected, String msg) {
        if (actual == expected) {
            return;
        }

        if ((actual == null && expected != null) || (actual != null && expected == null)) {
            failAssertNoEqual(actual, expected,
                "Arrays not equal: " + Arrays.toString(expected) + " and " + Arrays.toString(actual), msg);
        }

        if (actual.length != expected.length) {
            failAssertNoEqual(actual, expected, "Arrays do not have the same size:" + actual.length + " != "
                + expected.length, msg);
        }

        List<Object> actualCollection = new ArrayList<Object>();
        for (Object a : actual) {
            actualCollection.add(a);
        }
        for (Object o : expected) {
            actualCollection.remove(o);
        }
        if (actualCollection.size() != 0) {
            failAssertNoEqual(actual, expected,
                "Arrays not equal: " + Arrays.toString(expected) + " and " + Arrays.toString(actual), msg);
        }
    }

    public void assertEqualsNoOrder(Object[] actual, Object[] expected) {
        assertEqualsNoOrder(actual, expected, null);
    }

    public void assertExists(String identifier) {
        Object value = scriptEngine.get(identifier);
        assertNotNull(value, identifier + " is not defined");
    }

    /**
     * JavaScript has only a single numeric type such that <code>x = 1</code> and <code>y = 1.0</code> are considered to
     * be of the same type. From within a (JavaScript) script if you were to call <code>assertEquals(x, y)</code>, you
     * would get an exception that looks something like,
     *
     * <pre>
     * Caused by: javax.script.ScriptException: sun.org.mozilla.javascript.internal.EvaluatorException: The choice of
     * Java constructor assertEquals matching JavaScript argument types (number,number,string) is ambiguous;
     * candidate constructors are:
     * void assertEquals(java.lang.String,java.lang.String,java.lang.String)
     * void assertEquals(double,double,double)
     * void assertEquals(float,float,float)
     * void assertEquals(long,long,java.lang.String)
     * void assertEquals(byte,byte,java.lang.String)
     * void assertEquals(char,char,java.lang.String)
     * void assertEquals(short,short,java.lang.String)
     * void assertEquals(int,int,java.lang.String) (<Unknown source>#1) in <Unknown source> at line number 1
     </pre>
     *
     * To avoid the ambiguity when comparing numbers in JavaScript scripts, it is recommended to use this method.
     *
     * @param actual The actual value
     * @param expected The expected value
     * @param msg A useful, meaningful error message
     * 
     * @deprecated - it is now possible to use assertEquals() with numbers from within javascript because we
     * now only provide {@link #assertEquals(Object, Object)} to which the numbers convert to correctly.
     */
    @Deprecated
    public void assertNumberEqualsJS(double actual, double expected, String msg) {
        assertEquals(actual, expected, msg);
    }
}

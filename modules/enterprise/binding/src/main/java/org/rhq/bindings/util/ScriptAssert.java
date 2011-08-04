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

import java.util.Collection;

import javax.script.ScriptEngine;

import org.testng.Assert;

public class ScriptAssert {

    private ScriptEngine scriptEngine;

    public ScriptAssert() {
        
    }
    
    public ScriptAssert(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void init(ScriptEngine engine) {
        this.scriptEngine = engine;
    }
    
    public void assertTrue(boolean condition, String msg) {
        try {
            Assert.assertTrue(condition, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertTrue(boolean condition) {
        try {
            Assert.assertTrue(condition);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertFalse(boolean condition, String msg) {
        try {
            Assert.assertFalse(condition, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertFalse(boolean condition) {
        try {
            Assert.assertFalse(condition);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void fail(String msg, Throwable throwable) {
        try {
            Assert.fail(msg, throwable);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void fail(String msg) {
        try {
            Assert.fail(msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void fail() {
        throw new ScriptAssertionException(new AssertionError());
    }

    public void assertEquals(Object actual, Object expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(Object actual, Object expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(String actual, String expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(String actual, String expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(double actual, double expected, double delta, String msg) {
        try {
            Assert.assertEquals(actual, expected, delta, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(double actual, double expected, double delta) {
        try {
            Assert.assertEquals(actual, expected, delta);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(float actual, float expected, float delta, String msg) {
        try {
            Assert.assertEquals(actual, expected, delta, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(float actual, float expected, float delta) {
        try {
            Assert.assertEquals(actual, expected, delta);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(long actual, long expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(long actual, long expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(boolean actual, boolean expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(boolean actual, boolean expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(byte actual, byte expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(byte actual, byte expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(char actual, char expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(char actual, char expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(short actual, short expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch(AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(short actual, short expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(int actual, int expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(int actual, int expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertNotNull(Object object) {
        try {
            Assert.assertNotNull(object);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertNotNull(Object object, String msg) {
        try {
            Assert.assertNotNull(object, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertNull(Object object) {
        try {
            Assert.assertNull(object);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertNull(Object object, String msg) {
        try {
            Assert.assertNull(object, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertSame(Object actual, Object expected, String msg) {
        try {
            Assert.assertSame(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertSame(Object actual, Object expected) {
        try {
            Assert.assertSame(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertNotSame(Object actual, Object expected, String msg) {
        try {
            Assert.assertNotSame(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertNotSame(Object actual, Object expected) {
        try {
            Assert.assertNotSame(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(Collection actual, Collection expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(Collection actual, Collection expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(Object[] actual, Object[] expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEqualsNoOrder(Object[] actual, Object[] expected, String msg) {
        try {
            Assert.assertEqualsNoOrder(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(Object[] actual, Object[] expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEqualsNoOrder(Object[] actual, Object[] expected) {
        try {
            Assert.assertEqualsNoOrder(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(byte[] actual, byte[] expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertEquals(byte[] actual, byte[] expected, String msg) {
        try {
            Assert.assertEquals(actual, expected, msg);
        } catch (AssertionError e) {
            throw new ScriptAssertionException(e);
        }
    }

    public void assertExists(String identifier) {
        assertNotNull(scriptEngine.get(identifier), identifier + " is not defined");
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
     */
    public void assertNumberEqualsJS(double actual, double expected, String msg) {
        assertEquals(actual, expected, msg);
    }

}

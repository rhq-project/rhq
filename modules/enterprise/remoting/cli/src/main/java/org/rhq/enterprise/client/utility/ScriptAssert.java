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

package org.rhq.enterprise.client.utility;

import org.testng.Assert;

import javax.script.ScriptEngine;
import java.util.Collection;

public class ScriptAssert {

    private ScriptEngine scriptEngine;

    public ScriptAssert(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void assertTrue(boolean condition, String msg) {
        Assert.assertTrue(condition, msg);
    }

    public void assertTrue(boolean condition) {
        Assert.assertTrue(condition);
    }

    public void assertFalse(boolean condition, String msg) {
        Assert.assertFalse(condition, msg);
    }

    public void assertFalse(boolean condition) {
        Assert.assertFalse(condition);
    }

    public void fail(String msg, Throwable throwable) {
        Assert.fail(msg, throwable);
    }

    public void fail(String msg) {
        Assert.fail(msg);
    }

    public void fail() {
        Assert.fail();
    }

    public void assertEquals(Object actual, Object expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(Object actual, Object expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(String actual, String expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(String actual, String expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(double actual, double expected, double delta, String msg) {
        Assert.assertEquals(actual, expected, delta, msg);
    }

    public void assertEquals(double actual, double expected, double delta) {
        Assert.assertEquals(actual, expected, delta);
    }

    public void assertEquals(float actual, float expected, float delta, String msg) {
        Assert.assertEquals(actual, expected, delta, msg);
    }

    public void assertEquals(float actual, float expected, float delta) {
        Assert.assertEquals(actual, expected, delta);
    }

    public void assertEquals(long actual, long expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(long actual, long expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(boolean actual, boolean expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(boolean actual, boolean expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(byte actual, byte expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(byte actual, byte expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(char actual, char expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(char actual, char expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(short actual, short expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(short actual, short expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(int actual, int expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(int actual, int expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertNotNull(Object object) {
        Assert.assertNotNull(object);
    }

    public void assertNotNull(Object object, String msg) {
        Assert.assertNotNull(object, msg);
    }

    public void assertNull(Object object) {
        Assert.assertNull(object);
    }

    public void assertNull(Object object, String msg) {
        Assert.assertNull(object, msg);
    }

    public void assertSame(Object actual, Object expected, String msg) {
        Assert.assertSame(actual, expected, msg);
    }

    public void assertSame(Object actual, Object expected) {
        Assert.assertSame(actual, expected);
    }

    public void assertNotSame(Object actual, Object expected, String msg) {
        Assert.assertNotSame(actual, expected, msg);
    }

    public void assertNotSame(Object actual, Object expected) {
        Assert.assertNotSame(actual, expected);
    }

    public void assertEquals(Collection actual, Collection expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(Collection actual, Collection expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEquals(Object[] actual, Object[] expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
    }

    public void assertEqualsNoOrder(Object[] actual, Object[] expected, String msg) {
        Assert.assertEqualsNoOrder(actual, expected, msg);
    }

    public void assertEquals(Object[] actual, Object[] expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEqualsNoOrder(Object[] actual, Object[] expected) {
        Assert.assertEqualsNoOrder(actual, expected);
    }

    public void assertEquals(byte[] actual, byte[] expected) {
        Assert.assertEquals(actual, expected);
    }

    public void assertEquals(byte[] actual, byte[] expected, String msg) {
        Assert.assertEquals(actual, expected, msg);
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

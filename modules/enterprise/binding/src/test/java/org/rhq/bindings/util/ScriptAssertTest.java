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

import static org.testng.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.bindings.FakeRhqFacade;
import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;

public class ScriptAssertTest {

    private ScriptEngine engine;

    @BeforeTest
    public void verifyScriptEngineIsAvailable() throws Exception {
        StandardBindings bindings = new StandardBindings(new PrintWriter(System.out), new FakeRhqFacade());
        engine = ScriptEngineFactory.getScriptEngine("javascript", new PackageFinder(Collections.<File> emptyList()),
                bindings);
    }

    @Test
    public void testAssertExists() {
        testWorks("var a = 1; assertExists('a');", "assertExists should succeed for a defined variable.");
        testThrowsAssertion("assertExists('foo')", "assertExists should fail for an undefined variable.");
        testWorks("function func() { return 42 }; assertExists('func');",
            "assertExists should succeed for a defined function.");
    }

    @Test
    public void testAssertTrue() {
        testWorks("var a = true; assertTrue(a);", "assertTrue of a true variable should succeed");
        testWorks("assertTrue(1 == 1)", "assertTrue on a true boolean expression should succeed");
        testThrowsAssertion("var a = false; assertTrue(a)", "assertTrue should fail on a false variable");
        testThrowsAssertion("assertTrue(1 == 2)", "assertTrue should fail on a false boolean expression");
    }
    
    @Test
    public void testAssertFalse() {
        testWorks("var a = false; assertFalse(a);", "assertFalse of a false variable should succeed");
        testWorks("assertFalse(1 == 2)", "assertFalse on a false boolean expression should succeed");
        testThrowsAssertion("var a = true; assertFalse(a)", "assertFalse should fail on a true variable");
        testThrowsAssertion("assertFalse(1 == 1)", "assertFalse should fail on a true boolean expression");
    }
    
    @Test
    public void testAssertNull() {
        testWorks("assertNull(foo)", "assertNull should succeed on an undefined variable");
        testWorks("var foo = null; assertNull(foo);", "assertNull should succeed on a null variable");
        testWorks("assertNull(null)", "assertNull should succeed on a null literal");
        testThrowsAssertion("assertNull(1)", "assertNull should fail on a number");
        testThrowsAssertion("var foo = '1'; assertNull(foo)", "assertNull should fail on a non-null variable");
    }
    
    @Test
    public void testAssertNotNull() {
        testWorks("assertNotNull(1)", "assertNotNull should succeed on a number");
        testWorks("var foo = '1'; assertNotNull(foo)", "assertNotNull should succeed on a non-null variable");
        testThrowsAssertion("assertNotNull(foo)", "assertNotNull should fail on an undefined variable");
        testThrowsAssertion("var foo = null; assertNotNull(foo);", "assertNotNull should fail on a null variable");
        testThrowsAssertion("assertNotNull(null)", "assertNotNull should fail on a null literal");
    }
    
    @Test
    public void testAssertEquals_Numbers() {
        testWorks("assertEquals(1, 1)", "1 == 1");
        testWorks("assertEquals(1.0, 1)", "1.0 == 1");
        testWorks("assertEquals(1.0, 1.0)", "1.0 == 1.0");
        testThrowsAssertion("assertEquals(1, 2)", "1 == 2");
    }
    
    @Test
    public void testAssertEquals_Arrays() {
        testWorks("assertEquals(['a', 'b'], ['a', 'b'])", "native array comparison");
        testThrowsAssertion("assertEquals(['a', 'b'], ['c', 'd'])", "native array comparison with difference");
    }

    @Test
    public void testAssertEquals_Collections() {
        testWorks("a = new java.util.ArrayList; " + "b = new java.util.ArrayList; " + "a.add('a'); " + "b.add('a'); "
            + "assertEquals(a, b)", "ArrayList comparison");
        testThrowsAssertion("a = new java.util.ArrayList; " + "b = new java.util.ArrayList; " + "a.add('a'); "
            + "b.add('b'); " + "assertEquals(a, b)", "ArrayList comparison with difference");
    }

    @Test
    public void testAssertEqualsNoOrder() {
        testWorks("assertEqualsNoOrder(['a', 'b'], ['b', 'a'])", "native array comparison");
    }

    @Test
    public void testAssertSame() {
        testWorks("var a = '1'; assertSame(a, a)", "assertSame should succeed comparing one variable");
        testWorks("var a = '1'; b = a; assertSame(a, b)", "asserSame should succeed comparing 2 references of the same object");
        testWorks("assertSame(null, null);", "assertSame should succeed on null values");
        testThrowsAssertion("var a = '1'; b = '2'; assertSame(a, b)", "assertSame should fail comparing 2 different variables");
        testThrowsAssertion("var a = 1; assertSame(a, null)", "assertSame should fail comparing non-null variable with a null value");
    }
    
    @Test
    public void testAssertNotSame() {
        testThrowsAssertion("var a = '1'; assertNotSame(a, a)", "assertNotSame should fail comparing one variable");
        testThrowsAssertion("var a = '1'; b = a; assertNotSame(a, b)", "asserNotSame should fail comparing 2 references of the same object");
        testThrowsAssertion("assertNotSame(null, null);", "assertNotSame should fail on null values");
        testWorks("var a = '1'; b = '2'; assertNotSame(a, b)", "assertNotSame should succeed comparing 2 different variables");
        testWorks("var a = 1; assertNotSame(a, null)", "assertNotSame should succeed comparing non-null variable with a null value");
    }
    
    private void testWorks(String script, String message) {
        try {
            engine.eval(script);
        } catch (ScriptException e) {
            fail(message, e);
        }
    }

    private void testThrowsAssertion(String script, String message) {
        try {
            engine.eval(script);
        } catch (ScriptException e) {
            checkExpectedAssertionException(e, message);
        }
    }

    private static void checkExpectedAssertionException(Throwable t, String message) {
        boolean ok = false;
        while (t != null) {
            if (t instanceof ScriptAssertionException) {
                ok = true;
                break;
            } else if ((t instanceof ScriptException) && (t.getMessage().contains("ScriptAssertionException"))) {
                ok = true;
                break;
            }

            t = t.getCause();
        }

        if (!ok) {
            fail((message == null ? "" : message) + " ScriptAssertException expected but wasn't found.", t);
        }
    }
}

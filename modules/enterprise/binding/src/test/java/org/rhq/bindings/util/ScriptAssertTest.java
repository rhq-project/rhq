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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.annotations.Test;

import org.rhq.bindings.FakeRhqFacade;
import org.rhq.bindings.ScriptedTestBase;
import org.rhq.bindings.StandardBindings;

public class ScriptAssertTest extends ScriptedTestBase {

    private ScriptEngine getScriptEngine() {
        try {
            return getScriptEngine(new PackageFinder(Collections.<File> emptyList()), new StandardBindings(
                new PrintWriter(System.out), new FakeRhqFacade()));
        } catch (ScriptException e) {
            fail("Could not get the script engine.", e);
            return null;
        } catch (IOException e) {
            fail("Could not get the script engine.", e);
            return null;
        }
    }

    @Test
    public void testAssertExists_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "var a = 1; assertExists('a');", "assertExists should succeed for a defined variable.");
        testThrowsAssertion(engine, "assertExists('foo')", "assertExists should fail for an undefined variable.");
        testWorks(engine, "function func() { return 42 }; assertExists('func');",
            "assertExists should succeed for a defined function.");
    }

    @Test
    public void testAssertExists_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "a = 1\n" + "assertExists('a')", "assertExists should succeed for a defined variable.");
        testThrowsAssertion(engine, "assertExists('foo')", "assertExists should fail for an undefined variable.");
        testWorks(engine, "def func():\n" + " return 42\n" + "assertExists('func')",
            "assertExists should succeed for a defined function.");
    }

    @Test
    public void testAssertTrue_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "var a = true; assertTrue(a);", "assertTrue of a true variable should succeed");
        testWorks(engine, "assertTrue(1 == 1)", "assertTrue on a true boolean expression should succeed");
        testThrowsAssertion(engine, "var a = false; assertTrue(a)", "assertTrue should fail on a false variable");
        testThrowsAssertion(engine, "assertTrue(1 == 2)", "assertTrue should fail on a false boolean expression");
    }

    @Test
    public void testAssertTrue_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "a = True\n" + "assertTrue(a)", "assertTrue of a true variable should succeed");
        testWorks(engine, "assertTrue(1 == 1)", "assertTrue on a true boolean expression should succeed");
        testThrowsAssertion(engine, "a = False\n" + "assertTrue(a)", "assertTrue should fail on a false variable");
        testThrowsAssertion(engine, "assertTrue(1 == 2)", "assertTrue should fail on a false boolean expression");
    }

    @Test
    public void testAssertFalse_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "var a = false; assertFalse(a)", "assertFalse of a false variable should succeed");
        testWorks(engine, "assertFalse(1 == 2)", "assertFalse on a false boolean expression should succeed");
        testThrowsAssertion(engine, "var a = true; assertFalse(a)", "assertFalse should fail on a true variable");
        testThrowsAssertion(engine, "assertFalse(1 == 1)", "assertFalse should fail on a true boolean expression");
    }

    @Test
    public void testAssertFalse_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "a = False\n" + "assertFalse(a)", "assertFalse of a false variable should succeed");
        testWorks(engine, "assertFalse(1 == 2)", "assertFalse on a false boolean expression should succeed");
        testThrowsAssertion(engine, "a = True\n" + "assertFalse(a)", "assertFalse should fail on a true variable");
        testThrowsAssertion(engine, "assertFalse(1 == 1)", "assertFalse should fail on a true boolean expression");
    }

    @Test
    public void testAssertNull_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "var foo = null; assertNull(foo);", "assertNull should succeed on a null variable");
        testWorks(engine, "assertNull(null)", "assertNull should succeed on a null literal");
        testThrowsAssertion(engine, "assertNull(1)", "assertNull should fail on a number");
        testThrowsAssertion(engine, "var foo = '1'; assertNull(foo)", "assertNull should fail on a non-null variable");
    }

    @Test
    public void testAssertNull_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "foo = None\n" + "assertNull(foo)", "assertNull should succeed on a null variable");
        testWorks(engine, "assertNull(None)", "assertNull should succeed on a null literal");
        testThrowsAssertion(engine, "assertNull(1)", "assertNull should fail on a number");
        testThrowsAssertion(engine, "foo = '1'\n" + "assertNull(foo)", "assertNull should fail on a non-null variable");
    }

    @Test
    public void testAssertNotNull_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertNotNull(1)", "assertNotNull should succeed on a number");
        testWorks(engine, "var foo = '1'; assertNotNull(foo)", "assertNotNull should succeed on a non-null variable");
        testThrowsAssertion(engine, "assertNotNull(foo)", "assertNotNull should fail on an undefined variable");
        testThrowsAssertion(engine, "var foo = null; assertNotNull(foo);",
            "assertNotNull should fail on a null variable");
        testThrowsAssertion(engine, "assertNotNull(null)", "assertNotNull should fail on a null literal");
    }

    @Test
    public void testAssertNotNull_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertNotNull(1)", "assertNotNull should succeed on a number");
        testWorks(engine, "foo = '1'\n" + "assertNotNull(foo)", "assertNotNull should succeed on a non-null variable");
        testThrowsAssertion(engine, "assertNotNull(foo)", "assertNotNull should fail on an undefined variable");
        testThrowsAssertion(engine, "foo = None\n" + "assertNotNull(foo);",
            "assertNotNull should fail on a null variable");
        testThrowsAssertion(engine, "assertNotNull(None)", "assertNotNull should fail on a null literal");
    }

    @Test
    public void testAssertEquals_Numbers_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertEquals(1, 1)", "1 == 1");
        testWorks(engine, "assertEquals(1.0, 1)", "1.0 == 1");
        testWorks(engine, "assertEquals(1.0, 1.0)", "1.0 == 1.0");
        testThrowsAssertion(engine, "assertEquals(1, 2)", "1 == 2");
    }

    @Test
    public void testAssertEquals_Numbers_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertEquals(1, 1)", "1 == 1");
        //Python distinguishes between ints and floats, so this
        //won't work (even though "1.0 == 1" returns true in pure python)
        //testWorks(engine, "assertEquals(1.0, 1)", "1.0 == 1");
        testWorks(engine, "assertEquals(1.0, 1.0)", "1.0 == 1.0");
        testThrowsAssertion(engine, "assertEquals(1, 2)", "1 == 2");
    }

    @Test
    public void testAssertEquals_Arrays_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertEquals(['a', 'b'], ['a', 'b'])", "native array comparison");
        testThrowsAssertion(engine, "assertEquals(['a', 'b'], ['c', 'd'])", "native array comparison with difference");
    }

    @Test
    public void testAssertEquals_Arrays_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertEquals(['a', 'b'], ['a', 'b'])", "native array comparison");
        testThrowsAssertion(engine, "assertEquals(['a', 'b'], ['c', 'd'])", "native array comparison with difference");
    }

    @Test
    public void testAssertEquals_Collections_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "a = new java.util.ArrayList; " + "b = new java.util.ArrayList; " + "a.add('a'); "
            + "b.add('a'); " + "assertEquals(a, b)", "ArrayList comparison");
        testThrowsAssertion(engine, "a = new java.util.ArrayList; " + "b = new java.util.ArrayList; " + "a.add('a'); "
            + "b.add('b'); " + "assertEquals(a, b)", "ArrayList comparison with difference");
    }

    @Test
    public void testAssertEquals_Collections_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "import java.util as u\n" + "a = u.ArrayList()\n" + "b = u.ArrayList()\n" + "a.add('a')\n"
            + "b.add('a')\n" + "assertEquals(a, b)", "ArrayList comparison");
        testThrowsAssertion(engine, "import java.util as u\n" + "a = u.ArrayList()\n" + "b = u.ArrayList()\n" + "a.add('a')\n"
            + "b.add('b')\n" + "assertEquals(a, b)", "ArrayList comparison with difference");
    }

    @Test
    public void testAssertEqualsNoOrder_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertEqualsNoOrder(['a', 'b'], ['b', 'a'])", "native array comparison");
    }

    @Test
    public void testAssertEqualsNoOrder_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "assertEqualsNoOrder(['a', 'b'], ['b', 'a'])", "native array comparison");
    }

    @Test
    public void testAssertSame_javascript() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "var a = '1'; assertSame(a, a)", "assertSame should succeed comparing one variable");
        testWorks(engine, "var a = '1'; b = a; assertSame(a, b)",
            "asserSame should succeed comparing 2 references of the same object");
        testWorks(engine, "assertSame(null, null);", "assertSame should succeed on null values");
        testThrowsAssertion(engine, "var a = '1'; b = '2'; assertSame(a, b)",
            "assertSame should fail comparing 2 different variables");
        testThrowsAssertion(engine, "var a = 1; assertSame(a, null)",
            "assertSame should fail comparing non-null variable with a null value");
    }

    @Test
    public void testAssertSame_python() {
        ScriptEngine engine = getScriptEngine();
        testWorks(engine, "a = '1'\n" + "assertSame(a, a)", "assertSame should succeed comparing one variable");
        testWorks(engine, "a = '1'\n" + "b = a\n" + "assertSame(a, b)",
            "asserSame should succeed comparing 2 references of the same object");
        testWorks(engine, "assertSame(None, None)", "assertSame should succeed on null values");
        testThrowsAssertion(engine, "a = '1'\n" + "b = '2'\n" + "assertSame(a, b)",
            "assertSame should fail comparing 2 different variables");
        testThrowsAssertion(engine, "a = 1\n" + "assertSame(a, None)",
            "assertSame should fail comparing non-null variable with a null value");
    }

    @Test
    public void testAssertNotSame_javascript() {
        ScriptEngine engine = getScriptEngine();
        testThrowsAssertion(engine, "var a = '1'; assertNotSame(a, a)",
            "assertNotSame should fail comparing one variable");
        testThrowsAssertion(engine, "var a = '1'; b = a; assertNotSame(a, b)",
            "asserNotSame should fail comparing 2 references of the same object");
        testThrowsAssertion(engine, "assertNotSame(null, null);", "assertNotSame should fail on null values");
        testWorks(engine, "var a = '1'; b = '2'; assertNotSame(a, b)",
            "assertNotSame should succeed comparing 2 different variables");
        testWorks(engine, "var a = 1; assertNotSame(a, null)",
            "assertNotSame should succeed comparing non-null variable with a null value");
    }

    @Test
    public void testAssertNotSame_python() {
        ScriptEngine engine = getScriptEngine();
        testThrowsAssertion(engine, "a = '1'\n" + "assertNotSame(a, a)",
            "assertNotSame should fail comparing one variable");
        testThrowsAssertion(engine, "a = '1'\n" + "b = a\n" + "assertNotSame(a, b)",
            "asserNotSame should fail comparing 2 references of the same object");
        testThrowsAssertion(engine, "assertNotSame(None, None)", "assertNotSame should fail on null values");
        testWorks(engine, "a = '1'\n" + "b = '2'\n" + "assertNotSame(a, b)",
            "assertNotSame should succeed comparing 2 different variables");
        testWorks(engine, "a = 1\n" + "assertNotSame(a, None)",
            "assertNotSame should succeed comparing non-null variable with a null value");
    }

    private void testWorks(ScriptEngine engine, String script, String message) {
        try {
            engine.eval(script);
        } catch (ScriptException e) {
            fail(message, e);
        }
    }

    private void testThrowsAssertion(ScriptEngine engine, String script, String message) {
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

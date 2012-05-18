/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package com.sun.phobos.script.javascript;

import javax.script.ScriptContext;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * We want our script engine to mimic the real JDK one as close as
 * possible.
 * 
 * The Phobos impl of the script engine leaves out the predefined 
 * print() and println() functions, but we really want them.
 * 
 * To add them back we need to override a package-private method
 * in the phobos script engine.
 *
 * @author Lukas Krejci
 */
public class PrintHavingRhinoScriptEngine extends RhinoScriptEngine {

    //copied over from the JDK's impl of RhinoScriptEngine
    private static final String printSource =
        "function print(str, newline) {                \n" +
        "    if (typeof(str) == 'undefined') {         \n" +
        "        str = 'undefined';                    \n" +
        "    } else if (str == null) {                 \n" +
        "        str = 'null';                         \n" +
        "    }                                         \n" +
        "    var out = context.getWriter();            \n" +
        "    out.print(String(str));                   \n" +
        "    if (newline) out.print('\\n');            \n" +
        "    out.flush();                              \n" +
        "}\n" +
        "function println(str) {                       \n" +
        "    print(str, true);                         \n" +
        "}";
    
    @Override
    Scriptable getRuntimeScope(ScriptContext ctxt) {
        Scriptable newScope = super.getRuntimeScope(ctxt);
        
        Context cx = enterContext();
        try {
            cx.evaluateString(newScope, printSource, "print", 1, null);
            
            return newScope;
        } finally {
            Context.exit();
        }
    }
}

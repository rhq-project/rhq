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

package org.rhq.scripting.javascript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;

import com.sun.phobos.script.javascript.RhinoScriptEngineFactory;

import org.mozilla.javascript.Context;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ScriptEngineFactory extends RhinoScriptEngineFactory {
    
    private static final List<String> NAMES;
    private static final String ENGINE_VERSION;
    private static final String LANGUAGE_VERSION;
    static {
        Context.enter();
        ENGINE_VERSION = Context.getCurrentContext().getImplementationVersion();
        int ver = Context.getCurrentContext().getLanguageVersion();
        Context.exit();
        
        String version = null;
        if (ver == 0) {
            version = "1.7";
        } else {
            //the versions are formatted like 170 for 1.7, 180 for 1.8, etc
            int major = ver / 100;
            int minor = (ver - 100) / 10;
            version = major + "." + minor;
        }
        
        LANGUAGE_VERSION = version;
        
        NAMES = Collections.unmodifiableList(Arrays.asList("rhino-nonjdk"));
        
    }
    
    @Override
    public List<String> getNames() {
        return NAMES;
    }
    
    @Override
    public Object getParameter(String key) {
        if (ScriptEngine.ENGINE_VERSION.equals(key)) {
            return ENGINE_VERSION;
        } else if (ScriptEngine.LANGUAGE_VERSION.equals(key)) {
            return LANGUAGE_VERSION;
        } else {
            return super.getParameter(key);
        }
    }   
    
    @Override
    public ScriptEngine getScriptEngine() {
        org.rhq.scripting.javascript.ScriptEngine engine = new org.rhq.scripting.javascript.ScriptEngine();
        engine.setFactory(this);
        return engine;
    }
}

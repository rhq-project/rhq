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

package org.rhq.scripting.javascript.util;

import static org.testng.Assert.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.annotations.Test;

import org.rhq.scripting.ScriptSourceProvider;
import org.rhq.scripting.javascript.JsEngineInitializer;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ScriptSourceToModuleSourceProviderAdapterTest {

    private JsEngineInitializer initializer = new JsEngineInitializer();
    
    private static class ClasspathScriptSourceProvider implements ScriptSourceProvider {
        private ClassLoader cl;
        
        public ClasspathScriptSourceProvider() {
            this(ClasspathScriptSourceProvider.class.getClassLoader());
        }
        
        public ClasspathScriptSourceProvider(ClassLoader cl) {
            this.cl = cl;
        }
        
        private static final String SCHEME = "classpath";
        @Override
        public Reader getScriptSource(URI uri) {
            if (!SCHEME.equals(uri.getScheme())) {
                return null;
            }
            
            String path = uri.getPath().substring(1); //remove the leading /
            
            return new InputStreamReader(cl.getResourceAsStream(path));
        }
    }
    
    private ScriptEngine getScriptEngine() throws ScriptException {
        ScriptEngine engine = initializer.instantiate(Collections.<String>emptySet(), null);
        initializer.installScriptSourceProvider(engine, new ClasspathScriptSourceProvider());
        
        return engine;
    }
    
    public void simpleModuleLoad() throws Exception {
        String script = "var m = require('classpath:/nested/deep-nest'); m.nest();";
        
        Object ret = getScriptEngine().eval(script);
        
        assertEquals(ret, "Deep nest", "deep-nest.js#nest() returned unexpected value");
    }
    
    public void relativePathsInRequire() throws Exception {
        String script = "var m = require('classpath:/nest'); m.maze();";
        
        Object ret = getScriptEngine().eval(script);
        
        assertEquals(ret, "func1", "nest.js#maze() returned unexpected value");
    }    
    
    public void urisWithAuthorityDontConfuseResolution() throws Exception {
        String script = "var m = require('classpath://authority/nest'); m.maze();";
        
        Object ret = getScriptEngine().eval(script);
        
        assertEquals(ret, "func1", "nest.js#maze() returned unexpected value");
    }

    public void urisWithQueryDontConfuseResolution() throws Exception {
        String script = "var m = require('classpath:/nest?query'); m.maze();";
        
        Object ret = getScriptEngine().eval(script);
        
        assertEquals(ret, "func1", "nest.js#maze() returned unexpected value");
    }

    public void urisWithFragmentDontConfuseResolution() throws Exception {
        String script = "var m = require('classpath:/nest#fragment'); m.maze();";
        
        Object ret = getScriptEngine().eval(script);
        
        assertEquals(ret, "func1", "nest.js#maze() returned unexpected value");
    }
}

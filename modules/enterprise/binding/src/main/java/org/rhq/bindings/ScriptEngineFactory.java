/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.bindings;

import java.io.IOException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.rhq.bindings.engine.JsEngineInitializer;
import org.rhq.bindings.engine.ScriptEngineInitializer;
import org.rhq.bindings.export.Exporter;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.util.PageControl;

/**
 * This is RHQ specific imitation of ScriptEngineFactory.
 * 
 * In RHQ, we provide a standard set of bound variables in the script context
 * and also import classes from our standard packages to ease the development
 * of the scripts.
 * <p>
 * This factory is able to instantiate a script engine and initialize it consistently
 * so that all users of the script engine get the uniform environment to write the scripts
 * in.
 *
 * @author Lukas Krejci
 */
public class ScriptEngineFactory {

    private static final ScriptEngineInitializer[] KNOWN_ENGINES = {new JsEngineInitializer()};
    
    private ScriptEngineFactory() {
        
    }
    
    /**
     * Initializes the script engine for given language.
     * 
     * @param language the language of the script to instantiate
     * @param packageFinder the package finder to find the standard packages in user provided locations
     * @return the initialized engine or null if the engine for given language isn't known.
     * 
     * @throws ScriptException on error during initialization of the script environment
     * @throws IOException if the package finder fails to find the packages
     */
    public static ScriptEngine getScriptEngine(String language, PackageFinder packageFinder) throws ScriptException, IOException {
        ScriptEngineInitializer initializer = getInitializer(language);
        
        if (initializer == null) {
            return null;
        }
        
        ScriptEngine engine = initializer.instantiate(packageFinder.findPackages("org.rhq.core.domain"));

        injectStandardBindings(engine);
        
        return engine;
    }
    
    public static void injectStandardBindings(ScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        PageControl pc = new PageControl();
        pc.setPageNumber(-1);
        bindings.put("unlimitedPC", pc);
        bindings.put("pageControl", PageControl.getUnlimitedInstance());
        bindings.put("exporter", new Exporter());
        
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);        
    }
    
    private static ScriptEngineInitializer getInitializer(String language) {
        for (ScriptEngineInitializer i : KNOWN_ENGINES) {
            if (i.getEngineName().equals(language)) {
                return i;
            }
        }
        
        return null;
    }
}

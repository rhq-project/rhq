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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.engine.JsEngineInitializer;
import org.rhq.bindings.engine.ScriptEngineInitializer;
import org.rhq.bindings.util.PackageFinder;

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
    private static final Log LOG = LogFactory.getLog(ScriptEngineFactory.class);
    
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
    public static ScriptEngine getScriptEngine(String language, PackageFinder packageFinder, StandardBindings bindings) throws ScriptException, IOException {
        ScriptEngineInitializer initializer = getInitializer(language);
        
        if (initializer == null) {
            return null;
        }
        
        ScriptEngine engine = initializer.instantiate(packageFinder.findPackages("org.rhq.core.domain"));

        injectStandardBindings(engine, bindings);
        
        return engine;
    }
    
    public static void injectStandardBindings(ScriptEngine engine, StandardBindings bindings) {
        bindings.preInject(engine);
        
        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        for(Map.Entry<String, Object> entry : bindings.entrySet()) {
            engineBindings.put(entry.getKey(), entry.getValue());
        }
        
        engine.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
        
        bindings.postInject(engine);
    }
    
    public static void bindIndirectionMethods(ScriptEngine scriptEngine, String bindingName, Object object) {
        ScriptEngineInitializer initializer = getInitializer(scriptEngine.getFactory().getLanguageName());
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass(), Object.class);
            MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();

            for (MethodDescriptor methodDescriptor : methodDescriptors) {
                Method method = methodDescriptor.getMethod();
                try {
                    String methodDef = initializer.generateIndirectionMethod(bindingName, method);
                    scriptEngine.eval(methodDef);
                } catch (ScriptException e) {
                    LOG.warn("Unable to bind global function " + method.getName(), e);
                }
            }
        } catch (IntrospectionException e) {
            // TODO Should we altogether remove the object from the script engine bindings?
            LOG.warn("Could not bind " + object.getClass().getName() + " into script engine.");
        }
    }

    
    public static ScriptEngineInitializer getInitializer(String language) {
        for (ScriptEngineInitializer i : KNOWN_ENGINES) {
            if (i.implementsLanguage(language)) {
                return i;
            }
        }
        
        return null;
    }
}

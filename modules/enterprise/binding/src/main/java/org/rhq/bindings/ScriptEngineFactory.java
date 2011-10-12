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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.engine.JsEngineInitializer;
import org.rhq.bindings.engine.ScriptEngineInitializer;
import org.rhq.bindings.util.NoTopLevelIndirection;
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

    private static final ScriptEngineInitializer[] KNOWN_ENGINES = { new JsEngineInitializer() };

    private ScriptEngineFactory() {

    }

    /**
     * Initializes the script engine for given language.
     * 
     * @param language the language of the script to instantiate
     * @param packageFinder the package finder to find the standard packages in user provided locations
     * @param bindings the initial standard bindings or null if none required
     * @return the initialized engine or null if the engine for given language isn't known.
     * 
     * @throws ScriptException on error during initialization of the script environment
     * @throws IOException if the package finder fails to find the packages
     */
    public static ScriptEngine getScriptEngine(String language, PackageFinder packageFinder, StandardBindings bindings)
        throws ScriptException, IOException {
        ScriptEngineInitializer initializer = getInitializer(language);

        if (initializer == null) {
            return null;
        }

        ScriptEngine engine = initializer.instantiate(packageFinder.findPackages("org.rhq.core.domain"));

        if (bindings != null) {
            injectStandardBindings(engine, bindings, true);
        }

        return engine;
    }

    /**
     * Injects the values provided in the bindings into the {@link ScriptContext#ENGINE_SCOPE engine scope}
     * of the provided script engine.
     * 
     * @param engine the engine
     * @param bindings the bindings
     * @param deleteExistingBindings true if the existing bindings should be replaced by the provided ones, false
     * if the provided bindings should be added to the existing ones (possibly overwriting bindings with the same name).
     */
    public static void injectStandardBindings(ScriptEngine engine, StandardBindings bindings,
        boolean deleteExistingBindings) {
        bindings.preInject(engine);

        Bindings engineBindings =
            deleteExistingBindings ? engine.createBindings() : engine.getBindings(ScriptContext.ENGINE_SCOPE);

        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            engineBindings.put(entry.getKey(), entry.getValue());
        }

        engine.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);

        bindings.postInject(engine);
    }

    /**
     * Goes through the methods of the object found in the <code>scriptEngine</code>'s ENGINE_SCOPE
     * and for each of them generates a top-level function that is called the same name and accepts the same
     * parameters.
     * 
     * @param scriptEngine the script engine to generate the top-level functions in
     * @param bindingName the name of the object in the script engine to generate the functions from
     * 
     * @see ScriptEngineInitializer#generateIndirectionMethod(String, Method)
     * @see NoTopLevelIndirection
     */
    public static void bindIndirectionMethods(ScriptEngine scriptEngine, String bindingName) {
        Object object = scriptEngine.get(bindingName);
        if (object == null) {
            LOG.debug("The script engine doesn't contain a binding called '" + bindingName
                + "'. No indirection functions will be generated.");
            return;
        }

        ScriptEngineInitializer initializer = getInitializer(scriptEngine.getFactory().getLanguageName());
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass(), Object.class);
            MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();

            Map<String, Set<Method>> overloadsPerMethodName = new HashMap<String, Set<Method>>();
            for (MethodDescriptor methodDescriptor : methodDescriptors) {
                Method method = methodDescriptor.getMethod();
                if (shouldIndirect(method)) {
                    Set<Method> overloads = overloadsPerMethodName.get(method.getName());
                    if (overloads == null) {
                        overloads = new HashSet<Method>();
                        overloadsPerMethodName.put(method.getName(), overloads);
                    }
                    overloads.add(method);
                }
            }

            for (Set<Method> overloads : overloadsPerMethodName.values()) {
                Set<String> methodDefs = initializer.generateIndirectionMethods(bindingName, overloads);
                for (String methodDef : methodDefs) {
                    try {
                        scriptEngine.eval(methodDef);
                    } catch (ScriptException e) {
                        LOG.warn("Unable to define global function declared as:\n" + methodDef, e);
                    }
                }
            }
        } catch (IntrospectionException e) {
            LOG.debug("Could not inspect class " + object.getClass().getName()
                + ". No indirection methods for variable '" + bindingName + "' will be generated.", e);
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

    private static boolean shouldIndirect(Method method) {
        return method.getAnnotation(NoTopLevelIndirection.class) == null;
    }
}

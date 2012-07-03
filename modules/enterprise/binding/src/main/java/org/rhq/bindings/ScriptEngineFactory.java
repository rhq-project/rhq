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
import java.security.PermissionCollection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.client.RhqManager;
import org.rhq.bindings.util.MultiScriptSourceProvider;
import org.rhq.bindings.util.NoTopLevelIndirection;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.scripting.CodeCompletion;
import org.rhq.scripting.ScriptEngineInitializer;
import org.rhq.scripting.ScriptEngineProvider;
import org.rhq.scripting.ScriptSourceProvider;

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

    private static final Map<String, ScriptEngineProvider> KNOWN_PROVIDERS;
    static {
        KNOWN_PROVIDERS = new HashMap<String, ScriptEngineProvider>();
        
        reloadScriptEngineProviders(null);
    }

    private ScriptEngineFactory() {

    }

    /**
     * Reloads the list of the known script engine providers using the given classloader
     * or the current thread's context classloader if it is null.
     * 
     * @param classLoader the classloader used to find the script engine providers on the classpath
     * 
     * @throws IllegalStateException if more than 1 script engine provider is found for a single language
     */
    public static void reloadScriptEngineProviders(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        ServiceLoader<ScriptEngineProvider> loader = ServiceLoader.load(ScriptEngineProvider.class, classLoader);

        KNOWN_PROVIDERS.clear();

        for (ScriptEngineProvider provider : loader) {
            String lang = provider.getSupportedLanguage();

            if (KNOWN_PROVIDERS.containsKey(lang)) {
                String existing = KNOWN_PROVIDERS.get(lang).getClass().getName();
                String thisOne = provider.getClass().getName();
                throw new IllegalStateException("'" + lang + "' scripting language provided by at least 2 providers: '"
                    + existing + "' and '" + thisOne + "'. Only 1 provider per language is allowed.");
            }

            KNOWN_PROVIDERS.put(lang, provider);
        }
    }
    
    /**
     * @return the set of the scripting languages supported by this factory
     */
    public static Set<String> getSupportedLanguages() {
        return new HashSet<String>(KNOWN_PROVIDERS.keySet());       
    }
    
    /**
     * Initializes the script engine for given language.
     * 
     * @param language the language of the script to instantiate
     * @param packageFinder the package finder to find the standard packages in user provided locations
     * @param bindings the initial standard bindings or null if none required
     * 
     * @return the initialized engine or null if a {@link ScriptEngineProvider} for given language isn't known.
     * 
     * @throws ScriptException on error during initialization of the script environment
     * @throws IOException if the package finder fails to find the packages
     */
    public static ScriptEngine getScriptEngine(String language, PackageFinder packageFinder, StandardBindings bindings) throws ScriptException, IOException {

        return getSecuredScriptEngine(language, packageFinder, bindings, null);
    }

    /**
     * This method is similar to the {@link #getScriptEngine(String, PackageFinder, StandardBindings, ScriptSourceProvider...)} method
     * but additionally applies a security wrapper on the returned script engine so that the scripts execute
     * with the provided java permissions.
     * 
     * @see #getScriptEngine(String, PackageFinder, StandardBindings, ScriptSourceProvider...)
     */
    public static ScriptEngine getSecuredScriptEngine(String language, PackageFinder packageFinder,
        StandardBindings bindings, PermissionCollection permissions) throws ScriptException, IOException {
        
        ScriptEngineInitializer initializer = getInitializer(language);

        if (initializer == null) {
            return null;
        }

        ScriptEngine engine = initializer.instantiate(packageFinder.findPackages("org.rhq.core.domain"),
            permissions);

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
     *        if the provided bindings should be added to the existing ones (possibly overwriting bindings with the same name).
     * @param scriptSourceProviders the list of script source providers to be used by the script engine to locate the scripts.
     *        Note that the providers become associated with the script engine and its bindings and therefore should
     *        not be used with any other script engine. If the providers implement the 
     *        {@link StandardBindings.RhqFacadeChangeListener} interface, they will be automatically hooked up with the
     *        <code>bindings</code> so that the providers will get notified whenever the rhq facade changes.
     */
    public static void injectStandardBindings(ScriptEngine engine, StandardBindings bindings,
        boolean deleteExistingBindings, ScriptSourceProvider... scriptSourceProviders) {
        bindings.preInject(engine);

        Bindings engineBindings = deleteExistingBindings ? engine.createBindings() : engine
            .getBindings(ScriptContext.ENGINE_SCOPE);

        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            engineBindings.put(entry.getKey(), entry.getValue());
        }

        if (scriptSourceProviders != null) {
            //first figure out which initializer to use with this script engine
            String language = (String) engine.getFactory().getParameter(ScriptEngine.NAME);
            ScriptEngineProvider engineProvider = KNOWN_PROVIDERS.get(language);
            if (engineProvider == null) {
                throw new IllegalArgumentException("The supplied script engine [" + engine + "] is not supported.");
            }

            ScriptEngineInitializer initializer = engineProvider.getInitializer();

            ScriptSourceProvider provider = null;

            for (ScriptSourceProvider p : scriptSourceProviders) {
                if (p instanceof StandardBindings.RhqFacadeChangeListener) {
                    bindings.addRhqFacadeChangeListener((StandardBindings.RhqFacadeChangeListener) p);
                }
            }
            provider = new MultiScriptSourceProvider(scriptSourceProviders);

            initializer.installScriptSourceProvider(engine, provider);
        }

        engine.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);

        bindings.postInject(engine);
    }

    /**
     * Remove the specified bindings from the engine.
     * 
     * @param engine the engine
     * @param keySet the binding keys to be removed
     */
    public static void removeBindings(ScriptEngine engine, Set<RhqManager> keySet) {

        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        for (RhqManager key : keySet) {
            engineBindings.remove(key.name());
        }

        engine.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
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

        ScriptEngineInitializer initializer = getInitializer((String) scriptEngine.getFactory().getParameter(
            ScriptEngine.NAME));

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
        ScriptEngineProvider provider = KNOWN_PROVIDERS.get(language);
        
        return provider == null ? null : provider.getInitializer();
    }

    public static CodeCompletion getCodeCompletion(String language) {
        ScriptEngineProvider provider = KNOWN_PROVIDERS.get(language);
        
        return provider == null ? null : provider.getCodeCompletion();
    }
    
    private static boolean shouldIndirect(Method method) {
        return method.getAnnotation(NoTopLevelIndirection.class) == null;
    }
}

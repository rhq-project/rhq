/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * Portions Copyright 2012 RHQ Management Platform 
 */

/*
 * RHQ Management Platform elects to include this software in this distribution 
 * under the GPL Version 2 license.
 */


 
package org.rhq.scripting.javascript.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Synchronizer;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import org.rhq.scripting.javascript.engine.util.ExtendedScriptException;
import org.rhq.scripting.javascript.engine.util.InterfaceImplementor;


/**
 * Implementation of <code>ScriptEngine</code> using the Mozilla Rhino
 * interpreter.
 *
 * @author Mike Grogan
 * @author A. Sundararajan
 * @version 1.0
 * @since 1.6
 *
 * Modified for phobos to remove some of the restrictions.
 * Modified to allow subclassing and preprocessing of script source code.
 * Modified to avoid using the RhinoTopLevel class, since that introduces
 * a circularity that prevents objects from being garbage collected.
 *
 * @author Roberto Chinnici
 * 
 * Modified so that the top level scope is an ImportTopLevel instance so 
 * that importClass and importPackage functions are available.
 * Modified so that the "print" and "println" functions work the same as with
 * the stock javascript script engine provided by the JVM.
 * Modified to include the "require()" function by default.
 * Modified to tighten the security of the script execution by running it in an
 * AccessControlContext active at the time of the script engine creation.
 * Modified to allow correct interoperability between Java and javascript string even
 * if represented by the custom ConsString instance.
 * 
 * @author Lukas Krejci
 */
public class RhinoScriptEngine  extends AbstractScriptEngine
        implements  Invocable, Compilable {
    
    public static final boolean DEBUG = false;
    private static final String TOPLEVEL_SCRIPT_NAME = "META-INF/toplevel.js";

    private static class TopLevelScope extends ImporterTopLevel {

        private static final long serialVersionUID = 1L;

        private AccessControlContext acc;
        
        public TopLevelScope(AccessControlContext acc, Context cx, boolean sealed) {
            super(cx, sealed);
            this.acc = acc;
        }
        
        public AccessControlContext getAccessControlContext() {
            return acc;
        }
    }
    
    /* Scope where standard JavaScript objects and our
     * extensions to it are stored. Note that these are not
     * user defined engine level global variables. These are
     * variables have to be there on all compliant ECMAScript
     * scopes. We put these standard objects in this top level.
     */
    private TopLevelScope topLevel;

    /* map used to store indexed properties in engine scope
     * refer to comment on 'indexedProps' in ExternalScriptable.java.
     */
    private Map<?, ?> indexedProps;

    private ScriptEngineFactory factory;
    private InterfaceImplementor implementor;

    //LK - added support for CommonJS modules
    private RequireBuilder requireBuilder;
    
    //LK - custom wrap factory to overcome the difficulties comparing java strings with ConsString instances
    //     introduced by Rhino 1.7R4.
    private static class CustomWrapFactory extends WrapFactory {

        /**
         * This behaves exactly the same as the super class' method except the fact that
         * the ConsString is considered "primitive" and is not wrapped in any manner.
         * <p>
         * This is then consistent with the rest of Rhino that expects ConsString as a possible
         * implementation of the string.
         */
        @Override
        public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
            if (obj instanceof ConsString) {
                return obj;
            }
            
            return super.wrap(cx, scope, obj, staticType);
        }
    }
    

    //LK - make all the scripts run in an access control context
    //LK - use a custom wrap factory to overcome the ConsString being mishandled when transferring from java to js and back
    static {
        ContextFactory.initGlobal(new ContextFactory() {
            @Override
            protected Object doTopCall(final Callable callable,
                               final Context cx, final Scriptable scope,
                               final Scriptable thisObj, final Object[] args) {
                AccessControlContext accCtxt = null;
                Scriptable global = ScriptableObject.getTopLevelScope(scope);
                Scriptable globalProto = global.getPrototype();
                if (globalProto instanceof TopLevelScope) {
                    accCtxt = ((TopLevelScope)globalProto).getAccessControlContext();
                }

                if (accCtxt != null) {
                    return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            return superDoTopCall(callable, cx, scope, thisObj, args);
                        }
                    }, accCtxt);
                } else {
                    return superDoTopCall(callable, cx, scope, thisObj, args);
                }
            }
                        
            @Override
            protected Context makeContext() {
                Context cx = super.makeContext();
                cx.setOptimizationLevel(-1);
                cx.setWrapFactory(new CustomWrapFactory());
                return cx;
            }

            private Object superDoTopCall(final Callable callable,
                final Context cx, final Scriptable scope,
                final Scriptable thisObj, final Object[] args) {
                
                return super.doTopCall(callable, cx, scope, thisObj, args);
            }

        });
    }
    
    /*
    // in Phobos we want to support all javascript features
    static {
        ContextFactory.initGlobal(new ContextFactory() {
            protected Context makeContext() {
                Context cx = super.makeContext();
                cx.setClassShutter(RhinoClassShutter.getInstance());
                cx.setWrapFactory(RhinoWrapFactory.getInstance());                
                return cx;
            }

            public boolean hasFeature(Context cx, int feature) {
                // we do not support E4X (ECMAScript for XML)!
                if (feature == Context.FEATURE_E4X) {
                    return false;
                } else {
                    return super.hasFeature(cx, feature);
                }
            }
        });
    }

    static {
        if (USE_INTERPRETER) {
            ContextFactory.initGlobal(new ContextFactory() {
                protected Context makeContext() {
                    Context cx = super.makeContext();
                    cx.setOptimizationLevel(-1);
                    return cx;
                }
            });
        }
    }
    */
    
    public RhinoScriptEngine() {
        this(new UrlModuleSourceProvider(null, Arrays.asList(new File("./").toURI())));
    }
    
    /**
     * Creates a new instance of RhinoScriptEngine with given moduleSourceProvider as the "locator"
     * for the CommonJS modules.
     * 
     * @param moduleSourceProvider the implementation able to locate sources of modules for CommonJS.
     */
    public RhinoScriptEngine(ModuleSourceProvider moduleSourceProvider) {
       
        Context cx = enterContext();

        try { 
            /*
             * RRC - modified this code to register JSAdapter and some functions
             * directly, without using a separate RhinoTopLevel class
             */
            /*
             * LK - made the topLevel at the the ImporterTopLevel so that
             * the circular reference to this script engine is avoided but
             * all the functions (importClass, importPackage) are available.
             * Also provide the security features similar to the bundled script engine.
             */
            topLevel = new TopLevelScope(AccessController.getContext(), cx, System.getSecurityManager() != null);
            
            requireBuilder = new RequireBuilder();            
            setModuleSourceProvider(moduleSourceProvider);
            requireBuilder.setSandboxed(false);

            new LazilyLoadedCtor(topLevel, "JSAdapter",
                "org.rhq.scripting.javascript.engine.JSAdapter",
                false);
            // add top level functions
            String names[] = { "bindings", "scope", "sync"  };
            topLevel.defineFunctionProperties(names, RhinoScriptEngine.class, ScriptableObject.DONTENUM);
            
            processAllTopLevelScripts(cx);
        } finally {
            Context.exit();
        }
        
        indexedProps = new HashMap<Object, Object>();
 
        //construct object used to implement getInterface
        implementor = new InterfaceImplementor(this) {
                @Override
                protected Object convertResult(Method method, Object res)
                                            throws ScriptException {
                    Class<?> desiredType = method.getReturnType();
                    if (desiredType == Void.TYPE) {
                        return null;
                    } else {
                        return Context.jsToJava(res, desiredType);
                    }
                }
        };
    }
    
    public void setModuleSourceProvider(ModuleSourceProvider provider) {
        requireBuilder.setModuleScriptProvider(new SoftCachingModuleScriptProvider(provider));
    }
    
    @Override
    public Object eval(Reader reader, ScriptContext ctxt)
    throws ScriptException {
        Object ret;
        
        Context cx = enterContext();
        try {
            Scriptable scope = getRuntimeScope(ctxt);
            scope.put("context", scope, ctxt);

            // NOTE (RRC) - why does it look straight into the engine instead of asking
            // the given ScriptContext object?
            // Modified to use the context
            // String filename = (String) get(ScriptEngine.FILENAME);
            String filename = null;
            if (ctxt != null && ctxt.getBindings(ScriptContext.ENGINE_SCOPE) != null) {
                filename = (String) ctxt.getBindings(ScriptContext.ENGINE_SCOPE).get(RhinoScriptEngine.FILENAME);
            }
            if (filename == null) {
                filename = (String) get(RhinoScriptEngine.FILENAME);
            }
            
            filename = filename == null ? "<Unknown source>" : filename;
            ret = cx.evaluateReader(scope, preProcessScriptSource(reader), filename , 1,  null);
        } catch (JavaScriptException jse) {
            if (DEBUG) jse.printStackTrace();
            int line = (line = jse.lineNumber()) == 0 ? -1 : line;
            Object value = jse.getValue();
            String str = (value != null && value.getClass().getName().equals("org.mozilla.javascript.NativeError") ?
                          value.toString() :
                          jse.toString());
            throw new ExtendedScriptException(jse, str, jse.sourceName(), line);
        } catch (RhinoException re) {
            if (DEBUG) re.printStackTrace();
            int line = (line = re.lineNumber()) == 0 ? -1 : line;
            throw new ExtendedScriptException(re, re.toString(), re.sourceName(), line);
        } catch (IOException ee) {
            throw new ScriptException(ee);
        } finally {
            Context.exit();
        }
        
        return unwrapReturnValue(ret);
    }
    
    @Override
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("null script");
        }
        return eval(new StringReader(script), ctxt);
    }
    
    @Override
    public ScriptEngineFactory getFactory() {
        if (factory != null) {
            return factory;
        } else {
            return new RhinoScriptEngineFactory();
        }
    }
    
    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }
    
    //Invocable methods
    @Override
    public Object invokeFunction(String name, Object... args)
    throws ScriptException, NoSuchMethodException {
        return invokeMethod(null, name, args);
    }
    
    @Override
    public Object invokeMethod(Object thiz, String name, Object... args)
    throws ScriptException, NoSuchMethodException {
        
        Context cx = enterContext();
        try {
            if (name == null) {
                throw new NullPointerException("method name is null");
            }

            if (thiz != null && !(thiz instanceof Scriptable)) {
                thiz = Context.toObject(thiz, topLevel);
            }
            
            Scriptable engineScope = getRuntimeScope(context);
            Scriptable localScope = (thiz != null)? (Scriptable) thiz :
                                                    engineScope;
            Object obj = ScriptableObject.getProperty(localScope, name);
            if (! (obj instanceof Function)) {
                throw new NoSuchMethodException("no such method: " + name);
            }

            Function func = (Function) obj;
            Scriptable scope = func.getParentScope();
            if (scope == null) {
                scope = engineScope;
            }
            Object result = func.call(cx, scope, localScope, 
                                      wrapArguments(args));
            return unwrapReturnValue(result);
        } catch (JavaScriptException jse) {
            if (DEBUG) jse.printStackTrace();
            int line = (line = jse.lineNumber()) == 0 ? -1 : line;
            Object value = jse.getValue();
            String str = (value != null && value.getClass().getName().equals("org.mozilla.javascript.NativeError") ?
                          value.toString() :
                          jse.toString());
            throw new ExtendedScriptException(jse, str, jse.sourceName(), line);
        } catch (RhinoException re) {
            if (DEBUG) re.printStackTrace();
            int line = (line = re.lineNumber()) == 0 ? -1 : line;
            throw new ExtendedScriptException(re, re.toString(), re.sourceName(), line);
        } finally {
            Context.exit();
        }
    }
   
    @Override
    public <T> T getInterface(Class<T> clasz) {
        try {
            return implementor.getInterface(null, clasz);
        } catch (ScriptException e) {
            return null;
        }
    }
    
    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        if (thiz == null) {
            throw new IllegalArgumentException("script object can not be null");
        }

        try {
            return implementor.getInterface(thiz, clasz);
        } catch (ScriptException e) {
            return null;
        }
    }

    // RRC - not used
    // LK - make it used again and modified to conform to the JVM version.
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
    
    Scriptable getRuntimeScope(ScriptContext ctxt) {
        if (ctxt == null) {
            throw new NullPointerException("null script context");
        }

        // we create a scope for the given ScriptContext
        Scriptable newScope = new ExternalScriptable(ctxt, indexedProps);

        // Set the prototype of newScope to be 'topLevel' so that
        // JavaScript standard objects are visible from the scope.
        newScope.setPrototype(topLevel);

        // define "context" variable in the new scope
        newScope.put("context", newScope, ctxt);
       
        
        // RRC - save some time and don't define print
        // LK - these functions are assumed by a lot of code so let's
        //      make them available
        // define "print" function in the new scope
        Context cx = enterContext();
        try {
            cx.evaluateString(newScope, printSource, "print", 1, null);
            requireBuilder.createRequire(cx, newScope).install(newScope);
        } finally {
            Context.exit();
        }
        
        return newScope;
    }
    
    
    //Compilable methods
    @Override
    public CompiledScript compile(String script) throws ScriptException {
        return compile(new StringReader(script));
    }
    
    @Override
    public CompiledScript compile(java.io.Reader script) throws ScriptException {
        CompiledScript ret = null;
        Context cx = enterContext();
        
        try {
            String filename = (String) get(RhinoScriptEngine.FILENAME);
            if (filename == null) {
                filename = "<Unknown Source>";
            }
            
            Script scr = cx.compileReader(preProcessScriptSource(script), filename, 1, null);
            ret = new RhinoCompiledScript(this, scr);
        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        return ret;
    }
    
    
    //package-private helpers

    static Context enterContext() {
        // call this always so that initializer of this class runs
        // and initializes custom wrap factory and class shutter.
        return Context.enter();
    }

    void setEngineFactory(ScriptEngineFactory fac) {
        factory = fac;
    }

    Object[] wrapArguments(Object[] args) {
        if (args == null) {
            return Context.emptyArgs;
        }
        Object[] res = new Object[args.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = Context.javaToJS(args[i], topLevel);
        }
        return res;
    }
    
    Object unwrapReturnValue(Object result) {
        if (result instanceof Wrapper) {
            result = ( (Wrapper) result).unwrap();
        }
        
        return result instanceof Undefined ? null : result;
    }
    
    protected Reader preProcessScriptSource(Reader reader) throws ScriptException {
        return reader;
    }

    protected void processAllTopLevelScripts(Context cx) {
        processTopLevelScript(TOPLEVEL_SCRIPT_NAME, cx);
    }

    protected void processTopLevelScript(String scriptName, Context cx) {    
        InputStream toplevelScript = this.getClass().getClassLoader().getResourceAsStream(scriptName);
        if (toplevelScript != null) {
            Reader reader = new InputStreamReader(toplevelScript);
            try {
                cx.evaluateReader(topLevel, reader, scriptName, 1, null);
            }
            catch (Exception e) {
                if (DEBUG) e.printStackTrace();
            }
            finally {
                try {
                    toplevelScript.close();
                }
                catch (IOException e) {
                }
            }
        }
    }
        
    /**
     * The bindings function takes a JavaScript scope object 
     * of type ExternalScriptable and returns the underlying Bindings
     * instance.
     *
     *    var page = scope(pageBindings);
     *    with (page) {
     *       // code that uses page scope 
     *    } 
     *    var b = bindings(page);
     *    // operate on bindings here.
     */
    public static Object bindings(Context cx, Scriptable thisObj, Object[] args,
            Function funObj) {
        if (args.length == 1) {
            Object arg = args[0];
            if (arg instanceof Wrapper) {
                arg = ((Wrapper)arg).unwrap();
            }
            if (arg instanceof ExternalScriptable) {
                ScriptContext ctx = ((ExternalScriptable)arg).getContext();
                Bindings bind = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
                return Context.javaToJS(bind, 
                           ScriptableObject.getTopLevelScope(thisObj));
            }
        }
        return Context.getUndefinedValue();
    }
   
    /** 
     * The scope function creates a new JavaScript scope object 
     * with given Bindings object as backing store. This can be used
     * to create a script scope based on arbitrary Bindings instance.
     * For example, in webapp scenario, a 'page' level Bindings instance
     * may be wrapped as a scope and code can be run in JavaScripe 'with'
     * statement:
     *
     *    var page = scope(pageBindings);
     *    with (page) {
     *       // code that uses page scope 
     *    } 
     */
    public static Object scope(Context cx, Scriptable thisObj, Object[] args,
            Function funObj) {
        if (args.length == 1) {
            Object arg = args[0];
            if (arg instanceof Wrapper) {
                arg = ((Wrapper)arg).unwrap();
            }
            if (arg instanceof Bindings) {
                ScriptContext ctx = new SimpleScriptContext();
                ctx.setBindings((Bindings)arg, ScriptContext.ENGINE_SCOPE);
                Scriptable res = new ExternalScriptable(ctx);
                res.setPrototype(ScriptableObject.getObjectPrototype(thisObj));
                res.setParentScope(ScriptableObject.getTopLevelScope(thisObj));
                return res;
            }
        }
        return Context.getUndefinedValue();
    }
 
    /**
     * The sync function creates a synchronized function (in the sense
     * of a Java synchronized method) from an existing function. The
     * new function synchronizes on the <code>this</code> object of
     * its invocation.
     * js> var o = { f : sync(function(x) {
     *       print("entry");
     *       Packages.java.lang.Thread.sleep(x*1000);
     *       print("exit");
     *     })};
     * js> thread(function() {o.f(5);});
     * entry
     * js> thread(function() {o.f(5);});
     * js>
     * exit
     * entry
     * exit
     */
    public static Object sync(Context cx, Scriptable thisObj, Object[] args,
            Function funObj) {
        if (args.length == 1 && args[0] instanceof Function) {
            return new Synchronizer((Function)args[0]);
        } else {
            throw Context.reportRuntimeError("wrong argument(s) for sync");
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("No file specified");
            return;
        }
        
        InputStreamReader r = new InputStreamReader(new FileInputStream(args[0]));
        RhinoScriptEngine engine = new RhinoScriptEngine();
        
        SimpleScriptContext context = new SimpleScriptContext();
        engine.put(RhinoScriptEngine.FILENAME, args[0]);
        engine.eval(r, context);
        // added this statement to save some typing to most script authors
        context.getWriter().flush();
    }
}

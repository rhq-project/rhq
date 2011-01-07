/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.client.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.export.Exporter;
import org.rhq.bindings.output.TabularWriter;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.Controller;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.client.proxy.ResourceClientFactory;
import org.rhq.enterprise.client.proxy.ConfigurationEditor;
import org.rhq.enterprise.client.script.CLIScriptException;
import org.rhq.enterprise.client.script.CmdLineParser;
import org.rhq.enterprise.client.script.CommandLineParseException;
import org.rhq.enterprise.client.script.NamedScriptArg;
import org.rhq.enterprise.client.script.ScriptArg;
import org.rhq.enterprise.client.script.ScriptCmdLine;
import org.rhq.enterprise.client.utility.ScriptAssert;
import org.rhq.enterprise.client.utility.ScriptUtil;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ScriptCommand implements ClientCommand {

    private ScriptEngineManager sem;
    private ScriptEngine jsEngine;

    private final Log log = LogFactory.getLog(ScriptCommand.class);

    private StringBuilder script = new StringBuilder();

    private boolean isMultilineScript = false;
    private boolean inMultilineScript = false;

    public ScriptCommand() {
        sem = new ScriptEngineManager();
        PageControl pc = new PageControl();
        pc.setPageNumber(-1);
        sem.getBindings().put("unlimitedPC", pc);
        sem.getBindings().put("pageControl", PageControl.getUnlimitedInstance());
        sem.put("exporter", new Exporter());
        jsEngine = sem.getEngineByName("JavaScript");
        try {
            jsEngine.eval("1+1");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        importRecursive(jsEngine);
    }

    private void importRecursive(ScriptEngine jsEngine) {

        try {

            List<String> packages = new PackageFinder().findPackages("org.rhq.core.domain");

            for (String pkg : packages) {
                jsEngine.eval("importPackage(" + pkg + ")");
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }

    }

    public ScriptEngine getScriptEngine() {
        return jsEngine;
    }

    public String getPromptCommandString() {
        return "exec";
    }

    public boolean execute(ClientMain client, String[] args) {
        initBindings(client);

        if (isScriptFileCommandLine(args)) {
            try {
                CmdLineParser cmdLineParser = new CmdLineParser();
                ScriptCmdLine scriptCmdLine = cmdLineParser.parse(args);

                bindScriptArgs(scriptCmdLine);
                executeUtilScripts();

                return executeScriptFile(new FileReader(scriptCmdLine.getScriptFileName()), client);
            } catch (FileNotFoundException e) {
                client.getPrintWriter().println(e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Unable to locate script file: " + e.getMessage());
                }
            } catch (CommandLineParseException e) {
                if (client.isInteractiveMode()) {
                    client.getPrintWriter().println("parse error: " + e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("A parse error occurred.", e);
                    }
                }
                else {
                    throw new CLIScriptException(e);
                }
            }

            return true;
        }

        isMultilineScript = "\\".equals(args[args.length - 1]);
        inMultilineScript = inMultilineScript || isMultilineScript;

        if (!isMultilineScript && !inMultilineScript) {
            script = new StringBuilder();
        }

        if (isMultilineScript) {
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }

        for (int i = ("exec".equals(args[0]) ? 1 : 0); i < args.length; i++) {
            script.append(args[i]);
            script.append(" ");
        }

        if (isMultilineScript) {
            return true;
        }

        try {

            Object result = jsEngine.eval(script.toString());
            inMultilineScript = false;
            script = new StringBuilder();
            if (result != null) {
                //                client.getPrintWriter().print("result: ");
                TabularWriter writer = new TabularWriter(client.getPrintWriter());

                if (client.isInteractiveMode()) {
                    writer.setWidth(client.getConsoleWidth());
                }
                writer.print(result);
            }
        } catch (ScriptException e) {

            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            message = message.replace("sun.org.mozilla.javascript.internal.EcmaError: ","");
            message = message.replace("(<Unknown source>#1) in <Unknown source> at line number 1","");

            client.getPrintWriter().println(message);
            client.getPrintWriter().println(script);
            for (int i = 0; i < e.getColumnNumber(); i++) {
                client.getPrintWriter().print(" ");
            }
            client.getPrintWriter().println("^");
            script = new StringBuilder();
            inMultilineScript = false;
        }
        client.getPrintWriter().println();
        return true;
    }

    public void initBindings(ClientMain client) {
        // These are prepared on every call in case the user logs out and logs into another server
        if (client.getSubject() != null) {
            jsEngine.put("subject", client.getSubject());
            sem.getBindings().putAll(client.getRemoteClient().getManagers());
        }
        TabularWriter tw = new TabularWriter(client.getPrintWriter());
        tw.setWidth(client.getConsoleWidth());
        sem.getBindings().put("pretty", tw);

        sem.getBindings().put("ProxyFactory", new ResourceClientFactory(client));

        bindObjectAndGlobalFuctions(new Controller(client), "rhq");
        bindObjectAndGlobalFuctions(new ScriptUtil(client), "scriptUtil");
        bindObjectAndGlobalFuctions(new ConfigurationEditor(client), "configurationEditor");
        bindObjectAndGlobalFuctions(new ScriptAssert(jsEngine), "Assert");
    }

    private void bindObjectAndGlobalFuctions(Object object, String bindingName) {
        jsEngine.put(bindingName, object);
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass(),Object.class);
            MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();

            for (MethodDescriptor methodDescriptor : methodDescriptors) {
                Method method = methodDescriptor.getMethod();
                String methodName = method.getName();
                int argCount = method.getParameterTypes().length;

                StringBuilder functionBuilder = new StringBuilder();
                functionBuilder.append(methodName).append("(");
                for (int i = 0; i < argCount; ++i) {
                    if (i != 0) {
                        functionBuilder.append(", ");
                    }
                    functionBuilder.append("arg_" + i);
                }
                functionBuilder.append(")");
                String functionFragment = functionBuilder.toString();
                boolean returnsVoid = method.getReturnType().equals(Void.TYPE);

                String functionDefinition = "function " + functionFragment + " { " + (returnsVoid ? "" : "return ")
                    + bindingName + "." + functionFragment + "; }";

                log.info("Binding global function --> " + functionDefinition);
                try {
                    jsEngine.eval(functionDefinition);
                } catch (ScriptException e) {
                    log.warn("Unable to bind global function " + functionFragment, e);
                }
            }
        } catch (IntrospectionException e) {
            // TODO Should we altogether remove the object from the script engine bindings?
            log.warn("Could not bind " + object.getClass().getName() + " into script engine.");
        }
    }

    private void executeUtilScripts() {
        InputStream stream = getClass().getResourceAsStream("test_utils.js");
        InputStreamReader reader = new InputStreamReader(stream);

        try {
            jsEngine.eval(reader);
        } catch (ScriptException e) {
            log.warn("An error occurred while executing test_utils.js", e);
        }
    }

    private boolean isScriptFileCommandLine(String[] args) {
        if (args == null || args.length < 3) {
            return false;
        }

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-f")) {
                return true;
            }
        }

        return false;

    }

    private void bindScriptArgs(ScriptCmdLine cmdLine) {
        bindArgsArray(cmdLine);

        if (cmdLine.getArgType() == ScriptCmdLine.ArgType.NAMED) {
            bindNamedArgs(cmdLine);
        }

        jsEngine.put("script", new File(cmdLine.getScriptFileName()).getName());
    }

    private void bindArgsArray(ScriptCmdLine cmdLine) {
        String[] args = new String[cmdLine.getArgs().size()];
        int i = 0;

        for (ScriptArg arg : cmdLine.getArgs()) {
            args[i++] = arg.getValue();
        }

        jsEngine.put("args", args);
    }

    private void bindNamedArgs(ScriptCmdLine cmdLine) {
        for (ScriptArg arg : cmdLine.getArgs()) {
            NamedScriptArg namedArg = (NamedScriptArg) arg;
            jsEngine.put(namedArg.getName(), namedArg.getValue());
        }
    }

    private boolean executeScriptFile(Reader reader, ClientMain client) {
        try {
            Object result = jsEngine.eval(reader);
            if (result != null) {
                if (client.isInteractiveMode()) {
                    new TabularWriter(client.getPrintWriter()).print(result);
                }
            }
        } catch (ScriptException e) {
            if (client.isInteractiveMode()) {
                client.getPrintWriter().println(e.getMessage());
                client.getPrintWriter().println("^");
            }
            else {
                throw new CLIScriptException(e);
            }
        }
        return true;
    }

    public String getSyntax() {
        return "exec <statement> | [-s<indexed|named>] -f <file> [args]";
    }

    public String getHelp() {
        return "Execute a statement or a script";
    }

    public String getDetailedHelp() {
        return "Execute a statement or a script. The following services managers are available: " +
                RemoteClient.Manager.values();
    }

    public ScriptContext getContext() {
        return jsEngine.getContext();
    }

}

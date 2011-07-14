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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.client.RhqManagers;
import org.rhq.bindings.output.TabularWriter;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.Controller;
import org.rhq.enterprise.client.proxy.ConfigurationEditor;
import org.rhq.enterprise.client.proxy.EditableResourceClientFactory;
import org.rhq.enterprise.client.script.CLIScriptException;
import org.rhq.enterprise.client.script.CmdLineParser;
import org.rhq.enterprise.client.script.CommandLineParseException;
import org.rhq.enterprise.client.script.NamedScriptArg;
import org.rhq.enterprise.client.script.ScriptArg;
import org.rhq.enterprise.client.script.ScriptCmdLine;

/**
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class ScriptCommand implements ClientCommand {

    private ScriptEngine jsEngine;
    private StandardBindings bindings;
    
    private final Log log = LogFactory.getLog(ScriptCommand.class);

    private StringBuilder script = new StringBuilder();

    private boolean isMultilineScript = false;
    private boolean inMultilineScript = false;

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
        if (jsEngine == null) {
            bindings = new StandardBindings(client.getPrintWriter(), client.getRemoteClient());
            
            try {
                jsEngine = ScriptEngineFactory.getScriptEngine("JavaScript", new PackageFinder(Arrays.asList(getLibDir())), bindings);
                jsEngine.eval("1+1");
            } catch (ScriptException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        bindings.getSubject().setValue(client.getSubject());
        bindings.getPretty().getValue().setWidth(client.getConsoleWidth());
        bindings.getProxyFactory().setValue(new EditableResourceClientFactory(client));
        
        //non-standard bindings        
        bindings.put("configurationEditor", new ConfigurationEditor(client));
        bindings.put("rhq", new Controller(client));

        ScriptEngineFactory.injectStandardBindings(jsEngine, bindings, false);
        
        ScriptEngineFactory.bindIndirectionMethods(jsEngine, "configurationEditor", bindings.get("configurationEditor"));
        ScriptEngineFactory.bindIndirectionMethods(jsEngine, "rhq", bindings.get("rhq"));                
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
                RhqManagers.values();
    }

    public ScriptContext getContext() {
        return jsEngine.getContext();
    }

    private File getLibDir() {
        String cwd = System.getProperty("user.dir");
        return new File(cwd, "lib");
    }
}

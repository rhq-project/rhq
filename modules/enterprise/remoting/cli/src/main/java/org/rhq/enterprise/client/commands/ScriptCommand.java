/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
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

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.client.RhqManager;
import org.rhq.bindings.output.TabularWriter;
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
import org.rhq.scripting.ScriptSourceProvider;
import org.rhq.scripting.ScriptSourceProviderFactory;

/**
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class ScriptCommand implements ClientCommand {

    private StandardBindings bindings;

    private final Log log = LogFactory.getLog(ScriptCommand.class);

    private StringBuilder script = new StringBuilder();

    private boolean isMultilineScript = false;
    private boolean inMultilineScript = false;

    public String getPromptCommandString() {
        return "exec";
    }

    public boolean execute(ClientMain client, String[] args) {
        // for a command line session we don't want to reset the bindings for each executed command line, the
        // state, e.g. exporter settings, should be maintained from line to line. Note that scriptFiles
        // executed via 'exec -f' are treated like extensions of the command line session.  They inherit the
        // current bindings and any modifications made by the script file will affect the command line session
        // after the script file has completed.
        if (null == bindings) {
            initBindings(client);
        }

        if (isScriptFileCommandLine(args)) {
            try {
                CmdLineParser cmdLineParser = new CmdLineParser();
                ScriptCmdLine scriptCmdLine = cmdLineParser.parse(args);

                bindScriptArgs(client, scriptCmdLine);
                executeUtilScripts(client);

                FileReader reader = new FileReader(scriptCmdLine.getScriptFileName());
                try {
                    return executeScriptFile(reader, client);
                } finally {
                    try {
                        reader.close();
                    } catch (IOException ignore) {
                    }
                }
            } catch (FileNotFoundException e) {
                if (client.isInteractiveMode()) {
                    client.getPrintWriter().println(e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("Unable to locate script file: " + e.getMessage());
                    }
                } else {
                    throw new CLIScriptException(e);
                }
            } catch (CommandLineParseException e) {
                if (client.isInteractiveMode()) {
                    client.getPrintWriter().println("parse error: " + e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("A parse error occurred.", e);
                    }
                } else {
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

            Object result = client.getScriptEngine().eval(script.toString());
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

            if (client.isInteractiveMode()) {
            String message = client.getUsefulErrorMessage(e);

                client.getPrintWriter().println(message);
                client.getPrintWriter().println(script);
                for (int i = 0; i < e.getColumnNumber(); i++) {
                    client.getPrintWriter().print(" ");
                }
                client.getPrintWriter().println("^");
                script = new StringBuilder();
                inMultilineScript = false;
            } else {
                throw new CLIScriptException(e);
            }
        }
        client.getPrintWriter().println();
        return true;
    }

    private void initBindings(ClientMain client) {
        bindings = new StandardBindings(client.getPrintWriter(), client.getRemoteClient());
        // init pretty width with console setting
        bindings.getPretty().getValue().setWidth(client.getConsoleWidth());
        // replace ResourceClientFactory with Editable version or none at all
        if (client.getRemoteClient() != null) {
            bindings.getProxyFactory().setValue(new EditableResourceClientFactory(client));
        } else {
            bindings.getProxyFactory().setValue(null);
        }

        //non-standard bindings for console
        bindings.put("configurationEditor", new ConfigurationEditor(client));
        bindings.put("rhq", new Controller(client));

        ScriptEngine engine = client.getScriptEngine();

        ScriptSourceProvider[] sourceProviders = ScriptSourceProviderFactory.get(null);

        ScriptEngineFactory.injectStandardBindings(engine, bindings, false, sourceProviders);

        ScriptEngineFactory.bindIndirectionMethods(engine, "configurationEditor");
        ScriptEngineFactory.bindIndirectionMethods(engine, "rhq");
    }

    public void initClient(ClientMain client) {
        if (null == bindings) {
            initBindings(client);

        } else {
            ScriptEngine engine = client.getScriptEngine();

            // remove any current manager bindings from the engine, they may not be valid for the
            // new client. The new standard bindings will include any new managers.
            ScriptEngineFactory.removeBindings(engine, bindings.getManagers().keySet());

            bindings.setFacade(client.getPrintWriter(), client.getRemoteClient());

            // update the engine with the new client bindings. Keep the existing engine bindings as they
            // may contain bindings outside this standard set (like any var created by the script or command line user)
            ScriptEngineFactory.injectStandardBindings(engine, bindings, false, ScriptSourceProviderFactory.get(null));
        }

        return;
    }

    private void executeUtilScripts(ClientMain client) {
        InputStream stream = getClass().getResourceAsStream("test_utils.js");
        InputStreamReader reader = new InputStreamReader(stream);

        try {
            client.getScriptEngine().eval(reader);
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

    private void bindScriptArgs(ClientMain client, ScriptCmdLine cmdLine) {
        bindArgsArray(client, cmdLine);

        if (cmdLine.getArgType() == ScriptCmdLine.ArgType.NAMED) {
            bindNamedArgs(client, cmdLine);
        }

        client.getScriptEngine().put("script", new File(cmdLine.getScriptFileName()).getName());
    }

    private void bindArgsArray(ClientMain client, ScriptCmdLine cmdLine) {
        String[] args = new String[cmdLine.getArgs().size()];
        int i = 0;

        for (ScriptArg arg : cmdLine.getArgs()) {
            args[i++] = arg.getValue();
        }

        client.getScriptEngine().put("args", args);
    }

    private void bindNamedArgs(ClientMain client, ScriptCmdLine cmdLine) {
        for (ScriptArg arg : cmdLine.getArgs()) {
            NamedScriptArg namedArg = (NamedScriptArg) arg;
            client.getScriptEngine().put(namedArg.getName(), namedArg.getValue());
        }
    }

    private boolean executeScriptFile( Reader reader, ClientMain client) {
        try {
            Object result = client.getScriptEngine().eval(reader);
            if (result != null) {
                if (client.isInteractiveMode()) {
                    new TabularWriter(client.getPrintWriter()).print(result);
                }
            }
        } catch (ScriptException e) {
            if (client.isInteractiveMode()) {
                client.getPrintWriter().println(e.getMessage());
                client.getPrintWriter().println("^");
            } else {
                throw new CLIScriptException(e);
            }
        }
        return true;
    }

    public String getSyntax() {
        return getPromptCommandString() + " <statement> | [-s<indexed|named>] -f <file> [args]";
    }

    public String getHelp() {
        return "Execute a statement or a script";
    }

    public String getDetailedHelp() {
        return "Execute a statement or a script. The following service managers are available: "
            + Arrays.toString(RhqManager.values());
    }
}

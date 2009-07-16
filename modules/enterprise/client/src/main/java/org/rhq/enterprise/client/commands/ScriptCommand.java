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
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.client.TabularWriter;
import org.rhq.enterprise.client.utility.PackageFinder;
import org.rhq.enterprise.client.utility.ScriptUtil;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ScriptCommand implements ClientCommand {

    private ScriptEngineManager sem;
    private ScriptEngine jsEngine;

    private final Log log = LogFactory.getLog(ScriptCommand.class);

    public ScriptCommand() {
        sem = new ScriptEngineManager();
        sem.getBindings().put("pageControl", PageControl.getUnlimitedInstance());
        jsEngine = sem.getEngineByName("JavaScript");

        importRecursive(jsEngine);

        // jsEngine = sem.getEngineByName("groovy");
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

        // check to see if user logged in
        if (client.getSubject() == null) {
            client.getPrintWriter().println("Unable to execute scripts until successfully logged in.");
            return true;
        }

        // These are prepared on every call in case the user logs out and logs into another server
        Bindings bindings = sem.getBindings();

        initBindings(client);

        if (isScriptFileCommandLine(args)) {
            try {
                String scriptName = args[2].trim();
                addScriptArgsToBindings(args);
                return executeScriptFile(new FileReader(scriptName), client);
            } catch (FileNotFoundException e) {
                client.getPrintWriter().println(e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Unable to locate script file: " + e.getMessage());
                }
            } catch (IOException e) {
                client.getPrintWriter().println("An unpexected error occurred: " + e.getMessage());
                log.warn("An unexpected IO error occurred.", e);
            } catch (Exception e) {
                client.getPrintWriter().println("An unpexected error occurred: " + e.getMessage());
                log.warn("An unexpected IO error occurred.", e);
            }

            return true;
        }

        StringBuilder script = new StringBuilder();
        for (int i = ("exec".equals(args[0]) ? 1 : 0); i < args.length; i++) {
            script.append(args[i]);
            script.append(" ");
        }
        try {

            Object result = jsEngine.eval(script.toString());
            if (result != null) {
                client.getPrintWriter().print("result: ");
                new TabularWriter(client.getPrintWriter()).print(result);
            }
        } catch (ScriptException e) {
            client.getPrintWriter().println(e.getMessage());
            client.getPrintWriter().println(script);
            for (int i = 0; i < e.getColumnNumber(); i++) {
                client.getPrintWriter().print(" ");
            }
            client.getPrintWriter().println("^");
        }
        client.getPrintWriter().println();
        return true;
    }

    public void initBindings(ClientMain client) {
        // These are prepared on every call in case the user logs out and logs into another server
        sem.getBindings().put("subject", client.getSubject());
        sem.getBindings().putAll(client.getRemoteClient().getManagers());
        TabularWriter tw = new TabularWriter(client.getPrintWriter());
        tw.setWidth(client.getConsoleWidth());
        sem.getBindings().put("pretty", tw);

        bindScriptUtils();
    }

    private void bindScriptUtils() {
        ScriptUtil scriptUtil = new ScriptUtil(jsEngine);
        jsEngine.put("scriptUtil", scriptUtil);

        String func = "function isDefined(identifier) { return scriptUtil.isDefined(identifier); }";
        try {
            jsEngine.eval(func);
        } catch (ScriptException e) {
            log.warn("Unable to bind script utility function isDefined()", e);
        }
    }

    private boolean isScriptFileCommandLine(String[] args) {
        return args != null && args.length > 2 && args[1].trim().equalsIgnoreCase("-f");
    }

    private void addScriptArgsToBindings(String[] args) {
        if (args.length < 4) {
            return;
        }

        Bindings bindings = jsEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        String[] scriptArgs = Arrays.copyOfRange(args, 3, args.length);
        bindings.put("args", scriptArgs);
    }

    private boolean executeScriptFile(Reader reader, ClientMain client) {
        try {
            Object result = jsEngine.eval(reader);
            if (result != null) {
                client.getPrintWriter().print("result: ");
                new TabularWriter(client.getPrintWriter()).print(result);
            }
        }
        catch (ScriptException e) {
            client.getPrintWriter().println(e.getMessage());
            client.getPrintWriter().println("^");
        }
        return true;
    }

    public String getSyntax() {
        String example = "Ex.  exec 2 + 2";
        example += "\n" + "Ex2. exec -f (path to file)";
        String syntax = "exec <scripting code>";

        return example + "\n" + syntax;

    }

    public String getHelp() {
        return "Excecutes JavaScript commands using the various service interfaces";
    }

    public String getDetailedHelp() {
        StringBuilder help = new StringBuilder();
        help.append("Executes JavaScript commands. You can utilize the following service managers: "
                + RemoteClient.Manager.values());
        return help.toString();
    }


    public ScriptContext getContext() {
        return jsEngine.getContext();
    }
}

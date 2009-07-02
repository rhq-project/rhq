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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.client.TabularWriter;

/**
 * @author Greg Hinkle
 */
public class ScriptCommand implements ClientCommand {

    private ScriptEngineManager sem;
    private ScriptEngine jsEngine;

    public ScriptCommand() {
        sem = new ScriptEngineManager();
        // sem.getBindings().put("unlimitedPC", PageControl.getUnlimitedInstance());
        PageControl pc = new PageControl();
        pc.setPageNumber(-1);
        sem.getBindings().put("unlimitedPC", pc);
        jsEngine = sem.getEngineByName("JavaScript");
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
        sem.getBindings().put("subject", client.getSubject());
        sem.getBindings().putAll(client.getRemoteClient().getManagers());
        TabularWriter tw = new TabularWriter(client.getPrintWriter());
        tw.setWidth(client.getConsoleWidth());
        sem.getBindings().put("pretty", tw);

        // parse arg 1 for -f
        if ((args != null) && (args.length == 3)) {
            if ((args[1].trim().equalsIgnoreCase("-f")) && (args[2].trim().length() > 0)) {
                try {
                    BufferedReader ir = new BufferedReader(new FileReader(args[2]));
                    Object result = null;
                    String line = null;
                    // iterate through the lines of script and execute
                    boolean can_continue = true;
                    while ((ir != null) && ((line = ir.readLine()) != null) && (line.trim().length() > 0)
                        && can_continue) {
                        // parse the command into separate arguments and execute
                        String[] cmd_args = client.parseCommandLine(line);
                        can_continue = client.executePromptCommand(cmd_args);
                    }
                    if (result != null) {
                        client.getPrintWriter().print("result: ");
                        client.getPrintWriter().print(result);
                    }
                    return true;
                    // TODO: figure out what to do with exceptions.. too ugly, nicer info and log details?
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (ScriptException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // stop execution after custom script run/loaded
                return true;
            }
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
}

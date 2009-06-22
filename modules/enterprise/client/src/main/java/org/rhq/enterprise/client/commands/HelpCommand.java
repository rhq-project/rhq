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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.TabularWriter;

/**
 * @author Greg Hinkle
 */
public class HelpCommand implements ClientCommand {

    public String getPromptCommandString() {
        return "help";
    }

    public boolean execute(ClientMain client, String[] args) {
        Map<String, ClientCommand> commands = client.getCommands();
        if (args.length == 1) {
            String[][] data = new String[commands.size()][2];
            int i = 0;
            List<String> cmds = new ArrayList<String>(commands.keySet());
            Collections.sort(cmds);
            for (String name : cmds) {
                ClientCommand command = commands.get(name);
                data[i][0] = name;
                data[i++][1] = command.getHelp();
            }
            TabularWriter tw = new TabularWriter(client.getPrintWriter(), "Command", "Description");
            tw.setWidth(client.getConsoleWidth());

            tw.print(data);
        } else if ("api".equals(args[1])) {
            Map<String, Object> services = client.getRemoteClient().getAllManagers();
            if (args.length == 2) {
                TabularWriter tw = new TabularWriter(client.getPrintWriter(), "API", "Package");
                tw.setWidth(client.getConsoleWidth());

                String[][] data = new String[services.size()][2];
                int i = 0;
                for (String apiName : services.keySet()) {
                    data[i][0] = apiName;
                    Object service = services.get(apiName);
                    data[i][1] = service.getClass().getInterfaces()[0].getPackage().getName();
                    i++;
                }
                tw.print(data);
            } else if (args.length == 3) {
                Object service = services.get(args[2]);
                if (service != null) {
                    Class<?> intf = service.getClass().getInterfaces()[0];
                    Method[] methods = intf.getMethods();
                    Arrays.sort(methods, new Comparator<Method>() {
                        public int compare(Method o1, Method o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
                    String[][] data = new String[methods.length][2];
                    for (int i = 0; i < methods.length; i++) {
                        data[i][0] = methods[i].getName();

                        Class<?>[] paramTypes = methods[i].getParameterTypes();
                        StringBuilder buf = new StringBuilder();

                        buf.append(methods[i].getReturnType().getSimpleName());
                        buf.append(" ");
                        buf.append(methods[i].getName());
                        buf.append("(");

                        boolean secondary = false;
                        for (Class<?> paramType : paramTypes) {
                            if (secondary)
                                buf.append(", ");
                            secondary = true;
                            buf.append(paramType.getSimpleName());
                        }
                        buf.append(")");
                        data[i][1] = buf.toString();
                    }

                    TabularWriter tw = new TabularWriter(client.getPrintWriter(), "Method", "Signature");
                    tw.setWidth(client.getConsoleWidth());

                    tw.print(data);
                } else {
                    client.getPrintWriter().println(
                        "Unknown service [" + args[2] + "] try 'help api' for a listing of services");
                }

            }
        } else {
            ClientCommand cmd = commands.get(args[1]);
            if (cmd == null) {
                client.getPrintWriter().println("Uknown command [" + args[1] + "]");
            } else {
                client.getPrintWriter().println("Help [" + args[1] + "]");
                client.getPrintWriter().println("Syntax [" + cmd.getSyntax() + "]");
                client.getPrintWriter().println(cmd.getDetailedHelp());
            }
        }

        return true;
    }

    public String getSyntax() {
        return "help [command] | [api [service]]";
    }

    public String getHelp() {
        return "Help on the client and its commands";
    }

    public String getDetailedHelp() {
        return "Use help [command] to get detailed help\n"
            + "help api will return the list of service apis available for script execs\n"
            + "help api [service] will display the methods and signatures of a specific api";
    }
}
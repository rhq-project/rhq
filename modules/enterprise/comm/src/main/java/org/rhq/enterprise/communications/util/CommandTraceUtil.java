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
package org.rhq.enterprise.communications.util;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommand;
import org.rhq.enterprise.communications.command.impl.stream.RemoteInputStreamCommand;
import org.rhq.enterprise.communications.command.impl.stream.RemoteOutputStreamCommand;

/**
 * Static utility methods useful when logging commands, used mainly for debugging a client and remote endpoint.
 *
 * @author John Mazzitelli
 */
public class CommandTraceUtil {

    // name of system property that, when set to anything, dumps the configuration properties of all commands
    private static final String SYSPROP_TRACE_COMMAND_CONFIG = "rhq.trace-command-config";

    // name of system property that, when set to anything, dumps the results of all command responses
    private static final String SYSPROP_TRACE_COMMAND_RESPONSE_RESULTS = "rhq.trace-command-response-results";

    // just a simple cache so we can avoid having to parse the interface names all the time
    private static Map<String, String> interfaceSimpleNames = null;

    /**
     * Returns the string that represents the command configuration.
     * 
     * @param command the command whose configuration is stringified and returned
     * 
     * @return the stringified configuration
     */
    public static String getConfigString(Command command) {
        Properties config = null;
        if (command != null && System.getProperty(SYSPROP_TRACE_COMMAND_CONFIG) != null) {
            config = command.getConfiguration();
        }
        return (config != null) ? config.toString() : "?";
    }

    /**
     * Returns a good string to use to log the command.  If this is a remote pojo invocation
     * command, this will return the remote pojo method being invoked; otherwise, its the
     * command's type name.
     * 
     * @param command the command to convert to a string
     * 
     * @return the command's string that should be logged
     */
    public static String getCommandString(Command command) {
        String commandString;
        if (command == null) {
            commandString = "null command";
        } else if (command.getCommandType().equals(RemotePojoInvocationCommand.COMMAND_TYPE)) {
            RemotePojoInvocationCommand remoteCmd = (RemotePojoInvocationCommand) command;
            StringBuilder fullMethod = new StringBuilder(getInterfaceSimpleName(remoteCmd.getTargetInterfaceName()));
            fullMethod.append('.');
            fullMethod.append(remoteCmd.getNameBasedInvocation().getMethodName());
            commandString = fullMethod.toString();
        } else if (command.getCommandType().equals(RemoteOutputStreamCommand.COMMAND_TYPE)) {
            RemoteOutputStreamCommand streamCmd = (RemoteOutputStreamCommand) command;
            StringBuilder fullMethod = new StringBuilder("OutputStream.");
            fullMethod.append(streamCmd.getNameBasedInvocation().getMethodName());
            fullMethod.append('.');
            fullMethod.append(streamCmd.getStreamId());
            commandString = fullMethod.toString();
        } else if (command.getCommandType().equals(RemoteInputStreamCommand.COMMAND_TYPE)) {
            RemoteInputStreamCommand streamCmd = (RemoteInputStreamCommand) command;
            StringBuilder fullMethod = new StringBuilder("InputStream.");
            fullMethod.append(streamCmd.getNameBasedInvocation().getMethodName());
            fullMethod.append('.');
            fullMethod.append(streamCmd.getStreamId());
            commandString = fullMethod.toString();
        } else {
            commandString = command.getCommandType().getName();
        }
        return commandString;
    }

    /**
     * Returns a good string to use to log the command response.
     * 
     * @param response the command response to convert to a string
     * 
     * @return the command response string that should be logged
     */
    public static String getCommandResponseString(Object response) {
        String responseString;
        if (response instanceof CommandResponse) {
            CommandResponse cmdResponse = (CommandResponse) response;
            if (cmdResponse.isSuccessful()) {
                if (System.getProperty(SYSPROP_TRACE_COMMAND_RESPONSE_RESULTS) != null) {
                    responseString = "success:" + getCommandResponseResultsString(cmdResponse.getResults());
                } else {
                    responseString = "success";
                }
            } else {
                responseString = "failed:" + ThrowableUtil.getAllMessages(cmdResponse.getException());
            }
        } else if (response instanceof Throwable) {
            responseString = ThrowableUtil.getAllMessages((Throwable) response);
        } else if (response instanceof String) {
            responseString = "STRING:" + response.toString();
        } else if (response == null) {
            responseString = "NULL";
        } else {
            responseString = "UNEXPECTED:" + response.getClass().getName();
        }
        return responseString;
    }

    /**
     * Returns a stringified form of a command response results object.  This makes it so
     * the string isn't really long and doesn't contain newlines so we can show it on a single
     * line in the log file.
     * 
     * @param results the results object from a command response
     * 
     * @return results string
     */
    private static String getCommandResponseResultsString(Object results) {

        if (results == null) {
            return "null";
        }

        StringBuilder resultsString = new StringBuilder();

        try {
            resultsString.append(results.toString());
        } catch (Throwable t) {
            return "toString failed: [" + t + "]";
        }

        int maxLength;

        try {
            // if the sys prop is a number, use it as our max length - useful if developer wants to see alot or alittle
            maxLength = Integer.parseInt(System.getProperty(SYSPROP_TRACE_COMMAND_RESPONSE_RESULTS));
        } catch (NumberFormatException e) {
            maxLength = 256;
        }

        if (resultsString.length() > maxLength) {
            resultsString.setLength(maxLength);
            resultsString.append("...");
        }

        return resultsString.toString().replaceAll("[\r\n]", ".");
    }

    /**
     * Gets the simple name of the given interface name. The simple name is the name
     * of the interface itself, minus any package names.
     * 
     * @param interfaceName the full interface name
     * 
     * @return the name of the interface class itself, without packages
     */
    private static String getInterfaceSimpleName(String interfaceName) {
        if (interfaceName == null) { // i'm paranoid
            return "unknown";
        }

        if (interfaceSimpleNames == null) {
            interfaceSimpleNames = new Hashtable<String, String>();
        }

        String simpleName = interfaceSimpleNames.get(interfaceName);

        if (simpleName == null) {
            int period = interfaceName.lastIndexOf('.');
            if (period == -1 || period == (interfaceName.length() - 1)) {
                simpleName = interfaceName;
            } else {
                simpleName = interfaceName.substring(period + 1);
            }
            interfaceSimpleNames.put(interfaceName, simpleName);
        }

        return simpleName;
    }
}
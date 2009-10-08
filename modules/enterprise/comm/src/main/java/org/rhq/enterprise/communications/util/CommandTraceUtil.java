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

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
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

    // name of system property that, when set to true, dumps the configuration properties of all commands
    private static final String SYSPROP_TRACE_COMMAND_CONFIG = "rhq.trace-command-config";

    // name of system property that, when set to a number, dumps the results of all command responses
    private static final String SYSPROP_TRACE_COMMAND_RESPONSE_RESULTS = "rhq.trace-command-response-results";

    // name of system property that, when set to a number, dumps the size of the command when larger than the prop value
    private static final String SYSPROP_TRACE_COMMAND_SIZE_THRESHOLD = "rhq.trace-command-size-threshold";

    // name of system property that, when set to a number, dumps the size of the command response when larger than the prop value
    private static final String SYSPROP_TRACE_COMMAND_RESPONSE_SIZE_THRESHOLD = "rhq.trace-command-response-size-threshold";

    // just a simple cache so we can avoid having to parse the interface names all the time
    private static Map<String, String> interfaceSimpleNames = null;

    public static Boolean getSettingTraceCommandConfig() {
        if (System.getProperty(SYSPROP_TRACE_COMMAND_CONFIG) != null) {
            return Boolean.getBoolean(SYSPROP_TRACE_COMMAND_CONFIG);
        }
        return null; // we want to return null if not set
    }

    public static void setSettingTraceCommandConfig(Boolean val) {
        System.setProperty(SYSPROP_TRACE_COMMAND_CONFIG, (val != null) ? Boolean.toString(val) : "false");
    }

    public static Integer getSettingTraceCommandResponseResults() {
        int val = getPropertyInt(SYSPROP_TRACE_COMMAND_RESPONSE_RESULTS);
        return (val != -1) ? Integer.valueOf(val) : null; // returns null if not set, numeric otherwise
    }

    public static void setSettingTraceCommandResponseResults(Integer val) {
        System.setProperty(SYSPROP_TRACE_COMMAND_RESPONSE_RESULTS, (val != null) ? Integer.toString(val) : "-1");
    }

    public static Integer getSettingTraceCommandSizeThreshold() {
        int val = getPropertyInt(SYSPROP_TRACE_COMMAND_SIZE_THRESHOLD);
        return (val != -1) ? Integer.valueOf(val) : null; // returns null if not set, numeric otherwise
    }

    public static void setSettingTraceCommandSizeThreshold(Integer val) {
        System.setProperty(SYSPROP_TRACE_COMMAND_SIZE_THRESHOLD, (val != null) ? Integer.toString(val) : "-1");
    }

    public static Integer getSettingTraceCommandResponseSizeThreshold() {
        int val = getPropertyInt(SYSPROP_TRACE_COMMAND_RESPONSE_SIZE_THRESHOLD);
        return (val != -1) ? Integer.valueOf(val) : null; // returns null if not set, numeric otherwise
    }

    public static void setSettingTraceCommandResponseSizeThreshold(Integer val) {
        System.setProperty(SYSPROP_TRACE_COMMAND_RESPONSE_SIZE_THRESHOLD, (val != null) ? Integer.toString(val) : "-1");
    }

    /**
     * Returns the string that represents the command configuration.
     * 
     * @param command the command whose configuration is stringified and returned
     * 
     * @return the stringified configuration
     */
    public static String getConfigString(Command command) {
        Properties config = null;
        if (command != null && Boolean.getBoolean(SYSPROP_TRACE_COMMAND_CONFIG)) {
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
     * Returns the actual size, in bytes, of the given command but only if it
     * exceeds the given configured threshold (-1 is returned otherwise).
     * If size tracing threshold is disabled, this will not perform any
     * serialization and simply return -1
     * ({@link #SYSPROP_TRACE_COMMAND_SIZE_THRESHOLD} defines this threshold).
     * Note that if the command is not serializable, an exception is thrown.
     * All commands <b>must</b> be serializable; therefore, if we trace a command that is
     * not, it is appropriate for us to draw attention to it by throwing an
     * exception from this method.
     * 
     * @param command
     * 
     * @return the size of the command or -1 if not configured to perform size tracing
     * 
     * @throws NotSerializableException if failed to serialize the command
     */
    public static int getCommandSize(Command command) throws NotSerializableException {
        int threshold = getPropertyInt(SYSPROP_TRACE_COMMAND_SIZE_THRESHOLD);
        if (threshold > -1) {
            try {
                int size = StreamUtil.serialize(command).length;
                if (size > threshold) {
                    return size;
                }
            } catch (Exception e) {
                throw new NotSerializableException(ThrowableUtil.getAllMessages(e));
            }
        }
        return -1;
    }

    /**
     * Returns the actual size, in bytes, of the given response but only if it
     * exceeds the given configured threshold (-1 is returned otherwise).
     * If size tracing threshold is disabled, this will not perform any
     * serialization and simply return -1
     * (see {@link #SYSPROP_TRACE_COMMAND_RESPONSE_SIZE_THRESHOLD} defines this threshold).
     * Note that if the response is not serializable, a runtime exception is thrown.
     * All responses <b>must</b> be serializable; therefore, if we trace a response that is
     * not, it is appropriate for us to draw attention to it by throwing an
     * exception from this method.
     * 
     * @param response
     * 
     * @return the size of the response or -1 if not configured to perform size tracing
     * 
     * @throws NotSerializableException if failed to serialize the response
     */
    public static int getCommandResponseSize(Object response) throws NotSerializableException {
        int threshold = getPropertyInt(SYSPROP_TRACE_COMMAND_RESPONSE_SIZE_THRESHOLD);
        if (threshold > -1) {
            try {
                int size = StreamUtil.serialize((Serializable) response).length;
                if (size > threshold) {
                    return size;
                }
            } catch (Exception e) {
                throw new NotSerializableException(ThrowableUtil.getAllMessages(e));
            }
        }
        return -1;
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

    /**
     * Returns the given system property's value as a number. If the property
     * isn't set or is invalid, -1 is returned.
     * 
     * @param propertyName
     * 
     * @return the property's value as a number
     */
    private static int getPropertyInt(String propertyName) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            try {
                return Integer.parseInt(propertyValue, 10);
            } catch (Exception e) {
                System.setProperty(propertyName, "-1"); // invalid number, just disable it so we don't keep checking
            }
        }
        return -1;
    }
}
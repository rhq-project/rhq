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
package org.rhq.enterprise.communications.command.client;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandClient;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * A command line client that can be used to issue a command from a shell or script.
 *
 * <p>Note that using this type of client limits the types of data that can be sent to the server; specifically
 * parameters to the command. Since arguments are parsed from the command line, only data that can be represented as a
 * <code>String</code> can be passed to the server as command parameters.</p>
 *
 * @author John Mazzitelli
 */
public class CmdlineClient {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(CmdlineClient.class);

    /**
     * the name of the command to execute
     */
    private String m_commandName;

    /**
     * the version of the command to execute (default is 1)
     */
    private int m_commandVersion = 1;

    /**
     * the set of parameters to pass to the command
     */
    private Map<String, Object> m_params;

    /**
     * array of packages to search when looking for the concrete command client classes
     */
    private String[] m_packages;

    /**
     * the name of the command client class that was specifically defined by a cmdline argument
     */
    private String m_classname;

    /**
     * the remoting invoker locator URI
     */
    private String m_locatorUri;

    /**
     * the subsystem of the remoting invocation handler (aka command processor)
     */
    private String m_subsystem = JBossRemotingRemoteCommunicator.DEFAULT_SUBSYSTEM;

    /**
     * Main entry point to the command line client.
     *
     * @param args specifies the command to invoke and its parameters
     */
    public static void main(String[] args) {
        try {
            CmdlineClient client = new CmdlineClient();
            CommandResponse response = client.issueCommand(args);

            LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_RESPONSE, response);
        } catch (Throwable e) {
            String cmdline = "";
            for (int i = 0; i < args.length; i++) {
                cmdline += ("'" + args[i] + "'");
                if ((i + 1) < args.length) {
                    cmdline += " ";
                }
            }

            LOG.error(e, CommI18NResourceKeys.CMDLINE_CLIENT_EXECUTE_FAILURE, cmdline);
        }

        return;
    }

    /**
     * Simply builds and returns a command client defined by the given cmdline arguments. It does not send the command.
     *
     * <p>Given the command type information and the optional package locations where command clients can be found, this
     * will attempt to instantiate the specific client for the specific command. If the client cannot be found, an
     * attempt will be made to issue the command using the {@link GenericCommandClient generic client}.</p>
     *
     * @param  args cmdline arguments
     *
     * @return the command client
     *
     * @throws IllegalArgumentException if a failure occurred while processing the cmdline arguments
     * @throws ClassNotFoundException   if failed to find a valid command client class
     * @throws IllegalAccessException   if failed to instantiate the command's client class
     * @throws InstantiationException   if failed to instantiate the command's client class
     * @throws MalformedURLException    if the given URL is invalid and cannot be used to locate an invoker
     */
    public CommandClient buildCommandClient(String[] args) throws IllegalArgumentException, ClassNotFoundException,
        InstantiationException, IllegalAccessException, MalformedURLException {
        if (processCommandLine(args) != -1) {
            // this client cannot be used to invoke the command
            throw new IllegalArgumentException(LOG
                .getMsgString(CommI18NResourceKeys.CMDLINE_CLIENT_PROCESS_ARGS_FAILURE));
        }

        // determine the name of the command client class and instantiate a new instance of it
        CommandClient commandClient;

        try {
            Class commandClientClazz = findCommandClientClass();
            commandClient = instantiateCommandClient(commandClientClazz);
        } catch (ClassNotFoundException cnfe) {
            // can't find the command in question - assume the user knows best and just issue a generic command
            // cross your fingers and hope that the command type defined by the user is processable on the server-side
            LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_USING_GENERIC_CLIENT, cnfe.getMessage());

            try {
                GenericCommandClient customClient = new GenericCommandClient();
                customClient.setCommandType(new CommandType(m_commandName, m_commandVersion));
                commandClient = customClient;
            } catch (Exception e) {
                // any exception that occurred while trying to build a generic client should throw a ClassNotFound
                // to alert the caller that the original client class was not found and our workaround didn't work
                throw new ClassNotFoundException(cnfe.toString(), e);
            }
        }

        return commandClient;
    }

    /**
     * Simply builds and returns a command defined by the given cmdline arguments. It does not send the command.
     *
     * <p>Given the command type information and the optional package locations where command clients can be found, this
     * will attempt to instantiate the specific client for the specific command. If the client cannot be found, an
     * attempt will be made to issue the command using the {@link GenericCommandClient generic client}.</p>
     *
     * @param  args cmdline arguments
     *
     * @return the command
     *
     * @throws IllegalArgumentException if a failure occurred while processing the cmdline arguments
     * @throws ClassNotFoundException   if failed to find a valid command client class
     * @throws IllegalAccessException   if failed to instantiate the command's client class
     * @throws InstantiationException   if failed to instantiate the command's client class
     * @throws MalformedURLException    if the given URL is invalid and cannot be used to locate an invoker
     */
    public Command buildCommand(String[] args) throws IllegalArgumentException, ClassNotFoundException,
        InstantiationException, IllegalAccessException, MalformedURLException {
        CommandClient commandClient = buildCommandClient(args);

        return commandClient.createNewCommand(m_params);
    }

    /**
     * Issues a command defined by the given cmdline arguments.
     *
     * <p>Given the command type information and the optional package locations where command clients can be found, this
     * will attempt to instantiate the specific client for the specific command. If the client cannot be found, an
     * attempt will be made to issue the command using the {@link GenericCommandClient generic client}.</p>
     *
     * @param  args cmdline arguments
     *
     * @return the response of the command
     *
     * @throws IllegalArgumentException if a failure occurred while processing the cmdline arguments
     * @throws ClassNotFoundException   if failed to find a valid command client class
     * @throws IllegalAccessException   if failed to instantiate the command's client class
     * @throws InstantiationException   if failed to instantiate the command's client class
     * @throws MalformedURLException    if the given URL is invalid and cannot be used to locate an invoker
     * @throws Throwable                any other error that was due to a failure during the invocation of the command
     */
    public CommandResponse issueCommand(String[] args) throws IllegalArgumentException, ClassNotFoundException,
        InstantiationException, IllegalAccessException, MalformedURLException, Throwable {
        CommandClient commandClient = buildCommandClient(args);

        // tell the new concrete command client instance to connect to the desired remote server
        if (m_locatorUri == null) {
            throw new MalformedURLException(LOG.getMsgString(CommI18NResourceKeys.CMDLINE_CLIENT_NULL_URI));
        }

        JBossRemotingRemoteCommunicator communicator = new JBossRemotingRemoteCommunicator(m_locatorUri, m_subsystem);
        commandClient.setRemoteCommunicator(communicator);

        // tell the concrete command client instance to invoke the command on the remote server
        CommandResponse response = commandClient.invoke(m_params);

        commandClient.disconnectRemoteCommunicator();

        return response;
    }

    /**
     * Convienence method that invokes a given command type (with the given set of parameter values) on the remote
     * server found at the given URI.
     *
     * <p>Note that this method may or may not be the most appropriate to use. Use this method if you do not know ahead
     * of time the type of command you are going to issue. If you already know the type of command you are going to
     * issue, it is best to use the command's more strongly typed client subclass method that implements
     * {@link CommandClient#invoke(Command)}.</p>
     *
     * @param  commandType the type of command to issue
     * @param  locatorURI  location of the remote server
     * @param  params      set of name/value parameters sent along with the command (may be <code>null</code>)
     *
     * @return the command response
     *
     * @throws IllegalArgumentException if a failure occurred while processing the cmdline arguments
     * @throws ClassNotFoundException   if failed to find the command's client class
     * @throws IllegalAccessException   if failed to instantiate the command's client class
     * @throws InstantiationException   if failed to instantiate the command's client class
     * @throws MalformedURLException    if the given URL is invalid and cannot be used to locate an invoker
     * @throws Throwable                any other error that was due to a failure during the invocation of the command
     */
    public CommandResponse issueCommand(CommandType commandType, String locatorURI, Map<String, String> params)
        throws IllegalArgumentException, MalformedURLException, ClassNotFoundException, InstantiationException,
        IllegalAccessException, Throwable {
        ArrayList<String> args = new ArrayList<String>();

        args.add("-c");
        args.add(commandType.getName());
        args.add("-v");
        args.add("" + commandType.getVersion());
        args.add("-u");
        args.add(locatorURI);

        if (params != null) {
            for (Iterator iter = params.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object paramName = entry.getKey();
                Object paramValue = entry.getValue();
                String paramNVP = paramName.toString();
                if (paramValue != null) {
                    paramNVP += ("=" + paramValue);
                }

                args.add(paramNVP);
            }
        }

        return issueCommand(args.toArray(new String[args.size()]));
    }

    /**
     * Convienence method that invokes a given command type on the remote server found at the given URI. Note that this
     * method is only useful if the command to be issued does not require any parameters.
     *
     * @param  commandType the type of command to issue
     * @param  locatorURI  location of the remote server
     *
     * @return the command response
     *
     * @throws IllegalArgumentException if a failure occurred while processing the cmdline arguments
     * @throws ClassNotFoundException   if failed to find the command's client class
     * @throws IllegalAccessException   if failed to instantiate the command's client class
     * @throws InstantiationException   if failed to instantiate the command's client class
     * @throws MalformedURLException    if the given URL is invalid and cannot be used to locate an invoker
     * @throws Throwable                any other error that was due to a failure during the invocation of the command
     */
    public CommandResponse issueCommand(CommandType commandType, String locatorURI) throws IllegalArgumentException,
        MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, Throwable {
        return issueCommand(commandType, locatorURI, null);
    }

    /**
     * Returns the help usage string.
     *
     * @return help text
     */
    public String getUsage() {
        return LOG.getMsgString(CommI18NResourceKeys.CMDLINE_CLIENT_USAGE);
    }

    /**
     * Processes the command line arguments in a generic enough way for most if not all command clients to be able to
     * use.
     *
     * @param  args the full set of command line arguments given to the client main method
     *
     * @return -1 if everything is OK, anything else means a problem occurred or not enough information was provided on
     *         the cmdline to be able to continue. A 0 or higher means that the client cannot be used to invoke commands
     *         (in that case, the number can be used as the exit code should we want the JVM to exit)
     *
     * @throws Error this should rarely, if ever, occur - it is usually due to a bug in getopt library
     */
    private int processCommandLine(final String[] args) {
        int exitCode = -1;

        m_params = new HashMap<String, Object>();

        // set this from a system property or default to the client classname
        String programName = System.getProperty("program.name", this.getClass().getName());
        String sopts = "-:hv:p:c:l:u:s:";
        LongOpt[] lopts = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("pkgs", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
            new LongOpt("cmd", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
            new LongOpt("version", LongOpt.REQUIRED_ARGUMENT, null, 'v'),
            new LongOpt("class", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
            new LongOpt("uri", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
            new LongOpt("subsystem", LongOpt.REQUIRED_ARGUMENT, null, 's'), };

        Getopt getopt = new Getopt(programName, args, sopts, lopts);
        int code;
        String arg;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?': {
                // for now both of these should exit with error status
                return 1;
            }

            case 1: {
                // this will catch non-option arguments (which will be command params for a particular command)
                arg = getopt.getOptarg();
                String paramName;
                String paramValue;
                int i = arg.indexOf("=");
                if (i == -1) {
                    paramName = arg;
                    paramValue = "true";
                } else {
                    paramName = arg.substring(0, i);
                    paramValue = arg.substring(i + 1, arg.length());
                }

                // add the parameter to the returned map
                m_params.put(paramName, paramValue);

                LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_CMDLINE_PARAM, programName, paramName, ((!"password"
                    .equalsIgnoreCase(paramName)) ? paramValue : "*"));

                break;
            }

            case 'h': {
                // show command line help
                System.out.println(programName + " " + getUsage());
                System.out.println();
                return 0;
            }

            case 'v': {
                arg = getopt.getOptarg();

                try {
                    m_commandVersion = new Integer(arg).intValue();
                } catch (NumberFormatException nfe) {
                    LOG.error(CommI18NResourceKeys.CMDLINE_CLIENT_INVALID_CMD_VERSION, arg, nfe);
                    return 1;
                }

                break;
            }

            case 'p': {
                arg = getopt.getOptarg();
                LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_PACKAGES, programName, arg);

                ArrayList<String> packageList = new ArrayList<String>();
                StringTokenizer strtok = new StringTokenizer(arg, ":");
                while (strtok.hasMoreTokens()) {
                    packageList.add(strtok.nextToken());
                }

                m_packages = packageList.toArray(new String[packageList.size()]);

                break;
            }

            case 'l': {
                m_classname = getopt.getOptarg();

                LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_CLASSNAME, programName, m_classname);

                break;
            }

            case 'c': {
                m_commandName = getopt.getOptarg();

                LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_COMMAND, programName, m_commandName);

                break;
            }

            case 'u': {
                m_locatorUri = getopt.getOptarg();

                LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_LOCATOR_URI, programName, m_locatorUri);

                break;
            }

            case 's': {
                m_subsystem = getopt.getOptarg();

                LOG.debug(CommI18NResourceKeys.CMDLINE_CLIENT_SUBSYSTEM, programName, m_subsystem);

                break;
            }

            default: {
                // this should never happen, if it does its an error in getopt; throw an error so we know about it
                throw new Error(LOG.getMsgString(CommI18NResourceKeys.CMDLINE_CLIENT_UNHANDLED_OPTION, code));
            }
            }
        }

        return exitCode;
    }

    /**
     * Attempts to find the concrete command client class implementation for the command to be issued. This will search
     * the list of packages defined on the command line; if no packages were specified, see below for the algorithm
     * used.
     *
     * <p>When searching the set of packages for the command client class, the client class must be located in a
     * subpackage with the same name as the command name with a class name equal to the command name (capitalized)
     * followed by "CommandClient".</p>
     *
     * <p>As an example, if the package to search is called "org.foo", and the command to invoke is called "bar", the
     * following class name will be used as the command client class: <b><code>
     * org.foo.bar.BarCommandClient</code></b>.</p>
     *
     * <p>A subpackage named "v#" (where # is the command's version number) will be searched if the command client can't
     * be found using the above algorithm: <b><code>org.foo.bar.v1.BarCommandClient</code></b>.</p>
     *
     * <p>If no packages are defined on the command line, the default will be to look in these package (in this
     * order):</p>
     *
     * <ul>
     *   <li><code>org.rhq.enterprise.communications.[prefix.]command.impl</code></li>
     *   <li><code>this.getClass().getPackage().getName()</code> [i.e. this class' package]</li>
     * </ul>
     *
     * <p>The above <i>[prefix.]</i> is called a command prefix. It is defined as a string followed by a dot (.)
     * followed by the command name. A command may or may not have a prefix. The command prefix and the command will be
     * used as-is, unless searching in the default org.jboss.on package structure - in which case the prefix will be
     * inserted after org.rhq.enterprise.communications. when searching. As an example, if the command to execute is
     * version 1 of "hello.world" and no packages are defined on the command line, the packages will be searched for the
     * following classes (assume this class is in package com.abc.command.client):</p>
     *
     * <ul>
     *   <li><code>org.rhq.enterprise.communications.command.impl.hello.world.WorldCommandClient</code></li>
     *   <li><code>org.rhq.enterprise.communications.command.impl.hello.world.v1.WorldCommandClient</code></li>
     *   <li><code>org.rhq.enterprise.communications.hello.command.impl.world.WorldCommandClient</code></li>
     *   <li><code>org.rhq.enterprise.communications.hello.command.impl.world.v1.WorldCommandClient</code></li>
     *   <li><code>com.abc.command.client.hello.world.WorldCommandClient</code></li>
     *   <li><code>com.abc.command.client.hello.world.v1.WorldCommandClient</code></li>
     * </ul>
     *
     * @return the class of the command client to use to invoke the command
     *
     * @throws ClassNotFoundException if the command client class was not found
     */
    private Class findCommandClientClass() throws ClassNotFoundException {
        Class clazz = null;

        if (m_commandName == null) {
            throw new ClassNotFoundException(LOG.getMsgString(CommI18NResourceKeys.CMDLINE_CLIENT_CANNOT_FIND_CLIENT));
        }

        if (m_classname != null) {
            clazz = Class.forName(m_classname);
        } else {
            if ((m_packages == null) || (m_packages.length == 0)) {
                m_packages = new String[] { "org.rhq.enterprise.communications.command.impl",
                    this.getClass().getPackage().getName() };
            }

            // a command can be something like "doSomething" or "prefix.doSomething" or "prefix.xyz.doSomething"
            int lastDot = m_commandName.lastIndexOf('.');
            String lastElementCapitalized = Character.toUpperCase(m_commandName.charAt(lastDot + 1))
                + m_commandName.substring(lastDot + 2);
            String commandClientClassName = lastElementCapitalized + "CommandClient";
            String classPostfix = "." + m_commandName.toLowerCase() + "." + commandClientClassName;
            String versionedClassPostfix = "." + m_commandName.toLowerCase() + ".v" + m_commandVersion + "."
                + commandClientClassName;

            // use this string in case we fail to find the class - each location searched will go in here
            String searchFailures = LOG.getMsgString(CommI18NResourceKeys.CMDLINE_CLIENT_CANNOT_FIND_CLIENT_SEARCHED);

            // determine the list of fully-qualified class names that we will search in order to find the command client class
            List<String> classesToCheck = new ArrayList<String>();

            for (int i = 0; (i < m_packages.length) && (clazz == null); i++) {
                String pkg = m_packages[i];

                // clear the classes from the previous iteration
                classesToCheck.clear();

                // we have special naming rules if looking up in our internal package and we have a command prefix
                if ("org.rhq.enterprise.communications.command.impl".equals(pkg)) {
                    int firstDot = m_commandName.indexOf('.');
                    if (firstDot > -1) {
                        String prefix = m_commandName.substring(0, firstDot);
                        String classPostfixWithoutPrefix = classPostfix.substring(firstDot + 1);
                        String versionedClassPostfixWithoutPrefix = versionedClassPostfix.substring(firstDot + 1);

                        classesToCheck.add("org.rhq.enterprise.communications." + prefix + ".command.impl"
                            + classPostfixWithoutPrefix);
                        classesToCheck.add("org.rhq.enterprise.communications." + prefix + ".command.impl"
                            + versionedClassPostfixWithoutPrefix);
                    }
                }

                // ignoring our special rules, look for the class that is the package appended with the classPostfix
                classesToCheck.add(pkg + classPostfix);
                classesToCheck.add(pkg + versionedClassPostfix);

                // for the set of classes in the current package we are checking
                // doing the check here allows us to quit the package for-loop once we find the class
                for (Iterator iter = classesToCheck.iterator(); iter.hasNext() && (clazz == null);) {
                    String classToFind = (String) iter.next();

                    try {
                        clazz = Class.forName(classToFind);
                    } catch (ClassNotFoundException cnfe) {
                        // remember this location in case of error but move on to the next check
                        searchFailures += (" : " + classToFind);
                    }
                }
            }

            if (clazz == null) {
                throw new ClassNotFoundException(searchFailures);
            }
        }

        return clazz;
    }

    /**
     * Instantiates the given command client class and returns the new instance. Assumes a no-arg constructor exists for
     * the given class.
     *
     * @param  commandClientClass the command client class to instantiate
     *
     * @return the new instance
     *
     * @throws IllegalAccessException if failed to create the new instance
     * @throws InstantiationException if failed to create the new instance
     */
    private CommandClient instantiateCommandClient(Class commandClientClass) throws InstantiationException,
        IllegalAccessException {
        return (CommandClient) commandClientClass.newInstance();
    }
}
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
package org.rhq.enterprise.agent.update;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;

/**
 * The main entry class to the standalone agent updater.
 * This class must be placed in the agent update binary jar file in the
 * proper place and its name must be specified in that jar file's Main-Class
 * manifest entry.
 *  
 * @author John Mazzitelli
 *
 */
public class AgentUpdate {
    private static final String RHQ_AGENT_UPDATE_VERSION_PROPERTIES = "rhq-agent-update-version.properties";

    private static final String DEFAULT_OLD_AGENT_HOME = "rhq-agent";
    private static final String DEFAULT_NEW_AGENT_HOME_PARENT = ".";
    private static final String DEFAULT_LOG_FILE = "rhq-agent-update.log";
    private static final boolean DEFAULT_QUIET_FLAG = false;
    private static final String DEFAULT_SCRIPT_FILE = "rhq-agent-update-build.xml";

    private boolean showVersion = false;
    private boolean updateFlag = false;
    private boolean installFlag = false;
    private String oldAgentHomeArgument = DEFAULT_OLD_AGENT_HOME;
    private String newAgentHomeParentArgument = DEFAULT_NEW_AGENT_HOME_PARENT;
    private String logFileArgument = DEFAULT_LOG_FILE;
    private String jarFileArgument = null;
    private boolean quietFlag = DEFAULT_QUIET_FLAG;
    private String scriptFileArgument = DEFAULT_SCRIPT_FILE;

    /**
     * The main startup point for the self-executing agent update jar.
     * 
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        AgentUpdate agentUpdate = new AgentUpdate();

        // check the command line arguments and set our internals based on them
        // any exceptions coming out of here will abort the update
        try {
            agentUpdate.processArguments(args);
        } catch (IllegalArgumentException error) {
            error.printStackTrace();
            agentUpdate.printSyntax();
            return;
        } catch (UnsupportedOperationException helpException) {
            // this exception occurs simply when the --help option was specified or help needs to be displayed
            try {
                System.out.println(new String(agentUpdate.getJarFileContent("README.txt")));
            } catch (Throwable t) {
                System.out.println("Cannot show README.txt: " + t);
            }
            System.out.println();
            agentUpdate.printSyntax();
            System.out.println();
            agentUpdate.showVersion();
            return;
        }

        // if the user passed --version, show the version and exit immediately
        if (agentUpdate.showVersion) {
            agentUpdate.showVersion();
            return;
        }

        // where should we log our messages?
        File logFile = new File(agentUpdate.logFileArgument);

        // what script should we use? if user did not specify a custom one, use the default
        // one located in our classloader; otherwise, find the custom one on file system
        URL buildFile;
        if (DEFAULT_SCRIPT_FILE.equals(agentUpdate.scriptFileArgument)) {
            buildFile = AgentUpdate.class.getClassLoader().getResource(agentUpdate.scriptFileArgument);
        } else {
            buildFile = new File(agentUpdate.scriptFileArgument).toURI().toURL();
        }

        // set some properties that we can pass to the ANT script
        Properties props = agentUpdate.getAgentUpdateVersionProperties();
        props.setProperty("rhq.agent.update.jar-file", agentUpdate.getJarFilename());
        if (agentUpdate.updateFlag) {
            props.setProperty("rhq.agent.update.update-flag", "true");
            props.setProperty("rhq.agent.update.update-agent-dir", agentUpdate.oldAgentHomeArgument);
        } else if (agentUpdate.installFlag) {
            props.setProperty("rhq.agent.update.install-flag", "true");
            props.setProperty("rhq.agent.update.install-agent-dir", agentUpdate.newAgentHomeParentArgument);
        }

        // if we are updating, backup the current agent just in case we have to restore it
        if (agentUpdate.updateFlag) {
            try {
                agentUpdate.startAnt(buildFile, "backup-agent", "rhq-agent-update-build-tasks.properties", props,
                    logFile, !agentUpdate.quietFlag);
            } catch (Exception e) {
                logMessage(logFile, "WARNING! Agent backup failed! Agent will not recover if it can't update!");
                logStackTrace(logFile, e);
            }
        }

        // run the default ant script target now
        try {
            agentUpdate.startAnt(buildFile, null, "rhq-agent-update-build-tasks.properties", props, logFile,
                !agentUpdate.quietFlag);
        } catch (Exception e) {
            // if we were updating, try to restore the old agent to recover from the error
            if (agentUpdate.updateFlag) {
                logMessage(logFile, "WARNING! Agent update failed! Will try to restore old agent!");
                logStackTrace(logFile, e);
                try {
                    agentUpdate.startAnt(buildFile, "restore-agent", "rhq-agent-update-build-tasks.properties", props,
                        logFile, true);
                } catch (Exception e2) {
                    logMessage(logFile, "WARNING! Agent restore failed! Agent is dead and cannot recover!");
                    logStackTrace(logFile, e2);
                }
            } else {
                throw e;
            }
        }

        return;
    }

    /**
     * Logs a message to both the log file and the stdout console.
     * 
     * @param logFile where to write the log
     * @param msg the message to log
     */
    private static void logMessage(File logFile, String msg) {
        msg = new Date().toString() + ": " + msg;
        System.out.println(msg);
        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(logFile, true));
            pw.println(msg);
            pw.close();
        } catch (Throwable t) {
        }
    }

    /**
     * Logs a stack trace to both the log file and the stdout console.
     * 
     * @param logFile where to write the stack track
     * @param t the exception whose stack track is to be logged
     */
    private static void logStackTrace(File logFile, Throwable t) {
        t.printStackTrace(System.out);
        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(logFile, true));
            t.printStackTrace(pw);
            pw.close();
        } catch (Throwable t1) {
        }
    }

    /**
     * Logs a message to both the log file and the stdout console.
     * 
     * @param msg the message to log
     */
    private void logMessage(String msg) {
        msg = new Date().toString() + ": " + msg;
        System.out.println(msg);
        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(logFileArgument, true));
            pw.println(msg);
            pw.close();
        } catch (Throwable t) {
        }
    }

    private void showVersion() throws Exception {
        String str = "" //
            + "============================================" //
            + "RHQ Agent Update Binary Version Information:" //
            + "============================================" //
            + new String(getJarFileContent(RHQ_AGENT_UPDATE_VERSION_PROPERTIES));
        logMessage(str);
    }

    private String getJarFilename() throws Exception {
        if (this.jarFileArgument == null) {
            URL propsUrl = this.getClass().getClassLoader().getResource(RHQ_AGENT_UPDATE_VERSION_PROPERTIES);
            String propsUrlString = propsUrl.toString();
            // the URL string is something like "jar:file:/dir/foo.jar!/rhq-agent-update-version.properties
            // we need to get the filename, so strip the jar stuff off of it
            propsUrlString = propsUrlString.replaceFirst("jar:", "");
            propsUrlString = propsUrlString.replaceFirst("!/" + RHQ_AGENT_UPDATE_VERSION_PROPERTIES, "");
            File propsFile = new File(new URI(propsUrlString));
            this.jarFileArgument = propsFile.getAbsolutePath();
        }

        return this.jarFileArgument;
    }

    private Properties getAgentUpdateVersionProperties() throws Exception {
        byte[] bytes = getJarFileContent(RHQ_AGENT_UPDATE_VERSION_PROPERTIES);
        InputStream propertiesStream = new ByteArrayInputStream(bytes);
        Properties versionProps = new Properties();
        versionProps.load(propertiesStream);
        return versionProps;
    }

    private byte[] getJarFileContent(String filename) throws Exception {
        JarFile jarFile = new JarFile(getJarFilename()); // use the jar file because user might have used --jar
        JarEntry jarFileEntry = jarFile.getJarEntry(filename);
        InputStream jarFileEntryStream = jarFile.getInputStream(jarFileEntry);
        return slurp(jarFileEntryStream);
    }

    private void printSyntax() {
        String syntax = "Valid options are:" //
            + "[--help] : Help information on how to use this jar file." //
            + "[--version] : Shows version information about this jar file and exits." //
            + "[--update[=<old agent home>]] : When specified, this will update an existing" //
            + "                                agent. If you do not specify the directory" //
            + "                                where the existing agent is, the default is:" //
            + "                                " + DEFAULT_OLD_AGENT_HOME //
            + "                                This is mutually exclusive of --install" //
            + "[--install[=<new agent dir>]] : When specified, this will install a new agent" //
            + "                                without attempting to update any existing" //
            + "                                agent. If you do not specify the directory," //
            + "                                the default is:" + DEFAULT_NEW_AGENT_HOME_PARENT //
            + "                                Note the directory will be the parent of the" //
            + "                                new agent home installation directory." //
            + "                                This is mutually exclusive of --update" //
            + "[--quiet] : If specified, this turns off console log messages." //
            + "[--pause[=<ms>]] : If specified, the update will not occur until the given" //
            + "                   number of milliseconds expires. If this option is given" //
            + "                   without the number of milliseconds, 30000 is the default." //
            + "[--jar=<jar file>] : If specified, the agent found in the given jar file will" //
            + "                     be the new one that will be installed. You usually do not" //
            + "                     have to specify this, since the jar running this update" //
            + "                     code will usually be the one that contains the agent to" //
            + "                     be installed. Do not use this unless you have a reason." //
            + "[--log=<log file>] : If specified, this is where the log messages will be" //
            + "                     written. Default=" + DEFAULT_LOG_FILE //
            + "[--script=<ant script>] : If specified, this will override the default" //
            + "                          upgrade script URL found in the classloader." //
            + "                          Users will rarely need this;" //
            + "                          use this only if you know what you are doing." //
            + "                          Default=" + DEFAULT_SCRIPT_FILE; //
        System.out.println(syntax);
    }

    /**
     * Processes the command line arguments passed to the self-executing jar.
     * 
     * @param args the arguments to process
     * 
     * @throws UnsupportedOperationException if the help option was specified
     * @throws IllegalArgumentException if an argument was invalid 
     * @throws FileNotFoundException if --update is specified with an invalid agent home directory
     */
    private void processArguments(String args[]) throws Exception {

        // if no arguments were specified, then we assume user needs help
        if (args.length <= 0) {
            throw new UnsupportedOperationException();
        }

        String sopts = "u::i::qhvl:j:p::s:";
        LongOpt[] lopts = { new LongOpt("update", LongOpt.OPTIONAL_ARGUMENT, null, 'u'), // updates existing agent
            new LongOpt("install", LongOpt.OPTIONAL_ARGUMENT, null, 'i'), // installs agent
            new LongOpt("quiet", LongOpt.NO_ARGUMENT, null, 'q'), // if not set, dumps log message to stdout too
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'), // will show the syntax help
            new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'), // shows version info
            new LongOpt("log", LongOpt.REQUIRED_ARGUMENT, null, 'l'), // location of the log file
            new LongOpt("jar", LongOpt.REQUIRED_ARGUMENT, null, 'j'), // location of an external jar that has our agent
            new LongOpt("pause", LongOpt.OPTIONAL_ARGUMENT, null, 'p'), // pause (sleep) before updating
            new LongOpt("script", LongOpt.REQUIRED_ARGUMENT, null, 's') }; // switch immediately to the given server

        Getopt getopt = new Getopt(AgentUpdate.class.getSimpleName(), args, sopts, lopts);
        int code;
        long pause = -1L;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                throw new IllegalArgumentException("Bad argument!");
            }

            case 'u': {
                this.updateFlag = true;
                String value = getopt.getOptarg();
                if (value != null) {
                    this.oldAgentHomeArgument = value;
                }

                // make sure the directory actually exists
                File agentHome = new File(this.oldAgentHomeArgument);
                if (!agentHome.exists() || !agentHome.isDirectory()) {
                    throw new FileNotFoundException("There is no agent located at: " + this.oldAgentHomeArgument);
                }

                // be nice for the user - if the user specified the agent's parent directory,
                // then set the old agent home argument to the "real" agent home directory
                File possibleHomeDir = new File(agentHome, "rhq-agent");
                if (possibleHomeDir.exists() && possibleHomeDir.isDirectory()) {
                    agentHome = possibleHomeDir;
                }

                // make it an absolute path
                this.oldAgentHomeArgument = agentHome.getAbsolutePath();
                break;
            }

            case 'i': {
                this.installFlag = true;
                String value = getopt.getOptarg();
                if (value != null) {
                    this.newAgentHomeParentArgument = value;
                }
                break;
            }

            case 'q': {
                this.quietFlag = true;
                break;
            }

            case 'h': {
                throw new UnsupportedOperationException();
            }

            case 'v': {
                this.showVersion = true;
                break;
            }

            case 'l': {
                this.logFileArgument = getopt.getOptarg();
                break;
            }

            case 'j': {
                this.jarFileArgument = getopt.getOptarg();
                File jarFile = new File(this.jarFileArgument);
                if (!jarFile.exists() || !jarFile.isFile()) {
                    throw new FileNotFoundException("There is no agent jar located at: " + this.jarFileArgument);
                }
                break;
            }

            case 'p': {
                pause = 30000L;
                String value = getopt.getOptarg();
                if (value != null) {
                    try {
                        pause = Long.parseLong(value);
                    } catch (Exception e) {
                        pause = 30000L;
                    }
                }

                break;
            }
            case 's': {
                this.scriptFileArgument = getopt.getOptarg();
                break;
            }
            }
        }

        if (getopt.getOptind() < args.length) {
            throw new IllegalArgumentException("Bad arguments.");
        }

        if (this.showVersion) {
            return; // do not continue, this will exit the VM after showing the version info
        }

        if (this.updateFlag && this.installFlag) {
            throw new IllegalArgumentException("Cannot use both --update and --install");
        }

        if (!this.updateFlag && !this.installFlag) {
            throw new IllegalArgumentException("Must specify either --update or --install");
        }

        if (pause > 0) {
            try {
                logMessage("Pausing for [" + pause + "] milliseconds...");
                Thread.sleep(pause);
            } catch (InterruptedException e) {
            } finally {
                logMessage("Done pausing. Continuing with the update.");
            }
        }

        return;
    }

    private byte[] slurp(InputStream stream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long numBytesCopied = 0;
        int bufferSize = 32768;

        try {
            // make sure we buffer the input
            stream = new BufferedInputStream(stream, bufferSize);
            byte[] buffer = new byte[bufferSize];
            for (int bytesRead = stream.read(buffer); bytesRead != -1; bytesRead = stream.read(buffer)) {
                out.write(buffer, 0, bytesRead);
                numBytesCopied += bytesRead;
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Stream data cannot be slurped", ioe);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe2) {
            }
        }

        return out.toByteArray();
    }

    /**
     * Launches ANT and runs the default target in the given build file.
     *
     * @param  buildFile      the build file that ANT will run
     * @param  target         the target to run, <code>null</code> for the default target
     * @param  customTaskDefs the properties file found in classloader that contains all the taskdef definitions
     * @param  properties     set of properties to set for the ANT task to access
     * @param  logFile        where ANT messages will be logged
     * @param logStdOut       if <code>true</code>, log messages will be sent to stdout as well as the log file
     *
     * @throws RuntimeException
     */
    private void startAnt(URL buildFile, String target, String customTaskDefs, Properties properties, File logFile,
        boolean logStdOut) {
        PrintWriter logFileOutput = null;

        try {
            logFileOutput = new PrintWriter(new FileOutputStream(logFile, true));

            ClassLoader classLoader = getClass().getClassLoader();

            Properties taskDefs = new Properties();
            if (customTaskDefs != null) {
                InputStream taskDefsStream = classLoader.getResourceAsStream(customTaskDefs);
                try {
                    taskDefs.load(taskDefsStream);
                } finally {
                    taskDefsStream.close();
                }
            }

            Project project = new Project();
            project.setCoreLoader(classLoader);
            project.init();

            // notice we are adding the listener before we set the properties - if we do not want the
            // the properties echoed out in the log (e.g. if they contain sensitive passwords)
            // we should do this after we set the properties.
            project.addBuildListener(new LoggerAntBuildListener(logFileOutput, Project.MSG_DEBUG));
            if (logStdOut) {
                project.addBuildListener(new LoggerAntBuildListener(new PrintWriter(System.out), Project.MSG_INFO));
            }

            if (properties != null) {
                for (Map.Entry<Object, Object> property : properties.entrySet()) {
                    project.setProperty(property.getKey().toString(), property.getValue().toString());
                }
            }

            for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
                project.addTaskDefinition(taskDef.getKey().toString(), Class.forName(taskDef.getValue().toString(),
                    true, classLoader));
            }

            new ProjectHelper2().parse(project, buildFile);
            project.executeTarget((target == null) ? project.getDefaultTarget() : target);
        } catch (Exception e) {
            throw new RuntimeException("Cannot run ANT on script [" + buildFile + "]. Cause: " + e, e);
        } finally {
            if (logFileOutput != null) {
                logFileOutput.close();
            }
        }
    }
}

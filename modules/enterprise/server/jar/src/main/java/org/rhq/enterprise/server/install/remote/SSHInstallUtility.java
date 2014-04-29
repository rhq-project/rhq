/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.install.remote;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.core.AgentRegistrationRequest;
import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.AgentInstallStep;
import org.rhq.core.domain.install.remote.CustomAgentInstallData;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A utility object that is used to install, start and stop agents remotely over SSH.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class SSHInstallUtility {

    static class Credentials {
        private String username;
        private String password;
        public Credentials () {
        }
        public Credentials (String username, String password) {
            setUsername(username);
            setPassword(password);
        }
        public String getUsername() {
            return this.username;
        }
        public void setUsername(String u) {
            this.username = u;
        }
        public String getPassword() {
            return this.password;
        }
        public void setPassword(String p) {
            this.password = p;
        }
    }

    public static final String AGENT_STATUS_NOT_INSTALLED = "Agent Not Installed";

    private static final String RHQ_AGENT_LATEST_VERSION_PROP = "rhq-agent.latest.version";
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final long TIMEOUT = 30000L;
    private static final long POLL_TIMEOUT = 1000L;

    private Log log = LogFactory.getLog(SSHInstallUtility.class);

    private final RemoteAccessInfo accessInfo;
    private final Credentials defaultCredentials;

    private Session session;

    public SSHInstallUtility(RemoteAccessInfo accessInfo, Credentials defaultCredentials) {
        this.accessInfo = accessInfo;
        this.defaultCredentials = defaultCredentials;
        connect();
    }

    public SSHInstallUtility(RemoteAccessInfo accessInfo) {
        this(accessInfo, null);
    }

    public RemoteAccessInfo getRemoteAccessInfo() {
        return this.accessInfo;
    }

    public void connect() {
        try {
            JSch jsch = new JSch();

            //if (accessInfo.getKey() != null) {
            //    jsch.addIdentity(...);
            //}

            Credentials credentials = getCredentialsToUse();

            session = jsch.getSession(credentials.getUsername(), accessInfo.getHost(), accessInfo.getPort());

            if (credentials.getPassword() != null) {
                session.setPassword(credentials.getPassword());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(CONNECTION_TIMEOUT); // making a connection with timeout.
        } catch (JSchException e) {
            throw new RuntimeException("Failed SSH connection", e);
        }
    }

    public void disconnect() {
        session.disconnect();
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    public boolean agentInstallCheck(String agentInstallPath) {
        String agentWrapperScript = buildAgentWrapperScriptPath(agentInstallPath);

        String value = executeCommand("if  [ -f '" + agentWrapperScript + "' ]; then echo \"exists\"; fi",
            "Agent Install Check");
        if (value == null || value.trim().length() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public AgentInstallInfo installAgent(CustomAgentInstallData customData, String installId) {
        String agentFile;
        String agentPath;
        String agentVersion;

        // get information about the agent distro file
        try {
            File agentBinaryFile = LookupUtil.getAgentManager().getAgentUpdateBinaryFile();
            agentFile = agentBinaryFile.getName();
            agentPath = agentBinaryFile.getCanonicalPath();

            Properties props = LookupUtil.getAgentManager().getAgentUpdateVersionFileContent();
            agentVersion = props.getProperty(RHQ_AGENT_LATEST_VERSION_PROP);
        } catch (Exception e) {
            agentVersion = getClass().getPackage().getImplementationVersion();
            agentFile = "rhq-enterprise-agent-" + agentVersion + ".jar";
            agentPath = "/tmp/rhq-agent/" + agentFile;
            log.warn("Failed agent binary file lookup - using [" + agentPath + "]", e);
        }

        if (!new File(agentPath).exists()) {
            throw new RuntimeException("Unable to find agent binary file for installation at [" + agentPath + "]");
        }

        // confirm that we still have the custom files the user was supposed to have file uploaded
        if (customData.getAgentConfigurationXmlFile() != null) {
            if (!new File(customData.getAgentConfigurationXmlFile()).exists()) {
                throw new RuntimeException("Unable to find custom agent config file at ["
                    + customData.getAgentConfigurationXmlFile() + "]");
            }
        }
        if (customData.getRhqAgentEnvFile() != null) {
            if (!new File(customData.getRhqAgentEnvFile()).exists()) {
                throw new RuntimeException("Unable to find custom agent environment script file at ["
                    + customData.getRhqAgentEnvFile() + "]");
            }
        }

        // do the install work
        String parentPath = customData.getParentPath();
        Credentials credentials = getCredentialsToUse();
        String serverAddress = LookupUtil.getServerManager().getServer().getAddress();
        AgentInstallInfo info = new AgentInstallInfo(parentPath, credentials.getUsername(), agentVersion,
            serverAddress, accessInfo.getHost());

        executeCommand("uname -a", "Machine uname", info);
        executeCommand("java -version", "Java Version Check", info);
        executeCommand("mkdir -p '" + parentPath + "'", "Create Agent Install Directory", info);
        executeCommand("rm -rf '" + parentPath + "/rhq-agent'", "Remove any previously installed agent", info);
        executeCommand("rm -f '" + parentPath + "/rhq-agent-update.log'", "Remove any old agent update logs", info);
        executeCommand("rm -f " + parentPath.replace(" ", "\\ ") + "/rhq-enterprise-agent*.jar",
            "Remove any old agent update binary jars", info); // because we use * wildcard, can't wrap in quotes, so escape spaces if there are any in the path

        log.info("Copying agent binary update distribution file to [" + accessInfo.getHost() + "]...");

        long start = System.currentTimeMillis();
        boolean fileSent = SSHFileSend.sendFile(session, agentPath, parentPath);
        AgentInstallStep scpStep = new AgentInstallStep("ssh copy '" + agentPath + "' -> '" + parentPath + "'",
            "Remote copy the agent binary update distribution", 0, fileSent ? "Success" : "Failed", getTimeDiff(start));
        info.addStep(scpStep);
        if (!fileSent) {
            return info; // abort and return what we did - no sense continuing if the agent distro failed to copy
        }

        log.info("Agent binary update distribution file copied");

        executeCommand("cd '" + parentPath + "' ; " + "java -jar '" + parentPath + "/" + agentFile + "' '--install="
            + parentPath + "'", "Install Agent", info);

        String agentConfigXmlFilename = parentPath + "/rhq-agent/conf/agent-configuration.xml";

        if (customData.getAgentConfigurationXmlFile() != null) {
            log.info("Copying custom agent configuration file...");
            start = System.currentTimeMillis();
            fileSent = SSHFileSend.sendFile(session, customData.getAgentConfigurationXmlFile(), agentConfigXmlFilename);
            AgentInstallStep step = new AgentInstallStep("ssh copy '" + customData.getAgentConfigurationXmlFile()
                + "' -> '" + agentConfigXmlFilename + "'", "Remote copy the agent configuration file", 0,
                fileSent ? "Success" : "Failed", getTimeDiff(start));
            info.addStep(step);
            if (!fileSent) {
                return info; // abort and return what we did - no sense continuing if the custom config file failed to copy
            }
            log.info("Custom agent configuration file copied.");

            // tell the info object - this is needed so it adds the --config command line option
            info.setCustomAgentConfigurationFile("agent-configuration.xml");
        }

        // try to see if we can figure out what the port will be that the agent will bind to
        // this will use awk to find a line in the agent config xml that matches this:
        //    <entry key="rhq.communications.connector.bind-port" value="16163" />
        // where we use " as the field separator and the port number will be the fourth field.
        String agentPortAwkCommand = "awk '-F\"' '/key.*=.*" + AgentInstallInfo.AGENT_PORT_PROP + "/ {print $4}' "
            + "'" + agentConfigXmlFilename + "'";
        String portStr = executeCommand(agentPortAwkCommand, "Determine the agent's bind port", info);
        try {
            int port = Integer.parseInt(portStr.trim());
            info.setAgentPort(port);
        } catch (NumberFormatException nfe) {
            info.setAgentPort(0); // indicate that we don't know it
        }

        if (customData.getRhqAgentEnvFile() != null) {
            log.info("Copying custom agent environment script...");
            String destFilename = parentPath + "/rhq-agent/bin/rhq-agent-env.sh";
            start = System.currentTimeMillis();
            fileSent = SSHFileSend.sendFile(session, customData.getRhqAgentEnvFile(), destFilename);
            AgentInstallStep step = new AgentInstallStep("ssh copy '" + customData.getRhqAgentEnvFile()
                + "' -> '" + destFilename + "'", "Remote copy the agent environment script file", 0,
                fileSent ? "Success" : "Failed", getTimeDiff(start));
            info.addStep(step);
            if (!fileSent) {
                return info; // abort and return what we did - no sense continuing if the custom env script file failed to copy
            }
            log.info("Custom agent environment script copied.");
        }

        // Do a quick check to see if there is something already listening on the agent's port.
        start = System.currentTimeMillis();
        Boolean squatterCheck = checkAgentConnection(info, 1);
        if (squatterCheck != null) { // if this is null, we weren't even able to check
            if (squatterCheck.booleanValue()) {
                AgentInstallStep step = new AgentInstallStep("ping " + info.getAgentAddress() + ":"
                    + info.getAgentPort(), "See if anything has already taken the agent port", 1,
                    "Port already in use", getTimeDiff(start));
                info.addStep(step);
                return info; // abort, don't install an agent if something is already squatting on its port
            } else {
                AgentInstallStep step = new AgentInstallStep("ping " + info.getAgentAddress() + ":"
                    + info.getAgentPort(), "See if anything has already taken the agent port", 0, "Port free",
                    getTimeDiff(start));
                info.addStep(step);
            }
        }

        log.info("Will start new agent @ [" + accessInfo.getHost() + "] pointing to server @ [" + serverAddress + "]");

        String agentScript = parentPath + "/rhq-agent/bin/rhq-agent.sh"; // NOTE: NOT the wrapper script
        String startStringArgs = info.getConfigurationStartString();

        // this ID will be used by the agent when it registered, thus allowing the server to link this install with that agent
        if (installId != null) {
            startStringArgs += " -D" + AgentRegistrationRequest.SYSPROP_INSTALL_ID + "=" + installId;
        }

        // Tell the script to store a pid file to make the wrapper script work
        String envCmd1 = "RHQ_AGENT_IN_BACKGROUND='" + parentPath + "/rhq-agent/bin/rhq-agent.pid'";
        String envCmd2 = "export RHQ_AGENT_IN_BACKGROUND";

        String startCommand = envCmd1 + " ; " + envCmd2 + " ; nohup '" + agentScript + "' " + startStringArgs + " &";
        executeCommand(startCommand, "Start New Agent", info);

        // see if we can confirm the agent connection now
        Boolean pingResults = checkAgentConnection(info, 5);
        if (pingResults == null) {
            log.warn("Just installed an agent at [" + info.getAgentAddress()
                + "] but could not determine its port. No validation check will be made.");
        } else if (!pingResults.booleanValue()) {
            log.warn("Just installed an agent at [" + info.getAgentAddress()
                + "] but could not ping its port. Something might be bad with the install or it is behind a firewall.");
        }

        return info;
    }

    /**
     * Checks if the agent's host/port can be connected to via a TCP socket.
     * This will set the given info's "ConfirmedAgentConnection" attribute as well as return it.
     *
     * @param info information on the agent endpoint; its confirmed-agent-connection flag will be set
     * @param retries number of times to try to connect before aborting (it will set the flag to false and return false when it aborts)
     * @return the flag to indicate if the agent endpoint was able to be successfully connected to (could be null
     *         if the agent port was not known and thus the connection attempt was never made).
     */
    private Boolean checkAgentConnection(AgentInstallInfo info, int retries) {
        // If we know the port the agent is going to listen to, see if we can ping it.
        // If we don't know the port, then just skip this test and set the confirm connection flag to null.
        if (info.getAgentPort() > 0) {
            info.setConfirmedAgentConnection(false);
            for (int attempt = 0; attempt < retries && !info.isConfirmedAgentConnection(); attempt++) {
                Socket ping = new Socket();
                try {
                    ping.connect(new InetSocketAddress(info.getAgentAddress(), info.getAgentPort()), 5000);
                    info.setConfirmedAgentConnection(ping.isConnected());
                } catch (Exception e) {
                    info.setConfirmedAgentConnection(false);
                } finally {
                    try {
                        ping.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        } else {
            info.setConfirmedAgentConnection(null); // indicates we didn't try to ping the agent
        }

        return info.isConfirmedAgentConnection();
    }

    public String uninstallAgent(String doomedPath) {
        String theRealDoomedPath = findAgentInstallPath(doomedPath); // make sure we are looking at an agent
        if (theRealDoomedPath != null) {
            // if the agent is still running, make sure we stop it
            stopAgent(theRealDoomedPath);

            // Before removing the agent dir, remove these first. Since we use ".." it requires the parent to exist
            executeCommand("rm -f '" + theRealDoomedPath + "/../rhq-agent-update.log'", "Remove old agent update logs");
            executeCommand("rm -f " + theRealDoomedPath.replace(" ", "\\ ") + "/../rhq-enterprise-agent*.jar",
                "Remove old agent update binary jars"); // because we use * wildcard, can't wrap in quotes, so escape spaces if there are any in the path

            // now remove the actual agent dir
            String results = executeCommand("rm -rf '" + theRealDoomedPath + "'", "Uninstall Agent");
            return results;
        } else {
            log.warn("Asked to uninstall an agent from [" + accessInfo.getHost() + ":" + doomedPath
                + "] but there does not appear to be an agent there. Skipping the attempt to remove any files.");
            return "There does not appear to be an agent installed here: " + accessInfo.getHost() + ":" + doomedPath;
        }
    }

    public String startAgent(String agentInstallPath) {
        String agentWrapperScript = buildAgentWrapperScriptPath(agentInstallPath);

        return executeCommand("'" + agentWrapperScript + "' start", "Agent Start");
    }

    public String stopAgent(String agentInstallPath) {
        String agentWrapperScript = buildAgentWrapperScriptPath(agentInstallPath);

        return executeCommand("'" + agentWrapperScript + "' stop", "Agent Stop");
    }

    public String agentStatus(String agentInstallPath) {
        String agentWrapperScript = buildAgentWrapperScriptPath(agentInstallPath);

        if (!agentInstallCheck(agentInstallPath)) {
            return AGENT_STATUS_NOT_INSTALLED;
        }

        return executeCommand("'" + agentWrapperScript + "' status", "Agent Status");
    }

    public String findAgentInstallPath(String parentPath) {
        if (parentPath == null || parentPath.trim().length() == 0) {
            // user doesn't know where the agent might be - let's try to guess
            String[] possiblePaths = new String[] { "/opt", "/usr/local", "/usr/share", "/rhq",
                "/home/" + getCredentialsToUse().getUsername() };
            for (String possiblePath : possiblePaths) {
                String path = findAgentInstallPath(possiblePath);
                if (path != null) {
                    return path;
                }
            }
            return null;
        }

        if (parentPath.endsWith("/rhq-agent") || parentPath.endsWith("/rhq-agent/")) {
            // strip "rhq-agent" so we look to see if its really there in the parent
            // we can't use java.io.File for this because we might be running on a Windows box - don't forget, we are ssh'ing into a remote box
            parentPath = parentPath.substring(0, parentPath.lastIndexOf("/rhq-agent"));
        }

        String findOutput;

        try {
            findOutput = executeCommand("find '" + parentPath + "' -maxdepth 4 -name rhq-agent -print"); // don't call the other execute methods, we want to be able to catch the exception here
        } catch (ExecuteException e) {
            // It is possible the 'find' returned a non-zero exit code because some subdirectories were unreadable.
            // Ignore that and just analyze the files that 'find' did return.
            findOutput = e.stdout;
        }

        if (findOutput == null || findOutput.trim().length() == 0) {
            return null;
        }
        String[] results = findOutput.split("\n");
        for (String result : results) {
            if (result.contains("/.java/")) {
                continue; // ignore the rhq-agent Java Preference node - we know that's not an agent
            }
            return result; // just return the first place we find that looks like an agent
        }
        return null; // nothing looks like an agent
    }

    public String[] pathDiscovery(String parentPath) {
        String full = executeCommand("ls -1 '" + parentPath + "'", "Path Discovery");
        return full.split("\n");
    }

    private Credentials getCredentialsToUse() {
        String user = accessInfo.getUser();
        if ((user == null || user.length() == 0) && this.defaultCredentials != null) {
            user = this.defaultCredentials.getUsername();
        }
        String pw = accessInfo.getPassword();
        if ((pw == null || pw.length() == 0) && this.defaultCredentials != null) {
            pw = this.defaultCredentials.getPassword();
        }
        Credentials creds = new Credentials(user, pw);
        return creds;
    }

    private String buildAgentWrapperScriptPath(String agentInstallPath) {
        // its possible the caller is giving us the parent install directory, whereas we
        // want the child "rhq-agent" directory. Our find method will take care of this
        // and return the path we want - if it doesn't, just use the path the user gave us
        // and let the chips fall where they may
        String foundAgentInstall = findAgentInstallPath(agentInstallPath);
        if (foundAgentInstall != null) {
            agentInstallPath = foundAgentInstall;
        }
        String agentWrapperScript = agentInstallPath + "/bin/rhq-agent-wrapper.sh";
        return agentWrapperScript;
    }

    private String executeCommand(String command, String description) {
        return executeCommand(command, description, new AgentInstallInfo());
    }

    private String executeCommand(String command, String description, AgentInstallInfo info) {
        log.info("Running SSH command [" + description + "]");
        long start = System.currentTimeMillis();
        String result = null;
        try {
            result = executeCommand(command);
            info.addStep(new AgentInstallStep(command, description, 0, result, getTimeDiff(start)));
        } catch (ExecuteException e) {
            info.addStep(new AgentInstallStep(command, description, e.errorCode, e.getMessage(), getTimeDiff(start)));
        }
        log.info("Result of SSH command [" + description + "]: " + result);
        return result;
    }

    private String executeCommand(String command) throws ExecuteException {
        ChannelExec channel = null;
        int exitStatus = -1;

        InputStream is = null;
        InputStream es = null;

        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            is = channel.getInputStream();
            es = channel.getErrStream();

            channel.connect(CONNECTION_TIMEOUT); // connect and execute command

            String out = read(is, channel);
            String err = read(es, channel);

            if (log.isTraceEnabled()) {
                log.trace("SSH command output: " + out);
            }

            if (err.length() > 0) {
                exitStatus = channel.getExitStatus();

                if (log.isTraceEnabled()) {
                    log.trace("SSH command error [" + exitStatus + "]: " + err);
                }

                if (exitStatus != 0) {
                    throw new ExecuteException(exitStatus, err, out);
                } else if (out.length() == 0) {
                    return err;
                }
            } else {
                exitStatus = 0;
            }

            return out;

        } catch (ExecuteException ee) {
            throw ee;
        } catch (Exception e) {
            throw new ExecuteException(exitStatus, e.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }

            if (es != null) {
                try {
                    es.close();
                } catch (Exception e) {
                }
            }

            if (channel != null) {
                try {
                    channel.disconnect();
                } catch (Exception e) {
                    log.error("Failed to disconnect", e);
                }
            }
        }
    }

    private String read(InputStream is, Channel channel) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final long endTime = System.currentTimeMillis() + TIMEOUT;
        while (System.currentTimeMillis() < endTime) {
            while (is.available() > 0) {
                int count = is.read(buffer, 0, DEFAULT_BUFFER_SIZE);
                if (count >= 0) {
                    bos.write(buffer, 0, count);
                } else {
                    break;
                }
            }
            if (channel.isClosed()) {
                if (log.isDebugEnabled()) {
                    log.debug("SSH reading exit status=" + channel.getExitStatus());
                }
                break;
            }
            try {
                Thread.sleep(POLL_TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        return bos.toString();
    }

    private long getTimeDiff(long start) {
        return System.currentTimeMillis() - start;
    }

    private static class ExecuteException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        int errorCode;
        String stdout;

        public ExecuteException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public ExecuteException(int errorCode, String message, String stdout) {
            super(message);
            this.errorCode = errorCode;
            this.stdout = stdout;
        }
    }
}

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
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.AgentInstallStep;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A utility object that is used to install, start and stop agents remotely over SSH.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class SSHInstallUtility {

    private static final String RHQ_AGENT_LATEST_VERSION_PROP = "rhq-agent.latest.version";
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final long TIMEOUT = 30000L;
    private static final long POLL_TIMEOUT = 1000L;

    private Log log = LogFactory.getLog(SSHInstallUtility.class);

    private RemoteAccessInfo accessInfo;
    private Session session;

    private String agentFile;
    private String agentPath;
    private String agentVersion;

    public SSHInstallUtility(RemoteAccessInfo accessInfo) {
        this.accessInfo = accessInfo;

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

        connect();
    }

    public void connect() {
        try {
            JSch jsch = new JSch();

            //if (accessInfo.getKey() != null) {
            //    jsch.addIdentity(...);
            //}

            session = jsch.getSession(accessInfo.getUser(), accessInfo.getHost(), accessInfo.getPort());

            if (accessInfo.getPassword() != null) {
                session.setPassword(accessInfo.getPassword());
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

    public AgentInstallInfo installAgent(String parentPath) {

        AgentInstallInfo info = new AgentInstallInfo(parentPath, accessInfo.getUser(), agentVersion);

        executeCommand("uname -a", "Machine uname", info);
        executeCommand("java -version", "Java Version Check", info);
        executeCommand("mkdir -p '" + parentPath + "'", "Create Agent Install Directory", info);

        log.info("Copying agent binary update distribution file to [" + accessInfo.getHost() + "]...");

        long start = System.currentTimeMillis();
        boolean fileSent = SSHFileSend.sendFile(session, agentPath, parentPath);
        AgentInstallStep scpStep = new AgentInstallStep("ssh copy '" + agentPath + "' -> '" + parentPath + "'",
            "Remote copy the agent binary update distribution", 0, fileSent ? "Success" : "Failed", getTimeDiff(start));
        info.addStep(scpStep);

        log.info("Agent binary update distribution file copied");

        executeCommand("cd '" + parentPath + "'", "Change to install directory", info);
        executeCommand("java -jar '" + parentPath + "/" + agentFile + "' '--install=" + parentPath + "'",
            "Install Agent", info);

        String serverAddress = LookupUtil.getServerManager().getServer().getAddress();

        log.info("Will start new agent @ [" + accessInfo.getHost() + "] pointing to server @ [" + serverAddress + "]");

        String agentScript = parentPath + "/rhq-agent/bin/rhq-agent.sh"; // NOTE: NOT the wrapper script
        String properties = new AgentInstallInfo(serverAddress, accessInfo.getHost()).getConfigurationStartString();

        // Tell the script to store a pid file to make the wrapper script work
        String envCmd1 = "RHQ_AGENT_IN_BACKGROUND='" + parentPath + "/rhq-agent/bin/rhq-agent.pid'";
        String envCmd2 = "export RHQ_AGENT_IN_BACKGROUND";

        String startCommand = envCmd1 + " ; " + envCmd2 + " ; nohup '" + agentScript + "' " + properties + " &";
        executeCommand(startCommand, "Start New Agent", info);

        return info;
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
            return "Agent Not Installed";
        }

        return executeCommand("'" + agentWrapperScript + "' status", "Agent Status");
    }

    public String findAgentInstallPath(String parentPath) {
        if (parentPath == null || parentPath.trim().length() == 0) {
            // user doesn't know where the agent might be - let's try to guess
            String[] possiblePaths = new String[] { "/opt", "/usr/local", "/usr/share", "/rhq",
                "/home/" + accessInfo.getUser() };
            for (String possiblePath : possiblePaths) {
                String path = findAgentInstallPath(possiblePath);
                if (path != null) {
                    return path;
                }
            }
            return null;
        }

        if (parentPath.endsWith("rhq-agent") || parentPath.endsWith("rhq-agent/")) {
            return parentPath; // assume the caller's parent path *is* the agent install path
        }

        String full = executeCommand("find '" + parentPath + "' -name rhq-agent -print", "Find Agent Install Path");
        if (full == null || full.trim().length() == 0) {
            return null;
        }
        String[] results = full.split("\n");
        String path = results[0];
        return path;
    }

    public String[] pathDiscovery(String parentPath) {
        String full = executeCommand("ls -1 '" + parentPath + "'", "Path Discovery");
        return full.split("\n");
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
        return executeCommand(command, description, new AgentInstallInfo(null, null));
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

    private String executeCommand(String command) {
        ChannelExec channel = null;
        int exitStatus = -1;

        InputStream is = null;
        InputStream es = null;

        try {
            channel = (ChannelExec) session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

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
                    throw new ExecuteException(exitStatus, err);
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

        public ExecuteException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    public static void main(String[] args) throws IOException {

        String pass = null;
        if (args.length > 2) {
            pass = args[2];
        }
        RemoteAccessInfo info = new RemoteAccessInfo(args[0], args[1], pass);

        SSHInstallUtility ssh = new SSHInstallUtility(info);

        String parentPath = "/tmp/new-remote-agent";
        String agentInstallPath = parentPath + "/rhq-agent";

        System.out.println("Agent status: " + ssh.agentStatus(agentInstallPath));
        System.out.println("Agent stop: " + ssh.stopAgent(agentInstallPath));
        System.out.println("Agent find: " + ssh.findAgentInstallPath(parentPath));
        System.out.println("Agent install: " + ssh.installAgent(parentPath));
        System.out.println("Agent find: " + ssh.findAgentInstallPath(parentPath));
        System.out.println("Agent status: " + ssh.agentStatus(agentInstallPath));
        System.out.println("Agent stop: " + ssh.stopAgent(agentInstallPath));
        System.out.println("Agent status: " + ssh.agentStatus(agentInstallPath));
        System.out.println("Agent start: " + ssh.startAgent(agentInstallPath));

        ssh.disconnect();
    }

}

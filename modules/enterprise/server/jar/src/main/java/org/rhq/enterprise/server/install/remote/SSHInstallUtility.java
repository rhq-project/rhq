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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

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
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class SSHInstallUtility {

    static final int DEFAULT_BUFFER_SIZE = 4096;
    static final long TIMEOUT = 10000L;
    static final long POLL_TIMEOUT = 1000L;

    private Log log = LogFactory.getLog(SSHInstallUtility.class);

    private RemoteAccessInfo accessInfo;
    private Session session;

    private String agentDestination = "/tmp/rhqAgent"; // todo: Make configurable


    private String agentFile = "rhq-enterprise-agent-3.0.0-SNAPSHOT.jar"; // Corrected below
    private String agentPath = "/projects/rhq/dev-container/jbossas/server/default/deploy/rhq.ear/rhq-downloads/rhq-agent/" + agentFile; // corrected below


    public SSHInstallUtility(RemoteAccessInfo accessInfo) {
        this.accessInfo = accessInfo;

        try {
            File agentBinaryFile = LookupUtil.getAgentManager().getAgentUpdateBinaryFile();
            agentPath = agentBinaryFile.getCanonicalPath();
            agentFile = agentBinaryFile.getName();
        } catch (Exception e) {
            // Could not find agent file, leave the default
            log.warn("Failed agent binary file lookup", e);
        }

        if (!new File(agentPath).exists()) {
            throw new RuntimeException("Unable to find agent binary file for installation at [" + agentPath + "]");
        }

        connect();
    }


    public void connect() {
        try {
            JSch jsch = new JSch();


//            if (accessInfo.getKey() != null) {
            jsch.addIdentity("/Users/ghinkle/.ssh/ghinkleawskey.pem");

//            }

            session = jsch.getSession(accessInfo.getUser(), accessInfo.getHost(), 22); // accessInfo.getPort());

            if (accessInfo.getPass() != null) {
                session.setPassword(accessInfo.getPass());
            }

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            //session.connect();
            session.connect(30000);   // making a connection with timeout.
        } catch (JSchException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void disconnect() {
        session.disconnect();
    }


    public String[] pathDiscovery(String parentPath) {
        String full = executeCommand("ls" + parentPath);
        return full.split("\n");
    }


    private String executeCommand(String command, String description) {
        return executeCommand(command, description, new AgentInstallInfo(null, null));
    }


    private String executeCommand(String command, String description, AgentInstallInfo info) {
        log.info("Running: " + description);
        long start = System.currentTimeMillis();
        String result = null;
        try {
            result = executeCommand(command);
            info.addStep(new AgentInstallStep(0, command, description, result, (System.currentTimeMillis() - start)));
        } catch (ExecuteException e) {
            info.addStep(new AgentInstallStep(e.errorCode, command, e.message, description, (System.currentTimeMillis() - start)));
        }
        log.info("Result [" + description + "]: " + result);
        return result;
    }


    private String executeCommand(String command) {
        ChannelExec channel = null;
        int exitStatus = 0;

        InputStream is = null;
        InputStream es = null;

        try {
            channel = (ChannelExec) session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            is = channel.getInputStream();
            es = channel.getErrStream();

            channel.connect(10000); // connect and execute command

            String out = read(is, channel);
            String err = read(es, channel);

            // System.out.println("Output: " + out);
            if (err.length() > 0) {
                // System.out.println("Error [" + channel.getExitStatus() + "]: " + err);
                if (channel.getExitStatus() != 0) {
                    throw new ExecuteException(channel.getExitStatus(), err);
                } else if (out.length() == 0) {
                    return err;
                }
            }
            return out;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                es.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (channel != null) {
                channel.disconnect();
            }
        }
        return "exit: " + exitStatus;
    }


    public String read(InputStream is, Channel channel) throws IOException {
        // read command output
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
                // int exitStatus = channel.getExitStatus();
                // System.out.println("exit status: " + exitStatus);
                break;
            }
            try {
                Thread.sleep(POLL_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return bos.toString();
    }


    public String read2(BufferedReader reader) throws IOException {
        StringBuilder buf = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            buf.append(line + "\n");
        }
        return buf.toString();
    }

    public String agentStop() {
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";

        return executeCommand(agentWrapperScript + " stop", "Agent Stop");
    }

    public String agentStart() {
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";

        return executeCommand(agentWrapperScript + " start", "Agent Start");
    }

    public String agentStatus() {
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";


        String value = executeCommand("if  [ -f " + agentWrapperScript + " ]; then echo \"exists\"; fi");
        if (value == null || value.length() == 0) {
            return "Agent Not Installed";
        }

        return executeCommand(agentWrapperScript + " status", "Agent Status");
    }

    public AgentInstallInfo installAgent() {


        AgentInstallInfo info = new AgentInstallInfo(agentDestination, accessInfo.getUser(), "3.0.0-SNAPSHOT");


        executeCommand("uname -a", "Machine uname", info);

        executeCommand("java -version", "Java Version Check", info);

        executeCommand("mkdir -p " + agentDestination, "Create Agent Install Directory", info);


        log.info("Copying Agent Distribution");
        long start = System.currentTimeMillis();
        boolean fileSent = SSHFileSend.sendFile(session, agentPath, agentDestination);
        info.addStep(new AgentInstallStep(0, "scp agent-installer", "Remote copy the agent distribution", fileSent ? "Success" : "Failed", (System.currentTimeMillis() - start)));
        log.info("Agent Distribution Copied");


        executeCommand("cd " + agentDestination, "Change to install directory", info);

        executeCommand("java -jar " + agentDestination + "/" + agentFile + " --install=" + agentDestination, "Install Agent", info);


        String serverAddress = LookupUtil.getCloudManager().getAllCloudServers().get(0).getAddress();

        System.out.println("Install an agent at " + accessInfo.getHost() + " point to the server " + serverAddress);


        AgentInstallInfo agentInfo = new AgentInstallInfo(serverAddress, accessInfo.getHost());

        String agentScript = agentDestination + "/rhq-agent/bin/rhq-agent.sh ";
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";


        String properties = agentInfo.getConfigurationStartString();

        // Tell the script to store a pid file to make the wrapper script work
        String pidFileProp = "export RHQ_AGENT_IN_BACKGROUND=" + agentDestination + "/rhq-agent/bin/rhq-agent.pid";

        String startCommand = pidFileProp + " ; nohup " + agentScript + properties + "&";
        executeCommand(startCommand, "Agent Start With Configuration", info);


        return info;
    }


    public static class ExecuteException extends RuntimeException {
        int errorCode;
        String message;

        public ExecuteException(int errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }
    }


    public static void main(String[] args) throws IOException {

        String pass = null;
        if (args.length > 2) {
            pass = args[2];
        }
        RemoteAccessInfo info = new RemoteAccessInfo(args[0], args[1], pass);

        SSHInstallUtility ssh = new SSHInstallUtility(info);

        System.out.println("Agent status: " + ssh.agentStatus());

        System.out.println("Set server address: " + ssh.executeCommand("echo '71.162.145.95    ghinkle' >> /etc/hosts"));

        ssh.agentStop();

        ssh.installAgent();

        ssh.agentStatus();

        ssh.agentStop();
        ssh.agentStatus();
        ssh.agentStart();

        ssh.disconnect();
    }

}

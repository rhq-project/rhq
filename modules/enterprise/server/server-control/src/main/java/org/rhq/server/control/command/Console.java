/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.server.control.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;
import org.rhq.server.control.util.ExecutorAssist;

/**
 * @author John Sanda
 */
public class Console extends ControlCommand {

    private Options options;

    public Console() {
        options = new Options().addOption(null, "storage", false, "Start the RHQ storage node in the foreground")
            .addOption(null, "server", false, "Start the RHQ server in the foreground")
        // leaving out the agent option for now...
        ;//.addOption(null, "agent", false, "Start the RHQ agent in the foreground (unsupported)");
    }

    @Override
    public String getName() {
        return "console";
    }

    @Override
    public String getDescription() {
        return "Starts an RHQ service in the foreground. Only --server or --storage is supported. To start the agent in "
            + "the foreground, use the <RHQ_AGENT_HOME>/bin/rhq-agent.(sh|bat) script.";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected String getReadmeFilename() {
        return "CONSOLE_README.txt";
    }

    @Override
    protected int exec(CommandLine commandLine) {
        int rValue = RHQControl.EXIT_CODE_OK;

        if (commandLine.getOptions().length != 1) {
            printUsage();
            rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
        } else {
            String option = commandLine.getOptions()[0].getLongOpt();
            try {
                if (option.equals(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        rValue = Math.max(rValue, startStorageInForeground());
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
                    }
                } else if (option.equals(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        rValue = Math.max(rValue, startServerInForeground());
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
                    }
                } else if (option.equals(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        rValue = Math.max(rValue, startAgentInForeground());
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
                    }
                } else {
                    throw new IllegalArgumentException(option + " is not a supported option");
                }
            } catch (Exception e) {
                throw new RHQControlException("Failed to execute console command", e);
            }
        }
        return rValue;
    }

    private int startStorageInForeground() throws Exception {
        log.debug("Starting RHQ storage node in foreground");

        File storageBinDir = new File(getStorageBasedir(), "bin");

        if (isWindows()) {
            return startInWindowsForeground(storageBinDir, "cassandra", "-f");
        }

        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(getCommandLine(false,
            "cassandra", "-f"));
        return ExecutorAssist.execute(storageBinDir, commandLine);
    }

    private int startServerInForeground() throws Exception {
        log.debug("Starting RHQ server in foreground");

        validateServerPropertiesFile();
        File binDir = getBinDir();

        if (isWindows()) {
            return startInWindowsForeground(binDir, "rhq-server", "console");
        }

        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "console");
        return ExecutorAssist.execute(binDir, commandLine);
    }

    private int startAgentInForeground() throws Exception {
        log.info("Starting RHQ agent in foreground");

        File agentHomeDir = getAgentBasedir();
        File agentBinDir = new File(agentHomeDir, "bin");
        File confDir = new File(agentHomeDir, "conf");
        File agentConfigFile = new File(confDir, "agent-configuration.xml");

        Process process = new ProcessBuilder(getScript("rhq-agent"), "-c", agentConfigFile.getPath()).directory(
            agentBinDir)
        //            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        //            .redirectInput(ProcessBuilder.Redirect.INHERIT)
        //            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
        //            .waitFor();

        AgentInputStreamPipe pipe = new AgentInputStreamPipe(process.getInputStream());
        pipe.start();
        pipe.join();
        //        final InputStream inputStream = process.getInputStream();
        //        final CountDownLatch doneSignal = new CountDownLatch(1);
        //        Thread agentThread = new Thread(new Runnable() {
        //            @Override
        //            public void run() {
        //                InputStreamReader reader = new InputStreamReader(inputStream);
        //                Scanner scanner = new Scanner(reader);
        //                while (scanner.hasNextLine()) {
        //                    System.out.println(scanner.nextLine());
        //                }
        //                doneSignal.countDown();
        //            }
        //        });
        //        agentThread.start();
        //        doneSignal.await();
        //        agentThread.join();
        return RHQControl.EXIT_CODE_OK;
    }

    private int startInWindowsForeground(File binDir, String script, String... scriptArgs) throws Exception {
        org.apache.commons.exec.CommandLine commandLine = getConsoleCommandLine(script, scriptArgs);

        Future<Integer> f = ExecutorAssist.executeAsync(binDir, commandLine, null);
        // The program won't launch if rhqctl exits first, wait a few seconds
        for (int i = 0; (i < 3) && !f.isDone(); ++i) {
            Thread.sleep(1000);
        }

        return f.isDone() ? f.get() : 0;
    }

    private class AgentInputStreamPipe extends Thread {

        private InputStream inputStream;

        public AgentInputStreamPipe(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                boolean running = true;
                while (running) {
                    String line = reader.readLine();
                    System.out.println(line);
                    running = line == null || !line.equals("quit");
                }
            } catch (IOException e) {
                log.error("An error occurred processing input from the agent prompt", e);
            }
        }
    }
}

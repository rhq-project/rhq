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
package org.rhq.enterprise.agent.promptcmd;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mazz.i18n.Msg;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Allows the agent to execute an external program.
 *
 * @author John Mazzitelli
 */
public class ExecutePromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.EXECUTE);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (args.length <= 1) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return true;
        }

        // strip the first argument, which is the name of our prompt command
        String[] realArgs = new String[args.length - 1];
        System.arraycopy(args, 1, realArgs, 0, args.length - 1);

        processCommand(realArgs, out);

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.EXECUTE_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.EXECUTE_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.EXECUTE_DETAILED_HELP);
    }

    private void processCommand(String[] args, PrintWriter out) {
        boolean capture = false;
        long waitTime = 30000L;
        Map<String, String> environmentVars = null;
        String executable = null;
        String workingDir = null;
        boolean killOnTimeout = false;

        String sopts = "-cd:E:kw:";
        LongOpt[] lopts = { new LongOpt("capture", LongOpt.NO_ARGUMENT, null, 'c'),
            new LongOpt("killOnTimeout", LongOpt.NO_ARGUMENT, null, 'k'),
            new LongOpt("wait", LongOpt.REQUIRED_ARGUMENT, null, 'w'),
            new LongOpt("directory", LongOpt.REQUIRED_ARGUMENT, null, 'd') };

        Getopt getopt = new Getopt("execute", args, sopts, lopts);
        int code;

        while ((executable == null) && ((code = getopt.getopt()) != -1)) {
            switch (code) {
            case ':':
            case '?': {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return;
            }

            case 1: {
                // we found the executable name - stop processing arguments
                executable = getopt.getOptarg();
                break;
            }

            case 'c': {
                capture = true;
                break;
            }

            case 'd': {
                workingDir = getopt.getOptarg();
                break;
            }

            case 'k': {
                killOnTimeout = true;
                break;
            }

            case 'w': {
                String waitArg = getopt.getOptarg();

                try {
                    waitTime = Long.parseLong(waitArg);
                } catch (NumberFormatException nfe) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_BAD_WAIT_ARG, waitArg));
                    return;
                }

                break;
            }

            case 'E': {
                if (environmentVars == null) {
                    environmentVars = new LinkedHashMap<String, String>();
                }

                // here we parse it just to see if we need to set the value to true if a value wasn't provided
                String envvar = getopt.getOptarg();
                int i = envvar.indexOf("=");
                String name;
                String value;

                if (i == -1) {
                    name = envvar;
                    value = "true";
                } else {
                    name = envvar.substring(0, i);
                    value = envvar.substring(i + 1, envvar.length());
                }

                environmentVars.put(name, value);
                break;
            }
            }
        }

        if (executable == null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_MISSING_EXECUTABLE));
            return;
        }

        // determine what arguments, if any, need to be passed to the process
        List<String> procArgList = new ArrayList<String>();
        for (int i = getopt.getOptind(); i < args.length; i++) {
            procArgList.add(args[i]);
        }

        String[] procArgArray = procArgList.toArray(new String[procArgList.size()]);

        // tell the user what we are going to do
        out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_EXECUTING, executable, procArgList));

        if (environmentVars != null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_ENV, environmentVars));
        }

        if (workingDir != null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_DIR, workingDir));
        }

        out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_WILL_WAIT, Long.valueOf(waitTime)));

        // now execute the process
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        ProcessExecution processExecution = new ProcessExecution(executable);
        processExecution.setArguments(procArgArray);
        processExecution.setEnvironmentVariables(environmentVars);
        processExecution.setWorkingDirectory(workingDir);
        processExecution.setWaitForCompletion(waitTime);
        processExecution.setCaptureOutput(capture);
        processExecution.setKillOnTimeout(killOnTimeout);

        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);

        Integer exitCode = results.getExitCode();
        Throwable error = results.getError();
        String output = results.getCapturedOutput();

        if (exitCode != null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_EXIT_CODE, exitCode));
        }

        if (error != null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_ERROR, ThrowableUtil.getAllMessages(error)));
        }

        if (output != null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.EXECUTE_OUTPUT, output));
        }

        return;
    }
}
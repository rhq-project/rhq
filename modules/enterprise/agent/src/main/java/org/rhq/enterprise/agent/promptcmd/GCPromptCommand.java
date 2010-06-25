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

import java.beans.Introspector;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.List;

import mazz.i18n.Msg;

import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Provides a way to help the agent clean up memory.
 *
 * @author John Mazzitelli
 */
public class GCPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.GC);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        processArguments(agent, args);
        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.GC_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.GC_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.GC_DETAILED_HELP);
    }

    private void processArguments(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        String sopts = "dfv:";
        LongOpt[] lopts = { new LongOpt("verbose", LongOpt.REQUIRED_ARGUMENT, null, 'v'),
            new LongOpt("dump", LongOpt.NO_ARGUMENT, null, 'd'), new LongOpt("free", LongOpt.NO_ARGUMENT, null, 'f') };

        Getopt getopt = new Getopt(getPromptCommandString(), args, sopts, lopts);
        int code;
        Boolean verbose = null;
        Boolean free = null;
        Boolean dump = null;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                break;
            }

            case 'v': {
                String verboseOpt = getopt.getOptarg();
                verbose = Boolean.valueOf(Boolean.parseBoolean(verboseOpt));
                break;
            }

            case 'd': {
                dump = Boolean.TRUE;
                break;
            }

            case 'f': {
                free = Boolean.TRUE;
                break;
            }
            }
        }

        if ((getopt.getOptind() + 1) < args.length) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
        }

        if (Boolean.TRUE.equals(dump)) {
            printCurrentMemoryUsage(out);
        }

        if (verbose != null) {
            setVerbosity(verbose.booleanValue());
        }

        if (Boolean.TRUE.equals(free)) {
            freeMemory(out);
        }

        return;
    }

    private void setVerbosity(boolean verbose) {
        final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        memoryMxBean.setVerbose(verbose);
    }

    private void freeMemory(PrintWriter out) {
        final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();

        printGlobalMemoryUsage(out, memoryMxBean);

        // free up some global static caches
        Introspector.flushCaches();
        LogFactory.releaseAll();

        // invoke the garbage collector
        out.println(MSG.getMsg(AgentI18NResourceKeys.GC_INVOKE));
        memoryMxBean.gc();

        printGlobalMemoryUsage(out, memoryMxBean);
        return;
    }

    private void printCurrentMemoryUsage(PrintWriter out) {
        final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        printGlobalMemoryUsage(out, memoryMxBean);

        List<MemoryPoolMXBean> poolMxBeans = ManagementFactory.getMemoryPoolMXBeans();
        if (poolMxBeans != null) {
            for (MemoryPoolMXBean bean : poolMxBeans) {
                if (bean.isValid()) {
                    String name = bean.getName();
                    MemoryType type = bean.getType();
                    MemoryUsage usage = bean.getUsage();
                    printMemoryUsage(out, name, type, usage);
                }
            }
        }

        return;
    }

    private void printGlobalMemoryUsage(PrintWriter out, final MemoryMXBean memoryMxBean) {
        printMemoryUsage(out, "GLOBAL", MemoryType.HEAP, memoryMxBean.getHeapMemoryUsage());
        printMemoryUsage(out, "GLOBAL", MemoryType.NON_HEAP, memoryMxBean.getNonHeapMemoryUsage());
    }

    private void printMemoryUsage(PrintWriter out, String name, MemoryType type, MemoryUsage memUsage) {
        long init = memUsage.getInit();
        long max = memUsage.getMax();
        long used = memUsage.getUsed();
        long committed = memUsage.getCommitted();

        String typeStr;
        switch (type) {
        case HEAP:
            typeStr = "Heap";
            break;
        case NON_HEAP:
            typeStr = "Non-heap";
            break;
        default:
            typeStr = "?";
        }

        double usedPercentage = (used * 100.0) / committed;
        double committedPercentage = (committed * 100.0) / max;
        out.println(MSG.getMsg(AgentI18NResourceKeys.GC_MEM_USAGE, name, typeStr, init, max, used, usedPercentage,
            committed, committedPercentage));
    }
}
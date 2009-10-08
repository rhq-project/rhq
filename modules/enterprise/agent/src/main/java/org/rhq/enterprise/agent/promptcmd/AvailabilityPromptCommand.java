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
import java.util.Date;
import java.util.List;

import mazz.i18n.Msg;

import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Allows the user to get an availability report.
 *
 * @author John Mazzitelli
 */
public class AvailabilityPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        // strip the first argument, which is the name of our prompt command
        String[] realArgs = new String[args.length - 1];
        System.arraycopy(args, 1, realArgs, 0, args.length - 1);

        processCommand(realArgs, agent);

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_DETAILED_HELP);
    }

    private void processCommand(String[] args, AgentMain agent) {
        PrintWriter out = agent.getOut();
        boolean changedOnly = false;
        boolean verbose = false;

        String sopts = "-cv";
        LongOpt[] lopts = { new LongOpt("changed", LongOpt.NO_ARGUMENT, null, 'c'),
            new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v') };

        Getopt getopt = new Getopt(getPromptCommandString(), args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return;
            }

            case 'c': {
                changedOnly = true;
                break;
            }

            case 'v': {
                verbose = true;
                break;
            }
            }
        }

        if (getopt.getOptind() < args.length) {
            // we got too many arguments on the command line
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        PluginContainer pc = PluginContainer.getInstance();

        // the PC must be started
        if (!agent.isStarted() || !pc.isStarted()) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_MUST_BE_STARTED));
            return;
        }

        // process the inventory
        InventoryManager inventoryManager = pc.getInventoryManager();
        AvailabilityReport report = inventoryManager.executeAvailabilityScanImmediately(changedOnly);

        if (report == null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_NO_COMMITTED_INVENTORY));
            return;
        }

        List<Availability> availabilities = report.getResourceAvailability();

        out.println(MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_REPORT_HEADER, new Date(), availabilities.size(),
            report.isChangesOnlyReport()));

        for (Availability availability : availabilities) {
            if (verbose) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_REPORT_RESOURCE_VERBOSE, availability
                    .getAvailabilityType(), availability.getResource().getName(), availability.getResource().getId(),
                    availability.getResource().getResourceKey()));
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_REPORT_RESOURCE, availability
                    .getAvailabilityType(), availability.getResource().getName()));
            }
        }

        // we need to send the report to the server - otherwise, the "real" availability executor
        // will not send changed resources thinking someone else did
        out.println(MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_REPORT_SENDING));
        inventoryManager.handleReport(report);
        out.println(MSG.getMsg(AgentI18NResourceKeys.AVAILABILITY_REPORT_SENT));

        return;
    }
}
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Set;
import mazz.i18n.Msg;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryFile;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.InventoryPrinter;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Provides a view into the inventory.
 *
 * @author John Mazzitelli
 */
public class InventoryPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.INVENTORY);
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
        return MSG.getMsg(AgentI18NResourceKeys.INVENTORY_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.INVENTORY_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.INVENTORY_DETAILED_HELP);
    }

    private void processCommand(String[] args, AgentMain agent) {
        PrintWriter out = agent.getOut();
        String inventoryBinaryFile = null;
        String exportFile = null;
        boolean dumpXml = false;
        boolean dumpTypesOnly = false;
        Integer id = null;
        boolean noRecurse = false;

        String sopts = "-e:i:ntx";
        LongOpt[] lopts = { new LongOpt("export", LongOpt.REQUIRED_ARGUMENT, null, 'e'),
            new LongOpt("id", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
            new LongOpt("norecurse", LongOpt.NO_ARGUMENT, null, 'n'),
            new LongOpt("types", LongOpt.NO_ARGUMENT, null, 't'), new LongOpt("xml", LongOpt.NO_ARGUMENT, null, 'x') };

        Getopt getopt = new Getopt(getPromptCommandString(), args, sopts, lopts);
        int code;

        while ((inventoryBinaryFile == null) && ((code = getopt.getopt()) != -1)) {
            switch (code) {
            case ':':
            case '?': {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return;
            }

            case 1: {
                // we found the inventory binary file name - stop processing arguments
                inventoryBinaryFile = getopt.getOptarg();
                break;
            }

            case 'e': {
                exportFile = getopt.getOptarg();
                break;
            }

            case 't': {
                dumpTypesOnly = true;
                break;
            }

            case 'n': {
                noRecurse = true;
                break;
            }

            case 'i': {
                String idString = getopt.getOptarg();
                try {
                    id = Integer.valueOf(idString);
                } catch (NumberFormatException e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_BAD_ID, idString));
                    return;
                }

                break;
            }

            case 'x': {
                dumpXml = true;
                break;
            }
            }
        }

        if (getopt.getOptind() < args.length) {
            // we got too many arguments on the command line
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        if ((inventoryBinaryFile != null) && dumpTypesOnly) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_DUMP_TYPES_AND_BINARY_FILE_SPECIFIED));
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        if ((inventoryBinaryFile != null) && (id != null)) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_ID_AND_BINARY_FILE_SPECIFIED));
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        if ((dumpTypesOnly) && (id != null)) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_ID_AND_DUMP_TYPES_SPECIFIED));
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        PluginContainer pc = PluginContainer.getInstance();

        // to get inventory data, the PC must be started
        if (!agent.isStarted() || !pc.isStarted()) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_MUST_BE_STARTED));
            return;
        }

        PrintWriter exportWriter = out; // default is to dump to console out
        if (exportFile != null) {
            try {
                FileOutputStream fos = new FileOutputStream(new File(exportFile));
                exportWriter = new PrintWriter(fos);
            } catch (Exception e) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_BAD_EXPORT_FILE, exportFile, e));
                return;
            }
        }

        // process the inventory
        try {
            if (inventoryBinaryFile == null) {
                if (dumpTypesOnly) {
                    Set<ResourceType> rootTypes = pc.getPluginManager().getMetadataManager().getRootTypes();
                    InventoryPrinter.outputAllResourceTypes(exportWriter, dumpXml, rootTypes);
                } else {
                    ResourceContainer rc = null;
                    if (id != null) {
                        rc = pc.getInventoryManager().getResourceContainer(id);
                        if (rc == null) {
                            out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_INVALID_RESOURCE_ID, id));
                            return;
                        }
                    }

                    InventoryPrinter.outputInventory(exportWriter, !noRecurse, dumpXml, rc, 0,
                        pc.getInventoryManager(), null);
                }
            } else {
                try {
                    InventoryFile file = new InventoryFile(new File(inventoryBinaryFile));
                    file.loadInventory();
                    InventoryPrinter.outputInventory(exportWriter, !noRecurse, dumpXml, null, 0, null, file);
                } catch (PluginContainerException e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.INVENTORY_BAD_INVENTORY_FILE, inventoryBinaryFile, e));
                }
            }
        } finally {
            if (exportFile != null) {
                exportWriter.close();
            }
        }

        return;
    }
}
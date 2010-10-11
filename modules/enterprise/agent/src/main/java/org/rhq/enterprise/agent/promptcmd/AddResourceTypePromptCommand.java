/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.agent.promptcmd;

import java.io.PrintWriter;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.enterprise.agent.AgentMain;

/**
 * Allows the user to add new ResourceTypes via the agent console interface
 * 
 * @author Alexander Kiefer
 */
public class AddResourceTypePromptCommand implements AgentPromptCommand {

    @Override
    public boolean execute(AgentMain agent, String[] args) {

        //PrintWriter for enabling output to the user
        PrintWriter out = agent.getOut();

        //Get an instance of PluginContainer
        PluginContainer pc = PluginContainer.getInstance();

        // the PC must be started, otherwise return 
        if (!agent.isStarted() || !pc.isStarted()) {
            out.println("Agent or Plugin Conatiner have not been started!!");
            return false;
        }

        // process the inventory
        InventoryManager inventoryManager = pc.getInventoryManager();

        //Add new ResourceType
        //inventoryManager.createNewResourceType("AlexTestType", "AlexTestTypeMetric");

        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public String getDetailedHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPromptCommandString() {

        String addResourceTypeCommand = "addType";

        return addResourceTypeCommand;
    }

    @Override
    public String getSyntax() {
        // TODO Auto-generated method stub
        return null;
    }

}

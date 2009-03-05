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
package org.rhq.enterprise.client.commands;

import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.TabularWriter;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageControl;

/**
 * @author Greg Hinkle
 */
public class FindGroupsCommand implements ClientCommand {

    public String getPromptCommandString() {
        return "findGroups";
    }

    public boolean execute(ClientMain client, String[] args) {

        String search = args[1];

        ResourceGroupManagerLocal gm = client.getRemoteClient().getResourceGroupManager();

        PageList<ResourceGroupComposite> list =
                gm.getAllResourceGroups(client.getSubject(), GroupCategory.COMPATIBLE, null, null, search, PageControl.getUnlimitedInstance());


        String[][] data = new String[list.size()][4];
        int i = 0;
        for (ResourceGroupComposite group : list) {
            data[i++] = new String[] {
                    String.valueOf(group.getResourceGroup().getId()),
                    group.getResourceGroup().getName(),
                    group.getResourceGroup().getResourceType().getPlugin() + ":" + group.getResourceGroup().getResourceType().getName(),
                    String.valueOf(group.getMemberCount()),
                    "[" + (group.getAvailability() * 100.0) + "%]" };
        }
        TabularWriter tw = new TabularWriter(client.getPrintWriter(),"Id", "Name", "Type", "Members", "Availability");
        tw.setWidth(client.getConsoleWidth());
        tw.print( data);

        return true;
    }


    public String getSyntax() {
        return "findGroups searchString";
    }

    public String getHelp() {
        return "Search for groups in inventory by string";
    }

    public String getDetailedHelp() {
        return null;
    }
}
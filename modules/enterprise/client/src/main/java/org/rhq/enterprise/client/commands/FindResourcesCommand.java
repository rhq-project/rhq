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

import java.util.List;

import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.TabularWriter;
import org.rhq.enterprise.server.ws.PageControl;
import org.rhq.enterprise.server.ws.ResourceCategory;
import org.rhq.enterprise.server.ws.ResourceComposite;
import org.rhq.enterprise.server.ws.ResourceManagerRemote;

/**
 * @author Greg Hinkle, Simeon Pinder
 */
public class FindResourcesCommand implements ClientCommand {

    public String getPromptCommandString() {
        return "findResources";
    }

    public boolean execute(ClientMain client, String[] args) {

        String search = args[1];
        PageControl pc = new PageControl();
        pc.setPageSize(-1);
        ResourceCategory rc = null;
        String empty = null;
        ResourceManagerRemote res = client.getRemoteClient().getResourceManagerRemote();
        List<ResourceComposite> list = res.findResourceComposites(client.getSubject(), rc, empty, 0, search, pc);

        String[][] data = new String[list.size()][4];
        int i = 0;
        boolean missingData = false;
        for (ResourceComposite resource : list) {
            // initialize the sensible defaults?
            String rId = "(unavailable)";
            String rName = "(unavailable)";
            String rTypePlugin = "(unavailable)";
            String rTypeName = "(unavailable)";
            String rAvailability = "(unavailable)";
            if ((resource != null) && resource.getResource() != null) {
                rId = String.valueOf(resource.getResource().getId());
                rName = resource.getResource().getName();
                if (resource.getResource().getResourceType() != null) {
                    rTypePlugin = resource.getResource().getResourceType().getPlugin();
                    rTypeName = resource.getResource().getResourceType().getName();
                } else {
                    missingData = true;
                }
                if (resource.getAvailability() != null) {
                    rAvailability = resource.getAvailability().name();
                } else {
                    missingData = true;
                }
            }

            // trim name data down so more data visible
            int fieldLength = 25;
            if (rName.length() > fieldLength) {
                rName = rName.substring(0, fieldLength - 1);
            }
            // data[i++] = new String[] {
            // String.valueOf(resource.getResource().getId()),
            // resource.getResource().getName(),
            // resource.getResource().getResourceType().getPlugin() + ":"
            // + resource.getResource().getResourceType().getName(),
            // "[" + resource.getAvailability().name() + "]" };
            data[i++] = new String[] { rId, rName, rTypePlugin + ":" + rTypeName, "[" + rAvailability + "]" };
            // "[" + resource.getAvailability().getName() + "]" };
        }
        // Generate data in table format
        TabularWriter tw = new TabularWriter(client.getPrintWriter(), "Id", "Name", "Type", "Availability");
        tw.setWidth(client.getConsoleWidth());
        tw.print(data);
        if (missingData) {
            client
                .addMenuNote("Data for some of the objects was unavailable. Contact the System Administrator if that a problem.");
        }

        return true;
    }

    public String getSyntax() {
        return "findResources searchString";
    }

    public String getHelp() {
        return "Search for resources in inventory by string";
    }

    public String getDetailedHelp() {
        return null;
    }
}

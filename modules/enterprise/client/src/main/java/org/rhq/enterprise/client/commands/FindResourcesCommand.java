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
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageControl;

/**
 * @author Greg Hinkle
 */
public class FindResourcesCommand implements ClientCommand {
    
    public String getPromptCommandString() {
        return "findResources";
    }

    public boolean execute(ClientMain client, String[] args) {

        String search = args[1];

        ResourceManagerLocal res = client.getRemoteClient().getResourceManager();
        PageList<ResourceComposite> list =
                res.findResourceComposites(client.getSubject(),null, null, null, search, false, PageControl.getUnlimitedInstance());


        String[][] data = new String[list.size()][4];
        int i = 0;
        for (ResourceComposite resource : list) {
            data[i++] = new String[] {
                    String.valueOf(resource.getResource().getId()),
                    resource.getResource().getName(),
                    resource.getResource().getResourceType().getPlugin() + ":" + resource.getResource().getResourceType().getName(),
                    "[" + resource.getAvailability().getName()+ "]" };
        }
        TabularWriter tw = new TabularWriter(client.getPrintWriter(),"Id", "Name", "Type", "Availability");
        tw.setWidth(client.getConsoleWidth());        
        tw.print( data);

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

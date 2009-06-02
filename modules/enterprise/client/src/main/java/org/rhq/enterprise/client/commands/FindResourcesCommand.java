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
 * @author Greg Hinkle
 */
public class FindResourcesCommand implements ClientCommand {

    public String getPromptCommandString() {
        return "findResources";
    }

    public boolean execute(ClientMain client, String[] args) {

        String search = args[1];

        //        ResourceManagerLocal res = client.getRemoteClient().getResourceManager();
        //        PageList<ResourceComposite> list =
        //                res.findResourceComposites(client.getSubject(),null, null, null, search, false, PageControl.getUnlimitedInstance());
        //          ResourceManagerRemote res = client.getRemoteClient().getResourceManagerRemote();
        //          list = res.findResourceComposites(client.getSubject(), null, null, null, searchString, pageControl)

        PageControl pc = new PageControl();
        pc.setPageSize(-1);
        //        pc.setPageSize(CliEngine.PAGE_CONTROL_SIZE_UNLIMITED);
        ResourceCategory rc = null;
        String empty = null;
        //        ResourceManagerLocal res = client.getRemoteClient().getResourceManager();
        //        PageList<ResourceComposite> list = res.findResourceComposites(client.getSubject(), null, null, null, search,
        //            false, PageControl.getUnlimitedInstance());
        //        ResourceManagerBeanService rmes = new ResourceManagerBeanService();
        //        ResourceManagerRemote res = rmes.getResourceManagerBeanPort();
        //        List<ResourceComposite> list = res.findResourceComposites(client.getSubject(), rc, empty, 0, search, pc);
        ResourceManagerRemote res = client.getRemoteClient().getResourceManagerRemote();
        List<ResourceComposite> list = res.findResourceComposites(client.getSubject(), rc, empty, 0, search, pc);

        String[][] data = new String[list.size()][4];
        int i = 0;
        for (ResourceComposite resource : list) {
            data[i++] = new String[] {
                String.valueOf(resource.getResource().getId()),
                resource.getResource().getName(),
                resource.getResource().getResourceType().getPlugin() + ":"
                    + resource.getResource().getResourceType().getName(),
                "[" + resource.getAvailability().name() + "]" };
//            "[" + resource.getAvailability().getName() + "]" };
        }
        TabularWriter tw = new TabularWriter(client.getPrintWriter(), "Id", "Name", "Type", "Availability");
        tw.setWidth(client.getConsoleWidth());
        tw.print(data);

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

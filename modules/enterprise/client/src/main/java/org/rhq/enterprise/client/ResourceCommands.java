///*
// * RHQ Management Platform
// * Copyright (C) 2005-2008 Red Hat, Inc.
// * All rights reserved.
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation version 2 of the License.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
// */
//package org.rhq.enterprise.client;
//
//import org.rhq.enterprise.server.resource.ResourceManagerLocal;
//import org.rhq.core.domain.resource.composite.ResourceComposite;
//import org.rhq.core.domain.util.PageList;
//import org.rhq.core.domain.util.PageControl;
//
///**
// * @author Greg Hinkle
// */
//public class ResourceCommands {
//
//
//    public static void findResources(ClientMain client, String search) {
//
//        ResourceManagerLocal res = client.getRemoteClient().getResourceManager();
//        PageList<ResourceComposite> list = res.findResourceComposites(client.getSubject(),null, null, null, search, false, PageControl.getUnlimitedInstance());
//
//
//        String[][] data = new String[list.size()][3];
//        int i = 0;
//        for (ResourceComposite resource : list) {
//            data[i++] = new String[] { String.valueOf(resource.getResource().getId()), resource.getResource().getName(), "[" + resource.getAvailability().getName()+ "]" };
//        }
//        TabularWriter tw = new TabularWriter(client.getPrintWriter(), "Id", "Name", "Availability");
//        tw.print(data);
//    }
//
//
//}

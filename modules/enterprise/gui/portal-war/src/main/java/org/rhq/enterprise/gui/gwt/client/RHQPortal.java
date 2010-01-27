/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.gwt.client;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.gwt.client.inventory.resource.ResourceGWTService;
import org.rhq.enterprise.gui.gwt.client.inventory.resource.ResourceGWTServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;



/**
 * @author Greg Hinkle
 */
public class RHQPortal implements EntryPoint {
    ResourceGWTServiceAsync resourceService;

    private VerticalPanel mainPanel;


    public void onModuleLoad() {
        resourceService = ResourceGWTService.App.getInstance();

        mainPanel = new VerticalPanel();


        DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");


        RootPanel.get().add(mainPanel);


        final HTML loading = new HTML("    <img src=\"/images/ajax-loader.gif\" alt=\"loading\"> Loading Resources, please wait...");
        mainPanel.add(loading);


        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterName("ghinkle");

        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load" + caught.getMessage());
            }

            public void onSuccess(PageList<Resource> result) {
                mainPanel.clear();

                for (Resource res : result) {
                    mainPanel.add(new Label(res.getName() + " (" + res.getId() + ")"));
                }

                Window.alert("Loaded resources " + result.size());
            }
        });
    }
}

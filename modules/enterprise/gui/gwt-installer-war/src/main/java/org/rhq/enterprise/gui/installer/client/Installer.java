/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.installer.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.installer.client.gwt.InstallerGWTServiceAsync;

/**
 * The GWT {@link EntryPoint entry point} to the RHQ Installer GUI.
 *
 * @author John Mazzitelli
 */
public class Installer implements EntryPoint {

    // This must come first to ensure proper I18N class loading for dev mode
    private static final Messages MSG = GWT.create(Messages.class);

    public void onModuleLoad() {
        IButton button1 = new IButton(MSG.hello_msg());
        button1.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                InstallerGWTServiceAsync rpc = InstallerGWTServiceAsync.Util.getInstance();
                rpc.getAppServerVersion(new AsyncCallback<String>() {

                    @Override
                    public void onSuccess(String result) {
                        SC.say("SUCCESS: " + result);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say("FAILURE: " + caught);
                    }
                });
            }
        });

        IButton button2 = new IButton(MSG.hello_msg());
        button2.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                InstallerGWTServiceAsync rpc = InstallerGWTServiceAsync.Util.getInstance();
                rpc.getOperatingSystem(new AsyncCallback<String>() {

                    @Override
                    public void onSuccess(String result) {
                        SC.say("SUCCESS: " + result);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say("FAILURE: " + caught);
                    }
                });
            }
        });

        VLayout layout = new VLayout(10);
        layout.addMember(button1);
        layout.addMember(button2);
        layout.draw();

        // Remove loading image in case we don't completely cover it
        Element loadingPanel = DOM.getElementById("Loading-Panel");
        loadingPanel.removeFromParent();
    }
}

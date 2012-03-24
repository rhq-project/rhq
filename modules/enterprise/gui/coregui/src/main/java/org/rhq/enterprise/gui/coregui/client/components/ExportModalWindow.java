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
package org.rhq.enterprise.gui.coregui.client.components;

import com.google.gwt.user.client.Window;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.enterprise.gui.coregui.client.PopupWindow;

/**
 * Build a custom Export window based for particular export screens.
 * @todo: change to builder pattern
 * @author Mike Thompson
 */
public class ExportModalWindow {

    //@todo:pull from message bundle
    private static String BASE_URL = "http://localhost:7080/rest/1/reports/";
    
    private String reportUrl;

    PopupWindow exportWindow;

    // optional Fields

    /**
     * For recent Alerts.
     */
    AlertPriority recentAlertPriority;

    public ExportModalWindow( String reportUrl){
        this.reportUrl = reportUrl;
        createDialogWindow();
    }


    private void createDialogWindow(){
        exportWindow = new PopupWindow("exportSettings", null);

        VLayout layout = new VLayout();
        layout.setTitle("Export Settings");

        HLayout headerLayout = new HLayout();
        headerLayout.setAlign(Alignment.CENTER);
        Label header = new Label();
        header.setContents("Export Settings");
        header.setWidth100();
        header.setHeight(40);
        header.setPadding(10);
        //header.setStyleName("HeaderLabel");
        headerLayout.addMember(header);
        layout.addMember(headerLayout);

        HLayout formLayout = new HLayout();
        formLayout.setAlign(VerticalAlignment.TOP);

        DynamicForm form = new DynamicForm();

        //SelectItem formatsList = new SelectItem("Format", "Format");
        ////formatsList.setValueMap("CSV", "XML");
        // form.setItems(formatsList);

        formLayout.addMember(form);
        layout.addMember(formLayout);

        ToolStrip buttonBar = new ToolStrip();
        buttonBar.setAlign(Alignment.RIGHT);

        IButton finishButton = new IButton("Export", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                exportWindow.hide();
                Window.open(calculateUrl(), "download", null);
            }
        });
        buttonBar.addMember(finishButton);
        layout.addMember(buttonBar);

        exportWindow.addItem(layout);
    }

    public void setRecentAlertPriority(AlertPriority recentAlertPriority) {
        this.recentAlertPriority = recentAlertPriority;
    }

    public String calculateUrl(){
        return BASE_URL+reportUrl+".csv";
    }

    public void show(){
        exportWindow.show();
    }

}

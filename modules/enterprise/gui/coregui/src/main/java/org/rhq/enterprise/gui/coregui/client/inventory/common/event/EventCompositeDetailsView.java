/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.event;

import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author Joseph Marques
 */
public class EventCompositeDetailsView extends VLayout {

    private ListGridRecord eventRecord;

    public EventCompositeDetailsView(ListGridRecord eventRecord) {
        this.eventRecord = eventRecord;
    }

    @Override
    protected void onInit() {
        super.onInit();

        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setHeight100();

        StaticTextItem id = new StaticTextItem("id", "Id");
        id.setValue(eventRecord.getAttribute("id"));

        StaticTextItem severity = new StaticTextItem("severity", "Severity");
        severity.setValue(eventRecord.getAttribute("severity"));

        StaticTextItem source = new StaticTextItem("source", "Source");
        source.setValue(eventRecord.getAttribute("source"));

        StaticTextItem timestamp = new StaticTextItem("timestamp", "Timestamp");
        timestamp.setValue(eventRecord.getAttribute("timestamp"));

        TextAreaItem detail = new TextAreaItem("details", "Details");
        detail.setValue(eventRecord.getAttribute("details"));
        detail.setTitleOrientation(TitleOrientation.TOP);
        detail.setColSpan(2);
        detail.setWidth("100%");
        detail.setHeight("100%");

        form.setItems(id, severity, source, timestamp, detail);

        addMember(form);
    }

    public void displayInDialog() {
        Window window = new Window();
        window.setTitle("Event Details");
        window.setWidth(800);
        window.setHeight(800);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(this);
        window.setDismissOnEscape(true);
        window.setDismissOnOutsideClick(true);
        window.show();
        window.focus();
    }

}

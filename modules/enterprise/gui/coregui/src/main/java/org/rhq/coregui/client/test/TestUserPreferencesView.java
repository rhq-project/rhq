/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.coregui.client.test;

import java.util.Map;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.util.preferences.UserPreferences;

public class TestUserPreferencesView extends Table {

    // these are used both as the name of the fields, titles of the headers and the columns of the CSV output
    private static final String FIELD_NAME = "Name";
    private static final String FIELD_VALUE = "Value";

    private static final SortSpecifier[] defaultSorts = new SortSpecifier[] { new SortSpecifier(FIELD_NAME,
        SortDirection.DESCENDING) };

    private final UserPreferences prefs;

    private static String getTableTitle() {
        return "User Preferences (" + UserSessionManager.getSessionSubject().getName() + ")";
    }

    public TestUserPreferencesView() {
        super(getTableTitle(), null, defaultSorts, null, false);
        prefs = UserSessionManager.getUserPreferences();
    }

    @Override
    protected void configureTable() {
        ListGridField id = new ListGridField(FIELD_ID, "ID");
        ListGridField name = new ListGridField(FIELD_NAME, "Name");
        ListGridField value = new ListGridField(FIELD_VALUE, "Value");

        id.setWidth("10%");
        name.setWidth("45%");
        name.setWidth("45%");

        getListGrid().setFields(id, name, value);
        refresh();

        addTableAction("Export To CSV", ButtonColor.BLUE, new AbstractTableAction(TableActionEnablement.ALWAYS) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                final String s = "~";
                StringBuilder csv = new StringBuilder();
                csv.append(FIELD_ID).append(s).append(FIELD_NAME).append(s).append(FIELD_VALUE).append('\n');

                RecordList records = getListGrid().getDataAsRecordList();
                int recordsSize = records.getLength();
                for (int i = 0; i < recordsSize; i++) {
                    Record record = records.get(i);
                    csv.append(record.getAttribute(FIELD_ID)).append(s).append(record.getAttribute(FIELD_NAME))
                        .append(s).append(record.getAttribute(FIELD_VALUE)).append('\n');
                }

                new MessageWindow("Export To CSV", "<pre>" + csv.toString() + "</pre>").show();

                refreshTableInfo();
            }
        });
    }

    @Override
    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.NONE;
    }

    @Override
    public void refresh() {
        super.refresh();
        getListGrid().setRecords(transform(this.prefs.getConfiguration()));
        refreshTableInfo();
    }

    private ListGridRecord[] transform(Configuration configuration) {
        ListGridRecord[] results = new ListGridRecord[configuration.getAllProperties().size()];
        int i = 0;
        for (Map.Entry<String, Property> entry : configuration.getAllProperties().entrySet()) {
            results[i++] = transform(entry.getValue());
        }
        return results;
    }

    private ListGridRecord transform(Property prop) {
        String value;
        if (prop instanceof PropertySimple) {
            value = ((PropertySimple) prop).getStringValue();
        } else {
            String classname = prop.getClass().getName();
            value = "(value of type " + classname.substring(classname.lastIndexOf(".") + 1) + ")";
        }

        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_ID, prop.getId());
        record.setAttribute(FIELD_NAME, prop.getName());
        record.setAttribute(FIELD_VALUE, value);
        return record;
    }

    class MessageWindow extends Window {
        public MessageWindow(String title, String message) {
            super();

            HTMLPane htmlPane = new HTMLPane();
            htmlPane.setMargin(10);
            htmlPane.setDefaultWidth(600);
            htmlPane.setDefaultHeight(400);
            htmlPane.setContents(message);

            setTitle(title);
            setShowMinimizeButton(false);
            setShowMaximizeButton(true);
            setShowCloseButton(true);
            setDismissOnEscape(false); // force close button to be pressed to ensure our close handler is called
            setIsModal(true);
            setShowModalMask(true);
            setAutoSize(true);
            setAutoCenter(true);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClickEvent event) {
                    markForDestroy();
                }
            });
            addItem(htmlPane);
        }
    }
}

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
package org.rhq.coregui.client.inventory.common.event;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;

import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Joseph Marques
 */
public class EventCompositeDetailsView extends EnhancedVLayout implements BookmarkableView {

    private int eventId;

    private static EventCompositeDetailsView INSTANCE = new EventCompositeDetailsView();

    public static EventCompositeDetailsView getInstance() {
        return INSTANCE;
    }

    private EventCompositeDetailsView() {
        // access through the static singleton only
        super();
    }

    private void show(int eventId) {
        EventCriteria criteria = new EventCriteria();
        criteria.addFilterId(eventId);
        GWTServiceLookup.getEventService().findEventCompositesByCriteria(criteria,
            new AsyncCallback<PageList<EventComposite>>() {
                @Override
                public void onSuccess(PageList<EventComposite> result) {
                    EventComposite composite = result.get(0);
                    show(composite);
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_eventDetails_loadFailed(), caught);
                }
            });
    }

    private void show(EventComposite composite) {
        for (Canvas child : getMembers()) {
            removeChild(child);
        }

        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setHeight100();
        form.setWrapItemTitles(false);

        StaticTextItem id = new StaticTextItem("id", MSG.common_title_id());
        id.setValue(composite.getEventId());

        StaticTextItem severity = new StaticTextItem("severity", MSG.view_inventory_eventHistory_severity());
        String severityValue = Canvas.imgHTML(ImageManager.getEventSeverityBadge(composite.getSeverity()), 24, 24);
        switch (composite.getSeverity()) {
        case DEBUG:
            severityValue += MSG.common_severity_debug();
            break;
        case INFO:
            severityValue += MSG.common_severity_info();
            break;
        case WARN:
            severityValue += MSG.common_severity_warn();
            break;
        case ERROR:
            severityValue += MSG.common_severity_error();
            break;
        case FATAL:
            severityValue += MSG.common_severity_fatal();
            break;
        }
        severity.setValue(severityValue);

        StaticTextItem source = new StaticTextItem("source", MSG.view_inventory_eventHistory_sourceLocation());
        source.setValue(composite.getSourceLocation());

        StaticTextItem timestamp = new StaticTextItem("timestamp", MSG.view_inventory_eventHistory_timestamp());
        timestamp.setValue(TimestampCellFormatter.format(composite.getTimestamp(),
            TimestampCellFormatter.DATE_TIME_FORMAT_FULL));

        TextAreaItem detail = new TextAreaItem("details", MSG.view_inventory_eventHistory_details());
        detail.setValue(composite.getEventDetail());
        detail.setTitleOrientation(TitleOrientation.TOP);
        detail.setColSpan(2);
        detail.setWidth("100%");
        detail.setHeight("100%");

        form.setItems(id, severity, source, timestamp, detail);

        addMember(form);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        eventId = viewPath.getCurrentAsInt();
        show(eventId);
    }

}

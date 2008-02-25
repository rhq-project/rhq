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

package org.rhq.enterprise.gui.legacy.action.resource.common.events;

import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility.MetricsControlForm;

/**
 * @author Heiko W. Rupp
 */
public class EventsForm extends MetricsControlForm {

    public EventsForm() {
        super();

    }

    private PageList<EventComposite> events;
    private String sevFilter;
    private String sourceFilter;
    private String searchString;

    public PageList<EventComposite> getEvents() {
        return events;
    }

    public void setEvents(PageList<EventComposite> events) {
        this.events = events;
    }

    public String getSevFilter() {
        return sevFilter;
    }

    public void setSevFilter(String sevFilter) {
        this.sevFilter = sevFilter;
    }

    public String getSourceFilter() {
        return sourceFilter;
    }

    public void setSourceFilter(String sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }
}

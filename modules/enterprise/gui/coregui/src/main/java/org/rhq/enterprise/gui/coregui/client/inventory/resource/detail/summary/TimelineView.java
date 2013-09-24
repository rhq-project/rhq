/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author John Mazzitelli
 */
public class TimelineView extends EnhancedVLayout {
    public static final ViewName VIEW_ID = new ViewName("ResourceTimeline", MSG.view_tabs_common_timeline());

    private final ResourceComposite resourceComposite;

    public TimelineView(ResourceComposite resourceComposite) {
        super();
        this.resourceComposite = resourceComposite;

        setMargin(10);
        setMembersMargin(1);
    }

    @Override
    protected void onDraw() {
        //TODO: replace with GWT version
        final FullHTMLPane timelinePane = new FullHTMLPane("/portal/resource/common/monitor/events/EventsView.jsp?id="
            + resourceComposite.getResource().getId());

        // we create a simple subclass because we need to know when a new range has been set in order to refresh the timeline
        class RangeEditor extends UserPreferencesMeasRangeEditor {
            RangeEditor() {
                super();
            }

            @Override
            public void setMetricRangeProperties(MetricRangePreferences prefs) {
                super.setMetricRangeProperties(prefs);
                timelinePane.redraw();
            }
        }

        RangeEditor range = new RangeEditor();
        addMember(range); // put it at the top above the timeline's filters
        addMember(timelinePane);
    }
}

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.List;

import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3Graph;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Provide the shared stuff for create GraphListViews like Availability graphs
 * and User Preferences pickers for the date range.
 */
public abstract class AbstractD3GraphListView extends EnhancedVLayout {
    protected List<Availability> availabilityList;
    protected List<ResourceGroupAvailability> groupAvailabilityList;
    protected AvailabilityD3Graph availabilityGraph;
    protected static Label loadingLabel = new Label(MSG.common_msg_loading());
    protected UserPreferencesMeasurementRangeEditor measurementRangeEditor;
    protected boolean showAvailabilityGraph = false;

    public AbstractD3GraphListView() {
        super();
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor();
    }

    public abstract void redrawGraphs();

    protected abstract void queryAvailability(final EntityContext context, Long startTime, Long endTime,
        final CountDownLatch countDownLatch);

}

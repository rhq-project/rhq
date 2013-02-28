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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.AvailabilityD3Graph;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class AbstractD3GraphListView  extends LocatableVLayout {
    protected PageList<Availability> availabilityList;
    protected AvailabilityD3Graph availabilityGraph;
    protected static  Label loadingLabel = new Label(MSG.common_msg_loading());
    protected UserPreferencesMeasurementRangeEditor measurementRangeEditor;
    protected boolean monitorDetailView = false;

    public AbstractD3GraphListView(String locatorId){
        super(locatorId);
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
    }

    protected void queryAvailability(final int resourceId, final CountDownLatch countDownLatch) {

        final long startTime = System.currentTimeMillis();

        // now return the availability
        AvailabilityCriteria c = new AvailabilityCriteria();
        c.addFilterResourceId(resourceId);
        c.addFilterInitialAvailability(false);
        c.addSortStartTime(PageOrdering.ASC);
        GWTServiceLookup.getAvailabilityService().findAvailabilityByCriteria(c,
                new AsyncCallback<PageList<Availability>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                        if (countDownLatch != null) {
                            countDownLatch.countDown();
                        }
                    }

                    @Override
                    public void onSuccess(PageList<Availability> availList) {
                        Log.debug("\nSuccessfully queried availability in: " + (System.currentTimeMillis() - startTime)
                                + " ms.");
                        availabilityList = new PageList<Availability>();
                        for (Availability availability : availList) {
                            availabilityList.add(availability);
                        }
                        Log.debug("avail list size: " + availabilityList.size());
                        if (countDownLatch != null) {
                            countDownLatch.countDown();
                        }
                    }
                });
    }


}

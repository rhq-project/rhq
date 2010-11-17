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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class AvailabilityBarView extends HLayout {

    Messages MSG = CoreGUI.getMessages();

    private Resource resource;

    public AvailabilityBarView(Resource resource) {
        this.resource = resource;
        setHeight(28);
        setWidth100();
        setMargin(10);
    }

    @Override
    protected void onInit() {
        super.onInit();

        PageControl pc = PageControl.getUnlimitedInstance();
        pc.initDefaultOrderingField("av.startTime", PageOrdering.ASC);

        GWTServiceLookup.getAvailabilityService().findAvailabilityForResource(resource.getId(), pc,
            new AsyncCallback<PageList<Availability>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                }

                public void onSuccess(PageList<Availability> result) {
                    update(result);
                }
            });
    }

    private void update(PageList<Availability> result) {

        long start = result.get(0).getStartTime().getTime();
        long end = result.get(result.size() - 1).getEndTime() != null ? result.get(result.size() - 1).getEndTime()
            .getTime() : System.currentTimeMillis();

        long diff = end - start;

        Img leftCap = new Img("availBar/leftCap.png", 8, 28);
        addMember(leftCap);

        for (Availability a : result) {

            long endTime = a.getEndTime() != null ? a.getEndTime().getTime() : System.currentTimeMillis();

            double width = (((double) (endTime - a.getStartTime().getTime()) / diff) * 100);
            String widthString = width + "%";
            if (width == 0) {
                widthString = "2px";
            }

            String imagePath = a.getAvailabilityType() == AvailabilityType.UP ? "availBar/up.png" : "availBar/down.png";

            final Img section = new Img(imagePath);
            section.setHeight(28);
            section.setOpacity(60);
            section.setWidth(widthString);

            section.addMouseOverHandler(new MouseOverHandler() {
                public void onMouseOver(MouseOverEvent mouseOverEvent) {
                    section.animateFade(100);
                }
            });
            section.addMouseOutHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent mouseOutEvent) {
                    section.animateFade(60);
                }
            });

            long duration = endTime - a.getStartTime().getTime();

            String durationString = MeasurementConverterClient.format((double) duration, MeasurementUnits.MILLISECONDS,
                true);

            section.setTooltip("<div style=\"white-space: nowrap;\"><b>" + MSG.common_title_availability() + ": </b>"
                + a.getAvailabilityType().name() + "<br/><b>" + MSG.common_title_start() + ": </b>" + a.getStartTime()
                + "<br/><b>" + MSG.common_title_end() + ": </b>" + a.getEndTime() + "<br/><b>"
                + MSG.common_title_duration() + ": </b>" + durationString);

            addMember(section);

        }
        Img rightCap = new Img("availBar/rightCap.png", 8, 28);
        addMember(rightCap);

        /* StringBuffer buf = new StringBuffer("<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"><tr height=\"28\">");

         buf.append("<td width=\"8px\" class=\"availBarLeftCap\">&nbsp;</td>");


         for (Availability a : result) {

             long endTime = a.getEndTime() != null ? a.getEndTime().getTime() : System.currentTimeMillis();


             double width = (((double) (endTime - a.getStartTime().getTime()) / diff) * 100);
             String widthString = width + "%";
             if (width == 0) {
                 widthString = "2px";
             }

             buf.append("<td width=\"" + widthString + "\" class=\"" + (a.getAvailabilityType() == AvailabilityType.UP ? "availBarUp" : "availBarDown") + "\">&nbsp;</td>");
         }
         buf.append("<td width=\"8px\" class=\"availBarRightCap\">&nbsp;</td>");
         buf.append("</tr></table>");


         HTMLFlow bar = new HTMLFlow(buf.toString());

         addMember(bar);*/

    }
}

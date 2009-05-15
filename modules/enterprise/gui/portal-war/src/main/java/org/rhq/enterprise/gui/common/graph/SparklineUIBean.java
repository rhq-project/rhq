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
package org.rhq.enterprise.gui.common.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.util.List;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class SparklineUIBean {

    private int scheduleId;

    public void paint(Graphics2D g2d, Object obj) {

//        this.scheduleId = (Integer) obj;

        String[] keys = ((String)obj).split(":");

        int resourceId = Integer.parseInt(keys[0]);
        int scheduleDefId = Integer.parseInt(keys[1]);


        List<MeasurementDataNumericHighLowComposite> data = getData(resourceId, scheduleDefId);

        double min = Double.MAX_VALUE, max = Integer.MIN_VALUE;
        for (MeasurementDataNumericHighLowComposite d : data) {
            if (d.getLowValue() < min) {
                min = d.getLowValue();
            }
            if (d.getHighValue() > max) {
                max = d.getHighValue();
            }
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double heightScale = max-min != 0 ? (18d / (max - min)) : 0;

        g2d.setColor(Color.lightGray);
        int i = 1;
        for (MeasurementDataNumericHighLowComposite d : data) {
           g2d.drawLine(i, 18 - (int)(heightScale * (d.getValue() - min)) - 1, i, 18);
           i++;
        }

        g2d.setColor(Color.gray);
        i = 1;
        double lastValue = 0;
        g2d.setStroke(new BasicStroke(0.6f));
        for (MeasurementDataNumericHighLowComposite d : data) {

            g2d.drawRect(i, 18 - (int)(heightScale * (d.getValue() - min)), 1, 1);
            if (i > 1) {
                g2d.drawLine(i-1, 18 - (int)(heightScale * (lastValue - min)), i, 18 - (int)(heightScale * (d.getValue() - min)));
            } else {
                g2d.drawRect(i, 18 - (int)(heightScale * (d.getValue() - min)), 1, 1);

            }
            lastValue = d.getValue();
            i++;
        }
       
    }

    private List<MeasurementDataNumericHighLowComposite> getData(int resourceId, int scheduleDefId) {
        
        List<List<MeasurementDataNumericHighLowComposite>> dl =
                LookupUtil.getMeasurementDataManager().getMeasurementDataForResource(
                        EnterpriseFacesContextUtility.getSubject(),
                        resourceId,
                        new int[] {scheduleDefId},
                        System.currentTimeMillis() - (1000L * 60 * 60 * 8),
                        System.currentTimeMillis(),
                        60);

        List<MeasurementDataNumericHighLowComposite> data = dl.get(0);
                
        return data;
    }
}
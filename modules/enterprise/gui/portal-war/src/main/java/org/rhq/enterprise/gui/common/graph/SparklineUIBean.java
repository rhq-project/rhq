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
import java.awt.Point;
import java.awt.Polygon;
import java.awt.LinearGradientPaint;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class SparklineUIBean {

    private int scheduleId;

    public void paint(Graphics2D g2d, Object obj) {

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

        int i = 1;
        g2d.setStroke(new BasicStroke(0.6f));
        Polygon p = new Polygon();
        p.addPoint(0,18);
        for (MeasurementDataNumericHighLowComposite d : data) {

            if (!Double.isNaN(d.getValue())) {
                   p.addPoint(i,18 - (int)(heightScale * (d.getValue() - min)));
            }
            i++;
        }
        p.addPoint(60,18);

        g2d.setPaint(new GradientPaint(0,18, Color.lightGray , 0,0, Color.darkGray));
        g2d.fillPolygon(p);


        g2d.setColor(Color.lightGray);
        g2d.drawPolygon(p);

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
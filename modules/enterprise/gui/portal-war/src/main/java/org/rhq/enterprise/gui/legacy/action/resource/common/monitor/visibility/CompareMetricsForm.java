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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;

/**
 * Implementation notes: Rather than force a boss lookup just to find a name to display for the link back, (I mean,
 * c'mon) it'll be stuffed into the request
 */
public class CompareMetricsForm extends MetricsControlForm {
    /* measurement template ids for the metrics to line up */
    public Integer[] mtids;

    /* resource ids of the child resources to compare */
    public Integer[] childResourceIds;

    private String name;

    private ImageButtonBean back;

    /**
     * keys are MeasurementTemplateValue's values are List's of MetricDisplaySummary's for each resource - the list
     * length will not always be the same, in fact they'll only be the same if each of the resources has all of the same
     * metrics configured
     */
    public Map metrics;

    //-------------------------------------constructors

    public CompareMetricsForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    /**
     * @return
     */
    public Map getMetrics() {
        return metrics;
    }

    /**
     * @return
     */
    public Integer[] getMtids() {
        return mtids;
    }

    /**
     * An alias for getChildResourceIds()
     *
     * @return
     */
    public Integer[] getR() {
        return getChildResourceIds();
    }

    /**
     * @return
     */
    public Integer[] getChildResourceIds() {
        return childResourceIds;
    }

    /**
     * @param map
     */
    public void setMetrics(Map map) {
        metrics = map;
    }

    /**
     * @param integers
     */
    public void setMtids(Integer[] integers) {
        mtids = integers;
    }

    /**
     * @param integers
     */
    public void setR(Integer[] childResourceIds) {
        setChildResourceIds(childResourceIds);
    }

    /**
     * @param integers
     */
    public void setChildResourceIds(Integer[] childResourceIds) {
        this.childResourceIds = childResourceIds;
    }

    /**
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public ImageButtonBean getBack() {
        return back;
    }

    public void setBack(ImageButtonBean b) {
        back = b;
    }

    public boolean isBackClicked() {
        return getBack().isSelected();
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    //-------------------------------------private methods

    @Override
    protected void setDefaults() {
        super.setDefaults();
        mtids = new Integer[0];
        childResourceIds = new Integer[0];
        name = null;
        metrics = null;
        back = new ImageButtonBean();
    }
}
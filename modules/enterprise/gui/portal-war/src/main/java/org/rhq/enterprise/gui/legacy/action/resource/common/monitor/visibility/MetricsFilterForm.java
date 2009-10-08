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

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;

/**
 * Represents the search options of metric lists
 */
public class MetricsFilterForm extends MetricsControlForm {
    public static final int FILTER_AVAIL = 0;
    public static final int FILTER_UTIL = 1;
    public static final int FILTER_THRU = 2;
    public static final int FILTER_PERF = 3;

    public static final int FILTER_DYN = 4;
    public static final int FILTER_TREND_UP = 5;
    public static final int FILTER_TREND_DN = 6;
    public static final int FILTER_STATIC = 7;

    public static final int[] ALL_FILTERS = { FILTER_AVAIL, FILTER_UTIL, FILTER_THRU, FILTER_PERF, FILTER_DYN,
        FILTER_TREND_UP, FILTER_TREND_DN, FILTER_STATIC, };

    public static final long[] BITWISE_FILTERS = { MeasurementConstants.FILTER_AVAIL, MeasurementConstants.FILTER_UTIL,
        MeasurementConstants.FILTER_THRU, MeasurementConstants.FILTER_PERF, MeasurementConstants.FILTER_DYN,
        MeasurementConstants.FILTER_TREND_UP, MeasurementConstants.FILTER_TREND_DN, MeasurementConstants.FILTER_STATIC, };

    //-------------------------------------instance variables

    // links to metric display range edit page
    private ImageButtonBean filterSubmit;
    private int[] filter;
    private String keyword;

    public MetricsFilterForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    public void setFilter(int[] filter) {
        this.filter = filter;
    }

    public int[] getFilter() {
        return filter;
    }

    public long getFilters() {
        long filters = 0;
        if (filter != null) {
            for (int i = 0; i < filter.length; i++) {
                filters |= BITWISE_FILTERS[filter[i]];
            }
        }

        return filters;
    }

    //-------------------------------------public accessors

    public ImageButtonBean getFilterSubmit() {
        return filterSubmit;
    }

    public void setFilterSubmit(ImageButtonBean b) {
        filterSubmit = b;
    }

    public boolean isFilterSubmitClicked() {
        return getFilterSubmit().isSelected();
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    //-------------------------------------private methods

    protected void setDefaults() {
        super.setDefaults();
        filterSubmit = new ImageButtonBean();
        filter = ALL_FILTERS;
        keyword = null;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    //-------------------------------------public methods

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(" filterSubmit=").append(filterSubmit);
        s.append(" filter=").append(StringUtil.arrayToString(filter));
        s.append(" keyword=").append(keyword);
        return s.toString();
    }
}
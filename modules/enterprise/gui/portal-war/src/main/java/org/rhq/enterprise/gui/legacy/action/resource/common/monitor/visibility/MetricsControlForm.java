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
// -*- Mode: Java; indent-tabs-mode: nil; -*-
/*
 * MetricsControlForm.java
 *
 */

package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.apache.struts.util.LabelValueBean;
import org.rhq.enterprise.gui.legacy.Constants;

/**
 * Represents the common set of controls on various pages that display metrics.
 */
public class MetricsControlForm extends MetricDisplayRangeForm {
    private static int[] RN_OPTS = { 4, 8, 12, 24, 30, 48, 60, 90, 120 };

    //-------------------------------------instance variables

    // switches to advanced metric display range
    private ImageButtonBean advanced;

    // links to metric display range edit page
    private ImageButtonBean editRange;

    // changes (simple) metric display range
    private ImageButtonBean range;
    private Boolean readOnly;

    // range display: begin and end times
    private Long rb;
    private Long re;

    // switches to simple metric display range
    private ImageButtonBean simple;

    public MetricsControlForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(" id=").append(this.getId());
        s.append(" type=").append(this.getCategory());
        s.append(" ctype=").append(this.getCtype());
        s.append(" advanced=").append(advanced);
        s.append(" editRange=").append(editRange);
        s.append(" range=").append(range);
        s.append(" readOnly=").append(readOnly);
        s.append(" rb=").append(rb);
        s.append(" re=").append(re);
        s.append(" rn=").append(this.getRn());
        s.append(" ru=").append(this.getRu());
        s.append(" simple=").append(simple);
        return s.toString();
    }

    //-------------------------------------public accessors

    public ImageButtonBean getAdvanced() {
        return advanced;
    }

    public void setAdvanced(ImageButtonBean b) {
        advanced = b;
    }

    public ImageButtonBean getEditRange() {
        return editRange;
    }

    public void setEditRange(ImageButtonBean b) {
        editRange = b;
    }

    public ImageButtonBean getRange() {
        return range;
    }

    public void setRange(ImageButtonBean b) {
        range = b;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean b) {
        readOnly = b;
    }

    // range begin
    public Long getRb() {
        return rb;
    }

    public void setRb(Long l) {
        rb = l;
    }

    // range end
    public Long getRe() {
        return re;
    }

    public void setRe(Long l) {
        re = l;
    }

    public ImageButtonBean getSimple() {
        return simple;
    }

    public void setSimple(ImageButtonBean b) {
        simple = b;
    }

    public boolean isAdvancedClicked() {
        return getAdvanced().isSelected();
    }

    public boolean isEditRangeClicked() {
        return getEditRange().isSelected();
    }

    public boolean isRangeClicked() {
        return getRange().isSelected();
    }

    public boolean isSimpleClicked() {
        return getSimple().isSelected();
    }

    public boolean isAnythingClicked() {
        return isAdvancedClicked() || isEditRangeClicked() || isRangeClicked() || isSimpleClicked();
    }

    public Date getRbDate() {
        if (getRb() == null) {
            return null;
        }

        return new Date(getRb());
    }

    public Date getReDate() {
        if (getRe() == null) {
            return null;
        }

        return new Date(getRe());
    }

    public List getRnMenu() {
        List items = new ArrayList();

        // if no rn is selected, don't bother checking if we need to
        // put it in the menu
        boolean found = getRn() == null;
        String v = null;

        for (int i = 0; i < RN_OPTS.length; i++) {
            if (!found) {
                if (getRn() == RN_OPTS[i]) {
                    // the selected rn is one of the preset options
                    found = true;
                } else if (getRn() < RN_OPTS[i]) {
                    // the selected rn is between two of the preset
                    // options
                    v = getRn().toString();
                    items.add(new LabelValueBean(v, v));
                    found = true;
                }
            }

            v = Integer.toString(RN_OPTS[i]);
            items.add(new LabelValueBean(v, v));
        }

        // one final check to see if the selected rn is bigger than
        // any of the preset options
        if (!found && (getRn() != null)) {
            v = getRn().toString();
            items.add(new LabelValueBean(v, v));
        }

        return items;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    public Map getForwardParams() {
        Map forwardParams = new HashMap(2);
        forwardParams.put(Constants.RESOURCE_ID_PARAM, this.getId());
        if (this.getCtype() != null) {
            forwardParams.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, this.getCtype());
        }

        return forwardParams;
    }

    //-------------------------------------private methods

    @Override
    protected void setDefaults() {
        super.setDefaults();
        advanced = new ImageButtonBean();
        editRange = new ImageButtonBean();
        range = new ImageButtonBean();
        rb = null;
        re = null;
        readOnly = null;
        simple = new ImageButtonBean();
    }
}
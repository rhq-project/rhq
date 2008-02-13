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
 * MetricDisplayRangeForm.java
 *
 */

package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.gui.legacy.action.CalendarForm;

/**
 * Represents the controls on various pages that display metrics summaries.
 */
public class MetricDisplayRangeForm extends CalendarForm {
    public static final Integer ACTION_LASTN = 1;
    public static final Integer ACTION_DATE_RANGE = 2;

    // action radio button: "1" (last n) or "2" (date range)
    private Integer a;
    private Integer ctype;
    private Integer id;

    // fields for simple date range: Last "5" (rn) "Hours" (ru):
    private Integer rn; // range number
    private Integer ru; // range unit
    private int groupId = -1;
    private int parent = -1;

    /**
     * Resource category. The value corresponds to {@link ResourceCategory#name()} - either "PLATFORM", "SERVER, or
     * "SERVICE".
     */
    private String category;

    //-------------------------------------constructors

    public MetricDisplayRangeForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    public Integer getA() {
        return a;
    }

    public void setA(Integer b) {
        a = b;
    }

    public Integer getCtype() {
        return ctype;
    }

    public void setCtype(Integer ctype) {
        // NOTE: If the value of a form parameter is "", friggin' Struts maps it to 0, rather than null... (ips, 04/05/07)
        this.ctype = (ctype != 0) ? ctype : null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer resourceId) {
        // NOTE: If the value of a form parameter is "", friggin' Struts maps it to 0, rather than null... (ips, 04/05/07)
        this.id = (resourceId != 0) ? resourceId : null;
    }

    // range number
    public Integer getRn() {
        return rn;
    }

    public void setRn(Integer i) {
        rn = i;
    }

    // range unit
    public Integer getRu() {
        return ru;
    }

    public void setRu(Integer i) {
        ru = i;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Always check the end date.
     */
    @Override
    public boolean getWantEndDate() {
        return true;
    }

    public boolean isLastnSelected() {
        return (a != null) && (a.intValue() == ACTION_LASTN.intValue());
    }

    public boolean isDateRangeSelected() {
        return (a != null) && (a.intValue() == ACTION_DATE_RANGE.intValue());
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    @Override
    protected boolean shouldValidateDateRange() {
        return isDateRangeSelected();
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" id=").append(id);
        s.append(" category=").append(category);
        s.append(" ctype=").append(ctype);
        s.append(" a=").append(a);
        s.append(" rn=").append(rn);
        s.append(" ru=").append(ru);

        return s.toString();
    }

    protected void setDefaults() {
        a = null;
        rn = null;
        ru = null;
        ctype = null;
        id = null;
        category = null;
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (isLastnSelected()) {
            Integer lastN = this.getRn();
            if ((lastN == null) || (lastN == 0)) {
                if (errors == null) {
                    errors = new ActionErrors();
                }

                errors.add("rn", new ActionMessage("resource.common.monitor.error.LastNInteger"));
            }
        }

        return errors;
    }

    /**
     * @return the groupId
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    /**
     * @return the parent
     */
    public int getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(int parent) {
        this.parent = parent;
    }
}
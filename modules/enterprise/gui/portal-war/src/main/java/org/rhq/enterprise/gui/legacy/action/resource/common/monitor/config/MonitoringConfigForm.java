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
/*
 * MonitoringConfigForm.java
 *
 * Created on April 14, 2003, 1:44 PM
 */

package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.config;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;

import org.rhq.enterprise.gui.legacy.NumberConstants;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm;

/**
 * Form for setting the collection interval for metrics in resource/monitoring/configuration areas of the application,
 * and for adding metrics to a resource.
 */
public class MonitoringConfigForm extends ResourceForm {
    /**
     * Holds value of property mids (metric definition id's).
     */
    private int[] mids;

    /**
     * Holds value of property collectionInterval
     */
    private Long collectionInterval;

    /**
     * Holds value of property collectionUnit.
     */
    private long collectionUnit;

    /**
     * Holds value of property availableMids.
     */
    private Integer[] availableMids;

    /**
     * Holds value of property pendingMids.
     */
    private Integer[] pendingMids;

    /**
     * Holds value of property filterBy.
     */
    private String filterBy;

    /**
     * Holds value of property psp.
     */
    private Integer psp;

    /**
     * Holds value of property psa.
     */
    private Integer psa;

    /**
     * Holds value of property filterOptions.
     */
    private List filterOptions;

    private ImageButtonBean nullBtn;

    /**
     * If we are working on the MeasurementDefinitions, should an update of a MetricDefinition value also update
     * existing schedules?
     */
    private boolean schedulesShouldChange;

    /**
     * Creates new MonitoringConfigForm
     */
    public MonitoringConfigForm() {
        super();
        nullBtn = new ImageButtonBean();
        schedulesShouldChange = true;
    }

    /**
     * Derived property based on collectionInterval and collectionUnit, return the time as a long
     *
     * @return the collection interval time, in milliseconds
     */
    public long getIntervalTime() {
        return collectionInterval * collectionUnit;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.collectionUnit = NumberConstants.MINUTES;
        this.collectionInterval = null;
        this.mids = new int[0];
        this.availableMids = new Integer[0];
        this.pendingMids = new Integer[0];
        this.filterBy = null;
        this.filterOptions = null;
        this.schedulesShouldChange = false;
        super.reset(mapping, request);
    }

    public Long getCollectionInterval() {
        return collectionInterval;
    }

    /**
     * Getter for property mids.
     *
     * @return Value of property mids.
     */
    public int[] getMids() {
        return this.mids;
    }

    /**
     * Setter for property mids.
     *
     * @param mids New value of property mids.
     */
    public void setMids(int[] mids) {
        this.mids = mids;
    }

    /**
     * Setter for property collectionInterval.
     *
     * @param collectionInterval New value of property collectionInterval.
     */
    public void setCollectionInterval(Long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    /**
     * Getter for property collectionUnit.
     *
     * @return Value of property collectionUnit.
     */
    public long getCollectionUnit() {
        return this.collectionUnit;
    }

    /**
     * Setter for property collectionUnit.
     *
     * @param collectionUnit New value of property collectionUnit.
     */
    public void setCollectionUnit(long collectionUnit) {
        this.collectionUnit = collectionUnit;
    }

    /**
     * Indexed getter for property availableMids.
     *
     * @param  index Index of the property.
     *
     * @return Value of the property at <CODE>index</CODE>.
     */
    public Integer getAvailableMids(int index) {
        return this.availableMids[index];
    }

    /**
     * Non-Indexed getter for property availableMids.
     *
     * @return Value of the property at <CODE>index</CODE>.
     */
    public Integer[] getAvailableMids() {
        return this.availableMids;
    }

    /**
     * Non-Indexed setter for property availableMids.
     *
     * @param availableMids New value of the property.
     */
    public void setAvailableMids(Integer[] availableMids) {
        this.availableMids = availableMids;
    }

    /**
     * Getter for property pendingMids.
     *
     * @return Value of property pendingMids.
     */
    public Integer[] getPendingMids() {
        return this.pendingMids;
    }

    /**
     * Setter for property pendingMids.
     *
     * @param pendingMids New value of property pendingMids.
     */
    public void setPendingMids(Integer[] pendingMids) {
        this.pendingMids = pendingMids;
    }

    /**
     * Getter for property filterBy.
     *
     * @return Value of property filterBy.
     */
    public String getFilterBy() {
        return this.filterBy;
    }

    /**
     * Setter for property filterBy.
     *
     * @param filterBy New value of property filterBy.
     */
    public void setFilterBy(String filterBy) {
        this.filterBy = filterBy;
    }

    /**
     * Getter for property psp.
     *
     * @return Value of property psp.
     */
    public Integer getPsp() {
        return this.psp;
    }

    /**
     * Setter for property psp.
     *
     * @param psp New value of property psp.
     */
    public void setPsp(Integer psp) {
        this.psp = psp;
    }

    /**
     * Getter for property psa.
     *
     * @return Value of property psa.
     */
    public Integer getPsa() {
        return this.psa;
    }

    /**
     * Setter for property psa.
     *
     * @param psa New value of property psa.
     */
    public void setPsa(Integer psa) {
        this.psa = psa;
    }

    /**
     * Getter for property filterOptions.
     *
     * @return Value of property filterOptions.
     */
    public List getFilterOptions() {
        return this.filterOptions;
    }

    /**
     * Setter for property filterOptions.
     *
     * @param filterOptions New value of property filterOptions.
     */
    public void setFilterOptions(List filterOptions) {
        this.filterOptions = filterOptions;
    }

    /**
     * @return
     */
    public ImageButtonBean getNullBtn() {
        return nullBtn;
    }

    /**
     * @param bean
     */
    public void setNullBtn(ImageButtonBean bean) {
        nullBtn = bean;
    }

    /**
     * @return the schedulesShouldChange
     */
    public boolean getSchedulesShouldChange() {
        return schedulesShouldChange;
    }

    /**
     * @param schedulesShouldChange the schedulesShouldChange to set
     */
    public void setSchedulesShouldChange(boolean schedulesShouldChange) {
        this.schedulesShouldChange = schedulesShouldChange;
    }
}
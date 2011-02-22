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
package org.rhq.core.domain.measurement.ui;

import java.util.Date;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.Resource;

public class MetricDisplaySummary extends BaseMetricDisplay implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private int alertCount = 0;

    /**
     * for traits
     */
    private String value;
    private long timestamp;

    /**
     * id of a compatible group this metric is in
     */
    private int groupId = -1;

    /**
     * id of the parent of an autogroup this metric is in
     */
    private int parentId = -1;

    /**
     * resource type of the children of an autogroup this metric is in
     */
    private int childTypeId = -1;

    /**
     * Does this summary have metrics that are not NaN
     */
    private boolean valuesPresent = true;

    /**
     * Token used to identify the metric
     */
    private String metricToken;

    private boolean isTrait = false;
    private Resource resource;
    private Resource parent;
    protected int resourceId;

    public MetricDisplaySummary() {
        super();
    }

    @Override
    public String toString() {
        return "token=" + metricToken + "," + this.getClass().getName() + ",group=" + groupId + ",super("
            + super.toString() + ")";
    }

    public int getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(int alertCount) {
        this.alertCount = alertCount;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String val) {
        value = val;
    }

    public boolean getIsTrait() {
        return isTrait;
    }

    public void setIsTrait(boolean isThisATrait) {
        isTrait = isThisATrait;
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
     * Return the last changed timestamp as Date
     *
     * @return Date gets the date changed
     */
    public Date getChangedDate() {
        return new Date(timestamp);
    }

    /**
     * Return the last changed timestamp as long
     *
     * @return long the Timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the valuesPreent
     */
    public boolean getValuesPresent() {
        return valuesPresent;
    }

    /**
     * @param noValuesPreent the valuesPreent to set
     */
    public void setValuesPresent(boolean noValuesPreent) {
        this.valuesPresent = noValuesPreent;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Resource getParent() {
        return parent;
    }

    public void setParent(Resource parent) {
        this.parent = parent;
    }

    /**
     * @return the parentId
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    /**
     * @return the childTypeId
     */
    public int getChildTypeId() {
        return childTypeId;
    }

    /**
     * @param childTypeId the childTypeId to set
     */
    public void setChildTypeId(int childTypeId) {
        this.childTypeId = childTypeId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getMetricToken() {
        return metricToken;
    }

    public void setMetricToken(String metricToken) {
        this.metricToken = metricToken;
    }

    public EntityContext getContext() {
        return new EntityContext(resourceId, groupId, parentId, childTypeId);
    }

    public void init(EntityContext context) {
        this.resourceId = context.resourceId;
        this.groupId = context.groupId;
        this.parentId = context.parentResourceId;
        this.childTypeId = context.resourceTypeId;
    }
}
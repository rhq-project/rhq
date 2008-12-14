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
package org.rhq.enterprise.gui.operation.schedule.group;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.operation.definition.group.ResourceGroupExecutionTypeUIBean;
import org.rhq.enterprise.gui.operation.schedule.OperationScheduleDetailsUIBean;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.operation.OperationSchedule;

public class ResourceGroupOperationScheduleDetailsUIBean extends OperationScheduleDetailsUIBean {

    private String resourceExecutionOption;
    private List<IntegerOptionItem> resourceNameItems;

    @Override
    public OperationSchedule getOperationSchedule(Subject subject, String jobId) throws Exception {
        return manager.getGroupOperationSchedule(subject, jobId);
    }

    @Override
    protected void init() {
        if (null == this.schedule) {
            super.init();

            this.resourceExecutionOption = getResourceExecutionOption((GroupOperationSchedule) this.schedule);
            this.resourceNameItems = getResourceNameItems((GroupOperationSchedule) this.schedule);
        }
    }

    private String getResourceExecutionOption(GroupOperationSchedule schedule) {
        List<Resource> order = schedule.getExecutionOrder();

        boolean isOrdered = (order != null) && (order.size() > 0);

        if (isOrdered) {
            return ResourceGroupExecutionTypeUIBean.Type.ORDERED.name();
        } else {
            return ResourceGroupExecutionTypeUIBean.Type.CONCURRENT.name();
        }
    }

    private List<IntegerOptionItem> getResourceNameItems(GroupOperationSchedule schedule) {
        List<Resource> resourceOrder = schedule.getExecutionOrder();
        if (resourceOrder == null) {
            return new ArrayList<IntegerOptionItem>();
        }

        List<IntegerOptionItem> results = new ArrayList<IntegerOptionItem>();
        for (Resource next : resourceOrder) {
            results.add(new IntegerOptionItem(next.getId(), next.getName()));
        }

        return results;
    }

    public String getResourceExecutionOption() {
        init();

        return resourceExecutionOption;
    }

    public List<IntegerOptionItem> getResourceNameItems() {
        init();

        return resourceNameItems;
    }

    public void setResourceExecutionOption(String resourceExecutionOption) {
        this.resourceExecutionOption = resourceExecutionOption;
    }

    public void setResourceNameItems(List<IntegerOptionItem> resourceNameItems) {
        this.resourceNameItems = resourceNameItems;
    }

}
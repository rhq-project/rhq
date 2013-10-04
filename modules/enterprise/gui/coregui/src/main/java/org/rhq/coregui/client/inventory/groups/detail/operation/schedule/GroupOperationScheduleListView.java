/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.coregui.client.inventory.groups.detail.operation.schedule;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleListView;

/**
 * The list view of the Group Operations>Schedules subtab.
 *
 * @author Ian Springer
 */
public class GroupOperationScheduleListView extends AbstractOperationScheduleListView {

    private static final String TITLE = null;

    private ResourceGroupComposite groupComposite;

    public GroupOperationScheduleListView(ResourceGroupComposite groupComposite) {
        super(new GroupOperationScheduleDataSource(groupComposite), TITLE);
        this.groupComposite = groupComposite;
    }

    @Override
    protected boolean hasControlPermission() {
        return this.groupComposite.getResourcePermission().isControl();
    }

    @Override
    public Canvas getDetailsView(Integer scheduleId) {
        return new GroupOperationScheduleDetailsView(this.groupComposite, scheduleId);
    }

}

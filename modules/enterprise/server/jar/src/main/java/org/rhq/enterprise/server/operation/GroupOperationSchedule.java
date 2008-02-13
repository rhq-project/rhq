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
package org.rhq.enterprise.server.operation;

import java.util.List;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

public class GroupOperationSchedule extends OperationSchedule {
    private ResourceGroup group;
    private List<Resource> executionOrder;
    private boolean haltOnFailure = false; // default to not stop on error

    public GroupOperationSchedule() {
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    /**
     * The order to execute the operations - the first resource in the list is the first one to get its operation
     * invoked. If this is <code>null</code>, all operations can be invoked in any order and may even be done
     * concurrently.
     *
     * @return list of resources in the order in which their operations are invoked, or <code>null</code>
     */
    public List<Resource> getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(List<Resource> executionOrder) {
        this.executionOrder = executionOrder;
    }

    /**
     * If <code>true</code>, the group operation will halt whenever one individual resource fails to execute. When
     * executing in order, this means once a failure occurs, the resources next in line to execute will abort and not
     * attempt to execute. If not executing in any particular order, you are not guaranteed which will stop and which
     * will continue since all are executed as fast as possible, but the group operation will attempt to stop as best it
     * can.
     *
     * @return halt flag
     */
    public boolean isHaltOnFailure() {
        return haltOnFailure;
    }

    public void setHaltOnFailure(boolean haltOnFailure) {
        this.haltOnFailure = haltOnFailure;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("GroupOperationSchedule: ");

        str.append("group=[" + this.group);
        str.append("], execution-order=[" + this.executionOrder);
        str.append("], halt-on-failure=[" + this.haltOnFailure);
        str.append("],");
        str.append(super.toString());

        return str.toString();
    }
}
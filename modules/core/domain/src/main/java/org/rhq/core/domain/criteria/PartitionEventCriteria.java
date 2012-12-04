/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.domain.criteria;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEvent.ExecutionStatus;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.util.PageOrdering;

/**
 * Criteria object for querying {@link PartitionEvent}s.
 *
 * @author Jiri Kremser
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class PartitionEventCriteria extends Criteria {

    private static final long serialVersionUID = 1L;
    
    public static final String SORT_FIELD_CTIME = "ctime";

    private Integer filterId;
    private List<PartitionEventType> filterEventType; // requires override
    private List<ExecutionStatus> filterExecutionStatus; // requires override
    private String filterEventDetail;
        
    private PageOrdering sortCtime;
    
    public PartitionEventCriteria() {
        filterOverrides.put("eventType", "eventType IN ( ? )");
        filterOverrides.put("executionStatus", "executionStatus IN ( ? )");
    }
    
    public Class<?> getPersistentClass() {
        return PartitionEvent.class;
    }

    public void addFilterId(Integer id) {
        this.filterId = id;
    }
    
    public void addFilterEventType(PartitionEventType... filterEventType) {
        if (filterEventType != null && filterEventType.length > 0) {
            this.filterEventType = Arrays.asList(filterEventType);
        }
    }
    
    public void addFilterExecutionStatus(ExecutionStatus... filterExecutionStatus) {
        if (filterExecutionStatus != null && filterExecutionStatus.length > 0) {
            this.filterExecutionStatus = Arrays.asList(filterExecutionStatus);
        }
    }
    
    public void addFilterEventDetail(String eventDetail) {
        this.filterEventDetail = eventDetail;
    }
    
    public void addSortCtime(PageOrdering sort) {
        addSortField(SORT_FIELD_CTIME);
        this.sortCtime = sort;
    }
}

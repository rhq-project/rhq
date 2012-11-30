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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.content.Package;
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

    private Integer filterId;
    private String filterEventType;
    private String filterExecutionStatus;
    private String filterEventDetail;
        
    private PageOrdering sortCtime;
    
    public PartitionEventCriteria() {
        //        filterOverrides.put("packageTypeId", "packageType.id = ? ");
        //        filterOverrides.put("repoId", "id IN (" +
        //            "SELECT rpv.packageVersion.generalPackage.id FROM RepoPackageVersion rpv WHERE rpv.repo.id = ?" +
        //            ")");
    }
    
    public Class<?> getPersistentClass() {
        return Package.class;
    }

    public void addFilterId(Integer id) {
        this.filterId = id;
    }
    
    public void addFilterEventType(String eventType) {
        this.filterEventType = eventType;
    }
    
    public void addFilterExecutionStatus(String executionStatus) {
        this.filterExecutionStatus = executionStatus;
    }
    
    public void addFilterEventDetail(String eventDetail) {
        this.filterEventDetail = eventDetail;
    }
    
    public void addSortCtime(PageOrdering sort) {
        addSortField("ctime");
        this.sortCtime = sort;
    }
}

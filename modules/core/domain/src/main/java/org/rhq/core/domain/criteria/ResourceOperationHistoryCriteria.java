/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.criteria;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.operation.ResourceOperationHistory;

/**
 * @author Joseph Marques
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceOperationHistoryCriteria extends OperationHistoryCriteria {
    private static final long serialVersionUID = 1L;

    private List<Integer> filterResourceIds; // requires override
    private Integer filterGroupOperationHistoryId; // requires override

    private boolean fetchResults;

    public ResourceOperationHistoryCriteria() {
        filterOverrides.put("resourceIds", "resource.id IN ( ? )");
        filterOverrides.put("groupOperationHistoryId", "groupOperationHistory.id = ?");
    }

    @Override
    public Class<ResourceOperationHistory> getPersistentClass() {
        return ResourceOperationHistory.class;
    }

    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = Arrays.asList(filterResourceIds);
    }

    public void addFilterGroupOperationHistoryId(Integer groupOperationHistoryId) {
        this.filterGroupOperationHistoryId = groupOperationHistoryId;
    }

    public void fetchResults(boolean fetchResults) {
        this.fetchResults = fetchResults;
    }
}

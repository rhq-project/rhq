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
package org.rhq.core.domain.content.transfer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jason Dobies
 */
public class RemovePackagesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private int requestId;

    private ContentResponseResult overallRequestResult;
    private String overallRequestErrorMessage;

    private Set<RemoveIndividualPackageResponse> packageResponses = new HashSet<RemoveIndividualPackageResponse>();

    public RemovePackagesResponse() {
    }

    public RemovePackagesResponse(ContentResponseResult overallRequestResult) {
        setOverallRequestResult(overallRequestResult);
    }

    // Public  --------------------------------------------

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public ContentResponseResult getOverallRequestResult() {
        return overallRequestResult;
    }

    public void setOverallRequestResult(ContentResponseResult overallRequestResult) {
        if (overallRequestResult == null) {
            throw new IllegalArgumentException("overallRequestResult cannot be null");
        }

        this.overallRequestResult = overallRequestResult;
    }

    public String getOverallRequestErrorMessage() {
        return overallRequestErrorMessage;
    }

    public void setOverallRequestErrorMessage(String overallRequestErrorMessage) {
        this.overallRequestErrorMessage = overallRequestErrorMessage;
    }

    public Set<RemoveIndividualPackageResponse> getPackageResponses() {
        return packageResponses;
    }

    public void addPackageResponse(RemoveIndividualPackageResponse response) {
        packageResponses.add(response);
    }

    public String toString() {
        return "RemovePackagesResponse[id=" + requestId + ", result=" + overallRequestResult + "]";
    }
}
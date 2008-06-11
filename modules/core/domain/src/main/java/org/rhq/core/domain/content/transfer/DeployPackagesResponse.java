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
package org.rhq.core.domain.content.transfer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jason Dobies
 */
public class DeployPackagesResponse implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    private int requestId;

    private ContentResponseResult overallRequestResult;
    private String overallRequestErrorMessage;

    private Set<DeployIndividualPackageResponse> packageResponses = new HashSet<DeployIndividualPackageResponse>();

    // Constructors  --------------------------------------------

    public DeployPackagesResponse() {
    }

    public DeployPackagesResponse(ContentResponseResult overallRequestResult) {
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

    public Set<DeployIndividualPackageResponse> getPackageResponses() {
        return packageResponses;
    }

    public void addPackageResponse(DeployIndividualPackageResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response cannot be null");
        }

        packageResponses.add(response);
    }

    /**
     * Convienence method that sets the error message to the given throwable's stack trace dump. If the given throwable
     * is <code>null</code>, the error message will be set to <code>null</code> as if passing <code>null</code> to
     * {@link #setOverallRequestErrorMessage(String)}.
     *
     * @param t throwable whose message and stack trace will make up the error message (may be <code>null</code>)
     */
    public void setErrorMessageFromThrowable(Throwable t) {
        if (t != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            setOverallRequestErrorMessage(baos.toString());
        } else {
            setOverallRequestErrorMessage(null);
        }
    }
}
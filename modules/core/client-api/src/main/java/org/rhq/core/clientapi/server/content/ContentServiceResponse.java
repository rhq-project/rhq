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
package org.rhq.core.clientapi.server.content;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Set;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.transfer.RetrievePackageBitsRequest;

/**
 * Server/agent communications object to convey the result of a request for a content subsystem operation.
 *
 * @author Jason Dobies
 * @see    org.rhq.core.domain.content.transfer.DeployPackagesRequest
 * @see    org.rhq.core.domain.content.transfer.DeletePackagesRequest
 * @see    RetrievePackageBitsRequest
 */
public class ContentServiceResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    /**
     * Refers to the ID sent with the original request to deploy a new package.
     */
    private int requestId;

    /**
     * Indicates if the request was successful or failed.
     */
    private ContentRequestStatus status;

    /**
     * More detail in the case of a failure.
     */
    private String errorMessage;

    /**
     * Packages that were manipulated in this request.
     */
    private Set<InstalledPackage> installedPackages;

    // Constructors  --------------------------------------------

    /**
     * Convenience constructor for a response that conveys a success. The status will be set to successful.
     *
     * @param requestId ID of the original request
     */
    public ContentServiceResponse(int requestId) {
        this.requestId = requestId;
        this.status = ContentRequestStatus.SUCCESS;
    }

    /**
     * Convenience constructor for a response that conveys an error. The status will be set to error and the message
     * will be captured.
     *
     * @param requestId    request the response applies to
     * @param errorMessage message of the error encountered
     */
    public ContentServiceResponse(int requestId, String errorMessage) {
        this.requestId = requestId;
        this.status = ContentRequestStatus.FAILURE;
        this.errorMessage = errorMessage;
    }

    /**
     * Convenience constructor for a response that conveys an error. The status will be set to error and the message
     * will be that of the exception's stack trace.
     *
     * @param requestId request the response applies to
     * @param error     exception that occurred
     */
    public ContentServiceResponse(int requestId, Throwable error) {
        this.requestId = requestId;
        this.status = ContentRequestStatus.FAILURE;
        setErrorMessageFromThrowable(error);
    }

    // Public  --------------------------------------------

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public ContentRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ContentRequestStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Convienence method that sets the error message to the given throwable's stack trace dump. If the given throwable
     * is <code>null</code>, the error message will be set to <code>null</code> as if passing <code>null</code> to
     * {@link #setErrorMessage(String)}.
     *
     * @param t throwable whose message and stack trace will make up the error message (may be <code>null</code>)
     */
    public void setErrorMessageFromThrowable(Throwable t) {
        if (t != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            setErrorMessage(baos.toString());
        } else {
            setErrorMessage(null);
        }
    }

    public Set<InstalledPackage> getInstalledPackages() {
        return installedPackages;
    }

    public void setInstalledPackages(Set<InstalledPackage> installedPackages) {
        this.installedPackages = installedPackages;
    }
}
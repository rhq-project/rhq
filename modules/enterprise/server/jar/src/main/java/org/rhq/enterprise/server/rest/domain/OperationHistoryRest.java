/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * An operations history item
 * @author Heiko W. Rupp
 */
@ApiClass(value = "Denotes the outcome of an operation that has been scheduled to run.")
@XmlRootElement
public class OperationHistoryRest {

    String operationName;
    String resourceName;
    long lastModified;
    String status;
    String errorMessage;
    String jobId;

    Map<String,Object> result = new HashMap<String, Object>();
    List<Link> links = new ArrayList<Link>();

    @ApiProperty(value = "The status of the history", allowableValues = "In Progress, Cancelled, Failure, Success")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ApiProperty("Map with key/value pairs fo the operation result")
    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    @ApiProperty("Error message if the operation was not successful")
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @ApiProperty("Name of the operation that was run")
    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    @ApiProperty("Name of the resource the operation was run on")
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void lastModified(long date) {
        this.lastModified = date;
    }

    @ApiProperty("Job-ID of this history item")
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}

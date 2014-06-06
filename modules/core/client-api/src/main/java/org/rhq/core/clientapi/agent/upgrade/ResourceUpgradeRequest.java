/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.clientapi.agent.upgrade;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceUpgradeReport;

/**
 * Represents a request to upgrade a resource.
 *
 * @author Lukas Krejci
 */
public class ResourceUpgradeRequest extends ResourceUpgradeReport {

    private static final long serialVersionUID = 1L;

    private final int resourceId;
    private String upgradeErrorMessage;
    private String upgradeErrorStackTrace;
    private long timestamp;

    public ResourceUpgradeRequest(int resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceUpgradeRequest(int resourceId, ResourceUpgradeReport report) {
        this.resourceId = resourceId;
        fillInFromReport(report);
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getUpgradeErrorMessage() {
        return upgradeErrorMessage;
    }

    public void setUpgradeErrorMessage(String upgradeErrorMessage) {
        this.upgradeErrorMessage = upgradeErrorMessage;
    }

    public String getUpgradeErrorStackTrace() {
        return upgradeErrorStackTrace;
    }

    public void setUpgradeErrorStackTrace(String upgradeErrorStackTrace) {
        this.upgradeErrorStackTrace = upgradeErrorStackTrace;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setErrorProperties(Throwable t) {
        upgradeErrorMessage = null;
        upgradeErrorStackTrace = null;

        //first try to extract an error message from the exception
        //or some of its parents
        Throwable p = t;
        while (upgradeErrorMessage == null && p != null) {
            upgradeErrorMessage = p.getMessage();
            if (upgradeErrorMessage == null) {
                upgradeErrorMessage = p.getLocalizedMessage();
            }
            p = p.getCause();
        }

        //ok, we have no error messages. But we have to supply something
        //as the message, so let's use the class name as a last resort.
        if (upgradeErrorMessage == null) {
            if (t.getClass().getCanonicalName() != null) {
                upgradeErrorMessage = t.getClass().getCanonicalName();
            } else if (t.getClass().getName() != null) {
                upgradeErrorMessage = t.getClass().getName();
            }
        }

        //print the stack trace into a string
        StringWriter string = new StringWriter();
        PrintWriter w = new PrintWriter(string);
        t.printStackTrace(w);
        w.close();

        upgradeErrorStackTrace = string.toString();
    }

    public void fillInFromReport(ResourceUpgradeReport report) {
        setNewDescription(report.getNewDescription());
        setNewName(report.getNewName());
        setNewResourceKey(report.getNewResourceKey());
        setNewPluginConfiguration(report.getNewPluginConfiguration());
        setNewVersion(report.getNewVersion());
        setForceGenericPropertyUpgrade(report.isForceGenericPropertyUpgrade());
    }

    public void fillInFromResource(Resource resource) {
        setNewDescription(resource.getDescription());
        setNewName(resource.getName());
        setNewResourceKey(resource.getResourceKey());
        setNewPluginConfiguration(resource.getPluginConfiguration());
        setNewVersion(resource.getVersion());
    }

    public void updateResource(Resource resource) {
        if (getNewResourceKey() != null) {
            resource.setResourceKey(getNewResourceKey());
        }

        if (getNewName() != null) {
            resource.setName(getNewName());
        }

        if (getNewDescription() != null) {
            resource.setDescription(getNewDescription());
        }

        if (getNewPluginConfiguration() != null) {
            resource.setPluginConfiguration(getNewPluginConfiguration());
        }

        if (getNewVersion() != null) {
            resource.setVersion(getNewVersion());
        }
    }

    /**
     * Clears all the data to be upgraded apart from the error message, stacktrace and timestamp
     */
    public void clearUpgradeData() {
        setNewDescription(null);
        setNewName(null);
        setNewResourceKey(null);
        setNewPluginConfiguration(null);
        setNewVersion(null);
        setForceGenericPropertyUpgrade(false);
    }

    @Override
    public boolean hasSomethingToUpgrade() {
        return super.hasSomethingToUpgrade() || upgradeErrorMessage != null || upgradeErrorStackTrace != null;
    }

    @Override
    public int hashCode() {
        return 31 * resourceId;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof ResourceUpgradeRequest)) {
            return false;
        }

        ResourceUpgradeRequest r = (ResourceUpgradeRequest) other;

        return r.getResourceId() == resourceId;
    }

    @Override
    public String toString() {
        return "ResourceUpgradeRequest [resourceId=" + resourceId + ", upgradeErrorMessage=" + upgradeErrorMessage
            + ", upgradeErrorStackTrace=" + upgradeErrorStackTrace + ", timestamp=" + timestamp + ", toString()="
            + super.toString() + "]";
    }
}

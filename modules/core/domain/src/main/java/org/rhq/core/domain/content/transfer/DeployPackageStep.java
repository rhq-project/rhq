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

import java.io.Serializable;

/**
 * Represents a step that will be take in the installation of a package. The link to the particular package is known by
 * the call to the plugin to translate these; as such, there is no reference to a package or resource in this class.
 *
 * @author Jason Dobies
 */
public class DeployPackageStep implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    /**
     * Unique identifier for this step within the context of the overall package installation steps for a particular
     * package.
     */
    private String stepKey;

    /**
     * Textual description to be displayed to the user of what will occur when installing the package.
     */
    private String description;

    /**
     * Result of executing this step during a package deployment.
     */
    private ContentResponseResult stepResult;

    /**
     * Error message during execution of this step if one was encountered.
     */
    private String stepErrorMessage;

    // Public  --------------------------------------------

    public DeployPackageStep(String stepKey, String description) {
        if (stepKey == null)
            throw new IllegalArgumentException("stepKey cannot be null");

        if (description == null)
            throw new IllegalArgumentException("description cannot be null");

        this.stepKey = stepKey;
        this.description = description;
    }

    // Public  --------------------------------------------

    public String toString() {
        return "DeployPackageStep[stepId=" + stepKey + ", stepResult=" + stepResult + ", description=" + description
            + "]";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeployPackageStep that = (DeployPackageStep) o;

        if (!stepKey.equals(that.stepKey)) return false;

        return true;
    }

    public int hashCode() {
        return stepKey.hashCode();
    }

    public String getStepKey() {
        return stepKey;
    }

    public String getDescription() {
        return description;
    }

    public ContentResponseResult getStepResult() {
        return stepResult;
    }

    public void setStepResult(ContentResponseResult stepResult) {
        this.stepResult = stepResult;
    }

    public String getStepErrorMessage() {
        return stepErrorMessage;
    }

    public void setStepErrorMessage(String stepErrorMessage) {
        this.stepErrorMessage = stepErrorMessage;
    }
}
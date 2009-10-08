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
package org.rhq.core.pluginapi.configuration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;

/**
 * Represents the configuration update request and its results.
 *
 * @author John Mazzitelli
 * @see    ConfigurationFacet#updateResourceConfiguration(ConfigurationUpdateReport)
 */
public class ConfigurationUpdateReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private Configuration configuration;
    private ConfigurationUpdateStatus status;
    private String errorMessage;

    public ConfigurationUpdateReport(Configuration config) {
        configuration = config;
        status = ConfigurationUpdateStatus.INPROGRESS;
        errorMessage = null;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public ConfigurationUpdateStatus getStatus() {
        return status;
    }

    public void setStatus(ConfigurationUpdateStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Calling this method with a non-<code>null</code> error message implies that the request's status is
     * {@link ConfigurationUpdateStatus#FAILURE} and will set it as such. The inverse is <i>not</i> true - that is, if
     * you set the error message to <code>null</code>, the status is left as-is; it will not assume that a <code>
     * null</code> error message means the status is successful.
     *
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;

        if (this.errorMessage != null) {
            setStatus(ConfigurationUpdateStatus.FAILURE);
        }
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
}
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


/**
 * This exception can be thrown by instances of {@link ResourceConfigurationFacet} to indicate an update failed. This
 * exception can be throw by the validateXXX methods to indicate that validation failed or by the persistXXX method to
 * indicate that the actual update failed.
 * <br/><br/>
 * Any unchecked exception thrown from the validateXXX or from the persistXXX methods will be treated as an update
 * failure. When a ResourceConfigurationUpdateException is thrown, only the error messages associated with it are sent
 * back to the server whereas with any other exception, the entire stack trace is sent up to the server. The reason for
 * this is that with a ResourceConfigurationUpdateException, the plugin container assumes that the failure has been
 * handled in some fashion. For instance, catching any exceptions that might thrown from attempting to apply the update.
 * Or consider a scenario in which applying the update involves calling out to an external program. The failed update
 * might only be reported back with some error code.
 * <br/><br/>
 * With any other exception, the plugin container cannot reasonably assume that any errors were handled and as such
 * reports the entire stack trace back to the server to provide additional information for debugging if necessary.
 */
public class ResourceConfigurationUpdateException extends RuntimeException {

    public ResourceConfigurationUpdateException(String message) {
        super(message);
    }

}

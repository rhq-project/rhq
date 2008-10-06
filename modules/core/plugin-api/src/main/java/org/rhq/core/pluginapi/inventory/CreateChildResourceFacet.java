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
package org.rhq.core.pluginapi.inventory;

/**
 * Components that implement this facet have defined at least one of its child resources is creatable. Calls to create
 * any of the child resource types will be fielded by implementations of this interface.
 *
 * @author Jason Dobies
 */
public interface CreateChildResourceFacet {
    /**
     * <p>Creates a new resource. The report will contain all of the information necessary to configure the resource and
     * have it begin to function. If this call indicates success, the resource should be picked up by the next discovery
     * performed against the plugin.</p>
     *
     * <p>There are two different approaches to creating a resource, requiring either a set of configuration values for
     * the new resource or an artifact. The following description of how to interpret the status codes applies in both
     * cases. Implementers of this method should set the status code on the report appropriately and return the instance
     * at the end of the call.</p>
     *
     * <p>If the plugin was unable to create the resource for some reason, the report object will be returned with its
     * status set to {@link org.rhq.core.domain.resource.CreateResourceStatus#FAILURE}. More detail on the failure
     * should be specified in the report's error message. Additionally, if a configuration was specified as part of the
     * resource creation, it is possible one or more of the values failed plugin-side validation. In this case, the
     * <code>Configuration</code> inside the report should reflect the failed validations.</p>
     *
     * <p>If everything was successful (that is, the resource was created and was fully configured with the given set of
     * configuration values), then the returned report will indicate a
     * {@link org.rhq.core.domain.resource.CreateResourceStatus#SUCCESS}. Note that the returned report will <b>not</b>
     * have the new resource in it - that will be picked up in the next auto-discovery run (which should be kicked off
     * automatically by this method).</p>
     *
     * <p>Note that this method should not throw any exceptions. Any error conditions encountered should be indicated in
     * the report object through the status code and error message (as well as the configuration instance if
     * applicable).</p>
     *
     * <p>Prior to returning the report, the following should be set:</p>
     *
     * <ul>
     *   <li>Resource Key</li>
     *   <li>Status</li>
     *   <li>Any error messages (if applicable)</li>
     *   <li>Any exceptions that occurred (if applicable)</li>
     * </ul>
     *
     * @param  report contains all of the necessary information to create the specified resource and should be populated
     *                with the
     *
     * @return report specified in the call, with its status code updated to reflect the result of the create, along
     *         with any error messages if applicable
     */
    CreateResourceReport createResource(CreateResourceReport report);
}
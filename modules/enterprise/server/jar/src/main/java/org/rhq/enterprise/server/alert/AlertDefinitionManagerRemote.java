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
package org.rhq.enterprise.server.alert;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.FetchException;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface AlertDefinitionManagerRemote {
    @WebMethod
    AlertDefinition getAlertDefinition(
    //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "alertDefinitionId") int alertDefinitionId) //
        throws FetchException;

    /**
     * This find service can be used to find alert definitions based on various criteria and return various data.
     *
     * @param subject  The logged in user's subject.
     * @param criteria {@link AlertDefinition}
     * <pre>
     * If provided the AlertDefinition object can specify various search criteria as specified below.
     *   AlertDefinition.id : exact match
     *   AlertDefinition.name  : case insensitive substring match
     *   AlertDefinition.resourceId
     *   AlertDefinition.resourceType
     * </pre>
     * @param pc {@link PageControl}
     * <pre>
     * If provided PageControl specifies page size, requested page, sorting, and optional data.
     * 
     * Supported OptionalData
     *   To specify optional data call pc.setOptionalData() and supply one of more of the DATA_* constants
     *   defined in this interface.
     * 
     * Supported Sorting:
     *   Possible values to provide PageControl for sorting (PageControl.orderingFields)
     *     name
     *     resourceType
     *   
     * </pre>
     * @return
     * @throws FetchException
     */
    @WebMethod
    PageList<AlertDefinition> findAlertDefinitions( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") AlertDefinition criteria, //
        @WebParam(name = "pageControl") PageControl pageControl) //
        throws FetchException;

}

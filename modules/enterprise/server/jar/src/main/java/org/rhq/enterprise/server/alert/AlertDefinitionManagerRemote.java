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
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.system.ServerVersion;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface AlertDefinitionManagerRemote {
    @WebMethod
    AlertDefinition getAlertDefinition(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "alertDefinitionId") int alertDefinitionId);

    @WebMethod
    PageList<AlertDefinition> findAlertDefinitionsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") AlertDefinitionCriteria criteria);

    @WebMethod
    int enableAlertDefinitions(// 
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "alertDefinitionIds") int[] alertDefinitionIds);

    @WebMethod
    int disableAlertDefinitions(// 
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "alertDefinitionIds") int[] alertDefinitionIds);

    @WebMethod
    int removeAlertDefinitions(// 
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "alertDefinitionIds") int[] alertDefinitionIds);

    @WebMethod
    String[] getAlertNotificationConfigurationPreview(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "notifications") AlertNotification[] notifications);

}

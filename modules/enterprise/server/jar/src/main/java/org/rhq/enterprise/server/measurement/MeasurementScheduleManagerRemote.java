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
package org.rhq.enterprise.server.measurement;

import java.util.List;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.UpdateException;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface MeasurementScheduleManagerRemote {

    @WebMethod
    void disableSchedules(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "measurementDefinitionIds") int[] measurementDefinitionIds) //
        throws UpdateException;

    @WebMethod
    void disableSchedulesForCompatibleGroup(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "measurementDefinitionIds") int[] measurementDefinitionIds)//
        throws UpdateException;

    @WebMethod
    void enableSchedules(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "measurementDefinitionIds") int[] measurementDefinitionIds) //
        throws UpdateException;

    @WebMethod
    void enableSchedulesForCompatibleGroup(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "measurementDefinitionIds") int[] measurementDefinitionIds) //
        throws UpdateException;

    @WebMethod
    PageList<MeasurementScheduleComposite> findScheduleDefaultsForResourceType(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceTypeId") int resourceTypeId,//
        @WebParam(name = "pc") PageControl pageControl);

    @WebMethod
    List<MeasurementSchedule> findSchedulesByResourceIdsAndDefinitionId(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds,//
        @WebParam(name = "measurementDefinitionId") int measurementDefinitionId);

    @WebMethod
    PageList<MeasurementScheduleComposite> findSchedulesForAutoGroup(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "parentResourceId") int parentResourceId,//
        @WebParam(name = "resourceTypeId") int resourceTypeId,//
        @WebParam(name = "pc") PageControl pageControl);

    @WebMethod
    PageList<MeasurementScheduleComposite> findSchedulesForCompatibleGroup(//
        @WebParam(name = "subject") Subject subject,//
        @WebParam(name = "groupId") int groupId,//
        @WebParam(name = "pc") PageControl pageControl);

    @WebMethod
    PageList<MeasurementScheduleComposite> findSchedulesForResource(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId,//
        @WebParam(name = "pc") PageControl pageControl);

    @WebMethod
    MeasurementSchedule getScheduleById(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "scheduleId") int scheduleId);

}

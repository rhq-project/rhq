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
import java.util.Set;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.server.exception.FetchException;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface MeasurementDataManagerRemote {
    @WebMethod
    MeasurementAggregate getAggregate(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "scheduleId") int scheduleId, //
        @WebParam(name = "startTime") long startTime, //
        @WebParam(name = "endTime") long endTime) //
        throws FetchException;

    @WebMethod
    List<MeasurementDataTrait> findTraits(//
        @WebParam(name = "subject") Subject subject,//
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "definitionId") int definitionId) //
        throws FetchException;

    @WebMethod
    List<MeasurementDataTrait> findCurrentTraitsForResource(//
        @WebParam(name = "subject") Subject subject,//
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "displayType") DisplayType displayType) //
        throws FetchException;

    @WebMethod
    Set<MeasurementData> findLiveData(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "definitionIds") int[] definitionIds) //
        throws FetchException;

    @WebMethod
    List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId,//
        @WebParam(name = "definitionId") int definitionId, //
        @WebParam(name = "beginTime") long beginTime, //
        @WebParam(name = "endTime") long endTime, //
        @WebParam(name = "numPoints") int numPoints,//
        @WebParam(name = "groupAggregateOnly") boolean groupAggregateOnly)//
        throws FetchException;

    @WebMethod
    List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(//
        @WebParam(name = "subject") Subject subject,//
        @WebParam(name = "resourceId") int resourceId,//
        @WebParam(name = "definitionIds") int[] definitionIds,// 
        @WebParam(name = "beginTime") long beginTime, //
        @WebParam(name = "endTime") long endTime, //
        @WebParam(name = "numPoints") int numPoints) //
        throws FetchException;
}

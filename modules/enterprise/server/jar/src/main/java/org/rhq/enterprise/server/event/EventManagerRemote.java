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
package org.rhq.enterprise.server.event;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.FetchException;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface EventManagerRemote {

    @WebMethod
    EventSeverity[] getSeverityBuckets( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "begin") long begin, //
        @WebParam(name = "end") long end, //
        @WebParam(name = "numBuckets") int numBuckets) //
        throws FetchException;

    @WebMethod
    EventSeverity[] getSeverityBucketsForAutoGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "parentResourceId") int parentResourceId, //
        @WebParam(name = "resourceTypeId") int resourceTypeId, //
        @WebParam(name = "begin") long begin, //
        @WebParam(name = "end") long end, //
        @WebParam(name = "numBuckets") int numBuckets) //
        throws FetchException;

    @WebMethod
    EventSeverity[] getSeverityBucketsForCompGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceGroupId") int resourceGroupId, //
        @WebParam(name = "begin") long begin, //
        @WebParam(name = "end") long end, //
        @WebParam(name = "numBuckets") int numBuckets) //
        throws FetchException;

    @WebMethod
    PageList<Event> findEventsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") EventCriteria criteria);
}

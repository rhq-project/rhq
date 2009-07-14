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

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.FetchException;

/**
 * @author Noam Malki
 */

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface AvailabilityManagerRemote {

    /**
     * @param subject
     * @param resourceId
     * @param pc
     * @return
     * @throws FetchException
     */
    @WebMethod
    public PageList<Availability> findAvailabilityForResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "pageControl") PageControl pc) //
        throws FetchException;

    /**
     * Gets the last known Availability for the given resource - which includes whether it is currently up (i.e.
     * available) or down and the last time it was known to have changed to that state.
     * <b>Note:</b> only use this method if you really need to know the additional RLE information that
     * comes with the Availabilty entity.  If you really only need to know whether a resource is UP or DOWN,
     * then use the more efficient method {@link #getCurrentAvailabilityTypeForResource(Subject, int)}.
     * 
     * @param  subject
     * @param  resourceId
     *
     * @return the full and current status of the resource
     * @throws FetchException TODO
     * @throws FetchException
     */
    @WebMethod
    public Availability getCurrentAvailabilityForResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId) throws FetchException;

}

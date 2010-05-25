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
package org.rhq.enterprise.server.search;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * The remote interface to the SavedSearchManager.
 *
 * @author Joseph Marques
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface SavedSearchManagerRemote {

    /**
     * Persisted a new {@link SavedSearch} with the given primary key
     *
     * @param subject       the logged in user requesting the {@link SavedSearch} deletion
     * @param savedSearch   the primary key of the {@link SavedSearch} to be deleted
     *
     * @throws PermissionException if the user is not authorized to create the {@link SavedSearch}.  Only inventory
     *         managers can create global saved searches.  Regular users can only create {@link SavedSearch}es against
     *         their own accounts.
     */
    @WebMethod
    public int createSavedSearch( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "savedSearch") SavedSearch savedSearch);

    /**
     * Saves all changes to the passed {@link SavedSearch} database, correlating it to the record already
     * persisted with the same primary key
     *
     * @param subject       the logged in user requesting the {@link SavedSearch} persisted modification
     * @param savedSearch   the {@link SavedSearch} which will have its modifications persisted
     *.
     * @throws PermissionException if the user is not authorized to modify the {@link SavedSearch}.  Only inventory
     *         managers can update global saved searches.  Regular users can only update {@link SavedSearch}es from
     *         their own accounts.
     */
    @WebMethod
    public void updateSavedSearch( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "savedSearch") SavedSearch savedSearch);

    /**
     * Deletes the {@link SavedSearch} with the given primary key
     *
     * @param subject       the logged in user requesting the {@link SavedSearch} deletion
     * @param savedSearchId the primary key of the {@link SavedSearch} to be deleted
     *
     * @throws PermissionException if the user is not authorized to delete the {@link SavedSearch}.  Only inventory
     *         managers can delete global saved searches.  Regular users can only delete {@link SavedSearch}es from
     *         their own accounts.
     */
    @WebMethod
    public void deleteSavedSearch( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "savedSearchId") int savedSearchId);

    /**
     * Returns the {@link SavedSearch} with the given primary key
     *
     * @param subject       the logged in user requesting the {@link SavedSearch} to be loaded
     * @param savedSearchId the primary key of the {@link SavedSearch} to be loaded
     *
     * @return the {@link SavedSearch} or <code>null</code> if it wasn't found
     * @throws PermissionException if the user is not authorized to view the {@link SavedSearch}.  Regular users can
     *         only view {@link SavedSearch}es from their own accounts.
     */
    @WebMethod
    public SavedSearch getSavedSearchById( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "savedSearchId") int savedSearchId);

    /**
     * Returns the {@link PageList} of {@link SavedSearch} entities that match the criteria filters that are visible
     * to the user
     *
     * @param subject  the logged in user requesting the {@link PageList} of {@link SavedSearch} to be returned
     * @param criteria the {@link SavedSearchCriteria} object that will filter the returned results
     *
     * @return the {@link PageList} of {@link SavedSearch} entities that match the criteria filters, an empty list
     *         will be returned if no results were found or none matches the given filters
     */
    @WebMethod
    public PageList<SavedSearch> findSavedSearchesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") SavedSearchCriteria criteria);
}

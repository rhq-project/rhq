/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.drift;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.ejb.Local;

import org.rhq.common.drift.Headers;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;

/**
 * The SLSB methods needed to support the JPA (RHQ Default) Drift Server Plugin.
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
@Local
public interface JPADriftServerLocal {

    /**
     * Simple get method for a JPADriftFile. Does not return the content.
     * @param subject
     * @param sha256
     * @return The JPADriftFile sans content.
     */
    JPADriftFile getDriftFile(Subject subject, String sha256);

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The DriftChangeSets matching the criteria
     */
    PageList<JPADriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria);

    PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria);

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The Drifts matching the criteria
     */
    PageList<JPADrift> findDriftsByCriteria(Subject subject, DriftCriteria criteria);

    String persistChangeSet(Subject subject, DriftChangeSet<?> changeSet);

    String copyChangeSet(Subject subject, String changeSetId, int driftDefId, int resourceId);

    /**
     * SUPPORTS JPA DRIFT SERVER PLUGIN 
     * @param driftFile
     * @return
     * @throws Exception
     */
    JPADriftFile persistDriftFile(JPADriftFile driftFile) throws Exception;

    /**
     * SUPPORTS JPA DRIFT SERVER PLUGIN
     * @param driftFile
     * @param data
     * @throws Exception
     */
    void persistDriftFileData(JPADriftFile driftFile, InputStream data, long numBytes) throws Exception;

    /**
     * This method stores the provided change-set file for the resource. The version will be incremented based
     * on the max version of existing change-sets for the resource. The change-set will be processed generating
     * requests for drift file content and/or drift instances as required.
     *  
     * @param resourceId The resource for which the change-set is being reported.
     * @param changeSetZip The change-set zip file
     * @return a summary of the change set that was persisted
     * @throws Exception
     */
    DriftChangeSetSummary storeChangeSet(Subject subject, int resourceId, File changeSetZip) throws Exception;

    /**
     * For transactioning purposes only, part of storeChangeSet impl. Not to be exposed outside of local interface. 
     */
    DriftChangeSetSummary storeChangeSetInNewTransaction(Subject subject, int resourceId, File changeSetZip,
        List<JPADriftFile> driftFilesToRequest, Headers[] headers) throws Exception;

    /**
     * For transactioning purposes only, part of storeChangeSet impl. Not to be exposed outside of local interface. 
     */
    void ackChangeSetInNewTransaction(Subject subject, int resourceId, Headers headers,
        List<JPADriftFile> driftFilesToRequest) throws Exception;

    /**
     * This method stores the provided drift files. The files should correspond to requested drift files.
     * The unzipped files will have their sha256 generated. Those not corresponding to needed content will
     * be logged and ignored.
     *  
     * @param filesZip The change-set zip file
     * @throws Exception
     */
    void storeFiles(Subject subject, File filesZip) throws Exception;

    /**
     * SUPPORTS JPA DRIFT SERVER PLUGIN
     * This is for internal use only - do not call it unless you know what you are doing.
     * This purges all drift entities and changeset entities associated with the drift definition.
     */
    void purgeByDriftDefinitionName(Subject subject, int resourceId, String driftDefName) throws Exception;

    String getDriftFileBits(String hash);

    byte[] getDriftFileAsByteArray(String hash);
}

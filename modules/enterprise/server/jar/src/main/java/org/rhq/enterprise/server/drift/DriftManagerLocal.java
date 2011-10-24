/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.drift;

import java.io.File;
import java.io.InputStream;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComposite;
import org.rhq.core.domain.drift.DriftDetails;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;

@Local
public interface DriftManagerLocal extends DriftManagerRemote {

    /**
     * This method initiates an out-of-band (JMS-Based) server-side pull of the change-set file. Upon successful
     * upload of the change-set, it is processed. This may in turn generated requests for drift files to
     * be persisted.
     *  
     * @param resourceId The resource for which the change-set is being reported.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The change-set zip file stream
     * @throws Exception
     */
    void addChangeSet(Subject subject, int resourceId, long zipSize, InputStream zipStream) throws Exception;

    /**
     * This method initiates an out-of-band (JMS-Based) server-side pull of the drift file zip. Upon successful
     * upload of the zip, the files are stored.
     *  
     * @param resourceId The resource from which the drift file is being supplied.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The drift files zip file stream
     * @throws Exception
     */
    void addFiles(Subject subject, int resourceId, String driftDefName, String token, long zipSize,
        InputStream zipStream) throws Exception;

    /**
     * This is for internal use only - do not call it unless you know what you are doing.
     */
    void deleteResourceDriftDefinition(Subject subject, int resourceId, int driftDefId);

    /**
     * One time on-demand request to detect drift on the specified entities, using the supplied def.
     * 
     * @param entityContext
     * @param driftDef
     * @throws RuntimeException
     */
    void detectDrift(Subject subject, EntityContext context, DriftDefinition driftDef);

    PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria);

    PageList<DriftDefinitionComposite> findDriftDefinitionCompositesByCriteria(Subject subject,
        DriftDefinitionCriteria criteria);

    /**
     * Get the specified drift definition. Note, the full Configuration is fetched. 
     * 
     * @param driftDefId
     * @return The drift definition
     * @throws RuntimeException, IllegalArgumentException if entity or driftDef not found.
     */
    DriftDefinition getDriftDefinition(Subject subject, int driftDefId);

    /**
     * Returns an object that encapsulates the information needed for viewing drift details
     *
     * @param subject
     * @param driftId
     * @return
     */
    DriftDetails getDriftDetails(Subject subject, String driftId);

    DriftFile getDriftFile(Subject subject, String hashId) throws Exception;

    /**
     * Returns the content associated with the specified hash as a string
     *
     * @param hash The hash the uniquely identifies the requested content
     * @return The content as a string
     */
    String getDriftFileBits(Subject subject, String hash);

    boolean isBinaryFile(Subject subject, Drift<?, ?> drift);

    String persistSnapshot(Subject subject, DriftSnapshot snapshot, DriftChangeSet<? extends Drift<?, ?>> changeSet);

    void processRepeatChangeSet(int resourceId, String driftDefName, int version);

    /**
     * When a user wants to completely remove all data related to a drift definition,
     * this method will be called to give the plugin a chance to clean up any data related
     * to the drift definition that is going to be deleted.
     * @param Subject
     * @param resourceId the resource whose drift definition is being purged
     * @param driftDefName identifies the data that is to be purged
     */
    void purgeByDriftDefinitionName(Subject subject, int resourceId, String driftDefName) throws Exception;

    void saveChangeSetContent(Subject subject, int resourceId, String driftDefName, String token, File changeSetFilesZip)
        throws Exception;

    DriftChangeSetSummary saveChangeSet(Subject subject, int resourceId, File changeSetZip) throws Exception;

    void saveChangeSetFiles(Subject subject, File changeSetFilesZip) throws Exception;

    void updateDriftDefinition(DriftDefinition driftDefinition);

    /**
     * Update the provided driftDef (identified by name) on the specified EntityContext.  If it exists it will be replaced. If not it will
     * be added.  Agents, if available, will be notified of the change. 
     * @param subject
     * @param entityContext
     * @param driftDef
     */
    void updateDriftDefinition(Subject subject, EntityContext entityContext, DriftDefinition driftDef);

    /**
     * This will remove all drift files that are no longer referenced by drift entries. This is a maintenance method
     * to help reclaim space on the backend.
     * 
     * @param subject
     * @param purgeMillis only those unused drift files that are older than this (in epoch millis) will be purged.
     * @return number of orphaned drife files that were removed
     */
    int purgeOrphanedDriftFiles(Subject subject, long purgeMillis);

}

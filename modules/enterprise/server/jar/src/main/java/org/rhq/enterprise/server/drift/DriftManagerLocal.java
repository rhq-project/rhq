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

import java.io.InputStream;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftConfigurationCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftDetails;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;

@Local
public interface DriftManagerLocal extends DriftServerPluginFacet, DriftManagerRemote {

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
    void addChangeSet(int resourceId, long zipSize, InputStream zipStream) throws Exception;

    /**
     * This method initiates an out-of-band (JMS-Based) server-side pull of the drift file zip. Upon successful
     * upload of the zip, the files are stored.
     *  
     * @param resourceId The resource from which the drift file is being supplied.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The drift files zip file stream
     * @throws Exception
     */
    void addFiles(int resourceId, long zipSize, InputStream zipStream) throws Exception;

    /**
     * Remove the provided driftConfig (identified by name) on the specified entityContext.
     * Agents, if available, will be notified of the change. 
     * @param subject
     * @param entityContext
     * @param driftConfigName
     */
    void deleteDriftConfiguration(Subject subject, EntityContext entityContext, String driftConfigName);

    /**
     * This is for internal use only - do not call it unless you know what you are doing.
     */
    void deleteResourceDriftConfiguration(Subject subject, int resourceId, int driftConfigId);

    /**
     * One time on-demand request to detect drift on the specified entities, using the supplied config.
     * 
     * @param entityContext
     * @param driftConfig
     * @throws RuntimeException
     */
    void detectDrift(Subject subject, EntityContext context, DriftConfiguration driftConfig);

    PageList<DriftConfiguration> findDriftConfigurationsByCriteria(Subject subject, DriftConfigurationCriteria criteria);

    /**
     * Get the specified drift configuration. Note, the full Configuration is fetched. 
     * 
     * @param driftConfigId
     * @return The drift configuration
     * @throws RuntimeException, IllegalArgumentException if entity or driftConfig not found.
     */
    DriftConfiguration getDriftConfiguration(Subject subject, int driftConfigId);

    /**
     * Update the provided driftConfig (identified by name) on the specified EntityContext.  If it exists it will be replaced. If not it will
     * be added.  Agents, if available, will be notified of the change. 
     * @param subject
     * @param entityContext
     * @param driftConfig
     */
    void updateDriftConfiguration(Subject subject, EntityContext entityContext, DriftConfiguration driftConfig);

    /**
     * This will remove all drift files that are no longer referenced by drift entries. This is a maintenance method
     * to help reclaim space on the backend.
     * 
     * @param subject
     * @param purgeMillis only those unused drift files that are older than this (in epoch millis) will be purged.
     * @return number of orphaned drife files that were removed
     */
    int purgeOrphanedDriftFiles(Subject subject, long purgeMillis);

    /**
     * Returns the content associated with the specified hash as a string
     *
     * @param hash The hash the uniquely identifies the requested content
     * @return The content as a string
     */
    String getDriftFileBits(String hash);

    /**
     * Generates a unified diff of the two files references by drift. In the case of a
     * modified file, a Drift object references the current and previous versions of the
     * file. This method generates a diff of the two versions.
     *
     * @param drift Specifies the two files that will be compared
     * @return A report containing a unified diff of the two versions of the file
     * referenced by drift
     */
    FileDiffReport generateUnifiedDiff(Drift<?, ?> drift);

    /**
     * Returns an object that encapsulates the information needed for viewing drift details
     *
     * @param subject
     * @param driftId
     * @return
     */
    DriftDetails getDriftDetails(Subject subject, String driftId);

    boolean isBinaryFile(Drift<?, ?> drift);
}

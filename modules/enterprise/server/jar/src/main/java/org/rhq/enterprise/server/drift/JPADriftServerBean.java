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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.JPADriftChangeSetCriteria;
import org.rhq.core.domain.criteria.JPADriftCriteria;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftFileBits;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.StopWatch;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static org.rhq.core.domain.drift.DriftFileStatus.LOADED;

/**
 * The SLSB method implementation needed to support the JPA (RHQ Default) Drift Server Plugin.
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
@Stateless
public class JPADriftServerBean implements JPADriftServerLocal {
    private final Log log = LogFactory.getLog(this.getClass());

    @EJB
    AgentManagerLocal agentManager;

    @EJB
    JPADriftServerLocal JPADriftServer;

    @EJB
    SubjectManagerLocal subjectManager;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public int purgeOrphanedDriftFiles(Subject subject, long purgeMillis) {
        Query q = entityManager.createNativeQuery(JPADriftFile.NATIVE_DELETE_ORPHANED_DRIFT_FILES);
        q.setParameter(1, purgeMillis);
        int count = q.executeUpdate();
        log.debug("purged [" + count + "] drift files that were orphaned (that is, no longer referenced by drift)");
        return count;
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void purgeByDriftConfigurationName(Subject subject, int resourceId, String driftConfigName) throws Exception {

        int driftsDeleted;
        int changeSetsDeleted;
        StopWatch timer = new StopWatch();

        // purge all drift entities first
        Query q = entityManager.createNamedQuery(JPADrift.QUERY_DELETE_BY_DRIFTCONFIG_RESOURCE);
        q.setParameter("resourceId", resourceId);
        q.setParameter("driftConfigurationName", driftConfigName);
        driftsDeleted = q.executeUpdate();

        // now purge all changesets
        q = entityManager.createNamedQuery(JPADriftChangeSet.QUERY_DELETE_BY_DRIFTCONFIG_RESOURCE);
        q.setParameter("resourceId", resourceId);
        q.setParameter("driftConfigurationName", driftConfigName);
        changeSetsDeleted = q.executeUpdate();

        log.info("Purged [" + driftsDeleted + "] drift items and [" + changeSetsDeleted
            + "] changesets associated with drift config [" + driftConfigName + "] from resource [" + resourceId
            + "]. Elapsed time=[" + timer.getElapsed() + "]ms");
        return;
    }

    @Override
    public DriftSnapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria) {
        // TODO security checks
        DriftSnapshot snapshot = new DriftSnapshot();
        PageList<? extends DriftChangeSet<?>> changeSets = findDriftChangeSetsByCriteria(subject, criteria);

        for (DriftChangeSet<?> changeSet : changeSets) {
            snapshot.add(changeSet);
        }

        return snapshot;
    }

    @Override
    public PageList<JPADriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria) {

        JPADriftChangeSetCriteria jpaCriteria = (criteria instanceof JPADriftChangeSetCriteria) ? (JPADriftChangeSetCriteria) criteria
            : new JPADriftChangeSetCriteria(criteria);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, jpaCriteria);
        CriteriaQueryRunner<JPADriftChangeSet> queryRunner = new CriteriaQueryRunner<JPADriftChangeSet>(jpaCriteria,
            generator, entityManager);
        PageList<JPADriftChangeSet> result = queryRunner.execute();

        return result;
    }

    @Override
    public PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria) {

        JPADriftCriteria jpaCriteria = (criteria instanceof JPADriftCriteria) ? (JPADriftCriteria) criteria
            : new JPADriftCriteria(criteria);

        jpaCriteria.fetchChangeSet(true);
        PageList<JPADrift> drifts = findDriftsByCriteria(subject, jpaCriteria);
        PageList<DriftComposite> result = new PageList<DriftComposite>();
        for (JPADrift drift : drifts) {
            JPADriftChangeSet changeSet = drift.getChangeSet();
            DriftConfiguration driftConfig = changeSet.getDriftConfiguration();
            result.add(new DriftComposite(drift, changeSet.getResource(), driftConfig.getName()));
        }

        return result;
    }

    @Override
    public PageList<JPADrift> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {

        JPADriftCriteria jpaCriteria = (criteria instanceof JPADriftCriteria) ? (JPADriftCriteria) criteria
            : new JPADriftCriteria(criteria);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, jpaCriteria);
        CriteriaQueryRunner<JPADrift> queryRunner = new CriteriaQueryRunner<JPADrift>(jpaCriteria, generator,
            entityManager);
        PageList<JPADrift> result = queryRunner.execute();

        return result;
    }

    @Override
    public JPADriftFile getDriftFile(Subject subject, String sha256) {
        JPADriftFile result = entityManager.find(JPADriftFile.class, sha256);
        return result;
    }

    @Override
    public JPADriftFile persistDriftFile(JPADriftFile driftFile) {

        entityManager.persist(driftFile);
        return driftFile;
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void persistDriftFileData(JPADriftFile driftFile, InputStream data, long numBytes) throws Exception {

        JPADriftFileBits df = entityManager.find(JPADriftFileBits.class, driftFile.getHashId());
        if (null == df) {
            throw new IllegalArgumentException("JPADriftFile not found [" + driftFile.getHashId() + "]");
        }
        df.setDataSize(numBytes);
        df.setData(Hibernate.createBlob(new BufferedInputStream(data)));
        df.setStatus(LOADED);
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public DriftChangeSetSummary storeChangeSet(Subject subject, final int resourceId, final File changeSetZip)
        throws Exception {
        final Resource resource = entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new IllegalArgumentException("Resource not found [" + resourceId + "]");
        }

        final DriftChangeSetSummary summary = new DriftChangeSetSummary();

        try {
            ZipUtil.walkZipFile(changeSetZip, new ChangeSetFileVisitor() {

                @Override
                public boolean visit(ZipEntry zipEntry, ZipInputStream stream) throws Exception {
                    List<JPADriftFile> emptyDriftFiles = new ArrayList<JPADriftFile>();
                    JPADriftChangeSet driftChangeSet = null;

                    try {
                        ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new InputStreamReader(
                            stream)), false);

                        // store the new change set info (not the actual blob)
                        DriftConfiguration config = findDriftConfiguration(resource, reader.getHeaders());
                        if (config == null) {
                            log.error("Unable to locate DriftConfiguration for Resource [" + resource
                                + "]. Change set cannot be saved.");
                            return false;
                        }
                        // Commenting out the following line for now. We want to set the
                        // version to the value specified in the headers, but we also
                        // want to get the latest version we have in the database so that
                        // we can make sure that the agent is in sync with the server.
                        //
                        //int version = getChangeSetVersion(resource, config);
                        int version = reader.getHeaders().getVersion();

                        DriftChangeSetCategory category = reader.getHeaders().getType();
                        driftChangeSet = new JPADriftChangeSet(resource, version, category, config);
                        entityManager.persist(driftChangeSet);

                        summary.setCategory(category);
                        summary.setResourceId(resourceId);
                        summary.setDriftConfigurationName(reader.getHeaders().getDriftConfigurationName());
                        summary.setDriftHandlingMode(config.getDriftHandlingMode());
                        summary.setCreatedTime(driftChangeSet.getCtime());

                        for (FileEntry entry : reader) {
                            JPADriftFile oldDriftFile = getDriftFile(entry.getOldSHA(), emptyDriftFiles);
                            JPADriftFile newDriftFile = getDriftFile(entry.getNewSHA(), emptyDriftFiles);

                            // TODO Figure out an efficient way to save coverage change sets.
                            // The initial/coverage change set could contain hundreds or even thousands
                            // of entries. We probably want to consider doing some kind of batch insert
                            //
                            // jsanda

                            // use a path with only forward slashing to ensure consistent paths across reports
                            String path = FileUtil.useForwardSlash(entry.getFile());
                            JPADrift drift = new JPADrift(driftChangeSet, path, entry.getType(), oldDriftFile,
                                newDriftFile);
                            entityManager.persist(drift);

                            // we are taking advantage of the fact that we know the summary is only used by the server
                            // if the change set is a DRIFT report. If its a coverage report, it is not used (we do
                            // not alert on coverage reports) - so don't waste memory by collecting all the paths
                            // when we know they aren't going to be used anyway.
                            if (category == DriftChangeSetCategory.DRIFT) {
                                summary.addDriftPathname(path);
                            }
                        }

                        AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
                        DriftAgentService service = agentClient.getDriftAgentService();

                        service.ackChangeSet(resourceId, reader.getHeaders().getDriftConfigurationName());

                        // send a message to the agent requesting the empty JPADriftFile content
                        if (!emptyDriftFiles.isEmpty()) {
                            try {
                                if (service.requestDriftFiles(resourceId, reader.getHeaders(), emptyDriftFiles)) {
                                    for (DriftFile driftFile : emptyDriftFiles) {
                                        driftFile.setStatus(DriftFileStatus.REQUESTED);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn(" Unable to inform agent of drift file request  [" + emptyDriftFiles + "]", e);
                            }
                        }
                    } catch (Exception e) {
                        String msg = "Failed to store drift changeset [" + driftChangeSet + "]";
                        log.error(msg, e);
                        return false;
                    }

                    return true;
                }
            });

            return summary;

        } catch (Exception e) {
            String msg = "Failed to store drift changeset for ";
            if (null != resource) {
                msg += resource;
            } else {
                msg += ("resourceId " + resourceId);
            }
            log.error(msg, e);

            return null;
        } finally {
            // delete the changeSetFile?
        }
    }

    private JPADriftFile getDriftFile(String sha256, List<JPADriftFile> emptyDriftFiles) {
        JPADriftFile result = null;

        if (null == sha256 || "0".equals(sha256)) {
            return result;
        }

        result = entityManager.find(JPADriftFile.class, sha256);
        // if the JPADriftFile is not yet in the db, then it needs to be fetched from the agent
        if (null == result) {
            result = persistDriftFile(new JPADriftFile(sha256));
            emptyDriftFiles.add(result);
        }

        return result;
    }

    /**
     * This method only exists temporarily until the version header is added to the change
     * set meta data file. This method determines the version by looking at the number of
     * change sets in the database.
     *
     * @param r The resource
     * @param c The drift configuration
     * @return The next change set version number
     */
    private int getChangeSetVersion(Resource r, DriftConfiguration c) {
        JPADriftChangeSetCriteria criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterResourceId(r.getId());
        criteria.addFilterDriftConfigurationId(c.getId());
        List<? extends DriftChangeSet<?>> changeSets = findDriftChangeSetsByCriteria(subjectManager.getOverlord(),
            criteria);

        return changeSets.size();
    }

    private DriftConfiguration findDriftConfiguration(Resource resource, Headers headers) {
        for (DriftConfiguration driftConfig : resource.getDriftConfigurations()) {
            if (driftConfig.getName().equals(headers.getDriftConfigurationName())) {
                return driftConfig;
            }
        }
        return null;
    }

    private abstract class ChangeSetFileVisitor implements ZipUtil.ZipEntryVisitor {
    }

    @Override
    public void storeFiles(Subject subject, File filesZip) throws Exception {
        // No longer using ZipUtil.walkZipFile because an IOException was getting thrown
        // after reading the first entry, resulting in subsequent entries being skipped.
        // DriftFileVisitor passed the ZipInputStream to Hibernate.createBlob, and either
        // Hibernate, the JDBC driver, or something else is closing the stream which in
        // turn causes the exception.
        //
        // jsanda

        String zipFileName = filesZip.getName();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File dir = new File(tmpDir, zipFileName.substring(0, zipFileName.indexOf(".")));
        dir.mkdir();

        ZipUtil.unzipFile(filesZip, dir);
        for (File file : dir.listFiles()) {
            JPADriftFile driftFile = new JPADriftFile(file.getName());
            try {
                JPADriftServer.persistDriftFileData(driftFile, new FileInputStream(file), file.length());
            } catch (Exception e) {
                LogFactory.getLog(getClass()).info("Skipping bad drift file", e);
            }
        }

        for (File file : dir.listFiles()) {
            file.delete();
        }
        boolean deleted = dir.delete();
        if (!deleted) {
            LogFactory.getLog(getClass()).info(
                "Unable to delete " + dir.getAbsolutePath() + ". This directory and "
                    + "its contents are no longer needed. It can be deleted.");
        }
    }

    @Override
    public String getDriftFileBits(String hash) {
        // TODO add security
        try {
            JPADriftFileBits content = (JPADriftFileBits) entityManager.createNamedQuery(
                JPADriftFileBits.QUERY_FIND_BY_ID).setParameter("hashId", hash).getSingleResult();
            if (content.getDataSize() == null || content.getDataSize() < 1) {
                return "";
            }
            byte[] bytes = StreamUtil.slurp(content.getBlob().getBinaryStream());
            return new String(bytes);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

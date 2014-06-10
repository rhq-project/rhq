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

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftFileStatus.LOADED;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

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
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftFileBits;
import org.rhq.core.domain.drift.JPADriftSet;
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
    public void purgeByDriftDefinitionName(Subject subject, int resourceId, String driftDefName) throws Exception {

        int driftsDeleted;
        int changeSetsDeleted;
        StopWatch timer = new StopWatch();

        // purge all drift entities first
        Query q = entityManager.createNamedQuery(JPADrift.QUERY_DELETE_BY_DRIFTDEF_RESOURCE);
        q.setParameter("resourceId", resourceId);
        q.setParameter("driftDefinitionName", driftDefName);
        driftsDeleted = q.executeUpdate();

        // delete the drift set
        //        JPADriftChangeSet changeSet = entityManager.createQuery(
        //            "select c from JPADriftChangeSet c where c.version = 0 and c.driftDefinition")

        // now purge all changesets
        q = entityManager.createNamedQuery(JPADriftChangeSet.QUERY_DELETE_BY_DRIFTDEF_RESOURCE);
        q.setParameter("resourceId", resourceId);
        q.setParameter("driftDefinitionName", driftDefName);
        changeSetsDeleted = q.executeUpdate();

        log.info("Purged [" + driftsDeleted + "] drift items and [" + changeSetsDeleted
            + "] changesets associated with drift def [" + driftDefName + "] from resource [" + resourceId
            + "]. Elapsed time=[" + timer.getElapsed() + "]ms");
        return;
    }

    @Override
    public PageList<JPADriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria) {

        JPADriftChangeSetCriteria jpaCriteria = (criteria instanceof JPADriftChangeSetCriteria) ? (JPADriftChangeSetCriteria) criteria
            : new JPADriftChangeSetCriteria(criteria);

        // If looking for the initial change set make sure version is to to 0
        if (criteria.getFilterCategory() != null && criteria.getFilterCategory() == COVERAGE) {

            // If fetching Drifts then make sure we go through the DriftSet. Note that there is no guarantee
            // that the fetched Drift will refer to the ChangeSets found, because it may refer to a pinned
            // template's changeset and be shared amongst changesets.
            if (jpaCriteria.isFetchDrifts()) {
                jpaCriteria.fetchInitialDriftSet(true);
                jpaCriteria.fetchDrifts(false);
            }
            jpaCriteria.addFilterVersion("0");

        } else {
            jpaCriteria.fetchInitialDriftSet(false);
        }

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
            DriftDefinition driftDef = changeSet.getDriftDefinition();
            result.add(new DriftComposite(drift, changeSet.getResource(), driftDef.getName()));
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
    public String persistChangeSet(Subject subject, DriftChangeSet<?> changeSet) {
        JPADriftChangeSet jpaChangeSet;

        if (isTemplateChangeSet(changeSet)) {
            jpaChangeSet = new JPADriftChangeSet(null, changeSet.getVersion(), changeSet.getCategory(), null);
            jpaChangeSet.setDriftHandlingMode(changeSet.getDriftHandlingMode());
        } else {
            Resource resource = getResource(changeSet.getResourceId());
            DriftDefinition driftDef = null;

            for (DriftDefinition def : resource.getDriftDefinitions()) {
                if (def.getId() == changeSet.getDriftDefinitionId()) {
                    driftDef = def;
                    break;
                }
            }

            jpaChangeSet = new JPADriftChangeSet(resource, changeSet.getVersion(), changeSet.getCategory(), driftDef);
        }

        JPADriftSet driftSet = new JPADriftSet();

        for (Drift<?, ?> drift : changeSet.getDrifts()) {
            JPADrift jpaDrift = new JPADrift(jpaChangeSet, drift.getPath(), drift.getCategory(), toJPADriftFile(drift
                .getOldDriftFile()), toJPADriftFile(drift.getNewDriftFile()));

            driftSet.addDrift(jpaDrift);
        }
        entityManager.persist(jpaChangeSet);

        entityManager.persist(driftSet);

        jpaChangeSet.setInitialDriftSet(driftSet);

        return jpaChangeSet.getId();
    }

    private boolean isTemplateChangeSet(DriftChangeSet<?> changeSet) {
        return changeSet.getResourceId() == 0 && changeSet.getDriftDefinitionId() == 0;
    }

    @Override
    public String copyChangeSet(Subject subject, String changeSetId, int driftDefId, int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        DriftDefinition driftDef = entityManager.find(DriftDefinition.class, driftDefId);
        JPADriftChangeSet srcChangeSet = entityManager.find(JPADriftChangeSet.class, Integer.parseInt(changeSetId));
        JPADriftChangeSet destChangeSet = new JPADriftChangeSet(resource, 0, COVERAGE, driftDef);
        destChangeSet.setDriftHandlingMode(DriftConfigurationDefinition.DriftHandlingMode.normal);
        destChangeSet.setInitialDriftSet(srcChangeSet.getInitialDriftSet());

        entityManager.persist(destChangeSet);

        return destChangeSet.getId();
    }

    private JPADriftFile toJPADriftFile(DriftFile driftFile) {
        if (driftFile == null) {
            return null;
        }
        JPADriftFile jpaFile = new JPADriftFile(driftFile.getHashId());
        jpaFile.setDataSize(driftFile.getDataSize());
        jpaFile.setStatus(driftFile.getStatus());
        return jpaFile;
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
        Session session = (Session)entityManager.getDelegate();
        df.setDataSize(numBytes);
        df.setData(session.getLobHelper().createBlob(new BufferedInputStream(data), numBytes));
        df.setStatus(LOADED);
    }

    // This facade does not start, or participate in, a transaction so that it can execute its work
    // in two new transactions.  The first transaction ensures all new entities are committed to the
    // database.  The second transaction can then safely acknowledge that the changeset is persisted
    // and request drift file content, if necessary.
    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public DriftChangeSetSummary storeChangeSet(Subject subject, final int resourceId, final File changeSetZip)
        throws Exception {

        // a List to be populated by storeChangeSetInNewTransaction for use in ackChangeSetInNewTransaction
        List<JPADriftFile> driftFilesToRequest = new ArrayList<JPADriftFile>();
        // a 1 element array so storeChangeSetInNewTransaction can return the Headers for use in ackChangeSetInNewTransaction
        Headers[] headers = new Headers[1];

        DriftChangeSetSummary result = JPADriftServer.storeChangeSetInNewTransaction(subject, resourceId, changeSetZip,
            driftFilesToRequest, headers);

        if (null == result) {
            return result;
        }

        JPADriftServer.ackChangeSetInNewTransaction(subject, resourceId, headers[0], driftFilesToRequest);

        return result;
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public DriftChangeSetSummary storeChangeSetInNewTransaction(Subject subject, final int resourceId,
        final File changeSetZip, final List<JPADriftFile> driftFilesToRequest, final Headers[] headers)
        throws Exception {

        final Resource resource = getResource(resourceId);
        final DriftChangeSetSummary summary = new DriftChangeSetSummary();
        final boolean storeBinaryContent = isBinaryContentStorageEnabled();

        try {
            ZipUtil.walkZipFile(changeSetZip, new ChangeSetFileVisitor() {

                @Override
                public boolean visit(ZipEntry zipEntry, ZipInputStream stream) throws Exception {

                    JPADriftChangeSet driftChangeSet = null;

                    ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new InputStreamReader(stream)),
                        false);

                    // store the new change set info (not the actual blob)
                    DriftDefinition driftDef = findDriftDefinition(resource, reader.getHeaders());
                    if (driftDef == null) {
                        log.error("Unable to locate DriftDefinition for Resource [" + resource
                            + "]. Change set cannot be saved.");
                        return false;
                    }
                    // TODO: Commenting out the following line for now. We want to set the
                    // version to the value specified in the headers, but we may want to also
                    // validate it against the latest version we have in the database so that
                    // we can make sure that the agent is in sync with the server.
                    //
                    //int version = getChangeSetVersion(resource, config);
                    int version = reader.getHeaders().getVersion();

                    DriftChangeSetCategory category = reader.getHeaders().getType();
                    driftChangeSet = new JPADriftChangeSet(resource, version, category, driftDef);
                    entityManager.persist(driftChangeSet);

                    summary.setCategory(category);
                    summary.setResourceId(resourceId);
                    summary.setDriftDefinitionName(reader.getHeaders().getDriftDefinitionName());
                    summary.setDriftHandlingMode(driftDef.getDriftHandlingMode());
                    summary.setCreatedTime(driftChangeSet.getCtime());

                    if (version > 0) {
                        for (FileEntry entry : reader) {
                            boolean addToList = storeBinaryContent || !DriftUtil.isBinaryFile(entry.getFile());
                            JPADriftFile oldDriftFile = getDriftFile(entry.getOldSHA(), driftFilesToRequest, addToList);
                            JPADriftFile newDriftFile = getDriftFile(entry.getNewSHA(), driftFilesToRequest, addToList);

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
                    } else {
                        summary.setInitialChangeSet(true);
                        JPADriftSet driftSet = new JPADriftSet();
                        for (FileEntry entry : reader) {
                            boolean addToList = storeBinaryContent || !DriftUtil.isBinaryFile(entry.getFile());
                            JPADriftFile newDriftFile = getDriftFile(entry.getNewSHA(), driftFilesToRequest, addToList);
                            String path = FileUtil.useForwardSlash(entry.getFile());
                            // A Drift always has a changeSet. Note that in this code section the changeset is
                            // always going to be set to a DriftDefinition's changeSet. But that is not always the
                            // case, it could also be set to a DriftDefinitionTemplate's changeSet.
                            driftSet.addDrift(new JPADrift(driftChangeSet, path, entry.getType(), null, newDriftFile));
                        }
                        entityManager.persist(driftSet);
                        driftChangeSet.setInitialDriftSet(driftSet);
                        entityManager.merge(driftChangeSet);
                    }

                    headers[0] = reader.getHeaders();

                    return true;
                }
            });

            return summary;

        } catch (Exception e) {
            String msg = "Failed to store drift changeset for ";
            if (null != resource) {
                msg += resource;
            } else {
                msg += ("resourceId [" + resourceId + "]");
            }
            log.error(msg, e);

            return null;

        } finally {
            // delete the changeSetFile?
        }
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void ackChangeSetInNewTransaction(Subject subject, final int resourceId, final Headers headers,
        final List<JPADriftFile> driftFilesToRequest) throws Exception {

        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource.isSynthetic()) {
            return;
        }

        try {
            AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
            DriftAgentService service = agentClient.getDriftAgentService();

            service.ackChangeSet(resourceId, headers.getDriftDefinitionName());

            // send a message to the agent requesting the necessary JPADriftFile content. Note that the
            // driftFile status has been set to REQUESTED outside of this call.
            if (!driftFilesToRequest.isEmpty()) {
                try {
                    service.requestDriftFiles(resourceId, headers, driftFilesToRequest);

                } catch (Exception e) {
                    log.warn("Unable to inform agent of drift file request  [" + driftFilesToRequest + "]", e);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to acknowledge changeSet storage with agent for " + headers, e);
        }
    }

    private boolean isBinaryContentStorageEnabled() {
        String binaryContent = System.getProperty("rhq.server.drift.store-binary-content", "false");
        return binaryContent.equals("true");
    }

    private JPADriftFile getDriftFile(String sha256, List<JPADriftFile> emptyDriftFiles, boolean addToList) {
        JPADriftFile result = null;

        if (null == sha256 || "0".equals(sha256)) {
            return result;
        }

        result = entityManager.find(JPADriftFile.class, sha256);
        // if the JPADriftFile is not yet in the db then persist it, and mark it requested if content is to be fetched
        // note - by immediately setting the initial status to REQUESTED we avoid a future update and a
        // potential deadlock scenario where the REQUESTED and LOADED status updates can happen simultaneously
        if (null == result) {
            JPADriftFile driftFile = new JPADriftFile(sha256);
            if (addToList) {
                driftFile.setStatus(DriftFileStatus.REQUESTED);
            }
            result = persistDriftFile(driftFile);
            if (addToList) {
                emptyDriftFiles.add(result);
            }
        }

        return result;
    }

    private DriftDefinition findDriftDefinition(Resource resource, Headers headers) {
        for (DriftDefinition driftDef : resource.getDriftDefinitions()) {
            if (driftDef.getName().equals(headers.getDriftDefinitionName())) {
                return driftDef;
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
        File dir = FileUtil.createTempDirectory(zipFileName.substring(0, zipFileName.indexOf(".")),null,tmpDir);
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
                return null;
            }
            return IOUtils.toString(content.getBlob().getBinaryStream(), Charset.defaultCharset().name());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] getDriftFileAsByteArray(String hash) {
        try {
            JPADriftFileBits content = (JPADriftFileBits) entityManager.createNamedQuery(
                JPADriftFileBits.QUERY_FIND_BY_ID).setParameter("hashId", hash).getSingleResult();
            if (content.getDataSize() == null || content.getDataSize() < 1) {
                return new byte[] {};
            }
            return StreamUtil.slurp(content.getBlob().getBinaryStream());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Resource getResource(int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new IllegalArgumentException("Resource not found [" + resourceId + "]");
        }
        return resource;
    }
}

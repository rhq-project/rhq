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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;

import org.jboss.remoting.CannotConnectException;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.DriftChangeSetJPACriteria;
import org.rhq.core.domain.criteria.DriftJPACriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.RhqDriftFile;
import org.rhq.core.domain.drift.DriftFileBits;
import org.rhq.core.domain.drift.DriftFileStatus;
import org.rhq.core.domain.drift.RhqDrift;
import org.rhq.core.domain.drift.RhqDriftChangeSet;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

@Stateless
public class DriftManagerBean implements DriftManagerLocal, DriftManagerRemote {
    private final Log log = LogFactory.getLog(this.getClass());

    @javax.annotation.Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    @javax.annotation.Resource(mappedName = "queue/DriftChangesetQueue")
    private Queue changesetQueue;

    @javax.annotation.Resource(mappedName = "queue/DriftFileQueue")
    private Queue fileQueue;

    @EJB
    AgentManagerLocal agentManager;

    @EJB
    DriftManagerLocal driftManager;

    @EJB
    SubjectManagerLocal subjectManager;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    // use a new transaction when putting things on the JMS queue. see 
    // http://management-platform.blogspot.com/2008/11/transaction-recovery-in-jbossas.html
    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void addChangeSet(int resourceId, long zipSize, InputStream zipStream) throws Exception {

        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(changesetQueue);
        ObjectMessage msg = session.createObjectMessage(new DriftUploadRequest(resourceId, zipSize, zipStream));
        producer.send(msg);
        connection.close();
    }

    // use a new transaction when putting things on the JMS queue. see 
    // http://management-platform.blogspot.com/2008/11/transaction-recovery-in-jbossas.html
    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void addFiles(int resourceId, long zipSize, InputStream zipStream) throws Exception {

        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(fileQueue);
        ObjectMessage msg = session.createObjectMessage(new DriftUploadRequest(resourceId, zipSize, zipStream));
        producer.send(msg);
        connection.close();
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void storeChangeSet(final int resourceId, File changeSetZip) throws Exception {
        final Resource resource = entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new IllegalArgumentException("Resource not found [" + resourceId + "]");
        }

        try {
            DriftChangeSetJPACriteria c = new DriftChangeSetJPACriteria();
            c.addFilterResourceId(resourceId);
            List<RhqDriftChangeSet> changeSets = findDriftChangeSetsByCriteria(subjectManager.getOverlord(), c);
            final int version = changeSets.size();

            ZipUtil.walkZipFile(changeSetZip, new ChangeSetFileVisitor() {

                @Override
                public boolean visit(ZipEntry zipEntry, ZipInputStream stream) throws Exception {
                    List<? extends DriftFile> emptyDriftFiles = new ArrayList<RhqDriftFile>();
                    RhqDriftChangeSet driftChangeSet = null;

                    try {
                        ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new InputStreamReader(
                            stream)));

                        // store the new change set info (not the actual blob)
                        DriftConfiguration config = findDriftConfiguration(resource, reader.getHeaders());
                        if (config == null) {
                            log.error("Unable to locate " + config.getClass().getSimpleName() + "[id: " +
                                config.getId() + ", name: " + config.getName() + "]. Change set cannot be saved.");
                            return false;
                        }

                        DriftChangeSetCategory category = reader.getHeaders().getType();
                        driftChangeSet = new RhqDriftChangeSet(resource, version, category, config.getId());
                        entityManager.persist(driftChangeSet);

                        for (DirectoryEntry dir = reader.readDirectoryEntry(); null != dir; dir = reader
                            .readDirectoryEntry()) {

                            for (Iterator<FileEntry> i = dir.iterator(); i.hasNext();) {
                                FileEntry entry = i.next();
                                RhqDriftFile oldDriftFile = getDriftFile(entry.getOldSHA(),
                                    (List<RhqDriftFile>) emptyDriftFiles);
                                RhqDriftFile newDriftFile = getDriftFile(entry.getNewSHA(),
                                    (List<RhqDriftFile>) emptyDriftFiles);

                                // TODO Figure out an efficient way to save coverage change sets.
                                // The initial/coverage change set could contain hundreds or even thousands
                                // of entries. We probably want to consider doing some kind of batch insert
                                //
                                // jsanda

                                // use a path with only forward slashing to ensure consistent paths across reports
                                String path = new File(dir.getDirectory(), entry.getFile()).getPath();
                                path = FileUtil.useForwardSlash(path);
                                RhqDrift drift = new RhqDrift(driftChangeSet, path, entry.getType(), oldDriftFile,
                                    newDriftFile);
                                entityManager.persist(drift);

                            }
                        }
                        // send a message to the agent requesting the empty RhqDriftFile content
                        if (!emptyDriftFiles.isEmpty()) {

                            AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(),
                                resourceId);
                            DriftAgentService service = agentClient.getDriftAgentService();
                            try {
                                if (service.requestDriftFiles(resourceId, reader.getHeaders(),
                                    (List<DriftFile>) emptyDriftFiles)) {
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
        } catch (Exception e) {
            String msg = "Failed to store drift changeset for ";
            if (null != resource) {
                msg += resource;
            } else {
                msg += ("resourceId " + resourceId);
            }
            log.error(msg, e);

        } finally {
            // delete the changeSetFile?
        }
    }

    DriftConfiguration findDriftConfiguration(Resource resource, Headers headers) {
        for (Configuration config : resource.getDriftConfigurations()) {
            DriftConfiguration driftConfig = new DriftConfiguration(config);
            if (driftConfig.getName().equals(headers.getDriftConfigurationName())) {
                return driftConfig;
            }
        }
        return null;
    }

    private abstract class ChangeSetFileVisitor implements ZipUtil.ZipEntryVisitor {
    }

    private RhqDriftFile getDriftFile(String sha256, List<RhqDriftFile> emptyDriftFiles) {
        RhqDriftFile result = null;

        if (null == sha256 || "0".equals(sha256)) {
            return result;
        }

        result = entityManager.find(RhqDriftFile.class, sha256);
        // if the RhqDriftFile is not yet in the db, then it needs to be fetched from the agent
        if (null == result) {
            result = persistDriftFile(new RhqDriftFile(sha256));
            emptyDriftFiles.add(result);
        }

        return result;
    }

    @Override
    public RhqDriftFile persistDriftFile(RhqDriftFile driftFile) {

        entityManager.persist(driftFile);
        return driftFile;
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void persistDriftFileData(RhqDriftFile driftFile, InputStream data) throws Exception {

        DriftFileBits df = entityManager.find(DriftFileBits.class, driftFile.getHashId());
        if (null == df) {
            throw new IllegalArgumentException("RhqDriftFile not found [" + driftFile.getHashId() + "]");
        }
        df.setData(Hibernate.createBlob(new BufferedInputStream(data)));
        df.setStatus(DriftFileStatus.LOADED);
    }

    @Override
    public void storeFiles(File filesZip) throws Exception {
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
            RhqDriftFile driftFile = new RhqDriftFile(file.getName());
            try {
                driftManager.persistDriftFileData(driftFile, new FileInputStream(file));
            } catch (Exception e) {
                LogFactory.getLog(getClass()).info("Skipping bad drift file", e);
            }
        }

        for (File file : dir.listFiles()) {
            file.delete();
        }
        boolean deleted = dir.delete();
        if (!deleted) {
            LogFactory.getLog(getClass()).info("Unable to delete " + dir.getAbsolutePath() + ". This directory and " +
                "its contents are no longer needed. It can be deleted.");
        }

//        DriftFileVisitor dfVisitor = new DriftFileVisitor(driftManager);
//        ZipUtil.walkZipFile(filesZip, dfVisitor);

    }

    private static class DriftFileVisitor implements ZipUtil.ZipEntryVisitor {

        private DriftManagerLocal driftManager;

        public DriftFileVisitor(DriftManagerLocal driftManager) {
            this.driftManager = driftManager;
        }

        public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
            String sha256 = entry.getName();
            RhqDriftFile driftFile = new RhqDriftFile(sha256);

            // TODO use server plugin
            try {
                driftManager.persistDriftFileData(driftFile, stream);
            } catch (Exception e) {
                LogFactory.getLog(this.getClass()).info("Skipping bad drift file", e);
            }

            return true;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int deleteDriftsInNewTransaction(Subject subject, String... driftIds) {
        int result = 0;

        for (String driftId : driftIds) {
            Drift doomed = entityManager.find(RhqDrift.class, driftId);
            if (null != doomed) {
                entityManager.remove(doomed);
                ++result;
            }
        }

        return result;
    }

    @Override
    public int deleteDrifts(Subject subject, String[] driftIds) {
        // avoid big transactions by doing this one at a time. if this is too slow we can chunk in bigger increments.
        int result = 0;

        for (String driftId : driftIds) {
            result += driftManager.deleteDriftsInNewTransaction(subject, driftId);
        }

        return result;
    }

    @Override
    public int deleteDriftsByContext(Subject subject, EntityContext entityContext) throws RuntimeException {
        int result = 0;
        DriftJPACriteria criteria = new DriftJPACriteria();

        switch (entityContext.getType()) {
        case Resource:
            criteria.addFilterResourceIds(entityContext.getResourceId());
            break;

        case SubsystemView:
            // delete them all
            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }

        List<RhqDrift> drifts = driftManager.findDriftsByCriteria(subject, criteria);
        if (!drifts.isEmpty()) {
            String[] driftIds = new String[drifts.size()];
            int i = 0;
            for (Drift drift : drifts) {
                driftIds[i++] = drift.getId();
            }

            result = driftManager.deleteDrifts(subject, driftIds);
        }

        return result;
    }

    @Override
    public void deleteDriftConfiguration(Subject subject, EntityContext entityContext, String driftConfigName) {

        switch (entityContext.getType()) {
        case Resource:
            int resourceId = entityContext.getResourceId();
            Resource resource = entityManager.find(Resource.class, resourceId);
            if (null == resource) {
                throw new IllegalArgumentException("Entity not found [" + entityContext + "]");
            }

            for (Iterator<Configuration> i = resource.getDriftConfigurations().iterator(); i.hasNext();) {
                DriftConfiguration dc = new DriftConfiguration(i.next());
                if (dc.getName().equals(driftConfigName)) {
                    i.remove();
                    entityManager.merge(resource);

                    AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
                    DriftAgentService service = agentClient.getDriftAgentService();
                    try {
                        service.unscheduleDriftDetection(resourceId, dc);
                    } catch (Exception e) {
                        log.warn(" Unable to inform agent of unscheduled drift detection  [" + dc + "]", e);
                    }

                    break;
                }
            }

            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }
    }

    @Override
    public void updateDriftConfiguration(Subject subject, EntityContext entityContext, DriftConfiguration driftConfig) {
        switch (entityContext.getType()) {
        case Resource:
            int resourceId = entityContext.getResourceId();
            Resource resource = entityManager.find(Resource.class, resourceId);
            if (null == resource) {
                throw new IllegalArgumentException("Entity not found [" + entityContext + "]");
            }

            for (Iterator<Configuration> i = resource.getDriftConfigurations().iterator(); i.hasNext();) {
                DriftConfiguration dc = new DriftConfiguration(i.next());
                if (dc.getName().equals(driftConfig.getName())) {
                    i.remove();
                    break;
                }
            }

            resource.getDriftConfigurations().add(driftConfig.getConfiguration());
            entityManager.merge(resource);

            AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
            DriftAgentService service = agentClient.getDriftAgentService();
            try {
                service.scheduleDriftDetection(resourceId, driftConfig);
            } catch (Exception e) {
                log.warn(" Unable to inform agent of unscheduled drift detection  [" + driftConfig + "]", e);
            }

            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }
    }

    @Override
    public PageList<RhqDriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetJPACriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<RhqDriftChangeSet> queryRunner = new CriteriaQueryRunner<RhqDriftChangeSet>(criteria, generator,
            entityManager);
        PageList<RhqDriftChangeSet> result = queryRunner.execute();
        return result;
    }

    @Override
    public PageList<RhqDrift> findDriftsByCriteria(Subject subject, DriftJPACriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<RhqDrift> queryRunner = new CriteriaQueryRunner<RhqDrift>(criteria, generator, entityManager);
        PageList<RhqDrift> result = queryRunner.execute();
        return result;
    }

    @Override
    public DriftConfiguration getDriftConfiguration(Subject subject, EntityContext entityContext, int driftConfigId)
        throws RuntimeException {

        DriftConfiguration result = null;

        switch (entityContext.getType()) {
        case Resource:
            Resource resource = entityManager.find(Resource.class, entityContext.getResourceId());
            if (null == resource) {
                throw new IllegalArgumentException("Entity not found [" + entityContext + "]");
            }

            for (Configuration config : resource.getDriftConfigurations()) {
                if (config.getId() == driftConfigId) {
                    result = new DriftConfiguration(config);
                    break;
                }
            }

            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }

        if (null == result) {
            throw new IllegalArgumentException("RhqDrift Configuration Id [" + driftConfigId
                + "] not found for entityContext [" + entityContext + "]");
        }

        return result;
    }

    @Override
    public RhqDriftFile getDriftFile(Subject subject, String sha256) {
        RhqDriftFile result = entityManager.find(RhqDriftFile.class, sha256);
        return result;
    }

    @Override
    public void detectDrift(Subject subject, EntityContext context, DriftConfiguration driftConfig) {

        switch (context.getType()) {
        case Resource:
            int resourceId = context.getResourceId();
            Resource resource = entityManager.find(Resource.class, resourceId);
            if (null == resource) {
                throw new IllegalArgumentException("Resource not found [" + resourceId + "]");
            }

            AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
            DriftAgentService service = agentClient.getDriftAgentService();
            // this is a one-time on-demand call. If it fails throw an exception to make sure the user knows it
            // did not happen. But clean it up a bit if it's a connect exception            
            try {
                service.detectDrift(resourceId, driftConfig);
            } catch (CannotConnectException e) {
                throw new IllegalStateException(
                    "Agent could not be reached and may be down (see server logs for more). Could not perform drift detection request ["
                        + driftConfig + "]");
            }

            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + context + "]");
        }
    }
}

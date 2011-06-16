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

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;
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

@Stateless
public class DriftManagerBean implements DriftManagerLocal {
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

    @Override
    public void addChangeset(int resourceId, long zipSize, InputStream zipStream) throws Exception {

        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(changesetQueue);
        ObjectMessage msg = session.createObjectMessage(new DriftUploadRequest(resourceId, zipSize, zipStream));
        producer.send(msg);
        connection.close();
    }

    @Override
    public void addFiles(int resourceId, long zipSize, InputStream zipStream) throws Exception {

        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(fileQueue);
        ObjectMessage msg = session.createObjectMessage(new DriftUploadRequest(resourceId, zipSize, zipStream));
        producer.send(msg);
        connection.close();
    }

    @Override
    public void storeChangeSet(final int resourceId, File changeSetZip) throws Exception {
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }

        try {
            DriftChangeSetCriteria c = new DriftChangeSetCriteria();
            c.addFilterResourceId(resourceId);
            List<DriftChangeSet> changeSets = findDriftChangeSetsByCriteria(subjectManager.getOverlord(), c);
            final boolean isInitialChangeSet = changeSets.isEmpty();
            final int version = changeSets.size();

            // store the new change set info (not the actual blob)
            final DriftChangeSet driftChangeSet = new DriftChangeSet(resource, version);
            //driftChangeSet.setData(Hibernate.createBlob(new BufferedInputStream(new FileInputStream(changeSetZip))));
            entityManager.persist(driftChangeSet);

            ZipUtil.walkZipFile(changeSetZip, new ChangeSetFileVisitor() {

                @Override
                public boolean visit(ZipEntry zipEntry, ZipInputStream stream) throws Exception {
                    List<DriftFile> emptyDriftFiles = new ArrayList<DriftFile>();

                    try {
                        ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new InputStreamReader(
                            stream)));

                        for (DirectoryEntry dir = reader.readDirectoryEntry(); null != dir; dir = reader
                            .readDirectoryEntry()) {

                            for (Iterator<FileEntry> i = dir.iterator(); i.hasNext();) {
                                FileEntry entry = i.next();
                                DriftFile oldDriftFile = getDriftFile(entry.getOldSHA(), emptyDriftFiles);
                                DriftFile newDriftFile = getDriftFile(entry.getNewSHA(), emptyDriftFiles);

                                // We don't generate Drift occurrences off of the initial change set. It is used only
                                // to give us a starting point and to tell us what files we need to pull down. 
                                if (!isInitialChangeSet) {
                                    // use a canonical path with only forward slashing to ensure consistent paths across reports
                                    String path = new File(dir.getDirectory(), entry.getFile()).getPath();
                                    path = FileUtil.useForwardSlash(path);
                                    Drift drift = new Drift(driftChangeSet, path, entry.getType(), oldDriftFile,
                                        newDriftFile);
                                    entityManager.persist(drift);
                                }
                            }
                        }
                        // send a message to the agent requesting the empty DriftFile content
                        if (!emptyDriftFiles.isEmpty()) {

                            AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(),
                                resourceId);
                            DriftAgentService service = agentClient.getDriftAgentService();
                            if (service.requestDriftFiles(emptyDriftFiles)) {

                                for (DriftFile driftFile : emptyDriftFiles) {
                                    driftFile.setStatus(DriftFileStatus.REQUESTED);
                                }
                            }
                        }
                    } catch (Exception e) {
                        String msg = "Failed to store drift changeset: " + driftChangeSet;
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

    private abstract class ChangeSetFileVisitor implements ZipUtil.ZipEntryVisitor {
    }

    private DriftFile getDriftFile(String sha256, List<DriftFile> emptyDriftFiles) {
        DriftFile result = null;

        if (null == sha256 || "0".equals(sha256)) {
            return result;
        }

        result = entityManager.find(DriftFile.class, sha256);
        // if the DriftFile is not yet in the db, then it needs to be fetched from the agent
        if (null == result) {
            result = persistDriftFile(new DriftFile(sha256));
            emptyDriftFiles.add(result);
        }

        return result;
    }

    @Override
    public DriftFile persistDriftFile(DriftFile driftFile) {

        entityManager.persist(driftFile);
        return driftFile;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void persistDriftFileData(DriftFile driftFile, InputStream data) throws Exception {

        DriftFile df = entityManager.find(DriftFile.class, driftFile.getSha256());
        if (null == df) {
            throw new IllegalArgumentException("DriftFile not found: " + driftFile.getSha256());
        }
        df.setData(Hibernate.createBlob(new BufferedInputStream(data)));
        df.setStatus(DriftFileStatus.LOADED);
    }

    @Override
    public void storeFiles(File filesZip) throws Exception {

        DriftFileVisitor dfVisitor = new DriftFileVisitor(driftManager);
        ZipUtil.walkZipFile(filesZip, dfVisitor);

    }

    private static class DriftFileVisitor implements ZipUtil.ZipEntryVisitor {

        private DriftManagerLocal driftManager;

        public DriftFileVisitor(DriftManagerLocal driftManager) {
            this.driftManager = driftManager;
        }

        public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
            String sha256 = entry.getName();
            DriftFile driftFile = new DriftFile(sha256);

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
    public PageList<DriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<DriftChangeSet> queryRunner = new CriteriaQueryRunner<DriftChangeSet>(criteria, generator,
            entityManager);
        PageList<DriftChangeSet> result = queryRunner.execute();
        return result;
    }

    @Override
    public DriftFile getDriftFile(Subject subject, String sha256) {
        DriftFile result = entityManager.find(DriftFile.class, sha256);
        return result;
    }

}

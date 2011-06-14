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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
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

import org.rhq.common.drift.DriftChangeSetEntry;
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
    public void storeChangeSet(int resourceId, File changeSetZip) throws Exception {
        DriftChangeSet driftChangeSet = null;
        Resource resource = null;

        try {
            resource = entityManager.find(Resource.class, resourceId);

            DriftChangeSetCriteria c = new DriftChangeSetCriteria();
            c.addFilterResourceId(resourceId);
            List<DriftChangeSet> changeSets = findDriftChangeSetsByCriteria(subjectManager.getOverlord(), c);
            boolean isInitialChangeSet = changeSets.isEmpty();
            int version = changeSets.size();
            List<DriftFile> emptyDriftFiles = new ArrayList<DriftFile>();

            // TODO whole thing  will change to use the parser utility when it's available
            List<DriftChangeSetEntry> entries = new ArrayList<DriftChangeSetEntry>();
            for (DriftChangeSetEntry entry : entries) {
                DriftFile oldDriftFile = getDriftFile(entry.getOldSha256(), emptyDriftFiles);
                DriftFile newDriftFile = getDriftFile(entry.getNewSha256(), emptyDriftFiles);

                // We don't generate Drift occurrences off of the initial change set. It is used only
                // to give us a starting point and to tell us what files we need to pull down. 
                if (!isInitialChangeSet) {
                    Drift drift = new Drift(resource, entry.getPath(), entry.getCategory(), oldDriftFile, newDriftFile);
                    entityManager.persist(drift);
                }
            }

            // store the actual change set
            driftChangeSet = new DriftChangeSet(resource, version);
            driftChangeSet.setData(Hibernate.createBlob(new BufferedInputStream(new FileInputStream(changeSetZip))));
            entityManager.persist(driftChangeSet);

            // send a message to the agent requesting the empty DriftFile content
            AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
            DriftAgentService service = agentClient.getDriftAgentService();
            if (service.requestDriftFiles(emptyDriftFiles)) {
                for (DriftFile driftFile : emptyDriftFiles) {
                    driftFile.setStatus(DriftFileStatus.REQUESTED);
                }
            }

        } catch (Exception e) {
            String msg = "Failed to store drift changeset for ";
            if (null != driftChangeSet) {
                msg += driftChangeSet;
            } else if (null != resource) {
                msg += resource;
            } else {
                msg += ("resourceId " + resourceId);
            }
            log.error(msg, e);

        } finally {
            // delete the changeSetFile?
        }
    }

    private DriftFile getDriftFile(String sha256, List<DriftFile> emptyDriftFiles) {
        DriftFile result = null;

        if (null == sha256) {
            return result;
        }

        try {
            result = entityManager.find(DriftFile.class, sha256);
        } catch (Exception e) {
            // if the DriftFile content is not yet in the db, then it needs to be fetched from the agent
            result = persistDriftFile(new DriftFile(sha256));
            emptyDriftFiles.add(result);
        }

        return result;
    }

    @Override
    public DriftFile persistDriftFile(DriftFile driftFile) {

        entityManager.persist(driftFile);
        DriftFile result = entityManager.find(DriftFile.class, driftFile.getSha256());
        return result;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void persistDriftFileData(DriftFile driftFile, InputStream data) throws Exception {

        DriftFile df = entityManager.find(DriftFile.class, driftFile.getSha256());
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

}

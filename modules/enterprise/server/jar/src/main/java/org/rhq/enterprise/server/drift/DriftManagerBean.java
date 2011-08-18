/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.drift;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
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

import org.jboss.remoting.CannotConnectException;

import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftConfigurationCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfigurationComparator;
import org.rhq.core.domain.drift.DriftConfigurationComparator.CompareMode;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginManager;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;

import difflib.DiffUtils;
import difflib.Patch;

import static java.util.Arrays.asList;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * The SLSB supporting Drift management to clients.  
 * 
 * Wrappers are provided for the methods defined in DriftServerPluginFacet and the work is deferred to the plugin
 * No assumption is made about the  any back end implementation of a drift server plugin and therefore does not
 * declare any transactioning (the NOT_SUPPORTED transaction attribute is used for all wrappers). 
 * 
 * For methods not deferred to the server plugin, the implementations are done here.   
 * 
 * @author John Sanda
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Stateless
public class DriftManagerBean implements DriftManagerLocal {

    // TODO Should security checks be handled here instead of delegating to the drift plugin?
    // Currently any security checks that need to be performed are delegated to the plugin.
    // This is fine *so far* since the only plugin is the default which is our existing SLSB
    // layer backed by the RHQ database. If the plugins are only supposed to be responsible
    // for persistence management of drift entities and content, then they should not be
    // responsible for other concerns like security.

    private Log log = LogFactory.getLog(DriftManagerBean.class);

    @javax.annotation.Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    @javax.annotation.Resource(mappedName = "queue/DriftChangesetQueue")
    private Queue changesetQueue;

    @javax.annotation.Resource(mappedName = "queue/DriftFileQueue")
    private Queue fileQueue;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private DriftManagerLocal driftManager; // ourself

    @EJB
    private SubjectManagerLocal subjectManager;

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
    @TransactionAttribute(NOT_SUPPORTED)
    public DriftSnapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.createSnapshot(subject, criteria);
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

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void deleteDriftConfiguration(Subject subject, EntityContext entityContext, String driftConfigName) {

        switch (entityContext.getType()) {
        case Resource:
            int resourceId = entityContext.getResourceId();
            DriftConfigurationCriteria criteria = new DriftConfigurationCriteria();
            criteria.addFilterName(driftConfigName);
            criteria.addFilterResourceIds(resourceId);
            criteria.setStrict(true);
            PageList<DriftConfiguration> results = driftManager.findDriftConfigurationsByCriteria(subject, criteria);
            DriftConfiguration doomedDriftConfig = null;
            if (results != null && results.size() == 1) {
                doomedDriftConfig = results.get(0);
            }

            if (doomedDriftConfig != null) {

                // TODO security check!

                // tell the agent first - we don't want the agent reporting on the drift config after we delete it
                boolean unscheduledOnAgent = false;
                try {
                    AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
                    DriftAgentService service = agentClient.getDriftAgentService();
                    service.unscheduleDriftDetection(resourceId, doomedDriftConfig);
                    unscheduledOnAgent = true;
                } catch (Exception e) {
                    log.warn(" Unable to inform agent of unscheduled drift detection  [" + doomedDriftConfig + "]", e);
                }

                // purge all data related to this drift configuration
                try {
                    driftManager.purgeByDriftConfigurationName(subject, resourceId, doomedDriftConfig.getName());
                } catch (Exception e) {
                    String warnMessage = "Failed to purge data for drift configuration [" + driftConfigName
                        + "] for resource [" + resourceId + "].";
                    if (unscheduledOnAgent) {
                        warnMessage += " The agent was told to stop detecting drift for that configuration.";
                    }
                    log.warn(warnMessage, e);
                }

                // now purge the drift config itself
                driftManager.deleteResourceDriftConfiguration(subject, resourceId, doomedDriftConfig.getId());
            } else {
                throw new IllegalArgumentException("Resource does not have drift config named [" + driftConfigName
                    + "]");
            }
            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }
    }

    @Override
    public void deleteResourceDriftConfiguration(Subject subject, int resourceId, int driftConfigId) {
        DriftConfiguration doomed = entityManager.getReference(DriftConfiguration.class, driftConfigId);
        entityManager.remove(doomed);
        return;
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject,
        DriftChangeSetCriteria criteria) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.findDriftChangeSetsByCriteria(subject, criteria);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.findDriftCompositesByCriteria(subject, criteria);
    }

    @Override
    public PageList<DriftConfiguration> findDriftConfigurationsByCriteria(Subject subject,
        DriftConfigurationCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<DriftConfiguration> queryRunner = new CriteriaQueryRunner<DriftConfiguration>(criteria,
            generator, entityManager);
        PageList<DriftConfiguration> result = queryRunner.execute();

        return result;
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.findDriftsByCriteria(subject, criteria);
    }

    @Override
    public DriftConfiguration getDriftConfiguration(Subject subject, int driftConfigId) {
        DriftConfiguration result = entityManager.find(DriftConfiguration.class, driftConfigId);

        if (null == result) {
            throw new IllegalArgumentException("Drift Configuration Id [" + driftConfigId + "] not found.");
        }

        // force lazy loads
        result.getConfiguration().getProperties();
        result.getResource();

        return result;
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public DriftFile getDriftFile(Subject subject, String hashId) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.getDriftFile(subject, hashId);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void saveChangeSet(Subject subject, int resourceId, File changeSetZip) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        driftServerPlugin.saveChangeSet(subject, resourceId, changeSetZip);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void saveChangeSetFiles(Subject subject, File changeSetFilesZip) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        driftServerPlugin.saveChangeSetFiles(subject, changeSetFilesZip);
    }

    /**
     * This purges the persisted data related to drift configuration, but it does NOT talk to the agent to tell the agent
     * about this nor does it actually delete the drift config itself.
     * 
     * If you want to delete a drift configuration and all that that entails, you must use
     * {@link #deleteDriftConfiguration(Subject, EntityContext, String)} instead.
     * 
     * This method is really for internal use only.
     */
    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void purgeByDriftConfigurationName(Subject subject, int resourceId, String driftConfigName) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        driftServerPlugin.purgeByDriftConfigurationName(subject, resourceId, driftConfigName);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public int purgeOrphanedDriftFiles(Subject subject, long purgeMillis) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.purgeOrphanedDriftFiles(subject, purgeMillis);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public String getDriftFileBits(String hash) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.getDriftFileBits(hash);
    }

    @Override
    public FileDiffReport generateUnifiedDiff(Drift drift) {
        String oldContent = getDriftFileBits(drift.getOldDriftFile().getHashId());
        List<String> oldList = asList(oldContent.split("\\n"));
        String newContent = getDriftFileBits(drift.getNewDriftFile().getHashId());
        List<String> newList = asList(newContent.split("\\n"));

        Patch patch = DiffUtils.diff(oldList, newList);
        List<String> deltas = DiffUtils.generateUnifiedDiff(drift.getPath() + ":old", drift.getPath() + ":new",
            oldList, patch, 10);

        StringBuilder diff = new StringBuilder();
        for (String delta : deltas) {
            diff.append(delta).append("\n");
        }

        return new FileDiffReport(patch.getDeltas().size(), diff.toString());
    }

    @Override
    public PageList<Drift> findHistory(Drift drift) {
        return null;
    }

    @Override
    public void updateDriftConfiguration(Subject subject, EntityContext entityContext, DriftConfiguration driftConfig) {

        // before we do anything, make sure the drift config name is valid
        if (!driftConfig.getName().matches(DriftConfigurationDefinition.PROP_NAME_REGEX_PATTERN)) {
            throw new IllegalArgumentException("Drift configuration name contains invalid characters: "
                + driftConfig.getName());
        }

        switch (entityContext.getType()) {
        case Resource:
            int resourceId = entityContext.getResourceId();
            Resource resource = entityManager.find(Resource.class, resourceId);
            if (null == resource) {
                throw new IllegalArgumentException("Entity not found [" + entityContext + "]");
            }

            // Update or add the driftConfig as necessary
            DriftConfigurationComparator comparator = new DriftConfigurationComparator(
                CompareMode.ONLY_DIRECTORY_SPECIFICATIONS);
            boolean isUpdated = false;
            for (DriftConfiguration dc : resource.getDriftConfigurations()) {
                if (dc.getName().equals(driftConfig.getName())) {
                    // compare the directory specs (basedir/includes-excludes filters only - if they are different, abort.
                    // you cannot update drift config that changes basedir/includes/excludes from the original.
                    // the user must delete the drift config and create a new one, as opposed to trying to update the existing one.
                    if (comparator.compare(driftConfig, dc) == 0) {
                        dc.setConfiguration(driftConfig.getConfiguration());
                        isUpdated = true;
                        break;
                    } else {
                        throw new IllegalArgumentException(
                            "You cannot change an existing drift configuration's base directory or includes/excludes filters.");
                    }
                }
            }

            if (!isUpdated) {
                resource.addDriftConfiguration(driftConfig);
            }
            resource = entityManager.merge(resource);

            // Do not pass attached entities to the following Agent call, which is outside Hibernate's control. Flush
            // and clear the entities to ensure the work above is captured and we pass out a detached object. 
            entityManager.flush();
            entityManager.clear();

            AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
            DriftAgentService service = agentClient.getDriftAgentService();
            try {
                service.updateDriftDetection(resourceId, driftConfig);
            } catch (Exception e) {
                log.warn(" Unable to inform agent of unscheduled drift detection  [" + driftConfig + "]", e);
            }

            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }
    }

    private DriftServerPluginFacet getServerPlugin() {
        MasterServerPluginContainer masterPC = LookupUtil.getServerPluginService().getMasterPluginContainer();
        if (masterPC == null) {
            log.warn(MasterServerPluginContainer.class.getSimpleName() + " is not started yet");
            return null;
        }

        DriftServerPluginContainer pc = masterPC.getPluginContainerByClass(DriftServerPluginContainer.class);
        if (pc == null) {
            log.warn(DriftServerPluginContainer.class + " has not been loaded by the " + masterPC.getClass() + " yet");
            return null;
        }

        DriftServerPluginManager pluginMgr = (DriftServerPluginManager) pc.getPluginManager();

        return pluginMgr.getDriftServerPluginComponent();
    }

}

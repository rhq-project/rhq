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

import static java.util.Arrays.asList;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftComplianceStatus.IN_COMPLIANCE;
import static org.rhq.core.domain.drift.DriftComplianceStatus.OUT_OF_COMPLIANCE_DRIFT;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.hibernate.Hibernate;
import org.jboss.remoting.CannotConnectException;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinition.BaseDirectory;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode;
import org.rhq.core.domain.drift.DriftDefinitionComposite;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftDetails;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.drift.Filter;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.drift.dto.DriftDTO;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginManager;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;

import difflib.DiffUtils;
import difflib.Patch;

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
public class DriftManagerBean implements DriftManagerLocal, DriftManagerRemote {

    // TODO Should security checks be handled here instead of delegating to the drift plugin?
    // Currently any security checks that need to be performed are delegated to the plugin.
    // This is fine *so far* since the only plugin is the default which is our existing SLSB
    // layer backed by the RHQ database. If the plugins are only supposed to be responsible
    // for persistence management of drift entities and content, then they should not be
    // responsible for other concerns like security.

    private Log log = LogFactory.getLog(DriftManagerBean.class);

    @javax.annotation.Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    @javax.annotation.Resource(mappedName = "java:/queue/DriftChangesetQueue")
    private Queue changesetQueue;

    @javax.annotation.Resource(mappedName = "java:/queue/DriftFileQueue")
    private Queue fileQueue;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private DriftManagerLocal driftManager; // ourself

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    // use a new transaction when putting things on the JMS queue. see
    // http://management-platform.blogspot.com/2008/11/transaction-recovery-in-jbossas.html
    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void addChangeSet(Subject subject, int resourceId, long zipSize, InputStream zipStream) throws Exception {
        authorizeOrFail(subject, resourceId, "Can not update drifts");
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
    public void addFiles(Subject subject, int resourceId, String driftDefName, String token, long zipSize,
        InputStream zipStream) throws Exception {
        authorizeOrFail(subject, resourceId, "Can not update drifts");
        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(fileQueue);
        ObjectMessage msg = session.createObjectMessage(new DriftUploadRequest(resourceId, driftDefName, token,
            zipSize, zipStream));
        producer.send(msg);
        connection.close();
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void saveChangeSetContent(Subject subject, int resourceId, String driftDefName, String token,
        File changeSetFilesZip) throws Exception {
        authorizeOrFail(subject, resourceId, "Can not update drifts");
        saveChangeSetFiles(subject, changeSetFilesZip);

        AgentClient agent = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
        DriftAgentService driftService = agent.getDriftAgentService();
        driftService.ackChangeSetContent(resourceId, driftDefName, token);
    }

    @Override
    public void processRepeatChangeSet(int resourceId, String driftDefName, int version) {
        Subject overlord = subjectManager.getOverlord();

        DriftDefinitionCriteria driftDefCriteria = new DriftDefinitionCriteria();
        driftDefCriteria.setStrict(true);
        driftDefCriteria.addFilterResourceIds(resourceId);
        driftDefCriteria.addFilterName(driftDefName);

        PageList<DriftDefinition> defs = findDriftDefinitionsByCriteria(overlord, driftDefCriteria);
        if (defs.isEmpty()) {
            log.warn("Cannot process repeat change set. No drift definition found for [resourceId: " + resourceId
                + ", driftDefinitionName: " + driftDefName + "]");
        }
        DriftDefinition driftDef = defs.get(0);

        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterResourceId(resourceId);
        criteria.addFilterDriftDefinitionId(driftDef.getId());
        criteria.addFilterVersion(Integer.toString(version));
        criteria.fetchDrifts(true);

        PageList<? extends DriftChangeSet<?>> changeSets = driftServerPlugin.findDriftChangeSetsByCriteria(overlord,
            criteria);
        if (changeSets.isEmpty()) {
            log.warn("Cannot process repeat change set. No change set found for [driftDefinitionId: "
                + driftDef.getId() + ", version: " + version + "]");
            return;
        }

        DriftChangeSet<?> changeSet = changeSets.get(0);
        DriftChangeSetSummary summary = new DriftChangeSetSummary();
        summary.setCategory(changeSet.getCategory());
        // TODO ctime should come from agent
        summary.setCreatedTime(System.currentTimeMillis());
        summary.setResourceId(changeSet.getResourceId());
        summary.setDriftDefinitionName(driftDef.getName());
        summary.setDriftHandlingMode(driftDef.getDriftHandlingMode());

        for (Drift<?, ?> drift : changeSet.getDrifts()) {
            summary.addDriftPathname(drift.getPath());
        }

        notifyAlertConditionCacheManager("processRepeatChangeSet", summary);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public DriftSnapshot getSnapshot(Subject subject, DriftSnapshotRequest request) {
        DriftSnapshot result = new DriftSnapshot(request);
        int startVersion = request.getStartVersion();

        if (0 == startVersion) {
            DriftChangeSet<? extends Drift<?, ?>> initialChangeset = loadInitialChangeSet(subject, request);
            if (null == initialChangeset) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot create snapshot, no initial changeset for: " + request);
                }

                return result;

            } else {
                result.addChangeSet(initialChangeset);
            }

            // if startVersion and endVersion are both zero, we are done.
            if (Integer.valueOf(0).equals(request.getVersion())) {
                return result;
            }

            ++startVersion;
        }

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterCategory(DriftChangeSetCategory.DRIFT);
        criteria.addFilterStartVersion(String.valueOf(startVersion));
        if (null != request.getVersion()) {
            criteria.addFilterEndVersion(Integer.toString(request.getVersion()));
        }
        criteria.addFilterDriftDefinitionId(request.getDriftDefinitionId());
        criteria.addFilterDriftDirectory(request.getDirectory());
        criteria.setStrict(true);
        criteria.fetchDrifts(true);
        criteria.addSortVersion(PageOrdering.ASC);
        criteria.setPageControl(PageControl.getUnlimitedInstance());//disable paging as the code assumes all the results will be returned.

        PageList<? extends DriftChangeSet<?>> changeSets = findDriftChangeSetsByCriteria(subject, criteria);
        for (DriftChangeSet<? extends Drift<?, ?>> changeSet : changeSets) {
            result.addChangeSet(changeSet);
        }

        return result;
    }

    private DriftChangeSet<? extends Drift<?, ?>> loadInitialChangeSet(Subject subject, DriftSnapshotRequest request) {
        DriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterCategory(COVERAGE);
        criteria.addFilterVersion("0");
        // One of the next two filters will be null
        criteria.addFilterDriftDefinitionId(request.getDriftDefinitionId());
        criteria.addFilterId(request.getTemplateChangeSetId());
        criteria.fetchDrifts(true);
        criteria.setPageControl(PageControl.getUnlimitedInstance());//disable paging as the code assumes all the results will be returned.

        PageList<? extends DriftChangeSet<?>> changeSets = findDriftChangeSetsByCriteria(subject, criteria);
        if (changeSets.isEmpty()) {
            return null;
        }

        return changeSets.get(0);
    }

    @Override
    public void detectDrift(Subject subject, EntityContext context, DriftDefinition driftDef) {
        switch (context.getType()) {
        case Resource:
            authorizeOrFail(subject, context, "Can not update drifts");
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
                service.detectDrift(resourceId, driftDef);
            } catch (CannotConnectException e) {
                throw new IllegalStateException(
                    "Agent could not be reached and may be down (see server logs for more). Could not perform drift detection request ["
                        + driftDef + "]");
            }

            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + context + "]");
        }
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void deleteDriftDefinition(Subject subject, EntityContext entityContext, String driftDefName) {
        switch (entityContext.getType()) {
        case Resource:
            authorizeOrFail(subject, entityContext, "Can not delete drifts");
            int resourceId = entityContext.getResourceId();
            DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
            criteria.addFilterName(driftDefName);
            criteria.addFilterResourceIds(resourceId);
            criteria.setStrict(true);
            criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

            PageList<DriftDefinition> results = driftManager.findDriftDefinitionsByCriteria(subject, criteria);
            DriftDefinition doomedDriftDef = null;
            if (results != null && results.size() == 1) {
                doomedDriftDef = results.get(0);
            }

            if (doomedDriftDef != null) {

                // TODO security check!

                // tell the agent first - we don't want the agent reporting on the drift def after we delete it
                boolean unscheduledOnAgent = false;
                try {
                    AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
                    DriftAgentService service = agentClient.getDriftAgentService();
                    service.unscheduleDriftDetection(resourceId, doomedDriftDef);
                    unscheduledOnAgent = true;
                } catch (Exception e) {
                    log.warn(" Unable to inform agent of unscheduled drift detection  [" + doomedDriftDef + "]", e);
                }

                // purge all data related to this drift definition
                try {
                    driftManager.purgeByDriftDefinitionName(subject, resourceId, doomedDriftDef.getName());
                } catch (Exception e) {
                    String warnMessage = "Failed to purge data for drift definition [" + driftDefName
                        + "] for resource [" + resourceId + "].";
                    if (unscheduledOnAgent) {
                        warnMessage += " The agent was told to stop detecting drift for that definition.";
                    }
                    log.warn(warnMessage, e);
                }

                // now purge the drift def itself
                driftManager.deleteResourceDriftDefinition(subject, resourceId, doomedDriftDef.getId());
            } else {
                throw new IllegalArgumentException("Resource does not have drift definition named [" + driftDefName
                    + "]");
            }
            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }
    }

    @Override
    public void deleteResourceDriftDefinition(Subject subject, int resourceId, int driftDefId) {
        authorizeOrFail(subject, resourceId, "Can not delete drifts");
        DriftDefinition doomed = entityManager.find(DriftDefinition.class, driftDefId);
        doomed.getResource().setAgentSynchronizationNeeded();
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
    public PageList<DriftDefinition> findDriftDefinitionsByCriteria(Subject subject, DriftDefinitionCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<DriftDefinition> queryRunner = new CriteriaQueryRunner<DriftDefinition>(criteria,
            generator, entityManager);
        PageList<DriftDefinition> result = queryRunner.execute();

        return result;
    }

    @Override
    public PageList<DriftDefinitionComposite> findDriftDefinitionCompositesByCriteria(Subject subject,
        DriftDefinitionCriteria criteria) {

        PageList<DriftDefinition> defs = findDriftDefinitionsByCriteria(subject, criteria);
        PageList<DriftDefinitionComposite> result = new PageList<DriftDefinitionComposite>(defs.getPageControl());
        List<DriftDefinitionComposite> composites = new ArrayList<DriftDefinitionComposite>(defs.size());
        GenericDriftChangeSetCriteria csCriteria = new GenericDriftChangeSetCriteria();
        for (DriftDefinition def : defs) {
            DriftDefinitionComposite composite = new DriftDefinitionComposite(def, null);
            csCriteria.addFilterDriftDefinitionId(def.getId());
            csCriteria.addSortVersion(PageOrdering.DESC);
            csCriteria.setPageControl(PageControl.getSingleRowInstance());
            PageList<? extends DriftChangeSet<?>> changeSets = findDriftChangeSetsByCriteria(subject, csCriteria);
            if (!changeSets.isEmpty()) {
                composite.setMostRecentChangeset(changeSets.get(0));
            }
            composites.add(composite);
        }

        result.addAll(composites);

        return result;
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.findDriftsByCriteria(subject, criteria);
    }

    @Override
    public DriftDefinition getDriftDefinition(Subject subject, int driftDefId) {
        DriftDefinition result = entityManager.find(DriftDefinition.class, driftDefId);

        if (null == result) {
            throw new IllegalArgumentException("Drift Definition Id [" + driftDefId + "] not found.");
        }

        // force lazy loads
        result.getConfiguration().getProperties();
        Hibernate.initialize(result.getResource());
        Hibernate.initialize(result.getTemplate());

        return result;
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public DriftFile getDriftFile(Subject subject, String hashId) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.getDriftFile(subject, hashId);
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public DriftChangeSetSummary saveChangeSet(Subject subject, int resourceId, File changeSetZip) throws Exception {
        authorizeOrFail(subject, resourceId, "Can not update/create drifts");
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        DriftChangeSetSummary summary = driftServerPlugin.saveChangeSet(subject, resourceId, changeSetZip);

        if (DriftHandlingMode.plannedChanges != summary.getDriftHandlingMode()) {
            notifyAlertConditionCacheManager("saveChangeSet", summary);
        }

        DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
        criteria.setStrict(true);
        criteria.addFilterName(summary.getDriftDefinitionName());
        criteria.addFilterResourceIds(resourceId);
        criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

        PageList<DriftDefinition> definitions = findDriftDefinitionsByCriteria(subject, criteria);

        if (definitions.isEmpty()) {
            log.warn("Could not find drift definition for [resourceId: " + resourceId + ", driftDefinitionName: "
                + summary.getDriftDefinitionName() + "]. Will not be able check compliance for thiis drift definition");
        } else {
            updateCompliance(subject, definitions.get(0), summary);
        }

        return summary;
    }

    private void updateCompliance(Subject subject, DriftDefinition definition, DriftChangeSetSummary changeSetSummary) {
        authorizeOrFail(subject, definition.getResource().getId(), "Can not update drifts");
        boolean updateNeeded = false;

        if (changeSetSummary.isInitialChangeSet()) {
            updateNeeded = definition.getComplianceStatus() != IN_COMPLIANCE;
            definition.setComplianceStatus(IN_COMPLIANCE);
        }

        if (definition.isPinned()) {
            if (changeSetSummary.getDriftPathnames().isEmpty()) {
                updateNeeded = definition.getComplianceStatus() != IN_COMPLIANCE;
                definition.setComplianceStatus(IN_COMPLIANCE);
            } else {
                updateNeeded = definition.getComplianceStatus() == IN_COMPLIANCE;
                definition.setComplianceStatus(OUT_OF_COMPLIANCE_DRIFT);
            }
        }

        if (updateNeeded) {
            updateDriftDefinition(subject, definition);
        }
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void saveChangeSetFiles(Subject subject, File changeSetFilesZip) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        driftServerPlugin.saveChangeSetFiles(subject, changeSetFilesZip);
    }

    /**
     * This purges the persisted data related to drift definition, but it does NOT talk to the agent to tell the agent
     * about this nor does it actually delete the drift def itself.
     *
     * If you want to delete a drift definition and all that that entails, you must use
     * {@link #deleteDriftDefinition(Subject, EntityContext, String)} instead.
     *
     * This method is really for internal use only.
     */
    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void purgeByDriftDefinitionName(Subject subject, int resourceId, String driftDefName) throws Exception {
        authorizeOrFail(subject, resourceId, "Can not delete drifts");
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        driftServerPlugin.purgeByDriftDefinitionName(subject, resourceId, driftDefName);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public int purgeOrphanedDriftFiles(Subject subject, long purgeMillis) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.purgeOrphanedDriftFiles(subject, purgeMillis);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void pinSnapshot(Subject subject, int driftDefId, int snapshotVersion) {
        DriftDefinition driftDef = driftManager.getDriftDefinition(subject, driftDefId);
        authorizeOrFail(subject, driftDef.getResource().getId(), "Can not update drifts");

        if (driftDef.getTemplate() != null && driftDef.getTemplate().isPinned()) {
            throw new IllegalArgumentException(("Cannot repin a definition that has been created from a pinned "
                + "template."));
        }

        driftDef.setPinned(true);
        driftManager.updateDriftDefinition(subject, driftDef);
        driftDef.getResource().setAgentSynchronizationNeeded();

        DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(driftDefId, snapshotVersion);
        DriftSnapshot snapshot = getSnapshot(subject, snapshotRequest);

        DriftChangeSetDTO changeSet = new DriftChangeSetDTO();
        changeSet.setCategory(COVERAGE);
        changeSet.setVersion(0);
        changeSet.setDriftDefinitionId(driftDefId);
        changeSet.setDriftHandlingMode(DriftHandlingMode.normal);
        changeSet.setResourceId(driftDef.getResource().getId());

        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        try {
            driftServerPlugin.purgeByDriftDefinitionName(subject, driftDef.getResource().getId(), driftDef.getName());
            persistSnapshot(subject, snapshot, changeSet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to pin snapshot", e);
        }

        try {
            AgentClient agent = agentManager.getAgentClient(subjectManager.getOverlord(), driftDef.getResource()
                .getId());
            DriftAgentService driftService = agent.getDriftAgentService();
            driftService.pinSnapshot(driftDef.getResource().getId(), driftDef.getName(), snapshot);
        } catch (Exception e) {
            log.warn("Unable to notify agent that DriftDefinition[driftDefinitionId: " + driftDefId
                + ", driftDefinitionName: " + driftDef.getName() + "] has been pinned. The agent may be down.", e);
        }
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public String persistSnapshot(Subject subject, DriftSnapshot snapshot,
        DriftChangeSet<? extends Drift<?, ?>> changeSet) {
        authorizeOrFail(subject, changeSet.getResourceId(), "Can not update/create drifts");
        DriftChangeSetDTO changeSetDTO = new DriftChangeSetDTO();
        changeSetDTO.setCategory(changeSet.getCategory());
        changeSetDTO.setDriftHandlingMode(changeSet.getDriftHandlingMode());
        changeSetDTO.setVersion(changeSet.getVersion());
        changeSetDTO.setDriftDefinitionId(changeSet.getDriftDefinitionId());
        changeSetDTO.setResourceId(changeSet.getResourceId());

        Set<DriftDTO> drifts = new HashSet<DriftDTO>();
        for (Drift<?, ?> drift : snapshot.getDriftInstances()) {
            // we need to scrub ids and references to owning change sets
            DriftDTO driftDTO = new DriftDTO();
            driftDTO.setCategory(DriftCategory.FILE_ADDED);
            driftDTO.setChangeSet(changeSetDTO);
            driftDTO.setCtime(drift.getCtime());
            driftDTO.setNewDriftFile(toDTO(drift.getNewDriftFile()));
            driftDTO.setPath(drift.getPath());
            driftDTO.setDirectory(drift.getDirectory());
            drifts.add(driftDTO);
        }
        changeSetDTO.setDrifts(drifts);

        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        try {
            return driftServerPlugin.persistChangeSet(subject, changeSetDTO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to pin snapshot", e);
        }
    }

    private DriftFileDTO toDTO(DriftFile file) {
        DriftFileDTO dto = new DriftFileDTO();
        dto.setHashId(file.getHashId());
        dto.setStatus(file.getStatus());
        dto.setDataSize(file.getDataSize());

        return dto;
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public String getDriftFileBits(Subject subject, String hash) {
        log.debug("Retrieving drift file content for " + hash);
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.getDriftFileBits(subject, hash);
    }

    @Override
    public byte[] getDriftFileAsByteArray(Subject subject, String hash) {
        log.debug("Retrieving drift file content for " + hash);
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        return driftServerPlugin.getDriftFileAsByteArray(subject, hash);
    }

    @Override
    public FileDiffReport generateUnifiedDiff(Subject subject, Drift<?, ?> drift) {
        log.debug("Generating diff for " + drift);
        String oldContent = getDriftFileBits(subject, drift.getOldDriftFile().getHashId());
        List<String> oldList = asList(oldContent.split("\\n"));
        String newContent = getDriftFileBits(subject, drift.getNewDriftFile().getHashId());
        List<String> newList = asList(newContent.split("\\n"));

        Patch patch = DiffUtils.diff(oldList, newList);
        List<String> deltas = DiffUtils.generateUnifiedDiff(drift.getPath(), drift.getPath(), oldList, patch, 10);

        return new FileDiffReport(patch.getDeltas().size(), deltas);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FileDiffReport generateUnifiedDiffByIds(Subject subject, String driftId1, String driftId2) {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterId(driftId1);
        criteria.setPageControl(PageControl.getSingleRowInstance());

        List<? extends Drift<?, ?>> result = driftServerPlugin.findDriftsByCriteria(subject, criteria);
        if (result.size() != 1) {
            throw new IllegalArgumentException("Drift record not found: " + driftId1);
        }
        Drift drift1 = result.get(0);

        criteria.addFilterId(driftId2);
        criteria.setPageControl(PageControl.getSingleRowInstance());

        result = driftServerPlugin.findDriftsByCriteria(subject, criteria);
        if (result.size() != 1) {
            throw new IllegalArgumentException("Drift record not found: " + driftId2);
        }
        Drift drift2 = result.get(0);

        return generateUnifiedDiff(subject, drift1, drift2);
    }

    @Override
    public FileDiffReport generateUnifiedDiff(Subject subject, Drift<?, ?> drift1, Drift<?, ?> drift2) {
        DriftFile drift1File = drift1.getNewDriftFile();
        String content1 = (null == drift1File) ? "" : getDriftFileBits(subject, drift1File.getHashId());
        List<String> content1List = asList(content1.split("\\n"));

        DriftFile drift2File = drift2.getNewDriftFile();
        String content2 = (null == drift2File) ? "" : getDriftFileBits(subject, drift2File.getHashId());
        List<String> content2List = asList(content2.split("\\n"));

        Patch patch = DiffUtils.diff(content1List, content2List);
        List<String> deltas = DiffUtils
            .generateUnifiedDiff(drift1.getPath(), drift2.getPath(), content1List, patch, 10);

        return new FileDiffReport(patch.getDeltas().size(), deltas);
    }

    @Override
    public void updateDriftDefinition(Subject subject, DriftDefinition driftDefinition) {
        authorizeOrFail(subject, driftDefinition.getResource().getId(), "Can not update drifts");
        entityManager.merge(driftDefinition);
    }

    @Override
    public void updateDriftDefinition(Subject subject, EntityContext entityContext, DriftDefinition driftDef) {
        authorizeOrFail(subject, entityContext.getResourceId(), "Can not update drifts");

        // before we do anything, validate certain field values to prevent downstream errors
        validateDriftDefinition(driftDef);

        switch (entityContext.getType()) {
            case Resource: {
                int resourceId = entityContext.getResourceId();
                Resource resource = entityManager.find(Resource.class, resourceId);
                if (null == resource) {
                    throw new IllegalArgumentException("Entity not found [" + entityContext + "]");
                }

                if (!isDriftMgmtSupported(resource)) {
                    throw new IllegalArgumentException("Cannot create drift definition. The resource type " +
                            resource.getResourceType() + " does not support drift management");
                }

                // Update or add the driftDef as necessary
                DriftDefinitionComparator comparator = new DriftDefinitionComparator(
                        CompareMode.ONLY_DIRECTORY_SPECIFICATIONS);
                boolean isUpdated = false;
                for (DriftDefinition dc : resource.getDriftDefinitions()) {
                    if (dc.getName().equals(driftDef.getName())) {
                        // compare the directory specs (basedir/includes-excludes filters only - if they are different, abort.
                        // you cannot update drift def that changes basedir/includes/excludes from the original.
                        // the user must delete the drift def and create a new one, as opposed to trying to update the existing one.
                        if (comparator.compare(driftDef, dc) == 0) {
                            if (dc.isPinned() && !driftDef.isPinned()) {
                                dc.setComplianceStatus(DriftComplianceStatus.IN_COMPLIANCE);
                            }
                            entityManager.remove(dc.getConfiguration()); // don't orphan the config
                            dc.setConfiguration(driftDef.getConfiguration().deepCopyWithoutProxies());
                            isUpdated = true;
                            break;
                        } else {
                            throw new IllegalArgumentException(
                                    "A new definition must have a unique name. An existing definition cannot update it's base directory or includes/excludes filters.");
                        }
                    }
                }

                if (!isUpdated) {
                    validateTemplateForNewDef(driftDef, resource);
                    resource.addDriftDefinition(driftDef);
                    // We call persist here because if this definition is created
                    // from a pinned template, then we need to generate the initial
                    // change set. And we need the definition id to pass to the
                    // drift server plugin.
                    entityManager.persist(driftDef);
                    DriftDefinitionTemplate template = driftDef.getTemplate();
                    if (template != null && template.isPinned()) {
                        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
                        driftServerPlugin.copyChangeSet(subject, template.getChangeSetId(), driftDef.getId(),
                                resourceId);
                    }
                }
                resource.setAgentSynchronizationNeeded();

                AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);
                DriftAgentService service = agentClient.getDriftAgentService();
                try {
                    DriftSnapshot snapshot = null;
                    if (driftDef.getTemplate() != null && driftDef.getTemplate().isPinned()) {
                        snapshot = getSnapshot(subject, new DriftSnapshotRequest(driftDef.getId()));
                    }
                    // Do not pass attached entities to the following Agent call, which is outside Hibernate's control. Flush
                    // and clear the entities to ensure the work above is captured and we pass out a detached object.
                    entityManager.flush();
                    entityManager.clear();

                    if (snapshot != null) {
                        service.updateDriftDetection(resourceId, driftDef, snapshot);
                    } else {
                        service.updateDriftDetection(resourceId, driftDef);
                    }
                } catch (Exception e) {
                    log.warn(" Unable to inform agent of unscheduled drift detection  [" + driftDef + "]", e);
                }

                break;
            }
            default:
                throw new IllegalArgumentException("Entity Context Type not supported [" + entityContext + "]");
        }
    }

    public static void validateDriftDefinition(DriftDefinition driftDef) {
        if (!driftDef.getName().matches(DriftConfigurationDefinition.PROP_NAME_REGEX_PATTERN)) {
            throw new IllegalArgumentException("Drift definition name contains invalid characters: "
                + driftDef.getName());
        }
        BaseDirectory baseDir = driftDef.getBasedir();
        if (null == baseDir
            || !baseDir.getValueName().matches(DriftConfigurationDefinition.PROP_BASEDIR_PATH_REGEX_PATTERN)) {
            throw new IllegalArgumentException(
                "Drift definition base directory is null or contains invalid characters: " + baseDir);
        }
        List<List<Filter>> filtersList = new ArrayList<List<Filter>>(2);
        filtersList.add(driftDef.getIncludes());
        filtersList.add(driftDef.getExcludes());
        for (List<Filter> filterList : filtersList) {
            for (Filter filter : filterList) {
                String path = (null == filter.getPath()) ? null : filter.getPath().trim();
                if (null != path && !path.isEmpty()
                    && !path.matches(DriftConfigurationDefinition.PROP_FILTER_PATH_REGEX_PATTERN)) {
                    throw new IllegalArgumentException("Drift definition filter path contains invalid characters: "
                        + path);
                }
                String pattern = (null == filter.getPattern()) ? null : filter.getPattern().trim();
                if (null != pattern && !pattern.isEmpty()
                    && !pattern.matches(DriftConfigurationDefinition.PROP_FILTER_PATTERN_REGEX_PATTERN)) {
                    throw new IllegalArgumentException("Drift definition filter pattern contains invalid characters: "
                        + pattern);
                }
            }
        }
    }

    private boolean isDriftMgmtSupported(Resource resource) {
        ResourceType type = resource.getResourceType();
        return type.getDriftDefinitionTemplates() != null && !type.getDriftDefinitionTemplates().isEmpty();
    }

    private void validateTemplateForNewDef(DriftDefinition driftDef, Resource resource) {
        if (driftDef.getTemplate() == null) {
            return;
        }

        DriftDefinitionTemplate template = entityManager.find(DriftDefinitionTemplate.class,
                driftDef.getTemplate().getId());

        if (template == null) {
            throw new IllegalArgumentException("Cannot create drift definition with template " +
                    DriftDefinitionTemplate.class.getSimpleName() + "[" + driftDef.getTemplate().getName() +
                    "] that has not been saved");
        }

        if (!template.getResourceType().equals(resource.getResourceType())) {
            throw new IllegalArgumentException("Cannot create drift definition with template " +
                    DriftDefinitionTemplate.class.getSimpleName() + "[" + driftDef.getTemplate().getName() +
                    "] that is from a different resource type, " + template.getResourceType());
        }
    }

    @Override
    public boolean isBinaryFile(Subject subject, Drift<?, ?> drift) {
        return DriftUtil.isBinaryFile(drift);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public DriftDetails getDriftDetails(Subject subject, String driftId) {
        log.debug("Loading drift details for drift id: " + driftId);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterId(driftId);
        criteria.fetchChangeSet(true);
        criteria.setPageControl(PageControl.getSingleRowInstance());

        DriftDetails driftDetails = new DriftDetails();
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();

        DriftFile newFile = null;
        DriftFile oldFile = null;

        PageList<? extends Drift<?, ?>> results = driftServerPlugin.findDriftsByCriteria(subject, criteria);
        if (results.size() == 0) {
            log.warn("Unable to get the drift details for drift id " + driftId
                + ". No drift object found with that id.");
            return null;
        }

        Drift<?, ?> drift = results.get(0);
        driftDetails.setDrift(drift);
        try {
            switch (drift.getCategory()) {
            case FILE_ADDED:
                newFile = driftServerPlugin.getDriftFile(subject, drift.getNewDriftFile().getHashId());
                driftDetails.setNewFileStatus(newFile.getStatus());
                break;
            case FILE_CHANGED:
                newFile = driftServerPlugin.getDriftFile(subject, drift.getNewDriftFile().getHashId());
                oldFile = driftServerPlugin.getDriftFile(subject, drift.getOldDriftFile().getHashId());

                driftDetails.setNewFileStatus(newFile.getStatus());
                driftDetails.setOldFileStatus(oldFile.getStatus());

                driftDetails.setPreviousChangeSet(loadPreviousChangeSet(subject, drift));
                break;
            case FILE_REMOVED:
                oldFile = driftServerPlugin.getDriftFile(subject, drift.getOldDriftFile().getHashId());
                driftDetails.setOldFileStatus(oldFile.getStatus());
                break;
            }
        } catch (Exception e) {
            log.error("An error occurred while loading the drift details for drift id " + driftId + ": "
                + e.getMessage());
            throw new RuntimeException("An error occurred while loading th drift details for drift id " + driftId, e);
        }
        driftDetails.setBinaryFile(isBinaryFile(subject, drift));
        return driftDetails;
    }

    private void notifyAlertConditionCacheManager(String callingMethod, DriftChangeSetSummary summary) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(summary);
        if (log.isDebugEnabled()) {
            log.debug(callingMethod + ": " + stats.toString());
        }
    }

    private DriftChangeSet<?> loadPreviousChangeSet(Subject subject, Drift<?, ?> drift) {
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterResourceId(drift.getChangeSet().getResourceId());
        criteria.addFilterDriftDefinitionId(drift.getChangeSet().getDriftDefinitionId());
        criteria.addFilterVersion(Integer.toString(drift.getChangeSet().getVersion() - 1));
        criteria.setPageControl(PageControl.getSingleRowInstance());

        PageList<? extends DriftChangeSet<?>> results = findDriftChangeSetsByCriteria(subject, criteria);
        // TODO handle empty results
        return results.get(0);
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

    private void authorizeOrFail(Subject subject, int resourceId, String message) {
        if (!authorizationManager.hasResourcePermission(subject, Permission.MANAGE_DRIFT, resourceId)) {
            throw new PermissionException(message + " - " + subject + " lacks "
                + Permission.MANAGE_DRIFT + " for resource[id=" + resourceId + "]");
        }
    }

    private void authorizeOrFail(Subject subject, EntityContext context, String message) {
        authorizeOrFail(subject, context.getResourceId(), message);
    }

}

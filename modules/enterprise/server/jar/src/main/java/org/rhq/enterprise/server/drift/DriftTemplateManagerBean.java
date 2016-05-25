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

import static javax.ejb.TransactionAttributeType.NEVER;

import static org.rhq.core.domain.common.EntityContext.forResource;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

@Stateless
public class DriftTemplateManagerBean implements DriftTemplateManagerLocal, DriftTemplateManagerRemote {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    ResourceTypeManagerLocal resourceTypeMgr;

    @EJB
    DriftManagerLocal driftMgr;

    @EJB
    DriftTemplateManagerLocal templateMgr;

    @Override
    public PageList<DriftDefinitionTemplate> findTemplatesByCriteria(Subject subject,
        DriftDefinitionTemplateCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<DriftDefinitionTemplate> queryRunner = new CriteriaQueryRunner<DriftDefinitionTemplate>(
            criteria, generator, entityMgr);
        PageList<DriftDefinitionTemplate> result = queryRunner.execute();

        return result;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public DriftDefinitionTemplate createTemplate(Subject subject, int resourceTypeId, boolean isUserDefined,
        DriftDefinition definition) {

        try {
            // before we do anything, validate certain field values to prevent downstream errors            
            if (isUserDefined) {
                DriftManagerBean.validateDriftDefinition(definition);
            }

            ResourceType resourceType = resourceTypeMgr.getResourceTypeById(subject, resourceTypeId);
            DriftDefinitionTemplate template = new DriftDefinitionTemplate();
            template.setName(definition.getName());
            template.setDescription(definition.getDescription());
            template.setUserDefined(isUserDefined);
            template.setTemplateDefinition(definition);

            if (isDuplicateName(resourceType, template)) {
                throw new IllegalArgumentException("Drift definition template name must be unique. A template named "
                    + "[" + template.getName() + "] already exists for " + resourceType);
            }

            resourceType.addDriftDefinitionTemplate(template);
            entityMgr.persist(template);

            return template;
        } catch (ResourceTypeNotFoundException e) {
            throw new RuntimeException("Failed to create template", e);
        }
    }

    private boolean isDuplicateName(ResourceType resourceType, DriftDefinitionTemplate newTemplate) {
        for (DriftDefinitionTemplate template : resourceType.getDriftDefinitionTemplates()) {
            if (template.getName().equals(newTemplate.getName())) {
                return true;
            }
        }
        return false;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    @TransactionAttribute(NEVER)
    public void pinTemplate(final Subject subject, int templateId, int driftDefId, int snapshotVersion) {
        templateMgr.createTemplateChangeSet(subject, templateId, driftDefId, snapshotVersion);

        DriftDefinitionTemplateCriteria templateCriteria = new DriftDefinitionTemplateCriteria();
        templateCriteria.addFilterId(templateId);
        templateCriteria.fetchDriftDefinitions(true);
        templateCriteria.setPageControl(PageControl.getSingleRowInstance());

        PageList<DriftDefinitionTemplate> templates = templateMgr.findTemplatesByCriteria(subject, templateCriteria);
        DriftDefinitionTemplate template = templates.get(0);

        DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
        criteria.addFilterTemplateId(templateId);
        criteria.fetchConfiguration(true);
        criteria.fetchResource(true);

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<DriftDefinition, DriftDefinitionCriteria> queryExecutor = new CriteriaQueryExecutor<DriftDefinition, DriftDefinitionCriteria>() {
            @Override
            public PageList<DriftDefinition> execute(DriftDefinitionCriteria criteria) {
                return driftMgr.findDriftDefinitionsByCriteria(subject, criteria);
            }
        };

        CriteriaQuery<DriftDefinition, DriftDefinitionCriteria> definitions = new CriteriaQuery<DriftDefinition, DriftDefinitionCriteria>(
            criteria, queryExecutor);

        for (DriftDefinition def : definitions) {
            if (def.isAttached()) {
                int resourceId = def.getResource().getId();
                driftMgr.deleteDriftDefinition(subject, forResource(resourceId), def.getName());

                DriftDefinition newDef = new DriftDefinition(def.getConfiguration().deepCopyWithoutProxies());
                newDef.setTemplate(template);
                newDef.setPinned(true);

                driftMgr.updateDriftDefinition(subject, forResource(resourceId), newDef);
            }
        }
    }

    @Override
    @TransactionAttribute
    public void createTemplateChangeSet(Subject subject, int templateId, int driftDefId, int snapshotVersion) {
        DriftDefinitionTemplate template = entityMgr.find(DriftDefinitionTemplate.class, templateId);
        DriftSnapshot snapshot = driftMgr.getSnapshot(subject, new DriftSnapshotRequest(driftDefId, snapshotVersion));

        DriftChangeSetDTO changeSetDTO = new DriftChangeSetDTO();
        changeSetDTO.setCategory(COVERAGE);
        changeSetDTO.setDriftHandlingMode(normal);
        changeSetDTO.setVersion(0);

        String newChangeSetId = driftMgr.persistSnapshot(subject, snapshot, changeSetDTO);
        template.setChangeSetId(newChangeSetId);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void deleteTemplate(Subject subject, int templateId) {
        DriftDefinitionTemplate template = entityMgr.find(DriftDefinitionTemplate.class, templateId);
        if (!template.isUserDefined()) {
            throw new IllegalArgumentException(template.getName() + " is a plugin defined template. Plugin defined " +
                "templates cannot be deleted.");
        }
        for (DriftDefinition defintion : template.getDriftDefinitions()) {
            if (defintion.isAttached()) {
                driftMgr.deleteDriftDefinition(subject, forResource(defintion.getResource().getId()), defintion
                    .getName());
            } else {
                defintion.setTemplate(null);
            }
        }
        entityMgr.remove(template);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void updateTemplate(Subject subject, DriftDefinitionTemplate updatedTemplate) {

        DriftDefinitionTemplate template = entityMgr.find(DriftDefinitionTemplate.class, updatedTemplate.getId());

        if (null == template) {
            throw new IllegalArgumentException("Template with id [" + updatedTemplate.getId() + "] not found");
        }
        if (!template.isUserDefined()) {
            throw new IllegalArgumentException("Plugin-defined templates cannot be be modified");
        }
        if (!template.getName().equals(updatedTemplate.getName())) {
            throw new IllegalArgumentException("The template's name cannot be modified");
        }
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            DriftDefinitionComparator.CompareMode.ONLY_DIRECTORY_SPECIFICATIONS);
        if (comparator.compare(template.getTemplateDefinition(), updatedTemplate.getTemplateDefinition()) != 0) {
            throw new IllegalArgumentException("The template's base directory and filters cannot be modified");
        }

        entityMgr.remove(template.getTemplateDefinition());
        template.setTemplateDefinition(updatedTemplate.getTemplateDefinition());
        template = entityMgr.merge(template);
        DriftDefinition templateDef = template.getTemplateDefinition();

        for (DriftDefinition resourceDef : template.getDriftDefinitions()) {
            DriftDefinition driftDef = entityMgr.find(DriftDefinition.class, resourceDef.getId());
            if (driftDef.isAttached()) {
                driftDef.setInterval(templateDef.getInterval());
                driftDef.setDriftHandlingMode(templateDef.getDriftHandlingMode());
                driftDef.setEnabled(templateDef.isEnabled());

                driftMgr.updateDriftDefinition(subject, forResource(driftDef.getResource().getId()), driftDef);
            }
        }
    }

}

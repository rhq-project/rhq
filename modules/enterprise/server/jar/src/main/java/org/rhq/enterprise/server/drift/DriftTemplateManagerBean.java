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

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
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

        DriftDefinitionTemplate result = null;

        try {
            ResourceType resourceType = resourceTypeMgr.getResourceTypeById(subject, resourceTypeId);
            DriftDefinitionTemplate template = new DriftDefinitionTemplate();
            template.setName(definition.getName());
            template.setDescription(definition.getDescription());
            template.setUserDefined(isUserDefined);
            template.setTemplateDefinition(definition);

            resourceType.addDriftDefinitionTemplate(template);
            entityMgr.persist(template);

            return template;
        } catch (ResourceTypeNotFoundException e) {
            throw new RuntimeException("Failed to create template", e);
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void pinTemplate(Subject subject, int templateId, int driftDefId, int snapshotVersion) {
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
    public void updateTemplate(Subject subject, DriftDefinitionTemplate template, boolean applyToDefs) {
        @SuppressWarnings("unused")
        DriftDefinitionTemplate updatedTemplate = entityMgr.merge(template);
    }

}

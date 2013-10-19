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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.util.PageList;

/**
 * Public API for Drift Template Management.
 */
@Remote
public interface DriftTemplateManagerRemote {

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<DriftDefinitionTemplate> findTemplatesByCriteria(Subject subject,
        DriftDefinitionTemplateCriteria criteria);

    /**
     * @param subject
     * @param resourceTypeId
     * @param isUserDefined
     * @param definition
     * @return The new {@link DriftDefinitionTemplate}
     */
    DriftDefinitionTemplate createTemplate(Subject subject, int resourceTypeId, boolean isUserDefined,
        DriftDefinition definition);

    /**
     * @param subject
     * @param templateId
     * @param snapshotDriftDefId
     * @param snapshotVersion
     */
    void pinTemplate(Subject subject, int templateId, int snapshotDriftDefId, int snapshotVersion);

    /**
     * @param subject
     * @param template
     */
    void updateTemplate(Subject subject, DriftDefinitionTemplate template);

    /**
     * @param subject
     * @param templateId
     */
    void deleteTemplate(Subject subject, int templateId);
}

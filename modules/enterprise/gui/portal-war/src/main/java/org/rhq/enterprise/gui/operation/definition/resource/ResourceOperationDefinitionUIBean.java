/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.operation.definition.resource;

import java.util.List;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.operation.definition.OperationDefinitionUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

public class ResourceOperationDefinitionUIBean extends OperationDefinitionUIBean {
    @Override
    protected String getBeanName() {
        return "ResourceOperationDefinitionUIBean";
    }

    @Override
    public PageList<OperationDefinition> getOperationDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        List<OperationDefinition> definitions = operationManager.getSupportedResourceOperations(subject, resource
            .getId());

        return new PageList<OperationDefinition>(definitions, new PageControl(0, definitions.size()));
    }
}
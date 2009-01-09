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
package org.rhq.enterprise.gui.operation.definition;

import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class OperationDefinitionUIBean extends PagedDataTableUIBean {
    protected OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private OperationDefinition operationDefinition = null;

    protected String timeout = null;
    protected String description = null;

    public OperationDefinitionUIBean() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Integer operationId = FacesContextUtility.getRequiredRequestParameter("opId", Integer.class);

        this.operationDefinition = operationManager.getOperationDefinition(subject, operationId);

        Integer defaultTimeout = this.operationDefinition.getTimeout();
        this.timeout = (defaultTimeout == null) ? "" : String.valueOf(defaultTimeout);
    }

    public String getName() {
        return this.operationDefinition.getName();
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getTimeout() {
        return this.timeout;
    }

    public String getDescription() {
        if (description == null) {
            description = FacesContextUtility.getOptionalRequestParameter("newScheduleForm:notes", String.class, "");
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListOperationDefinitionDataModel(PageControlView.NONE, getBeanName());
        }

        return dataModel;
    }

    public abstract PageList<OperationDefinition> getOperationDefinitions();

    protected abstract String getBeanName();

    private class ListOperationDefinitionDataModel extends PagedListDataModel<OperationDefinition> {
        public ListOperationDefinitionDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<OperationDefinition> fetchPage(PageControl pc) {
            return getOperationDefinitions();
        }
    }
}
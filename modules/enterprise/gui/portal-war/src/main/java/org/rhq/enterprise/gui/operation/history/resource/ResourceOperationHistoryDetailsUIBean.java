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
package org.rhq.enterprise.gui.operation.history.resource;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.operation.model.OperationParameters;
import org.rhq.enterprise.gui.operation.model.OperationResults;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceOperationHistoryDetailsUIBean {
    private OperationHistory history;
    private OperationParameters parameters;
    private OperationResults results;

    private void init() {
        if (this.history == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer operationId = FacesContextUtility.getRequiredRequestParameter("opId", Integer.class);
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();

            this.history = operationManager.getOperationHistoryByHistoryId(subject, operationId);

            this.parameters = new OperationParameters(this.history);

            this.results = new OperationResults((ResourceOperationHistory) history);
        }
    }

    public OperationHistory getHistory() {
        init();

        return this.history;
    }

    public OperationParameters getParameters() {
        init();

        return parameters;
    }

    public OperationResults getResults() {
        init();

        return results;
    }
}
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
package org.rhq.core.domain.operation.composite;

import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;

public abstract class OperationLastCompletedComposite {
    private final int operationHistoryId;
    private final String operationName;
    private final long operationStartTime;
    private final OperationRequestStatus operationStatus;

    public OperationLastCompletedComposite(int operationHistoryId, String operationName, long operationStartTime,
        OperationRequestStatus operationStatus) {
        this.operationHistoryId = operationHistoryId;
        this.operationName = operationName;
        this.operationStartTime = operationStartTime;
        this.operationStatus = operationStatus;
    }

    /**
     * This is the operation history ID.
     *
     * @return ID of the {@link OperationHistory} item
     */
    public int getOperationHistoryId() {
        return operationHistoryId;
    }

    public String getOperationName() {
        return operationName;
    }

    public long getOperationStartTime() {
        return operationStartTime;
    }

    public OperationRequestStatus getOperationStatus() {
        return operationStatus;
    }
}
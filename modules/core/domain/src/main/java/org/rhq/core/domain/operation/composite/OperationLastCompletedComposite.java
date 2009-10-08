 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.domain.operation.composite;

import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;

import java.io.Serializable;

public abstract class OperationLastCompletedComposite implements Serializable {

    private static final long serialVersionUID = 1L;

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
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
package org.rhq.enterprise.server.alert;

import javax.ejb.Local;
import javax.ejb.TransactionAttributeType;

import org.rhq.core.domain.alert.Alert;
import org.rhq.enterprise.server.alert.engine.jms.model.AbstractAlertConditionMessage;

/**
 * @author Joseph Marques
 */
@Local
public interface CachedConditionManagerLocal {
    /**
     * The entry point to all out-of-band alert condition processing. By extracting this
     * into it's own interface, it can very easily be run in a new transaction using the
     * {@link TransactionAttributeType.REQUIRES_NEW} annotation. Thus, when this method
     * completes, the caller knows that any information it has committed to the database
     * will be visible to it as well as any other thread that is blocked, waiting for it
     * to complete.
     *
     * @param conditionMessage
     * @param definitionId
     * @return the newly fired alert resulting from the condition message, or null if no alert was fired.
     */
    Alert processCachedConditionMessageNewTx(AbstractAlertConditionMessage conditionMessage, Integer definitionId);
}

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
package org.rhq.core.domain.alert.composite;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.measurement.DataType;

/**
 * @author Joseph Marques
 */
public class AlertConditionChangesCategoryComposite extends AlertConditionScheduleCategoryComposite {

    /*
     * it would be nice to preload the current measurement numeric value for this condition at the time
     * of object creation, but since we don't have a proper entity for it (because we have several raw 
     * measurement tables with rotation logic at the business layer), we can not easily query the current
     * value via JPQL; instead, and for simplicity's sake, let's assume that most of the measurement-based
     * alert conditions will be threshold-oriented (which can be queried efficiently).
     */
    public AlertConditionChangesCategoryComposite(AlertCondition condition, Integer scheduleId, DataType dataType) {
        super(condition, scheduleId, dataType);
    }

}

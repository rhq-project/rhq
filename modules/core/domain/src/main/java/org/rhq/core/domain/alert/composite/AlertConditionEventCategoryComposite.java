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
import org.rhq.core.domain.alert.AlertConditionCategory;

/**
 * @author Joseph Marques
 */
public class AlertConditionEventCategoryComposite extends AbstractAlertConditionCategoryComposite {

    private final Integer resourceId;

    public AlertConditionEventCategoryComposite(
            int cid, AlertConditionCategory ccategory, String cname, String ccomparator, Double cthreshold, String coption, Integer triggerId,
            Integer resourceId) {
        super(new AlertCondition(cid, ccategory, cname, ccomparator, cthreshold, coption, triggerId));
        this.resourceId = resourceId;
    }

    public Integer getResourceId() {
        return resourceId;
    }

}

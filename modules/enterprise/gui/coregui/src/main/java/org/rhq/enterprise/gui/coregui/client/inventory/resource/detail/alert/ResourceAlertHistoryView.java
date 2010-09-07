/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.alert;

import com.smartgwt.client.data.Criteria;

import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.alert.AlertsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;

/**
 * @author Ian Springer
 */
public class ResourceAlertHistoryView extends AlertsView implements ResourceSelectListener {
    private static final String[] EXCLUDED_FIELD_NAMES = new String[] { AlertCriteria.SORT_FIELD_RESOURCE_ID };

    public ResourceAlertHistoryView(String locatorId, int resourceId) {
        super(locatorId, createCriteria(resourceId), EXCLUDED_FIELD_NAMES);
    }

    public void onResourceSelected(ResourceComposite resourceComposite) {
        refresh(createCriteria(resourceComposite.getResource().getId()));
    }

    private static Criteria createCriteria(int resourceId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(AlertCriteria.SORT_FIELD_RESOURCE_ID, resourceId);
        return criteria;
    }
}

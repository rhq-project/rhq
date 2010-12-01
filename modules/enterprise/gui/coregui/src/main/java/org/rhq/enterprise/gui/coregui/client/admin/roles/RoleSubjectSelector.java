/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersDataSource;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class RoleSubjectSelector extends AbstractSelector<Subject> {
    
    private static final String ITEM_ICON = "global/User_16.png";

    public RoleSubjectSelector(String id, ListGridRecord[] subjectRecords, boolean isReadOnly) {
        super(id, isReadOnly);
        
        setAssigned(subjectRecords);
    }

    @Override
    protected RPCDataSource<Subject> getDataSource() {
        return new RoleUsersDataSource();
    }

    @Override
    protected DynamicForm getAvailableFilterForm() {
        return null; // No Filters Currently
    }

    @Override
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        return null; // No Filters Currently
    }

    @Override
    protected String getItemTitle() {
        return MSG.common_title_users();
    }

    @Override
    protected String getItemIcon() {
        return ITEM_ICON;
    }

    class RoleUsersDataSource extends UsersDataSource {
        @Override
        protected void sendSuccessResponseRecords(DSRequest request, DSResponse response, PageList<Record> records) {
            Record rhqAdminRecord = null;
            for (Record record : records) {
                Integer id = record.getAttributeAsInt(Field.ID);
                if (id.equals(ID_RHQADMIN)) {
                   rhqAdminRecord = record;
                }
            }
            if (rhqAdminRecord != null) {
                records.remove(rhqAdminRecord);
            }
            super.sendSuccessResponseRecords(request, response, records);
        }
    }

}

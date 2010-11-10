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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersDataSource;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * This notification form will be used for the System Users sender. This form lets
 * you pick users that the sender will send notifications to.
 *
 * @author John Mazzitelli
 */
public class SystemUsersNotificationSenderForm extends AbstractNotificationSenderForm {

    // the alert configuration property name where the IDs are stored in |-separated form
    private static final String PROPNAME = "subjectId";

    private SubjectSelector selector;

    public SystemUsersNotificationSenderForm(String locatorId, AlertNotification notif, String sender) {
        super(locatorId, notif, sender);
    }

    @Override
    protected void onInit() {
        super.onInit();

        String subjectIds = getConfiguration().getSimpleValue(PROPNAME, ""); // we know the subject plugin defines this
        if (subjectIds != null && subjectIds.length() > 0) {
            try {
                List<Integer> ids = unfence(subjectIds, Integer.class);
                SubjectCriteria criteria = new SubjectCriteria();
                criteria.addFilterIds(ids.toArray(new Integer[ids.size()]));
                GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
                    new AsyncCallback<PageList<Subject>>() {
                        @Override
                        public void onSuccess(PageList<Subject> result) {
                            createNewSelector(result);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Cannot determine current users - starting empty",
                                caught);
                            createNewSelector(null);
                        }
                    });
            } catch (Exception e) {
                CoreGUI.getErrorHandler().handleError("Cannot use current users - starting empty", e);
                createNewSelector(null);
            }
        } else {
            createNewSelector(null);
        }
    }

    private void createNewSelector(Collection<Subject> preselectedSubjects) {
        String selectorLocatorId = extendLocatorId("userSelector");
        selector = new SubjectSelector(selectorLocatorId, preselectedSubjects);
        selector.setWidth(400);
        selector.setHeight(300);
        addMember(selector);
        markForRedraw();
    }

    @Override
    public boolean validate() {
        if (selector != null) {
            try {
                Set<Integer> selectedIds = selector.getSelection();
                String newPropValue = fence(selectedIds);
                getConfiguration().put(new PropertySimple(PROPNAME, newPropValue));
                return true;
            } catch (Exception e) {
                CoreGUI.getErrorHandler().handleError("Cannot save the selected users", e);
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> unfence(String fencedData, Class<T> type) {
        String delimiter = "\\|";
        String[] elements = fencedData.split(delimiter);
        List<T> results = new ArrayList<T>(elements.length);

        if (Integer.class.equals(type)) {
            for (String next : elements) {
                if (next.length() != 0) {
                    results.add((T) Integer.valueOf(next));
                }
            }
        } else if (String.class.equals(type)) {
            for (String next : elements) {
                if (next.length() != 0) {
                    results.add((T) next);
                }
            }
        } else {
            throw new IllegalArgumentException("No support for unfencing data of type " + type);
        }
        return results;
    }

    /**
     * Takes the list of elements e1, e2, e3 and fences
     * them with '|' delimiters such that the result looks
     * like "|e1|e2|e3|"
     */
    private String fence(Collection<?> elements) {
        if (elements.size() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('|');
        for (Object next : elements) {
            builder.append(String.valueOf(next)).append('|');
        }
        return builder.toString();
    }

    private class SubjectSelector extends AbstractSelector<Subject> {

        public SubjectSelector(String id, Collection<Subject> subjects) {
            super(id);
            if (subjects != null) {
                ListGridRecord[] data = (new UsersDataSource()).buildRecords(subjects);
                setAssigned(data);
            }
        }

        @Override
        protected RPCDataSource<Subject> getDataSource() {
            return new SelectedSubjectsDataSource();
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
            return "user";
        }

        public class SelectedSubjectsDataSource extends UsersDataSource {

            @Override
            public ListGridRecord[] buildRecords(Collection<Subject> subjects) {
                ListGridRecord[] records = super.buildRecords(subjects);
                for (ListGridRecord record : records) {
                    if (selection.contains(record.getAttributeAsInt("id"))) {
                        record.setEnabled(false);
                    }
                }
                return records;
            }
        }

    }
}

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;

import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Simeon Pinder
 */
//public class RoleLdapGroupSelector extends AbstractSelector<HashSet<Map<String, String>>> {
public class RoleLdapGroupSelector extends AbstractSelector<PageList<LdapGroup>> {
    public static final String id = "id";
    public static final String name = "name";
    public static final String description = "description";
    public static final String AVAILABLE_GROUPS = "Available Groups";
    public static final String SELECTED_GROUPS = "Selected Groups";
    private LdapGroupsDataSource availableDatasource;
    protected HashSet<String> selection = new HashSet<String>();
    private int currentRole = -1;
    private boolean initialLdapSelectionsLoad = true;

    //    public RoleLdapGroupSelector(String locatorId, Set<LdapGroup> available, Set<LdapGroup> assigned) {
    //        super(locatorId);
    //        if (available != null) {
    //            ListGridRecord[] data = (new LdapGroupsDataSource()).buildRecords(available);
    ////            setAssigned(data);
    ////            setA
    //        }
    //    }

    public RoleLdapGroupSelector(String locatorId, Integer integer) {
        super(locatorId);
        if (integer != null) {
            this.currentRole = integer.intValue();
        }
    }

    @Override
    protected DynamicForm getAvailableFilterForm() {
        return null; // TODO: Implement this method.
    }

    @Override
    //    protected RPCDataSource<HashSet<Map<String, String>>> getDataSource() {
    protected RPCDataSource<PageList<LdapGroup>> getDataSource() {
        if (availableDatasource == null) {
            availableDatasource = new LdapGroupsDataSource();
            Log.debug("++++++++++ RoleLDapGroupSelector.datasourceInit:" + availableDatasource);
            //add subsequent listener
            int currentRoleId = getCurrentRole();
            if (currentRoleId > -1) {

                //add listener to AvailableGrid, to act after successfully populated.
                getAvailableGrid().addDataArrivedHandler(new DataArrivedHandler() {
                    @Override
                    public void onDataArrived(DataArrivedEvent event) {
                        int currentRoleId = getCurrentRole();
                        if (currentRoleId > -1) {
                            if (initialLdapSelectionsLoad) {
                                GWTServiceLookup.getLdapService().findLdapGroupsAssignedToRole(currentRoleId,
                                //                                    new AsyncCallback<Set<Map<String, String>>>() {
                                    new AsyncCallback<PageList<LdapGroup>>() {

                                        public void onFailure(Throwable throwable) {
                                            CoreGUI.getErrorHandler().handleError(
                                                "Failed to load LdapGroups available for role.", throwable);
                                        }

                                        //                                        public void onSuccess(Set<Map<String, String>> currentlyAssignedLdapGroups) {
                                        public void onSuccess(PageList<LdapGroup> currentlyAssignedLdapGroups) {
                                            //translate groups into records for grid
                                            //                    response.setData(buildRecords(locatedGroups));
                                            //                        response.setData(buildAssignedRecords(currentlyAssignedLdapGroups));
                                            //instead of setting the data, find which ones are shared and transfer as before
                                            if ((currentlyAssignedLdapGroups != null)
                                                && (!currentlyAssignedLdapGroups.isEmpty())) {
                                                RecordList loaded = availableGrid.getDataAsRecordList();
                                                if (loaded != null) {
                                                    ArrayList<Integer> located = new ArrayList<Integer>();
                                                    //                                                    for (Map groupMap : currentlyAssignedLdapGroups) {
                                                    for (LdapGroup group : currentlyAssignedLdapGroups) {
                                                        //                                                        int index = loaded.findIndex(name, (String) groupMap.get(name));
                                                        int index = loaded.findIndex(name, (String) group.getName());
                                                        if (index > -1) {
                                                            group.setId(index);//overwrite RHQ Resource ID to match ldap fabricated id.
                                                            located.add(Integer.valueOf(index));
                                                        }
                                                    }
                                                    int[] records = new int[located.size()];
                                                    int i = 0;
                                                    for (Integer index : located) {
                                                        records[i++] = index.intValue();
                                                    }
                                                    availableGrid.selectRecords(records);
                                                    //now simulate button push
                                                    assignedGrid.transferSelectedData(availableGrid);
                                                    initialLdapSelectionsLoad = false;
                                                    select(assignedGrid.getSelection());
                                                    updateButtons();
                                                    assignedGrid.deselectAllRecords();
                                                    //                                                    assignedGrid.deselectAllRecords();
                                                    //                                                    assignedGrid.transferSelectedData(availableGrid);
                                                    //                                                    select(assignedGrid.getSelection());
                                                    //                                                    updateButtons();
                                                    Record rec = assignedGrid.getDataAsRecordList().get(0);
                                                    //                                                    for (String attr : rec.getAttributes()) {
                                                    //                                                        Log.debug("%%%%%%%%%% attribute:" + attr + ":value:"
                                                    //                                                            + rec.getAttribute(attr) + ":");
                                                    //                                                    }
                                                }
                                            }
                                        }
                                    });
                            }
                        }
                    }
                });
            }
        }
        return availableDatasource;
    }

    @Override
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        return null; // TODO: Implement this method.
    }

    //    protected void select(ListGridRecord[] records) {
    //        availableGrid.deselectAllRecords();
    //        for (ListGridRecord record : records) {
    //            record.setEnabled(false);
    //            selection.add(record.getAttributeAsString(name));
    //        }
    //        assignedGrid.markForRedraw();
    //    }
    //
    //    protected void deselect(ListGridRecord[] records) {
    //        HashSet<String> toRemove = new HashSet<String>();
    //        for (ListGridRecord record : records) {
    //            toRemove.add(record.getAttributeAsString(name));
    //        }
    //        selection.removeAll(toRemove);
    //
    //        for (String name : toRemove) {
    //            Record r = availableGrid.getDataAsRecordList().find(name, name);
    //            if (r != null) {
    //                ((ListGridRecord) r).setEnabled(true);
    //            }
    //        }
    //        int cnt = 0;
    //        for (Record lgr : availableGrid.getDataAsRecordList().toArray()) {
    //            if (lgr.getAttributeAsBoolean("enabled")) {
    //                cnt++;
    //            }
    //        }
    //        availableGrid.markForRedraw();
    //    }

    //    public class LdapGroupsDataSource extends RPCDataSource<HashSet<Map<String, String>>> {
    public class LdapGroupsDataSource extends RPCDataSource<PageList<LdapGroup>> {

        public static final String LDAP_NOT_CONFIGURED_EMPTY_MESSAGE = "(LDAP not configured. 'Administrator'->System Settings to change)";
        public static final String EMPTY_MESSAGE = "No items to show";

        public LdapGroupsDataSource() {
            DataSourceTextField nameField = new DataSourceTextField(name, name);
            nameField.setPrimaryKey(true);

            DataSourceTextField descriptionField = new DataSourceTextField(description, description);

            setFields(nameField, descriptionField);
        }

        //        public ListGridRecord[] buildRecords(Set<Map<String, String>> locatedGroups) {
        //        public ListGridRecord[] buildRecords(PageList<LdapGroup> locatedGroups) {
        public ListGridRecord[] buildRecords(Set<LdapGroup> locatedGroups) {
            ListGridRecord[] records = new ListGridRecord[0];
            int indx = 0;
            if ((locatedGroups != null) && (!locatedGroups.isEmpty())) {
                //load groupsData
                records = new ListGridRecord[locatedGroups.size()];
                int index = 0;
                //for each Map returned then iterate over to retrieve the values
                //                Iterator<Map<String, String>> iterator = locatedGroups.iterator();
                //                while (iterator.hasNext()) {
                for (LdapGroup group : locatedGroups) {
                    //                    Map<String, String> group = iterator.next();
                    //iterate over the group data to translate into records
                    ListGridRecord record = new ListGridRecord();
                    //load identifier 
                    record.setAttribute(id, index++);
                    //load name
                    //                    record.setAttribute(name, group.get(name));
                    record.setAttribute(name, group.getName());
                    //load description 
                    //                    record.setAttribute(description, group.get(description));
                    record.setAttribute(description, group.getDescription());
                    records[indx++] = record;
                }

                for (ListGridRecord record : records) {
                    if (selection.contains(record.getAttributeAsInt("id"))) {
                        record.setEnabled(false);
                    }
                }
            }
            return records;
        }

        @Override
        //        public HashSet<Map<String, String>> copyValues(ListGridRecord from) {
        public PageList<LdapGroup> copyValues(ListGridRecord from) {
            throw new UnsupportedOperationException("Ldap Group data is read only");
        }

        @Override
        //        public ListGridRecord copyValues(HashSet<Map<String, String>> from) {
        public ListGridRecord copyValues(PageList<LdapGroup> from) {
            return null;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            //determine if ldap enabled, if so then chain and proceed with finding groups
            GWTServiceLookup.getLdapService().checkLdapConfiguredStatus(new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean ldapConfigured) {
                    if (ldapConfigured) {
                        availableGrid.setEmptyMessage(EMPTY_MESSAGE);
                        GWTServiceLookup.getLdapService().findAvailableGroups(
                            new AsyncCallback<Set<Map<String, String>>>() {

                                public void onFailure(Throwable throwable) {
                                    CoreGUI.getErrorHandler().handleError(
                                        "Failed to load LdapGroups available for role.", throwable);
                                }

                                public void onSuccess(Set<Map<String, String>> locatedGroups) {
                                    Log.trace("Successfully located groups.");
                                    Log.debug("---------------------------------- Available groups:"
                                        + locatedGroups.size());
                                    //translate groups into records for grid
                                    //                                    response.setData(buildRecords(locatedGroups));
                                    //                                    Set<LdapGroup> collection = new HashSet<LdapGroup>();
                                    response.setData(buildRecords(convertToCollection(locatedGroups)));
                                    //entry count
                                    if (null != locatedGroups) {
                                        response.setTotalRows(locatedGroups.size());
                                    } else {
                                        response.setTotalRows(0);
                                    }
                                    //pass off for processing
                                    processResponse(request.getRequestId(), response);
                                }

                                //                                private PageList<LdapGroup> convertToCollection(Set<Map<String, String>> locatedGroups) {
                                //                                private Set<LdapGroup> convertToCollection(Set<Map<String, String>> locatedGroups) {
                                //                                    Set<LdapGroup> converted = new HashSet<LdapGroup>();
                                //                                    if (locatedGroups != null) {
                                //                                        Iterator<Map<String, String>> iterator = locatedGroups.iterator();
                                //                                        while (iterator.hasNext()) {
                                //                                            Map<String, String> map = iterator.next();
                                //                                            LdapGroup group = new LdapGroup();
                                //                                            group.setDescription(map.get("description"));
                                //                                            group.setName(map.get("name"));
                                //                                            converted.add(group);
                                //                                        }
                                //                                    }
                                //                                    return converted;
                                //                                }
                            });
                    } else {
                        Log.debug("(LDAP not currently enabled. " + EMPTY_MESSAGE);
                        response.setTotalRows(0);
                        availableGrid.setEmptyMessage(LDAP_NOT_CONFIGURED_EMPTY_MESSAGE);
                        processResponse(request.getRequestId(), response);
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.error("Unable to determine whether ldap configured - check server logs.");
                }
            });
        }
    }

    public static Set<LdapGroup> convertToCollection(Set<Map<String, String>> locatedGroups) {
        Set<LdapGroup> converted = new HashSet<LdapGroup>();
        if (locatedGroups != null) {
            Iterator<Map<String, String>> iterator = locatedGroups.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                Map<String, String> map = iterator.next();
                LdapGroup group = new LdapGroup();
                group.setDescription(map.get("description"));
                group.setName(map.get("name"));
                group.setId(index++);
                converted.add(group);
            }
        }
        return converted;
    }

    //    public HashSet<String> getGroupSelection() {
    //        RecordList records = assignedGrid.getDataAsRecordList();
    //        //empty out selection and populate with actual contents
    //        selection.clear();
    //        if (!records.isEmpty()) {
    //            for (Record r : records.toArray()) {
    //                selection.add(r.getAttributeAsString(name));
    //            }
    //        }
    //        HashSet<String> assignedSelections = new HashSet<String>();
    //        for (ListGridRecord r : assignedGrid.getSelection()) {
    //            assignedSelections.add(r.getAttributeAsString(name));
    //        }
    //        HashSet<String> remainingRecords = new HashSet<String>();
    //        for (Record r : assignedGrid.getDataAsRecordList().toArray()) {
    //            remainingRecords.add(r.getAttributeAsString(name));
    //        }
    //        return remainingRecords;
    //    }

    public class LdapAssignedGroupsDatasource extends RPCDataSource<Set<String>> {
        private Integer currentRoleId = Integer.valueOf(-1);

        public LdapAssignedGroupsDatasource(Integer integer) {
            if (integer != null) {
                this.currentRoleId = integer;
            }
            getAssignedGrid().invalidateCache();
            getAssignedGrid().markForRedraw();
        }

        @Override
        public Set<String> copyValues(ListGridRecord from) {
            return null;
        }

        @Override
        public ListGridRecord copyValues(Set<String> from) {
            return null;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            int currentRoleId = -1;
            if ((getCurrentRoleId() != null) && (getCurrentRoleId().intValue() > -1)) {
                currentRoleId = getCurrentRoleId().intValue();
            }

            GWTServiceLookup.getLdapService().findLdapGroupsAssignedToRole(currentRoleId,
            //                new AsyncCallback<Set<Map<String, String>>>() {
                new AsyncCallback<PageList<LdapGroup>>() {

                    public void onFailure(Throwable throwable) {
                        CoreGUI.getErrorHandler().handleError("Failed to load LdapGroups available for role.",
                            throwable);
                    }

                    //                    public void onSuccess(Set<Map<String, String>> currentlyAssignedLdapGroups) {
                    public void onSuccess(PageList<LdapGroup> currentlyAssignedLdapGroups) {
                        //translate groups into records for grid
                        //                    response.setData(buildRecords(locatedGroups));
                        //                        response.setData(buildAssignedRecords(currentlyAssignedLdapGroups));
                        //instead of setting the data, find which ones are shared and transfer as before
                        RecordList loaded = getAssignedGrid().getDataAsRecordList();
                        ArrayList<Integer> located = new ArrayList<Integer>();
                        //                        for (Map groupMap : currentlyAssignedLdapGroups) {
                        for (LdapGroup groupMap : currentlyAssignedLdapGroups) {
                            //                            int index = loaded.findIndex(name, (String) groupMap.get(name));
                            int index = loaded.findIndex(id, groupMap.getId());
                            if (index > -1) {
                                located.add(Integer.valueOf(index));
                            }
                        }
                        int[] records = new int[located.size()];
                        int i = 0;
                        for (Integer index : located) {
                            records[i++] = index.intValue();
                        }
                        getAssignedGrid().selectRecords(records);
                        //now simulate button push
                        assignedGrid.transferSelectedData(availableGrid);
                        select(assignedGrid.getSelection());
                        updateButtons();

                        //entry count
                        if (null != currentlyAssignedLdapGroups) {
                            response.setTotalRows(currentlyAssignedLdapGroups.size());
                        } else {
                            response.setTotalRows(0);
                        }
                        //pass off for processing
                        processResponse(request.getRequestId(), response);
                    }
                });
        }

        public ListGridRecord[] buildAssignedRecords(Set<LdapGroup> currentlyAssignedLdapGroups) {
            ListGridRecord[] records = new ListGridRecord[0];
            int index = 0;
            if ((currentlyAssignedLdapGroups != null) && (!currentlyAssignedLdapGroups.isEmpty())) {
                //load groupsData
                records = new ListGridRecord[currentlyAssignedLdapGroups.size()];
                for (LdapGroup group : currentlyAssignedLdapGroups) {
                    ListGridRecord record = new ListGridRecord();
                    //                    record.setAttribute(id, group.getName());
                    record.setAttribute(id, group.getId());
                    //load name 
                    record.setAttribute(name, group.getName());
                    //load description 
                    record.setAttribute(description, group.getDescription());

                    records[index++] = record;
                }
            }
            return records;
        }

        public Integer getCurrentRoleId() {
            return currentRoleId;
        }

        public void setCurrentRoleId(Integer currentRoleId) {
            this.currentRoleId = currentRoleId;
        }
    }

    public int getCurrentRole() {
        return currentRole;
    }
}

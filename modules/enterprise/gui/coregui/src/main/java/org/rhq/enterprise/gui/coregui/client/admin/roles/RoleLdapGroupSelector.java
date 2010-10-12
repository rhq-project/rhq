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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Simeon Pinder
 */
public class RoleLdapGroupSelector extends AbstractSelector<HashSet<Map<String, String>>> {
    public static final String id = "id";
    public static final String name = "name";
    public static final String description = "description";
    public static final String AVAILABLE_GROUPS = "Available Groups";
    public static final String SELECTED_GROUPS = "Selected Groups";
    private LdapGroupsDataSource availableDatasource;
    //    private LdapAssignedGroupsDatasource assignedDataSource;
    protected HashSet<String> selection = new HashSet<String>();
    private int currentRole = -1;
    private boolean initialLdapSelectionsLoad = true;

    public RoleLdapGroupSelector(String locatorId, Integer integer) {
        super(locatorId);
        //define datasource for AvailableLdap groups
        //        dataSource = new LdapGroupsDataSource();
        //        getAvailableGrid().setDataSource(dataSource);
        System.out.println("------- instantiating ldapGroupsDataSource:" + integer);
        this.currentRole = integer.intValue();
        //        assignedDataSource = new LdapAssignedGroupsDatasource(integer);
        System.out.println("----- instantiated: about to assign:");
        //        getAssignedGrid().setDataSource(assignedDataSource);
        //        System.out.println("--- set the datasource");
        //        if (integer != null) {
        //            //            ListGridRecord[] data = (new RolesDataSource()).buildRecords(set);
        //            ListGridRecord[] data = dataSource.buildAssignedRecords(integer);
        //            setAssigned(data);
        //        }
    }

    @Override
    protected DynamicForm getAvailableFilterForm() {
        return null; // TODO: Implement this method.
    }

    @Override
    protected RPCDataSource<HashSet<Map<String, String>>> getDataSource() {
        //        return new SelectedRolesDataSource();
        if (availableDatasource == null) {
            availableDatasource = new LdapGroupsDataSource();
            //add subsequent listener
            //add listener to AvailableGrid, to act after successfully populated.
            getAvailableGrid().addDataArrivedHandler(new DataArrivedHandler() {
                @Override
                public void onDataArrived(DataArrivedEvent event) {
                    System.out.println("----------- in ldapAss.executeFetch:" + getCurrentRole() + ":initialLdapLoad:"
                        + initialLdapSelectionsLoad);
                    int currentRoleId = getCurrentRole();
                    if (initialLdapSelectionsLoad) {
                        GWTServiceLookup.getLdapService().findLdapGroupsAssignedToRole(currentRoleId,
                            new AsyncCallback<Set<Map<String, String>>>() {

                                public void onFailure(Throwable throwable) {
                                    CoreGUI.getErrorHandler().handleError(
                                        "Failed to load LdapGroups available for role.", throwable);
                                }

                                public void onSuccess(Set<Map<String, String>> currentlyAssignedLdapGroups) {
                                    System.out.println("------ ldapAss.fetch.success:" + currentlyAssignedLdapGroups);
                                    //translate groups into records for grid
                                    //                    response.setData(buildRecords(locatedGroups));
                                    //                        response.setData(buildAssignedRecords(currentlyAssignedLdapGroups));
                                    //instead of setting the data, find which ones are shared and transfer as before
                                    if ((currentlyAssignedLdapGroups != null)
                                        && (!currentlyAssignedLdapGroups.isEmpty())) {
                                        RecordList loaded = availableGrid.getDataAsRecordList();
                                        ArrayList<Integer> located = new ArrayList<Integer>();
                                        //                                    for (LdapGroup group : currentlyAssignedLdapGroups) {
                                        for (Map groupMap : currentlyAssignedLdapGroups) {
                                            System.out.println("------- ldapAss.fetch.suc.grpName:"
                                                + groupMap.get(name) + ":");
                                            //                                            .println("------- ldapAss.fetch.suc.grpName:" + group.getName() + ":");
                                            //                                        int index = loaded.findIndex(name, group.getName());
                                            int index = loaded.findIndex(name, (String) groupMap.get(name));
                                            if (index > -1) {
                                                located.add(Integer.valueOf(index));
                                            }
                                        }
                                        System.out.println("--------located:" + located.size());
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
                                    }
                                }
                            });
                    }
                }
            });

        }
        return availableDatasource;
    }

    @Override
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        return null; // TODO: Implement this method.
    }

    //    @Override
    //    protected void onInit() {
    //        super.onInit();
    //        removeButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT);
    //        removeButton.setDisabled(true);
    //        //        addAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT_ALL);
    //        removeAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT_ALL);
    //        removeAllButton.setDisabled(true);
    //        removeButton.addClickHandler(new ClickHandler() {
    //            public void onClick(ClickEvent clickEvent) {
    //                System.out.println("Custom remove Button:");
    //                ListGridRecord[] storedSelection = assignedGrid.getSelection();
    //                deselect(assignedGrid.getSelection());
    //                assignedGrid.removeSelectedData();
    //                //now walk through and re-enable all selected elements in Available grid
    //                RecordList availList = availableGrid.getDataAsRecordList();
    //                for (ListGridRecord r : storedSelection) {
    //                    Record record = availList.find(name, r.getAttributeAsString(name));
    //                    if (record != null)
    //                        ((ListGridRecord) record).setEnabled(true);
    //                }
    //                updateButtons();
    //            }
    //        });
    //        removeAllButton.addClickHandler(new ClickHandler() {
    //            public void onClick(ClickEvent clickEvent) {
    //                assignedGrid.selectAllRecords();
    //                deselect(assignedGrid.getSelection());
    //                assignedGrid.removeSelectedData();
    //                updateButtons();
    //            }
    //        });
    //
    //    }

    //    HashSet<String> selection = new HashSet<String>();

    //    @Override
    //    protected void select(ListGridRecord[] records) {
    //        availableGrid.deselectAllRecords();
    //        //        RecordList recordList = availableGrid.getDataAsRecordList();
    //        for (ListGridRecord record : records) {
    //            record.setEnabled(false);
    //            //            selection.add(record.getAttributeAsInt("id"));
    //            //            int selectedIndex = -1;
    //            String groupName = record.getAttributeAsString(name);
    //            //            selectedIndex = recordList.findIndex(name, groupName);
    //            //            selection.add(Integer.valueOf(selectedIndex));
    //            selection.add(groupName);
    //        }
    //        assignedGrid.markForRedraw();
    //    }
    protected void select(ListGridRecord[] records) {
        System.out.println("********** Current Group Selection size:" + getGroupSelection().size());
        System.out.println("******** RoleLdapGroupSelector.select:" + records.length);
        availableGrid.deselectAllRecords();
        for (ListGridRecord record : records) {
            record.setEnabled(false);
            //            selection.add(record.getAttributeAsInt("id"));
            //            selection.add(record.getAttributeAsString(name));
            selection.add(record.getAttributeAsString(name));
        }
        System.out.println("********** Current Group Selection size:" + getGroupSelection().size());
        assignedGrid.markForRedraw();
    }

    @Override
    //    protected void deselect(ListGridRecord[] records) {
    //        HashSet<Integer> toRemove = new HashSet<Integer>();
    //        for (ListGridRecord record : records) {
    //            //            System.out.println("------- record.getAttribAsInt:" + record.getAttributeAsInt(name));
    //            String value = null;
    //            //            System.out.println("------- record.getAttribAsInt:" + record.getAttribute(name));
    //            //            System.out.println("------- record.getAttribAsString:" + (value = record.getAttributeAsString(name)));
    //            //            System.out.println("------- record.getAttribAsString-id:" + record.getAttributeAsString(id));
    //            //            if (record.getAttributeAsInt("id") != null) {
    //            if (value != null) {
    //                //                toRemove.add(record.getAttributeAsInt("id"));
    //                System.out.println("--------Inside getAttr as string:" + value + ":");
    //                int found = availableGrid.getDataAsRecordList().findIndex(name, record.getAttributeAsString(name));
    //                System.out.println("---------- index:" + found);
    //                if (found > -1) {
    //                    toRemove.add(Integer.valueOf(found));
    //                }
    //            }
    //        }
    //        selection.removeAll(toRemove);
    //
    //        for (Integer id : toRemove) {
    //            //            Record r = availableGrid.getDataAsRecordList().find("id", id);
    //            Record r = availableGrid.getDataAsRecordList().get(id);
    //            if (r != null) {
    //                ((ListGridRecord) r).setEnabled(true);
    //            }
    //        }
    //        availableGrid.markForRedraw();
    //    }
    protected void deselect(ListGridRecord[] records) {
        System.out.println("********** Current Group Selection size:" + getGroupSelection().size());
        System.out.println("******** RoleLdapGroupSelector.deselect:" + records.length);
        //        HashSet<Integer> toRemove = new HashSet<Integer>();
        HashSet<String> toRemove = new HashSet<String>();
        for (ListGridRecord record : records) {
            //            toRemove.add(record.getAttributeAsInt("id"));
            toRemove.add(record.getAttributeAsString(name));
        }
        selection.removeAll(toRemove);
        System.out.println("Selection size:" + selection.size());

        //        for (Integer id : toRemove) {
        for (String name : toRemove) {
            //            Record r = availableGrid.getDataAsRecordList().find("id", id);
            Record r = availableGrid.getDataAsRecordList().find(name, name);
            if (r != null) {
                ((ListGridRecord) r).setEnabled(true);
            }
        }
        int cnt = 0;
        for (Record lgr : availableGrid.getDataAsRecordList().toArray()) {
            if (lgr.getAttributeAsBoolean("enabled")) {
                cnt++;
            }
        }
        System.out.println("------ availableGrid.enabledCount.size:" + cnt);
        System.out.println("********** Current Group Selection size:" + getGroupSelection().size());
        availableGrid.markForRedraw();
    }

    //    protected void updateButtons() {
    //        super.updateButtons();
    //        System.out.println("+++++++updateButtons, called Super()");
    //        assignedGrid.deselectAllRecords();
    //        System.out.println("********** After Update Group Selection size:" + getGroupSelection().size());
    //        //        addButton.setDisabled(!availableGrid.anySelected() || availableGrid.getTotalRows() == 0);
    //        //        removeButton.setDisabled(!assignedGrid.anySelected() || assignedGrid.getTotalRows() == 0);
    //        //        addAllButton.setDisabled(availableGrid.getTotalRows() == 0);
    //        //        removeAllButton.setDisabled(assignedGrid.getTotalRows() == 0);
    //    }

    //    public class SelectedRolesDataSource extends RolesDataSource {
    //
    //        @Override
    //        public ListGridRecord[] buildRecords(Collection<Role> roles) {
    //            ListGridRecord[] records = super.buildRecords(roles);
    //            for (ListGridRecord record : records) {
    //                if (selection.contains(record.getAttributeAsInt("id"))) {
    //                    record.setEnabled(false);
    //                }
    //            }
    //            return records;
    //        }
    //    }

    public class LdapGroupsDataSource extends RPCDataSource<HashSet<Map<String, String>>> {

        public LdapGroupsDataSource() {
            //            DataSourceTextField idField = new DataSourceTextField(id, id);
            //            idField.setPrimaryKey(true);

            DataSourceTextField nameField = new DataSourceTextField(name, name);
            nameField.setPrimaryKey(true);

            DataSourceTextField descriptionField = new DataSourceTextField(description, description);

            //            DataSourceImageField availablilityField = new DataSourceImageField(available, "Current Availability");

            setFields(nameField, descriptionField);
        }

        //        public ListGridRecord[] buildAssignedRecords(Set<LdapGroup> currentlyAssignedLdapGroups) {
        //            ListGridRecord[] records = new ListGridRecord[0];
        //            int index = 0;
        //            if ((currentlyAssignedLdapGroups != null) && (!currentlyAssignedLdapGroups.isEmpty())) {
        //                //load groupsData
        //                records = new ListGridRecord[currentlyAssignedLdapGroups.size()];
        //                for (LdapGroup group : currentlyAssignedLdapGroups) {
        //                    ListGridRecord record = new ListGridRecord();
        //                    //load identifier 
        //                    //                    record.setAttribute(id, group.get(id));
        //                    record.setAttribute(id, group.getName());
        //                    //load name 
        //                    record.setAttribute(name, group.getName());
        //                    //load description 
        //                    record.setAttribute(description, group.getDescription());
        //
        //                    records[index++] = record;
        //                }
        //            }
        //            return records;
        //        }

        public ListGridRecord[] buildRecords(Set<Map<String, String>> locatedGroups) {
            ListGridRecord[] records = new ListGridRecord[0];
            int indx = 0;
            if ((locatedGroups != null) && (!locatedGroups.isEmpty())) {
                //load groupsData
                records = new ListGridRecord[locatedGroups.size()];
                int index = 0;
                //for each Map returned then iterate over to retrieve the values
                Iterator<Map<String, String>> iterator = locatedGroups.iterator();
                while (iterator.hasNext()) {
                    Map<String, String> group = iterator.next();
                    //iterate over the group data to translate into records
                    ListGridRecord record = new ListGridRecord();
                    //load identifier 
                    //                    record.setAttribute(id, group.get(id));
                    record.setAttribute(id, index++);
                    //load name 
                    record.setAttribute(name, group.get(name));
                    //load description 
                    record.setAttribute(description, group.get(description));

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
        public HashSet<Map<String, String>> copyValues(ListGridRecord from) {
            throw new UnsupportedOperationException("Ldap Group data is read only");
        }

        @Override
        public ListGridRecord copyValues(HashSet<Map<String, String>> from) {
            return null;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            GWTServiceLookup.getLdapService().findAvailableGroups(new AsyncCallback<Set<Map<String, String>>>() {

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError("Failed to load LdapGroups available for role.", throwable);
                }

                public void onSuccess(Set<Map<String, String>> locatedGroups) {

                    //translate groups into records for grid
                    response.setData(buildRecords(locatedGroups));
                    //entry count
                    if (null != locatedGroups) {
                        response.setTotalRows(locatedGroups.size());
                    } else {
                        response.setTotalRows(0);
                    }
                    //pass off for processing
                    processResponse(request.getRequestId(), response);
                }
            });

        }

        //        @Override
        //        public Set<String> getSelection() {
        //            //            System.out.println("------------ Returning selection:" + selection.size() + "selection:" + selection);
        //            return selection;
        //        }

    }

    public HashSet<String> getGroupSelection() {
        System.out.println("------- selection is:" + selection + ":cnt:" + selection.size());
        System.out.println("------- actual size is:" + assignedGrid.getSelection().length);
        RecordList records = assignedGrid.getDataAsRecordList();
        System.out.println("------- Rec list cnt:" + records.toArray().length);
        //empty out selection and populate with actual contents
        selection.clear();
        if (!records.isEmpty()) {
            for (Record r : records.toArray()) {
                selection.add(r.getAttributeAsString(name));
            }
        }
        System.out.println("------- selection is:" + selection + ":cnt:" + selection.size());
        System.out.println("-------AS selection is:" + assignedGrid.getSelection() + ":cnt:"
            + assignedGrid.getSelection().length);
        //        return selection;
        HashSet<String> assignedSelections = new HashSet<String>();
        for (ListGridRecord r : assignedGrid.getSelection()) {
            assignedSelections.add(r.getAttributeAsString(name));
        }
        HashSet<String> remainingRecords = new HashSet<String>();
        for (Record r : assignedGrid.getDataAsRecordList().toArray()) {
            remainingRecords.add(r.getAttributeAsString(name));
        }
        //        return assignedSelections;
        return remainingRecords;
    }

    public class LdapAssignedGroupsDatasource extends RPCDataSource<Set<String>> {
        private Integer currentRoleId = Integer.valueOf(-1);

        public LdapAssignedGroupsDatasource(Integer integer) {
            System.out.println("----------- LdapAssigned.Constructor:" + integer);
            if (integer != null) {
                this.currentRoleId = integer;
            }
            getAssignedGrid().invalidateCache();
            getAssignedGrid().markForRedraw();
        }

        @Override
        public Set<String> copyValues(ListGridRecord from) {
            //            Set<String> copied = new HashSet<String>();
            //            if (from != null) {
            //                copied.add(from.getAttributeAsString(name));
            //            }
            //            return copied;
            return null;
        }

        @Override
        public ListGridRecord copyValues(Set<String> from) {
            //            ListGridRecord lgr = new ListGridRecord();
            //            if (from != null) {
            //                lgr.setAttribute(name, from.iterator().next());
            //            }
            //            return lgr;
            return null;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            System.out.println("----------- in ldapAss.executeFetch:" + getCurrentRoleId());
            int currentRoleId = -1;
            if ((getCurrentRoleId() != null) && (getCurrentRoleId().intValue() > -1)) {
                currentRoleId = getCurrentRoleId().intValue();
            }

            GWTServiceLookup.getLdapService().findLdapGroupsAssignedToRole(currentRoleId,
            //                new AsyncCallback<Set<LdapGroup>>() {
                new AsyncCallback<Set<Map<String, String>>>() {

                    public void onFailure(Throwable throwable) {
                        CoreGUI.getErrorHandler().handleError("Failed to load LdapGroups available for role.",
                            throwable);
                    }

                    //                    public void onSuccess(Set<LdapGroup> currentlyAssignedLdapGroups) {
                    public void onSuccess(Set<Map<String, String>> currentlyAssignedLdapGroups) {
                        System.out.println("------ ldapAss.fetch.success:" + currentlyAssignedLdapGroups);
                        //translate groups into records for grid
                        //                    response.setData(buildRecords(locatedGroups));
                        //                        response.setData(buildAssignedRecords(currentlyAssignedLdapGroups));
                        //instead of setting the data, find which ones are shared and transfer as before
                        RecordList loaded = getAssignedGrid().getDataAsRecordList();
                        ArrayList<Integer> located = new ArrayList<Integer>();
                        //                        for (LdapGroup group : currentlyAssignedLdapGroups) {
                        for (Map groupMap : currentlyAssignedLdapGroups) {
                            //                            int index = loaded.findIndex(name, group.getName());
                            int index = loaded.findIndex(name, (String) groupMap.get(name));
                            if (index > -1) {
                                located.add(Integer.valueOf(index));
                            }
                        }
                        System.out.println("--------located:" + located.size());
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
            //now find out which groups are shared
            //mock up transfer via buttons
            //TODO: above
        }

        public ListGridRecord[] buildAssignedRecords(Set<LdapGroup> currentlyAssignedLdapGroups) {
            System.out.println("----- buildAssRecords:");
            ListGridRecord[] records = new ListGridRecord[0];
            int index = 0;
            if ((currentlyAssignedLdapGroups != null) && (!currentlyAssignedLdapGroups.isEmpty())) {
                //load groupsData
                records = new ListGridRecord[currentlyAssignedLdapGroups.size()];
                for (LdapGroup group : currentlyAssignedLdapGroups) {
                    ListGridRecord record = new ListGridRecord();
                    //load identifier 
                    //                    record.setAttribute(id, group.get(id));
                    record.setAttribute(id, group.getName());
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

    //    @Override
    //    public HashSet<Integer> getSelection() {
    //        // TODO Auto-generated method stub
    //        return super.getSelection();
    //    }

}

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
import java.util.HashMap;
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
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;

import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Simeon Pinder
 */
public class RoleLdapGroupSelector extends AbstractSelector<PageList<LdapGroup>> {

    public static final String id = "id";
    public static final String name = "name";
    public static final String description = "description";
    private LdapGroupsDataSource availableDatasource;
    protected Set<String> selection = new HashSet<String>();
    private int currentRoleId = -1;
    private boolean initialLdapSelectionsLoad = true;
    //cache ldap group data from external server
    private Set<Map<String, String>> cachedLdapGroupsAvailable;
    private Map<String, Map<String, String>> cachedNameKeyedMap;

    //override the selector key for ldap group selection.
    protected String getSelectorKey() {
        return "name";
    }

    public RoleLdapGroupSelector(String locatorId, Integer integer) {
        super(locatorId);
        if (integer != null) {
            this.currentRoleId = integer.intValue();
        }
    }

    /** Define search for case insensitive filtering on ldap name.
     */
    @Override
    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new LocatableDynamicForm(this.getLocatorId());
        availableFilterForm.setWidth100();
        availableFilterForm.setNumCols(2);

        final TextItem search = new TextItem("search", MSG.common_title_search());
        availableFilterForm.setItems(search, new SpacerItem());

        return availableFilterForm;
    }

    @Override
    protected RPCDataSource<PageList<LdapGroup>> getDataSource() {
        if (availableDatasource == null) {
            availableDatasource = new LdapGroupsDataSource();
            //add subsequent listener
            int currentRoleId = getCurrentRoleId();
            if (currentRoleId > -1) {

                //add listener to AvailableGrid, to act after successfully populated.
                this.availableGrid.addDataArrivedHandler(new DataArrivedHandler() {
                    @Override
                    public void onDataArrived(DataArrivedEvent event) {
                        int currentRoleId = getCurrentRoleId();
                        if (currentRoleId > -1) {
                            if (initialLdapSelectionsLoad) {
                                GWTServiceLookup.getLdapService().findLdapGroupsAssignedToRole(currentRoleId,
                                    new AsyncCallback<PageList<LdapGroup>>() {
                                        public void onFailure(Throwable throwable) {
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_adminRoles_failLdapGroupsRole(), throwable);
                                        }

                                        public void onSuccess(PageList<LdapGroup> currentlyAssignedLdapGroups) {
                                            //translate groups into records for grid
                                            //instead of setting the data, find which ones are shared and transfer as before. eliminate stale
                                            if ((currentlyAssignedLdapGroups != null)
                                                && (!currentlyAssignedLdapGroups.isEmpty())) {
                                                RecordList loaded = availableGrid.getDataAsRecordList();
                                                if (loaded != null) {
                                                    ArrayList<Integer> located = new ArrayList<Integer>();
                                                    for (LdapGroup group : currentlyAssignedLdapGroups) {
                                                        int index = loaded.findIndex(name, group.getName());
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
                                                    initialLdapSelectionsLoad = false;
                                                    addSelectedRows();
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

    /** Retrieve latest search string entered by the user.
     */
    @Override
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        String search = (String) availableFilterForm.getValue("search");
        Criteria criteria = new Criteria();
        if (null != search) {
            criteria.addCriteria("name", search);
        }
        return criteria;
    }

    @Override
    protected String getItemTitle() {
        return MSG.common_title_groups();
    }

    public class LdapGroupsDataSource extends RPCDataSource<PageList<LdapGroup>> {

        public LdapGroupsDataSource() {
            DataSourceTextField nameField = new DataSourceTextField(name, name);
            nameField.setPrimaryKey(true);

            DataSourceTextField descriptionField = new DataSourceTextField(description, description);

            setFields(nameField, descriptionField);
        }

        public ListGridRecord[] buildRecords(Set<LdapGroup> locatedGroups) {
            ListGridRecord[] records = new ListGridRecord[0];
            int indx = 0;
            if ((locatedGroups != null) && (!locatedGroups.isEmpty())) {
                //load groupsData
                records = new ListGridRecord[locatedGroups.size()];
                int index = 0;
                //for each Map returned then iterate over to retrieve the values
                for (LdapGroup group : locatedGroups) {
                    //iterate over the group data to translate into records
                    ListGridRecord record = new ListGridRecord();
                    //load identifier 
                    record.setAttribute(id, index++);
                    record.setAttribute(name, group.getName());
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
        public PageList<LdapGroup> copyValues(Record from) {
            throw new UnsupportedOperationException(MSG.view_adminRoles_ldapGroupsReadOnly());
        }

        @Override
        public ListGridRecord copyValues(PageList<LdapGroup> from) {
            return null;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            //if not null then go through to initialize
            if (cachedLdapGroupsAvailable == null) {
                //determine if ldap enabled, if so then chain and proceed with finding groups
                GWTServiceLookup.getLdapService().checkLdapConfiguredStatus(new AsyncCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean ldapConfigured) {
                        if (ldapConfigured) {
                            availableGrid.setEmptyMessage(MSG.view_adminRoles_noItems());
                            GWTServiceLookup.getLdapService().findAvailableGroups(
                                new AsyncCallback<Set<Map<String, String>>>() {

                                    public void onFailure(Throwable throwable) {
                                        CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdapGroupsRole(),
                                            throwable);
                                    }

                                    public void onSuccess(Set<Map<String, String>> locatedGroups) {
                                        Log.trace("Successfully located " + locatedGroups.size()
                                            + " LDAP available groups.");
                                        if (cachedLdapGroupsAvailable == null) {
                                            cachedLdapGroupsAvailable = locatedGroups;
                                        }
                                        //all groups displayed initially 
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
                                });//end of findAvailableGroups
                        } else {//ldap not configured
                            Log.debug("(LDAP not currently enabled. " + MSG.view_adminRoles_noItems());
                            response.setTotalRows(0);
                            String message = "("
                                + MSG.view_adminRoles_noLdap("href='#Administration/Configuration/SystemSettings'", MSG
                                    .view_adminConfig_systemSettings()) + ")";
                            availableGrid.setEmptyMessage(message);
                            processResponse(request.getRequestId(), response);
                        }
                    }//end onSuccess

                    @Override
                    public void onFailure(Throwable caught) {
                        Log.error("Unable to determine whether ldap configured - check server logs.");
                    }
                });//end of checkLdapConfigured status
            } else {//use cached data and return correct response
                //process cachedLdapGroupsAvailable based on criteria
                Criteria criteria = getLatestCriteria(availableFilterForm);
                String search = (String) criteria.getValues().get("name");
                //empty group
                Set<Map<String, String>> locatedGroups = new HashSet<Map<String, String>>();

                //populate the indexed map
                if (cachedNameKeyedMap == null) {
                    cachedNameKeyedMap = new HashMap<String, Map<String, String>>();
                    Iterator<Map<String, String>> iterator = cachedLdapGroupsAvailable.iterator();
                    while (iterator.hasNext()) {
                        Map<String, String> map = iterator.next();
                        String id = map.get("name");
                        cachedNameKeyedMap.put(id, map);
                    }
                }
                //if search non empty
                if ((search != null) && (!search.trim().isEmpty())) {
                    //now iterate over keys to find matches
                    Set<String> keySet = cachedNameKeyedMap.keySet();
                    for (String key : keySet) {
                        //do case insensitive match to entered string.
                        if (key.toLowerCase().contains(search.trim().toLowerCase())) {
                            locatedGroups.add(cachedNameKeyedMap.get(key));
                        }
                    }
                } else {//return full list .. as no filtering done.
                    locatedGroups = cachedLdapGroupsAvailable;
                }
                //then convert.
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
        public Set<String> copyValues(Record from) {
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
                new AsyncCallback<PageList<LdapGroup>>() {

                    public void onFailure(Throwable throwable) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdapGroupsRole(), throwable);
                    }

                    public void onSuccess(PageList<LdapGroup> currentlyAssignedLdapGroups) {
                        //instead of setting the data, find which ones are shared and transfer as before
                        RecordList loaded = getAssignedGrid().getDataAsRecordList();
                        ArrayList<Integer> located = new ArrayList<Integer>();
                        for (LdapGroup groupMap : currentlyAssignedLdapGroups) {
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
                        addSelectedRows();

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

    public int getCurrentRoleId() {
        return currentRoleId;
    }
    
}

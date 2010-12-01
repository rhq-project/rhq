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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Simeon Pinder
 */
public class RoleLdapGroupSelector extends AbstractSelector<LdapGroup> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";

    //override the selector key for ldap group selection.
    protected String getSelectorKey() {
        return "name";
    }

    public RoleLdapGroupSelector(String locatorId, ListGridRecord[] assignedRecords, boolean isReadOnly) {
        super(locatorId, isReadOnly);
        
        setAssigned(assignedRecords);
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
    protected RPCDataSource<LdapGroup> getDataSource() {
        return new LdapGroupsDataSource();
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
        return MSG.common_title_ldapGroups();
    }

    public static class LdapGroupsDataSource extends RPCDataSource<LdapGroup> {

        //cache ldap group data from external server
        private Set<Map<String, String>> cachedLdapGroupsAvailable;
        private Map<String, Map<String, String>> cachedNameKeyedMap;

        public LdapGroupsDataSource() {
            DataSourceTextField nameField = new DataSourceTextField(FIELD_NAME, FIELD_NAME);
            nameField.setPrimaryKey(true);

            DataSourceTextField descriptionField = new DataSourceTextField(FIELD_DESCRIPTION, FIELD_DESCRIPTION);

            setFields(nameField, descriptionField);
        }

        @Override
        public LdapGroup copyValues(Record from) {
            LdapGroup to = new LdapGroup();

            to.setId(from.getAttributeAsInt(FIELD_ID));
            to.setName(from.getAttributeAsString(FIELD_NAME));
            to.setDescription(from.getAttributeAsString(FIELD_DESCRIPTION));

            return to;
        }

        @Override
        public ListGridRecord copyValues(LdapGroup from) {
            ListGridRecord to = new ListGridRecord();

            to.setAttribute(FIELD_ID, from.getId());
            to.setAttribute(FIELD_NAME, from.getName());
            to.setAttribute(FIELD_DESCRIPTION, from.getDescription());

            return to;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            //if not null then go through to initialize
            if (cachedLdapGroupsAvailable == null) {
                fetchLdapGroupsFromServerAsync(request, response);
            } else {//use cached data and return correct response
                //process cachedLdapGroupsAvailable based on criteria
                PageList<LdapGroup> ldapGroups = filterCachedLdapGroups(request);
                sendSuccessResponse(request, response, ldapGroups);                
            }
        }

        private PageList<LdapGroup> filterCachedLdapGroups(DSRequest request) {
            //populate the indexed map
            if (cachedNameKeyedMap == null) {
                cachedNameKeyedMap = new HashMap<String, Map<String, String>>();
                for (Map<String, String> map : cachedLdapGroupsAvailable) {
                    String id = map.get("name");
                    cachedNameKeyedMap.put(id, map);
                }
            }

            Set<Map<String, String>> locatedGroupMaps;

            Criteria criteria = request.getCriteria();
            String nameFilter = (String) criteria.getValues().get("name");
            if ((nameFilter != null) && (!nameFilter.trim().isEmpty())) {
                // filter the cached list by the user-specified name filter
                locatedGroupMaps = new HashSet<Map<String, String>>();
                //now iterate over keys to find matches
                Set<String> keySet = cachedNameKeyedMap.keySet();
                for (String key : keySet) {
                    //do case insensitive match to entered string.
                    if (key.toLowerCase().contains(nameFilter.trim().toLowerCase())) {
                        locatedGroupMaps.add(cachedNameKeyedMap.get(key));
                    }
                }
            } else {//return full list .. as no filtering done.
                locatedGroupMaps = cachedLdapGroupsAvailable;
            }
            @SuppressWarnings({"UnnecessaryLocalVariable"})
            PageList<LdapGroup> ldapGroups = convertToPageList(locatedGroupMaps);
            return ldapGroups;
        }

        private void fetchLdapGroupsFromServerAsync(final DSRequest request, final DSResponse response) {
            GWTServiceLookup.getLdapService().findAvailableGroups(
                new AsyncCallback<Set<Map<String, String>>>() {
                    public void onSuccess(Set<Map<String, String>> locatedGroupMaps) {
                        Log.debug("Successfully located " + locatedGroupMaps.size() + " available LDAP groups.");
                        cachedLdapGroupsAvailable = locatedGroupMaps;
                        //all groups displayed initially
                        PageList<LdapGroup> ldapGroups = convertToPageList(locatedGroupMaps);
                        sendSuccessResponse(request, response, ldapGroups);
                    }

                    public void onFailure(Throwable throwable) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdapGroupsRole(),
                            throwable);
                    }
                });//end of findAvailableGroups
        }
    }

    public static PageList<LdapGroup> convertToPageList(Set<Map<String, String>> locatedGroupMaps) {
        PageList<LdapGroup> converted = new PageList<LdapGroup>();
        converted.setPageControl(PageControl.getUnlimitedInstance());
        if (locatedGroupMaps != null) {
            for (Map<String, String> locatedGroupMap : locatedGroupMaps) {
                LdapGroup group = new LdapGroup();
                group.setDescription(locatedGroupMap.get("description"));
                group.setName(locatedGroupMap.get("name"));
                //group.setId(0);
                converted.add(group);
            }
        }
        return converted;
    }

}

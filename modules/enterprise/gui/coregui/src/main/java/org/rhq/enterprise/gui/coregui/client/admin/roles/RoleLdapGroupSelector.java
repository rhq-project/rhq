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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Simeon Pinder
 */
public class RoleLdapGroupSelector extends AbstractSelector<LdapGroup, org.rhq.core.domain.criteria.Criteria> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    private static boolean queryCompleted = false;

    //override the selector key for ldap group selection.
    protected String getSelectorKey() {
        return "name";
    }

    public RoleLdapGroupSelector(ListGridRecord[] assignedRecords, boolean isReadOnly) {
        super(isReadOnly);

        setAssigned(assignedRecords);
    }

    /** Define search for case insensitive filtering on ldap name.
     */
    @Override
    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new DynamicForm();
        {
            availableFilterForm.setWidth100();
            availableFilterForm.setNumCols(2);
        }
        int groupPanelWidth = 375;
        int groupPanelHeight = 140;

        // final TextItem search = new TextItem("search",
        // MSG.common_title_search());

        // Structure the display area into two separate display regions
        // Available Groups region
        final DynamicForm availableGroupDetails = new DynamicForm();
        {
            availableGroupDetails.setWidth(groupPanelWidth);
            availableGroupDetails.setHeight(groupPanelHeight);
            availableGroupDetails.setGroupTitle("Available Groups Results");
            availableGroupDetails.setIsGroup(true);
            availableGroupDetails.setWrapItemTitles(false);
        }
        final TextItem resultCountItem = new TextItem("resultCount", "Groups Found");
        {
            resultCountItem.setCanEdit(false);
            resultCountItem.setWidth("100%");
        }
        final TextItem pageCountItem = new TextItem("pageCount", "Query Pages Parsed");
        {
            pageCountItem.setCanEdit(false);
            pageCountItem.setWidth("100%");
        }
        // final TextItem search = new TextItem("search",
        // MSG.common_title_search());
        final TextItem search = new TextItem("search", "Search[within results]");
        {
            search.setWidth("100%");
            search.setTooltip("Start typing here to show groups containing the typed characters.");
        }
        final FormItemIcon loadingIcon = new FormItemIcon();
        final FormItemIcon successIcon = new FormItemIcon();
        final FormItemIcon failIcon = new FormItemIcon();
        String successIconPath = "[SKIN]/actions/ok.png";
        String failedIconPath = "[SKIN]/actions/exclamation.png";
        String loadingIconPath = "[SKIN]/loading.gif";
        //icon.setSrc("[SKIN]/actions/help.png");
        loadingIcon.setSrc(loadingIconPath);
        successIcon.setSrc(successIconPath);
        failIcon.setSrc(failedIconPath);

        final StaticTextItem groupQueryStatus = new StaticTextItem();
        {
            groupQueryStatus.setName("groupQueryStatus");
            groupQueryStatus.setTitle("Query Progress");
            groupQueryStatus.setDefaultValue("Loading...");
            groupQueryStatus.setIcons(loadingIcon);
        }
        availableGroupDetails.setItems(resultCountItem, pageCountItem, groupQueryStatus, new SpacerItem(), search);

        // Ldap Group Settings region
        final DynamicForm ldapGroupSettings = new DynamicForm();
        {
            ldapGroupSettings.setWidth(groupPanelWidth);
            ldapGroupSettings.setHeight(groupPanelHeight);
            ldapGroupSettings.setGroupTitle("[Read Only] Ldap Group Settings. Edit in 'System Settings'");
            ldapGroupSettings.setIsGroup(true);
            ldapGroupSettings.setWrapItemTitles(false);
        }
        final TextItem groupSearch = new TextItem("groupSearch", "Search Filter");
        {
            groupSearch.setCanEdit(false);
            groupSearch.setWidth("100%");
        }
        final TextItem groupMember = new TextItem("groupMember", "Member Filter");
        {
            groupMember.setCanEdit(false);
            groupMember.setWidth("100%");
        }
        final CheckboxItem groupQueryPagingItem = new CheckboxItem("groupQueryEnable", "Query Paging Enabled");
        {
            groupQueryPagingItem.setCanEdit(false);
            groupQueryPagingItem.setValue(false);
            groupQueryPagingItem.setShowLabel(false);
            groupQueryPagingItem.setShowTitle(true);
            groupQueryPagingItem.setTitleOrientation(TitleOrientation.LEFT);
            //You have to set this attribute
            groupQueryPagingItem.setAttribute("labelAsTitle", true);
        }
        final TextItem groupQueryPagingCountItem = new TextItem("groupQueryCount", "Query Page Size");
        {
            groupQueryPagingCountItem.setCanEdit(false);
            groupQueryPagingCountItem.setWidth("100%");
        }
        final CheckboxItem groupUsePosixGroupsItem = new CheckboxItem("groupUsePosixGroups", "Use Posix Enabled");
        {
            groupUsePosixGroupsItem.setCanEdit(false);
            groupUsePosixGroupsItem.setValue(false);
            groupUsePosixGroupsItem.setShowLabel(false);
            groupUsePosixGroupsItem.setShowTitle(true);
            groupUsePosixGroupsItem.setTitleOrientation(TitleOrientation.LEFT);
            //You have to set this attribute
            groupUsePosixGroupsItem.setAttribute("labelAsTitle", true);
        }
        ldapGroupSettings
            .setItems(groupSearch, groupMember, groupQueryPagingItem, groupQueryPagingCountItem, groupUsePosixGroupsItem);

        // orient both panels next to each other
        HLayout panel = new HLayout();
        {
            panel.addMember(availableGroupDetails);
            DynamicForm spacerWrapper = new DynamicForm();
            spacerWrapper.setItems(new SpacerItem());
            panel.addMember(spacerWrapper);
            panel.addMember(ldapGroupSettings);
        }
        availableFilterForm.addChild(panel);

        //launch operations to populate/refresh LDAP Group Query contents.
        final Timer ldapPropertiesTimer = new Timer() {
            public void run() {
                //if system properties not set, launch request/update
                String ldapGroupQuery = groupSearch.getValueAsString();
                if ((ldapGroupQuery == null) || (ldapGroupQuery.trim().isEmpty())) {
                    GWTServiceLookup.getSystemService().getSystemSettings(new AsyncCallback<SystemSettings>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            groupQueryStatus.setIcons(failIcon);
                            groupQueryStatus.setDefaultValue("Fail: Unable to retrieve system settings.");
                            //TODO: update this message
                            CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdap(), caught);
                        }

                        @Override
                        public void onSuccess(SystemSettings settings) {
                            //retrieve relevant information once and update ui
                            String ldapGroupFilter = settings.get(SystemSetting.LDAP_GROUP_FILTER);
                            String ldapGroupMember = settings.get(SystemSetting.LDAP_GROUP_MEMBER);
                            String ldapGroupPagingEnabled = settings.get(SystemSetting.LDAP_GROUP_PAGING);
                            String ldapGroupPagingValue = settings.get(SystemSetting.LDAP_GROUP_QUERY_PAGE_SIZE);
                            String ldapGroupIsPosix = settings.get(SystemSetting.LDAP_GROUP_USE_POSIX);
                            groupSearch.setValue(ldapGroupFilter);
                            groupMember.setValue(ldapGroupMember);
                            groupQueryPagingItem.setValue(Boolean.valueOf(ldapGroupPagingEnabled));
                            groupQueryPagingCountItem.setValue(ldapGroupPagingValue);
                            groupUsePosixGroupsItem.setValue(Boolean.valueOf(ldapGroupIsPosix));
                            ldapGroupSettings.markForRedraw();
                        }
                    });
                }
            }
        };
        ldapPropertiesTimer.scheduleRepeating(2000); // repeat interval in milliseconds, e.g. 30000 = 30seconds

        //launch operations to populate/refresh LDAP Group Query contents.
        final Timer availableGroupsTimer = new Timer() {
            public void run() {
                if (!queryCompleted) {
                    //make request to RHQ about state of latest LDAP GWT request
                    GWTServiceLookup.getLdapService().findAvailableGroupsStatus(
                        new AsyncCallback<Set<Map<String, String>>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                groupQueryStatus.setIcons(failIcon);
                                groupQueryStatus
                                    .setDefaultValue("Fail: Unable to retrieve status for latest AvailableGroups() call.");
                                //TODO: update this message
                                CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdap(), caught);
                            }

                            @Override
                            public void onSuccess(Set<Map<String, String>> results) {
                                //                                Log.debug("@@@@@@@ findAvailableGroupsStatus: SUCCESS:" + System.currentTimeMillis()
                                //                                    + ":count:"
                                //                                    + results.size());
                                long start = -1, end = -1;
                                int pageCount = 0;
                                int resultCountValue = 0;
                                for (Map<String, String> map : results) {
                                    String key = map.keySet().toArray()[0] + "";
                                    if (key.equals("query.results.parsed")) {
                                        String value = map.get(key);
                                        resultCountItem.setValue(value);
                                        resultCountValue = Integer.valueOf(value);
                                    } else if (key.equals("query.complete")) {
                                        String value = map.get(key);
                                        queryCompleted = Boolean.valueOf(value);
                                    } else if (key.equals("query.start.time")) {
                                        String value = map.get(key);
                                        start = Long.valueOf(value);
                                    } else if (key.equals("query.end.time")) {
                                        String value = map.get(key);
                                        end = Long.valueOf(value);
                                    } else if (key.equals("query.page.count")) {
                                        String value = map.get(key);
                                        pageCountItem.setValue(value);
                                        pageCount = Integer.valueOf(value);
                                    }
                                }
                                //act on status details to add extra perf suggestions
                                if (queryCompleted) {
                                    groupQueryStatus.setIcons(successIcon);
                                    String success = "Success";
                                    String tooManyResults = success + ": Too many results.";
                                    String queryTookLongResults = success + ": Query took long to complete.";
                                    String queryTookManyPagesResults = success + ": Query required a lot of paging.";
                                    //TODO: add in extra information about results.
                                    if (resultCountValue > 20000) {//results throttled
                                        groupQueryStatus.setDefaultValue(tooManyResults);
                                    } else if ((end - start) >= 10 * 1000) {// took longer than 10s
                                        groupQueryStatus.setDefaultValue(queryTookLongResults);
                                    } else if (pageCount >= 20) {// took longer than 10s
                                        groupQueryStatus.setDefaultValue(queryTookManyPagesResults);
                                    }
                                }
                                availableGroupDetails.markForRedraw();
                                //now cancel the timer
                                cancel();
                            }
                        });
                }
            }
        };
        availableGroupsTimer.scheduleRepeating(3000); // repeat interval in milliseconds, e.g. 30000 = 30seconds

        return availableFilterForm;
    }

    @Override
    protected RPCDataSource<LdapGroup, org.rhq.core.domain.criteria.Criteria> getDataSource() {
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

    public static class LdapGroupsDataSource extends RPCDataSource<LdapGroup, org.rhq.core.domain.criteria.Criteria> {

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
        protected void executeFetch(final DSRequest request, final DSResponse response,
            final org.rhq.core.domain.criteria.Criteria unused) {
            //if not null then go through to initialize
            if (cachedLdapGroupsAvailable == null) {
                fetchLdapGroupsFromServerAsync(request, response);
            } else {//use cached data and return correct response
                //process cachedLdapGroupsAvailable based on criteria
                PageList<LdapGroup> ldapGroups = filterCachedLdapGroups(request);
                sendSuccessResponse(request, response, ldapGroups);
            }
        }

        @Override
        protected org.rhq.core.domain.criteria.Criteria getFetchCriteria(DSRequest request) {
            // we don't use criterias for this datasource, just return null
            return null;
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

            PageList<LdapGroup> ldapGroups = convertToPageList(locatedGroupMaps);
            return ldapGroups;
        }

        private void fetchLdapGroupsFromServerAsync(final DSRequest request, final DSResponse response) {
            GWTServiceLookup.getLdapService().findAvailableGroups(new AsyncCallback<Set<Map<String, String>>>() {
                public void onSuccess(Set<Map<String, String>> locatedGroupMaps) {
                    Log.debug("Successfully located " + locatedGroupMaps.size() + " available LDAP groups.");
                    cachedLdapGroupsAvailable = locatedGroupMaps;
                    //all groups displayed initially
                    PageList<LdapGroup> ldapGroups = convertToPageList(locatedGroupMaps);
                    sendSuccessResponse(request, response, ldapGroups);
                }

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdapGroupsRole(), throwable);
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

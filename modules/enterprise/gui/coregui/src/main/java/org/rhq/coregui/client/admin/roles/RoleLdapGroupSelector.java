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
package org.rhq.coregui.client.admin.roles;

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
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.selector.AbstractSelector;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Simeon Pinder
 */
public class RoleLdapGroupSelector extends AbstractSelector<LdapGroup, org.rhq.core.domain.criteria.Criteria> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    final TextItem searchTextItem = new TextItem();
    protected int cursorPosition;
    private static int retryAttempt = 0;//limit retries on failure
    private static int noProgressAttempts = 0;//limit really slow attempt parse times

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
        int groupPanelHeight = 150;

        // Structure the display area into two separate display regions
        // Available Groups region
        final DynamicForm availableGroupDetails = new DynamicForm();
        {
            availableGroupDetails.setWidth(groupPanelWidth);
            availableGroupDetails.setHeight(groupPanelHeight);
            availableGroupDetails.setGroupTitle(MSG.common_title_ldapGroupsAvailable());
            availableGroupDetails.setIsGroup(true);
            availableGroupDetails.setWrapItemTitles(false);
            //add itemChanged handler to listen for changes to SearchItem
            availableGroupDetails.addItemChangedHandler(new ItemChangedHandler() {
                public void onItemChanged(ItemChangedEvent itemChangedEvent) {

                    latestCriteria = getLatestCriteria(null);

                    Timer timer = new Timer() {
                        @Override
                        public void run() {
                            if (latestCriteria != null) {
                                Criteria criteria = latestCriteria;
                                latestCriteria = null;
                                populateAvailableGrid(criteria);
                            }
                        }
                    };
                    timer.schedule(500);
                }
            });
        }
        final TextItem resultCountItem = new TextItem("resultCount", MSG.common_title_groupsFound());
        {
            resultCountItem.setCanEdit(false);
            resultCountItem.setWidth("100%");
        }
        final TextItem pageCountItem = new TextItem("pageCount", MSG.common_title_queryPagesParsed());
        {
            pageCountItem.setCanEdit(false);
            pageCountItem.setWidth("100%");
        }
        final TextAreaItem adviceItem = new TextAreaItem("advice", MSG.common_title_suggest());
        {
            adviceItem.setWidth("100%");
            adviceItem.setHeight(20);
            String feedback = MSG.common_val_none();
            adviceItem.setValue(feedback);
            adviceItem.setTooltip(feedback);
            adviceItem.setDisabled(true);
            adviceItem.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    event.cancel();
                    cursorPosition = adviceItem.getSelectionRange()[0];
                }
            });
            adviceItem.addChangedHandler(new ChangedHandler() {

                @Override
                public void onChanged(ChangedEvent event) {
                    adviceItem.setSelectionRange(cursorPosition, cursorPosition);
                }
            });
        }
        //Customize Search component
        {
            searchTextItem.setName(MSG.common_title_search());
            searchTextItem.setTitle(MSG.view_admin_roles_filterResultsBelow());
            searchTextItem.setWidth("100%");
            searchTextItem.setTooltip(MSG.common_msg_typeToFilterResults());
        }
        final FormItemIcon loadingIcon = new FormItemIcon();
        final FormItemIcon successIcon = new FormItemIcon();
        final FormItemIcon failIcon = new FormItemIcon();
        final FormItemIcon attentionIcon = new FormItemIcon();
        String successIconPath = "[SKIN]/actions/ok.png";
        String failedIconPath = "[SKIN]/actions/exclamation.png";
        String loadingIconPath = "[SKIN]/loadingSmall.gif";
        String attentionIconPath = "[SKIN]/Dialog/warn.png";
        loadingIcon.setSrc(loadingIconPath);
        successIcon.setSrc(successIconPath);
        failIcon.setSrc(failedIconPath);
        attentionIcon.setSrc(attentionIconPath);

        final StaticTextItem groupQueryStatus = new StaticTextItem();
        {
            groupQueryStatus.setName("groupQueryStatus");
            groupQueryStatus.setTitle(MSG.common_title_queryProgress());
            groupQueryStatus.setDefaultValue(MSG.common_msg_loading());
            groupQueryStatus.setIcons(loadingIcon);
        }
        availableGroupDetails.setItems(resultCountItem, pageCountItem, groupQueryStatus, adviceItem, searchTextItem);

        // Ldap Group Settings region
        final DynamicForm ldapGroupSettings = new DynamicForm();
        {
            ldapGroupSettings.setWidth(groupPanelWidth);
            ldapGroupSettings.setHeight(groupPanelHeight);
            ldapGroupSettings.setGroupTitle(MSG.view_adminRoles_ldapGroupsSettingsReadOnly());
            ldapGroupSettings.setIsGroup(true);
            ldapGroupSettings.setWrapItemTitles(false);
        }
        final TextItem groupSearch = new TextItem("groupSearch", MSG.view_admin_systemSettings_LDAPFilter_name());
        {
            groupSearch.setCanEdit(false);
            groupSearch.setWidth("100%");
        }
        final TextItem groupMember = new TextItem("groupMember", MSG.view_admin_systemSettings_LDAPGroupMember_name());
        {
            groupMember.setCanEdit(false);
            groupMember.setWidth("100%");
        }
        final CheckboxItem groupQueryPagingItem = new CheckboxItem("groupQueryEnable",
            MSG.view_admin_systemSettings_LDAPGroupUsePaging_name());
        {
            groupQueryPagingItem.setCanEdit(false);
            groupQueryPagingItem.setValue(false);
            groupQueryPagingItem.setShowLabel(false);
            groupQueryPagingItem.setShowTitle(true);
            groupQueryPagingItem.setTitleOrientation(TitleOrientation.LEFT);
            //You have to set this attribute
            groupQueryPagingItem.setAttribute("labelAsTitle", true);
        }
        final TextItem groupQueryPagingCountItem = new TextItem("groupQueryCount",
            MSG.view_adminRoles_ldapQueryPageSize());
        {
            groupQueryPagingCountItem.setCanEdit(false);
            groupQueryPagingCountItem.setWidth("100%");
        }
        final CheckboxItem groupUsePosixGroupsItem = new CheckboxItem("groupUsePosixGroups",
            MSG.view_admin_systemSettings_LDAPGroupUsePosixGroup_name());
        {
            groupUsePosixGroupsItem.setCanEdit(false);
            groupUsePosixGroupsItem.setValue(false);
            groupUsePosixGroupsItem.setShowLabel(false);
            groupUsePosixGroupsItem.setShowTitle(true);
            groupUsePosixGroupsItem.setTitleOrientation(TitleOrientation.LEFT);
            //You have to set this attribute
            groupUsePosixGroupsItem.setAttribute("labelAsTitle", true);
        }
        ldapGroupSettings.setItems(groupSearch, groupMember, groupQueryPagingItem, groupQueryPagingCountItem,
            groupUsePosixGroupsItem);

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

        final long ldapGroupSelectorRequestId = System.currentTimeMillis();

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
                            groupQueryStatus.setDefaultValue(MSG.view_adminRoles_failLdapGroupsSettings());
                            CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdapGroupsSettings(), caught);
                            Log.debug(MSG.view_adminRoles_failLdapGroupsSettings());
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
        ldapPropertiesTimer.schedule(2000); // repeat interval in milliseconds, e.g. 30000 = 30seconds

        //launch operations to populate/refresh LDAP Group Query contents.
        final Timer availableGroupsTimer = new Timer() {
            public void run() {
                final String attention = MSG.common_status_attention();
                final String success = MSG.common_status_success();
                final String none = MSG.common_val_none();
                final String failed = MSG.common_status_failed();
                //make request to RHQ about state of latest LDAP GWT request
                GWTServiceLookup.getLdapService().findAvailableGroupsStatus(
                    new AsyncCallback<Set<Map<String, String>>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            groupQueryStatus.setIcons(failIcon);
                            groupQueryStatus.setDefaultValue(failed);
                            String adviceValue = MSG.view_adminRoles_failLdapAvailableGroups();
                            adviceItem.setValue(adviceValue);
                            adviceItem.setTooltip(adviceValue);
                            CoreGUI.getErrorHandler()
                                .handleError(MSG.view_adminRoles_failLdapAvailableGroups(), caught);
                            retryAttempt++;
                            if (retryAttempt > 3) {
                                cancel();//kill thread
                                Log.debug(MSG.view_adminRoles_failLdapRetry());
                                retryAttempt = 0;
                            }
                        }

                        @Override
                        public void onSuccess(Set<Map<String, String>> results) {
                            long start = -1, current = -1;
                            int pageCount = 0;
                            int resultCountValue = 0;
                            boolean queryCompleted = false;
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
                                } else if (key.equals("query.current.time")) {
                                    String value = map.get(key);
                                    current = Long.valueOf(value);
                                } else if (key.equals("query.page.count")) {
                                    String value = map.get(key);
                                    pageCountItem.setValue(value);
                                    pageCount = Integer.valueOf(value);
                                }
                            }

                            if (resultCountValue == 0) {
                                noProgressAttempts++;
                            }
                            //Update status information
                            String warnTooManyResults = MSG.view_adminRoles_ldapWarnTooManyResults();
                            String warnQueryTakingLongResults = MSG.view_adminRoles_ldapWarnQueryTakingLongResults();
                            String warnParsingManyPagesResults = MSG.view_adminRoles_ldapWarnParsingManyPagesResults();

                            boolean resultCountWarning = false;
                            boolean pageCountWarning = false;
                            boolean timePassingWarning = false;
                            if ((resultCountWarning = (resultCountValue > 5000))
                                || (pageCountWarning = (pageCount > 5))
                                || (timePassingWarning = (current - start) > 5 * 1000)) {
                                adviceItem.setDisabled(false);
                                groupQueryStatus.setIcons(attentionIcon);
                                if (resultCountWarning) {
                                    adviceItem.setValue(warnTooManyResults);
                                    adviceItem.setTooltip(warnTooManyResults);
                                } else if (pageCountWarning) {
                                    adviceItem.setValue(warnParsingManyPagesResults);
                                    adviceItem.setTooltip(warnParsingManyPagesResults);
                                } else if (timePassingWarning) {
                                    adviceItem.setValue(warnQueryTakingLongResults);
                                    adviceItem.setTooltip(warnQueryTakingLongResults);
                                }
                            }

                            //act on status details to add extra perf suggestions. Kill threads older than 30 mins
                            long parseTime = System.currentTimeMillis() - ldapGroupSelectorRequestId;
                            if ((queryCompleted) || (parseTime) > 30 * 60 * 1000) {
                                String tooManyResults = MSG.view_adminRoles_ldapTooManyResults();
                                String queryTookLongResults = MSG.view_adminRoles_ldapTookLongResults(parseTime + "");
                                String queryTookManyPagesResults = MSG
                                    .view_adminRoles_ldapTookManyPagesResults(pageCount + "");

                                adviceItem.setDisabled(false);
                                groupQueryStatus.setIcons(attentionIcon);
                                groupQueryStatus.setDefaultValue(attention);
                                if (resultCountValue > 20000) {//results throttled
                                    adviceItem.setValue(tooManyResults);
                                    adviceItem.setTooltip(tooManyResults);
                                    Log.debug(tooManyResults);//log error to client.
                                } else if ((current - start) >= 10 * 1000) {// took longer than 10s
                                    adviceItem.setValue(queryTookLongResults);
                                    adviceItem.setTooltip(queryTookLongResults);
                                    Log.debug(queryTookLongResults);//log error to client.
                                } else if (pageCount >= 20) {// required more than 20 pages of results
                                    adviceItem.setValue(queryTookManyPagesResults);
                                    adviceItem.setTooltip(queryTookManyPagesResults);
                                    Log.debug(queryTookManyPagesResults);//log error to client.
                                } else {//simple success.
                                    groupQueryStatus.setDefaultValue(success);
                                    groupQueryStatus.setIcons(successIcon);
                                    adviceItem.setValue(none);
                                    adviceItem.setTooltip(none);
                                    adviceItem.setDisabled(true);
                                }
                                noProgressAttempts = 0;
                                //now cancel the timer
                                cancel();
                            } else if (noProgressAttempts >= 10) {//availGroups query stuck on server side
                                //cancel the timer.
                                cancel();
                                String clientSideQuitting = MSG.view_adminRoles_failLdapCancelling();//catch all
                                adviceItem.setDisabled(false);
                                groupQueryStatus.setIcons(attentionIcon);
                                adviceItem.setValue(clientSideQuitting);
                                adviceItem.setTooltip(clientSideQuitting);
                                noProgressAttempts = 0;
                                Log.debug(clientSideQuitting);//log error to client.
                            }
                            availableGroupDetails.markForRedraw();
                        }
                    });
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
        //String search = (String) availableFilterForm.getValue("search");
        //non-trivial recursive form items possible. Retrieve from correct form item.
        String search = searchTextItem.getValueAsString();
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

        //cache ldap group data from external server for 30 mins then stale.
        private Set<Map<String, String>> cachedLdapGroupsAvailable;
        private Map<String, Map<String, String>> cachedNameKeyedMap;
        private long cachedLdapGroupsLast = -1;

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
            //if not null or stale then go through to initialize|reset
            if ((cachedLdapGroupsAvailable == null)
                || ((System.currentTimeMillis() - cachedLdapGroupsLast) > 30 * 60 * 1000)) {
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
                    cachedLdapGroupsLast = System.currentTimeMillis();
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

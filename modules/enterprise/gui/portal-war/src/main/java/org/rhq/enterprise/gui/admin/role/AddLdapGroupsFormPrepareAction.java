/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.admin.role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.ejb.EJBException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.exception.LdapCommunicationException;
import org.rhq.enterprise.server.exception.LdapFilterException;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data to facilitate display of the form for adding groups to a role.
 */
public class AddLdapGroupsFormPrepareAction extends TilesAction {
    final String LDAP_GROUP_CACHE = "ldapGroupCache";

    LdapGroupManagerLocal ldapManager = LookupUtil.getLdapGroupManager();

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddLdapGroupsFormPrepareAction.class.getName());

        AddLdapGroupsForm addForm = (AddLdapGroupsForm) form;
        Integer roleId = addForm.getR();

        if (roleId == null) {
            roleId = RequestUtils.getRoleId(request);
        }

        Role role = (Role) request.getAttribute(Constants.ROLE_ATTR);
        if (role == null) {
            RequestUtils.setError(request, Constants.ERR_ROLE_NOT_FOUND);
            return null;
        }
        //use cached LDAP group list to avoid hitting ldap server each time ui pref changed.
        Set<Map<String, String>> cachedAvailableLdapGroups = null;
        cachedAvailableLdapGroups = (Set<Map<String, String>>) request.getSession().getAttribute(LDAP_GROUP_CACHE);

        addForm.setR(role.getId());

        PageControl pca = WebUtility.getPageControl(request, "a");
        PageControl pcp = WebUtility.getPageControl(request, "p");

        //BZ-580127 Refactor so that all lists are initialized regardless of ldap server
        // availability or state of filter params
        List<String> pendingGroupIds = new ArrayList<String>();
        Set<Map<String, String>> allGroups = new HashSet<Map<String, String>>();
        PageList<LdapGroup> assignedList = new PageList<LdapGroup>();
        Set<Map<String, String>> availableGroupsSet = new HashSet<Map<String, String>>();
        Set<Map<String, String>> pendingSet = new HashSet<Map<String, String>>();

        PageList<Map<String, String>> pendingGroups = new PageList<Map<String, String>>(pendingSet, 0, pcp);
        PageList<Map<String, String>> availableGroups = new PageList<Map<String, String>>(availableGroupsSet, 0, pca);
        /* pending groups are those on the right side of the "add
         * to list" widget- awaiting association with the role when the form's "ok" button is clicked. */
        pendingGroupIds = SessionUtils.getListAsListStr(request.getSession(), Constants.PENDING_RESGRPS_SES_ATTR);

        log.trace("getting pending groups for role [" + roleId + ")");
        String name = "foo";

        try { //defend against ldap communication runtime difficulties.

            if (cachedAvailableLdapGroups == null) {
                //                allGroups = LdapGroupManagerBean.getInstance().findAvailableGroups();
                allGroups = ldapManager.findAvailableGroups();
            } else {//reuse cached.
                allGroups = cachedAvailableLdapGroups;
            }
            //store unmodified list in session.
            cachedAvailableLdapGroups = allGroups;

            //retrieve currently assigned groups
            assignedList = ldapManager.findLdapGroupsByRole(role.getId(), PageControl.getUnlimitedInstance());

            //trim already defined from all groups returned.
            allGroups = filterExisting(assignedList, allGroups);
            Set<String> pendingIds = new HashSet<String>(pendingGroupIds);

            //retrieve pending information
            pendingSet = findPendingGroups(pendingIds, allGroups);
            pendingGroups = new PageList<Map<String, String>>(pendingSet, pendingSet.size(), pcp);

            /* available groups are all groups in the system that are not
             * associated with the role and are not pending
             */
            log.trace("getting available groups for role [" + roleId + "]");

            availableGroupsSet = findAvailableGroups(pendingIds, allGroups);
            availableGroups = new PageList<Map<String, String>>(availableGroupsSet, availableGroupsSet.size(), pca);

            //We cannot reuse the PageControl mechanism as there are no database calls to retrieve list
            // must replicate paging using existing web params, formula etc.
            PageList<Map<String, String>> sizedAvailableGroups = new PageList<Map<String, String>>();
            sizedAvailableGroups = paginateLdapGroupData(sizedAvailableGroups, availableGroups, pca);

            //make sizedAvailableGroup the new reference to return.
            availableGroups = sizedAvailableGroups;
            //populate pagination elements for loaded elements.
            availableGroups.setTotalSize(availableGroupsSet.size());
            availableGroups.setPageControl(pca);
            //now do the same thing for Pending Groups.
            PageList<Map<String, String>> pagedPendingGroups = new PageList<Map<String, String>>();
            pagedPendingGroups = paginateLdapGroupData(pagedPendingGroups, pendingGroups, pcp);
            pendingGroups = pagedPendingGroups;
            //populate pagination elements for loaded elements.
            pendingGroups.setTotalSize(pendingSet.size());
            pendingGroups.setPageControl(pcp);

        } catch (EJBException ejx) {
            //this is the exception type thrown now that we use SLSB.Local methods
            // mine out other exceptions
            Exception cause = ejx.getCausedByException();
            if (cause == null) {
                ActionMessages actionMessages = new ActionMessages();
                actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.cam.general"));
                saveErrors(request, actionMessages);
            } else {
                if (cause instanceof LdapFilterException) {
                    ActionMessages actionMessages = new ActionMessages();
                    actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
                        "admin.role.LdapGroupFilterMessage"));
                    saveErrors(request, actionMessages);
                } else if (cause instanceof LdapCommunicationException) {
                    ActionMessages actionMessages = new ActionMessages();
                    SystemManagerLocal manager = LookupUtil.getSystemManager();
                    Properties options = manager.getSystemConfiguration(LookupUtil.getSubjectManager().getOverlord());
                    String providerUrl = options.getProperty(RHQConstants.LDAPUrl, "(unavailable)");
                    actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
                        "admin.role.LdapCommunicationMessage", providerUrl));
                    saveErrors(request, actionMessages);
                }
            }
        } catch (LdapFilterException lce) {
            ActionMessages actionMessages = new ActionMessages();
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("admin.role.LdapGroupFilterMessage"));
            saveErrors(request, actionMessages);
        } catch (LdapCommunicationException lce) {
            ActionMessages actionMessages = new ActionMessages();
            SystemManagerLocal manager = LookupUtil.getSystemManager();
            Properties options = manager.getSystemConfiguration(LookupUtil.getSubjectManager().getOverlord());
            String providerUrl = options.getProperty(RHQConstants.LDAPUrl, "(unavailable)");
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("admin.role.LdapCommunicationMessage",
                providerUrl));
            saveErrors(request, actionMessages);
        }

        //place calculated values into session.
        request.setAttribute(Constants.PENDING_RESGRPS_ATTR, pendingGroups);
        request.setAttribute(Constants.NUM_PENDING_RESGRPS_ATTR, new Integer(pendingGroups.getTotalSize()));
        request.setAttribute(Constants.AVAIL_RESGRPS_ATTR, availableGroups);
        request.setAttribute(Constants.NUM_AVAIL_RESGRPS_ATTR, new Integer(allGroups.size()));
        //store cachedAvailableGroups in session so trim down ldap communication chatter.
        request.getSession().setAttribute(LDAP_GROUP_CACHE, cachedAvailableLdapGroups);

        return null;
    }

    private Set<Map<String, String>> findPendingGroups(Set<String> pending, Set<Map<String, String>> allGroups) {
        Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
        for (Map<String, String> group : allGroups) {
            if (pending.contains(group.get("name"))) {
                ret.add(group);
            }
        }
        return ret;
    }

    private Set<Map<String, String>> findAvailableGroups(Set<String> pending, Set<Map<String, String>> allGroups) {
        Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
        for (Map<String, String> group : allGroups) {
            if (!pending.contains(group.get("name"))) {
                ret.add(group);
            }
        }
        return ret;
    }

    private Set<Map<String, String>> filterExisting(List<LdapGroup> pendingItems, Set<Map<String, String>> allGroups) {
        Set<String> pending = new HashSet<String>();
        for (LdapGroup group : pendingItems) {
            pending.add(group.getName());
        }

        Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
        for (Map<String, String> group : allGroups) {
            if (!pending.contains(group.get("name"))) {
                ret.add(group);
            }
        }
        return ret;
    }

    /** Method duplicates pageControl/pagination mechanism for LdapGroup data. This data has not been moved into the
     *  database yet so the PageList and PageControl mechanism does not yet work properly.
     *  There are only two columns so the pagination code uses Maps and Sorted Lists for efficient sorting.
     *
     * @param pagedGroupData Pagelist of Maps to be populated.
     * @param fullGroupData Full list of Maps available for paging
     * @param pc the pagination control from the web session reflecting user selections.
     * @return Pagelist includes datapoints matching pagination information passed in.
     */
    private PageList<Map<String, String>> paginateLdapGroupData(PageList<Map<String, String>> pagedGroupData,
        PageList<Map<String, String>> fullGroupData, PageControl pc) {
        if (pagedGroupData == null) { //
            pagedGroupData = new PageList<Map<String, String>>();
        }
        if ((fullGroupData == null) || (fullGroupData.isEmpty())) {
            return pagedGroupData;
        }
        if (pc == null) {
            pc = new PageControl(0, 15);
        }//npe defense

        //determine count to return up to 15|30|45 and page index
        int returnAmount = pc.getPageSize();
        int returnIndex = pc.getPageNumber();

        //determine sort order
        PageOrdering sortOrder = pc.getPrimarySortOrder();
        if (sortOrder == null) {
            sortOrder = PageOrdering.ASC;
            pc.setPrimarySortOrder(sortOrder);//reset on pc
        }

        //determine which column to sort on
        String sortColumn = pc.getPrimarySortColumn();
        if (sortColumn == null) {
            sortColumn = "lg.name";
            pc.setPrimarySort(sortColumn, sortOrder);//reset on pc
        }

        //now sort based off these values and populate sizedAvailableGroups accordingly
        ArrayList<Map<String, String>> groupsValues = fullGroupData.getValues();

        //store maps based off the keys and sort()
        Map<Integer, Map> groupLookup = new HashMap<Integer, Map>();
        TreeMap<String, Integer> groupNames = new TreeMap<String, Integer>();
        TreeMap<String, Integer> groupDescriptions = new TreeMap<String, Integer>();
        for (int i = 0; i < groupsValues.size(); i++) {
            Map<String, String> entry = groupsValues.get(i);
            Integer key = Integer.valueOf(i);
            groupLookup.put(key, entry);
            groupNames.put(entry.get("name"), key);
            groupDescriptions.put(entry.get("description"), key);
        }

        //do calculations to determine how many sorted values to return.
        int start, end;
        start = (int) (returnIndex * returnAmount);
        end = start + returnAmount;
        //        PageList<Map<String, String>> sizedAvailableGroups = new PageList<Map<String, String>>();

        //detect sort order
        boolean descending = false;
        if (PageOrdering.DESC == sortOrder) {
            descending = true;
        }
        //use sort column to determine which list to use
        if (sortColumn.equalsIgnoreCase("lg.name")) {
            int i = 0;
            List<String> keyList;

            if (descending) {
                keyList = new ArrayList<String>(groupNames.keySet());
                Collections.reverse(keyList);
            } else {
                keyList = new ArrayList<String>(groupNames.keySet());
                Collections.sort(keyList);
            }
            for (String key : keyList) {
                if ((i >= start) && (i < end)) {
                    pagedGroupData.add(groupLookup.get(groupNames.get(key)));
                }
                i++;
            }
        } else {
            int i = 0;
            List<String> keyList;
            if (descending) {
                keyList = new ArrayList<String>(groupDescriptions.keySet());
                Collections.reverse(keyList);
            } else {
                keyList = new ArrayList<String>(groupDescriptions.keySet());
                Collections.sort(keyList);
            }
            for (String key : keyList) {
                if ((i >= start) && (i < end)) {
                    pagedGroupData.add(groupLookup.get(groupDescriptions.get(key)));
                }
                i++;
            }
        }

        return pagedGroupData;
    }
}

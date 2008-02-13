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
package org.rhq.enterprise.gui.legacy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.Constants;

/* XXX: use reflection to combine some of these methods? */

/**
 * Utilities class that provides convenience methods for operating on bizapp objects.
 *
 * @deprecated
 */
@Deprecated
public class BizappUtils {
    /**
     * Caches the values returned by buildSupportedAIServerTypes
     */
    private static Map AIServerTypesByPlatform = new HashMap();
    private static Map platformTypesById = new HashMap();
    private static Map platformTypesByName = new HashMap();

    private static Log log = LogFactory.getLog(BizappUtils.class.getName());

    //    /** A helper method used by setRuntimeAIMessage */
    //    private static String getServiceName(PageList serviceTypes, int index,
    //                                         String stName) {
    //        String baseName
    //            = ((ServiceTypeValue) serviceTypes.get(index)).getName();
    //        return StringUtil.pluralize(StringUtil.removePrefix(baseName, stName));
    //    }

    /**
     * Return the full name of the subject.
     *
     * @param fname the subject's first name
     * @param lname the subject's last name
     */
    public static String makeSubjectFullName(String fname, String lname) {
        // XXX: what about Locales that display last name first?

        StringBuffer full = new StringBuffer();
        if ((fname == null) || fname.equals("")) {
            if ((lname != null) && !lname.equals("")) {
                full.append(lname);
            }
        } else {
            if ((lname == null) || lname.equals("")) {
                full.append(fname);
            } else {
                full.append(fname);
                full.append(" ");
                full.append(lname);
            }
        }

        return full.toString();
    }

    //    public static PlatformTypeValue getPlatformTypeName(ServletContext ctx,
    //                                                        HttpServletRequest request,
    //                                                        String name)
    //        throws Exception {
    //
    //        PlatformTypeValue ptValue;
    //
    //        // check the cache first
    //        synchronized (platformTypesByName) {
    //            ptValue = (PlatformTypeValue) platformTypesByName.get(name);
    //            if (ptValue != null) return ptValue;
    //        }
    //
    //        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
    //        int sessionId = RequestUtils.getSessionIdInt(request);
    //        ptValue = appdefBoss.findPlatformTypeByName(sessionId, name);
    //
    //        synchronized (platformTypesByName) {
    //            platformTypesByName.put(name, ptValue);
    //        }
    //        return ptValue;
    //    }
    //
    //    /**
    //     * filter on a list of AIAppdefResourceValue.  Either get the ignored
    //     * resources or non-ignored.
    //     *
    //     * @param resources List of AIAppdefResources to filter
    //     * @param status flag to indicate if we get the ignored/non-ignored
    //     * @return the list of resources
    //     */
    //    public static List<AiqResource> filterAIResourcesByStatus(List<AiqResource> resources, Integer status)
    //    {
    //        if (status == null || status.intValue() == -1)
    //            return resources;
    //
    //        List<AiqResource> resourceList = new PageList<AiqResource>();
    //
    //        Iterator<AiqResource> sIterator = resources.iterator();
    //        while (sIterator.hasNext())
    //        {
    //            AiqResource rValue = sIterator.next();
    //            if (rValue.getQueueStatus() != null)
    //            {
    //               if (rValue.getQueueStatus() == status.intValue() )
    //               {
    //                   resourceList.add(rValue);
    //               }
    //            }
    //
    //        }
    //
    //        return resourceList;
    //    }
    //
    //    /**
    //     * filter on a list of AIAppdefResourceValue by Server Type.
    //     *
    //     * @param resources List of AIAppdefResources to filter
    //     * @param ignored flag to indicate if we get the ignored/non-ignored
    //     */
    //    public static List filterAIResourcesByServerType(List resources, String name)
    //    {
    //        if (name == null || name.equals("") )
    //            return resources;
    //
    //        List resourceList = new PageList();
    //
    //        Iterator sIterator = resources.iterator();
    //        while (sIterator.hasNext())
    //        {
    //            AiqServer rValue = (AiqServer)sIterator.next();
    //            if (rValue.getServerTypeName().equals(name) )
    //            {
    //                resourceList.add(rValue);
    //            }
    //
    //        }
    //
    //        return resourceList;
    //    }
    //
    //    /**
    //     * When displaying the config options (both in the ViewXXX and EditXXX
    //     * tiles), we display a message "Auto-Discover foo, bar, and other
    //     * services?" next to the checkbox.  The "foo, bar, and other" part
    //     * is what gets generated here and stuck in the request attributes
    //     * as the Constants.SERVICE_TYPE_EXAMPLE_LIST attribute.
    //     */
    //    public static void setRuntimeAIMessage (int sessionId,
    //                                            HttpServletRequest request,
    //                                            ServerValue server,
    //                                            AppdefBoss appdefBoss)
    //        throws SessionTimeoutException, SessionNotFoundException,
    //               RemoteException {
    //
    //        // Find a couple of sample services
    //        int serverTypeId = server.getServerType().getId().intValue();
    //        PageList serviceTypes
    //            = appdefBoss.findServiceTypesByServerType(sessionId, serverTypeId);
    //        String serviceNameList;
    //        int numServiceTypes = serviceTypes.size();
    //        String serviceName;
    //        String stName = server.getServerType().getName();
    //
    //        if (numServiceTypes == 0) {
    //            // Should not really happen
    //            serviceNameList = "services";
    //
    //        } else if (numServiceTypes == 1) {
    //            serviceNameList = getServiceName(serviceTypes, 0, stName);
    //
    //        } else if (numServiceTypes == 2) {
    //            serviceNameList
    //                = getServiceName(serviceTypes, 0, stName)
    //                + " and "
    //                + getServiceName(serviceTypes, 1, stName);
    //        } else {
    //            serviceNameList
    //                = getServiceName(serviceTypes, 0, stName)
    //                + ", "
    //                + getServiceName(serviceTypes, 1, stName)
    //                + ", and other services";
    //        }
    //        // System.err.println("serviceNameList---->" + serviceNameList);
    //        request.setAttribute(Constants.AI_SAMPLE_SERVICETYPE_LIST,
    //                             serviceNameList);
    //    }
    //
    //    /**
    //     * builds a list of ids of ai resources for resources which are not
    //     * ignored.
    //     */
    //    public static List buildAIIpResourceIds(AiqIp[] aiResources, boolean ignored) {
    //       List listServerIds = new ArrayList();
    //
    //       for (int i = 0; i < aiResources.length; i++)
    //          if (aiResources[i].getIgnored() == ignored )
    //             listServerIds.add(aiResources[i].getId());
    //
    //       return listServerIds;
    //    }
    //
    //    /**
    //     * builds a list of ids of ai resources for resources which are not
    //     * ignored.
    //     */
    //    public static List buildAIServerResourceIds(AiqServer[] aiResources, boolean ignored) {
    //       List listServerIds = new ArrayList();
    //
    //       for (int i = 0; i < aiResources.length; i++)
    //          if (aiResources[i].getIgnored() == ignored )
    //             listServerIds.add(aiResources[i].getId());
    //
    //       return listServerIds;
    //    }
    //
    //    /**
    //     * build a list of supported server types for ai subsystem
    //     */
    //    public static AppdefResourceTypeValue[] buildSupportedAIServerTypes
    //        (ServletContext ctx,
    //         HttpServletRequest request,
    //         PlatformValue pValue) throws Exception {
    //
    //        PlatformTypeValue ptValue = pValue.getPlatformType();
    //
    //        // check the cache first
    //        AppdefResourceTypeValue[] sType = null;
    //        synchronized(AIServerTypesByPlatform) {
    //            sType = (AppdefResourceTypeValue[])
    //                AIServerTypesByPlatform.get(ptValue.getId());
    //            if (sType != null) return sType;
    //        }
    //
    //        AIBoss aiBoss = ContextUtils.getAIBoss(ctx);
    //        int sessionId = RequestUtils.getSessionIdInt(request);
    //
    //        // build support ai server types
    //        ServerTypeValue[] serverTypeValObjs = ptValue.getServerTypeValues();
    //
    //        List serverTypeVals = new ArrayList();
    //        for (int i=0, size=serverTypeValObjs.length; i<size; i++) {
    //            //XXX NewServerFormPrepareAction does similar, should there
    //            //be a generic method?
    //            if (serverTypeValObjs[i].getVirtual()) {
    //                continue;
    //            }
    //            serverTypeVals.add(serverTypeValObjs[i]);
    //        }
    //
    //        Map serverSigs = aiBoss.getServerSignatures(sessionId,
    //                                                    serverTypeVals);
    //
    //        List filteredServerTypes =
    //            BizappUtils.buildServerTypesFromServerSig(serverTypeValObjs,
    //                            serverSigs.values().iterator());
    //        sType = new AppdefResourceTypeValue[filteredServerTypes.size()];
    //
    //        filteredServerTypes.toArray(sType);
    //        synchronized(AIServerTypesByPlatform) {
    //            AIServerTypesByPlatform.put(ptValue.getId(), sType);
    //        }
    //        return sType;
    //    }
    //
    //    /**
    //     * build a list of server types extracted from the ai server list
    //     */
    //    public static AppdefResourceTypeValue[] buildfilteredAIServerTypes(
    //            AppdefResourceTypeValue[] supportedResTypes,
    //            AiqServer[] sValues) {
    //        List filteredServerTypes = new ArrayList();
    //        for (int i = 0; i < sValues.length; i++)
    //        {
    //            String sTypeName = sValues[i].getServerTypeName();
    //            AppdefResourceTypeValue appdefType =
    //                    findResourceTypeValue(supportedResTypes, sTypeName);
    //           if (appdefType != null)
    //            filteredServerTypes.add(appdefType);
    //        }
    //
    //        filteredServerTypes = sortAppdefResourceType(filteredServerTypes);
    //        AppdefResourceTypeValue[] sType = new AppdefResourceTypeValue[filteredServerTypes.size()];
    //
    //        filteredServerTypes.toArray(sType);
    //        return sType;
    //    }
    //
    //    /**
    //     * use this comparator to sort the AppdefResourceTypeValue objects
    //     */
    //    private static Comparator COMPARE_NAME = new Comparator() {
    //        public int compare(Object obj1, Object obj2) {
    //
    //            AppdefResourceTypeValue resType1 = (AppdefResourceTypeValue)obj1;
    //            AppdefResourceTypeValue resType2 = (AppdefResourceTypeValue)obj2;
    //
    //            if (resType1 == null) {
    //                if (resType2 == null) return 0;
    //                else return Integer.MAX_VALUE;
    //            }
    //            if (resType2 == null) return Integer.MIN_VALUE;
    //
    //            return resType1.getName().compareToIgnoreCase(resType2.getName());
    //        }
    //    };
    //
    //    /**
    //     * use this class to sort pending AppdefResourceTypeValue objects
    //     * for groups.  case-sensitive sort.
    //     */
    //    private class AppdefResourceNameComparator implements Comparator
    //    {
    //        private PageControl pc = null;
    //
    //        public AppdefResourceNameComparator(PageControl pc)
    //        {
    //            this.pc = pc;
    //        }
    //
    //            /* (non-Javadoc)
    //         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
    //         */
    //        public int compare(Object obj1, Object obj2) {
    //
    //            AppdefResourceValue res1 = (AppdefResourceValue)obj1;
    //            AppdefResourceValue res2 = (AppdefResourceValue)obj2;
    //
    //            if (pc != null && pc.isDescending() )
    //                return -(res1.getName().compareTo(res2.getName()));
    //            else
    //                return res1.getName().compareTo(res2.getName());
    //        }
    //
    //    }
    //
    //    /**
    //     * use this class to sort the AppdefResourceTypeValue objects
    //     */
    //    private class AIResourceIdComparator implements Comparator
    //    {
    //        public int compare(Object obj1, Object obj2)
    //        {
    //            AiqResource res1 = (AiqResource)obj1;
    //            AiqResource res2 = (AiqResource)obj2;
    //
    //            int res1Id = res1.getId();
    //            int res2Id = res2.getId();
    //
    //            return (res1Id < res2Id ? -1 : ( res1Id == res2Id ? 0 : 1));
    //        }
    //    }
    //
    //    public static List sortAppdefResourceType(List resourceType)
    //    {
    //        List sortedList = new ArrayList();
    //        SortedSet sSet = new TreeSet(COMPARE_NAME);
    //        sSet.addAll(resourceType);
    //        CollectionUtils.addAll(sortedList, sSet.iterator());
    //        return sortedList;
    //    }
    //
    //    public static List sortAIResource(List resource)
    //    {
    //        List sortedList = new ArrayList();
    //        SortedSet sSet =
    //            new TreeSet(new BizappUtils().new AIResourceIdComparator());
    //        sSet.addAll(resource);
    //        CollectionUtils.addAll(sortedList, sSet.iterator());
    //        return sortedList;
    //    }
    //
    //    /**
    //     * builds a list of server types from ServerSignature objects
    //     *
    //     * @param sTypes list of AppdefResourceType objects
    //     * @param serverSigs list of ServerSignature objects
    //     */
    //    public static List buildServerTypesFromServerSig(AppdefResourceTypeValue[] sTypes,
    //                                                     Iterator sigIterator)
    //    {
    //        List serverTypes = new ArrayList();
    //        synchronized(serverTypes) {
    //            while (sigIterator.hasNext()) {
    //                ServerSignature st = (ServerSignature)sigIterator.next();
    //
    //                AppdefResourceTypeValue stype =
    //                        BizappUtils.findResourceTypeValue(sTypes, st.getServerTypeName());
    //                serverTypes.add(stype);
    //            }
    //        }
    //        Collections.sort(serverTypes, COMPARE_NAME);
    //        return serverTypes;
    //    }
    //
    //    /**
    //     * find a ResourceTypeValue object from a list of ResourceTypeValue obects.
    //     *
    //     * @param resourceTypeVals
    //     * @param name name of the ResourceTypeValue object to find
    //     *
    //     * @return a ResourceTypeValue or null
    //     */
    //    public static AppdefResourceTypeValue findResourceTypeValue(
    //        AppdefResourceTypeValue[] resourceTypes, String name) {
    //        for(int i = 0; i < resourceTypes.length; i++)
    //        {
    //            AppdefResourceTypeValue resourceType = resourceTypes[i];
    //            if (resourceType != null && resourceType.getName().equals(name) )
    //                return resourceType;
    //        }
    //
    //        return null;
    //    }
    //
    //    /**
    //     * This method builds a list of AppdefResourceValue objects
    //     * from a list of AppdefEntityID.
    //     *
    //     * This function should be moved into bizapp layer if possible
    //     * later.  I am leaving it until I have an api which provides
    //     * similar functionality. - mtk
    //     *
    //     * @return a list of AppdefResourceValue objects
    //     */
    //    public static List buildAppdefResources(int sessionId, AppdefBoss boss,
    //                                            AppdefEntityID[] entities)
    //        throws ObjectNotFoundException, RemoteException,
    //               SessionTimeoutException, SessionNotFoundException,
    //               PermissionException
    //    {
    //        if (entities == null)
    //            return new ArrayList();
    //
    //        return boss.findByIds(sessionId, entities);
    //    }
    //
    //    /**
    //     * This method sorts list of AppdefResourceValue objects
    //     *
    //     * @return a list of AppdefResourceValue objects
    //     */
    //    public static List sortAppdefResource(List appdefList, PageControl pc)
    //    {
    //        List sortedList = new ArrayList();
    //        SortedSet sSet =
    //            new TreeSet(new BizappUtils().new AppdefResourceNameComparator(pc));
    //        sSet.addAll(appdefList);
    //        CollectionUtils.addAll(sortedList, sSet.iterator());
    //
    //        // There are duplicated names, figure out where to insert them
    //        for (Iterator it = appdefList.iterator();
    //             sortedList.size() != appdefList.size() && it.hasNext(); ) {
    //            AppdefResourceValue res = (AppdefResourceValue) it.next();
    //
    //            for (int i = 0; i < sortedList.size(); i++) {
    //                AppdefResourceValue sorted =
    //                    (AppdefResourceValue) sortedList.get(i);
    //                if (sorted.getEntityId().equals(res.getEntityId()))
    //                    break;
    //
    //                // Either it's meant to go in between or the last
    //                if (res.getName().toLowerCase().compareTo(
    //                    sorted.getName().toLowerCase()) < 0 ||
    //                    i == sortedList.size() - 1) {
    //                    sortedList.add(i, res);
    //                    break;
    //                }
    //            }
    //        }
    //
    //        return sortedList;
    //
    //    }
    //
    //    /**
    //     * This method builds a list of AppdefEntityID objects from
    //     * [entityType]:[resourceTypeId] strings
    //     *
    //     * @param entitIds list of [entityType]:[resourceTypeId] strings
    //     */
    //    public static List buildAppdefEntityIds(List entityIds)
    //    {
    //        List entities = new ArrayList();
    //        Iterator rIterator = entityIds.iterator();
    //        while (rIterator.hasNext())
    //        {
    //            AppdefEntityID entityId = new AppdefEntityID(
    //                        (String)rIterator.next());
    //            entities.add(entityId);
    //        }
    //
    //        return entities;
    //    }
    //
    //
    //    /**
    //     * builds the value objects in the form: [entity type id]:[resource type id]
    //     *
    //     * @param resourceTypes
    //     * @return List
    //     * @throws InvalidAppdefTypeException
    //     */
    //    public static PageList buildAppdefOptionList(List<org.jboss.on.domain.resource.ResourceType> resourceTypes,
    //                                                 boolean useHyphen)
    //        throws InvalidAppdefTypeException {
    //        PageList optionList = new PageList();
    //
    //        if (resourceTypes == null)
    //            return optionList;
    //
    //        Iterator aIterator = resourceTypes.iterator();
    //        optionList.setTotalSize(resourceTypes.size() );
    //        for (org.jboss.on.domain.resource.ResourceType type : resourceTypes) {
    //        //while (aIterator.hasNext())
    //        //{
    //            //AppdefResourceTypeValue sTypeVal =
    //            //                (AppdefResourceTypeValue)aIterator.next();
    //
    //            HashMap<String, Object> map1 = new HashMap<String, Object>(2);
    //            map1.put("value", type.getId());
    //            //map1.put("value", sTypeVal.getAppdefTypeKey() );
    //            if (useHyphen) {
    //                //map1.put("label", "- " + sTypeVal.getName());
    //               map1.put("label", "- " + type.getName());
    //            } else {
    //                map1.put("label", type.getName());
    //            }
    //
    //            optionList.add(map1);
    //        }
    //
    //        return optionList;
    //    }
    //

    /**
     * Return the full name of the subject.
     *
     * @param subject the subject
     */
    public static String makeSubjectFullName(Subject subject) {
        return makeSubjectFullName(subject.getFirstName(), subject.getLastName());
    }

    /**
     * build group types and its corresponding resource string respresentations from the ApplicationResources.properties
     * file.
     *
     * @return a list
     */
    public static List buildGroupTypes(HttpServletRequest request) {
        List<Map> groupTypes = new ArrayList<Map>();

        Map<String, Object> map2 = new HashMap<String, Object>(2);
        map2.put("value", GroupCategory.COMPATIBLE.name());
        map2.put("label", RequestUtils.message(request, "resource.group.inventory.CompatibleClusterResources"));
        groupTypes.add(map2);

        Map<String, Object> map1 = new HashMap<String, Object>(2);
        map1.put("value", GroupCategory.MIXED.name());
        map1.put("label", RequestUtils.message(request, "resource.group.inventory.MixedResources"));
        groupTypes.add(map1);

        return groupTypes;
    }

    /**
     * build group types and its corresponding resource string respresentations from the ApplicationResources.properties
     * file.
     *
     * @return a list
     */
    public static String getGroupLabel(HttpServletRequest request, ResourceGroup group) {
        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {
            return RequestUtils.message(request, "resource.group.inventory.CompatibleClusterResources");
        } else if (group.getGroupCategory() == GroupCategory.MIXED) {
            return RequestUtils.message(request, "resource.group.inventory.MixedResources");
        } else {
            throw new RuntimeException("Group label for type " + group.getClass().getName() + " not supported yet");
        }
    }

    //    /**
    //     * builds a list of AppdefResourceValue objects from a list
    //     * of AppdefEntityID objects stored in the group.
    //     *
    //     * @param group AppdefGroupValue which contains the list of resources
    //     * @return a list of AppdefResourceValue objects
    //     */
    //    public static List buildGroupResources(
    //        AppdefBoss boss,
    //        int sessionId,
    //        AppdefGroupValue group)
    //        throws ObjectNotFoundException, RemoteException, PermissionException,
    //               SessionTimeoutException, SessionNotFoundException
    //    {
    //        List grpEntries = group.getAppdefGroupEntries();
    //
    //        AppdefEntityID[] entities = new AppdefEntityID[grpEntries.size()];
    //        entities = (AppdefEntityID[]) grpEntries.toArray(entities);
    //
    //        return boss.findByIds(sessionId, entities);
    //    }

    /**
     * Return an array of operation ids as <code>String</code> objects corresponding to the operations contained in the
     * input map of resource types to operations.
     *
     * @param map the <code>Map</code> of resource types to operations
     */
    public static Integer[] mapOperationIds(Map map) {
        ArrayList ids = new ArrayList();
        Iterator ri = map.keySet().iterator();
        while (ri.hasNext()) {
            String name = (String) ri.next();
            List objs = (List) map.get(name);
            //            if (objs != null) {
            //                Iterator oi = objs.iterator();
            //                while (oi.hasNext()) {
            //                    OldOperation obj = (OldOperation) oi.next();
            //                    ids.add(obj.getId());
            //                }
            //            }
        }

        return (Integer[]) ids.toArray(new Integer[0]);
    }

    //    /**
    //     * Return the <code>Operation</code> object with the given
    //     * name from a list.
    //     * @param operations the <code>List</code> of operations
    //     * @param name the sought operation name
    //     */
    //    public static OldOperation findOperation(List operations, String name) {
    //        if (operations == null || name == null) {
    //            return null;
    //        }
    //
    //        Iterator oi = operations.iterator();
    //        while (oi.hasNext()) {
    //            OldOperation op = (OldOperation) oi.next();
    //            if (name.equals(op.getName())) {
    //                return op;
    //            }
    //        }
    //
    //        return null;
    //    }

    /**
     * Return a <code>List</code> of <code>Operation</code> objects corresponding to the input array of operation ids.
     *
     * @param operations the <code>List</code> of operations
     * @param ids        the operation ids
     */
    public static List mapOperations(List operations, List ids) {
        if ((operations == null) || (ids == null)) {
            return new ArrayList(0);
        }

        // build an index of operations
        HashMap index = new HashMap();
        Iterator oi = operations.iterator();
        //        while (oi.hasNext()) {
        //            OldOperation op = (OldOperation) oi.next();
        //            index.put(op.getId(), op);
        //        }

        // find the operation for each given id
        List objects = new ArrayList(operations.size());
        Iterator ii = ids.iterator();
        while (ii.hasNext()) {
            objects.add(index.get(ii.next()));
        }

        return objects;
    }

    /**
     * Check in the permissions map to see if the user can administer CAM.
     *
     * @param  A Map of Lists that contains the different groups of permissions.
     *
     * @return Whether or not the admin cam is contained in the type map.
     */
    public static boolean canAdminCam(Map roleOps) {
        if (roleOps == null) {
            return false;
        }

        List ops = (List) roleOps.get("covalentAuthzRootResourceType");
        if (ops == null) {
            return false;
        }

        //        Iterator i = ops.iterator();
        //        while (i.hasNext()) {
        //            OldOperation ov = (OldOperation)i.next();
        //            if ("administerCAM".equals(ov.getName())) {
        //                return true;
        //            }
        //        }

        return false;
    }

    /**
     * Check in the permissions map to see if the user can administer RHQ with the given permission.
     *
     * @param  request contains the attribute that has the user operations
     *
     * @return true if the current user is allowed to administer RHQ
     */
    public static boolean hasPermission(HttpServletRequest request, org.rhq.core.domain.authz.Permission perm) {
        HttpSession session = request.getSession(true);
        Map<String, Boolean> userOpsMap = (Map<String, Boolean>) session.getAttribute(Constants.USER_OPERATIONS_ATTR);

        if (userOpsMap == null) {
            return false;
        }

        return userOpsMap.containsKey(perm.toString());
    }

    /**
     * adds a list of AppdefEntityID objects to a group if this api is used in bizapp, will use it.
     */
    //    public static void addResourcesToGroup(AppdefGroupValue group, List ids)
    //        throws GroupVisitorException {
    //        Iterator iterator = ids.iterator();
    //        while (iterator.hasNext()) {
    //            String id = (String) iterator.next();
    //            List groupEntries = group.getAppdefGroupEntries();
    //
    //            AppdefEntityID entity = new AppdefEntityID(id);
    //            if (!group.existsAppdefEntity(entity))
    //                group.addAppdefEntity(entity);
    //        }
    //    }
    /**
     * Update the given role operations map to reflect the gui's permission model. Specifically, copy each operation on
     * the root resource type into the list for the resource type that the is display-wise associated with. For
     * instance, the "addSubject" operation is associated in the Bizapp with the root resource type, but we we need it
     * to be associated with the subject resource type for display purposes.
     *
     * @param map the <code>Map</code> of role operations
     */
    public static void fixupRoleOperationMap(Map map) {
        // i hate thos method as much as you do

        //        List ops = (List) map.get("covalentAuthzRootResourceType");
        //
        //        Iterator i = null;
        //        OldOperation op = null;
        //        String opName = null;
        //        List typeOps = null;
        //
        //        if (ops != null) {
        //            i = ops.iterator();
        //            while (i.hasNext()) {
        //                op = (OldOperation) i.next();
        //                opName = op.getName();
        //
        //                if ("createSubject".equals(opName) ||
        //                    "viewSubject".equals(opName)   ||
        //                    "modifySubject".equals(opName) ||
        //                    "removeSubject".equals(opName)) {
        //                    typeOps = (List) map.get("covalentAuthzSubject");
        //                    if (typeOps == null) {
        //                        typeOps = new ArrayList();
        //                    }
        //                    map.put("covalentAuthzSubject", typeOps);
        //                }
        //                else if ("createRole".equals(opName)) {
        //                    typeOps = (List) map.get("covalentAuthzRole");
        //                    if (typeOps == null) {
        //                        typeOps = new ArrayList();
        //                    }
        //                    map.put("covalentAuthzRole", typeOps);
        //                }
        //                else if ("createPlatform".equals(opName)) {
        //                    typeOps = (List) map.get("covalentEAMPlatform");
        //                    if (typeOps == null) {
        //                        typeOps = new ArrayList();
        //                    }
        //                    map.put("covalentEAMPlatform", typeOps);
        //                }
        //                else if ("createApplication".equals(opName)) {
        //                    typeOps = (List) map.get("covalentEAMApplication");
        //                    if (typeOps == null) {
        //                        typeOps = new ArrayList();
        //                    }
        //                    map.put("covalentEAMApplication", typeOps);
        //                }
        //
        //                if (typeOps != null) {
        //                    typeOps.add(op);
        //                }
        //            }
        //        }
        //
        //        typeOps = null;
        //        ops = (List) map.get("covalentEAMPlatform");
        //        if (ops != null) {
        //            i = ops.iterator();
        //            while (i.hasNext()) {
        //                op = (OldOperation) i.next();
        //                opName = op.getName();
        //
        //                if ("addServer".equals(opName)) {
        //                    typeOps = (List) map.get("covalentEAMServer");
        //                    if (typeOps == null) {
        //                        typeOps = new ArrayList();
        //                    }
        //                    typeOps.add(op);
        //                    map.put("covalentEAMServer", typeOps);
        //                }
        //            }
        //        }
        //
        //        typeOps = null;
        //        ops = (List) map.get("covalentEAMServer");
        //        if (ops != null) {
        //            i = ops.iterator();
        //            while (i.hasNext()) {
        //                op = (OldOperation) i.next();
        //                opName = op.getName();
        //
        //                if ("addService".equals(opName)) {
        //                    typeOps = (List) map.get("covalentEAMService");
        //                    if (typeOps == null) {
        //                        typeOps = new ArrayList();
        //                    }
        //                    typeOps.add(op);
        //                    map.put("covalentEAMService", typeOps);
        //                }
        //            }
        //        }
    }

    public static List loadPermissions(List operations) {
        // XXX: configure perm/op mapping externally?
        List perms = new ArrayList();

        //        if (operations == null) {
        //            return perms;
        //        }
        //
        //        Iterator i = operations.iterator();
        //        Map<String, OldOperation> opIdx = new HashMap<String, OldOperation>(operations.size());
        //        while (i.hasNext()) {
        //            OldOperation op = (OldOperation) i.next();
        //            opIdx.put(op.getName(), op);
        //        }
        //
        //        Permission viewPerms = new Permission("view");
        //        viewPerms.putOperation("covalentAuthzSubject",
        //                               opIdx.get("viewSubject"));
        //        viewPerms.putOperation("covalentAuthzRole",
        //                               opIdx.get("viewRole"));
        //        viewPerms.putOperation("covalentAuthzResourceGroup",
        //                               opIdx.get("viewResourceGroup"));
        //        viewPerms.putOperation("covalentEAMPlatform",
        //                               opIdx.get("viewPlatform"));
        //        viewPerms.putOperation("covalentEAMServer",
        //                               opIdx.get("viewServer"));
        //        viewPerms.putOperation("covalentEAMService",
        //                               opIdx.get("viewService"));
        //        viewPerms.putOperation("covalentEAMApplication",
        //                               opIdx.get("viewApplication"));
        //        perms.add(viewPerms);
        //
        //        Permission createPerms = new Permission("create");
        //        createPerms.putOperation("covalentAuthzSubject",
        //                                 opIdx.get("createSubject"));
        //        createPerms.putOperation("covalentAuthzRole",
        //                                 opIdx.get("createRole"));
        //        createPerms.putOperation("covalentEAMPlatform",
        //                                 opIdx.get("createPlatform"));
        //        createPerms.putOperation("covalentEAMServer",
        //                                 opIdx.get("addServer"));
        //        createPerms.putOperation("covalentEAMService",
        //                                 opIdx.get("addService"));
        //        createPerms.putOperation("covalentEAMApplication",
        //                                 opIdx.get("createApplication"));
        //        perms.add(createPerms);
        //
        //        Permission modifyPerms = new Permission("modify");
        //        modifyPerms.putOperation("covalentAuthzSubject",
        //                                 opIdx.get("modifySubject"));
        //        modifyPerms.putOperation("covalentAuthzRole",
        //                                 opIdx.get("modifyRole"));
        //        modifyPerms.putOperation("covalentAuthzResourceGroup",
        //                                 opIdx.get("modifyResourceGroup"));
        //        modifyPerms.putOperation("covalentEAMPlatform",
        //                                 opIdx.get("modifyPlatform"));
        //        modifyPerms.putOperation("covalentEAMServer",
        //                                 opIdx.get("modifyServer"));
        //        modifyPerms.putOperation("covalentEAMService",
        //                                 opIdx.get("modifyService"));
        //        modifyPerms.putOperation("covalentEAMApplication",
        //                                 opIdx.get("modifyApplication"));
        //        perms.add(modifyPerms);
        //
        //        Permission deletePerms = new Permission("delete");
        //        deletePerms.putOperation("covalentAuthzSubject",
        //                                 opIdx.get("removeSubject"));
        //        deletePerms.putOperation("covalentAuthzRole",
        //                                 opIdx.get("removeRole"));
        //        deletePerms.putOperation("covalentAuthzResourceGroup",
        //                                 opIdx.get("removeResourceGroup"));
        //        deletePerms.putOperation("covalentEAMPlatform",
        //                                 opIdx.get("removePlatform"));
        //        deletePerms.putOperation("covalentEAMServer",
        //                                 opIdx.get("removeServer"));
        //        deletePerms.putOperation("covalentEAMService",
        //                                 opIdx.get("removeService"));
        //        deletePerms.putOperation("covalentEAMApplication",
        //                                 opIdx.get("removeApplication"));
        //        perms.add(deletePerms);
        //
        //        Permission alertPerms = new Permission("alert");
        //        alertPerms.putOperation("covalentAuthzResourceGroup",
        //                                 opIdx.get("manageGroupAlerts"));
        //        alertPerms.putOperation("covalentEAMPlatform",
        //                                 opIdx.get("managePlatformAlerts"));
        //        alertPerms.putOperation("covalentEAMServer",
        //                                 opIdx.get("manageServerAlerts"));
        //        alertPerms.putOperation("covalentEAMService",
        //                                 opIdx.get("manageServiceAlerts"));
        //        alertPerms.putOperation("covalentEAMApplication",
        //                                 opIdx.get("manageApplicationAlerts"));
        //        perms.add(alertPerms);
        //
        ////         Permission monitorPerms = new Permission("monitor");
        ////         monitorPerms.putOperation("covalentEAMPlatform",
        ////                                   (Operation) opIdx.get("monitorPlatform"));
        ////         monitorPerms.putOperation("covalentEAMServer",
        ////                                   (Operation) opIdx.get("monitorServer"));
        ////         monitorPerms.putOperation("covalentEAMService",
        ////                                   (Operation) opIdx.get("monitorService"));
        ////         monitorPerms.putOperation("covalentEAMApplication",
        ////                                   (Operation) opIdx.get("monitorApplication"));
        ////         perms.add(monitorPerms);
        //
        //        Permission controlPerms = new Permission("control");
        //        controlPerms.putOperation("covalentEAMPlatform",
        //                                  opIdx.get("controlPlatform"));
        //        controlPerms.putOperation("covalentEAMServer",
        //                                  opIdx.get("controlServer"));
        //        controlPerms.putOperation("covalentEAMService",
        //                                  opIdx.get("controlService"));
        //        controlPerms.putOperation("covalentEAMApplication",
        //                                  opIdx.get("controlApplication"));
        //        perms.add(controlPerms);
        //
        return perms;
    }

    /**
     * Return a <code>List</code> of <code>Subject</code> objects from a list that do <strong>not</strong> appear in a
     * list of matches.
     *
     * @param all     the list to operate on
     * @param matches the list to grep out
     */
    public static List grepSubjects(List all, List matches) {
        if ((all == null) || (matches == null)) {
            return new ArrayList(0);
        }

        // build an index of role subjects
        HashMap index = new HashMap();
        Iterator mi = matches.iterator();
        while (mi.hasNext()) {
            Subject m = (Subject) mi.next();
            index.put(m.getId(), m);
        }

        // find available subjects (those not in the index)
        ArrayList objects = new ArrayList();
        Iterator ai = all.iterator();
        while (ai.hasNext()) {
            Subject obj = (Subject) ai.next();
            if (index.get(obj.getId()) == null) {
                objects.add(obj);
            }
        }

        return objects;
    }

    /**
     * Return a <code>List</code> of <code>ResourceTypeValue</code> objects from a given list that does not include the
     * root resource type.
     *
     * @param all     the list to operate on
     * @param matches the list to grep out
     */
    public static List filterTypes(List all) {
        if (all == null) {
            return new ArrayList(0);
        }

        ArrayList objects = new ArrayList();
        Iterator ai = all.iterator();
        //        while (ai.hasNext()) {
        //            ResourceType obj = (ResourceType) ai.next();
        //            // XXX: look for "system" flag
        //            String name = obj.getName();
        //            if (! "covalentAuthzRootResourceType".equals(name)) {
        //                objects.add(obj);
        //            }
        //        }

        return objects;
    }

    // A map of ServerType.name -> Boolean (true for auto-approved server types)
    private static Map serverTypeCache = new HashMap();

    //    private synchronized static void loadServerTypeCache(int sessionId,
    //                                                         AppdefBoss appdefBoss){
    //        if (serverTypeCache.size() > 0) return;
    //        PageList types;
    //        try {
    //            types = appdefBoss.findAllServerTypes(sessionId,
    //                                                  PageControl.PAGE_ALL);
    //        } catch (Exception e) {
    //            throw new IllegalStateException("Error loading server types: " + e);
    //        }
    //        ServerTypeValue stValue;
    //        String name;
    //        for (int i=0; i<types.size(); i++) {
    //            stValue = (ServerTypeValue) types.get(i);
    //            name = stValue.getName();
    //            if (stValue.getVirtual()) {
    //                serverTypeCache.put(name, Boolean.TRUE);
    //            } else {
    //                serverTypeCache.put(name, Boolean.FALSE);
    //            }
    //        }
    //    }
    //
    //    public static boolean isAutoApprovedServer(int sessionId,
    //                                               AppdefBoss appdefBoss,
    //                                               AiqServer aiServer) {
    //        // Load the server type cache if it's not loaded already
    //        synchronized(serverTypeCache) {
    //            if (serverTypeCache.size() == 0) {
    //                loadServerTypeCache(sessionId, appdefBoss);
    //            }
    //        }
    //        Boolean isAutoApproved
    //            = (Boolean) serverTypeCache.get(aiServer.getServerTypeName());
    //        if (isAutoApproved == null) {
    //            // Should never happen
    //            return false;
    //        }
    //        return isAutoApproved.booleanValue();
    //    }
    //
    //    // RHQ
    //    public static boolean isAutoApprovedServer(int sessionId,
    //                                               AppdefBoss appdefBoss,
    //                                               Resource server) {
    //        // Load the server type cache if it's not loaded already
    //        synchronized(serverTypeCache) {
    //            if (serverTypeCache.isEmpty()) {
    //                loadServerTypeCache(sessionId, appdefBoss);
    //            }
    //        }
    //        Boolean isAutoApproved = (Boolean) serverTypeCache.get(server.getResourceType().getName());
    //        return isAutoApproved != null && isAutoApproved;
    //    }
    //
    //    public static void populateAgentConnections(int sessionId,
    //                                               AppdefBoss appdefBoss,
    //                                               HttpServletRequest request,
    //                                               PlatformForm form,
    //                                               String usedIpPort)
    //        throws RemoteException,
    //               SessionTimeoutException,
    //               SessionNotFoundException {
    //
    //        PageList agents =
    //            appdefBoss.findAllAgents(sessionId,
    //                                     PageControl.PAGE_ALL);
    //        List uiAgents = new ArrayList();
    //        for (Iterator itr = agents.iterator();itr.hasNext();) {
    //            AgentValue agent = (AgentValue)itr.next();
    //            uiAgents.add(new AgentBean(agent.getAddress(),
    //                         new Integer(agent.getPort())));
    //
    //        }
    //
    //        form.setAgents(uiAgents);
    //        request.setAttribute(Constants.AGENTS_COUNT,
    //                             new Integer(uiAgents.size()));
    //        request.setAttribute("usedIpPort", usedIpPort);
    //    }
    //
    //    public static AgentValue getAgentConnection(int sessionId,
    //                                                AppdefBoss appdefBoss,
    //                                                HttpServletRequest request,
    //                                                PlatformForm form)
    //        throws RemoteException,
    //               SessionTimeoutException,
    //               SessionNotFoundException,
    //               AgentNotFoundException {
    //
    //        String agentIpPort = form.getAgentIpPort();
    //
    //        if (agentIpPort != null) {
    //            StringTokenizer st = new StringTokenizer(agentIpPort, ":");
    //            String ip = null;
    //            int port = -1;
    //            while(st.hasMoreTokens()) {
    //                ip = st.nextToken();
    //                port = Integer.parseInt(st.nextToken());
    //            }
    //
    //            AgentValue agentValue =
    //                appdefBoss.findAgentByIpAndPort(sessionId, ip, port);
    //
    //            return agentValue;
    //        }
    //        else {
    //            return null;
    //        }
    //    }
    //
    //    public static void startAutoScan(ServletContext ctx,
    //                                     int sessionId,
    //                                     AppdefEntityID entityId) {
    //        log.debug("startScan for platform=" + entityId);
    //
    //        try {
    //            AIBoss aiboss = ContextUtils.getAIBoss(ctx);
    //
    //            aiboss.startScan(sessionId, entityId.getID(),
    //                             new ScanConfigurationCore(),
    //                             null, null, null);
    //        } catch (Exception e) {
    //            log.error("Error starting scan: " + e.getMessage(), e);
    //        }
    //    }
}
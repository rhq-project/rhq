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
package org.rhq.enterprise.gui.action.navigation.hub;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.resource.hub.ResourceHubPortalAction;

/**
 * An <code>Action</code> that sets up the Resource Hub page when one of the lefthand navigation links (e.g. "JBoss
 * Servers" or "JMS Topics") is clicked. TODO (ips, 01/22/07): We can simplify this significantly for 2.0.
 */
public class NavigationHubPortalAction extends ResourceHubPortalAction {
    private static final Log LOG = LogFactory.getLog(NavigationHubPortalAction.class.getName());

    /*   protected PageList getNonGroupResources(ResourceHubForm hubForm, PageControl pc, int sessionId, AppdefBoss
     * appdefBoss, int entityType, int resourceType, String resourceName) throws Exception
     * {   String navigationNode = hubForm.getCategoryName();
     *
     * List viewableResourceTypes = getViewableResourceTypes(sessionId, appdefBoss, entityType, navigationNode);
     * sortResourceTypes(viewableResourceTypes, pc);
     *
     * PageList resources = new PageList();   if (viewableResourceTypes != null && !viewableResourceTypes.isEmpty())   {
     *      List viewableIds = new ArrayList();
     *
     *    // could be made more efficient if you could pass an array of      // AppdefResourceTypeValue's into
     * appdefBoss.findViewableEntityIds()      // rather than having to loop.      for (Object viewableResourceType :
     * viewableResourceTypes)      {         AppdefResourceTypeValue element =
     * (AppdefResourceTypeValue)viewableResourceType;
     * viewableIds.addAll(appdefBoss.findViewableEntityIds(sessionId, entityType,
     * element.getId().intValue(), PageControl.PAGE_ALL));      }
     *
     *    if (viewableIds.isEmpty())      {         LOG.trace("No viewable id's found for navigation node [" +
     * navigationNode + "].");      }      else      {         AppdefEntityID[] entityIds =
     * (AppdefEntityID[])viewableIds.toArray(new AppdefEntityID[0]);         resources = appdefBoss.findByIds(sessionId,
     * entityIds, pc);      }   }   else   {      LOG.trace("No viewable resource types found for navigation node [" +
     * navigationNode + "].");   }
     *
     * return resources; }
     *
     * private void sortResourceTypes(List viewableResourceTypes, PageControl pc) {   // set a sensible default   int
     * sortAttribute;   if (pc.getSortattribute() == SortAttribute.DEFAULT)   {      sortAttribute =
     * SortAttribute.RESTYPE_NAME;   }   else   {      sortAttribute = pc.getSortattribute();   }
     *
     * // lets first determine if the select sort attribute is one we know about   if (sortAttribute ==
     * SortAttribute.RESTYPE_NAME)   {      Comparator sorter = null;      if (pc.getSortorder() ==
     * PageControl.SORT_DESC)      {         sorter = new DescendingComparator();
     * Collections.sort(viewableResourceTypes, sorter);      }      else      {         // just use the natural ordering
     *         Collections.sort(viewableResourceTypes);      }   }
     *
     * }
     *
     * private List getViewableResourceTypes(int sessionId, AppdefBoss appdefBoss, int entityType, String navigationNode)
     * throws Exception {   PageList resourceTypes;   switch (entityType)   {      case
     * AppdefEntityConstants.APPDEF_TYPE_SERVER:         resourceTypes = appdefBoss.findViewableServerTypes(sessionId,
     * PageControl.PAGE_ALL);         break;      case AppdefEntityConstants.APPDEF_TYPE_SERVICE:         resourceTypes
     * = appdefBoss.findViewableServiceTypes(sessionId, PageControl.PAGE_ALL);         break;      default:
     * return null;   }   List resourceTypeNames = (List)NavigationResourceMapping
     * .getNavigationNodeToResourcesMapping().get(navigationNode);   if (resourceTypeNames == null)   {      throw new
     * RuntimeException("Invalid navigation selection, node [" + navigationNode + "] has not been configured.");   }
     * // check from the list of types we can see, which ones we are actually interested in.   List
     * viewableResourceTypes = new ArrayList();   for (Object resourceType : resourceTypes)   {
     * AppdefResourceTypeValue element = (AppdefResourceTypeValue)resourceType;      if
     * (resourceTypeNames.contains(element.getName()))      {         viewableResourceTypes.add(element);      }   }
     * return viewableResourceTypes; }
     *
     * protected void initResourceTypesPulldownMenu(HttpServletRequest request, ResourceHubForm hubForm, PageControl pc,
     * int sessionId, AppdefBoss appdefBoss, int entityType) throws Exception {   String navigationNode =
     * hubForm.getCategoryName();
     *
     * List types = getViewableResourceTypes(sessionId, appdefBoss, entityType, navigationNode);
     *
     * // if we're not able to view any of the types we're interested in, or there   // are just none deployed in this
     * install then lets make a best effort   // to return something useful in the selection options   if (types == null
     * || types.isEmpty())   {      super.initResourceTypesPulldownMenu(request, hubForm, pc, sessionId, appdefBoss,
     * entityType);   }   else   {      addTypeMenuItems(hubForm, types);
     * hubForm.addTypeFirst(buildResourceTypeMenuCategoryLabel(request, entityType));   }
     *
     * // now add the first entry, the resources we are trying to show,   // using a value from
     * ApplicationResource.properties   // e.g. resource.navigation.JBossEntityEJBs.filter
     * hubForm.addTypeFirst(createMenuLabel(request, hubForm.getCategoryName() + ".filter", ""));}*/

    @Override
    protected Portal createPortal() {
        Portal portal = Portal.createPortal("resource.hub.ResourceHubTitle", ".navigation.hub");
        return portal;
    }
}
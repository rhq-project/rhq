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
package org.rhq.enterprise.server.perspective;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;

@Local
public interface PerspectiveManagerLocal {
    /**
     * Return the core menu for the specified subject. Depending on their inventory and roles the
     * core menu for one subject1 could differ from that of subject2.
     * 
     * Subsequent calls will return the same core menu for the same Subject. In other words, it does
     * not change during a user session.
     * 
     * @param subject
     * @return
     */
    List<MenuItem> getMenu(Subject subject) throws PerspectiveException;

    /**
     * Returns the list of tabs that should be displayed for the specified user for the specified Resource.
     *
     * @param subject a user
     * @param resource an inventoried Resource
     * @return the list of tabs that should be displayed for the specified user for the specified Resource
     */
    List<Tab> getResourceTabs(Subject subject, Resource resource);

    /**
     * Return the url for the given page and name, if one is defined.
     *
     * @param subject a user
     * @param pageName, a valid page extension point
     * @param linkName, the link that should be replaced  
     * @param defaultValue, if no perspective link is defined for the pageName+linkName, the value to return 
     * @return the page link extenstion's url. defaultValue if not found
     */
    String getPageLink(Subject subject, String pageName, String linkName, String defaultValue);

    /**
     * Given a targetUrlKey parameter value, as set in the extension, resolve that key into the targetUrl
     * for the extension's content.
     * 
     * @param key, a valid key
     * @return the target url
     */
    public String getUrlViaKey(int key) throws PerspectiveException;

    // *************************************************************************************************
    // The following were previously the remote API.  The perspective API is not yet ready for release
    // *************************************************************************************************

    /**
     * Get the CoreUI context root. This can be used to assemble a url not otherwise obtainable via the API. 
     * This should be used with care as hardcoded paths may break in future releases of the core UI.
     *
     * @param subject
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return the Core GUI root url in the format "protocol://host:port/"
     */
    String getRootUrl(Subject subject, boolean makeExplicit, boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url.
     * @param subject
     * @param menuItemName The name of the menuItem extension point
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified extension point. May return null if the extension does not specify a url 
     * @throws IllegalArgumentException if the extension point does not exist. 
     */
    String getMenuItemUrl(Subject subject, String menuItemName, boolean makeExplicit, boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url. 
     * @param subject
     * @param tabName The name of the resource tab extension point
     * @param resourceId The resource id to be incorporated into the url. This method does not check the validity 
     * of the resourceId. 
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified extension point. May return null if the extension does not specify a url 
     * @throws IllegalArgumentException if the extension point does not exist.
     */
    String getResourceTabUrl(Subject subject, String tabName, int resourceId, boolean makeExplicit, boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url.
     *  
     * @param subject
     * @param target The target of the navigation link. for example, a role. 
     * @param targetId The id of the specified target. for example, a roleId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified target.   
     */
    String getTargetUrl(Subject subject, PerspectiveTarget target, int targetId, boolean makeExplicit,
        boolean makeSecure);

    /**
     * When requesting the same target url for several targets this is a more efficient call than calling
     * getTargetUrl() repeatedly. For example, if generating links to a list of resources.
     *  
     * This method does not ensure the specified subject can actually access the requested urls.
     *  
     * @param subject
     * @param target The target of the navigation link. for example, a role. 
     * @param targetId The id of the specified target. for example, a roleId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return A Map of targetId to url mappings.   
     */
    Map<Integer, String> getTargetUrls(Subject subject, PerspectiveTarget target, int[] targetIds,
        boolean makeExplicit, boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url.
     *  
     * @param subject
     * @param resourceId The resource id of the specified target. for example, the resource on which an alert is exists
     * @param target The target of the navigation link. for example, an alert. 
     * @param targetId The id of the specified target. for example, an alertId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified target.   
     */
    String getResourceTargetUrl(Subject subject, int resourceId, PerspectiveTarget target, int targetId,
        boolean makeExplicit, boolean makeSecure);

    /**
     * When requesting the same target url for several resource targets this is a more efficient call than calling
     * getResourceTargetUrl() repeatedly. For example, if generating links to a list of a resource's alerts.
     *  
     * Same This method does not ensure the specified subject can actually access the requested urls. 
     * 
     * @param subject
     * @param resourceId The resource id of the specified target. for example, the resource on which an alert is exists
     * @param target The target of the navigation link. for example, an alert. 
     * @param targetId The id of the specified target. for example, an alertId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return A Map of targetId to url mappings.   
     */
    Map<Integer, String> getResourceTargetUrls(Subject subject, int resourceId, PerspectiveTarget target,
        int[] targetIds, boolean makeExplicit, boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url. 
     * @param subject
     * @param resourceTypeId The resourceType id of the specified target. for example, the type for an alert template
     * @param target The target of the navigation link. for example, an alert template
     * @param targetId The id of the specified target. for example, an alert template definition Id
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified target.   
     */
    String getTemplateTargetUrl(Subject subject, int resourceId, PerspectiveTarget target, int targetId,
        boolean makeExplicit, boolean makeSecure);

}
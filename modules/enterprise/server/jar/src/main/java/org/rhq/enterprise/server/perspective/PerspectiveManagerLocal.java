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

    // ****************************************
    // The following shared with the Remote API
    // ****************************************

    /**
     * #see {@link PerspectiveManagerRemote#getRootUrl()}
     */
    String getRootUrl(Subject subject, boolean makeExplicit, boolean makeSecure);

    /**
     * #see {@link PerspectiveManagerRemote#getMenuUrl( Subject )}
     */
    String getMenuItemUrl(Subject subject, String menuItemName, boolean makeExplicit, boolean makeSecure);

    /**
     * #see {@link PerspectiveManagerRemote#getResourceUrl( Subject, String )}
     */
    String getResourceTabUrl(Subject subject, String tabName, int resourceId, boolean makeExplicit, boolean makeSecure);

    /**
     * #see {@link PerspectiveManagerRemote#getTargetUrl(Subject, PerspectiveTarget, int, boolean, boolean)}
     */
    String getTargetUrl(Subject subject, PerspectiveTarget target, int targetId, boolean makeExplicit,
        boolean makeSecure);

    /**
     * #see {@link PerspectiveManagerRemote#getTargetUrls(Subject, PerspectiveTarget, int[], boolean, boolean)}
     */
    Map<Integer, String> getTargetUrls(Subject subject, PerspectiveTarget target, int[] targetIds,
        boolean makeExplicit, boolean makeSecure);

    /**
     * #see {@link PerspectiveManagerRemote#getResourceTargetUrl(Subject, int, PerspectiveTarget, int, boolean, boolean)}
     */
    String getResourceTargetUrl(Subject subject, int resourceId, PerspectiveTarget target, int targetId,
        boolean makeExplicit, boolean makeSecure);

    /**
     * #see {@link PerspectiveManagerRemote#getResourceTargetUrls(Subject, int, PerspectiveTarget, int[], boolean, boolean)}
     */
    Map<Integer, String> getResourceTargetUrls(Subject subject, int resourceId, PerspectiveTarget target,
        int[] targetIds, boolean makeExplicit, boolean makeSecure);

    /**
     * #see {@link PerspectiveManagerRemote#getTemplateTargetUrl(Subject, int, PerspectiveTarget, int, boolean, boolean)}
     */
    String getTemplateTargetUrl(Subject subject, int resourceId, PerspectiveTarget target, int targetId,
        boolean makeExplicit, boolean makeSecure);

}
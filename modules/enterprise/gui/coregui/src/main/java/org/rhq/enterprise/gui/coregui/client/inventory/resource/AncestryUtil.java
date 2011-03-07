/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A utility class for working with Resource ancestry values.
 * 
 * @author Jay Shaughnessy
 */
public abstract class AncestryUtil {

    /**
     * Get the complete set of resource types in the ancestry of the provided resources. This is useful for
     * being able to load all the types in advance of generating decoded values.
     *  
     * @return
     */
    public static HashSet<Integer> getResourceTypeIds(Collection<Resource> resources) {
        HashSet<Integer> result = new HashSet<Integer>();

        for (Resource resource : resources) {
            String ancestry = resource.getAncestry();
            if (null == ancestry) {
                continue;
            }
            String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
            for (int i = 0; i < ancestryEntries.length; ++i) {
                String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
                int rtId = Integer.valueOf(entryTokens[0]);
                result.add(rtId);
            }
        }

        return result;
    }

    /**
     * Decode the provided ancestry into display values.
     * 
     * @param resourceId Used for unique locators in the LocatableHref links. 
     * @param ancestry The encoded ancestry for the resource
     * @param types if provided, must contain all of the resource types found in the ancestry.
     * @return Array of length 2. result[0] is the resource ancestry, complete with locatable Hrefs. result[1] is
     * the type ancestry, or null if types were not provided. 
     */
    public static String[] decodeAncestry(int resourceId, String ancestry, Map<Integer, ResourceType> types) {
        String[] result = new String[2];

        if (null == ancestry) {
            return result;
        }

        StringBuilder sbResources = new StringBuilder();
        StringBuilder sbTypes = (null != types) ? new StringBuilder() : null;
        String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
        for (int i = 0; i < ancestryEntries.length; ++i) {
            String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
            int ancestorTypeId = Integer.valueOf(entryTokens[0]);
            int ancestorResourceId = Integer.valueOf(entryTokens[1]);
            String ancestorName = entryTokens[2];

            sbResources.append((i > 0) ? " > " : "");
            String url = LinkManager.getResourceLink(ancestorResourceId);
            String suffix = resourceId + "_" + entryTokens[1];
            sbResources.append(SeleniumUtility.getLocatableHref(url, ancestorName, suffix));

            if (null != sbTypes) {
                sbTypes.append((i > 0) ? " > " : "");
                ResourceType rt = types.get(ancestorTypeId);
                sbTypes.append(rt.getName() + "[" + rt.getPlugin() + "]");
            }
        }

        result[0] = sbResources.toString();
        if (null != sbTypes) {
            result[1] = sbTypes.toString();
        }

        return result;
    }

    /**
     * Get a resource name that combines type and resource information. This is useful for when we want to
     * use a single column for dispay of the resource "name". The name is wrapped in a navigable link.
     * 
     * @param resource The resource
     *
     * @return the long name for the resource
     */
    public static String getResourceLongName(Resource resource) {
        StringBuilder sb = new StringBuilder();
        ResourceType type = resource.getResourceType();
        if (type != null) {
            if (type.getPlugin() != null) {
                sb.append(type.getPlugin());
                sb.append(" ");
            }

            sb.append(type.getName());
            sb.append(" ");
        }

        String url = LinkManager.getResourceLink(resource.getId());
        String suffix = String.valueOf(resource.getId());
        sb.append(SeleniumUtility.getLocatableHref(url, resource.getName(), suffix));

        return sb.toString();
    }

}

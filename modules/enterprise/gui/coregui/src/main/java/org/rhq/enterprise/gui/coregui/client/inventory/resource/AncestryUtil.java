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

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A utility class for working with Resource ancestry values.
 * 
 * @author Jay Shaughnessy
 */
public abstract class AncestryUtil {
    // ListGrid Record attribute names expected to be set on records processed by the utility  
    public static final String RESOURCE_ANCESTRY = "resourceAncestry";
    public static final String RESOURCE_ANCESTRY_VALUE = "resourceAncestryDecoded";
    public static final String RESOURCE_ANCESTRY_HOVER = "resourceAncestryHover";
    public static final String RESOURCE_ANCESTRY_TYPES = "resourceAncestryTypes";
    public static final String RESOURCE_HOVER = "resourceHover";
    public static final String RESOURCE_ID = "resourceId";
    public static final String RESOURCE_NAME = "resourceName";
    public static final String RESOURCE_TYPE_ID = "resourceTypeId";

    private static final String TITLE_ANCESTRY = CoreGUI.getMessages().util_ancestry_parentAncestry() + " ";
    private static final String TITLE_PLATFORM = CoreGUI.getMessages().common_title_platform() + ": ";

    /**
     * Convienence method that creates a standard ancestry ListGridField.
     * @return ancestry field
     */
    public static ListGridField setupAncestryListGridField() {
        ListGridField ancestryField;
        ancestryField = new ListGridField(AncestryUtil.RESOURCE_ANCESTRY, CoreGUI.getMessages().common_title_ancestry());
        ancestryField.setAlign(Alignment.LEFT);
        ancestryField.setCellAlign(Alignment.LEFT);
        setupAncestryListGridFieldCellFormatter(ancestryField);
        setupAncestryListGridFieldHover(ancestryField);
        return ancestryField;
    }

    /**
     * Convienence method that creates a standard ancestry ListGridField where the field already exists
     * in the given list grid.
     * 
     * @return ancestry field
     */
    public static ListGridField setupAncestryListGridField(ListGrid listGrid) {
        ListGridField ancestryField = listGrid.getField(AncestryUtil.RESOURCE_ANCESTRY);
        ancestryField.setAlign(Alignment.LEFT);
        ancestryField.setCellAlign(Alignment.LEFT);
        setupAncestryListGridFieldCellFormatter(ancestryField);
        setupAncestryListGridFieldHover(ancestryField);
        return ancestryField;
    }

    public static void setupAncestryListGridFieldHover(ListGridField ancestryField) {
        ancestryField.setShowHover(true);
        ancestryField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getAncestryHoverHTML(listGridRecord, 0);
            }
        });
    }

    public static void setupAncestryListGridFieldCellFormatter(ListGridField ancestryField) {
        ancestryField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return listGridRecord.getAttributeAsString(AncestryUtil.RESOURCE_ANCESTRY_VALUE);
            }
        });
    }

    /**
     * Get the complete set of resource types in the ancestries provided. This is useful for
     * being able to load all the types in advance of generating decoded values.
     *  
     * @return
     */
    public static HashSet<Integer> getAncestryTypeIds(Collection<String> ancestries) {
        HashSet<Integer> result = new HashSet<Integer>();

        for (String ancestry : ancestries) {
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
        StringBuilder sbTypes = (null != types) ? new StringBuilder("Parent Ancestry<hr/>") : null;
        String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
        for (int i = 0; i < ancestryEntries.length; ++i) {
            String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
            int ancestorTypeId = Integer.valueOf(entryTokens[0]);
            int ancestorResourceId = Integer.valueOf(entryTokens[1]);
            String ancestorName = entryTokens[2];

            sbResources.append((i > 0) ? " < " : "");
            //sbResources.append(" < ");
            String url = LinkManager.getResourceLink(ancestorResourceId);
            String suffix = resourceId + "_" + entryTokens[1];
            String href = SeleniumUtility.getLocatableHref(url, ancestorName, suffix);
            sbResources.append(href);

            if (null != sbTypes) {
                if (i > 0) {
                    sbTypes.append("<br/>");
                    for (int j = 0; j < i; ++j) {
                        sbTypes.append("&nbsp;&nbsp;");
                    }
                }
                ResourceType rt = types.get(ancestorTypeId);
                sbTypes.append(href);
                addFormattedType(sbTypes, rt);
            }
        }

        result[0] = sbResources.toString();
        if (null != sbTypes) {
            result[1] = sbTypes.toString();
        }

        return result;
    }

    public static String getFormattedType(ResourceType type) {
        return addFormattedType(new StringBuilder(), type).toString();
    }

    private static StringBuilder addFormattedType(StringBuilder sb, ResourceType type) {
        sb.append(" [<i>");
        sb.append(type.getPlugin());
        sb.append("</i>, ");
        sb.append(type.getName());
        sb.append("]");
        return sb;
    }

    public static String getAncestryValue(Record record) {
        return getAncestryValue(record, true);
    }

    public static String getAncestryValue(Record record, boolean generateLinks) {
        String ancestry = record.getAttributeAsString(RESOURCE_ANCESTRY);
        if (null == ancestry) {
            return "";
        }

        Integer resourceId = record.getAttributeAsInt(RESOURCE_ID);
        // if not set assume the standard "id" attr is a resourceId
        resourceId = (null != resourceId) ? resourceId : record.getAttributeAsInt("id");
        StringBuilder sbResources = new StringBuilder();
        String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
        for (int i = 0; i < ancestryEntries.length; ++i) {
            String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
            int ancestorResourceId = Integer.valueOf(entryTokens[1]);
            String ancestorName = entryTokens[2];

            sbResources.append((i > 0) ? " < " : "");
            if (generateLinks) {
                String url = LinkManager.getResourceLink(ancestorResourceId);
                String suffix = resourceId + "_" + entryTokens[1];
                String href = SeleniumUtility.getLocatableHref(url, ancestorName, suffix);
                sbResources.append(href);
            } else {
                sbResources.append(ancestorName);
            }
        }

        return sbResources.toString();
    }

    public static String getAncestryHoverHTML(ListGridRecord listGridRecord, int width) {
        String ancestryHover = listGridRecord.getAttributeAsString(RESOURCE_ANCESTRY_HOVER);
        if (null != ancestryHover) {
            return ancestryHover;
        }

        Integer resourceId = listGridRecord.getAttributeAsInt(RESOURCE_ID);
        // if not set assume the standard "id" attr is a resourceId
        resourceId = (null != resourceId) ? resourceId : listGridRecord.getAttributeAsInt("id");
        String resourceName = listGridRecord.getAttribute(RESOURCE_NAME);
        // if not set assume the standard "name" attr is a resourceName
        resourceName = (null != resourceName) ? resourceName : listGridRecord.getAttribute("name");
        Map<Integer, ResourceType> types = ((MapWrapper) listGridRecord.getAttributeAsObject(RESOURCE_ANCESTRY_TYPES))
            .getMap();
        Integer resourceTypeId = listGridRecord.getAttributeAsInt(RESOURCE_TYPE_ID);
        String ancestry = listGridRecord.getAttributeAsString(RESOURCE_ANCESTRY);

        String result = getAncestryHoverHTMLString(resourceId, resourceName, ancestry, resourceTypeId, types, width);

        listGridRecord.setAttribute(RESOURCE_ANCESTRY_HOVER, result);
        return result;
    }

    public static String getAncestryHoverHTMLForResource(Resource resource, Map<Integer, ResourceType> types, int width) {

        return getAncestryHoverHTMLString(resource.getId(), resource.getName(), resource.getAncestry(), resource
            .getResourceType().getId(), types, width);
    }

    private static String getAncestryHoverHTMLString(int resourceId, String resourceName, String ancestry,
        int resourceTypeId, Map<Integer, ResourceType> types, int width) {
        ResourceType type = types.get(resourceTypeId);
        String resourceLongName = getResourceLongName(resourceName, type);

        width = (width <= 0) ? 500 : width;

        // decode ancestry
        StringBuilder sb = new StringBuilder("<p style='width:");
        sb.append(width);
        sb.append("px'>");
        String title = (null != ancestry) ? TITLE_ANCESTRY : TITLE_PLATFORM;
        sb.append(title);
        sb.append(resourceLongName);
        if (null != ancestry) {
            sb.append("<hr/>");
            String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
            for (int i = ancestryEntries.length - 1, j = 0; i >= 0; --i, ++j) {
                String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
                int ancestorTypeId = Integer.valueOf(entryTokens[0]);
                String ancestorName = entryTokens[2];

                // indent with spaces
                if (j > 0) {
                    sb.append("<br/>");
                    for (int k = 0; k < j; ++k) {
                        sb.append("&nbsp;&nbsp;");
                    }
                }
                type = types.get(ancestorTypeId);
                sb.append(getResourceLongName(ancestorName, type));
            }

            // add target resource, indent with spaces
            sb.append("<br/>");
            for (int k = 0; k <= ancestryEntries.length; ++k) {
                sb.append("&nbsp;&nbsp;");
            }
            sb.append(resourceLongName);
        }

        sb.append("</p>");

        return sb.toString();
    }

    private static String getResourceLongName(String resourceName, ResourceType type) {
        StringBuilder sb = new StringBuilder("<b>");

        sb.append(resourceName);
        sb.append("</b>");

        if (type != null) {
            addFormattedType(sb, type);
        }

        return sb.toString();
    }

    /**
     * Get a resource name that combines type and resource information. This is useful for when we want to
     * use a single column for dispay of the resource "name". The name is wrapped in a navigable link.
     * 
     * @param resource The resource
     *
     * @return the long name for the resource
     */
    public static String getResourceHoverHTML(Record record, int width) {
        String resourceHover = record.getAttributeAsString(RESOURCE_HOVER);
        if (null != resourceHover) {
            return resourceHover;
        }

        String resourceName = record.getAttribute(RESOURCE_NAME);
        // if not set assume the standard "name" attr is a resourceName
        resourceName = (null != resourceName) ? resourceName : record.getAttribute("name");
        Map<Integer, ResourceType> types = ((MapWrapper) record.getAttributeAsObject(RESOURCE_ANCESTRY_TYPES)).getMap();
        Integer resourceTypeId = record.getAttributeAsInt(RESOURCE_TYPE_ID);
        ResourceType type = types.get(resourceTypeId);
        width = (width <= 0) ? 500 : width;

        StringBuilder sb = new StringBuilder("<p style='width:");
        sb.append(width);
        sb.append("px'>");

        sb.append(getResourceLongName(resourceName, type));

        sb.append("</p>");

        String result = sb.toString();
        record.setAttribute(RESOURCE_HOVER, result);
        return result;
    }

    // We do not want smargwt to see we are storing our map into an attribute because it barfs on our key/value pairs
    // so instead we have to wrap it in a non-Map POJO Object so smartgwt just handles it as a java.lang.Object.
    public static class MapWrapper {
        private Map<Integer, ResourceType> map;

        public MapWrapper(Map<Integer, ResourceType> map) {
            this.map = map;
        }

        public Map<Integer, ResourceType> getMap() {
            return this.map;
        }
    }

}

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.resource.group.composite;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class ResourceGroupComposite implements Serializable {

    public enum GroupAvailabilityType {
        EMPTY, UP, DOWN, WARN, DISABLED
    };

    private static final long serialVersionUID = 1L;

    ////JAXB Needs no args constructor and final fields make that difficult. 

    private ResourceGroup resourceGroup;

    private GroupCategory category;
    private long implicitCount;
    private long implicitDown;
    private long implicitUnknown;
    private long implicitDisabled;
    private long explicitCount;
    private long explicitDown;
    private long explicitUnknown;
    private long explicitDisabled;

    private ResourceFacets resourceFacets;

    @XmlTransient
    private ResourcePermission resourcePermission;

    //def no args constructor for JAXB
    public ResourceGroupComposite() {
    }

    // Constructor used in Hibernate Query, see ResourceGroupManagerBean
    public ResourceGroupComposite(Long explicitCount, Long explicitDown, Long explicitUnknown, Long explicitDisabled,
        Long implicitCount, Long implicitDown, Long implicitUnknown, Long implicitDisabled, ResourceGroup resourceGroup) {

        this(explicitCount, explicitDown, explicitUnknown, explicitDisabled, implicitCount, implicitDown,
            implicitUnknown, implicitDisabled,
            resourceGroup, null, new ResourcePermission());
    }

    // Constructor used in Hibernate Query, see ResourceGroupManagerBean 
    public ResourceGroupComposite(Long explicitCount, Long explicitDown, Long explicitUnknown, Long explicitDisabled,
        Long implicitCount, Long implicitDown, Long implicitUnknown, Long implicitDisabled,
        ResourceGroup resourceGroup, Number measure, Number inventory,
        Number control, Number alert, Number event, Number configureRead, Number configureWrite, Number content,
        Number createChildResources, Number deleteResources, Number drift) {

        this(explicitCount, explicitDown, explicitUnknown, explicitDisabled, implicitCount, implicitDown,
            implicitUnknown, implicitDisabled,
            resourceGroup, null, new ResourcePermission(measure.intValue() > 0, inventory.intValue() > 0,
                control.intValue() > 0, alert.intValue() > 0, event.intValue() > 0, configureRead.intValue() > 0,
                configureWrite.intValue() > 0, content.intValue() > 0, createChildResources.intValue() > 0,
                deleteResources.intValue() > 0, drift.intValue() > 0));
    }

    public ResourceGroupComposite(Long explicitCount, Long explicitDown, Long explicitUnknown, Long explicitDisabled,
        Long implicitCount, Long implicitDown, Long implicitUnknown, Long implicitDisabled,
        ResourceGroup resourceGroup, ResourceFacets facets) {

        this(explicitCount, explicitDown, explicitUnknown, explicitDisabled, implicitCount, implicitDown,
            implicitUnknown, implicitDisabled,
            resourceGroup, facets, new ResourcePermission());
    }

    // Private constructor that all public constructors delegate to
    public ResourceGroupComposite(Long explicitCount, Long explicitDown, Long explicitUnknown, Long explicitDisabled,
        Long implicitCount, Long implicitDown, Long implicitUnknown, Long implicitDisabled,
        ResourceGroup resourceGroup, ResourceFacets facets,
        ResourcePermission permissions) {

        this.implicitCount = implicitCount;
        this.implicitDown = implicitDown;
        this.implicitUnknown = implicitUnknown;
        this.implicitDisabled = implicitDisabled;

        this.explicitCount = explicitCount;
        this.explicitDown = explicitDown;
        this.explicitUnknown = explicitUnknown;
        this.explicitDisabled = explicitDisabled;

        this.resourceGroup = resourceGroup;

        if (this.resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE) {
            this.category = GroupCategory.COMPATIBLE;
        } else if (this.resourceGroup.getGroupCategory() == GroupCategory.MIXED) {
            this.category = GroupCategory.MIXED;
        } else {
            throw new IllegalArgumentException("Unknown category [" + this.resourceGroup.getGroupCategory()
                + "] for ResourceGroup [" + this.resourceGroup.getName() + "]");
        }

        this.resourceFacets = facets;
        this.resourcePermission = permissions;
    }

    public long getImplicitCount() {
        return implicitCount;
    }

    public long getImplicitDown() {
        return implicitDown;
    }

    public long getImplicitUnknown() {
        return implicitUnknown;
    }

    public long getImplicitDisabled() {
        return implicitDisabled;
    }

    public long getImplicitUp() {
        return implicitCount - implicitDown - implicitDisabled - implicitUnknown;
    }

    public long getExplicitCount() {
        return explicitCount;
    }

    public long getExplicitDown() {
        return explicitDown;
    }

    public long getExplicitUnknown() {
        return explicitUnknown;
    }

    public long getExplicitDisabled() {
        return explicitDisabled;
    }

    public long getExplicitUp() {
        return explicitCount - explicitDown - explicitDisabled - explicitUnknown;
    }

    public ResourceGroup getResourceGroup() {
        return this.resourceGroup;
    }

    public GroupCategory getCategory() {
        return this.category;
    }

    /**
     * Returns the explicit group availability determined with the following algorithm, evaluated top to bottom:
     * <pre>
     * empty group  = EMPTY
     * allDown      = DOWN
     * someDown/someUnknown = WARN
     * someDisabled = DISABLED
     * otherwise    = UP (all members UP)
     * </pre>  
     *   
     * @return the group availability type, null for an empty group
     */
    public GroupAvailabilityType getExplicitAvailabilityType() {
        return getAvailabilityType(true);
    }

    /**
     * Returns the implicit group availability determined with the following algorithm, evaluated top to bottom:
     * <pre>
     * empty group  = EMPTY
     * allDown      = DOWN
     * someDown/someUnknown = WARN
     * someDisabled = DISABLED
     * otherwise    = UP (all members UP)
     * </pre>  
     *   
     * @return the group availability type, null for an empty group
     */
    public GroupAvailabilityType getImplicitAvailabilityType() {
        return getAvailabilityType(false);
    }

    private GroupAvailabilityType getAvailabilityType(boolean isExplicit) {
        long count = isExplicit ? explicitCount : implicitCount;
        long down = isExplicit ? explicitDown : implicitDown;
        long disabled = isExplicit ? explicitDisabled : implicitDisabled;
        long unknown = isExplicit ? explicitUnknown : implicitUnknown;

        if (0 == count) {
            return GroupAvailabilityType.EMPTY;
        }

        if (down == count) {
            return GroupAvailabilityType.DOWN;
        }

        if (down > 0 || unknown > 0) {
            return GroupAvailabilityType.WARN;
        }

        if (disabled > 0) {
            return GroupAvailabilityType.DISABLED;
        }

        return GroupAvailabilityType.UP;
    }

    @XmlTransient
    public void setResourceFacets(ResourceFacets facets) {
        this.resourceFacets = facets;
    }

    public ResourceFacets getResourceFacets() {
        return resourceFacets;
    }

    public ResourcePermission getResourcePermission() {
        return resourcePermission;
    }

    public void setResourcePermission(ResourcePermission resourcePermission) {
        this.resourcePermission = resourcePermission;
    }

    /**
     * Returns a query string snippet that can be passed to group URLs that reference this specific group.
     * Note that the returned string does not include the "?" itself.
     * 
     * @return query string snippet that can appear after the "?" in group URLs.
     */
    public String getGroupQueryString() {
        return "groupId=" + getResourceGroup().getId();
    }

    // remove once the old UI is killed, for now this is still needed
    @Deprecated
    public Double getExplicitAvail() {
        return 0 == explicitCount ? null : (1.0 - (explicitDown / explicitCount));
    }

    // remove once the old UI is killed, for now this is still needed
    @Deprecated
    public String getExplicitFormatted() {
        return getAlignedAvailabilityResults(getExplicitUp() + getExplicitUnknown() + getExplicitDisabled(),
            getExplicitDown());
    }

    // remove once the old UI is killed, for now this is still needed
    @Deprecated
    public String getImplicitFormatted() {
        return getAlignedAvailabilityResults(getImplicitUp() + getImplicitUnknown() + getImplicitDisabled(),
            getImplicitDown());
    }

    // remove once the old UI is killed, for now this is still needed
    @Deprecated
    private String getAlignedAvailabilityResults(long up, long notUp) {
        StringBuilder results = new StringBuilder();
        results.append("<table width=\"120px\"><tr>");
        if (up == 0 && notUp == 0) {
            results.append(getColumn(false, "<img src=\""
                + "/coregui/images/subsystems/availability/availability_grey_16.png" + "\" /> 0"));
            results.append(getColumn(true));
            results.append(getColumn(false));
        } else {
            if (up > 0) {
                results.append(getColumn(false, " <img src=\""
                    + "/coregui/images/subsystems/availability/availability_green_16.png" + "\" />", up));
            }

            if (up > 0 && notUp > 0) {
                results.append(getColumn(true)); // , " / ")); // use a vertical separator image if we want a separator
            }

            if (notUp > 0) {
                results.append(getColumn(false, " <img src=\""
                    + "/coregui/images/subsystems/availability/availability_red_16.png" + "\" />", notUp));
            } else {
                results.append(getColumn(false,
                    "&nbsp;&nbsp;<img src=\"/coregui/images/blank.png\" width=\"16px\" height=\"16px\" />"));
            }
        }
        results.append("</tr></table>");
        return results.toString();
    }

    // remove once the old UI is killed, for now this is still needed
    @Deprecated
    private String getColumn(boolean isSpacerColumn, Object... data) {
        StringBuilder results = new StringBuilder();
        if (isSpacerColumn) {
            results.append("<td nowrap=\"nowrap\" style=\"white-space:nowrap;\" width=\"10px\" align=\"left\" >");
        } else {
            results.append("<td nowrap=\"nowrap\" style=\"white-space:nowrap;\" width=\"55px\" align=\"left\" >");
        }
        if (data == null) {
            results.append("&nbsp;");
        } else {
            for (Object datum : data) {
                results.append(datum == null ? "&nbsp;" : datum);
            }
        }
        results.append("</td>");
        return results.toString();
    }

    @Override
    public String toString() {
        return "ResourceGroupComposite[name="
            + this.resourceGroup.getName() //
            + ", implicit[count/down/disabled/unknown=," + this.implicitCount + "/" + this.implicitDown + "/"
            + this.implicitDisabled + "/" + this.implicitUnknown + "]" + ", explicit[count/down/disabled/unknown=,"
            + this.explicitCount + "/" + this.explicitDown + "/" + this.explicitDisabled + "/" + this.explicitUnknown
            + "]" + ", facets="
            + ((this.resourceFacets == null) ? "none" : this.resourceFacets.getFacets()) + "]";
    }
}
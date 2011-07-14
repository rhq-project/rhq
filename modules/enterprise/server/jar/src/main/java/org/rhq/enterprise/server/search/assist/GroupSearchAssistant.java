/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.search.assist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.search.SearchSubsystem;

/**
 * @author Joseph Marques
 */
public class GroupSearchAssistant extends TabAwareSearchAssistant {

    private static final List<String> parameterizedContexts;
    private static final List<String> simpleContexts;

    static {
        parameterizedContexts = Collections.emptyList();
        simpleContexts = Collections.unmodifiableList(Arrays.asList("availability", "groupCategory", "category",
            "type", "plugin", "name"));
    }

    public GroupSearchAssistant(Subject subject, String tab) {
        super(subject, tab);
    }

    public SearchSubsystem getSearchSubsystem() {
        return SearchSubsystem.GROUP;
    }

    @Override
    public String getPrimarySimpleContext() {
        return "name";
    }

    @Override
    public List<String> getSimpleContexts() {
        return simpleContexts;
    }

    @Override
    public List<String> getParameterizedContexts() {
        return parameterizedContexts;
    }

    @Override
    public List<String> getParameters(String context, String filter) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getValues(String context, String param, String filter) {
        filter = stripQuotes(filter);
        if (context.equals("availability")) {
            return filter(AvailabilityType.class, filter);

        } else if (context.equals("groupCategory")) {
            return filter(GroupCategory.class, filter);

        } else if (context.equals("category")) {
            return filter(ResourceCategory.class, filter);

        } else if (context.equals("type")) {
            return execute("" //
                + "SELECT DISTINCT type.name " //
                + "  FROM ResourceType type, ResourceGroup rg " //
                + " WHERE rg.resourceType = type " // only suggest names that exist for visible groups in inventory
                + "   AND rg.visible = true AND type.deleted = false" //
                + conditionallyAddJPQLString("type.name", filter) //
                + conditionallyAddJPQLString("rg.groupCategory", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
                + " ORDER BY type.name ");

        } else if (context.equals("plugin")) {
            return execute("" //
                + "SELECT DISTINCT type.plugin " //
                + "  FROM ResourceType type, ResourceGroup rg " //
                + " WHERE rg.resourceType = type " // only suggest names that exist for visible groups in inventory
                + "   AND rg.visible = true AND type.deleted = false" //
                + conditionallyAddJPQLString("type.plugin", filter) //
                + conditionallyAddJPQLString("rg.groupCategory", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
                + " ORDER BY type.plugin ");

        } else if (context.equals("name")) {
            return execute("" //
                + "SELECT DISTINCT rg.name " //
                + "  FROM ResourceGroup rg " //
                + " WHERE rg.visible = true " // only suggest names that exist for visible groups in inventory
                + conditionallyAddJPQLString("rg.name", filter) //
                + conditionallyAddJPQLString("rg.groupCategory", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
                + " ORDER BY rg.name ");

        } else {
            return Collections.emptyList();

        }
    }

    private String getAuthzFragment() {
        return "rg.id IN " //
            + "(SELECT igroup.id " //
            + "   FROM ResourceGroup igroup " //
            + "   JOIN igroup.roles irole " //
            + "   JOIN irole.subjects isubject " //
            + "  WHERE isubject.id = " + getSubjectId() + ")";
    }

}

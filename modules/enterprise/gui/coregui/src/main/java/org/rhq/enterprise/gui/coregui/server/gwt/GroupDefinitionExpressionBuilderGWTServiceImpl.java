/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.GroupDefinitionExpressionBuilderGWTService;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionExpressionBuilderManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Mazzitelli
 */
public class GroupDefinitionExpressionBuilderGWTServiceImpl extends AbstractGWTServiceImpl implements
    GroupDefinitionExpressionBuilderGWTService {

    private static final long serialVersionUID = 1L;

    private GroupDefinitionExpressionBuilderManagerLocal builder = LookupUtil
        .getGroupDefinitionExpressionBuilderManager();

    @Override
    public ArrayList<String> getPluginConfigurationPropertyNames(int resourceTypeId) {
        try {
            return new ArrayList<String>(builder.getPluginConfigurationPropertyNames(resourceTypeId));
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> getResourceConfigurationPropertyNames(int resourceTypeId) {
        try {
            return new ArrayList<String>(builder.getResourceConfigurationPropertyNames(resourceTypeId));
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> getTraitPropertyNames(int resourceTypeId) {
        try {
            return new ArrayList<String>(builder.getTraitPropertyNames(resourceTypeId));
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}

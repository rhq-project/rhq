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
package org.rhq.plugins.sudoers;

import java.util.List;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;

/**
 * @author Partha Aji 
 */
public class SudoersComponent extends AugeasConfigurationComponent {
    @Override
    protected Object toPropertyValue(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if ("tag".equals(node.getName())) {
            return !"NOPASSWD".equals(augeas.get(node.getPath()));
        }
        return super.toPropertyValue(propDefSimple, augeas, node);
    }

    @Override
    protected String toNodeValue(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        if ("tag".equals(node.getName())) {
            if (propSimple.getBooleanValue()) {
                return "PASSWD";
            }
            return "NOPASSWD";
        }

        return super.toNodeValue(augeas, node, propDefSimple, propSimple);
    }

    protected AugeasNode getExistingChildNodeForListMemberPropertyMap(AugeasNode parentNode,
        PropertyDefinitionList propDefList, PropertyMap propMap) {
        // First find all child nodes with the same 'spec' value as the PropertyMap.
        Augeas augeas = getAugeas();

        String userName = propMap.getSimple("user").getStringValue();
        String specFilter = parentNode.getPath() + "/spec[*]";
        List<String> userPaths = augeas.match(String.format(specFilter + "/user[.='%s']", userName));
        if (userPaths == null || userPaths.isEmpty()) {
            return null;
        }
        AugeasNode node = new AugeasNode(userPaths.get(0));
        return node.getParent();
    }
}

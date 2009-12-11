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

package org.rhq.plugins.postfix;

import java.util.List;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;
import org.rhq.plugins.augeas.helper.AugeasUtility;

/**
 * @author paji
 *
 */
public class PostfixAccessComponent extends AugeasConfigurationComponent {
    @Override
    protected AugeasNode getNewListMemberNode(AugeasNode listNode, PropertyDefinitionMap listMemberPropDefMap,
        int listIndex) {
        AugeasNode node = new AugeasNode(listNode, "0" + listIndex);
        return node;
    }

    protected AugeasNode getExistingChildNodeForListMemberPropertyMap(AugeasNode parentNode,
        PropertyDefinitionList propDefList, PropertyMap propMap) {
        // First find all child nodes with the same 'pattern' value as the PropertyMap.
        Augeas augeas = getAugeas();

        String nameFilter = parentNode.getPath() + "/*/pattern";
        String canonical = propMap.getSimple("pattern").getStringValue();
        List<String> namePaths = AugeasUtility.matchFilter(augeas, nameFilter, canonical);

        if (namePaths == null || namePaths.isEmpty()) {
            return null;
        }

        AugeasNode node = new AugeasNode(namePaths.get(0));
        return node.getParent();
    }
}

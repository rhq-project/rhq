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
package org.rhq.enterprise.gui.operation.definition.group;

import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;

public class ResourceGroupOperationDefinitionUtils {
    public static List<SelectItem> getResourceExecutionOptions() {
        List<SelectItem> results = new ArrayList<SelectItem>();

        results.add(new SelectItem(ResourceGroupExecutionType.concurrent.name(), ResourceGroupExecutionType.concurrent
            .getDisplayText()));
        results.add(new SelectItem(ResourceGroupExecutionType.ordered.name(), ResourceGroupExecutionType.ordered
            .getDisplayText()));

        return results;
    }

    public static String getDefaultExecutionOption() {
        return ResourceGroupExecutionType.concurrent.name(); // concurrent, by default
    }

    public static String getExecutionOption(boolean isConcurrent) {
        if (isConcurrent) {
            return ResourceGroupExecutionType.concurrent.name();
        } else {
            return ResourceGroupExecutionType.ordered.name();
        }
    }
}
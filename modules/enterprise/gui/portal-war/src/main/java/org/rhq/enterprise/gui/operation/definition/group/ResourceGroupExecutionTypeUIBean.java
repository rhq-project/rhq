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

import javax.faces.model.SelectItem;

public class ResourceGroupExecutionTypeUIBean {
    public static enum Type {
        CONCURRENT("At the same time for all resources"), //
        ORDERED("In order");

        private String displayName;

        private Type(String displayName) {
            this.displayName = displayName;
        }

        /**
         * A Java bean style getter to allow us to access the enum name from JSP 
         * or Facelets pages (e.g. #{ResourceGroupExecutionTypeUIBean.Type.concurrent}).
         */
        public String getName() {
            return name();
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public SelectItem getConcurrentOption() {
        return new SelectItem(ResourceGroupExecutionTypeUIBean.Type.CONCURRENT.name(),
            ResourceGroupExecutionTypeUIBean.Type.CONCURRENT.getDisplayName());
    }

    public SelectItem getOrderedOption() {
        return new SelectItem(ResourceGroupExecutionTypeUIBean.Type.ORDERED.name(),
            ResourceGroupExecutionTypeUIBean.Type.ORDERED.getDisplayName());
    }

}
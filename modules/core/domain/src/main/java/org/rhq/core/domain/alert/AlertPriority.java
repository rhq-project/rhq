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
package org.rhq.core.domain.alert;

public enum AlertPriority {
    LOW("! - Low"), MEDIUM("!! - Medium"), HIGH("!!! - High");

    private String displayName;

    private AlertPriority(String displayName) {
        this.displayName = displayName;
    }

    public static AlertPriority getByLegacyIndex(int index) {
        AlertPriority[] priorities = AlertPriority.values();
        if ((index > 0) && (index <= priorities.length)) {
            return priorities[index - 1];
        }

        /*
         * this is a special case, signifying to callers they want to search for alerts of ANY priority
         */
        return null;
    }

    /**
     * A Java bean style getter to allow us to access the enum name from JSP or Facelets pages (e.g.
     * ${alert.alertDefinition.priority.name}).
     *
     * @return the enum name
     */
    public String getName() {
        return name();
    }

    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
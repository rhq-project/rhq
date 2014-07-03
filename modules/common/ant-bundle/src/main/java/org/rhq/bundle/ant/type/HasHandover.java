/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.bundle.ant.type;

/**
 * Interface which ANT data types classes supporting content handover to the bundle target resource should implement. It
 * lets us deal with incompatible ANT data types (like children of {@link org.rhq.bundle.ant.type.AbstractUrlFileType}
 * and of {@link org.rhq.bundle.ant.type.AbstractFileType}) with the same piece of code.
 *
 * @author Thomas Segismont
 * @see org.rhq.bundle.ant.type.AbstractFileType
 * @see org.rhq.bundle.ant.type.AbstractUrlFileType
 */
public interface HasHandover {
    /**
     * Will be invoked by ANT when parsing the recipe.
     */
    void addConfigured(Handover handover);

    /**
     * @return the {@link org.rhq.bundle.ant.type.Handover} data type object
     */
    Handover getHandover();
}

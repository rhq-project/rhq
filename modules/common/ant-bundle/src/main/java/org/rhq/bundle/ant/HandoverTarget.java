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

package org.rhq.bundle.ant;

import org.rhq.bundle.ant.type.HandoverInfo;

/**
 * A class implementing this interface is able to get notified by ANT that a recipe element requires the participation
 * of the bundle target resource component.
 *
 * @author Thomas Segismont
 * @see org.rhq.bundle.ant.BundleAntProject#setHandoverTarget(HandoverTarget)
 */
public interface HandoverTarget {
    /**
     * @param handoverInfo an object wrapping the details of the content being handed over
     * @return true if the processing went well, false otherwise
     */
    boolean handoverContent(HandoverInfo handoverInfo);
}

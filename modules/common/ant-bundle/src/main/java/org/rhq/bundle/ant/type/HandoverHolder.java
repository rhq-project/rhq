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

import org.apache.tools.ant.BuildException;

/**
 * Convenience class to avoid code duplication. Implementors of thje {@link org.rhq.bundle.ant.type.HasHandover}
 * interface may hold a field of this type and delegate the interface operation to it.
 *
 * @author Thomas Segismont
 */
public class HandoverHolder implements HasHandover {
    private Handover handover;

    @Override
    public Handover getHandover() {
        return handover;
    }

    @Override
    public void addConfigured(Handover handover) {
        if (this.handover != null) {
            throw new BuildException("More than one 'handover' child node declared");
        }
        this.handover = handover;
    }
}

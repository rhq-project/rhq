/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.domain.drift;

public enum DriftComplianceStatus {
    IN_COMPLIANCE,

    OUT_OF_COMPLIANCE_NO_BASEDIR,

    OUT_OF_COMPLIANCE_DRIFT;

    public static DriftComplianceStatus fromCode(int code) {
        switch (code) {
            case 0: return IN_COMPLIANCE;
            case 1: return OUT_OF_COMPLIANCE_NO_BASEDIR;
            case 2: return OUT_OF_COMPLIANCE_DRIFT;
            default: throw new IllegalArgumentException(code + " is not a DriftComplianceStatus code");
        }
    }
}

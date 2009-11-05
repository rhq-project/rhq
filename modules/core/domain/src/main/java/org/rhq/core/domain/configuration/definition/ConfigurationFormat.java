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

package org.rhq.core.domain.configuration.definition;

/**
 * Specifies the type of configuration that is supported in a
 * {@link org.rhq.core.domain.configuration.definition.ConfigurationDefinition}. This currently applies only to resource
 * configuration.
 */
public enum ConfigurationFormat {

    STRUCTURED("Structured"), RAW("Raw"), STRUCTURED_AND_RAW("Structured and Raw");

    private String displayName;

    ConfigurationFormat(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return displayName;
    }

    public boolean isRawSupported() {
        return this.equals(RAW) || this.equals(STRUCTURED_AND_RAW);
    }

    public boolean isStructuredSupported() {
        return this.equals(STRUCTURED) || this.equals(STRUCTURED_AND_RAW);
    }

}

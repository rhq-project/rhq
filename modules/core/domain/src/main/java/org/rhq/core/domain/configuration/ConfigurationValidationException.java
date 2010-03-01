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
package org.rhq.core.domain.configuration;

import java.util.Collections;
import java.util.Map;

public class ConfigurationValidationException extends RuntimeException {
    private static final long serialVersionUID = -6924768743906659205L;

    /**
     * For Raw config, the key is the path to the raw config file
     * For structured, the key is simply "structured"
     * In the future, the key will be the same as the value returned from
     * org.rhq.core.domain.configuration.Configuration.get(string)
     */
    public final Map<String, String> errors;

    public ConfigurationValidationException(String reason, Map<String, String> errors) {
        super(reason);
        this.errors = Collections.unmodifiableMap(errors);
    }
}
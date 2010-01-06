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

package org.rhq.core.pc.configuration;

import java.util.LinkedList;
import java.util.List;

public class RawUpdateException extends ConfigurationUpdateException {

    private List<RawUpdateErrorDetail> details = new LinkedList<RawUpdateErrorDetail>();

    public RawUpdateException(List<RawUpdateErrorDetail> errors) {
        details = errors;
    }

    public RawUpdateException(String message, List<RawUpdateErrorDetail> errors) {
        super(message);
        details = errors;
    }

    public RawUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public RawUpdateException(Throwable cause) {
        super(cause);
    }

    public List<RawUpdateErrorDetail> getDetails() {
        return details;
    }
}

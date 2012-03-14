/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest.reporting;

import org.rhq.enterprise.server.rest.AbstractRestBean;

/**
 * Rest Bean with common reporting functionality like CSV generation.
 * @author Mike Thompson
 */
public abstract class AbstractReportingRestBean extends AbstractRestBean{

    /**
     * Define the text/csv media type as it is not a JAX-RS standard media type.
     */
    public static final String MEDIA_TYPE_TEXT_CSV = "text/csv";

    // Common CSV stuff here


}

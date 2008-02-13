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
package org.rhq.core.domain.measurement;

/**
 * Data displayed in the summary view will appear in the main measurement display page. Data displayed in the detail
 * view will only appear in the UI when examining the entire set of metric data. The location in the UI where the
 * measurement data is shown is typically in a different area from where the trait data is shown.
 *
 * @author John Mazzitelli
 */
public enum DisplayType {
    SUMMARY, DETAIL
}
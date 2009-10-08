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
package org.rhq.core.domain.measurement;

/**
 * Data displayed in the summary view will appear in the main measurement display page. Data displayed in the detail
 * view will only appear in the UI when examining the entire set of metric data. The location in the UI where the
 * measurement data is shown is typically in a different area from where the trait data is shown.
 * Note that metric collection is by default enabled for metrics with DisplayType.SUMMARY.
 *
 * @author John Mazzitelli
 */
public enum DisplayType {
    SUMMARY, DETAIL
}
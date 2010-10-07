/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.helpers.perftest.support.reporting;

import org.rhq.helpers.perftest.support.testng.PerformanceReporting;
import org.testng.ITestResult;

import java.util.Map;

/**
 * Define a reporter, that can be used to export
 * performance test results with.
 *
 * @author Heiko W. Rupp
 */
public interface PerformanceReportExporter {

    public void setBaseFile(String fileName);

    public void setRolling(PerformanceReporting.Rolling rolling);

    public void export(Map<String,Long> timings, ITestResult result);
}

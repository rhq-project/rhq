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
package org.rhq.helpers.perftest.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.rhq.helpers.perftest.support.reporting.ExcelExporter;
import org.rhq.helpers.perftest.support.reporting.PerformanceReportExporter;
import org.rhq.helpers.perftest.support.testng.PerformanceReporting;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.annotations.Test;

/**
 * Test basic functionality of the ExcelExporter
 *
 * @author Heiko W. Rupp
 * @see org.rhq.helpers.perftest.support.reporting.ExcelExporter
 */
@Test(groups = "PERF")
public class ExcelExporterTest {

    /**
     * Test writing a workbook twice.
     * Should contain one Overview tab and one 'test' tab
     * with two times the same result
     * @throws Exception If anything goes wrong
     */
    public void testRewriteSheets() throws Exception {

        /*
         * Run a dummy test.
         */
        TestNG testNG = new TestNG();
        testNG.setTestClasses(new Class[]{DummyTest.class});
        TestListenerAdapter adapter = new TestListenerAdapter();
        testNG.addListener(adapter);
        testNG.run();
        // RHQ additional timing data
        Map<String, Long> timings = new HashMap<String, Long>();
        timings.put("test", 123L);


        /*
         * Set up the reporter
         */
        PerformanceReportExporter rep = new ExcelExporter();
        rep.setBaseFile("test1");
        rep.setRolling(PerformanceReporting.Rolling.NONE);


        /*
         * Write to .xls file twice
         */
        rep.export(timings,adapter.getPassedTests().iterator().next());
        rep.export(timings,adapter.getPassedTests().iterator().next());

        /*
         * Now check the workbook written
         * But first delete an existing file
         */
        File file = new File("test1.xls");

        FileInputStream fis = new FileInputStream(file);
        Workbook wb = new HSSFWorkbook(fis);

        assert wb.getNumberOfSheets() ==2 : "Workbook does not have 2 sheets";

        Sheet overview = wb.getSheetAt(0);
        assert overview.getSheetName().equals("Overview") : "Sheet 0 is not the Overview";
        assert wb.getSheetAt(1).getSheetName().equals("DummyTest.one") : "Sheet 1 is not DummyTest.one";

        // 0 based as opposed to xls where it rows are 1 based.
        assert overview.getLastRowNum() == 2 :"Last row of overview is not 4, but " + overview.getLastRowNum();

        fis.close();

        if (file.exists()) {
            assert file.delete();
        }
    }

}

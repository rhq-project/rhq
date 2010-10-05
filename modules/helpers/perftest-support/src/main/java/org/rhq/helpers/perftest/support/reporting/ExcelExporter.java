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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.testng.ITestResult;

import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Export data to an excel sheet
 *
 * @author Heiko W. Rupp
 */
public class ExcelExporter implements PerformanceReportExporter {

    private static final String DOT_XLS = ".xls";
    String baseFileName ;
    CellStyle integerStyle;

    @Override
    public void setBaseFile(String fileName) {
        if (!fileName.endsWith(DOT_XLS))
            baseFileName = fileName + DOT_XLS;
        else
            baseFileName = fileName;
    }

    @Override
    public void export(Map<String, Long> timings, ITestResult result) {


        Workbook wb;
        InputStream inp = null;

        // Check if Workbook is present - otherwise create it
        try {
            inp = new FileInputStream(baseFileName);
            wb = new HSSFWorkbook(inp);
        } catch (Exception e) {
            wb = new HSSFWorkbook();
        }
        // Now write to it
        try {
            // Check if we have our sheet, otherwise create
            if (wb.getNumberOfSheets()==0) {
                wb.createSheet("Overview");

            }
            Sheet sheet = wb.getSheetAt(0);

            DataFormat df = wb.createDataFormat();
            integerStyle = wb.createCellStyle();
            integerStyle.setDataFormat(df.getFormat("#######0"));


            createOverviewHeaderIfNeeded(sheet);
            long time = getTotalTime(timings);
            createOverviewEntry(sheet, time, result);
            createDetailsSheet(wb,timings,result);

            // Write the output to a file
            FileOutputStream fileOut = new FileOutputStream(baseFileName);
            wb.write(fileOut);
            fileOut.close();
            if (inp!=null)
                inp.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a sheet per test to show the individual timings we recorded
     * @param wb Workbook to attach the new sheet to
     * @param timings The map with timings from the test
     * @param result TestNG results of the test
     */
    private void createDetailsSheet(Workbook wb, Map<String,Long> timings, ITestResult result) {
        Sheet sheet = wb.createSheet(result.getName());

        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Class");
        String name = result.getTestClass().getName();
        name = name.replace("org.rhq.enterprise.server.performance.test.","");
        row.createCell(1).setCellValue(name);
        row = appendRow(sheet);
        row.createCell(0).setCellValue("Method");
        row.createCell(1).setCellValue(result.getName());


        row.createCell(0).setCellValue("Success:");
        row.createCell(1).setCellValue(result.isSuccess());
        row = appendRow(sheet);
        row.createCell(0).setCellValue("TestNG timing");
        row.createCell(1).setCellValue(result.getEndMillis()-result.getStartMillis());
        row = appendRow(sheet);
        row.createCell(0).setCellValue("Perf test timing");
        row.createCell(1).setCellValue(getTotalTime(timings));
        row = appendRow(sheet); // Empty
        row = appendRow(sheet);
        row.createCell(0).setCellValue("Individual Timings");

        // Now the timings
        row = appendRow(sheet);
        row.createCell(0).setCellValue("Name");
        row.createCell(1).setCellValue("Duration");

        Set<Map.Entry<String,Long>> data = timings.entrySet();
        SortedSet<Map.Entry<String,Long>> sorted = new TreeSet<Map.Entry<String,Long>>(new Comparator<Map.Entry<String,Long>>() {

            public int compare(Map.Entry<String,Long> item1, Map.Entry<String,Long> item2) {

                return item1.getKey().compareTo(item2.getKey());
            }
        });
        sorted.addAll(data);

        for (Map.Entry<String,Long> entry: sorted) {
            row = appendRow(sheet);
            Cell cell = row.createCell(0);
            cell.setCellValue(entry.getKey());
            cell = row.createCell(1);
            cell.setCellStyle(integerStyle);
            cell.setCellValue(entry.getValue());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Create a row on the overview sheet
     * @param sheet sheet to use
     * @param testTime time this test took within the perf biz logic
     * @param result the TestNG result object
     */
    private void createOverviewEntry(Sheet sheet, long testTime, ITestResult result) {
        // Class name
        Row row = appendRow(sheet);
        Cell cell = row.createCell(0);
        cell.setCellType(Cell.CELL_TYPE_STRING);
        String name = result.getTestClass().getName();
        name = name.replace("org.rhq.enterprise.server.performance.test.","");
        cell.setCellValue(name);

        // Test name
        cell = row.createCell(1);
        cell.setCellType(Cell.CELL_TYPE_STRING);
        cell.setCellValue(result.getName());

        // success ?
        cell = row.createCell(2);
        cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
        cell.setCellValue(result.isSuccess());

        // timing from TestNG
        cell = row.createCell(3);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        cell.setCellStyle(integerStyle);
        cell.setCellValue(result.getEndMillis()-result.getStartMillis());

        // timing of our business logic

        cell = row.createCell(4);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        cell.setCellStyle(integerStyle);
        cell.setCellValue(testTime);
    }

    /**
     * Create a header row that describes the columns on the overview sheet
     * @param sheet sheet to write to.
     */
    private void createOverviewHeaderIfNeeded(Sheet sheet) {
        Row row = sheet.getRow(0);
        if (row==null)
            row = sheet.createRow(0);

        Cell cell = row.createCell(0);
        cell.setCellValue("Class");
        cell = row.createCell(1);
        cell.setCellValue("Name");
        cell = row.createCell(2);
        cell.setCellValue("Success");
        cell = row.createCell(3);
        cell.setCellValue("TestNG timing");
        cell = row.createCell(4);
        cell.setCellValue("Perf timing");

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);

    }

    /**
     * Get the accumulated time from all the perf test biz logic of the test
     * @param timings Map with timings and 'sub tests'
     * @return summary time
     */
    private long getTotalTime(Map<String, Long> timings) {
        long summaryTime = 0L;
        for (Map.Entry<String,Long> item : timings.entrySet()) {
            summaryTime += item.getValue();
        }
        return summaryTime;
    }

    /**
     * Append a row to the sheet
     * @param sheet Sheet to append a new empty row to
     * @return the newly created row
     */
    private Row appendRow(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        Row ret = sheet.createRow(lastRow+1);
        return ret;
    }
}

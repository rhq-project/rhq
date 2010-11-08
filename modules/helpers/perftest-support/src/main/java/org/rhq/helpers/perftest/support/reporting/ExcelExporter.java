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
import org.rhq.helpers.perftest.support.testng.PerformanceReporting;
import org.testng.ITestResult;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
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
    static final String TARGET = "target/";
    String baseFileName ;
    PerformanceReporting.Rolling rolling;
    CellStyle integerStyle;
    CellStyle boldText;

    @Override
    public void setBaseFile(String fileName) {
        baseFileName = fileName;
        if (baseFileName.startsWith(TARGET))
            baseFileName = TARGET + baseFileName;
    }

    @Override
    public void setRolling(PerformanceReporting.Rolling rolling) {
        this.rolling = rolling;
    }

    @Override
    public void export(Map<String, Long> timings, ITestResult result) {


        Workbook wb;
        InputStream inp = null;

        String fileName = getFileName();

        // Check if Workbook is present - otherwise create it
        try {
            inp = new FileInputStream(fileName);
            wb = new HSSFWorkbook(inp);
        } catch (Exception e) {
            wb = new HSSFWorkbook();
        }
        finally {
            if (inp!=null)
                try {
                    inp.close();
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }
        }
        // Now write to it
        FileOutputStream fileOut = null;
        try {
            // Check if we have our sheet, otherwise create
            if (wb.getNumberOfSheets()==0) {
                wb.createSheet("Overview");

            }
            Sheet sheet = wb.getSheetAt(0);

            DataFormat df = wb.createDataFormat();
            integerStyle = wb.createCellStyle();
            integerStyle.setDataFormat(df.getFormat("#######0"));
            Font boldFont = wb.createFont();
            boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
            boldText = wb.createCellStyle();
            boldText.setFont(boldFont);


            createOverviewHeaderIfNeeded(sheet);
            long time = getTotalTime(timings);
            createOverviewEntry(sheet, time, result);
            createDetailsSheet(wb,timings,result);

            // Write the output to a file
            File outFile = new File(fileName);
            System.out.println("ExcelExporter, writing to " + outFile.getAbsolutePath());
            fileOut = new FileOutputStream(outFile);
            wb.write(fileOut);
            fileOut.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fileOut!=null)
                    fileOut.close();
            } catch (IOException e) {
                System.err.println("Failed to close the workbook: " + e.getMessage());
            }
        }
    }

    /**
     * Create a sheet per test to show the individual timings we recorded
     * @param wb Workbook to attach the new sheet to
     * @param timings The map with timings from the test
     * @param result TestNG results of the test
     */
    private void createDetailsSheet(Workbook wb, Map<String,Long> timings, ITestResult result) {

        String name = result.getTestClass().getName();
        if (name.contains("."))
            name = name.substring(name.lastIndexOf(".")+1);
        name += "."  + result.getName();

        Sheet sheet = wb.getSheet(name);
        if (sheet ==null)
            sheet = wb.createSheet(name);

        Row row = appendRow(sheet);
        Cell cell = row.createCell(0);
        cell.setCellStyle(boldText);
        cell.setCellValue("Class");
        name = result.getTestClass().getName();
        if (name.contains("."))
            name = name.substring(name.lastIndexOf(".")+1);
        row.createCell(1).setCellValue(name);
        row = appendRow(sheet);

        cell = row.createCell(0);
        cell.setCellStyle(boldText);
        cell.setCellValue("Method");
        row.createCell(1).setCellValue(result.getName());


        cell = row.createCell(0);
        cell.setCellStyle(boldText);
        cell.setCellValue("Success:");
        row.createCell(1).setCellValue(result.isSuccess());

        row = appendRow(sheet);
        cell = row.createCell(0);
        cell.setCellStyle(boldText);
        cell.setCellValue("TestNG timing");
        row.createCell(1).setCellValue(result.getEndMillis()-result.getStartMillis());

        row = appendRow(sheet);
        cell = row.createCell(0);
        cell.setCellStyle(boldText);
        cell.setCellValue("Perf test timing");
        row.createCell(1).setCellValue(getTotalTime(timings));

        row = appendRow(sheet); // Empty row
        row = appendRow(sheet);
        cell = row.createCell(0);
        cell.setCellStyle(boldText);
        cell.setCellValue("Individual Timings");

        // Now the timings
        row = appendRow(sheet);
        cell = row.createCell(0);
        cell.setCellStyle(boldText);
        cell.setCellValue("Name");
        cell = row.createCell(1);
        cell.setCellStyle(boldText);
        cell.setCellValue("Duration");

        Set<Map.Entry<String,Long>> data = timings.entrySet();
        SortedSet<Map.Entry<String,Long>> sorted = new TreeSet<Map.Entry<String,Long>>(new Comparator<Map.Entry<String,Long>>() {

            public int compare(Map.Entry<String,Long> item1, Map.Entry<String,Long> item2) {

                return item1.getKey().compareTo(item2.getKey());
            }
        });
        sorted.addAll(data);

        for (Map.Entry<String,Long> entry: sorted) {
            row = appendRow(sheet);
            cell = row.createCell(0);
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
        // Test name
        Row row = appendRow(sheet);
        Cell cell = row.createCell(0);
        cell.setCellType(Cell.CELL_TYPE_STRING);
        String name = result.getTestClass().getName();
        if (name.contains("."))
            name = name.substring(name.lastIndexOf(".")+1);
        name += "." + result.getName();
        cell.setCellValue(name);

        // success ?
        cell = row.createCell(1);
        cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
        cell.setCellValue(result.isSuccess());

        // timing from TestNG
        cell = row.createCell(2);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        cell.setCellStyle(integerStyle);
        cell.setCellValue(result.getEndMillis()-result.getStartMillis());

        // timing of our business logic
        cell = row.createCell(3);
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
        cell.setCellStyle(boldText);
        cell.setCellValue("Name");
        cell = row.createCell(1);
        cell.setCellStyle(boldText);
        cell.setCellValue("Success");
        cell = row.createCell(2);
        cell.setCellStyle(boldText);
        cell.setCellValue("TestNG timing");
        cell.setCellStyle(boldText);
        cell = row.createCell(3);
        cell.setCellValue("Perf timing");
        cell.setCellStyle(boldText);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);

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


    /**
     * Return the filename to use based on the baseFileName and
     * the the rolling argument of the @PerformanceReporting annotation
     * @return filename for the excel file ending on .xls
     */
    private String getFileName() {
        String fileName = baseFileName;
        DateFormat df ;
        String suffix = "";

        switch (rolling) {
            case HOURLY:
                df = new SimpleDateFormat("yyMMdd-kk");
                suffix = "-" + df.format(new Date());
                break;
            case DAILY:
                df = new SimpleDateFormat("yyMMdd");
                suffix = "-" + df.format(new Date());
            default:
                break;
        }

        fileName = fileName + suffix + DOT_XLS;

        return fileName;
    }
}

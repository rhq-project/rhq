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

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.testng.ITestResult;

import java.io.*;
import java.util.Map;


/**
 * Export data to an excel sheet
 *
 * @author Heiko W. Rupp
 */
public class ExcelExporter implements PerformanceReportExporter {

    private static final String DOT_XLS = ".xls";
    String baseFileName ;

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
        CellStyle integerStyle;
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


            createHeaderIfNeeded(sheet);

            // Test name
            Row row = appendRow(sheet);
            Cell cell = row.createCell(0);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(result.getName());

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
            long time = getTotalTime(timings);
            cell = row.createCell(3);
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            cell.setCellStyle(integerStyle);
            cell.setCellValue(time);


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

    private void createHeaderIfNeeded(Sheet sheet) {
        Row row = sheet.getRow(0);
        if (row==null)
            row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Name");
        cell = row.createCell(1);
        cell.setCellValue("Success");
        cell = row.createCell(2);
        cell.setCellValue("TestNG timing");
        cell = row.createCell(3);
        cell.setCellValue("Perf timing");

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);

    }

    private long getTotalTime(Map<String, Long> timings) {
        long summaryTime = 0L;
        for (Map.Entry<String,Long> item : timings.entrySet()) {
            System.out.println(":| " + item.getKey() + " => " + item.getValue());
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

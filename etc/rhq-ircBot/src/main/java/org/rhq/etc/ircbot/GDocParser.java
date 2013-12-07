/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.etc.ircbot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author Jirka Kremser
 *
 */
public class GDocParser {
    public static String onSupport2() throws AuthenticationException, MalformedURLException, IOException,
        ServiceException, URISyntaxException {
        SpreadsheetService service = new SpreadsheetService("MySpreadsheetIntegration-foo");
        URL url = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full?title=GSS-Dev-Schedule");
        SpreadsheetFeed feed = service.getFeed(url, SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = feed.getEntries();
        SpreadsheetEntry spreadsheet = spreadsheets.get(0);
        WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
        List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
        WorksheetEntry worksheet = worksheets.get(0);
        // fetch A1 only
        URL cellFeedUrl = new URI(worksheet.getCellFeedUrl().toString() + "?min-row=1&max-row=1&min-col=1&max-col=1")
            .toURL();
        CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);
        // Iterate through each cell, printing its value.
        String onSupport = cellFeed.getEntries().get(0).getCell().getValue();
        return onSupport;
    }

    public static String onSupport1() {
        // the simplest way of accessing the publicly available gdoc (OAuth is not needed here)
        String url = "https://docs.google.com/spreadsheet/pub?key=0AsqiOfOdbEhBdEtMZGRGUTVZNTNmYXlFdllqdU9Oamc&single=true&gid=1&output=txt";
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc.body().html().trim();
    }

    public static void main(String[] args) {
        System.out.println(GDocParser.onSupport1());
    }
}

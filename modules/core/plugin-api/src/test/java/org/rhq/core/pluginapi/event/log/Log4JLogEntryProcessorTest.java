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
package org.rhq.core.pluginapi.event.log;

import java.util.Date;
import java.util.Calendar;
import java.util.Set;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.File;

import org.testng.annotations.Test;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;

/**
 * @author Ian Springer
 */
@Test
public class Log4JLogEntryProcessorTest {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final StringBuilder TEST_LOG = new StringBuilder();
    static
    {
        TEST_LOG.append("2007-12-09 15:32:49,909 DEBUG [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000").append(LINE_SEPARATOR);
        TEST_LOG.append("2008-02-09 02:10:11,909 INFO [com.example.FooBar] a multi-line entry").append(LINE_SEPARATOR);
        TEST_LOG.append("\tyada yada yada").append(LINE_SEPARATOR);
        TEST_LOG.append("\twocka wocka").append(LINE_SEPARATOR);
    }

    public void testProcessLine() throws Exception {
        String eventType = "logEntry";
        File logFile = new File("C:/test.log");
        System.out.println("Testing with no includes pattern...");
        Log4JLogEntryProcessor processor = new Log4JLogEntryProcessor(eventType, logFile);
        BufferedReader bufferedReader = new BufferedReader(new StringReader(TEST_LOG.toString()));
        Set<Event> events = processor.processLines(bufferedReader);
        assert events != null && events.size() == 2;
        Iterator<Event> eventIterator = events.iterator();
        Event event1 = eventIterator.next();
        assert eventType.equals(event1.getType());
        assert new File(event1.getSourceLocation()).equals(logFile);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2007, 11, 9, 15, 32, 49);
        calendar.set(Calendar.MILLISECOND, 909);
        Date expectedDate = calendar.getTime();
        assert event1.getTimestamp().equals(expectedDate);
        assert event1.getSeverity().equals(EventSeverity.DEBUG);
        assert event1.getDetail().equals("[com.example.FooBar] run: IdleRemover notifying pools, interval: 450000");
        Event event2 = eventIterator.next();
        assert event2.getDetail().startsWith("[com.example.FooBar] a multi-line entry\n");
        assert event2.getDetail().endsWith("\twocka wocka");
        System.out.println("SUCCESS!");
        System.out.println("Testing with matching includes pattern...");
        processor.setIncludesPattern(Pattern.compile("wocka wocka"));
        bufferedReader = new BufferedReader(new StringReader(TEST_LOG.toString()));
        events = processor.processLines(bufferedReader);
        assert events != null && events.size() == 1;        
        event1 = events.iterator().next();
        assert event1.getDetail().endsWith("\twocka wocka");
        System.out.println("SUCCESS!");
        System.out.println("Testing with non-matching includes pattern...");
        processor.setIncludesPattern(Pattern.compile("hubba bubba"));
        bufferedReader = new BufferedReader(new StringReader(TEST_LOG.toString()));
        events = processor.processLines(bufferedReader);
        assert events != null && events.size() == 0;
        System.out.println("SUCCESS!");
    }
}

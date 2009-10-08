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
package org.rhq.core.domain.operation.test;

import org.testng.annotations.Test;
import org.rhq.core.domain.operation.HistoryJobId;
import org.rhq.core.domain.operation.JobId;
import org.rhq.core.domain.operation.ScheduleJobId;

@Test
public class JobIdTest {
    public void testJobIdEquals() {
        JobId one;
        JobId two;

        one = new JobId("name", "group");
        assert one.equals(one);
        assert one.getJobName().equals("name");
        assert one.getJobGroup().equals("group");

        one = new JobId("name", "group");
        two = new JobId("name", "group");
        assert one != two;
        assert one.getJobName().equals(two.getJobName());
        assert one.getJobGroup().equals(two.getJobGroup());
        assert one.equals(two);
        assert one.hashCode() == two.hashCode();

        two = new JobId("nameX", "group");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        two = new JobId("name", "groupX");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new JobId("nameX", "group");
        two = new JobId("name", "group");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new JobId("name", "groupX");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        // cannot compare JobId with a HistoryJobId
        one = new HistoryJobId("name", "group", 1);
        two = new JobId("name", "group");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new JobId("name", "group");
        two = new HistoryJobId("name", "group", 1);
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();
    }

    public void testHistoryJobIdEquals() {
        HistoryJobId one;
        HistoryJobId two;

        one = new HistoryJobId("name", "group", 1);
        assert one.equals(one);
        assert one.getJobName().equals("name");
        assert one.getJobGroup().equals("group");
        assert one.getCreatedTime() == 1;

        one = new HistoryJobId("name", "group", 1);
        two = new HistoryJobId("name", "group", 1);
        assert one != two;
        assert one.getJobName().equals(two.getJobName());
        assert one.getJobGroup().equals(two.getJobGroup());
        assert one.getCreatedTime() == two.getCreatedTime();
        assert one.equals(two);
        assert one.hashCode() == two.hashCode();

        two = new HistoryJobId("nameX", "group", 1);
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        two = new HistoryJobId("name", "groupX", 1);
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        two = new HistoryJobId("name", "group", 2);
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new HistoryJobId("nameX", "group", 1);
        two = new HistoryJobId("name", "group", 1);
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new HistoryJobId("name", "groupX", 1);
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new HistoryJobId("name", "group", 2);
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();
    }

    public void testScheduleJobIdEquals() {
        ScheduleJobId one;
        ScheduleJobId two;

        one = new ScheduleJobId("name", "group");
        assert one.equals(one);
        assert one.getJobName().equals("name");
        assert one.getJobGroup().equals("group");

        one = new ScheduleJobId("name", "group");
        two = new ScheduleJobId("name", "group");
        assert one != two;
        assert one.getJobName().equals(two.getJobName());
        assert one.getJobGroup().equals(two.getJobGroup());
        assert one.equals(two);
        assert one.hashCode() == two.hashCode();

        two = new ScheduleJobId("nameX", "group");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        two = new ScheduleJobId("name", "groupX");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new ScheduleJobId("nameX", "group");
        two = new ScheduleJobId("name", "group");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();

        one = new ScheduleJobId("name", "groupX");
        assert one != two;
        assert !one.equals(two);
        assert one.hashCode() != two.hashCode();
    }
}
package org.rhq.enterprise.gui.legacy.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import org.rhq.enterprise.gui.common.time.converter.RelativeDurationConverter;

@Test
public class DurationConvertersTest extends AssertJUnit {

    private final long millisInMinute = (60 * 1000L);
    private final long millisInHour = (60 * millisInMinute);
    private final long millisInDay = (24 * millisInHour);

    @Test
    public void testRelativeDurationConverter() {
        // exhaustive test of RelativeDurationConverter converter across all timezones
        for (String tz : TimeZone.getAvailableIDs()) {
            convert(tz);
        }
    }

    private void convert(String timeZoneId) {
        // bootstrap the Converter properly
        RelativeDurationConverter.tz = TimeZone.getTimeZone(timeZoneId);
        //System.out.println(RelativeDurationConverter.tz);

        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aaa z");

        // hh:mm aaa z
        Date nowDate = new Date(System.currentTimeMillis());
        String simpleFormatted = formatter.format(nowDate);
        String converterFormatted = RelativeDurationConverter.format(nowDate.getTime());
        assertEqualsAndPrint(simpleFormatted, converterFormatted);

        // Yesterday, hh:mm aaa z
        nowDate = new Date(System.currentTimeMillis() - days(1));
        simpleFormatted = formatter.format(nowDate);
        converterFormatted = RelativeDurationConverter.format(nowDate.getTime());
        assertEqualsAndPrint("Yesterday, " + simpleFormatted, converterFormatted);

        // X days
        nowDate = new Date(System.currentTimeMillis() - days(2));
        converterFormatted = RelativeDurationConverter.format(nowDate.getTime());
        assertEqualsAndPrint("2 days ago", converterFormatted);

        // X days, Y hours
        nowDate = new Date(System.currentTimeMillis() - days(2) - hours(5));
        converterFormatted = RelativeDurationConverter.format(nowDate.getTime());
        assertEqualsAndPrint("2 days, 5 hours ago", converterFormatted);

        // X days, Z minutes
        nowDate = new Date(System.currentTimeMillis() - days(2) - mins(10));
        converterFormatted = RelativeDurationConverter.format(nowDate.getTime());
        assertEqualsAndPrint("2 days, 10 minutes ago", converterFormatted);

        // X days, Y hours (Z minutes suppressed)
        nowDate = new Date(System.currentTimeMillis() - days(2) - hours(3) - mins(10));
        converterFormatted = RelativeDurationConverter.format(nowDate.getTime());
        assertEqualsAndPrint("2 days, 3 hours ago", converterFormatted);
    }

    private void assertEqualsAndPrint(String arg1, String arg2) {
        // uncomment printlns if there are test failures
        //System.out.println("First: " + arg1);
        //System.out.println("Second: " + arg1);
        assertEquals(arg1, arg2);
    }

    private long days(int count) {
        return count * millisInDay;
    }

    private long hours(int count) {
        return count * millisInHour;
    }

    private long mins(int count) {
        return count * millisInMinute;
    }
}

package org.rhq.helpers.perftest.support.testng;

import org.rhq.helpers.perftest.support.reporting.PerformanceReportExporter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define were to export performance test results to
 *
 * @author Heiko W. Rupp
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PerformanceReporting {

    /** Base file name of the report without any .xls suffix */
    String baseFilename() default "performance-report";

    /** Exporter class to use to export the report */
    Class<? extends PerformanceReportExporter> exporter() ;

    /** Should reports be rolled over or overwritten. Gives the frequency of
     * new file creation
     */
    Rolling rolling() default Rolling.DAILY;

    public enum Rolling {
        NONE,
        DAILY,
        HOURLY;
    }
}

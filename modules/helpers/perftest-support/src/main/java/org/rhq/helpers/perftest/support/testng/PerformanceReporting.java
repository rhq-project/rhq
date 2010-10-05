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

    String baseFilename() default "performance-report";

    Class<? extends PerformanceReportExporter> exporter() ;
}

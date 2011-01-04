package org.rhq.enterprise.server.common;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;

/**
 * To enable this interceptor, add a binding to the ejb-jar.xml:
 * 
 * &lt;!-- enable this to get lots of performance data as you navigate around the UI
 *     &lt;interceptor-binding>
 *       &lt;ejb-name>*&lt;/ejb-name>
 *        &lt;interceptor-class>org.rhq.enterprise.server.common.PerformanceMonitorInterceptor&lt;/interceptor-class>
 *     &lt;/interceptor-binding>
 * -->
 */
public class PerformanceMonitorInterceptor {
    @AroundInvoke
    public Object monitorHibernatePerformance(InvocationContext context) throws Exception {
        String prefix = context.getMethod().getDeclaringClass().getSimpleName() + "." + context.getMethod().getName();
        long monitorId = HibernatePerformanceMonitor.get().start();
        Object results = context.proceed();
        HibernatePerformanceMonitor.get().stop(monitorId, "SLSB:" + prefix);
        return results;
    }
}

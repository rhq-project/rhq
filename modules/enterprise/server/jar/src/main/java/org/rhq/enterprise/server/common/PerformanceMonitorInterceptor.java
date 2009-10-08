package org.rhq.enterprise.server.common;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;

public class PerformanceMonitorInterceptor {
    @AroundInvoke
    public Object monitorHibernatePerformance(InvocationContext context) throws Exception {
        String prefix = context.getMethod().getDeclaringClass().getSimpleName() + "." + context.getMethod().getName();
        long monitorId = HibernatePerformanceMonitor.get().start();
        Object results = context.proceed();
        HibernatePerformanceMonitor.get().stop(monitorId, prefix);
        return results;
    }
}

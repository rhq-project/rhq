/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.test.upgrade.v2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.pluginapi.inventory.InventoryContext;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;

/**
 * Class that stresses the different sub contexts available through the ResourceContext.
 * 
 * @author Lukas Krejci
 */
public class ResourceContextStress {

    public static class Report {
        private final Method method;
        private final Throwable error;

        public Report(Method method, Throwable error) {
            this.method = method;
            this.error = error;
        }

        public Method getMethod() {
            return method;
        }

        public Throwable getError() {
            return error;
        }

        @Override
        public String toString() {
            StringWriter out = new StringWriter();
            PrintWriter wrt = new PrintWriter(out, true);

            try {
                wrt.write("ResourceContextStress.Report[");
                wrt.write("method='");
                wrt.write(method.toString());
                wrt.write("', error=");
        
                String message = error.getMessage();
                if (message != null) {
                    wrt.write(message);
                    wrt.write(":\n");
                }
                error.printStackTrace(wrt);
        
                wrt.write("]");
        
                wrt.flush();
                
                return out.toString();
            } finally {
                wrt.close();
            }
        }
    }

    private class ErrorCollector implements InvocationHandler {
        private Object object;

        public ErrorCollector(Object object) {
            this.object = object;
        }

        @Override
        public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
            try {
                return method.invoke(object, args);
            } catch (InvocationTargetException e) {
                addFailure(usageTag, method, e.getCause());
                
                Class<?> returnType = method.getReturnType();
                
                if (!returnType.isPrimitive() || returnType.isArray()) {
                    return null;
                } else {
                    if (returnType.equals(byte.class)) {
                        return Byte.valueOf((byte) 0);
                    } else if (returnType.equals(short.class)) {
                        return Short.valueOf((short) 0);
                    } else if (returnType.equals(int.class)) {
                        return Integer.valueOf(0);
                    } else if (returnType.equals(long.class)) {
                        return Long.valueOf(0);
                    } else if (returnType.equals(float.class)) {
                        return Float.valueOf(0);
                    } else if (returnType.equals(double.class)) {
                        return Double.valueOf(0);
                    } else if (returnType.equals(char.class)) {
                        return Character.valueOf((char) 0);
                    } else if (returnType.equals(boolean.class)) {
                        return Boolean.valueOf(false);
                    } else if (returnType.equals(void.class)) {
                        return null;
                    }
                }
                
                return null;
            }
        }
    }

    private static final Map<String, List<Report>> REPORTS_PER_USAGE = new LinkedHashMap<String, List<Report>>();

    private ResourceContext<?> context;
    private String usageTag;

    public ResourceContextStress(ResourceContext<?> context, String usageTag) {
        this.context = context;
        this.usageTag = usageTag;
    }

    public static void resetReports() {
        REPORTS_PER_USAGE.clear();
    }

    public static Map<String, List<Report>> getAllReports() {
        return REPORTS_PER_USAGE;
    }

    public static List<Report> getUsageReport(String usageTag) {
        return REPORTS_PER_USAGE.get(usageTag);
    }

    private static synchronized void addFailure(String usage, Method invokedMethod, Throwable failure) {
        List<Report> reports = REPORTS_PER_USAGE.get(usage);
        if (reports == null) {
            reports = new ArrayList<Report>();
            REPORTS_PER_USAGE.put(usage, reports);
        }

        reports.add(new Report(invokedMethod, failure));
    }

    public void stress() {
        stress(context.getAvailabilityContext());
        stress(context.getContentContext());
        stress(context.getEventContext());
        stress(context.getInventoryContext());
        stress(context.getOperationContext());
    }

    private void stress(AvailabilityContext ctx) {
        ctx = getErrorCollectingProxy(ctx, AvailabilityContext.class);

        //XXX These are executed in standalone threads and therefore causing
        //"possible deadlock" errors in agent.log during upgrade
        ctx.disable();
        ctx.enable();
        ctx.getLastReportedAvailability();
        ctx.requestAvailabilityCheck();
    }

    private void stress(ContentContext ctx) {
        ContentContext ectx = getErrorCollectingProxy(ctx, ContentContext.class);

        ContentServices services = ectx.getContentServices();

        //if services are null, we already had a failure collected
        if (services != null) {
            services = getErrorCollectingProxy(services, ContentServices.class);

            //XXX alll these fail because the ContentManager doesn't have the configuration
            //set and is not initialized
            services.downloadPackageBits(ctx, null, null, false);
            services.downloadPackageBitsForChildResource(ctx, null, null, null);
            services.downloadPackageBitsRange(ctx, null, null, 0, 0, false);
            services.getPackageBitsLength(ctx, null);
            services.getPackageVersionMetadata(ctx, PageControl.getUnlimitedInstance());
            services.getResourceSubscriptionMD5(ctx);
        }
    }

    private void stress(EventContext ctx) {
        ctx = getErrorCollectingProxy(ctx, EventContext.class);

        //XXX fails in EventManager.publishEvents() because EventManager.initialize() hasn't been called yet during
        //upgrade.
        ctx.publishEvent(new Event("log", "dummy", System.currentTimeMillis(), EventSeverity.INFO, ""));

        EventPoller fakePoller = new EventPoller() {

            @Override
            @Nullable
            public Set<Event> poll() {
                return Collections.emptySet();
            }

            @Override
            @NotNull
            public String getEventType() {
                return "log";
            }
        };

        ctx.registerEventPoller(fakePoller, 60);
        ctx.registerEventPoller(fakePoller, 60, "dummy");

        //XXX fails due to EventManager.initialize() not being called
        ctx.unregisterEventPoller("log");
        //XXX fails due to EventManager.initialize() not being called
        ctx.unregisterEventPoller("log", "dummy");
    }

    private void stress(InventoryContext ctx) {
        ctx = getErrorCollectingProxy(ctx, InventoryContext.class);

        //XXX fails because InventoryManager does not have the threadpools setup at the time of resource upgrade
        ctx.requestChildResourcesDiscovery();
        //XXX fails because InventoryManager does not have the threadpools setup at the time of resource upgrade
        ctx.requestDeferredChildResourcesDiscovery();
    }

    private void stress(OperationContext ctx) {
        OperationContext ectx = getErrorCollectingProxy(ctx, OperationContext.class);

        OperationServices ops = ectx.getOperationServices();

        //if ops are null, there already has been an error collected
        if (ops != null) {
            ops = getErrorCollectingProxy(ops, OperationServices.class);

            //XXX fails - OperationManager.initialize() wasn't called
            ops.invokeOperation(ctx, "op", null, 1);
        }
    }

    private <T> T getErrorCollectingProxy(T object, Class<T> iface) {
        return iface.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { iface },
            new ErrorCollector(object)));
    }
}

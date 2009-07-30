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
package org.rhq.core.system;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarProxy;

/**
 * Provides synchronized Sigar access allowing you to obtain low-level platform details
 * and functionality.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class SigarAccess {

    private static SigarAccessHandler invocationHandler;
    private static SigarProxy sigarProxy;

    /**
     * Provides the caller with direct access to the Sigar native API via a proxy to Sigar.
     * This provides the caller with low-level access to platform functionality.
     * If the Sigar native layer is not available on the platform we are running on, or if
     * it has been disabled by the user, an exception will be thrown.
     *  
     * @return the proxy used to access the native layer
     * 
     * @throws SystemInfoException if native layer is unavailable or has been disabled
     */
    public synchronized static SigarProxy getSigar() {
        if (isSigarAvailable()) {
            if (sigarProxy == null) {
                invocationHandler = new SigarAccessHandler();
                sigarProxy = (SigarProxy) Proxy.newProxyInstance(SigarAccess.class.getClassLoader(),
                    new Class[] { SigarProxy.class }, invocationHandler);
            }
            return sigarProxy;
        } else {
            throw new SystemInfoException("Native layer is not available or has been disabled");
        }
    }

    public static void close() {
        if (invocationHandler != null) {
            invocationHandler.close();
        }
    }

    public static boolean isSigarAvailable() {
        if (SystemInfoFactory.isNativeSystemInfoAvailable() && !SystemInfoFactory.isNativeSystemInfoDisabled()) {
            // its available, but it may not yet have been initialized. If it has not been initialized,
            // make a call that forces it to be initialized and loaded. 99% of the time, the native layer
            // will already be initialized and this check will be very fast.
            if (!SystemInfoFactory.isNativeSystemInfoInitialized()) {
                SystemInfoFactory.getNativeSystemInfoVersion();
            }
            return true;
        } else {
            return false;
        }

    }

    private static class SigarAccessHandler implements InvocationHandler {
        private Sigar sigar;
        private AtomicLong accessCount = new AtomicLong();

        public Object invoke(Object proxy, Method meth, Object[] args) throws Throwable {
            try {
                accessCount.incrementAndGet();
                synchronized (this) {
                    if (sigar == null) {
                        this.sigar = new Sigar();
                    }

                    return meth.invoke(sigar, args);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        public synchronized void close() {
            if (this.sigar != null) {
                this.sigar.close();
                this.sigar = null;
            }
        }
    }

}
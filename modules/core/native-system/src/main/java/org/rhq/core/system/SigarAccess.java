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

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Synchronized Sigar access
 *
 * @author Greg Hinkle
 */
public class SigarAccess {

    private static SigarAccessHandler invocationHandler;
    private static SigarProxy sigarProxy;


    public synchronized static SigarProxy getSigar() {
        if (sigarProxy == null) {
            invocationHandler = new SigarAccessHandler();
            sigarProxy = (SigarProxy)
                    Proxy.newProxyInstance(SigarAccess.class.getClassLoader(),
                            new Class[]{SigarProxy.class},
                            invocationHandler);
        }
        return sigarProxy;
    }

    public static void close() {
        if (invocationHandler != null) {
            invocationHandler.close();
        }
    }


    private static class SigarAccessHandler implements InvocationHandler {
        private Sigar sigar;
        private AtomicLong accessCount = new AtomicLong();

        public Object invoke(Object proxy, Method meth, Object[] args)
                throws Throwable {
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
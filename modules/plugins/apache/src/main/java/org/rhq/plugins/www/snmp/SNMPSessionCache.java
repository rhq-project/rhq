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
package org.rhq.plugins.www.snmp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * SNMPSession interface cache. Cache is per-session instance. Currently supports getColumn, getBulk, and getTable
 * methods only.
 */
class SNMPSessionCache implements InvocationHandler {
    private SNMPSession session;

    private Map columnCache = new HashMap();
    private Map bulkCache = new HashMap();
    private Map tableCache = new HashMap();
    private final Log log = LogFactory.getLog(this.getClass());

    public static final int EXPIRE_DEFAULT = 30 * 1000; //30 seconds
    private int expire;

    SNMPSessionCache(SNMPSession session, int expire) {
        this.session = session;
        this.expire = expire;
    }

    static SNMPSession newInstance(SNMPSession session, int expire) throws SNMPException {
        SNMPSessionCache handler = new SNMPSessionCache(session, expire);
        SNMPSession sessionCache;

        try {
            sessionCache = (SNMPSession) Proxy.newProxyInstance(SNMPSession.class.getClassLoader(),
                new Class[] { SNMPSession.class }, handler);
        } catch (Exception e) {
            throw new SNMPException(e.getMessage());
        }

        return sessionCache;
    }

    private SNMPCacheObject getFromCache(long timeNow, Map cache, String name, Object arg) {
        SNMPCacheObject cacheVal = (SNMPCacheObject) cache.get(arg);

        String argDebug = "";

        if (log.isDebugEnabled()) {
            argDebug = " with arg=" + arg;
        }

        if (cacheVal == null) {
            cacheVal = new SNMPCacheObject();
            cacheVal.expire = this.expire;
            cache.put(arg, cacheVal);
        } else if ((timeNow - cacheVal.timestamp) > cacheVal.expire) {
            if (log.isDebugEnabled()) {
                log.debug("expiring " + name + " from cache" + argDebug);
            }

            cacheVal.value = null;
        }

        return cacheVal;
    }

    private StringBuffer invokerToString(String name, Object[] args, Object cacheKey) {
        StringBuffer invoker = new StringBuffer(name);

        invoker.append('(');

        if (args!= null && args.length != 0) {
            String arg = args[0].toString();

            invoker.append(arg);

            for (int i = 1; i < args.length; i++) {
                invoker.append('.').append(args[i]);
            }

            if ((cacheKey != null) && !arg.equals(cacheKey)) {
                //note real cache key to match up with expire log
                invoker.append('/').append(cacheKey);
            }

        }
        invoker.append(')');

        return invoker;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws SNMPException {
        SNMPCacheObject cacheVal = null;
        Map cache = null;

        Object cacheKey = null;
        Object retval;
        String name = method.getName();

        long timeNow = 0;

        //XXX perhaps more later
        if (name.equals("getBulk")) {
            cache = this.bulkCache;
            cacheKey = args[0];
        } else if (name.equals("getTable")) {
            cache = this.tableCache;
            cacheKey = args[0].hashCode() ^ args[1].hashCode();
        } else if (name.equals("getColumn")) {
            cache = this.columnCache;
            cacheKey = args[0];
        }

        if (cache != null) {
            timeNow = System.currentTimeMillis();
            cacheVal = getFromCache(timeNow, cache, name, cacheKey);

            if (cacheVal.value != null) {
                return cacheVal.value;
            }
        }

        try {
            retval = method.invoke(this.session, args);
        } catch (InvocationTargetException e) {
            SNMPException snmpException;
            Throwable t = e.getTargetException();
            if (t instanceof SNMPException) {
                snmpException = (SNMPException) t;
            } else {
                String msg = t + " (while invoking: " + invokerToString(name, args, cacheKey) + ")";
                snmpException = new SNMPException(msg);
            }

            throw snmpException;
        } catch (Exception e) {
            String msg = e + " (while invoking: " + invokerToString(name, args, cacheKey) + ")";
            throw new SNMPException(msg);
        }

        if (cacheVal != null) {
            cacheVal.value = retval;
            cacheVal.timestamp = timeNow;

            //if (log.isDebugEnabled()) {
            //    log.debug(invokerToString(name, args, cacheKey) +
            //              " took: " + new net.hyperic.util.timer.StopWatch(timeNow));
            //}
        }

        return retval;
    }
}
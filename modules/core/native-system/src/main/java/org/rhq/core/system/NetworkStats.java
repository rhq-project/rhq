/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.core.system;

import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetStat;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Greg Hinkle
 */
public class NetworkStats {

    protected int[] tcpStates;
    protected int tcpInboundTotal, tcpOutboundTotal;
    protected int allInboundTotal, allOutboundTotal;


    public NetworkStats(NetStat interfaceStat) {
        refresh(interfaceStat);
    }

    public void refresh(NetStat interfaceStat) {
        this.tcpStates = interfaceStat.getTcpStates();
        this.tcpInboundTotal = interfaceStat.getTcpInboundTotal();
        this.tcpOutboundTotal = interfaceStat.getTcpOutboundTotal();
        this.allInboundTotal = interfaceStat.getAllInboundTotal();
        this.allOutboundTotal = interfaceStat.getAllOutboundTotal();
    }

    public int getByName(String propertyName) {
        try {
            BeanInfo info = Introspector.getBeanInfo(NetworkStats.class);
            PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : descriptors) {
                if (descriptor.getName().equals(propertyName)) {
                    Integer value = (Integer) descriptor.getReadMethod().invoke(this);
                    return value;
                }
            }
            throw new RuntimeException("Couldn't find property " + propertyName + " on NetworkStats");            
        } catch (Exception e) {
            throw new RuntimeException("Couldn't read property " + propertyName + " from NetworkStats", e);
        }
    }

    public int getTcpInboundTotal() {
        return this.tcpInboundTotal;
    }

    public int getTcpOutboundTotal() {
        return this.tcpOutboundTotal;
    }

    public int getAllInboundTotal() {
        return this.allInboundTotal;
    }

    public int getAllOutboundTotal() {
        return this.allOutboundTotal;
    }

    public int[] getTcpStates() {
        return this.tcpStates;
    }

    //state counters
    public int getTcpEstablished() {
        return this.tcpStates[NetFlags.TCP_ESTABLISHED];
    }

    public int getTcpSynSent() {
        return this.tcpStates[NetFlags.TCP_SYN_SENT];
    }

    public int getTcpSynRecv() {
        return this.tcpStates[NetFlags.TCP_SYN_RECV];
    }

    public int getTcpFinWait1() {
        return this.tcpStates[NetFlags.TCP_FIN_WAIT1];
    }

    public int getTcpFinWait2() {
        return this.tcpStates[NetFlags.TCP_FIN_WAIT2];
    }

    public int getTcpTimeWait() {
        return this.tcpStates[NetFlags.TCP_TIME_WAIT];
    }

    public int getTcpClose() {
        return this.tcpStates[NetFlags.TCP_CLOSE];
    }

    public int getTcpCloseWait() {
        return this.tcpStates[NetFlags.TCP_CLOSE_WAIT];
    }

    public int getTcpLastAck() {
        return this.tcpStates[NetFlags.TCP_LAST_ACK];
    }

    public int getTcpListen() {
        return this.tcpStates[NetFlags.TCP_LISTEN];
    }

    public int getTcpClosing() {
        return this.tcpStates[NetFlags.TCP_CLOSING];
    }

    public int getTcpIdle() {
        return this.tcpStates[NetFlags.TCP_IDLE];
    }

    public int getTcpBound() {
        return this.tcpStates[NetFlags.TCP_BOUND];
    }
}

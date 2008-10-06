 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.system;

import org.hyperic.sigar.NetInterfaceStat;

public class NetworkAdapterStats {
    private long rxBytes;
    private long rxPackets;
    private long rxErrors;
    private long rxDropped;
    private long rxOverruns;
    private long rxFrame;
    private long txBytes;
    private long txPackets;
    private long txErrors;
    private long txDropped;
    private long txOverruns;
    private long txCollisions;
    private long txCarrier;

    public NetworkAdapterStats(NetInterfaceStat nis) {
        this.rxBytes = nis.getRxBytes();
        this.rxPackets = nis.getRxPackets();
        this.rxErrors = nis.getRxErrors();
        this.rxDropped = nis.getRxDropped();
        this.rxOverruns = nis.getRxOverruns();
        this.rxFrame = nis.getRxFrame();
        this.txBytes = nis.getTxBytes();
        this.txPackets = nis.getTxPackets();
        this.txErrors = nis.getTxErrors();
        this.txDropped = nis.getTxDropped();
        this.txOverruns = nis.getTxOverruns();
        this.txCollisions = nis.getTxCollisions();
        this.txCarrier = nis.getTxCarrier();
    }

    public long getRxBytes() {
        return rxBytes;
    }

    public long getRxPackets() {
        return rxPackets;
    }

    public long getRxErrors() {
        return rxErrors;
    }

    public long getRxDropped() {
        return rxDropped;
    }

    public long getRxOverruns() {
        return rxOverruns;
    }

    public long getRxFrame() {
        return rxFrame;
    }

    public long getTxBytes() {
        return txBytes;
    }

    public long getTxPackets() {
        return txPackets;
    }

    public long getTxErrors() {
        return txErrors;
    }

    public long getTxDropped() {
        return txDropped;
    }

    public long getTxOverruns() {
        return txOverruns;
    }

    public long getTxCollisions() {
        return txCollisions;
    }

    public long getTxCarrier() {
        return txCarrier;
    }
}
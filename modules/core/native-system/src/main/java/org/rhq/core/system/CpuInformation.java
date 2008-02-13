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

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;

/**
 * Provides native information on a specific CPU.
 */
public class CpuInformation {
    private final int cpuIndex;
    private Cpu cpu;
    private CpuInfo cpuInfo;
    private CpuPerc cpuPercentage;

    public CpuInformation(int index) {
        cpuIndex = index;
        refresh();
    }

    public int getCpuIndex() {
        return cpuIndex;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public CpuInfo getCpuInfo() {
        return cpuInfo;
    }

    public CpuPerc getCpuPercentage() {
        return cpuPercentage;
    }

    public void refresh() {
        Sigar sigar = new Sigar();
        try {
            Cpu[] cpuList = sigar.getCpuList();
            CpuInfo[] cpuInfoList = sigar.getCpuInfoList();
            CpuPerc[] cpuPercentageList = sigar.getCpuPercList();

            cpu = (cpuList != null) ? cpuList[this.cpuIndex] : null;
            cpuInfo = (cpuInfoList != null) ? cpuInfoList[this.cpuIndex] : null;
            cpuPercentage = (cpuPercentageList != null) ? cpuPercentageList[this.cpuIndex] : null;
        } catch (Exception e) {
            throw new SystemInfoException("Cannot refresh the native CPU information", e);
        } finally {
            sigar.close();
        }

        return;
    }
}
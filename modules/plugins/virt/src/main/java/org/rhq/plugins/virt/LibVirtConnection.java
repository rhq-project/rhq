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
package org.rhq.plugins.virt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a connection, via libVirt to domain management.
 *
 * @author Greg Hinkle
 */
public class LibVirtConnection {
    private LibVirt.ConnectionPointer connectionPointer;

    private Log log = LogFactory.getLog(LibVirtConnection.class);

    public LibVirtConnection() {
        this.connectionPointer = LibVirt.INSTANCE.virConnectOpen(null);
        if (this.connectionPointer == null) {
            log.warn("Couldn't created authorized access to libvirt, using read only access");
            this.connectionPointer = LibVirt.INSTANCE.virConnectOpenReadOnly(null);
        }
    }

    public List<String> getDomainNames() {
        // this works though I should really figure out how to allocate the Memory with the jna Memory thing
        String[] names = new String[100];
        for (int i = 0; i<100;i++) {
                names[i] = new String();
        }
        int code = LibVirt.INSTANCE.virConnectListDefinedDomains(connectionPointer, names, 100);
        List<String> results = new ArrayList<String>();

        for (int i = 0; i< code;i++) {
            results.add(names[i]);
//            DomainInfo info = getDomainInfo(names[i]);
//            printDomainInfo(info);
        }
        return results;
    }

    public static void main(String[] args) {
        LibVirtConnection c = new LibVirtConnection();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        LibVirt.INSTANCE.virConnectClose(connectionPointer);
    }


    public int[] getDomainIds() throws Exception {
        int[] ids = new int[100];
        int result = LibVirt.INSTANCE.virConnectListDomains(connectionPointer, ids, 100);
        if (result < 0) {
            throw new Exception("Couldn't list domain ids");
        }

        int[] found = new int[result];
        for (int i = 0; i < result; i++) {
            found[i] = ids[i];
        }

        return found;
    }

    public void printDomainInfo(DomainInfo domainInfo) {
        System.out.println("\tDOMAIN INFO: \n\tstate=" + String.valueOf(domainInfo.domainInfo.state) +
                "\n\tmem=" + domainInfo.domainInfo.maxMem.longValue() +
                "\n\tfree=" + domainInfo.domainInfo.memory.longValue() +
                "\n\tCPUs=" + domainInfo.domainInfo.nrVirtCpu +
                "\n\tcpu=" + domainInfo.domainInfo.cpuTime);
    }
    public DomainInfo getDomainInfo(String domainName) {
            LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);

            DomainInfo info = new DomainInfo();

            //                    PointerByReference ref = new PointerByReference(new Memory(10000));
            //                    result = libVirt.virDomainGetUUID(domainPointer, ref);
            //                    if (result >= 0)
            //                        info.uuid = ref.getPointer.getString(0)

            info.name = LibVirt.INSTANCE.virDomainGetName(domainPointer);

            LibVirt.VirDomainInfo domainInfo = new LibVirt.VirDomainInfo();
            int result = LibVirt.INSTANCE.virDomainGetInfo(domainPointer, domainInfo);
            info.domainInfo = domainInfo;

            return info;
        }


    public DomainInfo getDomainInfo(int id) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByID(connectionPointer, id);

        DomainInfo info = new DomainInfo();

        // This causes inconsistent crashes
        //                    PointerByReference ref = new PointerByReference(new Memory(10000));
        //                    result = libVirt.virDomainGetUUID(domainPointer, ref);
        //                    if (result >= 0)
        //                        info.uuid = ref.getPointer.getString(0)

        info.name = LibVirt.INSTANCE.virDomainGetName(domainPointer);

        LibVirt.VirDomainInfo domainInfo = new LibVirt.VirDomainInfo();
        int result = LibVirt.INSTANCE.virDomainGetInfo(domainPointer, domainInfo);
        info.domainInfo = domainInfo;

        return info;
    }

    public String getDomainXML(String domainName) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);

        String xml = LibVirt.INSTANCE.virDomainGetXMLDesc(domainPointer, 0);
        return xml;
    }

    public int domainReboot(String domainName) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        return LibVirt.INSTANCE.virDomainReboot(domainPointer, 0);
    }

    public int domainRestore(String toPath) {
        return LibVirt.INSTANCE.virDomainRestore(connectionPointer, toPath);
    }

    public int domainSave(String domainName, String toPath) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        return LibVirt.INSTANCE.virDomainSave(domainPointer, toPath);
    }

    public int domainResume(String domainName) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        return LibVirt.INSTANCE.virDomainResume(domainPointer);
    }

    public int domainShutdown(String domainName) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        return LibVirt.INSTANCE.virDomainShutdown(domainPointer);
    }

    public int domainSuspend(String domainName) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        return LibVirt.INSTANCE.virDomainSuspend(domainPointer);
    }

    public int domainCreate(String domainName) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        return LibVirt.INSTANCE.virDomainCreate(domainPointer);
    }

    public boolean defineDomain(String xml) {
        LibVirt.DomainPointer ptr = LibVirt.INSTANCE.virDomainDefineXML(connectionPointer, xml);
        return ptr != null;
    }

    public void setMaxMemory(String domainName, long size) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        int result = LibVirt.INSTANCE.virDomainSetMaxMemory(domainPointer, size);
        if (result < 0)
            throw new RuntimeException("Failed to set max memory");
    }

    public void setMemory(String domainName, long size) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        int result = LibVirt.INSTANCE.virDomainSetMemory(domainPointer, size);
        if (result < 0)
            throw new RuntimeException("Failed to set memory");

    }

    public void setVcpus(String domainName, int count) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        int result = LibVirt.INSTANCE.virDomainSetVcpus(domainPointer, count);
        if (result < 0)
            throw new RuntimeException("Failed to set vcpu count");

    }

    public LibVirt.VirDomainInterfaceStats getDomainInterfaceStats(String domainName, String path) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        LibVirt.VirDomainInterfaceStats stats = new LibVirt.VirDomainInterfaceStats();
        LibVirt.INSTANCE.virDomainInterfaceStats(domainPointer, path, stats, 8*8);
        return stats;
    }

    public LibVirt.VirDomainBlockStats getDomainBlockStats(String domainName, String path) {
        LibVirt.DomainPointer domainPointer = LibVirt.INSTANCE.virDomainLookupByName(connectionPointer, domainName);
        LibVirt.VirDomainBlockStats stats = new LibVirt.VirDomainBlockStats();
        LibVirt.INSTANCE.virDomainBlockStats(domainPointer, path, stats, 8*5);
        return stats;
    }

    public static class DomainInfo {
        public int id;
        public String name;
        public String uuid;
        public LibVirt.VirDomainInfo domainInfo;
    }
}
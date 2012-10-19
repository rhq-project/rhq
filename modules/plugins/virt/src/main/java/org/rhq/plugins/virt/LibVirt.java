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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA interface mapping for libVirt.
 *
 * @author Greg Hinkle
 */
public interface LibVirt extends Library {
    LibVirt INSTANCE = (LibVirt) Native.loadLibrary("virt", LibVirt.class);

    // ---- Connect
    ConnectionPointer virConnectOpen(String domainName);

    ConnectionPointer virConnectOpenReadOnly(String domainName);

    int virConnectClose(ConnectionPointer connectionPointer);

    // ---- Domain lookup
    DomainPointer virDomainLookupByName(ConnectionPointer connectionPointer, String name);

    DomainPointer virDomainLookupByID(ConnectionPointer connectionPointer, int id);

    DomainPointer virDomainLookupByUUIDString(ConnectionPointer connectionPointer, String uuid);

    // ---- Domain info
    int virDomainGetUUID(DomainPointer domainPointer, PointerByReference uuid);

    int virDomainGetUUIDString(DomainPointer domainPointer, PointerByReference ref);

    String virDomainGetName(DomainPointer domainPointer);

    // ---- Domain information
    int virDomainGetInfo(DomainPointer domainPointer, VirDomainInfo info);

    int virConnectListDefinedDomains(ConnectionPointer connectionPointer, String[] names, int maxnames);

    int virConnectListDomains(ConnectionPointer connectionPointer, int[] ids, int maxids);

    String virDomainGetXMLDesc(DomainPointer domainPointer, int virDomainXMLFlags);

    // ---- Domain control
    int virDomainReboot(DomainPointer domainPointer, int flags);

    int virDomainRestore(ConnectionPointer connectionPointer, String fromPath);

    int virDomainSave(DomainPointer domain, String toPath);

    int virDomainResume(DomainPointer domainPointer);

    int virDomainSuspend(DomainPointer domainPointer);

    int virDomainShutdown(DomainPointer domainPointer);

    int virDomainCreate(DomainPointer domainPointer);

    DomainPointer virDomainDefineXML(ConnectionPointer connectionPointer, String xml);

    int	virDomainInterfaceStats(DomainPointer domainPointer, String path, VirDomainInterfaceStats stats, int size);

    int	virDomainBlockStats(DomainPointer domainPointer, String path, VirDomainBlockStats stats, int size);

    int virDomainSetMaxMemory(DomainPointer domainPointer, long size);

    int virDomainSetMemory(DomainPointer domainPointer, long size);

    int virDomainSetVcpus(DomainPointer domainPointer, int count);

    public static class ConnectionPointer extends PointerType {
    }

    public static class DomainPointer extends PointerType {
    }

    public static class VirDomainInfo extends Structure {
        public byte state; //the running state, one of virDomainFlag
        public NativeLong maxMem; //the maximum memory in KBytes allowed
        public NativeLong memory; //the memory in KBytes used by the domain
        public short nrVirtCpu; //the number of virtual CPUs for the doma
        public long cpuTime; //the CPU time used in nanoseconds
    }

    public static class VirDomainInterfaceStats extends Structure {
        public long rx_bytes;
        public long rx_packets;
        public long rx_errs;
        public long rx_drop;
        public long tx_bytes;
        public long tx_packets;
        public long tx_errs;
        public long tx_drop;
    }

    public static class VirDomainBlockStats extends Structure {

        public long rd_req; // number of read requests
        public long rd_bytes; // number of read bytes
        public long wr_req; // number of write requests
        public long wr_bytes; // number of written bytes
        public long errs; // In Xen this returns the mysterious 'oo_req'.

    }


    public enum virDomainXMLFlags {
        VIR_DOMAIN_XML_SECURE(1), // dump security sensitive informations too
        VIR_DOMAIN_XML_INACTIVE(2); // dump inactive domain informations

        public int index;

        virDomainXMLFlags(int index) {
            this.index = index;
        }
    }
}
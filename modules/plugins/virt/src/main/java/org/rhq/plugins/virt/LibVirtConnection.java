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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;

/**
 * Represents a connection, via libVirt to domain management.
 *
 * @author Greg Hinkle
 */
public class LibVirtConnection {

    private static int SUCCESS = 0;

    private Connect connection;

    private Log log = LogFactory.getLog(LibVirtConnection.class);

    public LibVirtConnection(String uri) throws LibvirtException {
        connection = new Connect(uri);
        if (connection == null) {
            log.warn("Can not obtain an instance of libvirt, trying read only");
            connection = new Connect(uri, true);
            if (connection == null) {
                log.warn("LIbvirt readonly access failed.");
            }
        }
        for (String i : connection.listDefinedInterfaces()) {
            System.out.println("i " + i);
        }
        for (String i : connection.listInterfaces()) {
            System.out.println("i2 " + i);
            System.out.println(connection.interfaceLookupByName(i).getXMLDescription(0));
        }
        for (String i : connection.listDefinedNetworks()) {
            System.out.println("n " + i);
        }
        for (String i : connection.listNetworks()) {
            System.out.println("n2 " + i);
            System.out.println(connection.networkLookupByName(i).getXMLDesc(0));
        }
    }

    public String getConnectionURI() throws LibvirtException {
        return connection.getURI();
    }

    public List<String> getDomainNames() throws LibvirtException {

        if (connection == null) {
            return new ArrayList<String>(); // Return empty list, so VirtualizationDiscovery will find no hosts.
        } else {
            return Arrays.asList(connection.listDefinedDomains());
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        connection.close();
    }

    public int[] getDomainIds() throws Exception {
        if (connection == null) {
            return new int[0]; // Return empty list, so VirtualizationDiscovery will find no hosts.
        } else {
            return connection.listDomains();
        }
    }

    public void printDomainInfo(DomainInfo domainInfo) {
        System.out.println("\tDOMAIN INFO: \n\tstate=" + String.valueOf(domainInfo.domainInfo.state) + "\n\tmem="
            + domainInfo.domainInfo.maxMem + "\n\tfree=" + domainInfo.domainInfo.memory + "\n\tCPUs="
            + domainInfo.domainInfo.nrVirtCpu + "\n\tcpu=" + domainInfo.domainInfo.cpuTime);
    }

    public DomainInfo getDomainInfo(String domainName) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);

        DomainInfo info = new DomainInfo();
        info.domainInfo = domain.getInfo();
        info.name = domainName;
        info.uuid = domain.getUUIDString();

        return info;
    }

    public DomainInfo getDomainInfo(int id) throws LibvirtException {
        Domain domain = connection.domainLookupByID(id);

        DomainInfo info = new DomainInfo();
        info.domainInfo = domain.getInfo();
        info.name = domain.getName();
        info.uuid = domain.getUUIDString();

        return info;
    }

    public String getDomainXML(String domainName) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        return domain.getXMLDesc(0);
    }

    public int domainReboot(String domainName) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.reboot(0);
        return SUCCESS;
    }

    public int domainRestore(String toPath) throws LibvirtException {
        connection.restore(toPath);
        return SUCCESS;
    }

    public int domainSave(String domainName, String toPath) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.save(toPath);
        return SUCCESS;
    }

    public int domainResume(String domainName) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.resume();
        return SUCCESS;
    }

    public int domainShutdown(String domainName) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.shutdown();
        return SUCCESS;
    }

    public int domainSuspend(String domainName) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.suspend();
        return SUCCESS;
    }

    public int domainCreate(String domainName) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.create();
        return SUCCESS;
    }

    public boolean defineDomain(String xml) throws LibvirtException {
        Domain dom = connection.domainDefineXML(xml);
        return dom != null;
    }

    public void setMaxMemory(String domainName, long size) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.setMaxMemory(size);
    }

    public void setMemory(String domainName, long size) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.setMemory(size);
    }

    public void setVcpus(String domainName, int count) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        domain.setVcpus(count);
    }

    public DomainInterfaceStats getDomainInterfaceStats(String domainName, String path) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        return domain.interfaceStats(path);
    }

    public DomainBlockStats getDomainBlockStats(String domainName, String path) throws LibvirtException {
        Domain domain = connection.domainLookupByName(domainName);
        return domain.blockStats(path);
    }

    public int close() throws LibvirtException {
        return connection.close();
    }

    public double getMemoryPercentage() throws LibvirtException {
        double memory = connection.nodeInfo().memory;
        double usedMemory = 0;
        for (int id : connection.listDomains()) {
            Domain domain = connection.domainLookupByID(id);
            usedMemory += domain.getInfo().memory;
        }
        return usedMemory / memory;
    }

    public long getCPUTime() throws LibvirtException {
        long cpuTime = 0;
        for (int id : connection.listDomains()) {
            Domain domain = connection.domainLookupByID(id);
            cpuTime += domain.getInfo().cpuTime;
        }
        return cpuTime;
    }

    public HVInfo getHVInfo() throws LibvirtException {
        HVInfo hvInfo = new HVInfo();
        hvInfo.libvirtVersion = connection.getLibVirVersion();
        hvInfo.hvType = connection.getType();
        hvInfo.uri = connection.getURI();
        hvInfo.version = connection.getVersion();
        hvInfo.hostname = connection.getHostName();
        hvInfo.nodeInfo = connection.nodeInfo();
        return hvInfo;
    }

    public static class DomainInfo {
        public int id;
        public String name;
        public String uuid;
        public org.libvirt.DomainInfo domainInfo;
    }

    public static class HVInfo {
        public long libvirtVersion;
        public String hvType;
        public String uri;
        public String hostname;
        public long version;
        public org.libvirt.NodeInfo nodeInfo;
    }

    public static void main(String args[]) throws Exception {
        //LibVirtConnection conn = new LibVirtConnection("qemu:///system");
        LibVirtConnection conn = new LibVirtConnection("");
        HVInfo hi = conn.getHVInfo();

        System.out.println("HV Version:" + hi.version);
        System.out.println("LV Version:" + hi.libvirtVersion);
        System.out.println("HV Type:" + hi.hvType);
        System.out.println("HV URI:" + hi.uri);
        for (int foo : conn.getDomainIds()) {
            System.out.println(foo);
            System.out.println(conn.connection.domainLookupByID(foo).interfaceStats(""));
        }
        for (String foo : conn.getDomainNames()) {
            System.out.println(foo);
            System.out.println(conn.connection.domainLookupByName(foo).getXMLDesc(0));
        }
    }
}
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

import com.sun.jna.Pointer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.jna.virError;

/**
 * Represents a connection, via libVirt to domain management.
 *
 * @author Greg Hinkle
 */
public class LibVirtConnection {

    private static int SUCCESS = 0;

    private Connect connection;

    private Log log = LogFactory.getLog(LibVirtConnection.class);

    private boolean connected = false;

    public LibVirtConnection(String uri) throws LibvirtException {
        try {
            Connect.setErrorCallback(Logger.INSTANCE);
            connection = new Connect(uri);
            connected = true;
            connection.setConnectionErrorCallback(Logger.INSTANCE);
        } catch (LibvirtException e) {
            log.warn("Can not obtain an instance of libvirt");
            connection = null;
            throw e;
            /*try {
                log.warn("Can not obtain an instance of libvirt, trying read only");
                connection = new Connect(uri, true);
                connection.setConnectionErrorCallback(Logger.INSTANCE);
                connected = true;
            } catch (LibvirtException ie) {
                throw ie;
            }*/
        } finally {
            Connect.setErrorCallback(null);
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
        if (connected) {
            connection.close();
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
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
        try {
            Domain domain = getDomain(domainName);
            try {
                DomainInfo info = new DomainInfo();
                info.domainInfo = domain.getInfo();
                info.name = domainName;
                info.uuid = domain.getUUIDString();

                return info;
            } finally {
                domain.free();
            }
        } catch (LibvirtException e) {
            log.error("Error looking up domain with name " + domainName, e);
            throw e;
        }
    }

    public DomainInfo getDomainInfo(int id) throws LibvirtException {
        try {
            Domain domain = connection.domainLookupByID(id);
            if (domain == null) {
                throw new IllegalArgumentException("No domain found with ID: " + id);
            }
            try {
                DomainInfo info = new DomainInfo();
                info.domainInfo = domain.getInfo();
                info.name = domain.getName();
                info.uuid = domain.getUUIDString();

                return info;
            } finally {
                domain.free();
            }
        } catch (LibvirtException e) {
            log.error("Error looking up domain with id " + id, e);
            throw e;
        }
    }

    public String getDomainXML(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            return domain.getXMLDesc(0);
        } finally {
            domain.free();
        }
    }

    public int domainReboot(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.reboot(0);
            return SUCCESS;
        } finally {
            domain.free();
        }
    }

    public int domainRestore(String toPath) throws LibvirtException {
        connection.restore(toPath);
        return SUCCESS;
    }

    public int domainDestroy(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.destroy();
            return SUCCESS;
        } finally {
            domain.free();
        }
    }

    public int domainDelete(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            DomainState state = domain.getInfo().state;
    
            if ((state != DomainState.VIR_DOMAIN_SHUTDOWN) && (state != DomainState.VIR_DOMAIN_SHUTOFF)) {
                domain.destroy();
            }
            domain.undefine();
            
            return SUCCESS;
        } finally {
            domain.free();            
        }
    }

    public int domainSave(String domainName, String toPath) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.save(toPath);
            return SUCCESS;
        } finally {
            domain.free();
        }
    }

    public int domainResume(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.resume();
            return SUCCESS;
        } finally {
            domain.free();
        }
    }

    public int domainShutdown(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.shutdown();
            return SUCCESS;
        } finally {
            domain.free();            
        }
    }

    public int domainSuspend(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.suspend();
            return SUCCESS;
        } finally {
            domain.free();
        }
    }

    public int domainCreate(String domainName) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.create();
            return SUCCESS;
        } finally {
            domain.free();
        }
    }

    public boolean defineDomain(String xml) throws LibvirtException {
        Domain dom = connection.domainDefineXML(xml);
        boolean returnValue = (dom != null);
        if (returnValue) {
            dom.free();
        }
        return returnValue;
    }

    public void setMaxMemory(String domainName, long size) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.setMaxMemory(size);
        } finally {
            domain.free();
        }
    }

    public void setMemory(String domainName, long size) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.setMemory(size);
        } finally {
            domain.free();
        }
    }

    public void setVcpus(String domainName, int count) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            domain.setVcpus(count);
        } finally {
            domain.free();
        }
    }

    public DomainInterfaceStats getDomainInterfaceStats(String domainName, String path) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            return domain.interfaceStats(path);
        } finally {
            domain.free();
        }
    }

    public DomainBlockStats getDomainBlockStats(String domainName, String path) throws LibvirtException {
        Domain domain = getDomain(domainName);
        try {
            return domain.blockStats(path);
        } finally {
            domain.free();
        }
    }

    public int close() throws LibvirtException {
        if (connected) {
            connected = false;
            return connection.close();
        } else {
            return 0;
        }
    }

    public double getMemoryPercentage() throws LibvirtException {
        double memory = connection.nodeInfo().memory;
        double usedMemory = 0;
        for (int id : connection.listDomains()) {
            Domain domain = connection.domainLookupByID(id);
            if (domain != null) {
                usedMemory += domain.getInfo().memory;
                domain.free();
            }
        }
        return usedMemory / memory;
    }

    public long getCPUTime() throws LibvirtException {
        long cpuTime = 0;
        for (int id : connection.listDomains()) {
            Domain domain = connection.domainLookupByID(id);
            if (domain != null) {
                cpuTime += domain.getInfo().cpuTime;
                domain.free();
            }
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

    public List<String> getNetworks() throws LibvirtException {
        if (connection == null) {
            return Arrays.asList(new String[0]);
        } else {
            return Arrays.asList(connection.listNetworks());
        }
    }

    public List<String> getDefinedNetworks() throws LibvirtException {
        if (connection == null) {
            return Arrays.asList(new String[0]);
        } else {
            return Arrays.asList(connection.listDefinedNetworks());
        }
    }

    public boolean isNetworkActive(String name) throws LibvirtException {
        return getNetworks().contains(name);
    }

    public NetworkInfo getNetwork(String name) throws LibvirtException {
        Network net = getLibVirtNetwork(name);
        
        try {
            NetworkInfo info = new NetworkInfo();
            info.name = net.getName();
            info.autostart = net.getAutostart();
            info.bridgeName = net.getBridgeName();
            return info;
        } finally {
            net.free();
        }
    }

    public String getNetworkXML(String name) throws LibvirtException {
        Network network = getLibVirtNetwork(name);
        try {
            return network.getXMLDesc(0);
        } finally {
            network.free();
        }
    }

    public void updateNetwork(String name, String xml, boolean autostart) throws LibvirtException {
        connection.networkDefineXML(xml);
        Network network = getLibVirtNetwork(name);
        try {
            network.setAutostart(autostart);
        } finally {
            network.free();
        }
    }

    public static class DomainInfo {
        public int id;
        public String name;
        public String uuid;
        public org.libvirt.DomainInfo domainInfo;
    }

    public static class NetworkInfo {
        public String name;
        public boolean autostart;
        public String bridgeName;
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
    
    private Domain getDomain(String domainName) throws LibvirtException, IllegalArgumentException {
        Domain ret = connection.domainLookupByName(domainName);
        if (ret == null) {
            throw new IllegalArgumentException("Could not find a domain called '" + domainName + "'.");
        }
        
        return ret;
    }
    
    private Network getLibVirtNetwork(String networkName) throws LibvirtException, IllegalArgumentException {
        Network ret = connection.networkLookupByName(networkName);
        if (ret == null) {
            throw new IllegalArgumentException("Could not find a netword called '" + networkName + "'.");
        }
        
        return ret;
    }
}

class Logger extends org.libvirt.ErrorCallback {

    // Make this static so the callback will always have an object
    // to reference. If the object is GC'ed a core dump will occur.
    public static Logger INSTANCE = new Logger();

    private Log log = LogFactory.getLog(Logger.class);

    @Override
    public void errorCallback(Pointer arg0, virError arg1) {
        log.warn("Libvirt Error: " + arg1.message);
    }
}
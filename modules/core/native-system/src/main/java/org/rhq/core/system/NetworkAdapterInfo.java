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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;

/**
 * Provides information on a network adapater.
 *
 * <p>Users of this object should expect possible <code>null</code> values for some of these fields, in the case when
 * some of the values are unknown or undetectable on the platform. For example, on platforms without native support, the
 * {@link #getMacAddressString() MAC address} will not be detectable and, since it is unknown, will be <code>
 * null</code>.</p>
 *
 * @author John Mazzitelli
 */
public class NetworkAdapterInfo {
    private final String name;
    private final String displayName;
    private final String description;
    private final String macAddress;
    private final String type;
    private final OperationState operationalStatus;
    private final Boolean dhcpEnabled;
    private final List<InetAddress> dnsServers;
    private List<InetAddress> unicastAddresses;
    private List<InetAddress> multicastAddresses;
    private long flags;

    public static enum OperationState {
        UP, DOWN, TESTING, UNKNOWN, DORMANT, NOTPRESENT, LOWERLAYERDOWN
    }

    public enum DisplayName {
        FROM_NAME,
        FROM_DESCRIPTION
    }

    public NetworkAdapterInfo(String name, String displayName, String description, String macAddress, String type,
        String operationalStatus, Boolean dhcpEnabled, List<InetAddress> dnsServers,
        List<InetAddress> unicastAddresses, List<InetAddress> multicastAddresses) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.macAddress = macAddress;
        this.type = type;
        this.operationalStatus = OperationState.valueOf(operationalStatus);
        this.dhcpEnabled = dhcpEnabled;
        this.dnsServers = dnsServers;
        this.unicastAddresses = unicastAddresses;
        this.multicastAddresses = multicastAddresses;
    }

    public NetworkAdapterInfo(NetInterfaceConfig a) {
        this(a, DisplayName.FROM_NAME);
    }

    public NetworkAdapterInfo(NetInterfaceConfig a, DisplayName displayName) {
        long flags = a.getFlags();
        NetworkAdapterInfo.OperationState state = NetworkAdapterInfo.OperationState.UP;
        if ((flags & NetFlags.IFF_UP) <= 0) {
            state = NetworkAdapterInfo.OperationState.DOWN;
        }

        this.name = a.getName();
        switch(displayName) {
            case FROM_DESCRIPTION:
                this.displayName = a.getDescription();
                break;
            case FROM_NAME:
            default:
                this.displayName = a.getName();
                break;
        }
        this.description = a.getDescription();
        this.macAddress = a.getHwaddr();
        this.type = a.getType();
        this.operationalStatus = state;
        this.dhcpEnabled = Boolean.FALSE;
        this.dnsServers = null; // TODO: DNS server addresses?
        this.flags = a.getFlags();
        try {
            this.unicastAddresses = Collections.singletonList(InetAddress.getByName(a.getAddress())); // TODO: can't sigar give us more than one?
        } catch (UnknownHostException e) {
        }

        try {
            this.multicastAddresses = Collections.singletonList(InetAddress.getByName(a.getBroadcast()));
        } catch (UnknownHostException e) {
        }
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getAllFlags() {
        return NetFlags.getIfFlagsString(flags);
    }

    public String getMacAddressString() {
        return macAddress;
    }

    public byte[] getMacAddressBytes() {
        if (macAddress == null) {
            return null;
        }

        byte[] bytes = new byte[6];
        String[] hex = macAddress.split("(\\:|\\-)");

        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address: " + macAddress);
        }

        for (int i = 0; i < 6; i++) {
            try {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hex digit [" + hex[i] + "] in MAC address: " + macAddress);
            }
        }

        return bytes;
    }

    public String getType() {
        return type;
    }

    public OperationState getOperationalStatus() {
        return operationalStatus;
    }

    public Boolean isDhcpEnabled() {
        return dhcpEnabled;
    }

    public List<InetAddress> getDnsServers() {
        return dnsServers;
    }

    public List<InetAddress> getUnicastAddresses() {
        return unicastAddresses;
    }

    public List<InetAddress> getMulticastAddresses() {
        return multicastAddresses;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("NetworkAdapterInfo: ");

        s.append("name=[" + this.name);
        s.append("], display-name=[" + this.displayName);
        s.append("], description=[" + this.description);
        s.append("], mac-address=[" + this.macAddress);
        s.append("], type=[" + this.type);
        s.append("], operational-status=[" + this.operationalStatus);
        s.append("], dhcp-enabled=[" + this.dhcpEnabled);
        s.append("], dns-servers=" + this.dnsServers);
        s.append(", unicast-addresses=" + this.unicastAddresses);
        s.append(", multicast-addresses=" + this.multicastAddresses);

        return s.toString();
    }
}
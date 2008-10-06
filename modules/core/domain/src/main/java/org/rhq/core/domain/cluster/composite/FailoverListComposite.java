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
package org.rhq.core.domain.cluster.composite;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Joseph Marques
 */
public class FailoverListComposite implements Iterator<FailoverListComposite.ServerEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    private List<ServerEntry> servers;
    private int nextIndex = 0;

    public static class ServerEntry {

        public final String address;
        public final int port;
        public final int securePort;

        public ServerEntry(String address, int port, int securePort) {
            super();
            this.address = address;
            this.port = port;
            this.securePort = securePort;
        }

        @Override
        public String toString() {
            // its very important that the format of this returned string looks like "address:port/securePort"
            return address + ":" + port + "/" + securePort;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ServerEntry))
                return false;

            ServerEntry se = (ServerEntry) obj;

            return (this.address.equals(se.address) && (this.port == se.port) && (this.securePort == se.securePort));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + port;
            result = prime * result + securePort;
            result = prime * result + ((address == null) ? 0 : address.hashCode());
            return result;
        }

    }

    public FailoverListComposite(List<ServerEntry> servers) {
        /*
         * make a new list populated with the immutable elements of the passed list, this
         * way if the passed list is later modified it doesn't indirectly modify this one;
         * also, put this new list into an unmodifiable wrapper, as this is intended to be
         * an immutable list, with read-only characteristics
         */
        this.servers = Collections.unmodifiableList(new ArrayList<ServerEntry>(servers));
    }

    public int size() {
        if (servers == null) {
            return 0;
        }
        return servers.size();
    }

    public boolean hasNext() {
        return (servers != null && servers.size() > 0);
    }

    public ServerEntry next() {
        if (!hasNext()) {
            return null;
        }

        ServerEntry nextOne = servers.get(nextIndex);

        nextIndex++;
        nextIndex %= servers.size();

        return nextOne;
    }

    /**
     * Same as {@link #next()} except this doesn't move the iterator
     * pointer forward.  Calling this method multiple times in a row
     * results in the same server being returned, unlike {@link #next()}.
     * If you call {@link #peek()} and then {@link #next()}, both will
     * return the same server.
     * 
     * @return the server entry that is next on the list to be returned (may be null)
     */
    public ServerEntry peek() {
        if (!hasNext()) {
            return null;
        }
        ServerEntry nextOne = servers.get(nextIndex);
        return nextOne;
    }

    public ServerEntry get(int index) {
        return servers.get(index);
    }

    /**
     * Call this method if you want the iterator to start over at the
     * first, topmost, server in the failover list.
     */
    public void resetIndex() {
        nextIndex = 0;
    }

    public void remove() {
        throw new IllegalAccessError(getClass().getSimpleName() + " are immutable lists, removal is disallowed");
    }

    public void print(PrintWriter writer) {
        for (int i = 0; i < size(); i++) {
            writer.println(servers.get(i) + (i == nextIndex ? " (next)" : ""));
        }
    }

    /**
     * Used to "serialize" this list in a human-readable form (useful for storing the list
     * in a text file that humans can read and potentially edit).
     * @return the list in a human readable format
     */
    public String writeAsText() {
        StringBuilder text = new StringBuilder();
        for (ServerEntry entry : servers) {
            if (text.length() > 0) {
                text.append("\n");
            }
            text.append(entry);
        }
        return text.toString();
    }

    /**
     * Factory method that takes text generated from a previous instance's {@link #writeAsText()} string.
     * 
     * @param text the failover list, in text form
     * 
     * @return a new instance of FailoverListComposite whose servers are found in <code>text</code>
     */
    public static FailoverListComposite readAsText(String text) {
        List<ServerEntry> servers = new ArrayList<ServerEntry>();

        StringTokenizer rowTokenizer = new StringTokenizer(text, "\n");
        while (rowTokenizer.hasMoreTokens()) {
            try {
                String row = rowTokenizer.nextToken();
                String[] addressPorts = row.split(":");
                String[] ports = addressPorts[1].split("/");
                servers.add(new ServerEntry(addressPorts[0], Integer.parseInt(ports[0]), Integer.parseInt(ports[1])));
            } catch (Exception e) {
                // why isn't this line valid? just ignore the bad line and continue on
            }
        }
        return new FailoverListComposite(servers);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(size());
        for (ServerEntry entry : servers) {
            out.writeUTF(entry.address);
            out.writeInt(entry.port);
            out.writeInt(entry.securePort);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int size = in.readInt();

        List<ServerEntry> entries = new ArrayList<ServerEntry>();
        for (int i = 0; i < size; i++) {
            String address = in.readUTF();
            int port = in.readInt();
            int securePort = in.readInt();
            entries.add(new ServerEntry(address, port, securePort));
        }

        /*
         * no need to wrap 'entries' in a new list before putting them into the immutable wrapper;
         * as long as the 'entries' reference does not escape this method, we can be assured that
         * 'servers' is correctly immutable.
         */
        servers = Collections.unmodifiableList(entries);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\nServerList:\n  ");
        for (ServerEntry server : this.servers) {
            sb.append(server.toString());
            sb.append("\n  ");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof FailoverListComposite))
            return false;

        FailoverListComposite flc = (FailoverListComposite) obj;

        return this.servers.equals(flc.servers);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((servers == null) ? 0 : servers.hashCode());
        return result;
    }

}

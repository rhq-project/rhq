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
        nextIndex++;
        nextIndex %= servers.size();
        return servers.get(nextIndex);
    }

    public ServerEntry get(int index) {
        return servers.get(index);
    }

    public void remove() {
        throw new IllegalAccessError(getClass().getSimpleName() + " are immutable lists, removal is disallowed");
    }

    public void print(PrintWriter writer) {
        for (int i = 0; i < size(); i++) {
            writer.println(servers.get(0) + (i == nextIndex ? " (active)" : ""));
        }
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

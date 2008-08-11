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

public class FailoverListComposite implements Iterator<FailoverListComposite.ServerEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    private List<ServerEntry> servers;
    private int nextIndex = 0;

    public static class ServerEntry {

        public final String address;
        public final int port;
        public final int securePort;

        public ServerEntry(String address, int port, int securePort) {
            this.address = address;
            this.port = port;
            this.securePort = securePort;
        }

        @Override
        public String toString() {
            return address + ":" + port + ":" + securePort;
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

}

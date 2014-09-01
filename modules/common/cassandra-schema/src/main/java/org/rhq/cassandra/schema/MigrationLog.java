package org.rhq.cassandra.schema;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An append only log that stores schedule ids in binary format. When data is successfully migrated, the schedule id
 * is written to the log.
 *
 * @author John Sanda
 */
public class MigrationLog {

    private File logFile;

    private DataOutputStream outputStream;

    public MigrationLog(File logFile) throws IOException {
        this.logFile = logFile;
        outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(logFile, true), 2048));
    }

    /**
     * Sequentially reads the log into a set. If the log is empty or does not yet exist, an empty set is returned.
     * This method does not support concurrent access.
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public Set<Integer> read() throws IOException {
        if (logFile.length() == 0) {
            return Collections.emptySet();
        }
        Set<Integer> scheduleIds = new HashSet<Integer>();
        DataInputStream inputStream = null;
        try {
            inputStream = new DataInputStream(new FileInputStream(logFile));
            long bytesRead = 0;
            while (bytesRead < logFile.length()) {
                scheduleIds.add(inputStream.readInt());
                bytesRead += 4;
            }

            return scheduleIds;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * Appends <code>scheduleId</code> to the log. Writes go to an internal buffer and are not immediately flushed to
     * disk. When the buffer fills up, it is automatically flushed. This method supports concurrent access.
     *
     * @throws IOException
     */
    public void write(int scheduleId) throws IOException {
        // Note that this method assumes concurrent writers. This is ok because
        // BufferedOutputStream.write is synchronized.
        outputStream.writeInt(scheduleId);
    }

    /**
     * Flushes any buffered writes and then closes the underlying output stream.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        outputStream.flush();
        outputStream.close();
    }
}

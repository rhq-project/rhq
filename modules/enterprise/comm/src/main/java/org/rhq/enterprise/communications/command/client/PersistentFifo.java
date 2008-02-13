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
package org.rhq.enterprise.communications.command.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.DumpBytes;
import org.rhq.enterprise.communications.util.StreamUtil;

/**
 * Persists byte arrays in a FIFO queue file. The file will grow to a limited, maximum size. If more entries are put on
 * the queue and those new entries cause the file to grow beyond the maximum file size, the file will be compressed and,
 * quite possibly, older entries will get deleted to shrink the file back down under a configured percentage of used
 * space.
 *
 * <p>This queue is implemented as a linked list with each entry in the file sequentially written (that is, the linked
 * list is actually a chain link running from start to end of the file - the next linked entry is always the next entry
 * in the file). Each list entry consists of a next pointer (a long) followed by a byte array that contains the entry's
 * true data. The next pointer is actually relative to the entry - meaning the next pointer is really the size of the
 * entry in bytes not including the size of the next pointer itself. If you know the file position of the next pointer
 * and the next pointer value itself, add the two together along with the size of the next pointer itself (a <code>
 * long</code> which is 8-bytes usually) and you get the file position of the next entry. We do it this way in order to
 * make the purging more efficient - since all the next pointers are relative to the actual position of the file (and we
 * know all entries are written sequentially in the file), we don't have to update the next pointers if we shift all
 * entries down in the file en masse.</p>
 *
 * <p>The first <code>long</code> in the file is the count of elements in the FIFO - this allows you to know the number
 * of entries in the FIFO without walking all the links to count along the way. The second <code>long</code> in the file
 * is the head pointer. The third <code>long</code> in the file is the tail pointer. If the head pointer is -1, the FIFO
 * queue is considered empty. If it is 0 or larger, it is the file pointer position within the file where the first
 * entry starts. When entries are put on the queue, they are added to the tail. When entries are taken from the queue,
 * they are removed from head.</p>
 *
 * <pre>
 * count | HEAD | TAIL | next | entry-byte-array-data | next | entry-byte-array-data | next | entry | EOF
 *          |      |     ^  |                           ^  |                           ^ ^
 *          |      |     |  |                           |  |                           | |
 *          |      |     |  +---------------------------+  +---------------------------+ |
 *          |      +-----|---------------------------------------------------------------+
 *          +------------+
 * </pre>
 *
 * @author John Mazzitelli
 */
public class PersistentFifo {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(PersistentFifo.class);

    private static final Object m_fileLock = PersistentFifo.class;

    private File m_file;
    private RandomAccessFile m_randomAccessFile;
    private long m_count; // the current count of entries in the FIFO
    private long m_head; // the position pointed to by the head pointer
    private long m_tail; // the position pointed to by the tail pointer
    private long m_countPosition; // the file position where the count is stored (i.e. the number of entries in the fifo)
    private long m_headPosition; // the file position where the head pointer can be found
    private long m_tailPosition; // the file position where the tail pointer can be found
    private long m_longSize; // the size in bytes that it takes to store a long number
    private long m_maxSizeBytes; // size of the file that, when reached, triggers a purge
    private long m_purgeResultMaxBytes; // the number of bytes the file must be less than after a purge
    private boolean m_compress; // will be true if we are to compress the data before persisting

    /**
     * A simple utility that dumps all the data found in the persistent FIFO to stdout.
     *
     * @param  args "filename [objects|bytes [compressed]]"
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        boolean objects = false;
        boolean compressed = false;
        int raw_bytes_base = -1;

        if (args.length > 1) {
            objects = "objects".equals(args[1]);
            compressed = ((args.length == 3) && "compressed".equals(args[2]));
            raw_bytes_base = (objects) ? 0 : DumpBytes.BASE_HEX;
        }

        dumpContents(new PrintWriter(System.out), file, compressed, raw_bytes_base);

        return;
    }

    /**
     * A simple utility that dumps all the data found in the persistent FIFO to the given stream. If the <code>
     * raw_byte_base</code> is <code>-1</code>, then only the number of entries is dumped - each individual entry is
     * not. If the <code>raw_byte_base</code> is <code>0</code>, then the data in the file is assumed to be serialized
     * objects and thus their <code>toString()</code> is dumped. Otherwise, a dump of each entry's raw byte array is
     * retrieved in the given <code>raw_byte_base</code>, where a base of 10 is for decimal, 16 is for hexidecimal, etc.
     *
     * @param  out           the stream to dump the output
     * @param  fifo_file     the FIFO file that contains 0 or more persisted entries
     * @param  compressed    if <code>true</code>, the entries will be assumed to be compressed in the file
     * @param  raw_byte_base if greater than 0, the raw entry data (i.e. the actual bytes) is dumped in this base (where
     *                       base=16 for hexidecimal for example; see {@link DumpBytes} for the various BASE constants:
     *                       {@link DumpBytes#BASE_HEX}, et. al.). 0 means dump entries as objects, -1 means do not dump
     *                       any entry data
     *
     * @throws IOException
     */
    public static void dumpContents(PrintWriter out, File fifo_file, boolean compressed, int raw_byte_base)
        throws IOException {
        PersistentFifo fifo = new PersistentFifo(fifo_file, Long.MAX_VALUE, 99, compressed);

        out.println(fifo_file);
        out.println(fifo.count());
        out.flush();

        // don't bother to continue, return immediately if caller only wanted to see the number of entries
        if (raw_byte_base < 0) {
            return;
        }

        RandomAccessFile raf = fifo.getRandomAccessFile();
        boolean last_entry = (fifo.m_head == -1L); // if head is -1, there are no entries
        long entry_num = 0;
        byte[] entry;

        // go to the first entry
        if (!last_entry) {
            raf.seek(fifo.m_head);
        }

        while (!last_entry) {
            // get the next pointer; if this is the last entry, then we'll read to the end of the file
            long next = raf.readLong();
            if (next == -1) {
                next = raf.length() - raf.getFilePointer();
                last_entry = true;
            }

            // we can now determine the size of the entry to read - go from current position to the next pointer
            int entry_size = (int) next;
            entry = new byte[entry_size];

            // fully read in the entry
            raf.readFully(entry);

            if (fifo.m_compress) {
                entry = fifo.decompress(entry);
            }

            String entry_string;

            out.print("[" + entry_num++ + "] ");

            if (raw_byte_base == 0) {
                Object obj = StreamUtil.deserialize(entry);
                entry_string = obj.toString();
            } else {
                out.println();

                switch (raw_byte_base) {
                case DumpBytes.BASE_HEX: {
                    entry_string = DumpBytes.dumpHexData(entry);
                    break;
                }

                case DumpBytes.BASE_DEC: {
                    entry_string = DumpBytes.dumpDecData(entry);
                    break;
                }

                case DumpBytes.BASE_OCT: {
                    entry_string = DumpBytes.dumpOctData(entry);
                    break;
                }

                case DumpBytes.BASE_BIN: {
                    entry_string = DumpBytes.dumpBinData(entry);
                    break;
                }

                default: {
                    entry_string = DumpBytes.dumpData(entry, 7, raw_byte_base);
                }
                }
            }

            out.println(entry_string);
        }

        out.flush();

        return;
    }

    /**
     * Creates a new {@link PersistentFifo} object. The <code>max_size_bytes</code> indicates the maximum size this file
     * is allowed to grow before a purge is triggered. If this threshold is crossed (that is, if the file grows larger
     * than the maximum size allowed), the file is compressed and, if needed, the oldest entries in the queue will get
     * deleted to make room for new entries. The amount of space purged will be enough to lower the used space
     * percentage down to <code>purge_percentage</code> or less.
     *
     * @param  file             the file containing the FIFO data
     * @param  max_size_bytes   the maximum size, in bytes, the persistent file is allowed to grow before a purge is
     *                          triggered
     * @param  purge_percentage when a purge is triggered, it will free up enough space to lower the amount of used
     *                          space down to this percentage of the total max space
     * @param  compress         if <code>true</code>, the data spooled to the file should be compressed
     *
     * @throws IOException              if the file does not exist but cannot be created
     * @throws IllegalArgumentException if purge_percentage is not between 0 and 99 or max_size_bytes is less than 1000
     */
    public PersistentFifo(File file, long max_size_bytes, int purge_percentage, boolean compress) throws IOException {
        if ((purge_percentage < 0) || (purge_percentage > 99)) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.INVALID_PURGE_PERCENTAGE,
                purge_percentage));
        }

        if (max_size_bytes < 1000L) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.INVALID_MAX_SIZE, max_size_bytes,
                1000));
        }

        m_file = file;
        m_purgeResultMaxBytes = (int) (max_size_bytes * (purge_percentage / 100.0f));
        m_maxSizeBytes = max_size_bytes;
        m_compress = compress;

        synchronized (m_fileLock) {
            // if file doesn't exist or is virtually empty
            if (!m_file.exists() || (m_file.length() < 8)) {
                initializeEmptyFile();
            } else {
                // file exists, let's get our count, head and tail pointers and their positions
                RandomAccessFile raf = getRandomAccessFile();

                // count is first, head is second, tail is third
                m_countPosition = 0L;
                readCount(raf);
                m_headPosition = raf.getFilePointer();
                readHead(raf);
                m_tailPosition = raf.getFilePointer();
                readTail(raf);
            }

            // this is me being paranoid - rather than hardcode the obvious value of 8, let's calculate it in case for some strange
            // reason, the value is different on some platform
            m_longSize = m_tailPosition - m_headPosition;
        }

        return;
    }

    /**
     * Puts the given Object in the FIFO queue. This method will attempt to serialize the object and store the
     * serialized bytes via {@link #put(byte[])}. An exception will occur if the serialization fails.
     *
     * @param  o the object to serialize and put in the FIFO queue
     *
     * @throws IOException      if failed to put the data in the file
     * @throws RuntimeException if failed to serialize the data
     */
    public void putObject(Serializable o) throws IOException, RuntimeException {
        byte[] serialized_bytes = StreamUtil.serialize(o);
        put(serialized_bytes);
        return;
    }

    /**
     * Takes an object from the FIFO, deserializes it and returns it.
     *
     * @return the object that was taken from the FIFO and deserialized
     *
     * @throws IOException      if failed to access the file
     * @throws RuntimeException if failed to deserialize the object after taking its serialized bytes off the FIFO queue
     */
    public Object takeObject() throws IOException, RuntimeException {
        Object o = null;

        byte[] serialized_bytes = take();
        if (serialized_bytes != null) {
            o = StreamUtil.deserialize(serialized_bytes);
        }

        return o;
    }

    /**
     * Puts an array of bytes on the FIFO queue
     *
     * @param  bytes the data to put in the queue
     *
     * @throws IOException if failed to access the file
     */
    public void put(byte[] bytes) throws IOException {
        if (m_compress) {
            bytes = compress(bytes);
        }

        synchronized (m_fileLock) {
            RandomAccessFile raf = getRandomAccessFile();
            long new_entry_pos = raf.length(); // where the new entry will start
            boolean is_first_entry = (m_head == -1);

            // bump up the count
            writeCount(raf, ++m_count);

            if (is_first_entry) {
                // this will be the first item in the queue, set the head to indicate we have data now
                writeHead(raf, new_entry_pos);
            } else {
                // update the previous entry's next pointer
                raf.seek(m_tail);
                raf.writeLong(new_entry_pos - m_tail - m_longSize);
            }

            // go to the new entry's position and write its next pointer and the data
            raf.seek(new_entry_pos);
            raf.writeLong(-1L); // sets the next pointer to indicate this is the last entry
            raf.write(bytes);

            // finally, update the tail
            writeTail(raf, new_entry_pos);

            // if we went over the maximum file size limit, start purging some entries to make room
            if (raf.length() > m_maxSizeBytes) {
                purge(raf, raf.length() - m_purgeResultMaxBytes);
            }
        }

        return;
    }

    /**
     * Takes the next entry from the queue and returns it.
     *
     * @return the next entry from the queue, or <code>null</code> if the queue is empty
     *
     * @throws IOException
     */
    public byte[] take() throws IOException {
        synchronized (m_fileLock) {
            // return immediately if there are no entries in the queue
            if (m_head == -1L) {
                return null;
            }

            RandomAccessFile raf = getRandomAccessFile();
            boolean last_entry = false;
            byte[] entry;

            // go to the first entry
            raf.seek(m_head);

            // get the next pointer; if this is the last entry, then we'll read to the end of the file
            long next = raf.readLong();
            if (next == -1) {
                next = raf.length() - raf.getFilePointer();
                last_entry = true;
            }

            // we can now determine the size of the entry to read - go from current position to the next pointer
            int entry_size = (int) next;
            entry = new byte[entry_size];

            // fully read in the entry
            raf.readFully(entry);

            if (last_entry) {
                // this was the last entry - let's shrink the file down to its minimal size
                initializeEmptyFile();
            } else {
                // move the head to the next entry
                writeHead(raf, m_head + next + m_longSize);

                // decrement the count
                writeCount(raf, --m_count);
            }

            if (m_compress) {
                entry = decompress(entry);
            }

            return entry;
        }
    }

    /**
     * Returns <code>true</code> if the file does not contain any entries in the queue.
     *
     * @return <code>true</code> if the queue is empty, <code>false</code> if at least one entry can be taken from the
     *         queue.
     *
     * @throws IOException if failed to access the file
     */
    public boolean isEmpty() throws IOException {
        synchronized (m_fileLock) {
            return readHead(getRandomAccessFile()) == -1;
        }
    }

    /**
     * Returns the number of entries currently in the FIFO.
     *
     * @return the number of entries
     *
     * @throws IOException if failed to access the file
     */
    public long count() throws IOException {
        synchronized (m_fileLock) {
            return readCount(getRandomAccessFile());
        }
    }

    /**
     * This initializes the file to indicate that the queue is empty - call this when the file does not yet exist or if
     * you want to shrink the file down to its minimal size.
     *
     * @throws IOException
     */
    public void initializeEmptyFile() throws IOException {
        synchronized (m_fileLock) {
            m_count = 0L;
            m_head = -1L; // -1 means the FIFO queue is empty
            m_tail = -1L;

            RandomAccessFile raf = getRandomAccessFile();

            // the first number in the file is the count
            m_countPosition = 0L;
            raf.seek(m_countPosition);
            raf.writeLong(m_count);

            // the second number in the file is the head pointer
            m_headPosition = raf.getFilePointer();
            raf.seek(m_headPosition);
            raf.writeLong(m_head);

            // the third number in the file is the tail pointer
            m_tailPosition = raf.getFilePointer();
            raf.writeLong(m_tail);

            // set the file length to right after the tail pointer thus shrinking the file size down to its minimal size
            raf.setLength(raf.getFilePointer());
        }

        return;
    }

    /**
     * This purges the file by compressing the old entries that have already been taken and, if that isn't possible,
     * will remove head entries (i.e. the oldest entries) and moves all items down in the file, thus shrinking the file.
     * The number of bytes to free up (that is, the number of bytes to shrink the file by) is determined by the <code>
     * bytes_to_purge</code> parameter. The file will be shrink by at least (but maybe more) that number of bytes.
     *
     * @param  raf            the file
     * @param  bytes_to_purge the minimum amount of bytes to purge
     *
     * @throws IOException if failed to access file
     */
    private void purge(RandomAccessFile raf, long bytes_to_purge) throws IOException {
        // first see if there are any unused bytes in the beginning of the file (entries that have already been taken from the queue
        // but not yet deleted from the file)
        long first_data_byte = m_tailPosition + m_longSize;
        long unused_bytes = m_head - first_data_byte;

        while (unused_bytes < bytes_to_purge) {
            // not enough unused bytes - we must sacrifice the oldest entry (at the head) to free up some more space
            take();

            if (m_head == -1) {
                return; // nothing left to purge
            }

            unused_bytes = m_head - first_data_byte;
        }

        // now move all the entries down to the beginning of the file; need to move in chunks so as not to override the data we want to keep
        // we start at the head and read in chunks to move down
        // chunk = the size of each full chunk that we move
        // total_bytes_to_move = the size of all the data we are moving down
        // num_full_chunks = the total number of fully filled chunks to be moved
        // final_chunk_size = the size of the last chunk to be moved, may be a partially filled chunk
        // cur_pos_to_read = the file position of the current chunk we are reading in
        // cur_pos_to_write = the file position where the current chunk will be moved to
        byte[] chunk = new byte[(int) unused_bytes];
        long total_bytes_to_move = raf.length() - first_data_byte - unused_bytes;
        long num_full_chunks = total_bytes_to_move / unused_bytes;
        long final_chunk_size = total_bytes_to_move % unused_bytes;
        long cur_pos_to_read = m_head;
        long cur_pos_to_write = first_data_byte;

        for (int i = 0; i < num_full_chunks; i++) {
            raf.seek(cur_pos_to_read);
            raf.readFully(chunk);
            raf.seek(cur_pos_to_write);
            raf.write(chunk);

            cur_pos_to_read += chunk.length;
            cur_pos_to_write += chunk.length;
        }

        // do the last, partial chunk
        if (final_chunk_size > 0) {
            raf.seek(cur_pos_to_read);
            raf.readFully(chunk, 0, (int) final_chunk_size);
            raf.seek(cur_pos_to_write);
            raf.write(chunk, 0, (int) final_chunk_size);
        }

        // we've purged and compacted the file - now let's shrink the file so its smaller
        raf.setLength(raf.getFilePointer());

        // fix our head and tail pointers by moving them back the number of bytes we just purged
        writeHead(raf, first_data_byte);
        writeTail(raf, m_tail - unused_bytes);

        return;
    }

    /**
     * Returns the <code>RandomAccessFile</code> object representation of the file that enables the caller to read
     * and/or write the file using random access.
     *
     * @return the file that can be randomly accessed
     *
     * @throws FileNotFoundException
     */
    private RandomAccessFile getRandomAccessFile() throws FileNotFoundException {
        if (m_randomAccessFile == null) {
            m_randomAccessFile = new RandomAccessFile(m_file, "rw");
        }

        return m_randomAccessFile;
    }

    /**
     * Reads and returns the count - this is the number of entries currently in the FIFO.
     *
     * @param  raf the file
     *
     * @return the number of entries currently in the FIFO
     *
     * @throws IOException if failed to access the file
     */
    private long readCount(RandomAccessFile raf) throws IOException {
        raf.seek(m_countPosition);
        m_count = raf.readLong();
        return m_count;
    }

    /**
     * Writes the given count to the file.
     *
     * @param  raf   the file
     * @param  count the new count to store
     *
     * @throws IOException if failed to write to the file
     */
    private void writeCount(RandomAccessFile raf, long count) throws IOException {
        raf.seek(m_countPosition);
        raf.writeLong(count);
        m_count = count;
        return;
    }

    /**
     * Reads the head pointer from the file.
     *
     * @param  raf the file
     *
     * @return the current position that the head pointer points to
     *
     * @throws IOException if failed to access the file
     */
    private long readHead(RandomAccessFile raf) throws IOException {
        raf.seek(m_headPosition);
        m_head = raf.readLong();
        return m_head;
    }

    /**
     * Writes the head pointer to the file.
     *
     * @param  raf      the file
     * @param  head_pos the new position of the head pointer
     *
     * @throws IOException if failed to write to the file
     */
    private void writeHead(RandomAccessFile raf, long head_pos) throws IOException {
        raf.seek(m_headPosition);
        raf.writeLong(head_pos);
        m_head = head_pos;
        return;
    }

    /**
     * Reads the tail pointer from the file.
     *
     * @param  raf the file
     *
     * @return the current position that the tail pointer points to
     *
     * @throws IOException if failed to access the file
     */
    private long readTail(RandomAccessFile raf) throws IOException {
        raf.seek(m_tailPosition);
        m_tail = raf.readLong();
        return m_tail;
    }

    /**
     * Writes the tail pointer to the file pointing to the last entry in the queue.
     *
     * @param  raf      the file
     * @param  tail_pos the new position of the tail pointer
     *
     * @throws IOException if failed to write to the file
     */
    private void writeTail(RandomAccessFile raf, long tail_pos) throws IOException {
        raf.seek(m_tailPosition);
        raf.writeLong(tail_pos);
        m_tail = tail_pos;
        return;
    }

    /**
     * Given an uncompressed byte array, the compressed bytes will be returned.
     *
     * @param  bytes uncompressed bytes
     *
     * @return compressed bytes
     *
     * @throws IOException if failed to compress the bytes
     */
    private byte[] compress(byte[] bytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
        GZIPOutputStream gzip = new GZIPOutputStream(baos);

        gzip.write(bytes);
        gzip.close();
        bytes = baos.toByteArray();
        baos = null;

        return bytes;
    }

    /**
     * Given a byte array that is compressed, the decompressed bytes will be returned.
     *
     * @param  entry the compressed bytes
     *
     * @return the decompressed bytes
     *
     * @throws IOException if failed to decompress the bytes
     */
    private byte[] decompress(byte[] entry) throws IOException {
        ByteArrayOutputStream decompressed = new ByteArrayOutputStream(entry.length);
        ByteArrayInputStream in = new ByteArrayInputStream(entry);
        GZIPInputStream gzip_in = new GZIPInputStream(in);

        StreamUtil.copy(gzip_in, decompressed);
        entry = decompressed.toByteArray();

        return entry;
    }
}
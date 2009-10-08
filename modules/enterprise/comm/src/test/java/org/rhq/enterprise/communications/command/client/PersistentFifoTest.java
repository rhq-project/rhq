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

import java.io.File;
import org.testng.annotations.Test;

/**
 * Tests the persistent fifo.
 *
 * @author John Mazzitelli
 */
@Test(groups = "comm-client")
public class PersistentFifoTest {
    /**
     * Tests putting and taking from fifo.
     */
    public void testFifoCompressed() {
        doFifoTests(true);
    }

    /**
     * Tests putting and taking from fifo.
     */
    public void testFifo() {
        doFifoTests(false);
    }

    /**
     * Tests putting and taking from fifo with either compressed or uncompressed data.
     *
     * @param compress wheter or not to test with compression on or off
     */
    private void doFifoTests(boolean compress) {
        print("Test with compression " + (compress ? "on" : "off"));

        String tmpDir = System.getProperty("java.io.tmpdir");
        File fifoFile = new File(tmpDir, "TEST.data");

        try {
            PersistentFifo fifo = new PersistentFifo(fifoFile, 1000L, 0, compress);

            // start fresh
            if (fifo.count() > 0) {
                fifo.initializeEmptyFile();
            }

            assert fifo.take() == null : "SHOULD NOT BE ABLE TO TAKE";
            assert fifo.isEmpty() : "SHOULD BE EMPTY";
            assert fifo.count() == 0 : "SHOULD BE EMPTY - count should be 0";

            fifo.put("Mazz Was Here 1".getBytes());

            assert !fifo.isEmpty() : "SHOULD NOT BE EMPTY";
            assert fifo.count() == 1 : "count should be 1";

            print(new String(fifo.take()));

            assert fifo.take() == null : "SHOULD NOT BE ABLE TO TAKE";
            assert fifo.isEmpty() : "SHOULD BE EMPTY";
            assert fifo.count() == 0 : "count should be 0";

            fifo.put("Mazz Was Here 2!".getBytes());
            fifo.put("Mazz Was Here 3!!".getBytes());
            fifo.put("Mazz Was Here 4!!!".getBytes());

            assert fifo.count() == 3 : "count should be 3";

            print(new String(fifo.take()));
            print(new String(fifo.take()));
            print(new String(fifo.take()));

            assert fifo.take() == null : "SHOULD NOT BE ABLE TO TAKE";
            assert fifo.isEmpty() : "SHOULD BE EMPTY";
            assert fifo.count() == 0 : "count should be 0";

            fifo.put("Mazz Was Here 5 to be deleted".getBytes());
            fifo.put("Mazz Was Here 6 to be deleted!".getBytes());
            fifo.put("Mazz Was Here 7 to be deleted!!".getBytes());
            fifo.initializeEmptyFile();

            assert fifo.take() == null : "SHOULD NOT BE ABLE TO TAKE";
            assert fifo.isEmpty() : "SHOULD BE EMPTY";
            assert fifo.count() == 0 : "count should be 0";

            fifo.put("Mazz Was Here 8!".getBytes());
            fifo.put("Mazz Was Here 9!!".getBytes());
            fifo.put("Mazz Was Here 10!!!".getBytes());

            print(new String(fifo.take()));
            print(new String(fifo.take()));

            fifo.put("Mazz Was Here 11!!!!".getBytes());

            print(new String(fifo.take()));
            print(new String(fifo.take()));

            assert fifo.take() == null : "SHOULD NOT BE ABLE TO TAKE";
            assert fifo.isEmpty() : "SHOULD BE EMPTY";
            assert fifo.count() == 0 : "count should be 0";

            for (int i = 0; i < 50; i++) {
                fifo.put(("Mazz was here " + i).getBytes());
            }

            byte[] data;
            while ((data = fifo.take()) != null) {
                print(new String(data));
            }

            assert fifo.take() == null : "SHOULD NOT BE ABLE TO TAKE";
            assert fifo.isEmpty() : "SHOULD BE EMPTY";
            assert fifo.count() == 0 : "count should be 0";

            // test with a 1MB file
            fifo = new PersistentFifo(fifoFile, 1000000L, 75, compress);
            for (int i = 0; i < 50000; i++) {
                if ((i % 1000) == 0) {
                    print("-->" + i);
                }

                fifo.put(("0123456789012345678901234567890123456789x-" + i).getBytes());
            }

            int num = 0;
            byte[] last_good = null;
            while ((data = fifo.take()) != null) {
                last_good = data;
                if ((num++ % 1000) == 0) {
                    print("Took: " + new String(data));
                }
            }

            assert fifo.take() == null : "SHOULD NOT BE ABLE TO TAKE";
            assert fifo.isEmpty() : "SHOULD BE EMPTY";
            assert fifo.count() == 0 : "count should be 0";

            print("Last = " + new String(last_good));

            // test putting/taking *object* in a 100KB file
            fifo = new PersistentFifo(fifoFile, 100000L, 75, compress);
            for (int i = 0; i < 5000; i++) {
                if ((i % 1000) == 0) {
                    print("-->" + i);
                }

                fifo.putObject("0123456789012345678901234567890123456789x-" + i);
            }

            num = 0;
            String last_good_string = null;
            String data_string;
            while ((data_string = (String) fifo.takeObject()) != null) {
                last_good_string = data_string;
                if ((num++ % 1000) == 0) {
                    print("Took: " + data_string);
                }
            }

            print("Last = " + last_good_string);

            // test putting primitive array in queue
            fifo.putObject(new float[] { 0.0f, 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

            float[] floats = (float[]) fifo.takeObject();
            if ((Float.floatToIntBits(floats[0]) != Float.floatToIntBits(0.0f))
                || (Float.floatToIntBits(floats[1]) != Float.floatToIntBits(1.1f))
                || (Float.floatToIntBits(floats[2]) != Float.floatToIntBits(2.2f))
                || (Float.floatToIntBits(floats[3]) != Float.floatToIntBits(3.3f))
                || (Float.floatToIntBits(floats[4]) != Float.floatToIntBits(4.4f))
                || (Float.floatToIntBits(floats[5]) != Float.floatToIntBits(5.5f))) {
                throw new RuntimeException("Failed to properly put/take object");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            fifoFile.delete();
        }
    }

    /**
     * In case something goes wrong in this test, we can manually enable it to dump things to stdout here.
     *
     * @param obj object whose toString will be dumped to stdout
     */
    private void print(Object obj) {
        boolean enable_print = Boolean.getBoolean("PersistentFifoTest.print");
        if (enable_print) {
            System.out.println(obj);
        }
    }
}
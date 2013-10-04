package org.rhq.devUtil.zipCheck;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Util to check path lengths inside zip files, as especially windows has issues with long paths
 * @author Heiko W. Rupp
 */
public class ZipCheck {

    private static final int WARN_LENGTH = 245;

    public static void main(String[] args) throws Exception {

        if (args.length<1) {
            System.err.println("Usage: ZipCheck <file.zip> [warnLength]");
            System.exit(1);
        }

        int warnLength = WARN_LENGTH;
        if (args.length == 2) {
                warnLength = Integer.valueOf( args[1] );
        }

        ZipFile file = new ZipFile(args[0]);
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.length()> warnLength) {
                System.err.printf("Long path: %4d : %s\n", name.length(), name);
            }
        }
        file.close();
    }
}

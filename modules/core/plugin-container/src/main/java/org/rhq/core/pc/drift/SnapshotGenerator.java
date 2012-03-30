package org.rhq.core.pc.drift;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.rhq.core.pc.configuration.ConfigurationCheckExecutor;
import org.rhq.core.util.MessageDigestGenerator;

import static java.io.File.separator;

public class SnapshotGenerator extends DirectoryWalker {

    private final Log log = LogFactory.getLog(ConfigurationCheckExecutor.class);

    private MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    private File snapshotDir;

    /** @param dir The directory to which snapshot files will be written. */
    public void setSnapshotDir(File dir) {
        snapshotDir = dir;
    }

    /**
     * Generates snapshot data and meta data files for the specified resource id starting at <code>basedir</code>. The
     * files are written to the directory specified in {@link #setSnapshotDir(java.io.File)}. The files are currently
     * stored as snapshot_dir/<resourceId>-snapshot.zip and snapshot_dir/<resourceId>_snapshot_metadata.txt.
     *
     * @param resourceId The id of the resource for which the snapshot is being created
     * @param basedir The root directory from which the snapshot will be taken
     * @return A {@link SnapshotHandle} that points the generated files on disk.
     * @throws IOException If any errors occur
     */
    public SnapshotHandle generateSnapshot(int resourceId, File basedir) throws IOException {
        List<File> files = new ArrayList<File>();
        walk(basedir, files);

        File metadatFile = new File(snapshotDir, resourceId + "_snapshot_metadata.txt");
        PrintWriter metadataWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(metadatFile)));
        File zipFile = new File(snapshotDir, resourceId + "-snapshot" + ".zip");
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            for (File file : files) {
                String relativePath = relativePath(basedir, file);
                metadataWriter.println(relativePath + " " + sha256(file));
                InputStream istream = null;
                try {
                    istream = new BufferedInputStream(new FileInputStream(new File(basedir.getParent(), relativePath)));
                    ZipEntry entry = new ZipEntry(relativePath);
                    zos.putNextEntry(entry);
                    IOUtils.copy(istream, zos);
                } catch (IOException e) {
                    log.error("Failed to add file " + file + " to zipfile " + zipFile + ".", e);
                } finally {
                    if (istream != null) {
                        istream.close();
                    }
                }
            }
        } finally {
            if (metadataWriter != null) {
                metadataWriter.close();
            }

            if (zos != null) {
                zos.close();
            }
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        IOUtils.copy(new FileInputStream(zipFile), byteStream);

        return new SnapshotHandle(zipFile, metadatFile);
    }

    private String relativePath(File basedir, File file) {
        return FilenameUtils.getName(basedir.getAbsolutePath()) + separator +
            file.getAbsolutePath().substring(basedir.getAbsolutePath().length() + 1);
    }

    private String sha256(File file) throws IOException {
        return digestGenerator.calcDigestString(file);
    }

    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        return true;
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        results.add(file);
    }
}

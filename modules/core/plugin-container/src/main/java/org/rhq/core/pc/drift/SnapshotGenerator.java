package org.rhq.core.pc.drift;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.rhq.core.domain.drift.Snapshot;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;

import static java.io.File.pathSeparatorChar;
import static java.io.File.separator;

public class SnapshotGenerator extends DirectoryWalker {

    private MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);


    public Snapshot generateSnapshot(File basedir) throws IOException {
        List<File> files = new ArrayList<File>();
        walk(basedir, files);

        Snapshot snapshot = new Snapshot();
        Map<String, String> metadata = snapshot.getMetadata();

        String snapshotName = "snapshot_" + System.currentTimeMillis();
        File snapshotDir = new File(System.getProperty("java.io.tmpdir"), "." + snapshotName);
        File snapshotRootDir = new File(snapshotDir, basedir.getName());
        snapshotRootDir.mkdirs();

        File zipFile = new File(System.getProperty("java.io.tmpdir"), snapshotName + ".zip");
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            for (File file : files) {
                metadata.put(file.getPath(), digestGenerator.calcDigestString(file.toURI().toURL()));
                String relativePath = relativePath(basedir, file);
                InputStream istream = null;
                try {
                    istream = new BufferedInputStream(new FileInputStream(new File(basedir.getParent(), relativePath)));
                    ZipEntry entry = new ZipEntry(relativePath);
                    zos.putNextEntry(entry);
                    IOUtils.copy(istream, zos);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (istream != null) {
                        istream.close();
                    }
                }
            }
        } finally {
            if (zos != null) {
                zos.close();
            }
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        IOUtils.copy(new FileInputStream(zipFile), byteStream);

        snapshot.setData(byteStream.toByteArray());

        return snapshot;
    }

    private String relativePath(File basedir, File file) {
        return FilenameUtils.getBaseName(basedir.getAbsolutePath()) + separator +
            file.getAbsolutePath().substring(basedir.getAbsolutePath().length() + 1);
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

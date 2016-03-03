/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.patching;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;

import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.wildfly10.helper.ServerPluginConfiguration;

/**
* @author Lukas Krejci
* @since 4.13
*/
final class MetadataFiles {
    final File[] files;
    final File baseDir;

    private MetadataFiles(File baseDir, File[] files) {
        this.baseDir = baseDir;
        this.files = files;
    }

    boolean exists() {
        for (File f : files) {
            if (!f.exists()) {
                return false;
            }
        }

        return files.length > 0;
    }

    void delete() {
        FileUtil.purge(baseDir, true);

        File destinationNameFile = getNameFile();

        //noinspection ResultOfMethodCallIgnored
        destinationNameFile.delete();

        File activeFile = getActiveMarkerFile();

        if (activeFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            activeFile.delete();
        }
    }

    static Result<MetadataFiles> forDeployment(BundleResourceDeployment rd, Configuration referencedConfiguration) {
        File destinationDir = baseDirFor(rd, referencedConfiguration);

        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            return Result
                .error("Failed to create metadata storage under " + destinationDir.getAbsolutePath());
        }

        Result<File[]> files = getDestinationFiles(destinationDir);
        if (files.failed()) {
            return Result.error(files.errorMessage);
        }

        return Result.with(new MetadataFiles(destinationDir, files.result));
    }

    static File baseDirFor(BundleResourceDeployment rd, Configuration referencedConfiguration) {
        File root = getMetadataRoot(referencedConfiguration);

        return baseDirFor(rd.getBundleDeployment().getDestination().getId(), root);
    }

    private static File baseDirFor(int destinationId, File metadataRoot) {
        return new File(metadataRoot, Integer.toString(destinationId));
    }

    static Result<MetadataFiles> getActive(Configuration referencedConfiguration) {
        File root = getMetadataRoot(referencedConfiguration);

        File activeDir = new File(root, "active");
        if (!activeDir.exists()) {
            return Result.with(null);
        }

        String[] files = activeDir.list();
        if (files == null || files.length == 0) {
            return Result.with(null);
        }


        int destinationId = Integer.parseInt(files[0]);

        File baseDir = baseDirFor(destinationId, root);
        Result<File[]> destFiles = getDestinationFiles(baseDir);
        if (destFiles.failed()) {
            return Result.error(destFiles.errorMessage);
        }

        return Result.with(new MetadataFiles(baseDir, destFiles.result));
    }

    public Result<Void> saveAsActive() {
        File activeDir = new File(baseDir.getParentFile(), "active");
        if (!activeDir.exists() && !activeDir.mkdirs()) {
            return Result.error("Failed to persist active destination");
        }

        File[] markers = activeDir.listFiles(); // there should be at most 1
        if (markers == null) {
            return Result.error("Could not determine the previously active destination");
        }

        for (File f : markers) {
            if (!f.delete()) {
                return Result.error("Could not clear the flag of previously active destination");
            }
        }

        File activeFile = new File(activeDir, baseDir.getName());
        try {
            //noinspection ResultOfMethodCallIgnored
            activeFile.createNewFile();

            return Result.with(null);
        } catch (IOException e) {
            return Result.error("Could not flag the destination as active: " + e.getMessage());
        }
    }

    public boolean isActive() {
        return getActiveMarkerFile().exists();
    }

    public File getNameFile() {
        return new File(baseDir.getParent(), baseDir.getName() + ".name");
    }

    public Result<Void> saveDestinationName(String name) {
        File destinationNameFile = getNameFile();
        if (!destinationNameFile.exists()) {
            StringReader nameRdr = new StringReader(name);
            try {
                Writer nameWrt = new PrintWriter(destinationNameFile, "UTF-8");

                StreamUtil.copy(nameRdr, nameWrt, true);
            } catch (FileNotFoundException e) {
                return Result.error("Could to create the file for storing the name of the destination. " + e.getMessage());
            } catch (RuntimeException e) {
                return Result.error("Failed to store the name of the destination. " + e.getMessage());
            } catch (UnsupportedEncodingException e) {
                return Result.error("Could not write the destination name file. " + e.getMessage());
            }
        }

        return Result.with(null);
    }

    private File getActiveMarkerFile() {
        return new File(new File(baseDir.getParentFile(), "active"), baseDir.getName());
    }

    public int getDestinationId() {
        return Integer.parseInt(baseDir.getName());
    }

    public String getDestinationName() throws FileNotFoundException {
        InputStream rdr = new FileInputStream(getNameFile());
        ByteArrayOutputStream wrt = new ByteArrayOutputStream();

        StreamUtil.copy(rdr, wrt, true);

        try {
            return wrt.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return wrt.toString();
        }
    }

    private static File getMetadataRoot(Configuration referencedConfiguration) {
        ServerPluginConfiguration config = new ServerPluginConfiguration(referencedConfiguration);

        File installationDir = new File(config.getHomeDir(), ".installation");

        return new File(installationDir, ".rhq");
    }

    private static Result<File[]> getDestinationFiles(File destinationDir) {
        File[] files = destinationDir.listFiles();

        if (files == null) {
            return Result
                .error("Could not list files in the destination metadata directory " + destinationDir);
        }

        // sort the files in reverse order so that the newest, current state, is on 0th index.
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getName().compareTo(o1.getName());
            }
        });

        return Result.with(files);
    }
}

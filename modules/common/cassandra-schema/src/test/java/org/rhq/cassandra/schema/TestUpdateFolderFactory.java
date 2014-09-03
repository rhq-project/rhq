package org.rhq.cassandra.schema;

import static java.util.Arrays.asList;

import java.util.Iterator;
import java.util.List;

/**
 * Provides support for removing update files from a requested update folder. See
 * {@link #removeFiles(String...) removeFiles()} for details.
 *
 * @author John Sanda
 */
public class TestUpdateFolderFactory extends UpdateFolderFactory {

    private String folder;

    private List<String> removedFiles;

    public TestUpdateFolderFactory(String folder) {
        this.folder = folder;
    }

    /**
     * Specifies update files to be removed from update folders created by this factory.
     *
     * @param files Update files to be removed. Each string should contain <strong>only</strong> the base name of the
     *              file excluding the path, e.g., 0003.xml and not schema/update/0003.xml or update/0003.xml
     *
     * @return This factory
     */
    public TestUpdateFolderFactory removeFiles(String... files) {
        removedFiles = asList(files);
        return this;
    }

    @Override
    public UpdateFolder newUpdateFolder(String folder) throws Exception {
        UpdateFolder updateFolder = super.newUpdateFolder(folder);
        if (folder.equals(folder) && !removedFiles.isEmpty()) {
            Iterator<UpdateFile> iterator = updateFolder.getUpdateFiles().iterator();
            while (iterator.hasNext()) {
                UpdateFile updateFile = iterator.next();
                if (isRemovedFile(updateFolder, updateFile)) {
                    iterator.remove();
                }
            }
        }
        return updateFolder;
    }

    private boolean isRemovedFile(UpdateFolder updateFolder, UpdateFile updateFile) {
        for (String removedFile : removedFiles) {
            if (updateFile.getFile().equals(updateFolder.getFolder() + "/" + removedFile)) {
                return true;
            }
        }
        return false;
    }
}

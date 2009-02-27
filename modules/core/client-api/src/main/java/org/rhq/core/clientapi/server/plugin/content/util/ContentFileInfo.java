package org.rhq.core.clientapi.server.plugin.content.util;

import java.io.File;

/**
 * Implemented by server content plugin classes responsible for determining information about their known File-based content.
 *  
 * @author jay shaughnessy
 */
public interface ContentFileInfo {

    /**
     * Set the content file to be used for the other methods.
     * @param contentFile.
     * @return
     */
    void setContentFile(File contentFile);

    /**
     * Get the current content file used for the other methods.
     * @return The File. Can be null.
     */
    File getContentFile();

    /**
     * Return an appropriate description for the file content.
     * 
     * @param defaultValue If a description can not be determined, the value to return. Can be null. This method should not throw an exception.
     * @return The version. Can be null.
     */
    String getDescription(String defaultValue);

    /**
     * Return an appropriate version for the file content.
     * 
     * @param defaultValue If a version can not be determined, the value to return. Can be null. This method should not throw an exception.
     * @return The version. Can be null.
     */
    String getVersion(String defaultValue);
}

package org.rhq.core.clientapi.server.plugin.content.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Return the version of the Jar file by inspecting the Manifest. The file does not necessarily need to be a Jar but
 * rather can be any archive satisfying Jar file structure, exploded or not.  
 *
 * @author jay shaughnessy
 */
public class ContentJarFileInfo implements ContentFileInfo {

    private File contentFile = null;
    private JarFile jarFile = null;
    private Manifest manifest = null;
    private Attributes mainAttributes = null;

    public ContentJarFileInfo(File contentFile) {
        setContentFile(contentFile);
    }

    public File getContentFile() {
        return this.contentFile;
    }

    public void setContentFile(File contentFile) {
        this.contentFile = contentFile;
        this.jarFile = null;
        this.manifest = null;
        this.mainAttributes = null;
        try {
            if (!this.contentFile.isDirectory()) {
                this.jarFile = new JarFile(this.contentFile);
                if (null != this.jarFile) {
                    this.manifest = this.jarFile.getManifest();
                }
            } else {
                File manifestFile = new File(this.contentFile, "/META-INF/MANIFEST.MF");
                if (manifestFile.exists()) {
                    InputStream is = null;
                    try {
                        is = new FileInputStream(manifestFile);

                        this.manifest = new Manifest(is);
                    } finally {
                        if (null != is)
                            is.close();
                    }
                }
            }
            if (null != this.manifest) {
                this.mainAttributes = manifest.getMainAttributes();
            }
        } catch (Exception e) {
            // ignore, leave values as set so far
        }
    }

    /**
     * Return a description of the Jar file (or archive satisfying Jar file structure) by inspecting the Manifest.
     * The returned description will be the value of one of the following attributes, with this preference:<br>
     * <br>
     * Implementation-Title<br>
     * Specification-Title<br> 
     * 
     * @param defaultValue If a version can not be determined, the value to return. Can be null.
     * @return The description. Can be null.
     */
    @Override
    public String getDescription(String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String val = this.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                if (null != val) {
                    result = val;
                } else {
                    val = this.mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
                    if (null != val) {
                        result = val;
                    }
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }

        return result;
    }

    /**
     * Return the version of the Jar file (or archive satisfying Jar file structure) by inspecting the Manifest.
     * The returned version will be the following, with this preference:<br>
     * <br>
     * Specification-Version (Implementation-Version)<br>
     * Implementation-Version<br> 
     * Specification-Version<br>  
     * @param contentFile The jar file for which we want a version.
     * @param defaultValue If a version can not be determined, the value to return. Can be null.
     * @return The version. Can be null.
     */
    @Override
    public String getVersion(String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String specVersion = this.mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
                String implVersion = this.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if ((null != specVersion) && (null != implVersion)) {
                    result = specVersion + " (" + implVersion + ")";
                } else {
                    if (null != implVersion) {
                        result = implVersion;
                    } else if (null != specVersion) {
                        result = specVersion;
                    }
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }
        return result;
    }

    public String getClasspath(String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String val = this.mainAttributes.getValue(Attributes.Name.CLASS_PATH);
                if (null != val) {
                    result = val;
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }

        return result;
    }

    public String getUrl(String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String val = this.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_URL);
                if (null != val) {
                    result = val;
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }

        return result;
    }

    public String getVendor(String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String val = this.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                if (null != val) {
                    result = val;
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }

        return result;
    }

    public String getSealed(String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String val = this.mainAttributes.getValue(Attributes.Name.SEALED);
                if (null != val) {
                    result = val;
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }

        return result;
    }
}

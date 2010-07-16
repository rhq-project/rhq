package org.rhq.plugins.apache;

import java.io.File;

public class ApacheTestConstants {

    public static final String MODULE_NAME="Httpd";
    public static final String PLUGIN_NAME="Apache";
    public static final String LENS_NAME="httpd.aug";
    public static final String TEMP_CONFIG_FILE_DIRECTORY = "HttpdTest";
    public static final String CONFIG_FILE_NAMES [] = {
        "httpd.conf",
        "included.conf",
        "nested.conf"};
    public static final String FILES_TO_LOAD [] = {
        "httpd.conf",
        "included.conf",
        "nested.conf",
        "httpd.aug"};
    public static final String ROOT_CONFIG_FILE_NAME = "httpd.conf";
    public static final String PLUGIN_CONFIG_PROP_SERVER_ROOT = "serverRoot";
    public static final String TEST_FILE_CONFIG_FOLDER="loadconfig";
    public static final String TEST_FILE_APACHE_CONFIG_FOLDER="updateconfig";
    public static String TEMP_FILES_PATH;

    /**
     * Path to folder containing rhq configuration in xml files.
     * @return
     */
    public static String getConfigFilesPathForLoad(){
        return TEMP_FILES_PATH+File.separator+TEST_FILE_CONFIG_FOLDER+File.separator;
    }
    /**
     * Path to folder containing apache configuration in "loadconfig" folder
     * @return
     */
    public static String getApacheConfigFilesPathForUpdate(){
        return TEMP_FILES_PATH+File.separator+TEST_FILE_APACHE_CONFIG_FOLDER+File.separator;
    }
    /**
     * Path to temporary folder where are/will be stored all config files.
     * @return
     */
    public static String getApacheConfigFilesPath(){
        return TEMP_FILES_PATH+File.separator;
    }
}

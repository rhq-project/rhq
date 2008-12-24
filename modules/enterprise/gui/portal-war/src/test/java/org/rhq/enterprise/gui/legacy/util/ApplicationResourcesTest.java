package org.rhq.enterprise.gui.legacy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationResourcesTest {

    class BundleLeveragingFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            if (dir.isHidden()) {
                return false;
            }
            if (dir.getAbsolutePath().endsWith("target")) {
                return false;
            }
            if (dir.getAbsolutePath().endsWith("svn")) {
                return false;
            }
            if (name.endsWith("svn")) {
                return false;
            }
            if (name.endsWith("class")) {
                return false;
            }

            return true;
        }
    }

    class PropertyFile implements Comparable<PropertyFile> {
        public final String property;
        public final String file;

        public PropertyFile(String property, String file) {
            this.file = file;
            this.property = property;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((file == null) ? 0 : file.hashCode());
            result = prime * result + ((property == null) ? 0 : property.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PropertyFile other = (PropertyFile) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (file == null) {
                if (other.file != null)
                    return false;
            } else if (!file.equals(other.file))
                return false;
            if (property == null) {
                if (other.property != null)
                    return false;
            } else if (!property.equals(other.property))
                return false;
            return true;
        }

        private ApplicationResourcesTest getOuterType() {
            return ApplicationResourcesTest.this;
        }

        public int compareTo(PropertyFile o) {
            int result = this.property.compareTo(o.property);
            if (result != 0) {
                return result;
            }
            return this.file.compareTo(o.file);
        }

    }

    Pattern patternXHTML = Pattern.compile("" + //
        ".*" + // some number of leading characters
        "[\\$|#]" + // starting with a dollar sign or #, the characters supported by UEL
        "\\{" + // wrapped in curly braces
        "msg" + // followed by the literal 'msg'
        "\\[" + // and then brackets
        "(" + // start capture group
        "[^\\]]+" + // one or more characters up to the first close brace
        ")" + // finish capture group
        "\\]" + // and closing brackets
        "\\}" + // and closing curly 
        ".*", // some number of trailing characters
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    Pattern patternJAVA = null;

    Pattern patternJSP = Pattern.compile("" + //
        ".*?" + // some number of leading characters
        "fmt\\:message" + // starting with 'fmt:message' literal
        "\\s+?" + // then some whitespace
        "key=" + // and the 'key' attribute, with an equals sign, and open double-quote
        "\"" + // an opening double-quote
        "(" + // start capture group
        "[^\"]+" + // one or more characters up to the first closing double-quote
        ")" + // finish capture group
        "\"", // a closing double-quote
        //"\\s*" + // then some optional whitespace
        //" />", // and closing tag 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public void test() {
        File modulesDir = new File("F:\\ws\\rhq\\trunk\\modules");
        List<File> toSearch = new LinkedList<File>();
        toSearch.add(modulesDir);

        FilenameFilter filter = new BundleLeveragingFilenameFilter();

        Map<PropertyFile, Integer> counts = new HashMap<PropertyFile, Integer>();

        int found = 0;
        int matches = 0;
        while (toSearch.size() > 0) {
            File nextFile = toSearch.remove(0);
            if (nextFile.isDirectory()) {
                for (File child : nextFile.listFiles(filter)) {
                    toSearch.add(child);
                }
            } else if (nextFile.isFile()) {
                found++;
                //System.out.println("Search: " + nextFile.getAbsolutePath());

                String path = nextFile.getAbsolutePath();
                int extPos = path.lastIndexOf('.');
                String ext = path.substring(extPos + 1);

                Pattern pattern = null;
                if (ext.equals("xhtml")) {
                    pattern = patternXHTML;
                } else if (ext.equals("java")) {
                    pattern = patternJAVA;
                } else if (ext.equals("jsp")) {
                    pattern = patternJSP;
                }

                if (pattern != null) {
                    matches += process(nextFile, pattern, counts);
                }
            }
        }

        List<PropertyFile> propertyNames = new ArrayList<PropertyFile>(counts.keySet());
        Collections.sort(propertyNames);
        for (PropertyFile pf : propertyNames) {
            int count = counts.get(pf);
            System.out.println("count: " + count + ", property: " + pf.property + ", file: " + pf.file);
        }

        Properties propertiesBundle = new Properties();
        File propertyBundleFile = new File(
            "F:/ws/rhq/trunk/modules/enterprise/gui/portal-war/src/main/webapp-filtered/WEB-INF/classes/ApplicationResources.properties");
        FileInputStream propertyBundleInputStream = null;
        try {
            propertyBundleInputStream = new FileInputStream(propertyBundleFile);
            propertiesBundle.load(propertyBundleInputStream);
        } catch (Exception e) {
            System.out.println("Could not load properties from file '" + propertyBundleFile.getAbsolutePath() + "'");
            return;
        } finally {
            if (propertyBundleInputStream != null) {
                try {
                    propertyBundleInputStream.close();
                } catch (Exception e) {
                    System.out.println("Could not close bundle file '" + propertyBundleFile.getAbsolutePath() + "'");
                }
            }
        }

        List<String> extraProperties = new ArrayList<String>(); // found in bundle, not in source
        List<String> missingProperties = new ArrayList<String>(); // found in source, not in bundle
        Set<String> bundlePropertyNames = propertiesBundle.stringPropertyNames();
        Set<String> foundPropertyNames = new HashSet<String>();
        for (PropertyFile pf : counts.keySet()) {
            foundPropertyNames.add(pf.property);
            if (bundlePropertyNames.contains(pf.property) == false && missingProperties.contains(pf.property) == false) {
                missingProperties.add(pf.property);
            }
        }
        int shortBundleNames = 0;
        for (String bundlePropertyName : bundlePropertyNames) {
            if (foundPropertyNames.contains(bundlePropertyName) == false
                && extraProperties.contains(bundlePropertyName) == false) {
                extraProperties.add(bundlePropertyName);
            }
            if (bundlePropertyName.split("\\.").length == 1) {
                shortBundleNames++;
            }
        }

        Collections.sort(extraProperties);
        Collections.sort(missingProperties);

        for (String extraProperty : extraProperties) {
            if (extraProperty.toLowerCase().indexOf("") != -1) {
                System.out.println("Extra: " + extraProperty);
            }
        }
        /*
        for (String missingProperty : missingProperties) {
            System.out.println("Missing: " + missingProperty);
        }
        */

        System.out.println("Found " + found + " files");
        System.out.println("Found " + matches + " matches");
        System.out.println("Found " + extraProperties.size() + " extra");
        System.out.println("Found " + missingProperties.size() + " missing");
        System.out.println("Found " + shortBundleNames + " short bundle names");
        System.out.println("Found " + bundlePropertyNames.size() + " total bundle names");
    }

    private int process(File f, Pattern pattern, Map<PropertyFile, Integer> properties) {
        BufferedReader lineReader = null;
        int matches = 0;
        try {
            lineReader = new BufferedReader(new FileReader(f));
            String nextLine = null;
            while ((nextLine = lineReader.readLine()) != null) {
                Matcher matcher = pattern.matcher(nextLine);
                if (matcher.find()) {
                    //System.out.println("Matched line '" + nextLine + "' in file '" + f + "'");
                    String propertyCapture = matcher.group(1);
                    boolean startQuote = propertyCapture.startsWith("'") || propertyCapture.startsWith("\"");
                    boolean endQuote = propertyCapture.endsWith("'") || propertyCapture.endsWith("\"");
                    if (startQuote && endQuote) {
                        propertyCapture = propertyCapture.substring(1, propertyCapture.length() - 1);
                    } else if (startQuote) {
                        propertyCapture = propertyCapture.substring(1, propertyCapture.length());
                    } else if (endQuote) {
                        propertyCapture = propertyCapture.substring(0, propertyCapture.length() - 1);
                    }

                    PropertyFile nextCount = new PropertyFile(propertyCapture, f.getAbsolutePath());
                    if (properties.containsKey(nextCount)) {
                        int count = properties.get(nextCount);
                        properties.put(nextCount, count + 1);
                    } else {
                        properties.put(nextCount, 1);
                        matches++;
                    }
                } else if (nextLine.indexOf("msg") != -1) {
                    System.out.println("No match, but found '" + nextLine + "'");
                }
            }
        } catch (Exception e) {
            System.out.println("Error opening file '" + f + "': " + e.getMessage());
        } finally {
            if (lineReader != null) {
                try {
                    lineReader.close();
                } catch (Exception e) {
                    System.out.println("Error closing file '" + f + "': " + e.getMessage());
                }
            }
        }
        return matches;
    }

    //      \$ \{ msg \[ " [(^\])] " \] \}
    public static void main(String[] args) {
        ApplicationResourcesTest tester = new ApplicationResourcesTest();
        tester.test();
    }
}

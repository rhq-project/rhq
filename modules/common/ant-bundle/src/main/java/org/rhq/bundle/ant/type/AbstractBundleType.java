package org.rhq.bundle.ant.type;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tools.ant.types.DataType;

import org.rhq.bundle.ant.BundleAntProject;

/**
 *
 */
public abstract class AbstractBundleType extends DataType {
    @Override
    public BundleAntProject getProject() {
        return (BundleAntProject) super.getProject();
    }

    protected static Pattern getPattern(List<FileSet> fileSets) {
        if (fileSets == null || fileSets.isEmpty()) {
            return null;
        }
        boolean first = true;
        StringBuilder regex = new StringBuilder();
        for (FileSet fileSet : fileSets) {
            if (!first) {
                regex.append("|");
            } else {
                first = false;
            }
            regex.append("(");
            File dir = fileSet.getDir();
            if (dir != null) {
                regex.append(dir);
                regex.append('/');
            }
            if (fileSet.getIncludePatterns().length == 0) {
                regex.append(".*");
            } else {
                boolean firstIncludePattern = true;
                for (String includePattern : fileSet.getIncludePatterns()) {
                    if (!firstIncludePattern) {
                        regex.append("|");
                    } else {
                        firstIncludePattern = false;
                    }
                    regex.append("(");
                    buildIncludePatternRegex(includePattern, regex);
                    regex.append(")");
                }
            }
            regex.append(")");
        }
        return Pattern.compile(regex.toString());
    }

    /**
     * Builds a regex expression for a single include pattern.
     * 
     * @param includePattern the single include pattern to build a regex for
     * @param regex appends all regex characters to this regex string
     */
    protected static void buildIncludePatternRegex(String includePattern, StringBuilder regex) {
        for (int i = 0; i < includePattern.length(); i++) {
            char c = includePattern.charAt(i);
            if (c == '?') {
                regex.append('.');
            } else if (c == '*') {
                if (i + 1 < includePattern.length()) {
                    char c2 = includePattern.charAt(i + 1);
                    if (c2 == '*') {
                        regex.append(".*");
                        i += 2;
                        continue;
                    }
                }
                regex.append("[^/]*");
            } else if (c == '.') {
                regex.append("\\.");
            } else {
                regex.append(c);
            }
            // TODO: Escape backslashes.
        }
    }
}

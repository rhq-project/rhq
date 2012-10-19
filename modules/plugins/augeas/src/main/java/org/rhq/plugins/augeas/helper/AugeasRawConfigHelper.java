/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.plugins.augeas.helper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.augeas.Augeas;

import org.apache.commons.io.FileUtils;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.util.MessageDigestGenerator;

/**
 * @author paji
 *
 */
public class AugeasRawConfigHelper {
    private String rootPath;
    private String loadPath;
    private String rootNodePath;
    private AugeasTranslator translator;
    private Map<String, List<String>> nodePaths = new HashMap<String, List<String>>();
    private final String transformPrefix = "/augeas/load/custom" + System.currentTimeMillis();
    private Map<String, String> configMap = new HashMap<String, String>();

    public AugeasRawConfigHelper(String augeasRootPath, String augeasLoadPath, String rootNode, AugeasTranslator t) {
        rootPath = augeasRootPath;
        loadPath = augeasLoadPath;
        rootNodePath = rootNode;
        translator = t;
    }

    public void addLens(String lensName, String configFilePath) {
        configMap.put(configFilePath, lensName);
    }

    public void addNode(String configFilePath, String nodeSuffix) {
        if (!nodePaths.containsKey(configFilePath)) {
            nodePaths.put(configFilePath, new LinkedList<String>());
        }
        nodePaths.get(configFilePath).add(nodeSuffix);
    }

    public void mergeRawConfig(Configuration from, RawConfiguration existingConfig, RawConfiguration toUpdate)
        throws Exception {
        Augeas aug = null;
        try {
            String lens = configMap.get(existingConfig.getPath());
            aug = createAugeas(lens, existingConfig.getContents());
            String file = getFile(aug);
            for (String pathSuffix : nodePaths.get(existingConfig.getPath())) {
                String propName = ("/files" + existingConfig.getPath() + "/" + pathSuffix).substring(rootNodePath
                    .length());
                String propValue = translator.getPropertyValue(propName, from);

                aug.set("/files" + file + "/" + pathSuffix, propValue);
            }
            aug.save();

            toUpdate.setPath(existingConfig.getPath());
            String contents = FileUtils.readFileToString(new File(file));
            String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(contents);
            toUpdate.setContents(contents, sha256);
        } finally {
            if (aug != null) {
                aug.close();
            }
        }
    }

    public void mergeStructured(RawConfiguration from, Configuration toUpdate) throws Exception {
        Augeas aug = null;
        try {
            String lens = configMap.get(from.getPath());
            aug = createAugeas(lens, from.getContents());
            String file = getFile(aug);
            for (String pathSuffix : nodePaths.get(from.getPath())) {
                String propName = ("/files" + from.getPath() + "/" + pathSuffix).substring(rootNodePath.length());
                String augeasPath = "/files" + file + "/" + pathSuffix;
                Property property = translator.createProperty(propName, augeasPath, aug);
                toUpdate.put(property);
            }
        } finally {
            if (aug != null) {
                aug.close();
            }
        }
    }

    public void save(RawConfiguration config) throws Exception {
        File f = new File(config.getPath());
        FileUtils.writeStringToFile(f, config.getContents());
    }

    private String getFile(Augeas aug) {
        return aug.get(transformPrefix + "/incl");
    }

    private Augeas createAugeas(String lens, String contents) throws Exception {
        Augeas aug = null;
        try {
            aug = new Augeas(rootPath, loadPath, Augeas.NO_MODL_AUTOLOAD);
            File fl = File.createTempFile("_rhq", null);
            //write the 'to' file to disk
            FileUtils.writeStringToFile(fl, contents);
            aug.set(transformPrefix + "/lens", lens);
            aug.set(transformPrefix + "/incl", fl.getAbsolutePath());
            aug.load();
        } catch (Exception e) {
            if (aug != null) {
                try {
                    aug.close();
                } catch (Exception e2) {
                }
                aug = null;
            }
            throw e;
        }
        return aug;
    }

    private String normalizeToUnix(byte[] contents) throws UnsupportedEncodingException {
        String s = new String(contents, "UTF8");
        return s.replaceAll("\r\n", "\n");
    }
}

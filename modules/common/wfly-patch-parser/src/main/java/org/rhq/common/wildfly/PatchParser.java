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

package org.rhq.common.wildfly;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A simple parser of the Wildfly patch files. Can be used only to find a limited set of metadata about the patches.
 *
 * @author Lukas Krejci
 * @since 4.13
 */
public final class PatchParser {
    private static final String PATCHES_NAMESPACE_URI = "urn:jboss:patch:bundle:1.0";
    private static final String PATCH_NAMESPACE_URI = "urn:jboss:patch:1.0";

    private PatchParser() {

    }

    /**
     * Tries to find if the provided input stream is a Wildfly patch file and return information about the patch parsed
     * out of it. This method assumes that the provided input stream is the contents of a ZIP file.
     * <p/>
     * The stream is not closed after this method returns.
     *
     * @param patchContents      the contents of the patch file
     * @param captureDescriptors whether to store the full contents of the patch XML descriptors in the returned info
     *
     * @return the info about the patch or null if the stream is not a patch file
     *
     * @throws IOException        on IO error or if the stream is not in a valid ZIP format
     * @throws XMLStreamException on error parsing XML patch descriptors (patch.xml or patches.xml files inside the
     *                            ZIP)
     */
    public static PatchInfo parse(InputStream patchContents, boolean captureDescriptors) throws IOException,
        XMLStreamException {

        ZipInputStream zip = new ZipInputStream(patchContents);

        Map<String, Patch> foundPatches = new HashMap<String, Patch>();
        ParsedPatchesXml patchesXml = null;

        PatchInfo ret = null;

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName().toLowerCase();
            if ("patch.xml".equals(name)) {
                ret = new PatchInfo(parsePatchXml(zip, captureDescriptors));
            } else if ("patches.xml".equals(name)) {
                patchesXml = parsePatchesXml(zip, captureDescriptors);
            } else if (name.endsWith(".zip")) {
                // try to parse proactively so that we have the picture of all patches in the zip file even if we get
                // entries of those patches before we parse "patches.xml". This is the only way to do it in one pass
                // through the provided input stream...
                try {
                    PatchInfo p = parse(zip, captureDescriptors);
                    if (p != null && p.is(Patch.class)) {
                        //important to use the exact name, not the lowercase variant
                        foundPatches.put(entry.getName(), p.as(Patch.class));
                    }
                } catch (IllegalArgumentException e) {
                    //ignore, this might not be a patch file after all...
                } catch (XMLStreamException e) {
                    //ignore, this might not be a patch file after all...
                }
            }
        }

        if (patchesXml != null) {
            List<PatchBundle.Element> elements = new ArrayList<PatchBundle.Element>(patchesXml.elements.size());
            for (String fileName : patchesXml.elements) {
                elements.add(new PatchBundle.Element(fileName, foundPatches.get(fileName)));
            }

            ret = new PatchInfo(new PatchBundle(elements, patchesXml.contents));
        }

        return ret;
    }

    private static ParsedPatchesXml parsePatchesXml(InputStream patchesXmlStream, boolean captureContents)
        throws IllegalArgumentException, XMLStreamException, IOException {

        String contents = null;

        XMLStreamReader rdr;

        if (captureContents) {
            contents = slurp(patchesXmlStream, "UTF-8");
            rdr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(contents));
        } else {
            rdr = XMLInputFactory.newInstance().createXMLStreamReader(patchesXmlStream, "UTF-8");
        }

        try {

            List<String> elements = new ArrayList<String>();

            boolean foundPatches = false;
            boolean foundElements = false;
            while (rdr.hasNext()) {
                int eventType = rdr.next();
                if (eventType != XMLStreamConstants.START_ELEMENT) {
                    continue;
                }

                QName name = rdr.getName();
                if (PATCHES_NAMESPACE_URI.equals(name.getNamespaceURI())) {
                    if ("patches".equals(name.getLocalPart())) {
                        if (foundElements) {
                            throw new IllegalArgumentException("Not a Wildfly patch.");
                        }
                        foundPatches = true;
                    } else if (foundPatches && "element".equals(name.getLocalPart())) {
                        elements.add(getAttributeValue(rdr, "", "path"));
                    }
                }

                foundElements = true;
            }

            return new ParsedPatchesXml(contents, elements);
        } finally {
            rdr.close();
        }
    }

    private static Patch parsePatchXml(InputStream patchXmlStream, boolean captureContents)
        throws IllegalArgumentException, IOException, XMLStreamException {

        String contents = null;

        XMLStreamReader rdr;

        if (captureContents) {
            contents = slurp(patchXmlStream, "UTF-8");
            rdr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(contents));
        } else {
            rdr = XMLInputFactory.newInstance().createXMLStreamReader(patchXmlStream, "UTF-8");
        }

        try {
            boolean foundPatch = false;
            boolean foundElements = false;

            String patchId = null;
            Patch.Type patchType = null;
            String identityName = null;
            String version = null;
            String description = null;

            while (rdr.hasNext()) {
                int eventType = rdr.next();

                if (eventType != XMLStreamConstants.START_ELEMENT) {
                    continue;
                }

                if (!foundPatch) {
                    if (foundElements) {
                        throw new IllegalArgumentException("Not a Wildfly patch");
                    }
                    String namespace = rdr.getName().getNamespaceURI();
                    foundPatch = PATCH_NAMESPACE_URI.equals(namespace) && "patch".equals(rdr.getName().getLocalPart());
                    patchId = getAttributeValue(rdr, "", "id");
                } else {
                    QName name = rdr.getName();
                    if (PATCH_NAMESPACE_URI.equals(name.getNamespaceURI())) {
                        if ("upgrade".equals(name.getLocalPart())) {
                            patchType = Patch.Type.CUMULATIVE;
                            identityName = getAttributeValue(rdr, "", "name");
                            version = getAttributeValue(rdr, "", "to-version");
                        } else if ("no-upgrade".equals(name.getLocalPart())) {
                            patchType = Patch.Type.ONE_OFF;
                            identityName = getAttributeValue(rdr, "", "name");
                            version = getAttributeValue(rdr, "", "version");
                        } else if ("description".equals(name.getLocalPart())) {
                            while (rdr.hasNext()) {
                                eventType = rdr.next();
                                switch (eventType) {
                                case XMLStreamConstants.CDATA:
                                case XMLStreamConstants.CHARACTERS:
                                    description = rdr.getText();
                                    break;
                                }

                                if (description != null) {
                                    break;
                                }
                            }
                        } else if ("element".equals(name.getLocalPart())) {
                            //we've found enough info... break out of the reader loop
                            break;
                        }
                    }
                }

                foundElements = true;
            }

            return new Patch(patchId, patchType, identityName, version, description, contents);
        } finally {
            rdr.close();
        }
    }

    private static String getAttributeValue(XMLStreamReader rdr, String namespaceURI, String name) {
        for (int i = 0; i < rdr.getAttributeCount(); ++i) {
            QName qname = rdr.getAttributeName(i);
            if (namespaceURI.equals(qname.getNamespaceURI()) && name.equals(qname.getLocalPart())) {
                return rdr.getAttributeValue(i);
            }
        }

        return null;
    }

    private static String slurp(InputStream stream, String charsetName) throws IOException {
        StringBuilder bld = new StringBuilder();
        InputStreamReader rdr = new InputStreamReader(stream, charsetName);
        char[] buffer = new char[1024];
        int cnt;
        while ((cnt = rdr.read(buffer)) != -1) {
            bld.append(buffer, 0, cnt);
        }

        return bld.toString();
    }

    private static final class ParsedPatchesXml {
        String contents;
        List<String> elements;

        private ParsedPatchesXml(String contents, List<String> elements) {
            this.contents = contents;
            this.elements = elements;
        }
    }
}

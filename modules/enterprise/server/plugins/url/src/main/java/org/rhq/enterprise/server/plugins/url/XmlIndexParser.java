/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.url;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.ConfigurationType;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.ListPropertyType;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.MapPropertyType;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.ObjectFactory;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.PackageDetailsKeyType;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.PackageDetailsType;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.PackageType;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.ResourceVersionsType;
import org.rhq.core.clientapi.descriptor.contentsource.packagedetails.SimplePropertyType;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Parses the index XML file whose format follows the package details schema.
 * 
 * @author John Mazzitelli
 */
public class XmlIndexParser implements IndexParser {
    private final Log log = LogFactory.getLog(XmlIndexParser.class);

    private static final String PLUGIN_SCHEMA_PATH = "rhq-contentsource-packagedetails.xsd";

    public Map<String, RemotePackageInfo> parse(InputStream indexStream, UrlSource contentSource) throws Exception {
        return jaxbParse(indexStream, contentSource.getIndexUrl(), contentSource.getRootUrlString());
    }

    @SuppressWarnings("unchecked")
    protected Map<String, RemotePackageInfo> jaxbParse(InputStream indexStream, URL indexUrl, String rootUrlString)
        throws Exception {

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.CONTENTSOURCE_PACKAGEDETAILS);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        // Enable schema validation
        URL pluginSchemaURL = XmlIndexParser.class.getClassLoader().getResource(PLUGIN_SCHEMA_PATH);
        Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);
        unmarshaller.setSchema(pluginSchema);

        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);

        BufferedReader reader = new BufferedReader(new InputStreamReader(indexStream));
        JAXBElement<PackageType> packagesXml = (JAXBElement<PackageType>) unmarshaller.unmarshal(reader);

        for (ValidationEvent event : vec.getEvents()) {
            log.debug("URL content source index [" + indexUrl + "] message {Severity: " + event.getSeverity()
                + ", Message: " + event.getMessage() + ", Exception: " + event.getLinkedException() + "}");
        }

        Map<String, RemotePackageInfo> fileList = new HashMap<String, RemotePackageInfo>();

        List<PackageDetailsType> allPackages = packagesXml.getValue().getPackage();
        for (PackageDetailsType pkg : allPackages) {
            URL locationUrl = new URL(rootUrlString + pkg.getLocation());
            ContentSourcePackageDetails details = translateXmlToDomain(pkg);
            FullRemotePackageInfo rpi = new FullRemotePackageInfo(locationUrl, details);
            fileList.put(stripLeadingSlash(rpi.getLocation()), rpi);
        }

        return fileList;
    }

    /**
     * Translates the details XML type to its domain object.
     * @param pkg the XML type object
     * @return the domain object with the same data that the XML object had
     * @throws Exception 
     */
    protected ContentSourcePackageDetails translateXmlToDomain(PackageDetailsType pkg) throws Exception {

        PackageDetailsKeyType keyType = pkg.getPackageDetailsKey();

        try {
            ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey(keyType.getName(), keyType
                .getVersion(), keyType.getPackageTypeName(), keyType.getArchitectureName(), keyType
                .getResourceTypeName(), keyType.getResourceTypePlugin());
            ContentSourcePackageDetails details = new ContentSourcePackageDetails(key);
            details.setDisplayName(pkg.getDisplayName());
            details.setDisplayVersion(pkg.getDisplayVersion());
            details.setShortDescription(pkg.getShortDescription());
            details.setLongDescription(pkg.getLongDescription());
            details.setClassification(pkg.getClassification());
            details.setFileName(pkg.getFileName());
            details.setFileSize(pkg.getFileSize());
            details.setFileCreatedDate(pkg.getFileCreatedDate());
            details.setMD5(pkg.getMd5());
            details.setSHA256(pkg.getSha256());
            details.setLicenseName(pkg.getLicenseName());
            details.setLicenseVersion(pkg.getLicenseVersion());
            if (pkg.getMetadata() != null) {
                details.setMetadata(pkg.getMetadata().getBytes());
            }
            details.setLocation(pkg.getLocation());

            ResourceVersionsType resourceVersions = pkg.getResourceVersions();
            if (resourceVersions != null) {
                details.setResourceVersions(new HashSet<String>(resourceVersions.getResourceVersion()));
            }

            ConfigurationType extraPropertiesXml = pkg.getExtraProperties();
            if (extraPropertiesXml != null) {
                Configuration config = new Configuration();
                List<Object> configXml = extraPropertiesXml.getSimplePropertyOrListPropertyOrMapProperty();
                for (Object object : configXml) {
                    if (object instanceof SimplePropertyType) {
                        config.put(translateSimpleProperty((SimplePropertyType) object, null));
                    } else if (object instanceof ListPropertyType) {
                        config.put(translateListProperty((ListPropertyType) object, null));
                    } else if (object instanceof MapPropertyType) {
                        config.put(translateMapProperty((MapPropertyType) object, null));
                    } else {
                        throw new IllegalStateException("Unknown JAXB type: " + object); // did the schema change?
                    }
                }
                details.setExtraProperties(config);
            }
            return details;
        } catch (Exception e) {
            log.error("Failed to process package [" + keyType.getName() + " v" + keyType.getVersion() + ']');
            throw e;
        }

    }

    protected Property translateSimpleProperty(SimplePropertyType object, String defaultName) {
        String name = object.getName();
        String value = object.getValue();
        return new PropertySimple((name != null) ? name : defaultName, value);
    }

    protected Property translateListProperty(ListPropertyType object, String defaultName) {
        String name = object.getName();
        List<Object> listXml = object.getSimplePropertyOrListPropertyOrMapProperty();
        PropertyList list = new PropertyList((name != null) ? name : defaultName);
        for (Object listItem : listXml) {
            if (listItem instanceof SimplePropertyType) {
                list.add(translateSimpleProperty((SimplePropertyType) listItem, name));
            } else if (listItem instanceof ListPropertyType) {
                list.add(translateListProperty((ListPropertyType) listItem, name));
            } else if (listItem instanceof MapPropertyType) {
                list.add(translateMapProperty((MapPropertyType) listItem, name));
            } else {
                throw new IllegalStateException("Unknown JAXB type: " + object); // did the schema change?
            }
        }
        return list;
    }

    protected Property translateMapProperty(MapPropertyType object, String defaultName) {
        String name = object.getName();
        List<Object> mapXml = object.getSimplePropertyOrListPropertyOrMapProperty();
        PropertyMap map = new PropertyMap((name != null) ? name : defaultName);
        for (Object mapItem : mapXml) {
            if (mapItem instanceof SimplePropertyType) {
                map.put(translateSimpleProperty((SimplePropertyType) mapItem, null));
            } else if (mapItem instanceof ListPropertyType) {
                map.put(translateListProperty((ListPropertyType) mapItem, null));
            } else if (mapItem instanceof MapPropertyType) {
                map.put(translateMapProperty((MapPropertyType) mapItem, null));
            } else {
                throw new IllegalStateException("Unknown JAXB type: " + object); // did the schema change?
            }
        }
        return map;
    }

    protected String stripLeadingSlash(String str) {
        while (str.startsWith("/")) {
            str = (str.length() > 1) ? str.substring(1) : "";
        }
        return str;
    }

    /**
     * A utility that can build an index file that contains metadata for content found
     * in a given directory.
     *
     * <pre>
     * java -cp "target\rhq-serverplugin-url-1.3.0-SNAPSHOT.jar;
     *           ..\..\..\..\core\client-api\target\rhq-core-client-api-1.3.0-SNAPSHOT.jar;
     *           ..\..\..\..\core\domain\target\rhq-core-domain-ejb3.jar;
     *           %HOMEPATH%\.m2\repository\commons-logging\commons-logging\1.1.0.jboss\commons-logging-1.1.0.jboss.jar"
     *      org.rhq.enterprise.server.plugins.url.XmlIndexParser
     *      C:\my\content\source\directory
     *      library
     *      noarch
     *      "JBossAS Server"
     *      JBossAS
     * </pre>
     *
     * @param args
     */
    public static void main(String args[]) {
        try {
            if (args.length != 5) {
                System.err.println("Syntax: <directory> <packageTypeName> <archName> <resTypeName> <resTypePlugin>");
                System.exit(1);
            }

            String directoryString = args[0];
            File directory = new File(directoryString);
            if (!directory.isDirectory()) {
                System.err.println("You did not supply a valid directory name: " + directoryString);
                System.exit(1);
            }

            PackageDetailsKeyType packageTypeInfo = new PackageDetailsKeyType();
            packageTypeInfo.setPackageTypeName(args[1]);
            packageTypeInfo.setArchitectureName(args[2]);
            packageTypeInfo.setResourceTypeName(args[3]);
            packageTypeInfo.setResourceTypePlugin(args[4]);

            File index = new File(directory, "content-index.xml");

            JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.CONTENTSOURCE_PACKAGEDETAILS);
            Marshaller marshaller = jaxbContext.createMarshaller();
            PackageType packagesXml = new PackageType();
            PrintWriter writer = new PrintWriter(index);

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            try {
                generatePackageIndex(directory, packagesXml.getPackage(), directory, packageTypeInfo);
                marshaller.marshal(new ObjectFactory().createPackages(packagesXml), writer);
            } finally {
                writer.close();
            }
        } catch (Throwable t) {
            System.err.println("Failed to generate content index file. Cause: " + t);
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    protected static void generatePackageIndex(File file, List<PackageDetailsType> list, File root,
        PackageDetailsKeyType packageTypeInfo) throws Exception {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            for (File childFile : childFiles) {
                generatePackageIndex(childFile, list, root, packageTypeInfo);
            }
        } else if (!file.getCanonicalPath().equals(new File(root, "content-index.xml").getCanonicalPath())) {
            String relativeLocation = file.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);
            String md5 = generateHash(file, "MD5");
            String sha256 = generateHash(file, "SHA-256");

            PackageDetailsKeyType detailsKeyType = new PackageDetailsKeyType();
            detailsKeyType.setName(file.getName());
            detailsKeyType.setVersion(md5);
            detailsKeyType.setPackageTypeName(packageTypeInfo.getPackageTypeName());
            detailsKeyType.setArchitectureName(packageTypeInfo.getArchitectureName());
            detailsKeyType.setResourceTypeName(packageTypeInfo.getResourceTypeName());
            detailsKeyType.setResourceTypePlugin(packageTypeInfo.getResourceTypePlugin());

            PackageDetailsType detailsType = new PackageDetailsType();
            detailsType.setPackageDetailsKey(detailsKeyType);
            //detailsType.setResourceVersions();
            detailsType.setDisplayName(file.getName());
            detailsType.setDisplayVersion(md5);
            //detailsType.setShortDescription();
            //detailsType.setLongDescription();
            //detailsType.setClassification();
            detailsType.setFileName(file.getName());
            detailsType.setFileSize(file.length());
            detailsType.setFileCreatedDate(file.lastModified());
            detailsType.setMd5(md5);
            detailsType.setSha256(sha256);
            //detailsType.setLicenseName();
            //detailsType.setLicenseVersion();
            //detailsType.setMetadata();
            //detailsType.setExtraProperties();
            detailsType.setLocation(relativeLocation);

            list.add(detailsType);
        }

        return;
    }

    /**
     * Generates a hash code for the given file.
     * @param file the file whose contents is used to generate the hash
     * @param algorithm "MD5" or "SHA-256"
     * @return 
     * @return the hash as a string
     * @throws Exception
     */
    protected static String generateHash(File file, String algorithm) throws Exception {
        String hash;
        FileInputStream is = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            is = new FileInputStream(file);

            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes, 0, bytes.length)) != -1) {
                messageDigest.update(bytes, 0, len);
            }

            bytes = messageDigest.digest();
            StringBuffer sb = new StringBuffer(bytes.length * 2);

            for (int i = 0; i < bytes.length; i++) {
                int hi = (bytes[i] >> 4) & 0xf;
                int lo = bytes[i] & 0xf;
                sb.append(Character.forDigit(hi, 16));
                sb.append(Character.forDigit(lo, 16));
            }
            hash = sb.toString();
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return hash;
    }
}

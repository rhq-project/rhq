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
package org.rhq.core.db;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import mazz.i18n.Logger;
import mazz.i18n.Msg;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class performs XML parsing on content that defines database specific type mappings. This is how you define the
 * type names that are valid for the different database vendors. A type map has one or more typemap nodes - a typemap
 * defines a generic type (e.g. LONG, BOOLEAN) and a set of database specific types. You define a "generic" type name
 * (the <i>typemap</i> element's <i>type</i> attribute) and you give a database vendor mapping that defines the
 * database-specific type that is the most appropriate for that generic type. Here's an example for the generic type
 * "LONG":
 *
 * <pre>
 *    &lt;typemaps type="LONG">
 *       &lt;typemap type="LONG">
 *          &lt;map db="java"       type="BIGINT" />
 *          &lt;map db="postgresql" type="BIGINT" />
 *          &lt;map db="oracle"     type="NUMBER(19,0)" />
 *       &lt;/typemap>
 *    &lt;/typemaps>
 * </pre>
 *
 * <p>The database name of "java" represents JDBC SQL types. The other database names are those that are returned by the
 * different implementations of {@link DatabaseType#getName()}.</p>
 *
 * <p>Instances of this object are created via {@link #loadTypeMapsFromStream(InputStream)} and
 * {@link #loadKnownTypeMaps()}.</p>
 */
public class TypeMap {
    private static final Logger LOG = DbUtilsI18NFactory.getLogger(TypeMap.class);
    private static final Msg MSG = DbUtilsI18NFactory.getMsg();

    /**
     * The XML file name containing the type maps of the known DB types. This should be found in this class's
     * classloader.
     */
    private static final String DB_TYPEMAPS_XML_FILENAME = "db-typemaps.xml";

    /**
     * XML element name that maps a generic type to a set of database specific types.
     */
    private static final String TYPEMAP_ELEMENT = "typemap";

    /**
     * The subelement name that is a child of the {@link #TYPEMAP_ELEMENT} element. Its children will define a single
     * mapping for a database specific type.
     */
    private static final String TYPEMAP_MAP_ELEMENT = "map";

    /**
     * The attribute of the {@link #TYPEMAP_ELEMENT} element that indicates the specific database the mapping is for.
     */
    private static final String DB_ATTRIBUTE = "db";

    /**
     * The attribute of either the {@link #TYPEMAP_ELEMENT} or the {@link #TYPEMAP_MAP_ELEMENT} element that indicates
     * the generic or database specific type name, respectively.
     */
    private static final String TYPE_ATTRIBUTE = "type";

    /**
     * Represents the non-database-specific map type; types mapped to this are JDBC SQL types.
     */
    private static final String JAVA_DATABASE_TYPE = "java";

    /**
     * The generic name for the type this type map is defining. The mappings of this typemap will define the database
     * specific type that correlates to this generic, common type name.
     */
    private String m_genericTypeName;

    /**
     * Contains the database specific type map which maps the different databases and their respective types that
     * correlate to the generic type.
     */
    private Hashtable<String, String> m_databaseTypeMap = new Hashtable<String, String>();

    /**
     * A collection of types that this class already knows about.
     */
    private static Collection<TypeMap> m_knownTypes;

    /**
     * Given a DOM node, this will ensure it is a valid <i>typemap</i> node and parses it. This constructor is private,
     * use {@link #loadTypeMapsFromFile(File)} to create instances of this object.
     *
     * @param  typemap_node the top-level <i>typemap</i> DOM node
     *
     * @throws SAXException if <code>node</code> is not a valid typemap node
     */
    private TypeMap(Node typemap_node) throws SAXException {
        if (TypeMap.isTypeMap(typemap_node)) {
            NamedNodeMap typemap_node_attribs = typemap_node.getAttributes();

            // there really should only be one attrib, but loop through looking for the one type attrib we want
            for (int i_typemap_node_attrib = 0; i_typemap_node_attrib < typemap_node_attribs.getLength(); i_typemap_node_attrib++) {
                Node typemap_attrib = typemap_node_attribs.item(i_typemap_node_attrib);
                String typemap_attrib_name = typemap_attrib.getNodeName();
                String typemap_attrib_value = typemap_attrib.getNodeValue();

                // if this is the type attrib we want, then its value is the generic type being defined
                if (TYPE_ATTRIBUTE.equalsIgnoreCase(typemap_attrib_name)) {
                    this.m_genericTypeName = typemap_attrib_value;

                    // each child node of the typemap is one database-specific type mapping
                    NodeList maps = typemap_node.getChildNodes();

                    for (int i_map = 0; i_map < maps.getLength(); i_map++) {
                        Node typemap_map_node = maps.item(i_map);

                        // ignore text/comment nodes
                        if (typemap_map_node.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (TYPEMAP_MAP_ELEMENT.equalsIgnoreCase(typemap_map_node.getNodeName())) {
                            String db = null;
                            String db_type = null;

                            NamedNodeMap map_attribs = typemap_map_node.getAttributes();

                            for (int i_attr = 0; i_attr < map_attribs.getLength(); i_attr++) {
                                Node typemap_map_attrib = map_attribs.item(i_attr);
                                typemap_attrib_name = typemap_map_attrib.getNodeName();
                                typemap_attrib_value = typemap_map_attrib.getNodeValue();

                                if (DB_ATTRIBUTE.equalsIgnoreCase(typemap_attrib_name)) {
                                    db = typemap_attrib_value;
                                } else if (TYPE_ATTRIBUTE.equalsIgnoreCase(typemap_attrib_name)) {
                                    db_type = typemap_attrib_value;
                                } else {
                                    throw new SAXException(MSG.getMsg(DbUtilsI18NResourceKeys.INVALID_TYPE_MAP_ATTRIB,
                                        this.m_genericTypeName, typemap_attrib_name, typemap_attrib_value));
                                }
                            }

                            this.m_databaseTypeMap.put(db, db_type);
                        } else {
                            throw new SAXException(MSG.getMsg(DbUtilsI18NResourceKeys.INVALID_TYPE_MAP_CHILD,
                                typemap_map_node.getNodeName(), TYPEMAP_MAP_ELEMENT));
                        }
                    }
                }
            }
        } else {
            throw new SAXException(MSG.getMsg(DbUtilsI18NResourceKeys.NODE_NOT_VALID_TYPEMAP_NODE, typemap_node));
        }

        if (m_genericTypeName == null) {
            throw new SAXException(MSG.getMsg(DbUtilsI18NResourceKeys.MISSING_TYPE_MAP_GENERIC_TYPE, TYPE_ATTRIBUTE));
        }

        return;
    }

    /**
     * If this type map defines database types for the given generic type, this returns the type for the given database.
     * If this type mapping does not define database specific types for the given generic type or it doesn't have a type
     * defined for the given database, <code>null</code> is returned.
     *
     * <p>The database name (which is unique to a vendor/version) is first used to look up the type. If none is found,
     * simply the vendor name is used to look up the type. This allows the XML to define a default type for all versions
     * of a specific database vendor; but if a type needs to be different for a specific version of the vendor database,
     * this allows for that.</p>
     *
     * If <code>database</code> is <code>null</code>, this means you are asking for a non-vendor-specific map type; in
     * other words, the JDBC SQL type.
     *
     * @param  generic_type
     * @param  database
     *
     * @return the database specific type that is to be used for the given generic type on the given database; will be
     *         <code>null</code> if the type is not found within this object.
     */
    public String getMappedType(String generic_type, DatabaseType database) {
        String db_specific_type = null;

        if (m_genericTypeName.equalsIgnoreCase(generic_type)) {
            if (database == null) {
                db_specific_type = m_databaseTypeMap.get(JAVA_DATABASE_TYPE);
            } else {
                db_specific_type = m_databaseTypeMap.get(database.getName());

                // there is no vendor/version specific type, see if one is defined for all versions of the database vendor
                if (db_specific_type == null) {
                    db_specific_type = m_databaseTypeMap.get(database.getVendor());
                }
            }
        }

        return db_specific_type;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer str = new StringBuffer(m_genericTypeName);
        str.append('=');
        str.append(m_databaseTypeMap);
        return str.toString();
    }

    /**
     * Given a collection of generic type maps, this returns the database specific type for the given generic type. If
     * <code>database</code> is <code>null</code>, this means you are asking for a non-vendor-specific map type; in
     * other words, the JDBC SQL type.
     *
     * @param  typemaps     collection of mappings for multiple generic types
     * @param  generic_type the generic type name whose corresponding database specific type is to be returned
     * @param  database
     *
     * @return returns the database specific type that maps to the given generic type; <code>null</code> is returned if
     *         there is no database specific mapping
     */
    public static String getMappedType(Collection<TypeMap> typemaps, String generic_type, DatabaseType database) {
        for (TypeMap typemap : typemaps) {
            String type_name = typemap.getMappedType(generic_type, database);
            if (type_name != null) {
                return type_name;
            }
        }

        return null;
    }

    /**
     * Tests to see if the given node is a valid {@link #TYPEMAP_ELEMENT} node. A valid typemap node consists of the
     * parent node that defines the generic type name and has children that defines all the database specific types.
     *
     * @param  node_to_test the suspect typemap node
     *
     * @return <code>true</code> if the given node is a typemap node.
     */
    private static boolean isTypeMap(Node node_to_test) {
        String node_name = node_to_test.getNodeName();
        return TYPEMAP_ELEMENT.equalsIgnoreCase(node_name);
    }

    /**
     * Given a typemaps node, this checks all its direct children and those that are a {@link #TYPEMAP_ELEMENT} node
     * will be parsed and their children typemaps are parsed and returned.
     *
     * @param  typemaps_node root node that contains one or more typemaps node children
     *
     * @return the collection of all the generic types which contain their database specific type mappings
     *
     * @throws SAXException if an invalid type mapping was encountered
     */
    private static Collection<TypeMap> readTypeMaps(Node typemaps_node) throws SAXException {
        Collection<TypeMap> result = new ArrayList<TypeMap>();
        NodeList typemap_nodes = typemaps_node.getChildNodes();

        for (int j = 0; j < typemap_nodes.getLength(); j++) {
            try {
                Node node = typemap_nodes.item(j);

                // ignore text/comment nodes
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.add(new TypeMap(node));
                }
            } catch (SAXException e) {
                throw new SAXException(MSG.getMsg(DbUtilsI18NResourceKeys.INVALID_TYPE_MAP, e));
            }
        }

        return result;
    }

    /**
     * Loads in the known type maps. These type mappings are predefined in an XML file that ships with the code. The
     * known types, once loaded, are cached in this object - calling this method afterwards will not re-parse the XML
     * file, instead the cached type map collection will be returned.
     *
     * @return a collection of known type maps
     *
     * @throws IllegalStateException if the known XML file could not be loaded (should never occur) or was invalid
     */
    public static Collection<TypeMap> loadKnownTypeMaps() {
        if (m_knownTypes == null) {
            InputStream stream = TypeMap.class.getClassLoader().getResourceAsStream(DB_TYPEMAPS_XML_FILENAME);

            if (stream == null) {
                // this should not happen - this file should ship in the jar with this class
                throw new IllegalStateException(MSG.getMsg(DbUtilsI18NResourceKeys.KNOWN_TYPEMAPS_XML_FILE_NOT_FOUND,
                    DB_TYPEMAPS_XML_FILENAME));
            }

            try {
                m_knownTypes = loadTypeMapsFromStream(stream);
            } catch (Exception e) {
                // this should never happen - we should never ship an invalid set of default types
                throw new IllegalStateException(e);
            }
        }

        return m_knownTypes;
    }

    /**
     * Given an input stream that contains an XML file of type maps, this will load in those type maps.
     *
     * @param  xml_stream the stream containing the XML mapping (must not be <code>null</code>)
     *
     * @return a collection of type maps as found in the given typemaps XML stream
     *
     * @throws NullPointerException if <code>xml_stream</code> is <code>null</code>
     * @throws Exception            if the type maps data was invalid
     */
    public static Collection<TypeMap> loadTypeMapsFromStream(InputStream xml_stream) throws Exception {
        if (xml_stream == null) {
            throw new NullPointerException("xml_stream == null");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xml_stream);
        Collection<TypeMap> typemaps = readTypeMaps(doc.getDocumentElement());

        LOG.debug(DbUtilsI18NResourceKeys.LOADED_TYPE_MAPS, typemaps);

        return typemaps;
    }
}
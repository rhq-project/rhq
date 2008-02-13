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
package org.rhq.core.db.setup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import mazz.i18n.Logger;
import mazz.i18n.Msg;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.db.DbUtilsI18NFactory;
import org.rhq.core.db.DbUtilsI18NResourceKeys;
import org.rhq.core.db.TypeMap;
import org.rhq.core.db.log.FileLoggerListener;
import org.rhq.core.db.log.LoggerDriver;
import org.rhq.core.db.log.StdOutLoggerListener;

/**
 * Performs XML parsing of the database schema and data files and sets up the database accordingly. This has the ability
 * to not only set up the database, but also to purge the database of the schemas and data.
 */
public class DBSetup {
    private static final Logger LOG = DbUtilsI18NFactory.getLogger(DBSetup.class);
    private static final Msg MSG = DbUtilsI18NFactory.getMsg();

    private static final String DBSETUP_ROOT_ELEMENT_NAME = "dbsetup";

    private String m_jdbcUrl;
    private String m_username;
    private String m_password;
    private boolean m_jdbcLogEnabled;
    private String m_jdbcLogFileName;
    private boolean m_consoleMode;

    private Connection m_connection;
    private DatabaseType m_databaseType;

    /**
     * Creates a new {@link DBSetup} object where this object is not in console mode (meaning messages will not be
     * emitted to the console).
     *
     * @param jdbc_url           the JDBC URL used to connect to the database
     * @param username           the user that will be logged into the database
     * @param password           user's credentials
     * @param jdbc_log_file_name if not <code>null</code>, the JDBC logger log messages will be written here
     */
    public DBSetup(String jdbc_url, String username, String password, String jdbc_log_file_name) {
        m_jdbcUrl = jdbc_url;
        m_username = username;
        m_password = password;
        m_jdbcLogFileName = jdbc_log_file_name;
        m_jdbcLogEnabled = jdbc_log_file_name != null;
        m_consoleMode = false;
    }

    /**
     * Creates a new {@link DBSetup} object where this object is in console mode (meaning messages will be emitted to
     * the console).
     *
     * @param jdbc_url         the JDBC URL used to connect to the database
     * @param username         the user that will be logged into the database
     * @param password         user's credentials
     * @param jdbc_log_enabled if <code>true</code> JDBC logging will be enabled and messages will be sent to the
     *                         console
     */
    public DBSetup(String jdbc_url, String username, String password, boolean jdbc_log_enabled) {
        m_jdbcUrl = jdbc_url;
        m_username = username;
        m_password = password;
        m_jdbcLogFileName = null;
        m_jdbcLogEnabled = jdbc_log_enabled;
        m_consoleMode = true;
    }

    /**
     * A console application that can be used to run the DBSetup from a command line. The arguments are as follows:
     *
     * <pre>
     * DBSetup -op=setup|clear|uninstall|uninstallsetup [-log=none|all|sql]
     *         -jdbcurl=&lt;db-url> [-jdbcuser=&lt;username>] [-jdbcpassword=&lt;password>]
     *         -file=&lt;dbsetup-xml-file>
     * </pre>
     *
     * where:
     *
     * <ul>
     *   <li>-op: Defines what operation to perform:
     *
     *     <ul>
     *       <li>export: exports an existing database schema and its data to an XML file</li>
     *       <li>setup: creates the new database schema and inserts all data</li>
     *       <li>clear: deletes all data from the schema but leaves the schema in the database</li>
     *       <li>uninstall: deletes all data and the schema itself from the database</li>
     *       <li>uninstallsetup: performs an uninstall first, and then a setup</li>
     *     </ul>
     *   </li>
     *   <li>-log: enables JDBC logging so JDBC calls are logged to the console
     *
     *     <ul>
     *       <li>all: logs all JDBC calls</li>
     *       <li>sql: logs only JDBC calls that execute SQL</li>
     *     </ul>
     *   </li>
     *   <li>-jdbcurl: JDBC URL used to connect to the database</li>
     *   <li>-jdbcuser: username that is used to connect to the database</li>
     *   <li>-jdbcpassword: credentials of the user</li>
     *   <li>-file=&ltdbsetup-xml-file>: specifies the path to the DBSetup XML file that is read or exported to</li>
     * </ul>
     *
     * @param args see description
     */
    public static void main(String[] args) {
        boolean do_export = false;
        boolean do_setup = false;
        boolean do_clear = false;
        boolean do_uninstall = false;
        boolean do_uninstallsetup = false;
        String op_requested = "";
        boolean jdbc_log_enabled = false;
        String jdbc_url = null;
        String jdbc_user = null;
        String jdbc_password = null;
        String dbsetup_file = null;

        try {
            if (args.length >= 2) {
                for (String arg : args) {
                    if (arg.startsWith("-op=")) {
                        op_requested = arg.substring(arg.indexOf('=') + 1);
                        do_export = op_requested.equals("export");
                        do_setup = op_requested.equals("setup");
                        do_clear = op_requested.equals("clear");
                        do_uninstall = op_requested.equals("uninstall");
                        do_uninstallsetup = op_requested.equals("uninstallsetup");
                    } else if (arg.startsWith("-log=")) {
                        String log_arg = arg.substring(arg.indexOf('=') + 1);

                        if (log_arg.equalsIgnoreCase("sql")) {
                            jdbc_log_enabled = true;
                            System.setProperty(LoggerDriver.PROP_LOGSQLONLY, "true");
                        } else if (log_arg.equalsIgnoreCase("all")) {
                            jdbc_log_enabled = true;
                            System.setProperty(LoggerDriver.PROP_LOGSQLONLY, "false");
                        } else if (!log_arg.equalsIgnoreCase("none")) {
                            throw new IllegalArgumentException(MSG.getMsg(
                                DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_BAD_LOG, log_arg));
                        }
                    } else if (arg.startsWith("-jdbcurl=")) {
                        jdbc_url = arg.substring(arg.indexOf('=') + 1);
                    } else if (arg.startsWith("-jdbcuser=")) {
                        jdbc_user = arg.substring(arg.indexOf('=') + 1);
                    } else if (arg.startsWith("-jdbcpassword=")) {
                        jdbc_password = arg.substring(arg.indexOf('=') + 1);
                    } else if (arg.startsWith("-file=")) {
                        dbsetup_file = arg.substring(arg.indexOf('=') + 1);
                    } else {
                        throw new IllegalArgumentException(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_BAD_ARG,
                            arg));
                    }
                }
            } else {
                throw new IllegalArgumentException(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_HELP));
            }

            if (!do_export && !do_setup && !do_clear && !do_uninstall && !do_uninstallsetup) {
                throw new IllegalArgumentException(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_BAD_OP,
                    op_requested));
            }

            // if we are exporting, the file is the file we are writing to; otherwise, it should be an existing XML file
            if (do_export) {
                if ((dbsetup_file == null) || (dbsetup_file.trim().length() == 0)) {
                    throw new IllegalArgumentException(MSG.getMsg(
                        DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_BAD_DBSETUPFILE, dbsetup_file));
                }
            } else {
                File f = new File(dbsetup_file);
                if (!f.exists()) {
                    throw new IllegalArgumentException(MSG.getMsg(
                        DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_BAD_DBSETUPFILE, dbsetup_file));
                }

                dbsetup_file = f.getAbsolutePath();
            }

            if (jdbc_url == null) {
                throw new IllegalArgumentException(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_BAD_JDBCURL));
            }

            if (dbsetup_file == null) {
                throw new IllegalArgumentException(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_BAD_DBSETUPFILE,
                    ""));
            }
        } catch (Exception iae) {
            System.out.println(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_USAGE, iae.getMessage()));
            return;
        }

        System.out.println(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_OPTIONS, op_requested, jdbc_log_enabled,
            jdbc_url, jdbc_user, dbsetup_file));

        DBSetup dbsetup = new DBSetup(jdbc_url, jdbc_user, jdbc_password, jdbc_log_enabled);

        try {
            boolean ok = true;

            if (do_export) {
                dbsetup.export(dbsetup_file);
                ok = true; // all failures will be in form of exceptions
            }

            if (do_setup) {
                dbsetup.setup(dbsetup_file);
                ok = true; // all failures will be in form of exceptions
            } else if (do_clear) {
                ok = dbsetup.clear(dbsetup_file);
            } else if (do_uninstall) {
                ok = dbsetup.uninstall(dbsetup_file);
            } else if (do_uninstallsetup) {
                ok = dbsetup.uninstall(dbsetup_file);
                if (ok) {
                    dbsetup.setup(dbsetup_file);
                }
            }

            if (ok) {
                System.out.println(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_OK));
            } else {
                System.out.println(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_NOT_OK));
            }
        } catch (Exception e) {
            System.out.println(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_CMDLINE_ERROR, e.getMessage()));
            e.printStackTrace(System.out);
        }

        return;
    }

    /**
     * Creates a node via the target document which is a duplicate of the <code>source</code> node. The returned node is
     * not placed in the target document, the caller should place the returned node in the target document where it
     * deems appropriate. If <code>deep</code> is <code>true</code>, any child nodes of <code>source</code> are also
     * deeply copied.
     *
     * @param  target the document used to create the node
     * @param  source the node to copy
     * @param  deep   if <code>true</code>, all child nodes of <code>source</code> are also deeply copied
     *
     * @return the new node that was duplicated
     */
    private Node copyNode(Document target, Node source, boolean deep) {
        Node ret_new_node;

        String node_name = source.getNodeName();
        String node_value = source.getNodeValue();

        switch (source.getNodeType()) {
        case Node.ELEMENT_NODE: {
            Element new_elem = target.createElement(node_name);
            ret_new_node = new_elem;

            NamedNodeMap map = source.getAttributes();

            for (int iMap = 0; iMap < map.getLength(); iMap++) {
                Node attr = map.item(iMap);
                new_elem.setAttribute(attr.getNodeName(), attr.getNodeValue());
            }

            break;
        }

        case Node.COMMENT_NODE: {
            ret_new_node = target.createComment(node_name);
            break;
        }

        case Node.TEXT_NODE: {
            ret_new_node = target.createTextNode(node_name);
            break;
        }

        default: {
            // we don't care about any other type of node, don't copy it
            ret_new_node = null;
            break;
        }
        }

        if (ret_new_node != null) {
            ret_new_node.setNodeValue(node_value);

            if (deep) {
                importChildNodes(ret_new_node, source, deep);
            }
        }

        return ret_new_node;
    }

    /**
     * Copies the source node and places it after the <code>after</code> node. Setting <code>deep</code> to <code>
     * true</code> means you want to also copy the child nodes of <code>source</code>.
     *
     * @param after
     * @param source
     * @param deep
     */
    private void importNodeAfter(Node after, Node source, boolean deep) {
        Node nodeNew = copyNode(after.getOwnerDocument(), source, deep);

        // Append the node to the target
        Node nodeNext = after.getNextSibling();

        if (nodeNext != null) {
            after.getParentNode().insertBefore(nodeNew, after);
        } else {
            after.getParentNode().appendChild(nodeNew);
        }
    }

    /**
     * All child nodes of <code>source</code> are copied and appended to the parent node.
     *
     * @param parent
     * @param source
     * @param deep   if <code>true</code>, deeply copies all child nodes of <code>source</code>
     */
    private void importChildNodes(Node parent, Node source, boolean deep) {
        NodeList listChildren = source.getChildNodes();

        for (int i = 0; i < listChildren.getLength(); i++) {
            parent.appendChild(copyNode(parent.getOwnerDocument(), listChildren.item(i), deep));
        }
    }

    /**
     * Reads in a DBSetup XML file. The file can be found either in this object's classloader or on the file system.
     *
     * @param  file
     *
     * @return the DOM document of the DBSetup XML file
     *
     * @throws IOException
     * @throws SAXException
     */
    private Document readDocument(String file) throws IOException, SAXException {
        Document docResult;

        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(file);

        if (stream == null) {
            docResult = readDocument(file, null);
        } else {
            docResult = readDocument(stream, null);
        }

        return docResult;
    }

    /**
     * Reads in either a file or stream that contains DBSetup XML content. <code>source</code> can be either a String (a
     * file name) or an InputStream. If <code>after</code> is not <code>null</code>, the XML content that is read in
     * will have its top-most node appended to that <code>after</code> node.
     *
     * @param  source either a String or InputStream
     * @param  after  the node where to append the new XML data (may be <code>null</code>)
     *
     * @return the document that was read in
     *
     * @throws IOException
     * @throws SAXException
     */
    private Document readDocument(Object source, Node after) throws IOException, SAXException {
        Document ret_document_result = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler());

            File source_file = null;

            if (source instanceof String) {
                source_file = new File((String) source);
                ret_document_result = builder.parse(source_file);
            } else if (source instanceof InputStream) {
                ret_document_result = builder.parse((InputStream) source);
            } else {
                throw new IOException("source=" + source.getClass());
            }

            Node node_root = ret_document_result.getDocumentElement();

            // Make sure we have a DBSetup XML file.
            if (node_root.getNodeName().equalsIgnoreCase(DBSETUP_ROOT_ELEMENT_NAME) == false) {
                if (source instanceof String) {
                    throw new IOException(LOG.getMsgString(DbUtilsI18NResourceKeys.DBSETUP_SOURCE_NOT_VALID, source
                        .toString()));
                }

                throw new IOException(LOG.getMsgString(DbUtilsI18NResourceKeys.DBSETUP_SOURCE_NOT_VALID, "<stream>"));
            }

            // Look for include tags
            NodeList listNodes = node_root.getChildNodes();

            for (int iNode = 0; iNode < listNodes.getLength(); iNode++) {
                Node node = listNodes.item(iNode);

                if (node.getNodeName().equalsIgnoreCase("include") == true) {
                    NamedNodeMap map = node.getAttributes();

                    for (int iAttr = 0; iAttr < map.getLength(); iAttr++) {
                        Node nodeMap = map.item(iAttr);

                        if (nodeMap.getNodeName().equalsIgnoreCase("file") == true) {
                            File fileInclude = new File(nodeMap.getNodeValue());

                            if (fileInclude.isAbsolute() == false) {
                                if (!(source instanceof String)) {
                                    throw new IOException(LOG
                                        .getMsgString(DbUtilsI18NResourceKeys.DBSETUP_PATHS_NOT_RELATIVE_TO_STREAM));
                                }

                                fileInclude = new File(source_file.getParentFile(), nodeMap.getNodeValue());
                            }

                            readDocument(fileInclude.getAbsolutePath(), node);
                            node_root.removeChild(node);
                        }
                    }
                } else if (after != null) {
                    importNodeAfter(after, node, true);
                }
            }
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }

        return ret_document_result;
    }

    /**
     * Performs the full database setup.
     *
     * @param  file_name the path to the database setup XML file
     *
     * @throws Exception
     *
     * @see    #setup(String, String, boolean, boolean)
     */
    public void setup(String file_name) throws Exception {
        setup(file_name, null, false, false);
    }

    /**
     * Performs the actual database setup. This will read the source XML file and will perform the necessary SQL to
     * create the schema and insert the data.
     *
     * @param  file_name  the file that contains the DBSetup XML content.
     * @param  table_name if not <code>null</code>, only this table will be setup
     * @param  data_only  if <code>true</code> does not create the schema; only inserts data into an existing schema
     * @param  do_delete  if <code>true</code>, and <code>table_name</code> was specified, this will delete all rows
     *                    from that table before inserting the new data. The table must exist. It only makes sense to
     *                    use this with <code>data_only</code> set to <code>true</code>.
     *
     * @throws Exception
     */
    public void setup(String file_name, String table_name, boolean data_only, boolean do_delete) throws Exception {
        int created_views = 0;
        int created_tables = 0;
        int created_indexes = 0;

        try {
            Document doc = readDocument(file_name);
            Node root_node = doc.getDocumentElement();

            // Make sure we can connect to the database
            connect();
            log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CONNECTED_TO_DB, m_jdbcUrl, m_username);

            // let's get the known types of all our supported databases
            Collection<TypeMap> column_types_map = TypeMap.loadKnownTypeMaps();

            // process the tables

            List<Table> tables = Table.getTables(root_node, getDatabaseType(), this);

            for (Table table : tables) {
                if (table_name != null) {
                    if (!table_name.equalsIgnoreCase(table.getName())) {
                        continue;
                    }

                    if (do_delete) {
                        table.clear();
                    }
                }

                log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_SETTING_UP_TABLE, table.getName());

                if (!data_only) {
                    // we were told that we can create the table
                    try {
                        // Only attempt to create the table if the table tag has columns
                        if (table.getColumns().size() > 0) {
                            table.create(column_types_map);
                            created_tables++;
                            log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CREATED_TABLE, table.getName());
                        }
                    } catch (SQLException e) {
                        handleFatalSQLException(e, DbUtilsI18NResourceKeys.DBSETUP_CREATED_TABLE_ERROR, table.getName());
                    }

                    // create the indexes

                    Collection<Index> indexes = table.getIndexes();

                    for (Index index : indexes) {
                        try {
                            index.create();
                            log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CREATED_INDEX, index.getName(),
                                index.getTable().getName());
                            created_indexes++;
                        } catch (SQLException e) {
                            handleFatalSQLException(e, DbUtilsI18NResourceKeys.DBSETUP_CREATED_INDEX_ERROR, index
                                .getName(), index.getTable().getName());
                        }
                    }
                }

                // Create the Data

                try {
                    DataSet dataset = table.getDataSet();
                    int rows_created = dataset.create();
                    log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CREATED_ROWS, rows_created, table.getName());
                } catch (SQLException e) {
                    handleFatalSQLException(e, DbUtilsI18NResourceKeys.DBSETUP_CREATED_ROWS_ERROR, table.getName());
                }
            }

            // process views

            Collection<View> views = View.getViews(root_node, getDatabaseType(), this);

            for (View view : views) {
                log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_SETTING_UP_VIEW, view.getName());

                if (!data_only) {
                    // create the view
                    try {
                        view.create(column_types_map);
                        log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CREATED_VIEW, view.getName());
                        created_views++;
                    } catch (SQLException e) {
                        handleFatalSQLException(e, DbUtilsI18NResourceKeys.DBSETUP_CREATED_VIEW_ERROR, view.getName());
                    }
                }
            }

            // log results

            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_SETUP_TABLES, created_tables);
            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_SETUP_INDEXES, created_indexes);
            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_SETUP_VIEWS, created_views);
        } finally {
            disconnect();
        }

        return;
    }

    /**
     * Throws an SQL exception that wraps the given exception, with the new exception having a descriptive error message
     * consisting of the <code>msg_key</code> bundle message plus all messages of the given exception.
     *
     * @param  e       the original exception that occurred
     * @param  msg_key the message key from the resource bundle
     * @param  params  the parameters to replace the resource bundle message's placeholders
     *
     * @throws SQLException
     */
    private void handleFatalSQLException(SQLException e, String msg_key, Object... params) throws SQLException {
        String err_msg;

        if (m_consoleMode) {
            err_msg = MSG.getMsg(msg_key, params);
        } else {
            err_msg = LOG.getMsgString(msg_key, params);
        }

        String exception_string = DbUtil.getSQLExceptionString(e);
        String full_error_msg = err_msg + " [" + exception_string + ']';
        SQLException wrapper_exception = new SQLException(full_error_msg, e.getSQLState(), e.getErrorCode());
        wrapper_exception.initCause(e);
        log(LogPriority.FATAL, e, DbUtilsI18NResourceKeys.DBSETUP_FATAL_SQL_EXCEPTION, full_error_msg);
        throw wrapper_exception;
    }

    /**
     * Purges the entire schema of all existing data. The schema (tables, views, etc) are left intact.
     *
     * @param  file_name the path to the database setup XML file
     *
     * @return <code>true</code> if all data was successfully deleted; <code>false</code> if at least one failure
     *         occurred when trying remove data from the tables
     *
     * @throws Exception
     *
     * @see    #clear(String, String)
     * @see    #uninstall(String)
     */
    public boolean clear(String file_name) throws Exception {
        return this.clear(file_name, null);
    }

    /**
     * Purges data from the schema. If <code>table</code> is not <code>null</code>, only that table will be cleared of
     * data; the remaining tables will have their data remain as-is.
     *
     * @param  file_name  the path to the database setup XML file
     * @param  table_name the table to clear, if <code>null</code>, then all tables are cleared
     *
     * @return <code>true</code> if all data was successfully deleted; <code>false</code> if at least one failure
     *         occurred when trying remove data from the tables
     *
     * @throws Exception
     *
     * @see    #uninstall(String)
     */
    public boolean clear(String file_name, String table_name) throws Exception {
        boolean ret_ok = false; // assume a failure will occur
        int modified_tables_count = 0;
        int failed_tables_count = 0;

        try {
            Document doc = readDocument(file_name);
            Node root_node = doc.getDocumentElement();

            // Make sure we can connect to the database
            connect();
            log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CONNECTED_TO_DB, m_jdbcUrl, m_username);

            // remove data from tables in reverse order of their creation
            // to bypass dependency constraints.  This and our two-passes we make should be able to clear
            // out all data - I hope :)
            List<Table> tables = Table.getTables(root_node, getDatabaseType(), this);
            List<Table> failed_tables = new ArrayList<Table>();
            Collections.reverse(tables);

            // our first pass
            for (Table table : tables) {
                if ((table_name != null) && (table.getName().compareToIgnoreCase(table_name) != 0)) {
                    continue;
                }

                try {
                    table.clear();
                    modified_tables_count++;
                    log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CLEARED_TABLE, table.getName());
                } catch (SQLException e) {
                    failed_tables_count++;
                    failed_tables.add(table); // add it to the list so we try to clear it again in our second pass
                    log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CLEARED_TABLE_ERROR_FIRST_PASS, table
                        .getName(), DbUtil.getSQLExceptionString(e));
                }
            }

            // our second pass - hopefully, we've cleared out data that caused contraint errors in the first pass
            if (failed_tables.size() > 0) {
                log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CLEAR_SECOND_PASS, failed_tables.size());

                for (Table table : failed_tables) {
                    if ((table_name != null) && (table.getName().compareToIgnoreCase(table_name) != 0)) {
                        continue;
                    }

                    try {
                        table.clear();

                        // hooray! we were able to finally clear out all the data
                        modified_tables_count++;
                        failed_tables_count--;
                        log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CLEARED_TABLE, table.getName());
                    } catch (SQLException e) {
                        // crap - there is still a problem causing us to be unable to clear the data
                        log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CLEARED_TABLE_ERROR_SECOND_PASS, table
                            .getName(), DbUtil.getSQLExceptionString(e));
                    }
                }
            }

            // log Results

            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_CLEAR_CLEARED_TABLES, modified_tables_count);
            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_CLEAR_FAILED_TABLES, failed_tables_count);

            ret_ok = (failed_tables_count == 0);
        } finally {
            disconnect();
        }

        return ret_ok;
    }

    /**
     * This is the reverse of setup - that is, it removes a schema from a database, deleting all data with it.
     *
     * @param  file_name the file that contains the DBSetup XML content.
     *
     * @return <code>true</code> if all views and tables were successfully removed; <code>false</code> if at least one
     *         failure occurred when trying remove views and tables
     *
     * @throws Exception
     */
    public boolean uninstall(String file_name) throws Exception {
        boolean ret_ok = false; // assume a failure will occur
        int uninstalled_views = 0;
        int failed_views = 0;
        int modified_tables = 0;
        int failed_tables = 0;

        try {
            Document doc = readDocument(file_name);
            Node root_node = doc.getDocumentElement();

            // Make sure we can connect to the database
            connect();
            log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CONNECTED_TO_DB, m_jdbcUrl, m_username);

            // drop views first

            Collection<View> views = View.getViews(root_node, getDatabaseType(), this);

            for (View view : views) {
                try {
                    view.drop();
                    uninstalled_views++;
                    log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_DROPPED_VIEW, view.getName());
                } catch (SQLException e) {
                    failed_views++;
                    log(LogPriority.ERROR, DbUtilsI18NResourceKeys.DBSETUP_DROPPED_VIEW_ERROR, view.getName(), DbUtil
                        .getSQLExceptionString(e));
                }
            }

            View.uninstallCleanup(this);

            // drop tables - do so in reverse order of their creation to bypass dependency constraints

            List<Table> tables = Table.getTables(root_node, getDatabaseType(), this);
            Collections.reverse(tables);

            for (Table table : tables) {
                try {
                    table.drop();
                    modified_tables++;
                    log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_DROPPED_TABLE, table.getName());
                } catch (SQLException e) {
                    failed_tables++;
                    log(LogPriority.ERROR, DbUtilsI18NResourceKeys.DBSETUP_DROPPED_TABLE_ERROR, table.getName(), DbUtil
                        .getSQLExceptionString(e));
                }
            }

            Table.uninstallCleanup(this);

            // log results

            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_UNINSTALL_DROPPED_VIEWS, uninstalled_views);
            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_UNINSTALL_DROPPED_TABLES, modified_tables);
            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_UNINSTALL_FAILED_VIEWS, failed_views);
            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_UNINSTALL_FAILED_TABLES, failed_tables);

            ret_ok = ((failed_views + failed_tables) == 0);
        } finally {
            disconnect();
        }

        return ret_ok;
    }

    /**
     * Exports an existing schema to a DBSetup XML file.
     *
     * <p><b>NOTE:</b> you cannot use the generated XML file for input into DBSetup. This exported XML file is only for
     * reference purposes; it is not generated in a way that can be used to recreate the DB. Use your database vendor's
     * backup/restore utilities to export databases for backup and recovery.</p>
     *
     * @param  file the XML file that will contain the exported schema
     *
     * @throws Exception
     */
    public void export(String file) throws Exception {
        int created_tables = 0;

        try {
            // Make sure we can connect to the database
            connect();
            log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_CONNECTED_TO_DB, m_jdbcUrl, m_username);

            // Create the DBSetup XML file

            Document doc = createNewDBSetupXmlDocument();
            Node warning = doc.createComment(MSG.getMsg(DbUtilsI18NResourceKeys.DBSETUP_EXPORT_WARNING_NOTICE,
                new Date()));
            Element root_element = doc.createElement(DBSETUP_ROOT_ELEMENT_NAME + "-export");

            doc.appendChild(warning);
            root_element.setAttribute("name", file);
            doc.appendChild(root_element);

            // Find Tables

            Collection<Table> tables = Table.getTables(getDatabaseType(), this, m_username);
            for (Table table : tables) {
                log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_EXPORT_FOUND_TABLE, table.getName());

                Element elemTab = doc.createElement("table");
                elemTab.setAttribute("name", table.getName());
                root_element.appendChild(elemTab);

                // Get Columns

                Iterator iterCols = table.getColumns().iterator();

                while (iterCols.hasNext() == true) {
                    Column col = (Column) iterCols.next();

                    log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_EXPORT_FOUND_COLUMN, table.getName(), col
                        .getName());

                    Element elemChild = doc.createElement("column");
                    elemChild.setAttribute("name", col.getName());
                    elemChild.setAttribute("type", col.getType());
                    elemChild.setAttribute("size", String.valueOf(col.getSize()));

                    if (col.isRequired() == true) {
                        elemChild.setAttribute("required", String.valueOf(col.isRequired()));
                    }

                    elemTab.appendChild(elemChild);
                }

                // Get Data

                DataSet dataset = table.getDataSet();

                while (dataset.next()) {
                    Element elemChild = doc.createElement("data");

                    int iCols = table.getColumns().size();

                    for (int i = 0; i < iCols; i++) {
                        Data data = dataset.getData(i);
                        elemChild.setAttribute(data.getColumnName(), data.getValue());
                    }

                    elemTab.appendChild(elemChild);
                }

                created_tables++;
            }

            writeDBSetupXmlDocument(doc, file);

            // log results

            log(LogPriority.INFO, DbUtilsI18NResourceKeys.DBSETUP_EXPORT_CREATED_TABLES, created_tables, file);
        } finally {
            disconnect();
        }

        return;
    }

    /**
     * Creates a new XML document that will contain DBSetup content.
     *
     * @return the new document
     *
     * @throws Exception
     */
    private Document createNewDBSetupXmlDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new ErrorHandler());

        Document docResult = builder.newDocument();

        return docResult;
    }

    /**
     * Given a document node, writes its content into the given file.
     *
     * @param  doc
     * @param  file
     *
     * @throws Exception
     */
    private void writeDBSetupXmlDocument(Document doc, String file) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer trans = factory.newTransformer();

        trans.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource src = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(file));

        trans.transform(src, result);

        return;
    }

    /**
     * SAX parser error handler that will log XML problems.
     */
    private class ErrorHandler implements org.xml.sax.ErrorHandler {
        /**
         * @see ErrorHandler#fatalError(SAXParseException)
         */
        public void fatalError(SAXParseException e) throws SAXException {
            log(LogPriority.FATAL, e, DbUtilsI18NResourceKeys.DBSETUP_SAX_FATAL, e.getLineNumber(),
                e.getColumnNumber(), e.getMessage());
        }

        /**
         * @see ErrorHandler#error(SAXParseException)
         */
        public void error(SAXParseException e) throws SAXException {
            log(LogPriority.ERROR, e, DbUtilsI18NResourceKeys.DBSETUP_SAX_ERROR, e.getLineNumber(),
                e.getColumnNumber(), e.getMessage());
        }

        /**
         * @see ErrorHandler#warning(SAXParseException)
         */
        public void warning(SAXParseException e) throws SAXException {
            log(LogPriority.WARN, e, DbUtilsI18NResourceKeys.DBSETUP_SAX_WARNING, e.getLineNumber(), e
                .getColumnNumber(), e.getMessage());
        }
    }

    /**
     * Executes the given SQL.
     *
     * @param  sql
     *
     * @throws SQLException
     */
    protected void doSQL(String sql) throws SQLException {
        this.doSQL(sql, false);
    }

    /**
     * This method either executes the given SQL (if <code>returnPreparedStatement</code> is <code>false</code>) or it
     * just prepares it in a statement and expects the caller to execute the statement. The caller must close the
     * returned prepared statement (which occurs if <code>returnPreparedStatement</code> is <code>true</code>)
     *
     * @param  sql
     * @param  returnPreparedStatement if <code>true</code>, the SQL isn't executed, it is just prepared
     *
     * @return the statement (which may be a prepared statement or may be the statement that was executed)
     *
     * @throws SQLException
     */
    protected Statement doSQL(String sql, boolean returnPreparedStatement) throws SQLException {
        Statement stmt;

        log(LogPriority.DEBUG, DbUtilsI18NResourceKeys.DBSETUP_DO_SQL, sql);

        // Cache the original commit option
        boolean committing = this.getConnection().getAutoCommit();
        if (committing) {
            this.getConnection().setAutoCommit(false);
        }

        if (returnPreparedStatement) {
            stmt = this.getConnection().prepareStatement(sql);
        } else {
            stmt = this.getConnection().createStatement();

            try {
                stmt.executeUpdate(sql);
                this.getConnection().commit();
            } catch (SQLException e) {
                try {
                    this.getConnection().rollback();
                } catch (Exception e2) {
                    // Log this?
                }

                throw e;
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }

        // Reset the commit option
        this.getConnection().setAutoCommit(committing);

        return stmt;
    }

    /**
     * Gets the live connection that this object is using. If one hasn't been created yet, <code>null</code> is
     * returned.
     *
     * @return live connection
     */
    protected Connection getConnection() {
        return m_connection;
    }

    /**
     * Gets the database type information of the database this object is currently {@link #getConnection() connected}
     * to. If there is not a live connection to a database, <code>null</code> is returned.
     *
     * @return database type
     */
    protected DatabaseType getDatabaseType() {
        return m_databaseType;
    }

    /**
     * Returns the username that this object will use when connecting to the database.
     *
     * @return username
     */
    protected String getJdbcUser() {
        return m_username;
    }

    /**
     * Creates a new connection to the database. If this object is configured to do logging, the {@link LoggerDriver}
     * will be used to wrap wrapper loggers around the JDBC objects.
     *
     * @return the connection
     *
     * @throws Exception if failed to connect or determine the type of database that was connected to
     */
    private Connection connect() throws Exception {
        if (m_jdbcLogEnabled) {
            // if we want to log SQL, we need to prefix the JDBC URL with our logger driver's URL
            if (!m_jdbcUrl.startsWith(LoggerDriver.JDBC_URL_PREFIX)) {
                m_jdbcUrl = LoggerDriver.JDBC_URL_PREFIX + m_jdbcUrl;
            }

            // if the VM hasn't been told how to configure the logger stuff, let's due it now
            if (System.getProperty(LoggerDriver.PROP_LOGSQLONLY) == null) {
                System.setProperty(LoggerDriver.PROP_LOGSQLONLY, "false");
            }

            // note that this is a way you can override the m_logFileName - set the VM's properties for log listener
            // and log file, and we'll use those and not m_logFileName as the log file. But m_logFileEnabled still has to be
            // true for logging to be enabled.
            if (System.getProperty(LoggerDriver.PROP_LOGLISTENER) == null) {
                if (m_jdbcLogFileName != null) {
                    System.setProperty(LoggerDriver.PROP_LOGLISTENER, FileLoggerListener.class.getName());
                    if (System.getProperty(FileLoggerListener.PROP_LOGFILE) == null) {
                        System.setProperty(FileLoggerListener.PROP_LOGFILE, m_jdbcLogFileName);
                    }
                } else {
                    System.setProperty(LoggerDriver.PROP_LOGLISTENER, StdOutLoggerListener.class.getName());
                }
            }
        }

        m_connection = DbUtil.getConnection(m_jdbcUrl, m_username, m_password);

        try {
            m_databaseType = DatabaseTypeFactory.getDatabaseType(m_connection);

            // MySQL complains if autocomit is true and you try to commit.
            // DDL operations are not transactional anyhow.
            m_connection.setAutoCommit(false);
        } catch (Exception e) {
            // what probably happened was we connected to a database that we do not support
            // let's close the connection and throw this exception back out
            try {
                m_connection.close();
            } catch (Exception ignore) {
            }

            m_connection = null;
            m_databaseType = null;
            throw e;
        }

        return m_connection;
    }

    /**
     * If this object is currently connected, this will close that live connection.
     */
    private void disconnect() {
        try {
            m_connection.close();
        } catch (Exception e) {
        } finally {
            m_connection = null;
            m_databaseType = null;
        }

        return;
    }

    /**
     * Logs the given message. This will divert the message to the logger if we are not in console mode. If we are in
     * console mode, the message goes to stdout in the user's locale.
     *
     * @param priority the priority of the message
     * @param msg_key  the bundle message
     * @param params   the parameters to the bundle message placeholders
     */
    private void log(LogPriority priority, String msg_key, Object... params) {
        if (m_consoleMode) {
            // use MSG to get the message in the user's locale
            System.out.println(priority.toString() + ": " + MSG.getMsg(msg_key, params));
        } else {
            switch (priority) {
            case DEBUG: {
                LOG.debug(msg_key, params);
                break;
            }

            case INFO: {
                LOG.info(msg_key, params);
                break;
            }

            case WARN: {
                LOG.warn(msg_key, params);
                break;
            }

            case ERROR: {
                LOG.error(msg_key, params);
                break;
            }

            case FATAL: {
                LOG.fatal(msg_key, params);
                break;
            }
            }
        }

        return;
    }

    /**
     * Logs the given message with the exception. This will divert the message to the logger if we are not in console
     * mode. If we are in console mode, the message goes to stdout in the user's locale.
     *
     * @param priority  the priority of the message
     * @param exception the exception to log with the message
     * @param msg_key   the bundle message
     * @param params    the parameters to the bundle message placeholders
     */
    private void log(LogPriority priority, Exception exception, String msg_key, Object... params) {
        if (m_consoleMode) {
            // use MSG to get the message in the user's locale
            System.out.println(priority.toString() + ": " + MSG.getMsg(msg_key, params));
            exception.printStackTrace(System.out);
        } else {
            switch (priority) {
            case DEBUG: {
                LOG.debug(exception, msg_key, params);
                break;
            }

            case INFO: {
                LOG.info(exception, msg_key, params);
                break;
            }

            case WARN: {
                LOG.warn(exception, msg_key, params);
                break;
            }

            case ERROR: {
                LOG.error(exception, msg_key, params);
                break;
            }

            case FATAL: {
                LOG.fatal(exception, msg_key, params);
                break;
            }
            }
        }

        return;
    }

    /**
     * Used to determine how to log a message (used for both logging and console output).
     */
    private enum LogPriority {
        DEBUG, INFO, WARN, ERROR, FATAL
    }
}
package org.rhq.core.db.setup;

import java.util.List;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class H2Column extends Column {
    protected H2Column(Node node, Table table) throws SAXException {
        super(node, table);
    }

    protected String getDefaultCommand(List cmds) {
        if (m_strType.equalsIgnoreCase("auto") || (m_iDefault == Column.DEFAULT_SEQUENCE_ONLY)) {
            return "";
        }

        return "DEFAULT nextval('" + getSequenceName() + "')";
    }

    protected void getPreCreateCommands(List cmds) {
        if (m_strType.equalsIgnoreCase("auto")) {
            return;
        }

        if (hasDefault()) {
            switch (getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY: {
                cmds.add(0, "" //
                    + " CREATE SEQUENCE " + getSequenceName() //
                    + " START WITH " + this.getInitialSequence() //
                    + " INCREMENT BY " + this.getIncrementSequence() //
                    + " CACHE 10");
                break;
            }
            }
        }
    }

    protected void getDropCommands(List cmds) {
        if (hasDefault()) {
            switch (getDefault()) {
            case Column.DEFAULT_SEQUENCE_ONLY:
            case Column.DEFAULT_AUTO_INCREMENT: {
                cmds.add("DROP SEQUENCE " + getSequenceName());
                break;
            }
            }
        }
    }

    private String getSequenceName() {
        String tableName = this.m_strTableName.toUpperCase();
        String columnName = this.getName().toUpperCase();
        String sequenceName = tableName + '_' + columnName + "_SEQ";
        return sequenceName;
    }
}

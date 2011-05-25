package org.rhq.core.domain.drift;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "RHQ_SNAPSHOT")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_SNAPSHOT_ID_SEQ")
public class Snapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    private int id;

    //private SnapshotMetadata metadata = new SnapshotMetadata();

    @Lob
    @Column(name = "DATA")
    private Blob data;

    @Column(name = "DATA_SIZE")
    private long dataSize;

    @Column(name = "CTIME")
    private long ctime = System.currentTimeMillis();

    public int getId() {
        return id;
    }

//    public SnapshotMetadata getMetadata() {
//        return metadata;
//    }
//
//    public InputStream getData() {
//        return data;
//    }
//
//    public void setData(InputStream data) {
//        this.data = data;
//    }

    public Blob getBlob() {
        return data;
    }

    public InputStream getData() throws SQLException {
        return data.getBinaryStream();
    }

    public void setData(Blob blob) {
        this.data = blob;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long size) {
        dataSize = size;
    }

    public long getCtime() {
        return ctime;
    }
}

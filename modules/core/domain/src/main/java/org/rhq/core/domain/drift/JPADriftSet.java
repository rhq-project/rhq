/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.domain.drift;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.FetchType.EAGER;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "RHQ_DRIFT_SET")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_SET_ID_SEQ")
public class JPADriftSet implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @OneToMany(cascade = PERSIST, fetch = EAGER)
    @JoinColumn(name = "DRIFT_SET_ID")
    private Set<JPADrift> drifts = new LinkedHashSet<JPADrift>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<JPADrift> getDrifts() {
        return drifts;
    }

    public void addDrift(JPADrift drift) {
        drifts.add(drift);
    }
}

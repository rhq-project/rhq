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

package org.rhq.enterprise.server.plugins.rhnhosted.certificate;

import java.security.Provider;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.signers.DSASigner;
import org.bouncycastle.jce.provider.JDKDSASigner;
import org.bouncycastle.jce.provider.JDKKeyFactory;
/**
 * @author pkilambi
 * This module is taken from Satellite
 */
/**
 * This JCE provider exists solely to hack around the fact that the bouncycastle
 * provider does not offer <code>RIPEMD160 + DSA</code> signature processing. It
 * cobbles the existing bits from bouncycastle together to add processing of
 * these signatures.
 * 
 */
final class RhnSecurityProvider extends Provider {

    /**
     * The name under which this provider registers
     */
    public static final String NAME = "RHNSP";
    private static final String INFO = "RHN Security Provider (provides RIPEMD160WithDSA signatures)";

    /**
     * Create the provider
     */
    public RhnSecurityProvider() {
        super(NAME, 1.0, INFO);
        put("KeyFactory.DSA", JDKKeyFactory.DSA.class.getName());
        put("Signature.RIPEMD160WithDSA", RIPEMD160WithDSA.class.getName());
    }

    /**
     * The signer that combines <code>RIPEMD160</code> hashing with DSA signing.
     */
    public static class RIPEMD160WithDSA extends JDKDSASigner {

        public RIPEMD160WithDSA() {
            super(new RIPEMD160Digest(), new DSASigner());
        }

    }

}

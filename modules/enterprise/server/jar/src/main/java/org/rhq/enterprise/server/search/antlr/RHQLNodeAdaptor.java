/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.search.antlr;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;

/**
 * @author Joseph Marques
 */
public class RHQLNodeAdaptor extends CommonTreeAdaptor {
    List<String> errorMessages = new ArrayList<String>();

    @Override
    public Object create(Token payload) {
        return new RHQLNode(payload);
    }

    @Override
    public Object errorNode(TokenStream input, Token start, Token stop, RecognitionException e) {
        if (e instanceof NoViableAltException) {
            NoViableAltException noAlt = (NoViableAltException) e;
            String description = noAlt.grammarDecisionDescription;
            int position = noAlt.index;
            errorMessages.add(description + ":" + position);
        }
        return super.errorNode(input, start, stop, e);
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}

package org.rhq.enterprise.server.search.antlr;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;

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

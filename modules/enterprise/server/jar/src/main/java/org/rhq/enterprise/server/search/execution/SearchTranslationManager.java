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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.search.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.server.search.RHQLLexer;
import org.rhq.enterprise.server.search.RHQLParser;
import org.rhq.enterprise.server.search.antlr.RHQLNodeAdaptor;
import org.rhq.enterprise.server.search.assist.SearchAssistant;
import org.rhq.enterprise.server.search.assist.SearchAssistantFactory;
import org.rhq.enterprise.server.search.translation.SearchTranslator;
import org.rhq.enterprise.server.search.translation.SearchTranslatorFactory;
import org.rhq.enterprise.server.search.translation.antlr.RHQLAdvancedTerm;
import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.search.translation.antlr.RHQLSimpleTerm;
import org.rhq.enterprise.server.search.translation.antlr.RHQLTerm;
import org.rhq.enterprise.server.search.translation.antlr.RHQLTreeOperator;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragment;

/**
 * @author Joseph Marques
 */
public class SearchTranslationManager {
    
    private static final Log LOG = LogFactory.getLog(SearchTranslationManager.class);

    private SearchSubsystem context;
    private String expression;
    private SearchTranslator translator;
    private SearchAssistant assistant;

    private RHQLLexer lexer;
    private RHQLParser parser;
    private RHQLNodeAdaptor adaptor;

    private String entity;
    private String alias;

    private Subject subject;

    public SearchTranslationManager(String alias, Subject subject, SearchSubsystem context) {
        this.subject = subject;
        this.context = context;
        this.entity = this.context.getEntityClass().getSimpleName();
        this.alias = alias;
    }

    public void setExpression(String expression) {
        if (expression == null) {
            expression = "";
        } else {
            expression = expression.trim();
        }
        this.expression = expression;

        this.translator = SearchTranslatorFactory.getTranslator(subject, this.context);
        this.assistant = SearchAssistantFactory.getAssistant(subject, this.context);

        ANTLRStringStream input = new ANTLRStringStream(this.expression); // Create an input character stream from standard in
        this.lexer = new RHQLLexer(input); // Create an echoLexer that feeds from that stream

        CommonTokenStream tokens = new CommonTokenStream(this.lexer); // Create a stream of tokens fed by the lexer
        this.parser = new RHQLParser(tokens); // Create a parser that feeds off the token stream

        this.adaptor = new RHQLNodeAdaptor();
        parser.setTreeAdaptor(adaptor);
    }

    public String getJPQLSelectStatement() throws Exception {
        String jpql = "SELECT " + alias + " FROM " + entity + " " + alias + " WHERE " + getJPQLWhereFragment();
        if (LOG.isDebugEnabled()) {
            LOG.debug("JPQL was:");
            PrintUtils.printJPQL(jpql.split(" "));
        }
        return jpql;
    }

    public String getJPQLWhereFragment() throws Exception {
        RHQLParser.searchExpression_return searchAST = parser.searchExpression();
        CommonTree searchExpressionTree = searchAST.getTree();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Search was: " + expression);
            LOG.debug("Errors found: " + adaptor.getErrorMessages());
            LOG.debug("Tree was:");
            PrintUtils.print(searchExpressionTree, "");
        }
        String fragment = generateJPQL(searchExpressionTree);
        return fragment;
    }

    private String generateJPQL(CommonTree tree) {
        StringBuilder builder = new StringBuilder();
        builder.append(" ( ");

        Token token = tree.getToken();
        if (token == null) {
            return null;
        }

        RHQLTreeOperator operator = getTreeOperatorFromTokenType(token.getType());
        if (operator != null) {
            for (int childIndex = 0; childIndex < tree.getChildCount(); childIndex++) {
                CommonTree child = (CommonTree) tree.getChild(childIndex);
                if (childIndex != 0) {
                    builder.append(" " + operator.name() + " ");
                }
                builder.append(generateJPQL(child));
            }
        } else {
            List<RHQLTerm> terms = getFromAST(tree);
            boolean first = true;
            for (RHQLTerm nextTerm : terms) {
                if (first == false) {
                    builder.append(" AND "); // implicit AND for IN-clause values
                } else {
                    first = false;
                }

                RHQLAdvancedTerm advancedTerm = null;
                if (nextTerm instanceof RHQLSimpleTerm) {
                    RHQLSimpleTerm simpleTerm = (RHQLSimpleTerm) nextTerm;
                    advancedTerm = new RHQLAdvancedTerm(null, assistant.getPrimarySimpleContext(), null,
                        RHQLComparisonOperator.EQUALS, simpleTerm.getValue());
                } else {
                    advancedTerm = (RHQLAdvancedTerm) nextTerm;
                }

                SearchFragment searchFragment = translator.getSearchFragment(alias, advancedTerm);
                String jpqlFragment = searchFragment.getJPQLFragment();
                if (searchFragment.getType() == SearchFragment.Type.PRIMARY_KEY_SUBQUERY) {
                    jpqlFragment = " " + alias + ".id IN (" + jpqlFragment + ")";
                }

                builder.append(jpqlFragment);
            }
        }

        builder.append(" ) ");
        return builder.toString();
    }

    public static List<RHQLTerm> getFromAST(CommonTree tree) {
        List<RHQLTerm> terms = new ArrayList<RHQLTerm>();

        // simple text match
        if (tree.getToken().getType() == RHQLLexer.IDENT) {
            String value = PrintUtils.collapseStringChildren(tree);
            RHQLTerm nextTerm = new RHQLSimpleTerm(value);
            terms.add(nextTerm);
            return terms;
        }

        // advanced query match
        String lineage = null;
        String path = null;
        String param = null;
        CommonTree contextTree = (CommonTree) tree.getChild(0);
        if (contextTree != null) {
            for (int childIndex = 0; childIndex < contextTree.getChildCount(); childIndex++) {
                CommonTree child = (CommonTree) contextTree.getChild(childIndex);
                if (child.getToken().getType() == RHQLLexer.LINEAGE) {
                    lineage = PrintUtils.collapseStringChildren(child);
                } else if (child.getToken().getType() == RHQLLexer.PATH) {
                    path = PrintUtils.collapseStringChildren(child);
                } else if (child.getToken().getType() == RHQLLexer.PARAM) {
                    child = (CommonTree) child.getChild(0); // get the IDENT child of PARAM
                    param = PrintUtils.collapseStringChildren(child);
                }
            }
        } else {
            throw new IllegalStateException("There is an issue with AST tree construction. Token "
                + tree.getToken().getText() + " has no children");
        }

        String value = null;
        if (tree.getChildCount() > 1) {
            CommonTree valueTree = (CommonTree) tree.getChild(1);
            for (int childIndex = 0; childIndex < valueTree.getChildCount(); childIndex++) {
                CommonTree indentChildTree = (CommonTree) valueTree.getChild(childIndex);
                value = PrintUtils.collapseStringChildren(indentChildTree);

                int type = tree.getToken().getType();
                RHQLComparisonOperator operator = getComparisonOperatorFromTokenType(type, value);

                RHQLTerm nextTerm = new RHQLAdvancedTerm(lineage, path, param, operator, value);
                terms.add(nextTerm);
            }
        }

        return terms;
    }

    public static RHQLComparisonOperator getComparisonOperatorFromTokenType(int tokenType, String value) {
        boolean nullValue = value.trim().toLowerCase().equals("null");

        switch (tokenType) {
        case RHQLLexer.OP_EQUALS:
            return nullValue ? RHQLComparisonOperator.NULL : RHQLComparisonOperator.EQUALS;
        case RHQLLexer.OP_EQUALS_STRICT:
            return nullValue ? RHQLComparisonOperator.NULL : RHQLComparisonOperator.EQUALS_STRICT;
        case RHQLLexer.OP_NOT_EQUALS:
            return nullValue ? RHQLComparisonOperator.NOT_NULL : RHQLComparisonOperator.NOT_EQUALS;
        case RHQLLexer.OP_NOT_EQUALS_STRICT:
            return nullValue ? RHQLComparisonOperator.NOT_NULL : RHQLComparisonOperator.NOT_EQUALS_STRICT;
        case RHQLLexer.OP_LESS_THAN:
            return RHQLComparisonOperator.LESS_THAN;
        case RHQLLexer.OP_GREATER_THAN:
            return RHQLComparisonOperator.GREATER_THAN;
        default:
            throw new IllegalArgumentException("There is no known RHQLComparisonOperator for token type " + tokenType);
        }
    }

    public static RHQLTreeOperator getTreeOperatorFromTokenType(int tokenType) {
        switch (tokenType) {
        case RHQLLexer.AND:
            return RHQLTreeOperator.AND;
        case RHQLLexer.OR:
            return RHQLTreeOperator.OR;
        default:
            return null;
        }
    }

    private static class PrintUtils {
        public static void printJPQL(String[] tokens) {
            String indent = "";
            List<String> lineBreakers = Arrays.asList("SELECT", "FROM", "WHERE", "AND", "OR", "(", ")");
            for (String next : tokens) {
                if (next.equals("(")) {
                    indent = "   " + indent;
                }

                if (lineBreakers.contains(next)) {
                    //System.out.println();
                    LOG.debug("indent = " + indent);
                }

                LOG.debug(next);
                if (next.equals(")")) {
                    indent = indent.substring(3);
                }
            }
        }

        public static void print(CommonTree tree, String indent) {
            if (tree == null) {
                return;
            }
            /*
            if (tree instanceof CommonErrorNode) {
                CommonErrorNode errorNode = (CommonErrorNode) tree;
                RecognitionException error = errorNode.trappedException;
                int position = error.index;
                if (error instanceof MismatchedTokenException) {
                    MismatchedTokenException mismatchedError = (MismatchedTokenException) error;
                    int expecting = mismatchedError.expecting;
                    String expectedToken = parser.getTokenNames()[expecting];
                }
            }
            */

            Token token = tree.getToken();
            if (token == null) {
                return;
            }

            LOG.debug(indent + token.getText());
            if (isStringNode(token)) {
                LOG.debug(collapseStringChildren(tree));
            } else {
                //System.out.println();
                for (int childIndex = 0; childIndex < tree.getChildCount(); childIndex++) {
                    CommonTree child = (CommonTree) tree.getChild(childIndex);
                    print(child, indent + "   ");
                }
            }
        }

        private static boolean isStringNode(Token token) {
            switch (token.getType()) {
            case RHQLLexer.PATH:
            case RHQLLexer.IDENT:
                return true;
            default:
                return false;
            }
        }

        public static String collapseStringChildren(CommonTree tree) {
            StringBuilder builder = new StringBuilder();
            for (int childIndex = 0; childIndex < tree.getChildCount(); childIndex++) {
                CommonTree child = (CommonTree) tree.getChild(childIndex);
                builder.append(child.getText());
            }
            return builder.toString();
        }
    }
}

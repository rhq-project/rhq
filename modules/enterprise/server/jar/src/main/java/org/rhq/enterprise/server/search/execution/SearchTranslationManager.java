package org.rhq.enterprise.server.search.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;

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
import org.rhq.enterprise.server.search.translation.jpql.SearchFragmentType;

public class SearchTranslationManager {

    private SearchSubsystem context;
    private String expression;
    private SearchTranslator translator;
    private SearchAssistant assistant;

    private RHQLLexer lexer;
    private RHQLParser parser;
    private RHQLNodeAdaptor adaptor;

    private String entity;
    private String alias;

    public SearchTranslationManager(SearchSubsystem context) {
        this.context = context;
        this.entity = this.context.getEntityClass().getSimpleName();
        this.alias = this.entity.toLowerCase();
    }

    public void setExpression(String expression) {
        this.expression = expression;

        this.translator = SearchTranslatorFactory.getTranslator(this.context);
        this.assistant = SearchAssistantFactory.getAssistant(this.context);

        ANTLRStringStream input = new ANTLRStringStream(this.expression); // Create an input character stream from standard in
        this.lexer = new RHQLLexer(input); // Create an echoLexer that feeds from that stream

        CommonTokenStream tokens = new CommonTokenStream(this.lexer); // Create a stream of tokens fed by the lexer
        this.parser = new RHQLParser(tokens); // Create a parser that feeds off the token stream

        this.adaptor = new RHQLNodeAdaptor();
        parser.setTreeAdaptor(adaptor);
    }

    public String getJPQLSelectStatement() throws Exception {
        String jpql = "SELECT " + alias + " FROM " + entity + " " + alias + " WHERE " + getJPQLWhereFragment();
        //System.out.println("JPQL was:");
        PrintUtils.printJPQL(jpql.split(" "));
        //System.out.println();
        return jpql;
    }

    public String getJPQLWhereFragment() throws Exception {
        RHQLParser.searchExpression_return searchAST = parser.searchExpression();

        //System.out.println("Search was: " + expression);
        CommonTree searchExpressionTree = (CommonTree) searchAST.getTree();
        //System.out.println("Errors found: " + adaptor.getErrorMessages());
        //System.out.println("Tree was:");
        PrintUtils.print(searchExpressionTree, "");
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
                String jpqlFragment = searchFragment.getFragment();
                if (searchFragment.getType() == SearchFragmentType.PRIMARY_KEY_SUBQUERY) {
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
                    //System.out.print(indent);
                }

                //System.out.print(next);
                //System.out.print(" ");

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

            //System.out.print(indent + token.getText());
            if (isStringNode(token)) {
                //System.out.println(collapseStringChildren(tree));
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

    public static void main(String[] args) throws Exception {
        SearchTranslationManager manager = new SearchTranslationManager(SearchSubsystem.RESOURCE);
        manager.setExpression("(name = rhq and category = server) or plugin = jbossas");
        String jpql = manager.getJPQLSelectStatement();
    }
}

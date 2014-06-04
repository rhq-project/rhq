package org.rhq.cassandra.schema;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.datastax.driver.core.Session;

import org.rhq.core.util.obfuscation.PicketBoxObfuscator;

/**
 * Executes an CQL statement that is nested in a &lt;step&gt; element. This class is also used if the step element
 * declares the <code>class</code> attribute with one of the following as its value,
 *
 * <ul>
 *   <li>CQL (case insensitive)</li>
 *   <li>CQLStep</li>
 * </ul>
 *
 * @author John Sanda
 */
public class CQLStep implements Step {

    private String parametrizeQuery;

    private String query;

    private Session session;

    public CQLStep(String query) {
        parametrizeQuery = query;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void bind(Properties properties) {
        Set<String> foundProperties = new HashSet<String>();
        Pattern regex = Pattern.compile("%([^%]*)%");
        Matcher matchPattern = regex.matcher(parametrizeQuery);
        while (matchPattern.find()) {
            String matchedString = matchPattern.group();
            String property = matchedString.substring(1, matchedString.length() - 1);
            foundProperties.add(property);
        }

        query = parametrizeQuery;

        if( foundProperties.size() !=0 && properties == null){
            throw new RuntimeException("No properties provided but " + foundProperties.size()
                + " required for binding.");
        } else if (foundProperties.size() != 0) {
            for (String foundProperty : foundProperties) {
                String propertyValue = properties.getProperty(foundProperty);
                if (propertyValue == null) {
                    throw new RuntimeException("Cannot bind query. Property [" + foundProperty + "] not found.");
                }

                query = query.replaceAll("%" + foundProperty + "%", propertyValue);
            }

            if (query.toUpperCase().contains("CREATE USER")) {
                query =  replaceEncodedPassword(query);
            }
        }
    }

    private String replaceEncodedPassword(String step) {
        int firstQuoteIndex = step.indexOf("'");
        int lastQuoteIndex = step.lastIndexOf("'");
        String encodedPassword = step.substring(++firstQuoteIndex, lastQuoteIndex);
        String decodedPassword = PicketBoxObfuscator.decode(encodedPassword);
        String decodedStep = step.replace(encodedPassword, decodedPassword);
        return decodedStep;
    }

    @Override
    public void execute() {
        session.execute(query);
    }

    @Override
    public String toString() {
        return parametrizeQuery;
    }
}

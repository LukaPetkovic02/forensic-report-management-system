package com.example.backend.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BooleanQueryParser {

    private static final String[] SEARCH_FIELDS = {
            "threatName",
            "behaviorDescription",
            "reportText",
            "organizationName",
            "forensicExpert1",
            "forensicExpert2"
    };

    public Query parse(String input) {

        List<String> tokens = tokenize(input);

        BoolQuery.Builder bool = new BoolQuery.Builder();

        String currentOperator = "AND"; // default

        for (String token : tokens) {

            if (token.equalsIgnoreCase("AND")) {
                currentOperator = "AND";
                continue;
            }

            if (token.equalsIgnoreCase("OR")) {
                currentOperator = "OR";
                continue;
            }

            Query fieldQuery = buildFieldQuery(token);

            if (currentOperator.equals("AND")) {
                bool.must(fieldQuery);
            } else {
                bool.should(fieldQuery);
            }
        }

        return Query.of(q -> q.bool(bool.build()));
    }

    private List<String> tokenize(String input) {

        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(input);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add("\"" + matcher.group(1) + "\"");
            } else {
                tokens.add(matcher.group(2));
            }
        }

        return tokens;
    }

    private Query buildFieldQuery(String token) {

        boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

        String cleaned = token.replaceAll("^\"|\"$", "");

        if (isPhrase) {
            return Query.of(q -> q.multiMatch(mm -> mm
                    .query(cleaned)
                    .fields(List.of(SEARCH_FIELDS))
                    .type(TextQueryType.Phrase)
            ));
        }

        return Query.of(q -> q.multiMatch(mm -> mm
                .query(cleaned)
                .fields(List.of(SEARCH_FIELDS))
        ));
    }
}

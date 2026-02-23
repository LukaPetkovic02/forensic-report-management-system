package com.example.backend.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
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
        List<String> postfix = toPostfix(tokens);

        Stack<Query> stack = new Stack<>();

        for (String token : postfix) {

            if (token.equalsIgnoreCase("AND")) {

                Query right = stack.pop();
                Query left = stack.pop();

                stack.push(Query.of(q -> q.bool(b -> b
                        .must(left)
                        .must(right)
                )));

            } else if (token.equalsIgnoreCase("OR")) {

                Query right = stack.pop();
                Query left = stack.pop();

                stack.push(Query.of(q -> q.bool(b -> b
                        .should(left)
                        .should(right)
                        .minimumShouldMatch("1")
                )));

            } else if (token.equalsIgnoreCase("NOT")) {

                Query operand = stack.pop();

                stack.push(Query.of(q -> q.bool(b -> b
                        .mustNot(operand)
                )));

            } else {
                stack.push(buildFieldQuery(token));
            }
        }

        return stack.pop();
    }

    private List<String> tokenize(String input) {

        List<String> tokens = new ArrayList<>();

        Pattern pattern = Pattern.compile(
                "\"[^\"]+\"" +          // fraza
                        "|\\bAND\\b" +
                        "|\\bOR\\b" +
                        "|\\bNOT\\b" +
                        "|\\(" +
                        "|\\)" +
                        "|[^\\s()]+"
        );

        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }

    private Query buildFieldQuery(String token) {

        boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

        String cleaned = token.replaceAll("^\"|\"$", "");

        BoolQuery.Builder bool = new BoolQuery.Builder();

        if (isPhrase) {
            return Query.of(q -> q.multiMatch(mm -> mm
                    .query(cleaned)
                    .fields(List.of(SEARCH_FIELDS))
                    .type(TextQueryType.Phrase)
            ));
        } else {
            bool.should(Query.of(q -> q.multiMatch(mm -> mm
                    .query(cleaned)
                    .fields(List.of(SEARCH_FIELDS))
            )));
        }

        // KEYWORD fields (exact match)
        bool.should(Query.of(q -> q.term(t -> t
                .field("classification")
                .value(cleaned)
        )));

        bool.should(Query.of(q -> q.term(t -> t
                .field("hash")
                .value(cleaned)
        )));

        return Query.of(q -> q.bool(bool.build()));
    }

    private List<String> toPostfix(List<String> tokens) {

        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();

        for (String token : tokens) {

            if (token.equalsIgnoreCase("AND") ||
                    token.equalsIgnoreCase("OR") ||
                    token.equalsIgnoreCase("NOT")) {

                while (!operators.isEmpty()
                        && !operators.peek().equals("(")
                        && precedence(operators.peek()) >= precedence(token)) {

                    output.add(operators.pop());
                }

                operators.push(token);

            } else if (token.equals("(")) {
                operators.push(token);

            } else if (token.equals(")")) {

                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }

                operators.pop(); // ukloni "("

            } else {
                output.add(token);
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }

        return output;
    }

    private int precedence(String operator) {
        return switch (operator.toUpperCase()) {
            case "NOT" -> 3;
            case "AND" -> 2;
            case "OR" -> 1;
            default -> 0;
        };
    }
}

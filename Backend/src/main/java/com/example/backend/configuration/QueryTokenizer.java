package com.example.backend.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryTokenizer {
    public static List<String> tokenize(String input){
        List<String> tokens = new ArrayList<>();

        Matcher matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(input);

        while(matcher.find()){
            if(matcher.group(1) != null){
                tokens.add("\"" + matcher.group(1) + "\"");
            } else{
                tokens.add(matcher.group(2));
            }
        }

        return tokens;
    }
}

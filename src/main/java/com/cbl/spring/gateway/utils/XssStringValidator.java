package com.cbl.spring.gateway.utils;

import lombok.experimental.UtilityClass;
import org.owasp.esapi.ESAPI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@UtilityClass
public  final  class  XssStringValidator {

    public static boolean isGoodHtmlString(String value){
        boolean isValid = true;
        if (value != null) {
            // NOTE: It's highly recommended to use the ESAPI library and uncomment the following line to
            // avoid encoded attacks.
            value = ESAPI.encoder().canonicalize(value);

            // Avoid null characters
            value = value.replaceAll("", "");

            // Avoid anything between script tags
            Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }

            // Avoid anything in a src='...' type of expression
            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }
            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }
            // Remove any lonesome </script> tag
            scriptPattern = Pattern.compile("</script>", Pattern.CASE_INSENSITIVE);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }
            // Remove any lonesome <script ...> tag
            scriptPattern = Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }
            // Avoid eval(...) expressions
            scriptPattern = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }
            // Avoid expression(...) expressions
            scriptPattern = Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            if(getMatchCount(scriptPattern, value)>0){

                return false;
            }
            // Avoid javascript:... expressions
            scriptPattern = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
            if(getMatchCount(scriptPattern, value)>0){
                isValid = false;
                return isValid;
            }
            // Avoid vbscript:... expressions
            scriptPattern = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }
            // Avoid onload= expressions
            scriptPattern = Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            if(getMatchCount(scriptPattern, value)>0){
                return false;
            }
        }
        return true;
    }

    static  int getMatchCount(Pattern pattern, String value){
        Matcher matcher = pattern.matcher(value);
        int count = 0;
        while (matcher.find())
            count++;
        return  count;
    }
}

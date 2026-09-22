package com.uit.feature.login.infrastructure;

import com.uit.shared.exception.AppException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoogleUserInfo {

    private GoogleUserInfo() {
    }

    public static String verifiedEmail(String json) {
        String email = stringField(json, "email");
        if (email == null || email.isBlank()) {
            throw new AppException("Google did not return an email address.");
        }
        if (!"true".equals(rawField(json, "email_verified"))) {
            throw new AppException("Google did not confirm this email address.");
        }
        return email;
    }

    public static String accessToken(String json) {
        String error = stringField(json, "error_description");
        if (error == null) {
            error = stringField(json, "error");
        }
        if (error != null) {
            throw new AppException("Google sign-in failed: " + error);
        }
        String token = stringField(json, "access_token");
        if (token == null || token.isBlank()) {
            throw new AppException("Google sign-in failed. Try again.");
        }
        return token;
    }

    private static String stringField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(json);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        if (!value.contains("\\")) {
            return value;
        }
        return value.replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String rawField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(true|false)").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}

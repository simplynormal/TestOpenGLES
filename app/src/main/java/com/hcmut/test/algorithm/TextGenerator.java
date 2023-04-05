package com.hcmut.test.algorithm;

public class TextGenerator {
    public static String generateText(String text, int length) {
        if (text.length() > length) {
            return text.substring(0, length);
        }
        StringBuilder builder = new StringBuilder(text);
        while (builder.length() < length) {
            builder.append(" ");
        }
        return builder.toString();
    }
}

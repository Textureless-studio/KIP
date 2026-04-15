package org.Textureless.kip.listeners.death;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConditionExpressionEvaluator {

    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
            "^\\s*(?<left>.+?)\\s*(?<operator>>=|<=|==|!=|>|<)\\s*(?<right>.+?)\\s*$"
    );

    public boolean evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            return false;
        }

        String normalized = expression.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }

        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }

        Matcher matcher = COMPARISON_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return false;
        }

        String left = stripQuotes(matcher.group("left"));
        String right = stripQuotes(matcher.group("right"));
        String operator = matcher.group("operator").toLowerCase(Locale.ROOT);

        Double leftNumber = tryParseDouble(left);
        Double rightNumber = tryParseDouble(right);
        if (leftNumber != null && rightNumber != null) {
            return compareNumbers(leftNumber, rightNumber, operator);
        }

        return compareStrings(left, right, operator);
    }

    private boolean compareNumbers(double left, double right, String operator) {
        return switch (operator) {
            case ">" -> left > right;
            case "<" -> left < right;
            case ">=" -> left >= right;
            case "<=" -> left <= right;
            case "==" -> Double.compare(left, right) == 0;
            case "!=" -> Double.compare(left, right) != 0;
            default -> false;
        };
    }

    private boolean compareStrings(String left, String right, String operator) {
        return switch (operator) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            default -> false;
        };
    }

    private Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String stripQuotes(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        return trimmed;
    }
}

package utility;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Validate {

    private static Validate instance;

    private Validate() {
        instance = this;
    }

    public static Validate that() {
        return instance == null ? instance = new Validate() : instance;
    }

    public <T> ValidationResult valuesArePresentInMaps(List<Map<T, T>> requiredValues, List<Map<T, T>> actualValues, Method method) {
        requiredValues = new LinkedList<>(requiredValues);
        actualValues = new LinkedList<>(actualValues);
        requiredLoop:
        for (int j = requiredValues.size() - 1; j >= 0; j--) {
            for (int i = actualValues.size() - 1; i >= 0; i--) {
                if (valuesArePresentInMap(requiredValues.get(j), actualValues.get(i), method)) {
                    actualValues.remove(i);
                    requiredValues.remove(j);
                    continue requiredLoop;
                }
            }
        }
        return requiredValues.isEmpty() ? ValidationResult.pass() : ValidationResult.fail("No matches found for the following data: %s", requiredValues.toString());
    }

    public ValidationResult valuesArePresentInMap(Map<String, String> expectedValues, Function<String, String> function, Method method) {
        StringBuilder builder = new StringBuilder();
        expectedValues.keySet().forEach(field -> {
            ValidationResult result = compare(expectedValues.get(field), function.apply(field), method);
            builder.append(result.passed() ? "" : String.format("%s: %s%s", field, result.getReason(), builder.toString().isBlank() ? "" : System.lineSeparator()));
        });
        return builder.toString().isBlank() ? ValidationResult.pass() : ValidationResult.fail(builder.toString());
    }

    public <T> boolean valuesArePresentInMap(Map<T, T> requiredValues, Map<T, T> actualValues, Method method) {
        for (T key : requiredValues.keySet()) {
            if (!actualValues.containsKey(key) || compare(requiredValues.get(key), actualValues.get(key), method).failed()) {
                return false;
            }
        }
        return true;
    }

    public ValidationResult valuesArePresentInList(List<?> expected, List<?> actual) {
        expected = new LinkedList<>(expected);
        actual = new LinkedList<>(actual);
        for (int i = expected.size() - 1; i >= 0; i--) {
            if (actual.remove(expected.get(i))) {
                expected.remove(expected.get(i));
            }
        }
        return expected.isEmpty() ? ValidationResult.pass() : ValidationResult.fail("The following values were not found: %s", expected.toString());
    }

    public <T> ValidationResult compare(T expected, T actual, Method method) {
        switch (method) {
            case EQUALS:
                return actual.equals(expected) ? ValidationResult.pass() : ValidationResult.fail("Expected {%s} Actual {%s}", String.valueOf(expected), String.valueOf(actual));
            case EQUALS_CASE_INSENSITIVE:
                return asString(actual).equalsIgnoreCase(asString(expected)) ? ValidationResult.pass() : ValidationResult.fail("Expected {%s} Actual {%s}", asString(expected), asString(actual));
            case CONTAINS:
                return asString(actual).contains(asString(expected)) ? ValidationResult.pass() : ValidationResult.fail("Expected {%s} to contain {%s}", asString(actual), asString(expected));
            default:
                throw new Error("Invalid validation method");
        }
    }

    private <T> String asString(T object) {
        if (!(object instanceof String)) {
            throw new ClassCastException("Given object is not a string");
        }
        return (String) object;
    }

    public ValidationResult listInAlphabeticalOrder(List<String> values, boolean ascending) {
        if (values.size() > 1) {
            Iterator<String> iter = values.iterator();
            String current, previous = iter.next();
            while (iter.hasNext()) {
                current = iter.next();
                if (ascending ? previous.compareTo(current) > 0 : previous.compareTo(current) == 0) {
                    return ValidationResult.fail("List is not in %s alphabetical order", ascending ? "ascending" : "descending");
                }
                previous = current;
            }
        }
        return ValidationResult.pass();
    }


    public static class ValidationResult {
        private final boolean status;
        private final String message;

        private ValidationResult(boolean status, String message) {
            this.status = status;
            this.message = message;
        }

        public static ValidationResult pass() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String message, String... format) {
            return new ValidationResult(false, String.format(message, format));
        }

        public ValidationResult assertPass() {
            if (failed()) throw new AssertionError(message);
            return this;
        }

        public ValidationResult assertFail() {
            if (passed()) throw new AssertionError("Expected a false validation result, however was true");
            return this;
        }

        public boolean passed() {
            return status == true;
        }

        public boolean failed() {
            return status == false;
        }

        public String getReason() {
            return message;
        }

    }

    /**
     * Validation mechanism, e.g compare via equals, contains, or case insensitive etc
     */
    public enum Method {
        EQUALS, EQUALS_CASE_INSENSITIVE, CONTAINS
    }
}

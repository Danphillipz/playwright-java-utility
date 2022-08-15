package com.ensono.utility;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A utility class which allows for various types of validation, with results returned in an {@link ValidationResult} object
 */
public class Validate {

    private static Validate instance;

    private Validate() {
        instance = this;
    }

    /**
     * Gets the singleton instance of the validator
     * @return
     */
    public static Validate that() {
        return instance == null ? instance = new Validate() : instance;
    }

    /**
     * Checks that all the key/values in each map are found in the other map
     * <br>Validation is unique, e.g:
     * <br>requiredValues =  [{k = "1", v = "1"},{k = "1", v = "1"}]
     * <br>actualValues = [{k = "1", v = "1"}]
     * <br>result = Fail, as only one occurrence of the key value pair was found in the second map
     * @param requiredValues the list of maps which should be found in the second map
     * @param actualValues the list of maps to check for matches within
     * @param method The comparison method when checking two values {@link Method}
     * @return {@link ValidationResult}
     * @param <T> type of input being compared
     */
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

    /**
     * For each key in the map, applies that to the function and compares the result with the keys corresponding map value
     * @param expectedValues Map of the expected values
     * @param function Function to apply to each map key, where the result should be the value to compare the keys corresponding value
     * @param method The comparison method when checking two values {@link Method}
     * @return {@link ValidationResult}
     */
    public ValidationResult valuesArePresentInMap(Map<String, String> expectedValues, Function<String, String> function, Method method) {
        StringBuilder builder = new StringBuilder();
        expectedValues.keySet().forEach(field -> {
            ValidationResult result = compare(expectedValues.get(field), function.apply(field), method);
            builder.append(result.passed() ? "" : String.format("%s: %s%s", field, result.getReason(), builder.toString().isBlank() ? "" : System.lineSeparator()));
        });
        return builder.toString().isBlank() ? ValidationResult.pass() : ValidationResult.fail(builder.toString());
    }

    /**
     * Checks whether all of the key/values can be found within the second map
     * @param requiredValues Map of required values
     * @param actualValues Map of values to check against
     * @param method The comparison method when checking two values {@link Method}
     * @return true if all keys/values found in the second map
     * @param <T> input type for this method
     */
    public <T> boolean valuesArePresentInMap(Map<T, T> requiredValues, Map<T, T> actualValues, Method method) {
        for (T key : requiredValues.keySet()) {
            if (!actualValues.containsKey(key) || compare(requiredValues.get(key), actualValues.get(key), method).failed()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that all required values are found in the second list
     * <br>Validation is unique, e.g:
     * <br>expected =  ["Eg", "Eg", "Eg"]
     * <br>actual = ["Eg", "Eg"]
     * <br>result = Fail, as only 2/3 occurrences of the value "Eg" was found in the second list
     * @param expected List of values to check for
     * @param actual List to values to check values are present inside
     * @return {@link ValidationResult}
     */
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

    /**
     * Compares two inputs using the specified {@link Method}
     * @param expected Expected Value
     * @param actual Actual Value
     * @param method {@link Method}
     * @return {@link ValidationResult}
     * @param <T> input type to this method
     */
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

    /**
     * Checks that the list is in either ascending or descending alphabetical order <b>lexicographically</b> via {@link String#compareTo(String)}
     * @param values List to check is in order
     * @param ascending true if ascending ("A", "B", "C"), false if decending ("C", "B", "A")
     * @return {@link ValidationResult}
     */
    public ValidationResult listInAlphabeticalOrder(List<String> values, boolean ascending) {
        if (values.size() > 1) {
            Iterator<String> iter = values.iterator();
            String current, previous = iter.next();
            while (iter.hasNext()) {
                current = iter.next();
                if (ascending ? previous.compareTo(current) > 0 : previous.compareTo(current) == 0) {
                    return ValidationResult.fail("List is not in %s alphabetical order. Failing elements {%s, %s}", ascending ? "ascending" : "descending", previous, current);
                }
                previous = current;
            }
        }
        return ValidationResult.pass();
    }

    /**
     * A utility class which contains information regarding performed validation
     */
    public static class ValidationResult {
        private final boolean status;
        private final String message;

        private ValidationResult(boolean status, String message) {
            this.status = status;
            this.message = message;
        }

        /**
         * Creates a {@link ValidationResult} object indicating validation was successful
         * @return {@link ValidationResult}
         */
        public static ValidationResult pass() {
            return new ValidationResult(true, null);
        }

        /**
         * Creates a {@link ValidationResult} object indicating validation failed
         * @param message Reason validation failed
         * @param format Any formatting to perform on the failure message
         * @return {@link ValidationResult}
         */
        public static ValidationResult fail(String message, String... format) {
            return new ValidationResult(false, String.format(message, format));
        }

        /**
         * Asserts that the result was a pass
         * @return {@link ValidationResult}
         * @throws AssertionError if validation failed
         */
        public ValidationResult assertPass() {
            if (failed()) throw new AssertionError(message);
            return this;
        }
        /**
         * Asserts that the result was a fail
         * @return {@link ValidationResult}
         * @throws AssertionError if validation passed
         */
        public ValidationResult assertFail() {
            if (passed()) throw new AssertionError("Expected a false validation result, however was true");
            return this;
        }

        /**
         * Checks if the validation passed
         * @return true if passed
         */
        public boolean passed() {
            return status == true;
        }

        /**
         * Checks if the validation failed
         * @return true if failed
         */
        public boolean failed() {
            return status == false;
        }

        /**
         * Gets the reason for failure
         * @return String - Reason for failure
         */
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

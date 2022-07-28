package ensono;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ValidatorTest {

    List<Map<String, String>> actualMaps = Arrays.asList(Map.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4"),
            Map.of("key1", "v1", "key2", "v2", "key3", "v3", "key4", "v4"));

    @Test
    public void testValuesArePresentInMapsMissingKeys() {
        String failureReason = Validate.that().valuesArePresentInMaps(List.of(Map.of("keyv1", "v1", "k4", "v4")), actualMaps, Validate.Method.EQUALS)
                .assertFail().getReason();
        Assert.assertTrue(failureReason.contains("keyv1=v1") && failureReason.contains("k4=v4"));
    }

    @Test
    public void testValuesArePresentInMapsExtraKey() {
        Map<String, String> map = Map.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "key5", "v5");
        Assert.assertTrue(Validate.that().valuesArePresentInMaps(List.of(map),
                actualMaps, Validate.Method.EQUALS).assertFail().getReason().endsWith("[" + map + "]"));
    }

    @Test
    public void testValuesArePresentInMapsUnmatchedValue() {
        Map<String, String> map = Map.of("key1", "v2", "key2", "v2", "key3", "k3noMatch");
        Assert.assertTrue(Validate.that().valuesArePresentInMaps(List.of(map),
                actualMaps, Validate.Method.EQUALS).assertFail().getReason().endsWith("[" + map + "]"));
    }

    @Test
    public void testValuesArePresentInMapsMatchingValuesEquals() {
        Validate.that().valuesArePresentInMaps(List.of(Map.of("k1", "v1", "k4", "v4")),
                actualMaps, Validate.Method.EQUALS).assertPass();
    }

    @Test
    public void testValuesArePresentInMapsMatchingValuesContains() {
        Validate.that().valuesArePresentInMaps(List.of(Map.of("k1", "v", "k4", "v")),
                actualMaps, Validate.Method.CONTAINS).assertPass();
        Validate.that().valuesArePresentInMaps(List.of(Map.of("k1", "a", "k4", "b")),
                actualMaps, Validate.Method.CONTAINS).assertFail();
    }


    @Test
    public void testValuesArePresentInListDuplicateValueMissing() {
        Assert.assertTrue(Validate.that().valuesArePresentInList(
                        Arrays.asList("Val1", "Val2", "Val2", "Val4"),
                        Arrays.asList("Val1", "Val2", "Val3", "Val4"))
                .assertFail().getReason().endsWith("[Val2]"));
    }

    @Test
    public void testValuesPresentInListMatchingValues() {
        Validate.that().valuesArePresentInList(
                        Arrays.asList("Val1", "Val2", "Val2", "Val4"),
                        Arrays.asList("Val1", "Val2", "Val2", "Val4", "Val5"))
                .assertPass();
    }

    @Test
    public void testListIsAlphabetical() {
        List<String> data = Arrays.asList("aaa", "aab", "abb", "baa", "bba", "bbb", "c", "d");
        Validate.that().listInAlphabeticalOrder(data, true).assertPass();
        Collections.reverse(data);
        Validate.that().listInAlphabeticalOrder(data, true).assertFail();
        Validate.that().listInAlphabeticalOrder(data, false).assertPass();
    }

}

package com.mastfrog.util.strings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * A VariableInterpolationTest.
 *
 * @author Tim Boudreau
 */
public class VariableInterpolationTest {

    ResolverImpl resolver;

    @Test
    public void testSomeMethod() {
        assertMatch("nothing", "nothing");
        assertMatch("nothing.${GURGLES}", "nothing.${GURGLES}");
        resolver.assertQueriedFor("GURGLES");
        assertMatch("foo.wurgle", "foo.${place}");
        assertMatch("foo.wurgle.bar", "foo.${place}.bar");
        assertMatch("foo.wurgle.epsilon", "foo.${place}.${deployState}");
        assertMatch("foo.wurgle.epsilon.wug", "foo.${place}.${deployState}.wug");
        assertMatch("foo.wurgleepsilon.wug", "foo.${place}${deployState}.wug");
        assertMatch("foo.wurgle.wug.epsilon", "foo.${place}.wug.${deployState}");
        // And all the trivial ways to have an off-by-one error:
        assertMatch("foo.${STA", "foo.${STA");
        assertMatch("foo.${STA${", "foo.${STA${");
        assertMatch("foo.${}STA${}}}", "foo.${}STA${}}}");
        assertMatch("${${${}}}}${}}", "${${${}}}}${}}");
        assertMatch("${", "${");
        assertMatch("}", "}");
        assertMatch("", "");
    }

    private void assertMatch(String expect, String key) {
        String result = Strings.variableSubstitution(key, "${", "}", resolver);
        assertEquals("Wrong result for '" + key + "'", expect, result);
    }

    @Before
    public void setup() {
        resolver = new ResolverImpl();
    }

    private static class ResolverImpl implements Function<String, Optional<CharSequence>> {

        private final Set<String> queries = new HashSet<>();

        void assertQueriedFor(String... all) {
            Set<String> q = new HashSet<>(this.queries);
            this.queries.clear();
            assertTrue("Queries do not match: " + q + " expected "
                    + Arrays.toString(all),
                    q.containsAll(Arrays.asList(all)));
        }

        @Override
        public Optional<CharSequence> apply(String value) {
            queries.add(value);
            switch (value) {
                case "place":
                    return Optional.of("wurgle");
                case "deployState":
                    return Optional.of("epsilon");
                case "THING":
                    return Optional.of("wugga");
                default:
                    return Optional.empty();
            }
        }
    }
}

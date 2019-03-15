/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.config.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static io.helidon.config.internal.EnvironmentVariables.shouldMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link EnvironmentVariables}.
 */
public class EnvironmentVariablesTest {

    static Map<String, String> env(final String... additionalPairs) {
        return add(new HashMap<>(System.getenv()), additionalPairs);
    }

    static Map<String, String> toMap(final String... pairs) {
        return add(new HashMap<>(), pairs);
    }

    static Map<String, String> add(final Map<String, String> map, final String... additionalPairs) {
        IntStream.range(0, additionalPairs.length / 2)
                 .map(i -> i * 2)
                 .forEach(i -> map.put(additionalPairs[i], additionalPairs[i + 1]));
        return map;
    }

    static int countKeysToMap(final Map<String, String> env) {
        return (int) env.keySet()
                        .stream()
                        .filter(EnvironmentVariables::shouldMap)
                        .count();
    }

    static int expectedMappedSize(final Map<String, String> env) {
        return env.size() + (countKeysToMap(env) * 2);
    }

    static String variant(final String key) {
        return key.replace("_dash_", "-").replace("_", ".");
    }

    static String lowerVariant(final String key) {
        return variant(key).toLowerCase();
    }

    @Test
    public void testCurrentEnvMappings() {
        Map<String, String> env = env();
        int expectedSize = expectedMappedSize(env);

        Map<String, String> mapped = EnvironmentVariables.expand(env);
        assertThat(mapped, is(not(nullValue())));
        assertThat(mapped.size(), is(expectedSize));

        env.forEach((k, v) -> {
            if (shouldMap(k)) {
                assertThat(mapped, hasEntry(k, v));
                assertThat(mapped, hasEntry(variant(k), v));
                assertThat(mapped, hasEntry(lowerVariant(k), v));
            }
        });
    }

    @Test
    public void testDashMapping() {
        Map<String, String> env = toMap("SERVER_EXECUTOR_dash_SERVICE_MAX_dash_POOL_dash_SIZE", "16");
        Map<String, String> mapped = EnvironmentVariables.expand(env);
        assertThat(mapped, is(not(nullValue())));
        assertThat(mapped.size(), is(3));
        assertThat(mapped, hasEntry("SERVER_EXECUTOR_dash_SERVICE_MAX_dash_POOL_dash_SIZE", "16"));
        assertThat(mapped, hasEntry("SERVER.EXECUTOR-SERVICE.MAX-POOL-SIZE", "16"));
        assertThat(mapped, hasEntry("server.executor-service.max-pool-size", "16"));
    }

    @Test
    public void testNotMapped() {
        Map<String, String> env = toMap("k", "v", "_k", "_v", "", "");
        Map<String, String> mapped = EnvironmentVariables.expand(env);
        assertThat(mapped, is(not(nullValue())));
        assertThat(mapped.size(), is(3));
        assertThat(mapped, hasEntry("k", "v"));
        assertThat(mapped, hasEntry("_k", "_v"));
        assertThat(mapped, hasEntry("", ""));
    }
}

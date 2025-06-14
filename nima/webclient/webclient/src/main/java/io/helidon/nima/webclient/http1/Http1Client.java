/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.nima.webclient.http1;

import io.helidon.nima.webclient.HttpClient;
import io.helidon.nima.webclient.WebClient;

/**
 * HTTP/1.1 client.
 */
public interface Http1Client extends HttpClient<Http1ClientRequest, Http1ClientResponse> {
    /**
     * A new fluent API builder to customize instances.
     *
     * @return a new builder
     */
    static Http1ClientBuilder builder() {
        return new Http1ClientBuilder();
    }

    /**
     * Builder for {@link io.helidon.nima.webclient.http1.Http1Client}.
     */
    class Http1ClientBuilder extends WebClient.Builder<Http1ClientBuilder, Http1Client> {

        private Http1ClientBuilder() {
        }

        @Override
        public Http1Client build() {
            return new Http1ClientImpl(this);
        }
    }

}

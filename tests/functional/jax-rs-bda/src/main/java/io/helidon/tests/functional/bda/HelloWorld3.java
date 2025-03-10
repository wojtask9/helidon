/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.tests.functional.bda;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * This resource will not be available given that a synthetic app is not
 * created when other apps exist. Note that {@link Path} is also a BDA.
 */
@Path("/greet3")
public class HelloWorld3 {

    @GET
    public String hello() {
        return "hello3";
    }
}

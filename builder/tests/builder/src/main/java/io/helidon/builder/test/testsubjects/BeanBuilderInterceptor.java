/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.builder.test.testsubjects;

import io.helidon.builder.BuilderInterceptor;

/**
 * See {@link InterceptedBean}.
 */
class BeanBuilderInterceptor implements BuilderInterceptor<DefaultInterceptedBean.Builder> {
    private int callCount;

    @Override
    public DefaultInterceptedBean.Builder intercept(DefaultInterceptedBean.Builder target) {
        if (callCount++ > 0) {
            throw new AssertionError();
        }
        return target.helloMessage("Hello " + target.name());
    }
}

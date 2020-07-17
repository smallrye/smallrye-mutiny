/*
 * Copyright (c) 2019-2020 Red Hat
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.mutiny.context;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class MyThreadContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        MyContext capturedContext = MyContext.get();
        return () -> {
            MyContext movedContext = MyContext.get();
            MyContext.set(capturedContext);
            return () -> {
                MyContext.set(movedContext);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            MyContext movedContext = MyContext.get();
            MyContext.clear();
            return () -> {
                if (movedContext == null)
                    MyContext.clear();
                else
                    MyContext.set(movedContext);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return "MyContext";
    }

}

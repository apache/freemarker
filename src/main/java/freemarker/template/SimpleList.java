/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template;

/**
 * This is a trivial subclass that exists for backward compatibility
 * with the SimpleList from FreeMarker Classic.
 *
 * <p>This class is thread-safe.
 *
 * @deprecated Use SimpleSequence instead.
 * @see SimpleSequence
 */

public class SimpleList extends SimpleSequence {

    public SimpleList() {
    }

    public SimpleList(java.util.List list) {
        super(list);
    }
}


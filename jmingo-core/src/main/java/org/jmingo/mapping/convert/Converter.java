/**
 * Copyright 2013-2014 The JMingo Team
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmingo.mapping.convert;

import com.mongodb.DBObject;

/**
 * Specific converter interface which contains methods to convert DBObject to an object with specified type.
 *
 * @param <T> the type of the class modeled by this {@code Class} object.
 */
@FunctionalInterface
public interface Converter<T> {

    /**
     * Converts the given source into an object with specified type.
     *
     * @param type   the type of target object
     * @param source implementation of {@link DBObject} interface.
     * @return converted object
     */
    T convert(Class<T> type, DBObject source);
}

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
package org.jmingo.parser;

import org.jmingo.exceptions.JMingoParserException;

import java.nio.file.Path;

/**
 * This interface provides API which can be used for parsing data.
 *
 * @param <T> The type of object which will be create after data.
 */
public interface Parser<T> {

    /**
     * Parse data from file with specified path.
     *
     * @param path the path to file to parse
     * @return specific domain object as result of parsing
     * @throws org.jmingo.exceptions.JMingoParserException {@link org.jmingo.exceptions.JMingoParserException}
     */
    T parse(Path path) throws JMingoParserException;
}

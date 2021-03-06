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

import org.jmingo.mapping.marshall.BsonUnmarshaller;
import org.jmingo.mapping.marshall.jackson.JacksonBsonMarshallingFactory;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jmingo.mapping.convert.ConversionUtils.getFirstElement;

/**
 * Default implementation of {@link Converter} interface.
 *
 * @param <T> the type of the class modeled by this {@code Class} object.
 */
public class DefaultConverter<T> implements Converter<T> {

    private static final BsonUnmarshaller BSON_UNMARSHALLER = new JacksonBsonMarshallingFactory().createUnmarshaller();
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConverter.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public T convert(Class<T> type, DBObject source) {
        LOGGER.debug("converts {} in {}", source, type);
        T result;
        source = getFirstElement(source);
        result = BSON_UNMARSHALLER.unmarshall(type, source);
        return result;
    }

}

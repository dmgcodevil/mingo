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
package org.jmingo.mapping.marshall.mongo.callback;

import com.mongodb.BasicDBList;
import org.apache.commons.collections.CollectionUtils;

import java.util.Map;

/**
 * Callback to replace elements in {@link BasicDBList}.
 */
public class BasicDBListReplacementCallback extends AbstractReplacementCallback<BasicDBList> implements ReplacementCallback<BasicDBList> {

    private ReplacementCallback<Map> mapReplacementCallback;

    /**
     * Constructor this parameters.
     *
     * @param replacements the replacements
     */
    public BasicDBListReplacementCallback(Map<String, Object> replacements) {
        super(replacements);
        mapReplacementCallback = new MapReplacementCallback(replacements);
    }

    /**
     * Replace all elements in dbList.
     *
     * @param dbList the dbList to replace
     * @return dbList with replaced elements
     */
    @Override
    public Object doReplace(BasicDBList dbList) {
        if (CollectionUtils.isNotEmpty(dbList)) {
            dbList.replaceAll(this::getReplacement);
        }
        return dbList;
    }
}

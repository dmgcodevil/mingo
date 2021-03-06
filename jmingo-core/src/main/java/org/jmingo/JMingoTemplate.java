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
package org.jmingo;

import com.google.common.collect.Iterables;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jmingo.document.id.IdFieldGenerator;
import org.jmingo.document.id.generator.factory.IdGeneratorFactory;
import org.jmingo.executor.QueryExecutor;
import org.jmingo.mapping.convert.ConverterService;
import org.jmingo.mapping.marshall.BsonMarshaller;
import org.jmingo.mapping.marshall.BsonMarshallingFactory;
import org.jmingo.mapping.marshall.JsonToDBObjectMarshaller;
import org.jmingo.mapping.marshall.jackson.JacksonBsonMarshallingFactory;
import org.jmingo.mapping.marshall.mongo.MongoBsonMarshallingFactory;
import org.jmingo.mongo.MongoDBFactory;
import org.jmingo.mongo.index.Index;
import org.jmingo.query.Criteria;
import org.jmingo.util.DocumentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class that specifies a basic set of JMingo operations.
 */
public class JMingoTemplate {


    private QueryExecutor queryExecutor;
    private MongoDBFactory mongoDBFactory;
    private ConverterService converterService;
    private BsonMarshallingFactory bsonMarshallingFactory = new JacksonBsonMarshallingFactory();
    private BsonMarshallingFactory mongoBsonMarshallingFactory = new MongoBsonMarshallingFactory();
    private BsonMarshaller jacksonBsonMarshaller = bsonMarshallingFactory.createMarshaller();
    private JsonToDBObjectMarshaller mongoBsonMarshaller = mongoBsonMarshallingFactory.createJsonToDbObjectMarshaller();

    private IdFieldGenerator idFieldModifier;

    public JMingoTemplate(QueryExecutor queryExecutor, MongoDBFactory mongoDBFactory, ConverterService converterService,
                          IdGeneratorFactory idGeneratorFactory) {
        this.queryExecutor = queryExecutor;
        this.mongoDBFactory = mongoDBFactory;
        this.converterService = converterService;
        this.idFieldModifier = new IdFieldGenerator(idGeneratorFactory);
    }

    /**
     * Gets current db.
     *
     * @return current db
     */
    public DB getDB() {
        return mongoDBFactory.getDB();
    }

    /**
     * Creates collection.
     *
     * @param document the document to get collection name to create index
     */
    public DBCollection createCollection(Class<?> document) {
        DocumentUtils.assertDocument(document);
        String collectionName = DocumentUtils.getCollectionName(document);
        return mongoDBFactory.getDB().createCollection(collectionName, null);
    }

    /**
     * Drops specified collection by name.
     *
     * @param collectionName the collection name to drop
     */
    public void dropCollection(String collectionName) {
        mongoDBFactory.getDB().getCollection(collectionName).drop();
    }

    /**
     * Drops specified collection by collection name which is taken from {@link org.jmingo.document.annotation.Document#collectionName()}.
     * If collection name isn't specified in annotation then the simple class name of the given type is used.
     *
     * @param type the type
     */
    public void dropCollection(Class<?> type) {
        mongoDBFactory.getDB().getCollection(DocumentUtils.getCollectionName(type)).drop();
    }

    /**
     * Creates index.
     *
     * @param document the document to get collection name to create index
     * @param index    the index to create
     */
    public void ensureIndex(Class<?> document, Index index) {
        DocumentUtils.assertDocument(document);
        String collectionName = DocumentUtils.getCollectionName(document);
        ensureIndex(collectionName, index);
    }

    /**
     * Creates index.
     *
     * @param collectionName the collection name to create index
     * @param index          the index to create
     */
    public void ensureIndex(String collectionName, Index index) {
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        Validate.notNull(index, "index cannot be null or empty");
        DBCollection dbCollection = mongoDBFactory.getDB().getCollection(collectionName);

        DBObject keys;
        DBObject options = null;
        if (MapUtils.isEmpty(index.getKeys())) {
            throw new IllegalArgumentException("necessary specify one or more keys to create an index");
        }
        keys = jacksonBsonMarshaller.marshall(BasicDBObject.class, index.getKeys());
        if (MapUtils.isNotEmpty(index.getOptions())) {
            options = jacksonBsonMarshaller.marshall(BasicDBObject.class, index.getOptions());
        }
        if (options != null) {
            dbCollection.createIndex(keys, options);
        } else {
            dbCollection.createIndex(keys);
        }
    }

    /**
     * Drops index by index name.
     *
     * @param collectionName the collection name to drop index
     * @param indexName      the index name to drop
     */
    public void dropIndex(String collectionName, String indexName) {
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        Validate.notBlank(indexName, "index name cannot be null or empty");

        DBCollection dbCollection = mongoDBFactory.getDB().getCollection(collectionName);
        dbCollection.dropIndex(indexName);
    }

    /**
     * Gets all existing indexes for in the collection.
     *
     * @param collectionName the collection name to find indexes
     * @return indexes in the given collection
     */
    public List<DBObject> getIndexes(String collectionName) {
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        DBCollection dbCollection = mongoDBFactory.getDB().getCollection(collectionName);
        return dbCollection.getIndexInfo();
    }

    /**
     * Gets index by name.
     *
     * @param collectionName the collection name
     * @param indexName      the index name
     * @return index or null if no indexes for the given name
     */
    public DBObject getIndex(String collectionName, final String indexName) {
        DBObject index;
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        Validate.notBlank(collectionName, "indexName cannot be null or empty");
        DBCollection dbCollection = mongoDBFactory.getDB().getCollection(collectionName);
        List<DBObject> dbObjects = dbCollection.getIndexInfo();
        index = Iterables.tryFind(dbObjects, dbObject -> StringUtils.equalsIgnoreCase(dbObject.get("name").toString(), indexName)).orNull();
        return index;
    }

    /**
     * Inserts the object to the collection for the document type of the object to save.
     * as the collection name is the {@link org.jmingo.document.annotation.Document#collectionName()} or simple class name
     * of stored object if collection name isn't explicitly defined in @Document annotation.
     *
     * @param objectToInsert the object to store in the collection
     */
    public void insert(Object objectToInsert) {
        DocumentUtils.assertDocument(objectToInsert);
        String collectionName = DocumentUtils.getCollectionName(objectToInsert);
        insert(objectToInsert, collectionName);
    }

    /**
     * Inserts array of different objects in necessary collections. Method {@link #insert(Object)} is used for each object from array.
     *
     * @param objectsToInsert the objects to insert
     */
    public void insert(Object... objectsToInsert) {
        for (Object objectToInsert : objectsToInsert) {
            insert(objectToInsert);
        }
    }

    /**
     * Inserts the object to the collection for the document type of the object to save.
     *
     * @param objectToInsert the object to store in the collection
     * @param collectionName the collection name
     */
    public void insert(Object objectToInsert, String collectionName) {
        Validate.notNull(objectToInsert, "object to insert cannot be null");
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        idFieldModifier.generateId(objectToInsert);
        DBObject dbObject = jacksonBsonMarshaller.marshall(BasicDBObject.class, objectToInsert);
        mongoDBFactory.getDB().getCollection(collectionName).insert(dbObject);
    }

    /**
     * Updates one or multiple objects from collection that satisfy selection criteria.
     *
     * @param objectToUpdate the object to update
     * @param criteria       the criteria to find objects in the collection that should be updated
     * @return the result of the operation
     */
    public WriteResult update(Object objectToUpdate, Criteria criteria) {
        DocumentUtils.assertDocument(objectToUpdate);
        DBObject updateDocument = jacksonBsonMarshaller.marshall(BasicDBObject.class, objectToUpdate);
        DBObject queryObject = buildQuery(criteria);
        return update(updateDocument, queryObject, objectToUpdate.getClass(), criteria.isUpsert(), criteria.isMulti());
    }

    /**
     * Updates one or multiple objects from collection that satisfy selection criteria.
     *
     * @param objectToUpdate the object to update
     * @param criteria       the criteria to find objects in the collection that should be updated. the parameters 'multi' and 'upsert' are taken from criteria.
     * @param collectionName the collection name
     * @return the result of the operation
     */
    public WriteResult update(Object objectToUpdate, Criteria criteria, String collectionName) {
        Validate.notNull(objectToUpdate, "object to update cannot be null");
        Validate.notNull(criteria, "update criteria should be not null");
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        DBObject updateDocument = jacksonBsonMarshaller.marshall(BasicDBObject.class, objectToUpdate);
        DBObject queryObject = buildQuery(criteria);
        return update(updateDocument, queryObject, collectionName, criteria.isUpsert(), criteria.isMulti());
    }

    /**
     * Updates one or multiple objects (depends on 'multi' value) from collection that satisfy selection criteria (query).
     *
     * @param update        the modifications to apply
     * @param query         the query that specifies criteria used to update documents
     * @param documentClass the type of document
     * @param upsert        if true - inserts document in collection if no documents that satisfy given criteria
     * @param multi         if true then all documents that satisfy condition will be updated otherwise only first matched
     * @return the result of the operation
     */
    public WriteResult update(DBObject update, DBObject query, Class<?> documentClass, boolean upsert, boolean multi) {
        DocumentUtils.assertDocument(documentClass);
        String collectionName = DocumentUtils.getCollectionName(documentClass);
        return update(update, query, collectionName, upsert, multi);
    }

    /**
     * Updates one or multiple objects (depends on 'multi' value) from collection that satisfy selection criteria (query).
     *
     * @param update         the modifications to apply
     * @param query          the query that specifies criteria used to update documents
     * @param collectionName the collection name
     * @param upsert         if true - inserts document in collection if no documents that satisfy given criteria
     * @param multi          if true then all documents that satisfy condition will be updated otherwise only first matched
     * @return the result of the operation
     */
    public WriteResult update(DBObject update, DBObject query, String collectionName, boolean upsert, boolean multi) {
        Validate.notNull(update, "object to update cannot be null");
        Validate.notNull(query, "update query cannot be null");
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        return mongoDBFactory.getDB().getCollection(collectionName).update(query, update, upsert, multi);
    }

    /**
     * Finds all documents from the given collection.
     *
     * @param type           the type of document
     * @param collectionName the collection name
     * @return the list of objects of type
     */
    public <T> List<T> findAll(Class<T> type, String collectionName) {
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        List<T> result = new ArrayList<>();
        DBCursor cursor = mongoDBFactory.getDB().getCollection(collectionName).find();
        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            T item = converterService.lookupConverter(type).convert(type, object);
            result.add(item);
        }
        return result;
    }

    /**
     * Finds all documents from the given collection.
     *
     * @param type the type of document
     * @return the list of objects of type
     */
    public <T> List<T> findAll(Class<T> type) {
        DocumentUtils.assertDocument(type);
        return findAll(type, DocumentUtils.getCollectionName(type));
    }

    /**
     * Finds document by id.
     *
     * @param id   the id value
     * @param type the document type
     * @param <T>  type of document
     * @return document
     */
    public <T> T findById(Object id, Class<T> type) {
        DocumentUtils.assertDocument(type);
        Criteria criteria = Criteria.whereId(id);
        return findOne(criteria, type);
    }

    /**
     * Finds first matched document which satisfies the given criteria.
     *
     * @param criteria the criteria to find document in the collection
     * @param type     the document type
     * @return matched document of type
     */
    public <T> T findOne(Criteria criteria, Class<T> type) {
        Validate.notNull(criteria, "criteria to find-one operation cannot be null or empty");
        T result = null;
        DocumentUtils.assertDocument(type);
        DBObject query = buildQuery(criteria);
        DBCursor cursor = mongoDBFactory.getDB().getCollection(DocumentUtils.getCollectionName(type)).find(query);
        if (cursor.hasNext()) {
            DBObject dbObject = cursor.iterator().next();
            result = converterService.lookupConverter(type).convert(type, dbObject);
        }
        return result;
    }

    /**
     * Finds documents which satisfies given criteria.
     *
     * @param criteria the criteria to find documents in the collection
     * @param type     the document type
     * @return list of found documents of type
     */
    public <T> List<T> find(Criteria criteria, Class<T> type) {
        Validate.notNull(criteria, "criteria to find operation cannot be null or empty");
        List<T> result = new ArrayList<>();
        DocumentUtils.assertDocument(type);
        DBObject query = buildQuery(criteria);
        DBCursor cursor = mongoDBFactory.getDB().getCollection(DocumentUtils.getCollectionName(type)).find(query);
        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            T item = converterService.lookupConverter(type).convert(type, object);
            result.add(item);
        }
        return result;
    }

    /**
     * Removes the object from the collection by id.
     *
     * @param object the object to remove
     */
    public WriteResult remove(Object object) {
        DocumentUtils.assertDocument(object);
        return remove(object, DocumentUtils.getCollectionName(object));
    }

    /**
     * Removes the object from the collection by document id.
     *
     * @param object     the object to remove
     * @param collection the collection name
     */
    public WriteResult remove(Object object, String collection) {
        Validate.notNull(object, "object to remove cannot be null or empty");
        Validate.notBlank(collection, "collectionName cannot be null or empty");
        Object idValue = DocumentUtils.getIdValue(object);
        Criteria criteria = Criteria.whereId(idValue);
        DBObject query = buildQuery(criteria);
        return remove(query, collection);
    }

    /**
     * Removes all documents from the collection that satisfies the given query criteria.
     *
     * @param query          the query that specifies criteria used to remove documents
     * @param collectionName the collection name
     * @return result of operation
     */
    public WriteResult remove(DBObject query, String collectionName) {
        Validate.notNull(query, "remove query cannot be null or empty");
        Validate.notBlank(collectionName, "collectionName cannot be null or empty");
        return mongoDBFactory.getDB().getCollection(collectionName).remove(query);
    }

    /**
     * Removes all documents from the collection that is used to store the instances of documentClass.
     *
     * @param criteria      the query criteria
     * @param documentClass the document class
     * @return result of operation
     */
    public <T> WriteResult remove(Criteria criteria, Class<T> documentClass) {
        DocumentUtils.assertDocument(documentClass);
        DBObject query = buildQuery(criteria);
        return remove(query, DocumentUtils.getCollectionName(documentClass));
    }

    /**
     * Removes all documents from the collection that is used to store the instances of documentClass.
     *
     * @param documentClass the document class
     * @return result of operation
     */
    public <T> WriteResult removeAll(Class<T> documentClass) {
        DocumentUtils.assertDocument(documentClass);
        DBObject query = buildQuery(Criteria.empty());
        return remove(query, DocumentUtils.getCollectionName(documentClass));
    }

    /**
     * Performs query with parameters and returns one document which satisfies the given criteria.
     *
     * @param queryName  the query name
     * @param type       type of document
     * @param <T>        the type of the class modeled by this {@code Class} object.
     * @param parameters query parameters
     * @return found document or null if no documents in collection which satisfy the criteria
     */
    public <T> T queryForObject(String queryName, Class<T> type, Map<String, Object> parameters) {
        return queryExecutor.queryForObject(queryName, type, parameters);
    }

    /**
     * Performs query without parameters and returns one document which satisfies the given criteria.
     *
     * @param queryName the query name
     * @param type      type of object
     * @param <T>       the type of the class modeled by this {@code Class} object.
     * @return found document or null if no documents in collection which satisfy the criteria
     */
    public <T> T queryForObject(String queryName, Class<T> type) {
        return queryExecutor.queryForObject(queryName, type);
    }

    /**
     * Performs query with parameters and returns list of objects of type.
     *
     * @param queryName  the query name
     * @param type       the type of object
     * @param parameters the query parameters
     * @param <T>        the type of the class modeled by this {@code Class} object.
     * @return list of objects, implementation of list is {@link java.util.ArrayList}.
     */
    public <T> List<T> queryForList(String queryName, Class<T> type, Map<String, Object> parameters) {
        return queryExecutor.queryForList(queryName, type, parameters);
    }

    /**
     * Performs query with parameters and returns list of objects of type.
     *
     * @param queryName the query name
     * @param type      the type of document
     * @param <T>       the type of the class modeled by this {@code Class} object.
     * @return list of found documents, implementation of list is {@link java.util.ArrayList}.
     */
    public <T> List<T> queryForList(String queryName, Class<T> type) {
        return queryExecutor.queryForList(queryName, type);
    }

    private DBObject buildQuery(Criteria criteria) {
        return mongoBsonMarshaller.marshall(criteria.query(), criteria.getParameters());
    }

}

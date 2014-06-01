/**
 * Copyright 2012-2013 The Mingo Team
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
package com.mingo.context;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mingo.MingoTemplate;
import com.mingo.benchmark.BenchmarkService;
import com.mingo.config.ContextConfiguration;
import com.mingo.document.id.generator.factory.DefaultIdGeneratorFactory;
import com.mingo.document.id.generator.factory.IdGeneratorFactory;
import com.mingo.exceptions.ContextInitializationException;
import com.mingo.exceptions.ShutdownException;
import com.mingo.executor.MongoQueryExecutor;
import com.mingo.executor.QueryExecutor;
import com.mingo.executor.QueryExecutorBenchmark;
import com.mingo.mapping.convert.ConverterService;
import com.mingo.mongo.MongoDBFactory;
import com.mingo.parser.Parser;
import com.mingo.parser.xml.dom.ParserFactory;
import com.mingo.query.el.ELEngineType;
import com.mingo.query.QueryManager;
import com.mingo.query.el.ELEngine;
import com.mingo.query.el.ELEngineFactory;
import com.mingo.util.FileUtils;
import com.mongodb.Mongo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.mingo.parser.xml.dom.ParserFactory.ParseComponent.CONTEXT;

/**
 * Mingo context includes all components and also combines them together.
 * Context is loaded from xml file that's managed by context.xsd schema.
 */
public class Context {

    private ELEngine elEngine;
    private ConverterService converterService;
    private QueryManager queryManager;

    private ContextConfiguration config;
    private IdGeneratorFactory idGeneratorFactory = new DefaultIdGeneratorFactory();
    private MongoDBFactory mongoDBFactory;
    private QueryExecutor queryExecutor;
    private MingoTemplate mingoTemplate;
    private List<BenchmarkService> benchmarkServices = Lists.newCopyOnWriteArrayList();

    private static final String CONTEXT_PATH_ERROR = "path to context configuration cannot be empty or null";

    private static final Parser<ContextConfiguration> CONTEXT_CONFIGURATION_PARSER =
            ParserFactory.createParser(CONTEXT);

    private static ThreadLocal<Context> currentContext = new InheritableThreadLocal<>();

    /**
     * Loads context from file. Path can be relative(file should be in application classpath) or absolute.
     *
     * @param contextPath the path to the xml file with context configuration
     * @return loaded and configured context
     */
    public static Context create(String contextPath) {
        return create(contextPath, null);
    }

    /**
     * Loads context from file. Path can be relative(file should be in application classpath) or absolute.
     *
     * @param contextPath
     * @param mongo       the {@link Mongo} instance. If mongo configuration was defined in mingo context then will be
     *                    ignored and the given mongo will be used instead. It can be useful if you want to configure
     *                    your own mongo instance and don't use mingo for it.
     * @return loaded and configured context
     */
    public static Context create(String contextPath, Mongo mongo) {
        return create(contextPath, mongo, Collections.emptyList());
    }

    /**
     * Loads context from file. Path can be relative(file should be in application classpath) or absolute.
     *
     * @param contextPath
     * @param mongo             the {@link Mongo} instance. If mongo configuration was defined in mingo context then will be
     *                          ignored and the given mongo will be used instead. It can be useful if you want to configure
     *                          your own mongo instance and don't use mingo for it.
     * @param benchmarkServices the list of {@link com.mingo.benchmark.BenchmarkService}.
     * @return loaded and configured context
     */
    public static Context create(String contextPath, Mongo mongo, List<BenchmarkService> benchmarkServices) {
        Context context = new Context();
        context.initialize(contextPath, mongo);
        currentContext.set(context);
        register(benchmarkServices, context);
        return context;
    }

    private static void register(List<BenchmarkService> services, Context context) {
        if (CollectionUtils.isNotEmpty(services)) {
            services.forEach(service -> {
                service.init(context);
                context.addBenchmarkService(service);
            });
        }
    }

    private void addBenchmarkService(BenchmarkService service) {
        benchmarkServices.add(service);
    }

    /**
     * Return current context instance.
     *
     * @return current context instance or null if this method is being called from another/no-child thread.
     */
    public static Context getCurrent() {
        return currentContext.get();
    }

    /**
     * Gets id generator factory {@link IdGeneratorFactory}.
     * Factory to create generators {@link com.mingo.document.id.generator.IdGenerator}.
     * The factory has the opportunity to register custom generators for specific strategies.
     * Context doesn't have ability to set own implementation of {@link IdGeneratorFactory}.
     *
     * @return id generator factory
     */
    public IdGeneratorFactory getIdGeneratorFactory() {
        return idGeneratorFactory;
    }

    /**
     * Gets current implementation of {@link ELEngine} interface. A EL engine allows evaluate EL expressions.
     * Basically this component is required by another components and isn't of interest as separate component to use.
     *
     * @return EL engine
     */
    public ELEngine getElEngine() {
        return elEngine;
    }

    /**
     * Gets converter service. Converter service is used to find an applicable converter by specific type.
     * Also responsible to create converters.
     *
     * @return converter service
     */
    public ConverterService getConverterService() {
        return converterService;
    }

    /**
     * Gets query manager. Contains all information about queries and provides methods to get them.
     *
     * @return query manager
     */
    public QueryManager getQueryManager() {
        return queryManager;
    }

    /**
     * Gets mongoDB factory.
     * Factory creates and configures {@link com.mongodb.Mongo} instances  based on mongo configuration.
     *
     * @return mongoDB factory
     */
    public MongoDBFactory getMongoDBFactory() {
        return mongoDBFactory;
    }

    /**
     * Gets query executor.
     * Executor is used to perform queries from parsed and loaded query sets.
     *
     * @return query executor
     */
    public QueryExecutor getQueryExecutor() {
        return queryExecutor;
    }

    /**
     * Gets mingo template. Template contains methods to perform CRUD
     * operations and also user queries that are defined in xml files.
     *
     * @return mingo template
     */
    public MingoTemplate getMingoTemplate() {
        return mingoTemplate;
    }

    /**
     * Gets EL engine type.
     *
     * @return EL engine type
     */
    public ELEngineType getELEngineType() {
        return config.getQueryAnalyzerType();
    }

    /**
     * Gets registered benchmark services.
     *
     * @return immutable list
     */
    public List<BenchmarkService> getBenchmarkServices() {
        return ImmutableList.copyOf(benchmarkServices);
    }

    /**
     * Closes context. Notifies all component what context is closing and will be destroyed soon.
     * How long this method will execute depends on time of actions that will be performed be other components:
     * for instance if one of the benchmarks services will save data in slow storage then calling code will wait until
     * the operation is completed, thus try to avoid complicated logic in those methods.
     * @throws ShutdownException if any errors occur
     */
    public void shutdown() throws ShutdownException {
        try {
            queryManager.shutdown();
            if (CollectionUtils.isNotEmpty(benchmarkServices)) {
                benchmarkServices.forEach(BenchmarkService::destroy);
            }
        } catch (RuntimeException e) {
            throw new ShutdownException(e);
        }
    }

    /**
     * Creates and initialize mingo context with all necessary components based on {@link ContextConfiguration}.
     *
     * @param contextPath the path to the xml file with context configuration
     * @param mongo       the mongo instance , can be null
     */
    private void initialize(String contextPath, Mongo mongo) {
        Context context;
        try {
            if (StringUtils.isBlank(contextPath)) {
                throw new IllegalArgumentException(CONTEXT_PATH_ERROR);
            }
            // load context configuration from xml file
            config = loadContextConfiguration(contextPath);
            converterService = new ConverterService(config.getConverterPackageScan(), config.getDefaultConverter());
            queryManager = new QueryManager(config.getQuerySetConfiguration().getQuerySets());
            mongoDBFactory = mongo != null ? new MongoDBFactory(config.getMongoConfig(), mongo)
                    : new MongoDBFactory(config.getMongoConfig());
            elEngine = ELEngineFactory.create(config.getQueryAnalyzerType());
            queryExecutor = new MongoQueryExecutor(mongoDBFactory, queryManager, elEngine, converterService);

            //todo add factory for executors
            if (config.getMingoContextConfig().isBenchmarkEnabled()) {
                queryExecutor = new QueryExecutorBenchmark(queryExecutor);
            }

            mingoTemplate = new MingoTemplate(queryExecutor, mongoDBFactory, converterService, idGeneratorFactory);
        } catch (Throwable e) {
            throw new ContextInitializationException(e);
        }

    }

    /**
     * Parses xml file which managing by schema: context.xsd and creates {@link ContextConfiguration} object.
     */
    private ContextConfiguration loadContextConfiguration(String contextPath) {
        return CONTEXT_CONFIGURATION_PARSER.parse(FileUtils.getAbsolutePath(contextPath));
    }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.tests.integration.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.tests.containers.CassandraContainer;
import org.apache.pulsar.tests.integration.utils.TestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testng.collections.Maps;

/**
 * A tester for testing cassandra sink.
 */
@Slf4j
public class CassandraSinkTester extends SinkTester {

    private static final String ROOTS = "cassandra";
    private static final String KEY = "key";
    private static final String COLUMN = "col";

    private final String keySpace;
    private final String tableName;

    private CassandraContainer cassandraCluster;

    private Cluster cluster;
    private Session session;

    public CassandraSinkTester() {
        super("cassandra");

        String suffix = TestUtils.randomName(8) + "_" + System.currentTimeMillis();
        this.keySpace = "keySpace_" + suffix;
        this.tableName = "tableName_" + suffix;

        sinkConfig.put("roots", ROOTS);
        sinkConfig.put("keyspace", keySpace);
        sinkConfig.put("columnFamily", tableName);
        sinkConfig.put("keyname", KEY);
        sinkConfig.put("columnName", COLUMN);
    }

    @Override
    protected Map<String, GenericContainer<?>> newSinkService(String clusterName) {
        this.cassandraCluster = new CassandraContainer(clusterName);
        Map<String, GenericContainer<?>> containers = Maps.newHashMap();
        containers.put(CassandraContainer.NAME, cassandraCluster);
        return containers;
    }

    @Override
    protected void prepareSink() {
        // build the sink
        cluster = Cluster.builder()
            .addContactPoint("localhost")
            .withPort(cassandraCluster.getCassandraPort())
            .build();

        // connect to the cluster
        session = cluster.connect();
        log.info("Connecting to cassandra cluster at localhost:{}", cassandraCluster.getCassandraPort());

        String createKeySpace =
            "CREATE KEYSPACE " + keySpace
                + " WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}; ";
        log.info(createKeySpace);
        session.execute(createKeySpace);
        session.execute("USE " + keySpace);

        String createTable = "CREATE TABLE " + tableName
            + "(" + KEY + " text PRIMARY KEY, "
            + COLUMN + " text);";
        log.info(createTable);
        session.execute(createTable);
    }

    @Override
    protected void validateSinkResult(Map<String, String> kvs) {
        String query = "SELECT * FROM " + tableName + ";";
        ResultSet result = session.execute(query);
        List<Row> rows = result.all();
        assertEquals(kvs.size(), rows.size());
        for (Row row : rows) {
            String key = row.getString(KEY);
            String value = row.getString(COLUMN);

            String expectedValue = kvs.get(key);
            assertNotNull(expectedValue);
            assertEquals(expectedValue, value);
        }
    }
}

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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.github.dockerjava.api.command.CreateContainerCmd;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.impl.schema.AvroSchema;
import org.apache.pulsar.tests.integration.topologies.PulsarCluster;
import org.testcontainers.containers.MySQLContainer;

/**
 * A tester for testing jdbc sink.
 * This will use MySql as DB server
 */
@Slf4j
public class JdbcSinkTester extends SinkTester<MySQLContainer> {

    /**
     * A Simple class to test jdbc class，
     *
     */
    @Data
    @ToString
    @EqualsAndHashCode
    public static class Foo {
        private String field1;
        private String field2;
        private int field3;
    }

    private static final String NAME = "jdbc";
    private static final String MYSQL = "mysql";

    private AvroSchema<Foo> schema = AvroSchema.of(Foo.class);
    private String tableName = "test";
    private Connection connection;

    public JdbcSinkTester() {
        super(NAME, SinkType.JDBC);

        // container default value is test
        sinkConfig.put("userName", "test");
        sinkConfig.put("password", "test");
        sinkConfig.put("tableName", tableName);
        sinkConfig.put("batchSize", 1);
    }

    @Override
    public Schema<?> getInputTopicSchema() {
        return schema;
    }

    @Override
    protected MySQLContainer createSinkService(PulsarCluster cluster) {
        return (MySQLContainer) new MySQLContainer()
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("test")
            .withNetworkAliases(MYSQL)
            .withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
                @Override
                public void accept(CreateContainerCmd createContainerCmd) {
                    createContainerCmd
                        .withName(MYSQL)
                        .withHostName(cluster.getClusterName() + "-" + MYSQL);
                }
            });
    }

    @Override
    public void prepareSink() throws Exception {
        String jdbcUrl = serviceContainer.getJdbcUrl();
        // we need set mysql server address in cluster network.
        sinkConfig.put("jdbcUrl", "jdbc:mysql://" + MYSQL + ":3306/test");
        String driver = serviceContainer.getDriverClassName();
        Class.forName(driver);

        connection = DriverManager.getConnection(jdbcUrl, "test", "test");
        log.info("getConnection: {}, jdbcurl: {}", connection, jdbcUrl);

        // create table
        String createTable = "CREATE TABLE " + tableName +
            " (field1 TEXT, field2 TEXT, field3 INTEGER, PRIMARY KEY (field3))";
        int ret = connection.createStatement().executeUpdate(createTable);
        log.info("created table in jdbc: {}, return value: {}", createTable, ret);
    }

    @Override
    public void validateSinkResult(Map<String, String> kvs) {
        log.info("Query table content from mysql server: {}", tableName);
        String querySql = "SELECT * FROM " + tableName;
        ResultSet rs;
        try {
            // backend flush may not complete.
            Thread.sleep(1000);

            PreparedStatement statement = connection.prepareStatement(querySql);
            rs = statement.executeQuery();

            while (rs.next()) {
                String field1 = rs.getString(1);
                String field2 = rs.getString(2);
                int field3 = rs.getInt(3);

                String value = kvs.get("key-" + field3);

                Foo obj = schema.decode(value.getBytes());
                assertEquals(obj.field1, field1);
                assertEquals(obj.field2, field2);
                assertEquals(obj.field3, field3);
            }
        } catch (Exception e) {
            log.error("Got exception: ", e);
            fail("Got exception when op sql.");
            return;
        }
    }
}

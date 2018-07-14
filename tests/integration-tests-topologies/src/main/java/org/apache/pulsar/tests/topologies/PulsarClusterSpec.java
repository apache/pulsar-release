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
package org.apache.pulsar.tests.topologies;

import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.pulsar.tests.containers.ChaosContainer;
import org.testcontainers.containers.GenericContainer;
import org.testng.collections.Maps;

/**
 * Spec to build a pulsar cluster.
 */
@Builder
@Accessors(fluent = true)
@Getter
@Setter
public class PulsarClusterSpec {

    /**
     * Returns the cluster name.
     *
     * @return the cluster name.
     */
    String clusterName;

    /**
     * Returns number of bookies.
     *
     * @return number of bookies.
     */
    @Default
    int numBookies = 3;

    /**
     * Returns number of brokers.
     *
     * @return number of brokers.
     */
    @Default
    int numBrokers = 2;

    /**
     * Returns number of proxies.
     *
     * @return number of proxies.
     */
    @Default
    int numProxies = 1;

    /**
     * Returns number of function workers.
     *
     * @return number of function workers.
     */
    @Default
    int numFunctionWorkers = 0;

    /**
     * Returns the function runtime type.
     *
     * @return the function runtime type.
     */
    @Default
    FunctionRuntimeType functionRuntimeType = FunctionRuntimeType.PROCESS;

    /**
     * Returns the list of external services to start with
     * this cluster.
     *
     * @return the list of external services to start with the cluster.
     */
    Map<String, GenericContainer<?>> externalServices = Maps.newHashMap();

    /**
     * Returns the flag whether to enable/disable container log.
     *
     * @return the flag whether to enable/disable container log.
     */
    boolean enableContainerLog = false;

}

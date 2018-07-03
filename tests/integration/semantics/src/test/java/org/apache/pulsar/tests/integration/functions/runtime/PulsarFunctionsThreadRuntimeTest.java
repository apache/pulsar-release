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
package org.apache.pulsar.tests.integration.functions.runtime;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.tests.topologies.PulsarClusterTestBase;
import org.testcontainers.containers.Container.ExecResult;
import org.testng.annotations.BeforeClass;

/**
 * Run the runtime test cases in thread mode.
 */
@Slf4j
public class PulsarFunctionsThreadRuntimeTest extends PulsarFunctionsRuntimeTest {

    @BeforeClass
    public static void setupCluster() throws Exception {
        PulsarClusterTestBase.setupCluster();

        pulsarCluster.startFunctionWorkersWithThreadContainerFactory(1);

        ExecResult result = pulsarCluster.getAnyWorker().execCmd("cat", "/pulsar/conf/functions_worker.yml");
        log.info("Functions Worker Config : \n{}", result.getStdout());
    }

}

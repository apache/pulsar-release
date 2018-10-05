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
package org.apache.pulsar.tests.integration.containers;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.tests.integration.utils.DockerUtils;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

/**
 * Abstract Test Container for Pulsar.
 */
@Slf4j
public abstract class PulsarContainer<SelfT extends PulsarContainer<SelfT>> extends ChaosContainer<SelfT> {

    public static final int INVALID_PORT = -1;
    public static final int ZK_PORT = 2181;
    public static final int CS_PORT = 2184;
    public static final int BOOKIE_PORT = 3181;
    public static final int BROKER_PORT = 6650;
    public static final int BROKER_HTTP_PORT = 8080;

    private static final String IMAGE_NAME = "apachepulsar/pulsar-test-latest-version:latest";

    private final String hostname;
    private final String serviceName;
    private final String serviceEntryPoint;
    private final int servicePort;
    private final int httpPort;
    private final String httpPath;

    public PulsarContainer(String clusterName,
                           String hostname,
                           String serviceName,
                           String serviceEntryPoint,
                           int servicePort,
                           int httpPort) {
        this(clusterName, hostname, serviceName, serviceEntryPoint, servicePort, httpPort, "/metrics");
    }

    public PulsarContainer(String clusterName,
                           String hostname,
                           String serviceName,
                           String serviceEntryPoint,
                           int servicePort,
                           int httpPort,
                           String httpPath) {
        super(clusterName, IMAGE_NAME);
        this.hostname = hostname;
        this.serviceName = serviceName;
        this.serviceEntryPoint = serviceEntryPoint;
        this.servicePort = servicePort;
        this.httpPort = httpPort;
        this.httpPath = httpPath;
    }

    @Override
    protected void beforeStop() {
        super.beforeStop();
        if (null != containerId) {
            DockerUtils.dumpContainerDirToTargetCompressed(
                getDockerClient(),
                containerId,
                "/var/log/pulsar"
            );
        }
    }

    @Override
    public String getContainerName() {
        return clusterName + "-" + hostname;
    }

    @Override
    protected void configure() {
        if (httpPort > 0) {
            addExposedPorts(httpPort);
        }
        if (servicePort > 0) {
            addExposedPort(servicePort);
        }
    }

    protected void beforeStart() {}

    @Override
    public void start() {
        if (httpPort > 0 && servicePort < 0) {
            this.waitStrategy = new HttpWaitStrategy()
                .forPort(httpPort)
                .forStatusCode(200)
                .forPath(httpPath)
                .withStartupTimeout(Duration.of(300, SECONDS));
        } else if (httpPort > 0 || servicePort > 0) {
            this.waitStrategy = new HostPortWaitStrategy()
                .withStartupTimeout(Duration.of(300, SECONDS));
        }
        this.withCreateContainerCmdModifier(createContainerCmd -> {
            createContainerCmd.withHostName(hostname);
            createContainerCmd.withName(getContainerName());
            createContainerCmd.withEntrypoint(serviceEntryPoint);
        });

        beforeStart();
        super.start();
        log.info("Start pulsar service {} at container {}", serviceName, containerName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PulsarContainer)) {
            return false;
        }

        PulsarContainer another = (PulsarContainer) o;
        return containerName.equals(another.containerName)
            && super.equals(another);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(
            containerName);
    }
}

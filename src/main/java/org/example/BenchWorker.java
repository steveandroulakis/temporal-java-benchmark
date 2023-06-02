/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.example;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.serviceclient.SimpleSslContextBuilder;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;


import io.temporal.client.WorkflowClient;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class BenchWorker {
    // Define the task queue name
    static final String TASK_QUEUE = "BenchTaskQueue";

    public static void main(String[] args) {

        String targetEndpoint = System.getenv("TEMPORAL_HOST_URL");
        // if TEMPORAL_HOST_URL is not set then use 127.0.0.1:7233
        if (targetEndpoint == null || targetEndpoint.isEmpty()) {
            targetEndpoint = "127.0.0.1:7233";
        }

        WorkflowServiceStubsOptions.Builder stubsOptions = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(targetEndpoint);

        // If TEMPORAL_PROMETHEUS_ENABLED is set then enable prometheus metrics
        if (System.getenv("TEMPORAL_PROMETHEUS_ENABLED") != null && System.getenv("TEMPORAL_PROMETHEUS_ENABLED").equals("true")) {
            // print 'PROMETHEUS ENABLED'
            System.out.println("PROMETHEUS ENABLED");

            // Set up prometheus registry and stats reported
            PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            // Set up a new scope, report every 1 second
            Scope scope =
                    new RootScopeBuilder()
                            .reporter(new MicrometerClientStatsReporter(registry))
                            .reportEvery(com.uber.m3.util.Duration.ofSeconds(1));
            // Start the prometheus scrape endpoint
            HttpServer scrapeEndpoint = MetricsUtils.startPrometheusScrapeEndpoint(registry, 9090);
            // Stopping the worker will stop the http server that exposes the
            // scrape endpoint.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> scrapeEndpoint.stop(1)));

            stubsOptions.setMetricsScope(scope);
        }


        WorkflowServiceStubs service = null;
        if (System.getenv("TEMPORAL_MTLS_TLS_KEY") == null || System.getenv("TEMPORAL_MTLS_TLS_KEY").isEmpty()) {
            service =
                    WorkflowServiceStubs.newServiceStubs(stubsOptions.build());
        } else {
            try {
                InputStream clientCert = new FileInputStream(System.getenv("TEMPORAL_MTLS_TLS_CERT"));
                InputStream clientKey = new FileInputStream(System.getenv("TEMPORAL_MTLS_TLS_KEY"));

                service =
                        WorkflowServiceStubs.newServiceStubs(stubsOptions.
                                setSslContext(
                                        SimpleSslContextBuilder.forPKCS8(clientCert, clientKey)
                                                .build())
                                .build());

            } catch (IOException e) {
                System.err.println("Error loading certificates: " + e.getMessage());
            }
        }

        String targetNamespace = System.getenv("TEMPORAL_NAMESPACE");
        if (targetNamespace == null || targetNamespace.isEmpty()) {
            targetNamespace = "default";
        }

        WorkflowClient client = WorkflowClient.newInstance(service,
                WorkflowClientOptions.newBuilder().setNamespace(targetNamespace).build());

        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(BenchWorkflow.ComposerWorkflowImpl.class);
        worker.registerActivitiesImplementations(new BenchWorkflow.SampleActivityImpl());

        factory.start();
    }
}
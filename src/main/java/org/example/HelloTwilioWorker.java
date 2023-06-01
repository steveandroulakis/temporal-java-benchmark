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

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

public class HelloTwilioWorker {
    // Define the task queue name
    static final String TASK_QUEUE = "HelloTwilioTaskQueue";

    // get logger

    public static void main(String[] args) {

        WorkflowServiceStubs service = null;
        if (System.getenv("TEMPORAL_MTLS_TLS_KEY") == null || System.getenv("TEMPORAL_MTLS_TLS_KEY").isEmpty()) {
            service = WorkflowServiceStubs.newLocalServiceStubs();
        } else {
            try {
                InputStream clientCert = new FileInputStream(System.getenv("TEMPORAL_MTLS_TLS_CERT"));
                // PKCS8 client key, which should look like:
                // -----BEGIN PRIVATE KEY-----
                // ...
                // -----END PRIVATE KEY-----
                InputStream clientKey = new FileInputStream(System.getenv("TEMPORAL_MTLS_TLS_KEY"));
                // For temporal cloud this would likely be ${namespace}.tmprl.cloud:7233
                String targetEndpoint = System.getenv("TEMPORAL_HOST_URL");
                // Create SSL enabled client by passing SslContext, created by SimpleSslContextBuilder.
                service =
                    WorkflowServiceStubs.newServiceStubs(
                        WorkflowServiceStubsOptions.newBuilder()
                            .setSslContext(SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build())
                            .setTarget(targetEndpoint)
                            .build()); 
            
            } catch (IOException e) {
                System.err.println("Error loading certificates: " + e.getMessage());
            }  
        }

        /*
         * Get a Workflow service client which can be used to start, Signal, and Query
         * Workflow Executions.
         */
        WorkflowClient client = WorkflowClient.newInstance(service);

        /*
         * Define the workflow factory. It is used to create workflow workers for a
         * specific task queue.
         */
        WorkerFactory factory = WorkerFactory.newInstance(client);

        /*
         * Define the workflow worker. Workflow workers listen to a defined task queue
         * and process
         * workflows and activities.
         */
        Worker worker = factory.newWorker(TASK_QUEUE);

        /*
         * Register our workflow implementation with the worker.
         * Workflow implementations must be known to the worker at runtime in
         * order to dispatch workflow tasks.
         */
        worker.registerWorkflowImplementationTypes(HelloTwilio.ComposerWorkflowImpl.class);

        /**
         * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
         * the Activity Type is a shared instance.
         */
        worker.registerActivitiesImplementations(new HelloTwilio.SampleActivityImpl());

        /*
         * Start all the workers registered for a specific task queue.
         * The started workers then start polling for workflows and activities.
         */
        // log some debug info
        factory.start();
    }
}
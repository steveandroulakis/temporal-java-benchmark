package org.example;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloTwilio {
    // Define the task queue name
    static final String TASK_QUEUE = "HelloTwilioTaskQueue";
    ;

    private static final Logger log = LoggerFactory.getLogger(SampleActivityImpl.class);
    private static final Logger logWorkflow = LoggerFactory.getLogger(HelloTwilioWorker.class);

    @WorkflowInterface
    public interface ComposerWorkflow {
        @WorkflowMethod
        void start();
    }

    @ActivityInterface
    public interface SampleActivity {
        @ActivityMethod(name = "sampleActivity")
        Integer execute(final Integer input);
    }

    public static class ComposerWorkflowImpl implements ComposerWorkflow {
        private static final int MAX_STEPS = 500;
        private final SampleActivity sampleActivity;
        private int step;

        public ComposerWorkflowImpl() {

            this.sampleActivity =
                    Workflow.newActivityStub(
                            SampleActivity.class,
                            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
            this.step = 0;
        }

        @Override
        public void start() {
            logWorkflow.info("Starting workflow for task queue: {}", TASK_QUEUE);
            int sum = 0;

            while (true) {
                // This is temp for now - we will do a better job at determining this later
                this.step++;
                if (this.step > MAX_STEPS) {
                    // log.warn("Reached a maximum of {} steps, terminating", MAX_STEPS);
                    return;
                }

                // log.info("Running loop, sum is {}", sum);
                sum = sampleActivity.execute(sum);
                Workflow.sleep(25);
            }
        }
    }

    static class SampleActivityImpl implements SampleActivity {
        @Override
        public Integer execute(final Integer input) {
            log.info("Executing http call " + input);

            return input + 1;
        }
    }

    public static void main(String[] args) {
        int numWorkflows = -1;

        // Get a Workflow service stub.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        // Number of workflows to run in parallel.
        String numWorkflowsString = System.getProperty("numWorkflows");
        if (numWorkflowsString != null) {
            numWorkflows = Integer.parseInt(numWorkflowsString);
            // Now you can use numWorkflows in your code...
        } else {
            // Handle the case where the numWorkflows property was not set...
            // log an error with an example of how to set the property
            // ./gradlew runWorkflows -PnumWorkflows=10
            log.error("Please set the numWorkflows property. For example: ./gradlew runWorkflows -PnumWorkflows=10");
            System.exit(1);
        };

        // Create an ExecutorService with a fixed thread pool.
        ExecutorService executorService = Executors.newFixedThreadPool(numWorkflows);

        // Create a list to hold the Future objects.
        List<Future<?>> futures = new ArrayList<>();

        // Get a Workflow service client.
                WorkflowClient client = WorkflowClient.newInstance(service);

        // Start NUM_WORKFLOWS workflows in parallel.
        for (int i = 0; i < numWorkflows; i++) {
            // Each task is a new Runnable.
            Runnable task = () -> {
                // Define unique workflow id.
                String WORKFLOW_ID = "HelloTwilioWorkflow-" + UUID.randomUUID();

                // Create the workflow client stub.
                ComposerWorkflow workflow = client.newWorkflowStub(
                        ComposerWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

                // Start the workflow.
                workflow.start();
            };

            // Submit the task to the ExecutorService.
            futures.add(executorService.submit(task));
        }

        // Now, wait for all the futures to complete.
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();


        System.exit(0);
    }
}
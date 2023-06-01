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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloTwilio {
    // Define the task queue name
    static final String TASK_QUEUE = "HelloTwilioTaskQueue";

    // Define our workflow unique id
    static final String WORKFLOW_ID = "HelloTwilioWorkflow-" + UUID.randomUUID();
    ;

    private static final Logger log = LoggerFactory.getLogger(SampleActivityImpl.class);

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

        // Get a Workflow service stub.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        /*
         * Get a Workflow service client which can be used to start, Signal, and Query
         * Workflow Executions.
         */
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Create the workflow client stub. It is used to start our workflow execution.
        ComposerWorkflow workflow =
                client.newWorkflowStub(
                        ComposerWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        workflow.start();

        System.exit(0);
    }
}
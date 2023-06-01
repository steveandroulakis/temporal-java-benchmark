package org.example;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.UUID;
//
//// @@@SNIPSTART money-transfer-project-template-java-workflow-initiator
//public class InitiateMoneyTransfer {
//
//    public static void main(String[] args) throws Exception {
//
//        // WorkflowServiceStubs is a gRPC stubs wrapper that talks to the local Docker instance of the Temporal server.
//        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
//        WorkflowOptions options = WorkflowOptions.newBuilder()
//                .setTaskQueue(moneytransferapp.Shared.MONEY_TRANSFER_TASK_QUEUE)
//                // A WorkflowId prevents this it from having duplicate instances, remove it to duplicate.
//                .setWorkflowId("money-transfer-workflow")
//                .build();
//        // WorkflowClient can be used to start, signal, query, cancel, and terminate Workflows.
//        WorkflowClient client = WorkflowClient.newInstance(service);
//        // WorkflowStubs enable calls to methods as if the Workflow object is local, but actually perform an RPC.
//        moneytransferapp.MoneyTransferWorkflow workflow = client.newWorkflowStub(moneytransferapp.MoneyTransferWorkflow.class, options);
//        String referenceId = UUID.randomUUID().toString();
//        String fromAccount = "001-001";
//        String toAccount = "002-002";
//        String toAccountFail = "accountId_FAIL_WITHDRAW";
//        double amount = 18.74;
////        // Asynchronous execution. This process will exit after making this call.
////        WorkflowExecution we = WorkflowClient.start(workflow::transfer, fromAccount, toAccount, referenceId, amount);
//
//        // Uncomment this workflow execution to force a withdrawal failure (and comment out the one above)
//        // Asynchronous execution. This process will exit after making this call.
//        WorkflowExecution we = WorkflowClient.start(workflow::transfer, fromAccount, toAccountFail, referenceId, amount);
//
//        System.out.printf("\nTransfer of $%f from account %s to account %s is processing\n", amount, fromAccount, toAccount);
//        System.out.printf("\nWorkflowID: %s RunID: %s", we.getWorkflowId(), we.getRunId());
//        System.exit(0);
//    }
//}
//// @@@SNIPEND

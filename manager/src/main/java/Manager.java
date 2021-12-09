import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.*;
import java.util.LinkedList;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


@SuppressWarnings("InfiniteLoopStatement")
public class Manager {
    private static final String managerInputQueueName = "manager-input-queue";
    private static final AwsLib lib = AwsLib.getInstance();

    private final static String workersInQueueName = "workers-in-queue";
    private final static String workersOutQueueName = "workers-out-queue";

    private static final String workersInQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersInQueueName);
    private static final String workersOutQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersOutQueueName);

//    private static final String summeryFolder = ".\\summeryFiles";
//    private static final String inputFolder = ".\\inputFiles";

    private static final AwsBundle awsBundle = AwsBundle.getInstance();


    public static void main(String[] args) {
//        new File(summeryFolder).mkdirs();
//        new File(inputFolder).mkdirs();
        run();
    }

    /*
        //If the message is a termination message, then the manager:
        //▪ Does not accept any more input files from local applications.
        //▪ Waits for all the workers to finish their job, and then terminates them
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private static void run() {


        String mangerQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(managerInputQueueName);
        ExecutorService executor = Executors.newFixedThreadPool(5);//creating a pool of 5 threads

        while (true) {
            Message msg = lib.sqsGetMessageFromQueue(mangerQueueUrl);

            if (msg.body().startsWith("terminate")) {
                lib.sqsSendMessage(workersInQueueUrl, "terminate");
                System.out.println("waiting for workers to die before me");
                while (awsBundle.checkInstanceCount() > 1) ;
                System.out.println("workers died");
                lib.deleteAllQueues();
                //todo delete the right buckets
                lib.s3DeleteBucket("");
                awsBundle.terminateCurrentInstance();
                System.exit(1);
            }
            Runnable worker = new managerTask(msg);
            executor.execute(worker);//calling execute method of ExecutorService
            System.out.printf("accepted message { %s }\n", msg.body());
        }

    }
}








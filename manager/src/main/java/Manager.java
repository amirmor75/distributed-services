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
    private static final String managerInputQueueName= "manager-input-queue";
    private static final AwsLib lib = AwsLib.getInstance();
    private final static String workersQueueName = "workers-queue";

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
        String workersQueueUrl = lib.sqsCreateAndGetQueueUrlFromName( workersQueueName);
        ExecutorService executor = Executors.newFixedThreadPool(5);//creating a pool of 5 threads

        while (true) {
            List<Message> list_m = lib.sqsGetMessagesFromQueue(mangerQueueUrl);
            if (list_m==null || list_m.size()<=0)
                continue;
            for (Message m : list_m) {
                if (m.body().equals("terminate")) {
                    while (lib.sqsGetMessagesFromQueue(workersQueueUrl).size()!=0);
                    lib.sqsSendMessage(workersQueueUrl,"terminate");
                    // todo check all tasks finshed and workers dead before you die
                    // todo delete sqs and s3 stuff if nedded
                    while(awsBundle.checkInstanceCount()>1);
                    lib.deleteAllQueues();
                    awsBundle.terminateCurrentInstance();
                }
            }
            for (Message m : list_m) {
                Runnable worker = new managerTask( m );
                executor.execute(worker);//calling execute method of ExecutorService
                System.out.printf("accepted message { %s }\n",m.body());
            }
        }

    }



    }

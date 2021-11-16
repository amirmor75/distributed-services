package manager;

import lib.AwsLib;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.List;

public class Manager {

    private static final String QUEUE_NAME= "manager_special_sqs";
    public void main(String[] args) {
        run();
    }
    public void run() {
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
        String queueUrl = AwsLib.CreateAndGetQueueUrlFromName(sqs,QUEUE_NAME);
        List<Message> messages = AwsLib.getMessagesFromQueue(sqs, queueUrl);


        // download the input file from S3



        // Creates an SQS message for each URL in the input file together with the operation
        //that should be performed on it.




        //Checks the SQS message count and starts Worker processes (nodes) accordingly.


        //The manager should create a worker for every n messages, if there are no
        //running workers.


        //If the message is a termination message, then the manager:
        //▪ Does not accept any more input files from local applications.
        //▪ Waits for all the workers to finish their job, and then terminates them
    }

}

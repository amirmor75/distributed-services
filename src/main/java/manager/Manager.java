package manager;

import lib.AwsLib;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

import static lib.AwsLib.sqsSendMessages;


public class Manager {

    private static int n ;
    public static void main(String[] args) {
        n = Integer.parseInt(args[2]);
        new Manager().run();
    }

    /*
        //If the message is a termination message, then the manager:
        //▪ Does not accept any more input files from local applications.
        //▪ Waits for all the workers to finish their job, and then terminates them
     */
    public void run() {
        Region region = Region.US_EAST_1;
        String mangerQueueName = "manager_special_sqs";
        String workersQueueName = "workers_queue";
        String bucketName = "localApplication_special_s3Bucket";
        String KEY = "key";
        SqsClient sqs = SqsClient.builder().region(region).build();
        S3Client s3 = S3Client.builder().region(region).build();
        // download the input file from S3
        AwsLib.downloadS3Bucket();
        File inputFile = new File("./input");
        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder().region(region).build();
        s3.getObject(GetObjectRequest.builder().bucket(bucketName).key(KEY).build(),
                ResponseTransformer.toFile(inputFile));

        // Creates an SQS message for each URL in the input file together with the operation
        //that should be performed on it.
        String workersQueueUrl = AwsLib.sqsCreateAndGetQueueUrlFromName(sqs, workersQueueName);
        List<String> messageBodies = parseInputFile("input");
        sqsSendMessages(sqs,workersQueueUrl,messageBodies);

        //Checks the SQS message count and starts Worker processes (nodes) accordingly.
        //The manager should create a worker for every n messages, if there are no
        //running workers.

        int workersPoolSize= messageBodies.size()/n;
        for (int i = 0; i < workersPoolSize; i++) {
            createEc2Instance("myEc2Instance1982754928173"+i);// complex so name is unique
            // make the instances do work now somehow

        }

        //Manager reads all Workers' messages from SQS and creates one summary file, once all URLs
        //in the input file have been processed.
        List<Message> managerQueueMessages = AwsLib.sqsGetMessagesFromQueue(sqs,workersQueueUrl);
        while (managerQueueMessages.size() < messageBodies.size()){
            managerQueueMessages.addAll(AwsLib.sqsGetMessagesFromQueue(sqs,workersQueueUrl));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored){
            }
        }



        // Manager uploads the summary file to S3.
        AwsLib.createAndUploadS3Bucket();


        // Manager posts an SQS message about the summary file

    }

    private List<String> parseInputFile(String fileName) {

            List<String> listOperationUrl = new LinkedList<>();
        try{
            BufferedReader readBuffer = new BufferedReader(new FileReader(fileName));
            String strRead;

            while ((strRead=readBuffer.readLine())!=null)
                listOperationUrl.add(strRead);
        }catch (Exception e){
            //TODO
        }

        return listOperationUrl;
    }


    public static void createEc2Instance(String instanceName){

    }

}

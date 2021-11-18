package manager;

import lib.AwsLib;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.InstanceStateChange;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
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
        String key = "key";
        SqsClient sqs = SqsClient.builder().region(region).build();
        S3Client s3 = S3Client.builder().region(region).build();
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();
        String mangerQueueUrl = AwsLib.sqsCreateAndGetQueueUrlFromName(sqs, mangerQueueName);
        String workersQueueUrl = AwsLib.sqsCreateAndGetQueueUrlFromName(sqs, workersQueueName);


        // download the input file from S3
        File inputFile = AwsLib.downloadS3File(s3,bucketName,"input");

        // Creates an SQS message for each URL in the input file together with the operation
        //that should be performed on it.
        List<String> messageBodies = parseInputFile("input");
        sqsSendMessages(sqs,workersQueueUrl,messageBodies);

        //Checks the SQS message count and starts Worker processes (nodes) accordingly.
        //The manager should create a worker for every n messages, if there are no
        //running workers.

        int workersPoolSize = messageBodies.size()/n == 0 ? 1 : messageBodies.size()/n;
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
        //now have read all workers results
        File summery = createSummeryFile(managerQueueMessages);
        //kill all instances
        terminateEC2Instances(ec2,new LinkedList<>());// what to do with ids???????????

        // Manager uploads the summary file to S3.
        AwsLib.createAndUploadS3Bucket(s3,bucketName,key,summery);

        // Manager posts an SQS message about the summary file
        AwsLib.sqsSendMessage(sqs,mangerQueueUrl,"resultReady");
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

    //todo
    private void createEc2Instance(String instanceName){

    }
    private void terminateEC2Instance( Ec2Client ec2, String instanceID) {

        try{
            TerminateInstancesRequest ti = TerminateInstancesRequest.builder()
                    .instanceIds(instanceID)
                    .build();

            TerminateInstancesResponse response = ec2.terminateInstances(ti);
            List<InstanceStateChange> list = response.terminatingInstances();

            for (int i = 0; i < list.size(); i++) {
                InstanceStateChange sc = (list.get(i));
                System.out.println("The ID of the terminated instance is "+sc.instanceId());
            }
        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
    private void terminateEC2Instances(Ec2Client ec2,List<String> ids){
        for (String id : ids){
            terminateEC2Instance(ec2,id);
        }
    }


    //todo
    private File createSummeryFile(List<Message> messages){
        return null;
    }

}

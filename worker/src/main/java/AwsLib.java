import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;


import java.io.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

public class AwsLib {
    private static final AwsLib instance = new AwsLib();

    private final Ec2Client ec2 = Ec2Client.create();
    private  final SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
    private final S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();



    public static   AwsLib getInstance() {
        return instance;
    }
    public  void sqsSendMessages( String queueUrl,List<String> messages) {
        for (String body : messages) {
            SendMessageRequest send_msg_request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .delaySeconds(2)
                    .build();


            try {
                sqs.sendMessage(send_msg_request);
            } catch (Exception e) {
                System.out.println("error deleting msg:" +body + "from queue " + queueUrl + e.getMessage());
            }
        }
    }
    public void sqsSendMessage( String queueUrl,String body){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .delaySeconds(5)
                .build();
        try {
            sqs.sendMessage(send_msg_request);
        } catch (Exception e) {
            System.out.println("error deleting msg:" +body + "from queue " + queueUrl + e.getMessage());
        }
    }
    public  void sqsDeleteMessage( String queueUrl,Message m){

        try{
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(m.receiptHandle())
                    .build();
            sqs.deleteMessage(deleteRequest);
        }
        catch(Exception e){
            System.out.println("error deleting msg:"+ m.body()+"from queue "+queueUrl + e.getMessage());
        }

    }
    public void sqsDeleteQueue(String queueUrl){
        try{
            sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        }
        catch(Exception e){
            System.out.println("error queueUrl"+ queueUrl + e.getMessage());
        }

    }
    public String sqsCreateAndGetQueueUrlFromName ( String queueName) {

        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            CreateQueueResponse create_result = sqs.createQueue(request);
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();
            return sqs.getQueueUrl(getQueueRequest).queueUrl();
        } catch (Exception e) {
            System.out.println("error create queue "+queueName+e.getMessage());
            System.exit(1);
        }
        return null;
    }
    public List<Message> sqsGetMessagesFromQueue( String queueUrl) {
        try{
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .waitTimeSeconds(5)
                    .build();
            return sqs.receiveMessage(receiveRequest).messages();
        }catch (QueueDoesNotExistException e){
            System.out.println("sqsGetMessagesFromQueue "+queueUrl+e.getMessage());
            AwsBundle.getInstance().terminateCurrentInstance();
            System.exit(1);
        }catch (Exception e){
        System.out.println("sqsGetMessagesFromQueue "+queueUrl+e.getMessage());
        return null;
    }
        return null;
    }
    public void createAndUploadS3Bucket( String bucketName,String key, File file){
        try{
            software.amazon.awssdk.regions.Region region = Region.US_EAST_1;
            s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            // Put Object
            s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key)
                            .build(),
                    RequestBody.fromFile(file));
            return;
        }
        catch (NoSuchBucketException e){
            System.out.println("bucket created");
        }catch (Exception e){
            System.out.println("createAndUploadS3Bucket bucket:"+ bucketName +"key "+ key +"file"+file.getName()+ e.getMessage());
        }
        try{
            s3.createBucket(CreateBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .build())
                    .build());

            s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key)
                            .build(),
                    RequestBody.fromFile(file));
        }catch (Exception e){
            System.out.println("createAndUploadS3Bucket bucket:"+ bucketName +"key "+ key +"file"+file.getName()+ e.getMessage());
        }

    }
    public void createEC2KeyPair( String keyName) {

        try {
            CreateKeyPairRequest request = CreateKeyPairRequest.builder()
                    .keyName(keyName).build();

            ec2.createKeyPair(request);
            System.out.printf(
                    "Successfully created key pair named %s\n",
                    keyName);

        } catch (Ec2Exception ignored) {}
    }
    public void changeVisibility(Message msg,String queue_url,int timeOut){
        String receipt = msg.receiptHandle();
        sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                .visibilityTimeout(timeOut).
                queueUrl(queue_url).receiptHandle(receipt).build());
    }
    public void deleteAllQueues() {
        for (String qUrl : sqs.listQueues().queueUrls())
            sqsDeleteQueue(qUrl);
    }

}

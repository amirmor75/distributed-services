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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsLib {
    private static final AwsLib instance = new AwsLib();

    private  final SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
    private final S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
    public static   AwsLib getInstance() {
        return instance;
    }




    //sqs
    public String sqsCreateAndGetQueueUrlFromName ( String queueName) {

        try {
            Map<QueueAttributeName, String> attributes = new HashMap<QueueAttributeName, String>();
            attributes.put(QueueAttributeName.FIFO_QUEUE, "true");
            attributes.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true");
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .attributes(attributes)
                    .build();
            CreateQueueResponse create_result = sqs.createQueue(request);
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();

            return sqs.getQueueUrl(getQueueRequest).queueUrl();
        } catch (Exception e) {
            System.out.println("error create queue "+queueName+" "+e.getMessage());
            System.exit(1);
        }
        return null;
    }

    public void sqsSendMessage( String queueUrl,String body){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageGroupId("generic")//required for fifo
                .build();
        try {
            sqs.sendMessage(send_msg_request);
        } catch (Exception e) {
            System.out.println("error sending msg:" +body + "from queue " + queueUrl + e.getMessage());
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
            System.out.println("error deleting msg:"+ m.body()+"from queue "+queueUrl +" " + e.getMessage());
        }
    }

    public Message sqsGetMessageFromQueue( String queueUrl) {
        try{
            List<Message> msgl = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .waitTimeSeconds(3)
                    .queueUrl(queueUrl)
                    .build()).messages();
            return msgl==null || msgl.size()<=0 ? null : msgl.get(0);
        }catch (QueueDoesNotExistException e){
            System.out.println("sqsGetMessagesFromQueue "+queueUrl+e.getMessage());
            System.exit(1);
        }catch (Exception e){
            System.out.println("sqsGetMessagesFromQueue "+queueUrl+e.getMessage());
            return null;
        }
        return null;
    }

    //s3
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

    public void changeVisibility(Message msg,String queue_url,int timeOut){
        String receipt = msg.receiptHandle();
        sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                .visibilityTimeout(timeOut).
                queueUrl(queue_url).receiptHandle(receipt).build());
    }

    //ec2
    public static void terminateEC2( Ec2Client ec2, String instanceID) {
        try {
            TerminateInstancesRequest ti = TerminateInstancesRequest.builder()
                    .instanceIds(instanceID)
                    .build();

            TerminateInstancesResponse response = ec2.terminateInstances(ti);
            List<InstanceStateChange> list = response.terminatingInstances();

            for (int i = 0; i < list.size(); i++) {
                InstanceStateChange sc = (list.get(i));
                System.out.println("The ID of the terminated instance is " + sc.instanceId());
            }
        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

}

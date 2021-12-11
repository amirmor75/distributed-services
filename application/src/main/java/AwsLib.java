import com.amazonaws.services.ec2.model.DescribeInstancesResult;
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
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsLib {
    private final Ec2Client ec2 = Ec2Client.create();
    private final SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
    private final S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();


    final private static String iamRole = "arn:aws:iam::059028560710:instance-profile/LabInstanceProfile";
    final private static String secGroup = "sg-02af33083ff48c46b";
    private static final AwsLib instance = new AwsLib();




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

    public void sqsSendMessage( String queueUrl,String body,String dupId,String groupId){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageGroupId(groupId)//required for fifo
                .messageDeduplicationId(dupId)
                .build();
        try {
            sqs.sendMessage(send_msg_request);
        } catch (Exception e) {
            System.out.println("error sending msg:" +body + "from queue " + queueUrl + e.getMessage());
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

    public void sqsDeleteQueue(String queueUrl){
        try{
            sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        }
        catch(Exception e){
            System.out.println("error queueUrl"+ queueUrl + e.getMessage());
        }

    }

    public void sqsDeleteAllQueues() {
        try{
            for (String qUrl : sqs.listQueues().queueUrls())
                sqsDeleteQueue(qUrl);
        }catch(Exception ignored){

        }

    }

    //s3
    public void createAndUploadS3Bucket( String bucketName,String key, File file){
        try{
            software.amazon.awssdk.regions.Region region = Region.US_EAST_1;
            s3.createBucket(CreateBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .build())
                    .build());
            // Put Object
            s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key)
                            .build(),
                    RequestBody.fromFile(file));
        }
        catch (Exception e){
            System.out.println("createAndUploadS3Bucket bucket:"+ bucketName +"key "+ key +"file"+file.getName()+ e.getMessage());
        }
    }

    public  File downloadS3File( String bucketName,String keyName ,String fileName){
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();
            ResponseInputStream<GetObjectResponse> responseInputStream = s3.getObject(getObjectRequest);
            File file = new File(fileName);
            InputStream inputStream = new ByteArrayInputStream(responseInputStream.readAllBytes());
            try(OutputStream outputStream = new FileOutputStream(file)){
                IOUtils.copy(inputStream, outputStream);
                return file;
            } catch (FileNotFoundException e) {
                // handle exception here
            } catch (IOException e) {
                // handle exception here
            }
        } catch (Exception e) {
            System.out.println("downloadS3File failed "+bucketName+" "+keyName+" "+fileName +" "+ e.getMessage());
        }
        return null;
    }

    //ec2
    public void ec2CreateKeyPair( String keyName) {

        try {
            CreateKeyPairRequest request = CreateKeyPairRequest.builder()
                    .keyName(keyName).build();

            ec2.createKeyPair(request);
            System.out.printf(
                    "Successfully created key pair named %s\n",
                    keyName);

        } catch (Ec2Exception ignored) {}
    }

    public void ec2CreateManager(String instanceName, String userScript){

        String keyName = "boobik";
        ec2CreateKeyPair( keyName);
        String amiId = /*args[1]*/ "ami-01cc34ab2709337aa";

        try{
            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                    .imageId(amiId)
                    .instanceType(InstanceType.T1_MICRO)
                    .maxCount(1)
                    .minCount(1)
                    .securityGroupIds(secGroup)
                    .keyName(keyName)
                    .iamInstanceProfile(IamInstanceProfileSpecification.builder().arn(iamRole).build())
                    .userData(Base64.encodeBase64String(userScript.getBytes(StandardCharsets.UTF_8)))
                    .build();
            RunInstancesResponse response = ec2.runInstances(runRequest);
            String instanceId = response.instances().get(0).instanceId();

            software.amazon.awssdk.services.ec2.model.Tag tag = Tag.builder()
                    .key("Name")
                    .value(instanceName)
                    .build();

            CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                    .resources(instanceId)
                    .tags(tag)
                    .build();

            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 instance named %s, id %s, based on AMI %s\n",
                    instanceName, instanceId, amiId);
        }catch(Exception e){
            System.out.println("thread "+ Thread.currentThread().getId() +" launching instances failed err msg: "+e.getMessage());
        }
    }
}

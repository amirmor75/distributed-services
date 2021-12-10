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
import software.amazon.awssdk.utils.builder.Buildable;


import java.io.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsLib {
    private static final AwsLib instance = new AwsLib();

    private final Ec2Client ec2 = Ec2Client.create();
    private  final SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
    private final S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();

    final public static String iamRole = "arn:aws:iam::059028560710:instance-profile/LabInstanceProfile";
    private final String secGroup= "sg-02af33083ff48c46b";


    private static final String jarsBucket = "manager-worker-jar-bucket";


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

    public  void sqsSendMessages( String queueUrl,List<String> messages) {
        for (String body : messages)
            sqsSendMessage(queueUrl,body);
    }
    public  void sqsDeleteMessages( String queueUrl,List<Message> messages){
        for (Message m : messages) {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(m.receiptHandle())
                    .build();
            sqs.deleteMessage(deleteRequest);

            try {
                sqs.deleteMessage(deleteRequest);
            } catch (Exception e) {
                System.out.println("error deleting msg:" + m.body() + "from queue " + queueUrl + e.getMessage());
            }
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
    public void sqsChangeVisibility(Message msg,String queue_url,int timeOut){
        String receipt = msg.receiptHandle();
        sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                .visibilityTimeout(timeOut).
                queueUrl(queue_url).receiptHandle(receipt).build());
    }
    public void deleteAllQueues() {
        for (String qUrl : sqs.listQueues().queueUrls())
            sqsDeleteQueue(qUrl);
    }


    //s3
    public void s3DeleteBucket(String bucketName){
        try{
            s3.deleteBucket(DeleteBucketRequest.builder()
                    .bucket(bucketName).build());
        }catch (Exception e){
            System.out.println("failed deleting bucket "+bucketName+e.getMessage());

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
                System.out.println(bucketName+" "+ keyName +" "+ fileName +" "+ e.getMessage());
                // handle exception here
            } catch (IOException e) {
                System.out.println(bucketName+" "+ keyName +" "+ fileName +" "+ e.getMessage());

                // handle exception here
            }
        } catch (Exception e) {
            System.out.println("downloadS3File failed "+bucketName+" "+keyName+" "+fileName +" "+ e.getMessage());
        }
        return null;
    }
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
    public String s3GetUrlOfPdfByUrlOfS3( String operation, String pdfUrlInputFile, String bucketName, String keyName){
        String[] arrSplit=pdfUrlInputFile.split("/",30); // 30 is arbitrary
        String nameOfTheFileWithDot = arrSplit[arrSplit.length-1]; //it looks like shelly.pdf
        String[] arrSplit2=nameOfTheFileWithDot.split(".",30); // 30 is arbitrary
        String nameOfTheFile = arrSplit2[0]; //it looks like shelly (without .pdf)
        switch (operation) {
            case "ToImage":
                nameOfTheFile = nameOfTheFile + ".png";
                break;
            case "ToHTML":
                nameOfTheFile = nameOfTheFile + ".html";
                break;
            case "ToText":
                nameOfTheFile = nameOfTheFile + ".txt";
                break;
            default:
                throw new IllegalArgumentException();
        }
        String path = "../"+nameOfTheFile;
        String bucket = bucketName;
        String key = keyName;
        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toFile(Paths.get(path)));
        return path;
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
    public void ec2CreateInstance(String instanceName){

        String keyName = "boobik";
        ec2CreateKeyPair( keyName);
        String amiId = /*args[1]*/ "ami-01cc34ab2709337aa";

        // snippet-start:[ec2.java2.create_instance.main]
        String userData = "#!/bin/sh\n"+
                "rpm --import https://yum.corretto.aws/corretto.key\n"+
                "curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo\n"+
                "yum install -y java-15-amazon-corretto-devel\n"+
                "aws s3 cp s3://"+jarsBucket+"/worker.jar /home/ec2-user\n"+
                "cd /home/ec2-user\n"+
                "java -jar worker.jar >> a.out";
        try{
            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                    .imageId(amiId)
                    .instanceType(InstanceType.T1_MICRO)
                    .maxCount(1)
                    .minCount(1)
                    .securityGroupIds(secGroup)
                    .keyName(keyName)
                    .iamInstanceProfile(IamInstanceProfileSpecification.builder().arn(iamRole).build())
                    .userData(Base64.encodeBase64String(userData.getBytes(StandardCharsets.UTF_8)))
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
    public  void ec2Terminate( Ec2Client ec2, String instanceID) {
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

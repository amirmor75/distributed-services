import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

public class AwsLib {

    public static String CreateAndGetQueueUrlFromName (SqsClient sqs, String queueName) {

        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            CreateQueueResponse create_result = sqs.createQueue(request);
        } catch (QueueNameExistsException e) {
            throw e;
        }

        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        return sqs.getQueueUrl(getQueueRequest).queueUrl();
    }

    public static List<Message> getMessagesFromQueue(SqsClient sqs, String queueUrl) {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();
        return sqs.receiveMessage(receiveRequest).messages();
    }
    public static void sendSqsMessage(SqsClient sqs, String queueUrl,String message){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .delaySeconds(5)
                .build();
        sqs.sendMessage(send_msg_request);
    }

    public static void createAndUploadS3Bucket(S3Client s3, String bucketName, File file){
        software.amazon.awssdk.regions.Region region = Region.US_EAST_1;
        String key = "key"; //TODO
        s3.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucketName)
                .createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .locationConstraint(region.id())
                                .build())
                .build());

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key)
                        .build(),
                RequestBody.fromFile(file));
    }

}

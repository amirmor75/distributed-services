
package lib;

import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;


import java.io.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

public class AwsLib {

    public static String sqsCreateAndGetQueueUrlFromName (SqsClient sqs, String queueName) {

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

    public static List<Message> sqsGetMessagesFromQueue(SqsClient sqs, String queueUrl) {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();
        return sqs.receiveMessage(receiveRequest).messages();
    }
    public static void sqsSendMessage(SqsClient sqs, String queueUrl,String message){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .delaySeconds(5)
                .build();
        sqs.sendMessage(send_msg_request);
    }
    public static void sqsSendMessages(SqsClient sqs, String queueUrl,List<String> messages){
        for (String body: messages) {
            SendMessageRequest send_msg_request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .delaySeconds(5)
                    .build();
            sqs.sendMessage(send_msg_request);
        }
    }

    public static void sqsDeleteMessages(SqsClient sqs, String queueUrl,List<Message> messages){
        for (Message m : messages) {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(m.receiptHandle())
                    .build();
            sqs.deleteMessage(deleteRequest);
        }
    }
    public static void sqsDeleteMessage(SqsClient sqs, String queueUrl,Message m){
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(m.receiptHandle())
                    .build();
            sqs.deleteMessage(deleteRequest);

    }

    public static void createAndUploadS3Bucket(S3Client s3, String bucketName,String key, File file){
        software.amazon.awssdk.regions.Region region = Region.US_EAST_1;
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

    //Assume that S3Url looks like: bucket /t key
    public static String getUrlOfPdfByUrlOfS3(S3Client s3, String operation, String pdfUrlInputFile, String bucketName, String keyName){
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

    public static File downloadS3File(S3Client s3, String bucketName, String fileName){

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();
        ResponseInputStream<GetObjectResponse> responseInputStream = s3.getObject(getObjectRequest);
        File file = new File(fileName);
        try {
            InputStream inputStream = new ByteArrayInputStream(responseInputStream.readAllBytes());
            try(OutputStream outputStream = new FileOutputStream(file)){
                IOUtils.copy(inputStream, outputStream);
            } catch (FileNotFoundException e) {
                // handle exception here
            } catch (IOException e) {
                // handle exception here
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }



}

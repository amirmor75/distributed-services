package manager;

import lib.AwsLib;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Random;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import java.nio.file.Paths;
import java.util.List;

import static lib.AwsLib.sqsSendMessages;

public class Manager {

    private static final String QUEUE_NAME= "manager_special_sqs";
    private static final String BUCKET_NAME= "localApplication_special_s3Bucket";
    private static final String KEY= "key";

    public void main(String[] args) {
        run();
    }
    public void run() {
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();



        // download the input file from S3
        File inputFile = new File("./input");
        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder().region(region).build();
        s3.getObject(GetObjectRequest.builder().bucket(BUCKET_NAME).key(KEY).build(),
                ResponseTransformer.toFile(inputFile));

        // Creates an SQS message for each URL in the input file together with the operation
        //that should be performed on it.
        String myQueueUrl = AwsLib.sqsCreateAndGetQueueUrlFromName(sqs,QUEUE_NAME);
        List<String> messageBodies = parseInputFile("input");
        sqsSendMessages(sqs,myQueueUrl,messageBodies);


        //Checks the SQS message count and starts Worker processes (nodes) accordingly.


        //The manager should create a worker for every n messages, if there are no
        //running workers.


        //If the message is a termination message, then the manager:
        //▪ Does not accept any more input files from local applications.
        //▪ Waits for all the workers to finish their job, and then terminates them
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

}

//import javafx.util.Pair;

import lib.AwsLib;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class LocalApplication {

    private static final String BUCKET_NAME= "localApplication_special_s3Bucket";
    private static final String QUEUE_NAME= "manager_special_sqs";
    public static void main(String[] args) throws IOException {
        parseInputFile("input-sample-1.txt");
        run();
    }

    private static void run() {
        S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
        File file = new File("input-sample-1.txt");
        AwsLib.createAndUploadS3Bucket(s3,BUCKET_NAME,file);
        String queueUrl = AwsLib.CreateAndGetQueueUrlFromName(sqs,QUEUE_NAME);
        AwsLib.sendSqsMessage(sqs,queueUrl,BUCKET_NAME);
    }


    private static void parseInputFile(String fileName) throws IOException {
        LinkedList<MessageOperationUrl> listOperationUrl = new LinkedList<>();
        Map<String, String> map = new HashMap<String, String>();
        String splitarray[];
        MessageOperationUrl messageOperationUrl;
        BufferedReader readbuffer = new BufferedReader(new FileReader(fileName));
        String strRead;
        String operation;
        String pdfUrl;
        while ((strRead=readbuffer.readLine())!=null){
            splitarray = strRead.split("\t");
            operation = splitarray[0];
            pdfUrl = splitarray[1];
            messageOperationUrl = new MessageOperationUrl(operation,pdfUrl);
            listOperationUrl.add(messageOperationUrl);
            map.put(pdfUrl,operation);
        }
        //System.out.println(map);
        System.out.println(map.size());
        System.out.println(listOperationUrl.size());
    }
}

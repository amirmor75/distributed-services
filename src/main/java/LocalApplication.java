//import javafx.util.Pair;

import lib.AwsLib;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;


import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class LocalApplication {

    private static final String BUCKET_NAME= "localApplication_special_s3Bucket";
    private static final String QUEUE_NAME= "manager_special_sqs";
    public static void main(String[] args) throws IOException {
        //parseInputFile("input-sample-1.txt"); redundent shelly!
        run();
    }

    private static void run() {
        S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
        File file = new File("input-sample-1.txt");

        //Uploads the file to S3
        AwsLib.createAndUploadS3Bucket(s3,BUCKET_NAME,"key",file);
        //Get url of a created sqs
        String queueUrl = AwsLib.sqsCreateAndGetQueueUrlFromName(sqs,QUEUE_NAME);
        //Sends a message to an SQS queue, stating the location of the file on S3 (=BUCKET_NAME)
        AwsLib.sqsSendMessage(sqs,queueUrl,BUCKET_NAME);
        //Checks an SQS queue for a message indicating the process is done and the response (the
        //summary file) is available on S3.

        //Creates an html file representing the results
       // htmlFileResult(operations,linkInputFile,linkOutputFile);
    }

    private static void htmlFileResult(List<String> operations, List<String> linkInputFile, List<String> linkOutputFile) throws IOException {
        File f = new File("result.htm");
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write("<html><body><h1>.........</h1>");
        int operationsSize = operations.size();
        int linkInputFileSize = linkInputFile.size();
        int linkOutputFileSize = linkOutputFile.size();
        int op = 0;
        int li = 0;
        int lo = 0;
        while (op < operationsSize && li < linkInputFileSize && lo < linkOutputFileSize){
            bw.write("<p>" + operations.get(op) + ": " + linkInputFile.get(li) + " " + linkOutputFile.get(lo) + "</p>");
            op++;
            li++;
            lo++;
        }
        bw.write("</body></html>");
        bw.close();

        Desktop.getDesktop().browse(f.toURI());
    }

/*
    private static void parseInputFile(String fileName) throws IOException {
        LinkedList<MessageOperationUrl> listOperationUrl = new LinkedList<>();
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
        }
    }

 */
}

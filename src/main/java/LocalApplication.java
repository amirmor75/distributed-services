//import javafx.util.Pair;

import lib.AwsLib;
import lib.AwsBundle;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;


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
        run(args);
    }

    private static void run(String[] args) {
        S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
        File file = new File("input-sample-1.txt");
        //Checks if a Manager node is active on the EC2 cloud. If it is not, the application will start the
        //manager node.
        checkAndCreateManagerNodeActive();
        //Uploads the file to S3 TODO:WHAT IS THE KEY. awwwww.... i shall tell you when the time is right.
        AwsLib.createAndUploadS3Bucket(s3,BUCKET_NAME,"key",file);
        //Get url of a created sqs
        String managerQueueUrl = AwsLib.sqsCreateAndGetQueueUrlFromName(sqs,QUEUE_NAME);
        //Sends a message to an SQS queue, stating the location of the file on S3 (=BUCKET_NAME)
        AwsLib.sqsSendMessage(sqs,managerQueueUrl,"locationInputFile:\t"+BUCKET_NAME);
        //Checks an SQS queue for a message indicating the process is done and the response (the
        //summary file) is available on S3.
        List<String> results;
        if(searchForConfirmMessageInSQS(sqs,managerQueueUrl)){
            try {
                results = downloadSummaryFileFromS3(s3,BUCKET_NAME,"summary");
                htmlFileResult(results); //Creates an html file representing the results
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //In case of terminate mode (as defined by the command-line argument), sends a termination
        //message to the Manager.
        if(terminateMode(args)){
            AwsLib.sqsSendMessage(sqs,managerQueueUrl,"terminateMode"); //sends to the same queue = QUEUE_NAME
        }
    }

    private static void checkAndCreateManagerNodeActive(){
        AwsBundle instance=AwsBundle.getInstance();
        if(!instance.checkIfInstanceExist("Manager"))
        {
            createManager();
        }
    }

    private static void createManager()
    {
        AwsBundle instance=AwsBundle.getInstance();
        String managerScript = "#! /bin/bash\n" +
                "sudo yum update -y\n" +
                "mkdir ManagerFiles\n" +
                "aws s3 cp s3://ocr-assignment1/JarFiles/Manager.jar ./ManagerFiles\n" +
                "java -jar /ManagerFiles/Manager.jar\n";

        instance.createInstance("Manager",AwsBundle.ami,managerScript);
    }

    private static boolean terminateMode(String[] args) {
        return args.length == 3;
    }

    private static boolean searchForConfirmMessageInSQS(SqsClient sqs, String queueUrl){
        List<Message> messages = AwsLib.sqsGetMessagesFromQueue(sqs,queueUrl);
        int i = 0;
        while(i < messages.size()){
            if(messages.get(i).body().equals("resultReady")){
                return true;
            }
            i++;
        }
        return false;
    }

    private static List<String> downloadSummaryFileFromS3(S3Client s3, String bucketName,String filename) throws IOException {
        File file = AwsLib.downloadS3File(s3,bucketName,filename);
        LinkedList<String> listOperationUrlResult = new LinkedList<>();
        String splitarray[];
        BufferedReader readbuffer = new BufferedReader(new FileReader(file));
        String strRead;
        String operation;
        String pdfUrlInputFile;
        String pdfUrlInS3OutputFile;
        String pdfUrlOutputFile;
        String S3BucketName;
        String S3Key;
        while ((strRead=readbuffer.readLine())!=null){
            splitarray = strRead.split("\t");
            pdfUrlInputFile = splitarray[0];
            S3BucketName = splitarray[1];
            S3Key = splitarray[2];
            operation = splitarray[3];
            pdfUrlOutputFile = AwsLib.getUrlOfPdfByUrlOfS3(s3,operation,pdfUrlInputFile,S3BucketName,S3Key);
            listOperationUrlResult.add(operation + ": " + pdfUrlInputFile + " " + pdfUrlOutputFile);
        }
        return listOperationUrlResult;
    }

    private static void htmlFileResult(List<String> results) throws IOException {
        File f = new File("result.html");
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write("<html><body><h1>.........</h1>");
        int i = 0;
        while (i < results.size()){
            bw.write("<p>" + results.get(i) + "</p>");
            i++;
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

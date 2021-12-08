//import javafx.util.Pair;

import com.amazonaws.services.ec2.model.*;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;


import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class LocalApplication {

    private static final String bucketManagerName= "manager-input-bucket";
    private static final String keyManagerName= "manager-input-bucket-key";

    private static final String managerInputQueueName= "manager-input-queue";
    private static final String outputQueueName="local-app-result-queue";

    private static final String jarsBucket = "manager-worker-jar-bucket";

    private static final AtomicLong LAST_TIME_MS = new AtomicLong();
    private static final AwsLib lib = AwsLib.getInstance();


    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("not enough arguments given!");
            System.exit(1);
        }

        String inputFileName = args[0];
        String outputFileName = args[1];
        int n = Integer.parseInt(args[2]);
        run(inputFileName,outputFileName,n,args.length==4);
//        lib.createAndUploadS3Bucket(jarsBucket,"manager.jar",new File("manager.jar"));
//        lib.createAndUploadS3Bucket(jarsBucket,"worker.jar",new File("worker.jar"));

    }

    private static void run(String inputFileName,String outputFileName, int n,boolean terminate) {
        long localUnique = uniqueCurrentTimeMS();
        String localKeyName = keyManagerName + localUnique ;
        String localOutputQueueName = outputQueueName + localUnique;
        String localOutputQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(localOutputQueueName);
        File inputfile = new File(inputFileName);

        //Checks if a Manager node is active on the EC2 cloud. If it is not, the application will start the
        //manager node.
        checkAndCreateManagerNodeActive();

        //Uploads the file to S3
        lib.createAndUploadS3Bucket(bucketManagerName,localKeyName,inputfile);

        //Get url of a created sqs
        String managerQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(managerInputQueueName);

        //Sends a message to an SQS queue, stating the location of the file on S3 (=BUCKET_NAME)
        lib.sqsSendMessage(managerQueueUrl, bucketManagerName+'\t'+localKeyName+'\t'+ localOutputQueueName +'\t'+n);

        //Checks an SQS queue for a message indicating the process is done and the response (the
        //summary file) is available on S3.
        Message resMsg = getResultMessage(localOutputQueueUrl);
        String splitarray[];
        splitarray = resMsg.body().split("\t");
        String outBucket = splitarray[0];
        String outKey = splitarray[1];
        List<String> results;
        try {
            results = downloadSummaryFileFromS3(outBucket,outKey,"summary"+localUnique);
            htmlFileResult(results,outputFileName); //Creates an html file representing the results
            System.out.printf("result file ready, name : %s\n " , outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //In case of terminate mode (as defined by the command-line argument), sends a termination
        //message to the Manager.
        if(terminate){
            lib.sqsSendMessage(managerQueueUrl,"terminate");
            AwsBundle.getInstance().deleteBucket(bucketManagerName);
        }
    }

    private static Message getResultMessage(String localOutputQueueUrl) {
        AwsLib lib = AwsLib.getInstance();

        System.out.println("waiting for result message");
        List<Message> messages;
        while(true){
            messages = lib.sqsGetMessagesFromQueue(localOutputQueueUrl);
            if(messages!= null && messages.size()>0)
                return messages.get(0);
        }
    }

    private static void checkAndCreateManagerNodeActive(){
        AwsBundle instance=AwsBundle.getInstance();
        if(!instance.checkIfInstanceExist("manager"))
        {
//            uploadManagerWorkerCodeToS3();
            createManager();
        }else{
            System.out.println("manager exists,continuing..");
        }
    }

    private static void createManager()
    {

        String managerScript = "#!/bin/bash\n" +
                "sudo rpm --import https://yum.corretto.aws/corretto.key\n" +
                "sudo curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo\n"+
                "sudo yum install -y java-15-amazon-corretto-devel\n"+
                "aws s3 cp s3://"+jarsBucket+"/manager.jar .\n" +
                "cd /\n"+
                "java -jar manager.jar >> a.out\n";
        lib.ec2CreateManager("manager", managerScript);
    }
//    private static void uploadManagerWorkerCodeToS3() {
//        lib.createAndUploadS3Bucket(managerBucketcode,managerKey,new File("manager.jar"));
//        lib.createAndUploadS3Bucket(workerBucket,workerKey,new File("worker.jar"));
//    }

    private static List<String> downloadSummaryFileFromS3( String bucketName,String localKeyName,String filename) throws IOException {
        File file = lib.downloadS3File(bucketName,localKeyName,filename);
        LinkedList<String> listOperationUrlResult = new LinkedList<>();
        String splitarray[];
        BufferedReader readbuffer = new BufferedReader(new FileReader(file));
        String strRead;
        String operation;
        String pdfUrlInputFile;
        String pdfUrlInS3OutputFile;
        String pdfUrlOutputFile;
        String S3BucketName;
        String S3KeyOrErrorMsg;
        while ((strRead=readbuffer.readLine())!=null){
            splitarray = strRead.split("\t");
            pdfUrlInputFile = splitarray[2];
            S3BucketName = splitarray[3];
            S3KeyOrErrorMsg = splitarray[4];
            operation = splitarray[5];
            if(S3BucketName.equals("error")) {
                listOperationUrlResult.add('-'+operation + ":\t" + pdfUrlInputFile + '\t' + S3KeyOrErrorMsg);
                continue;
            }
            pdfUrlOutputFile = getUrlOfPdfByUrlOfS3(operation,pdfUrlInputFile,S3BucketName,S3KeyOrErrorMsg);
            listOperationUrlResult.add('+'+operation + ":\t" + pdfUrlInputFile + '\t' + pdfUrlOutputFile);
        }
        return listOperationUrlResult;
    }

    private static void htmlFileResult(List<String> results,String outputFileName) throws IOException {
        File f = new File(outputFileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write("<html><body><h1>resultFile</h1>");
        int i = 0;
        while (i < results.size()){
            bw.write("<p>" + results.get(i) + "</p>");
            i++;
        }
        bw.write("</body></html>");
        bw.close();

        Desktop.getDesktop().browse(f.toURI());
    }
    public static String getUrlOfPdfByUrlOfS3( String operation, String pdfUrlInputFile, String bucketName, String keyName){
        String[] arrSplit=pdfUrlInputFile.split("/",30); // 30 is arbitrary
        String nameOfTheFileWithDot = arrSplit[arrSplit.length-1]; //it looks like shelly.pdf
        String nameOfTheFile = "";
        if (nameOfTheFileWithDot.indexOf(".") > 0)
            nameOfTheFile = nameOfTheFileWithDot.substring(0, nameOfTheFileWithDot.lastIndexOf("."));

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
        String path = "./"+nameOfTheFile;
        String bucket = bucketName;
        String key = keyName;
        lib.downloadS3File(bucket,key,"./"+nameOfTheFile);
        return path;
    }

    public static long uniqueCurrentTimeMS() {
        long now = System.currentTimeMillis();
        while(true) {
            long lastTime = LAST_TIME_MS.get();
            if (lastTime >= now)
                now = lastTime+1;
            if (LAST_TIME_MS.compareAndSet(lastTime, now))
                return now;
        }
    }

}

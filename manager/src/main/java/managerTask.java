import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.InstanceStateChange;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class managerTask implements Runnable{
    private final AwsLib lib = AwsLib.getInstance();
    private static final String managerInputQueueName= "manager-input-queue";
    private static Message startMessage;

    private final static String workersQueueName = "workers-queue";
    private final String workersQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersQueueName);

    private static final String outputBucketName = "manager-output-bucket";
//    private static final String summeryFolder = ".\\summeryFiles";
//    private static final String inputFolder = ".\\inputFiles";

    managerTask(Message startMessage) {
        this.startMessage = startMessage;
    }

    public void run( ) {

        String[] splitArray;
        splitArray = startMessage.body().split("\t");
        String inputBucketName = splitArray[0];
        String inputKeyName = splitArray[1];
        String outputQueueName = splitArray[2];
        int n  = Integer.parseInt( splitArray[3]);
        // download the input file from S3
        File inputFile = lib.downloadS3File( inputBucketName,inputKeyName, "inputFile" + Thread.currentThread().getId());//make name unique per thread
        String outputQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(outputQueueName);
        String outputKeyName = outputQueueName ;
        String mangerQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(managerInputQueueName);
        lib.changeVisibility(startMessage,mangerQueueUrl,1800/*30min*/);

        // Creates an SQS message for each URL in the input file together with the operation
        //that should be performed on it.
        List<String> messageBodies = parseInputFile(inputFile,outputQueueName);
        lib.sqsSendMessages(workersQueueUrl, messageBodies);
        System.out.println("sent all tasks to workers t" + Thread.currentThread().getId());
        //Checks the SQS message count and starts Worker processes (nodes) accordingly.
        //The manager should create a worker for every n messages, if there are no
        //running workers.

        int workersPoolSize = messageBodies.size() / n == 0 ? 1 : messageBodies.size() / n;
        int running = AwsBundle.getInstance().checkInstanceCount()-1;
        workersPoolSize = Math.min ( 19 - running, Math.max(workersPoolSize - running , 0));
        System.out.println("thread"+Thread.currentThread().getId()+" worker pool size:" + workersPoolSize);
        for (int i = 0; i < workersPoolSize; i++)
            lib.ec2CreateInstance("worker" +Thread.currentThread().getId()+ i);

        System.out.printf("sent %d messages\n",messageBodies.size());

        //Manager reads all Workers' messages from SQS and creates one summary file, once all URLs
        //in the input file have been processed.
        List<Message> managerQueueMessages = new LinkedList<>();
        while (managerQueueMessages.size() < messageBodies.size()) {
            List<Message> message_list = lib.sqsGetMessagesFromQueue(workersQueueUrl);
            if(message_list == null || message_list.size()<=0)
                continue;
            for (Message m : message_list) {
                if ( m.body().startsWith("out\t"+outputQueueName+'\t') && !managerQueueMessages.contains(m))
                    managerQueueMessages.add(m);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        System.out.println("t"+Thread.currentThread().getId()+" received all output messages");
        //now have read all workers results
        File summery = createSummaryFile(managerQueueMessages);
        //kill all instances
        // Manager uploads the summary file to S3.
        lib.createAndUploadS3Bucket( outputBucketName, outputKeyName, summery);

        // Manager posts an SQS message about the summary file
        lib.sqsSendMessage(outputQueueUrl, outputBucketName + '\t'+ outputKeyName );
        lib.sqsDeleteMessage(mangerQueueUrl, startMessage);
    }
    private static File createSummaryFile(List<Message> messages){
        // <outputqueue> \t <pdf url input file> \t <bucket> \t <keyname> <operation>
        File summary = new File("summaryFile"+Thread.currentThread().getId());
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(summary));
            for (Message m: messages )
                bw.write(m.body()+'\n');
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return summary;
    }
//    public static long uniqueCurrentTimeMS() {
//        long now = System.currentTimeMillis();
//        while(true) {
//            long lastTime = LAST_TIME_MS.get();
//            if (lastTime >= now)
//                now = lastTime+1;
//            if (LAST_TIME_MS.compareAndSet(lastTime, now))
//                return now;
//        }
//    }
    private List<String> parseInputFile(File file,String outputQueueName) {

        List<String> listOperationUrl = new LinkedList<>();
        try{
            BufferedReader readBuffer = new BufferedReader(new FileReader(file.getName()));
            String strRead;

            while ((strRead=readBuffer.readLine())!=null)
                listOperationUrl.add("in\t"+outputQueueName+'\t'+strRead);
        }catch (Exception e){
            e.printStackTrace();
        }
        return listOperationUrl;
    }

}

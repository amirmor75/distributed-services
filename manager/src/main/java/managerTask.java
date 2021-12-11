
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class managerTask implements Runnable{
    private final AwsLib lib = AwsLib.getInstance();
    private static final String managerInputQueueName= "manager-input-queue.fifo";
    private static Message startMessage;

    private final static String workersInQueueName = "workers-in-queue.fifo";
    private final static String workersOutQueueName = "workers-out-queue.fifo";

    private final String workersInQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersInQueueName);
    private final String workersOutQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersOutQueueName);

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
        String resultQueueName = splitArray[2];
        int n  = Integer.parseInt( splitArray[3]);
        // download the input file from S3
        File inputFile = lib.downloadS3File( inputBucketName,inputKeyName, "inputFile" + Thread.currentThread().getId());//make name unique per thread
        String outputQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(resultQueueName);
        String outputKeyName = resultQueueName ;
        String mangerQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(managerInputQueueName);
        lib.sqsChangeVisibility(startMessage,mangerQueueUrl,1800/*30min*/);

        // Creates an SQS message for each URL in the input file together with the operation
        //that should be performed on it.
        List<String> messageBodies = parseInputFile(inputFile,resultQueueName);
        lib.sqsSendMessages(workersInQueueUrl, messageBodies);
        System.out.println("sent all tasks to workers t" + Thread.currentThread().getId());
        //Checks the SQS message count and starts Worker processes (nodes) accordingly.
        //The manager should create a worker for every n messages, if there are no
        //running workers.

        int workersPoolSize = messageBodies.size() / n == 0 ? 1 : messageBodies.size() / n;
        int running = AwsBundle.getInstance().checkInstanceCount()-1;
        workersPoolSize = Math.min ( 19 - running, Math.max(workersPoolSize - running , 0));
        System.out.println("thread"+Thread.currentThread().getId()+" worker pool size:" + workersPoolSize);

//        for (int i = 0; i < workersPoolSize; i++)
//            lib.ec2CreateInstance("worker" +Thread.currentThread().getId()+ i);

        System.out.printf("sent %d messages\n",messageBodies.size());

        //Manager reads all Workers' messages from SQS and creates one summary file, once all URLs
        //in the input file have been processed.
        List<Message> workersOutputMessages = new LinkedList<>();
        while (workersOutputMessages.size() < messageBodies.size()) {
            List<Message> currMsgl = lib.sqsGetMessagesFromQueue(workersOutQueueUrl);
            if(currMsgl == null)
                continue;
            for (Message currMsg:currMsgl){
                if(currMsg == null)
                    continue;
                if ( currMsg.body().startsWith(resultQueueName+'\t'))
                    lib.sqsDeleteMessage(workersOutQueueUrl,currMsg);
                    workersOutputMessages.add(currMsg);
            }
            try {
                System.out.println("t"+Thread.currentThread().getId()+" got "+workersOutputMessages.size()+" messages");
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
        System.out.println("t"+Thread.currentThread().getId()+" received all output messages");
        //now have read all workers results
        File summery = createSummaryFile(workersOutputMessages);
        //kill all instances
        // Manager uploads the summary file to S3.
        lib.createAndUploadS3Bucket( outputBucketName, outputKeyName, summery);

        // Manager posts an SQS message about the summary file
        lib.sqsSendMessage(outputQueueUrl, outputBucketName + '\t'+ outputKeyName , "res","res" );
        lib.sqsDeleteMessage(mangerQueueUrl, startMessage);
        lib.sqsDeleteMessages(workersOutQueueUrl,workersOutputMessages);
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
                listOperationUrl.add(outputQueueName+'\t'+strRead);
        }catch (Exception e){
            e.printStackTrace();
        }
        return listOperationUrl;
    }

}

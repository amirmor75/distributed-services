//package lib;
//import com.amazonaws.services.sqs.model.Message;
//import com.google.gson.Gson;
//
//import java.io.*;
//import java.util.*;
//
//public class Local {
//
//    final static AwsBundle awsBundle = AwsBundle.getInstance();
//
//    public static void main(String[] args) {
//
//        final String uniqueLocalId = "id1";
//        final String uniquePathLocalApp =  awsBundle.inputFolder+"/"+ uniqueLocalId;
//        boolean shouldTerminate = false;
//        boolean gotResult = false;
//
//        if(args.length == 3 || args.length == 4) {
//            if (args.length == 4) {
//                if (args[3].equals("terminate"))
//                    shouldTerminate = true;
//                else {
//                    System.err.println("Invalid command line argument: " + args[4]);
//                    System.exit(1);
//                }
//            }
//        }
//        else {
//            System.err.println("Invalid number of command line arguments");
//            System.exit(1);
//        }
//
//        if (!isLegalFileSize(new File(args[0])))
//        {
//            System.out.println("Input file is over maximal size (10MB)");
//            System.exit(1);
//        }
//
//        if(!awsBundle.checkIfInstanceExist("Manager"))
//        {
//                createManager();
//        }
//
//        String resultQueueUrl = awsBundle.createMsgQueue(AwsBundle.resultQueuePrefix+uniqueLocalId);
//        awsBundle.createBucketIfNotExists(AwsBundle.bucketName);
//        awsBundle.uploadFileToS3(AwsBundle.bucketName,uniquePathLocalApp,args[0]);
//
//        String managerQueueUrl= awsBundle.getQueueUrl(awsBundle.requestsAppsQueueName);
//        String type = shouldTerminate ? "terminate" : "input";
//        awsBundle.sendMessage(managerQueueUrl,createMessage(type,uniquePathLocalApp,AwsBundle.Delimiter,args[1].toLowerCase(),args[2]));
//        System.out.println("\nMessage sent");
//
//        while(!gotResult)
//        {
//            List<Message> messages= awsBundle.fetchNewMessages(resultQueueUrl);
//            if (!messages.isEmpty())
//            {
//                String result = messages.get(0).getBody();
//                if (result.equals("terminate message"))
//                {
//                    terminate(1,uniquePathLocalApp,resultQueueUrl,result);
//                    System.exit(0);
//                }
//                InputStream resultFile = awsBundle.downloadFileFromS3(AwsBundle.bucketName,result);
//                Gson gson = new Gson();
//                try (Reader reader = new InputStreamReader(resultFile)) {
//                    // Convert JSON File to Java Object
//                    Pair<String,String>[] urlsAndText = gson.fromJson(reader, Pair[].class);
//                    createHtmlFile(urlsAndText,args[1]);
//                    System.out.println("Html file created successfully\n");
//                    terminate(0,uniquePathLocalApp,resultQueueUrl,result);
//                    break;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//
//    }
//
//    private static void createManager()
//    {
//        String managerScript = "#! /bin/bash\n" +
//                "sudo yum update -y\n" +
//                "mkdir ManagerFiles\n" +
//                "aws s3 cp s3://ocr-assignment1/JarFiles/Manager.jar ./ManagerFiles\n" +
//                "java -jar /ManagerFiles/Manager.jar\n";
//
//        awsBundle.createInstance("Manager",AwsBundle.ami,managerScript);
//    }
//
//    private static String createMessage(String type, String filePath,String delimiter)
//    {
//        StringBuilder message = new StringBuilder();
//        message.append(type);
//        message.append(filePath);
//        message.append(delimiter);
//        return message.toString();
//    }
//
//
//}
//

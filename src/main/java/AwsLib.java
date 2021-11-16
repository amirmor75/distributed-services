import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

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
}

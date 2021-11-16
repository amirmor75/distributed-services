import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

public class Manager {

    private static final String QUEUE_NAME= "manager_special_sqs";
    public void main(String[] args) {
        run();
    }
    public void run() {
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
        String queueUrl = AwsLib.CreateAndGetQueueUrlFromName(sqs,QUEUE_NAME);
        List<Message> messages = AwsLib.getMessagesFromQueue(sqs, queueUrl);
    }

}

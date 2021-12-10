import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

import java.io.File;
import java.io.InputStream;
import java.util.*;


public class AwsBundle {
    private final AmazonEC2 ec2;


    private static final AwsBundle instance = new AwsBundle();

    private AwsBundle(){
        ec2 = AmazonEC2ClientBuilder.defaultClient();
    }

    public static AwsBundle getInstance()
    {
        return instance;
    }


    public int checkInstanceCount()
    {
        int k = 0;
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        DescribeInstancesResult describeInstancesResult = this.ec2.describeInstances(describeInstancesRequest);
        for (Reservation r : describeInstancesResult.getReservations())
        {
            for (Instance i : r.getInstances())
            {
                if (i.getState().getName().equals("running"))
                    k++;
            }
        }
        return k;
    }



    public void terminateCurrentInstance()
    {
//        String instanceId = EC2MetadataUtils.getInstanceId();
//        List<String> instanceIds = new ArrayList<>();
//        instanceIds.add(instanceId);
//        TerminateInstancesRequest request = new TerminateInstancesRequest(instanceIds);
//        this.ec2.terminateInstances(request);
        String instanceId = EC2MetadataUtils.getInstanceId();
        software.amazon.awssdk.regions.Region region = Region.US_EAST_1;
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();

        AwsLib.getInstance().ec2Terminate(ec2, instanceId); ;
        ec2.close();
    }
}

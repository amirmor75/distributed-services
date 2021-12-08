import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.s3.model.*;

import java.util.Iterator;


public class AwsBundle {
    private static final int MAX_INSTANCES = 19 ;
    private final AmazonEC2 ec2;
    private final AmazonS3 s3;
    private final AmazonSQS sqs;


    public final String requestsAppsQueueName = "requestsAppsQueue";
    public final String requestsWorkersQueueName = "requestsWorkersQueue";
    public final String resultsWorkersQueueName = "resultsWorkersQueue";

    public static final String bucketName = "assignment1";
    public static final String inputFolder = "input-files";
    public static final String outputFolder = "output-files";
    public static final String resultQueuePrefix = "resultQueue_";


    public static final String ami = "ami-0279c3b3186e54acd";

    //message from local
    public final int messageType = 0;
    public final int uniqueLocalFilePath = 1;
    public final int outputFilepath = 2;
    public final int workersRatio = 3;
    static final String Delimiter = "X";

    //message from worker
    public final int urlIndex = 0;
    public final int textIndex = 1;

    //message to worker
    public final int localIdIndex = 0;
    public final int lineNumberIndex = 1;
    public final int urlWorkerIndex = 2;


    private static final AwsBundle instance = new AwsBundle();

    private AwsBundle(){
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        s3 = AmazonS3ClientBuilder.defaultClient();
        sqs = AmazonSQSClientBuilder.defaultClient();
    }

    public static AwsBundle getInstance()
    {
        return instance;
    }


    public boolean checkIfInstanceExist(String name)
    {
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        DescribeInstancesResult describeInstancesResult = this.ec2.describeInstances(describeInstancesRequest);
        for (Reservation r : describeInstancesResult.getReservations())
        {
            for (Instance i : r.getInstances())
            {
                if (!i.getState().getName().equals("running"))
                    continue;
                for (Tag t : i.getTags())
                {
                    if (t.getKey().equals("Name")&&t.getValue().equals(name))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void deleteBucket(String BucketName){
        Regions clientRegion = Regions.DEFAULT_REGION;
        String bucketName = "*** Bucket name ***";

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new ProfileCredentialsProvider())
                    .withRegion(clientRegion)
                    .build();

            // Delete all objects from the bucket. This is sufficient
            // for unversioned buckets. For versioned buckets, when you attempt to delete objects, Amazon S3 inserts
            // delete markers for all objects, but doesn't delete the object versions.
            // To delete objects from versioned buckets, delete all of the object versions before deleting
            // the bucket (see below for an example).
            ObjectListing objectListing = s3Client.listObjects(bucketName);
            while (true) {
                Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
                while (objIter.hasNext()) {
                    s3Client.deleteObject(bucketName, objIter.next().getKey());
                }

                // If the bucket contains many objects, the listObjects() call
                // might not return all of the objects in the first listing. Check to
                // see whether the listing was truncated. If so, retrieve the next page of objects
                // and delete them.
                if (objectListing.isTruncated()) {
                    objectListing = s3Client.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }

            // Delete all object versions (required for versioned buckets).
            VersionListing versionList = s3Client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
            while (true) {
                Iterator<S3VersionSummary> versionIter = versionList.getVersionSummaries().iterator();
                while (versionIter.hasNext()) {
                    S3VersionSummary vs = versionIter.next();
                    s3Client.deleteVersion(bucketName, vs.getKey(), vs.getVersionId());
                }

                if (versionList.isTruncated()) {
                    versionList = s3Client.listNextBatchOfVersions(versionList);
                } else {
                    break;
                }
            }

            // After all objects and object versions are deleted, delete the bucket.
            s3Client.deleteBucket(bucketName);
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            System.out.println(e.getMessage());
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client couldn't
            // parse the response from Amazon S3.
            System.out.println(e.getMessage());

        }

    }


}

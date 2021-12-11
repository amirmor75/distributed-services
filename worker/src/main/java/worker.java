import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Timer;

import org.apache.pdfbox.tools.PDFText2HTML;
import software.amazon.awssdk.services.sqs.model.Message;


public class worker {
    private static String outputQueue;
    private static String operation;
    private static String pdfUrlInputFile;
    private final static String workersResultsBucketName = "workers-results-bucket";
    private final static String workersResultsKey = "workers-results-key";

    private final static String workersInQueueName = "workers-in-queue.fifo";
    private final static String workersOutQueueName = "workers-out-queue.fifo";



    private final static AwsLib lib = AwsLib.getInstance();
    private static Message startMessage ;


    private static final String pdfFolder = "."/*".\\pdfFiles"*/;
    private static final String convertedFolder = "."/*".\\convertedFiles"*/;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args){
        String workersInQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersInQueueName);
        String workersOutQueueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersOutQueueName);
           while (true) {
            run(workersInQueueUrl,workersOutQueueUrl);
        }
    }

    private static void run( String workersInQueueUrl, String workersOutQueueUrl) {
        //▪ Get a message from an SQS queue.
        while (true){
            startMessage = lib.sqsGetMessageFromQueue(workersInQueueUrl);
            if(startMessage!=null)
                break;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        lib.changeVisibility(startMessage,workersInQueueUrl,120/*2min*/);

        if (startMessage.body().startsWith("terminate")){
            System.out.println("found termination");
            lib.changeVisibility(startMessage,workersInQueueUrl,0/*2min*/);
            AwsBundle.getInstance().terminateCurrentInstance();
            System.exit(1);
        }
        String[] splitArray;
        splitArray = startMessage.body().split("\t");
        outputQueue = splitArray[0];
        operation = splitArray[1];
        pdfUrlInputFile = splitArray[2];
        System.out.println(startMessage.body() + "\n");
        //▪ Download the PDF file indicated in the message.
        downloadPdf(pdfUrlInputFile, pdfFolder,workersOutQueueUrl);

        //▪ Perform the operation requested on the file.
        String[] arrSplit = pdfUrlInputFile.split("/", 30); // 30 is arbitrary
        String nameOfTheFileWithDot = arrSplit[arrSplit.length - 1]; //it looks like shelly.pdf
        String nameOfTheFile = "";
        if (nameOfTheFileWithDot.indexOf(".") > 0)
            nameOfTheFile = nameOfTheFileWithDot.substring(0, nameOfTheFileWithDot.lastIndexOf("."));

        String format = convertPdf(nameOfTheFile, operation,workersOutQueueUrl);
        if (format==null)
            return;
        //▪ Upload the resulting output file to S3.
        try {
            lib.createAndUploadS3Bucket(workersResultsBucketName, workersResultsKey+nameOfTheFile, new File(convertedFolder+"\\"+nameOfTheFile + format));
        }catch (Exception e){
            sendErrorMsg(workersOutQueueUrl,e.getMessage());
            return;
        }
        //▪ Put a message in an SQS queue indicating the original URL of the PDF, the S3 url of the new
        // image file, and the operation that was performed.
        lib.sqsSendMessage(workersOutQueueUrl, outputQueue + "\t" + pdfUrlInputFile + "\t"
                + workersResultsBucketName + "\t" + workersResultsKey+nameOfTheFile +"\t" + operation , outputQueue+nameOfTheFile ,outputQueue+nameOfTheFile  );
        //▪ remove the processed message from the SQS queue.
        lib.sqsDeleteMessage(workersInQueueUrl, startMessage);
    }

    public static void downloadPdf(String urlStr, String destinationPath, String queueUrl){
        String[] arrSplit=urlStr.split("/",30); // 30 is arbitrary
        String name = arrSplit[arrSplit.length-1];
        try {
            URL url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10000);//10 sec til time out
            connection.getInputStream();
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, Paths.get(name), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                sendErrorMsg(queueUrl,e.getMessage());
            }
        }catch(Exception e){
            sendErrorMsg(queueUrl,e.getMessage());
        }
    }

    public static String convertPdf(String fileName, String action, String queueUrl) {
        // load pdf to an object in order to convert it
        System.out.println("FileName is: "+fileName + "\n");
        File file = new File(fileName+".pdf");
        PDDocument document;
        try {
            document = Loader.loadPDF(file);
        } catch (IOException e) {
            //send a message to the manager
            System.out.println("loadPDF failed: "+fileName+" "+ e.getMessage());
            sendErrorMsg(queueUrl,e.getMessage());
            return null;
        }
        PDDocument toConvert= new PDDocument();
        toConvert.addPage(document.getPage(0));
        //switch case
            switch (action) {
                    case "ToImage":
                        return convertPdfToImage(fileName,".",toConvert,queueUrl);
                    case "ToHTML":
                        return convertPdfToHtml(fileName,".",toConvert,queueUrl);
                    case "ToText":
                        return convertPdfToText(fileName,".",toConvert,queueUrl);

                default:
                    return null;
            }
    }


    public static String convertPdfToText(String fileName, String destinationPath, PDDocument document, String queueUrl){

        try{

            PDFTextStripper pdfStripper = new PDFTextStripper();

            //Retrieving text from PDF document

            String text = pdfStripper.getText(document);
            //Closing the document
            document.close();
            File textFile = new File(destinationPath+"\\"+fileName + ".txt");
            FileWriter myWriter = new FileWriter(textFile);
            myWriter.write(text);
            myWriter.close();
        } catch (IOException e) {
            //send a message to the manager
            sendErrorMsg(queueUrl,e.getMessage());

            return null;
        }
        return ".txt";
    }


    public static String convertPdfToHtml(String fileName, String destinationFolder, PDDocument document, String queueUrl){
        try{
            PDFText2HTML parser = new PDFText2HTML();
            Writer output = new PrintWriter(destinationFolder+"\\"+fileName+".html", "utf-8");
            parser.writeText(document, output);
            output.close();
            if (document != null) {
                document.close();
            }
        }catch (IOException e){
            //send a message to the manager
            sendErrorMsg(queueUrl,e.getMessage());

            return null;

        }
        return ".html";

    }

    public static String convertPdfToImage(String fileName, String destinationFolder, PDDocument document, String queueUrl){
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int page = 0;
        BufferedImage bim;
        try {
            bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
        } catch (IOException e) {
            //send a message to the manager
            sendErrorMsg(queueUrl,e.getMessage());
            return null;
        }

        try {
            ImageIOUtil.writeImage(bim, destinationFolder+"\\"+fileName + ".png", 300);
        } catch (IOException e) {
            //send a message to the manager
            sendErrorMsg(queueUrl,e.getMessage());
            return null;
        }
        try {
            document.close();
        } catch (IOException e) {
            //send a message to the manager
            sendErrorMsg(queueUrl,e.getMessage());
            return null;
        }
        return ".png";

    }

    private static void sendErrorMsg(String queueUrl,String errorMsg){
        System.out.println(errorMsg);
        lib.sqsSendMessage(queueUrl,outputQueue+
                '\t'+pdfUrlInputFile+'\t'+"error"+'\t'+errorMsg+'\t'+operation,outputQueue+ LocalDateTime.now(),outputQueue+ LocalDateTime.now() );
        lib.sqsDeleteMessage(queueUrl, startMessage);
    }

}

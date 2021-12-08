import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.List;

import org.apache.pdfbox.tools.PDFText2HTML;
import software.amazon.awssdk.services.sqs.model.Message;


public class worker {
    private static String outputQueue;
    private static String operation;
    private static String pdfUrlInputFile;
    private final static String workersResultsBucketName = "workers-results-bucket";
    private final static String workersResultsKey = "workers-results-key";

    private final static String workersQueueName = "workers-queue";

    private final static AwsLib lib = AwsLib.getInstance();
    private static Message startMessage ;


    private static final String pdfFolder = "."/*".\\pdfFiles"*/;
    private static final String convertedFolder = "."/*".\\convertedFiles"*/;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args){
        String queueUrl = lib.sqsCreateAndGetQueueUrlFromName(workersQueueName);
//        new File(pdfFolder).mkdirs();
//        new File(convertedFolder).mkdirs();
        while (true) {
            run(queueUrl);
        }

    }

    private static void run(String queueUrl) {
        //▪ Get a message from an SQS queue.
        List<Message> messages;
        do{
             messages = lib.sqsGetMessagesFromQueue(queueUrl);
        } while ( messages==null || messages.size()<=0);

        for (Message m: messages) { //terminate in case the manager sends a termination message
            if(m.body().startsWith("terminate")){
                System.out.println("found termination");
                AwsBundle.getInstance().terminateCurrentInstance();
                System.exit(1);
            }
        }

        //▪ Download the PDF file indicated in the message.
        String[] splitArray;
        int i = randomIntFromInterval(0, messages.size() - 1);
        Message message = messages.get(i);
        int count =0;
        while (!message.body().startsWith("in\t")) {
            do{
                messages = lib.sqsGetMessagesFromQueue(queueUrl);
            } while ( messages==null || messages.size()<=0);
            while (!message.body().startsWith("in\t")) {
                i = randomIntFromInterval(0, messages.size() - 1);
                message = messages.get(i);
                count++;
                if (messages.size() < count)
                    break;
            }
        }
        startMessage = message;

        splitArray = message.body().split("\t");
        outputQueue = splitArray[1];
        operation = splitArray[2];
        pdfUrlInputFile = splitArray[3];
        System.out.println(message.body() + "\n");
        downloadPdf(pdfUrlInputFile, pdfFolder,queueUrl);
        //▪ Perform the operation requested on the file.
        String[] arrSplit = pdfUrlInputFile.split("/", 30); // 30 is arbitrary
        String nameOfTheFileWithDot = arrSplit[arrSplit.length - 1]; //it looks like shelly.pdf
        String nameOfTheFile = "";
        if (nameOfTheFileWithDot.indexOf(".") > 0)
            nameOfTheFile = nameOfTheFileWithDot.substring(0, nameOfTheFileWithDot.lastIndexOf("."));

        String format = convertPdf(nameOfTheFile, operation,queueUrl);
        if (format==null)
            return;
        //▪ Upload the resulting output file to S3.
        try {
            lib.createAndUploadS3Bucket(workersResultsBucketName, workersResultsKey+nameOfTheFile, new File(convertedFolder+"\\"+nameOfTheFile + format));
        }catch (Exception e){
            sendErrorMsg(queueUrl,e.getMessage());
            return;
        }
        //▪ Put a message in an SQS queue indicating the original URL of the PDF, the S3 url of the new
        // image file, and the operation that was performed.
        lib.sqsSendMessage(queueUrl, "out\t"+outputQueue + "\t" + pdfUrlInputFile + "\t"
                + workersResultsBucketName + "\t" + workersResultsKey+nameOfTheFile +"\t" + operation);
        //▪ remove the processed message from the SQS queue.
        lib.sqsDeleteMessage(queueUrl, message);
    }

    public static int randomIntFromInterval(int min, int max) { // min and max included
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
    }


    public static void downloadPdf(String url, String destinationPath, String queueUrl){
        String[] arrSplit=url.split("/",30); // 30 is arbitrary
        String name = arrSplit[arrSplit.length-1];
        try{
            FileUtils.copyURLToFile(new URL(url), new File(destinationPath+"\\"+name));
        } catch (IOException e) {
            //send a message to the manager
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
        lib.sqsSendMessage(queueUrl,"out\t"+outputQueue+
                '\t'+pdfUrlInputFile+'\t'+"error"+'\t'+errorMsg+'\t'+operation);
        lib.sqsDeleteMessage(queueUrl, startMessage);
    }

}

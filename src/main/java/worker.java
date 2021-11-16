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

import org.apache.pdfbox.tools.PDFText2HTML;




public class worker {
    public static void main(String[] args){
        //downloadPdf("");
        convertPdf("assignment1","..","ToText");
    }

    public static String downloadPdf(String url) {
        String[] arrSplit=url.split("/",1);
        String name = arrSplit[arrSplit.length-1];
//      int CONNECT_TIMEOUT= 1000;
//      int READ_TIMEOUT =1000;
        try{
            FileUtils.copyURLToFile(new URL(url), new File(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; //source path name
    }

    public static void convertPdf(String fileName,String sourceFolder,String action){

        // load pdf to an object in order to convert it
        File file = new File(sourceFolder+"\\"+fileName+".pdf");
        PDDocument document = null;
        try {
            document = Loader.loadPDF(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PDDocument toConvert= new PDDocument();
        toConvert.addPage(document.getPage(1));
        //switch case
            switch (action) {
                case "ToImage":
                    convertPdfToImage(fileName,"","..",toConvert);
                    break;
                case "ToHTML":
                    convertPdfToHtml(fileName,"","..",toConvert);
                    break;
                case "ToText":
                    convertPdfToText(fileName,"","..",toConvert);
                    break;
                default:
                    throw new IllegalArgumentException();
            }

    }





    public static void convertPdfToText(String fileName, String sourcePath , String destinationPath, PDDocument document){

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
            e.printStackTrace();
        }
    }


    public static void convertPdfToHtml(String fileName,String sourceFolder ,String destinationFolder,PDDocument document){
        try{
            PDFText2HTML parser = new PDFText2HTML();
            Writer output = new PrintWriter(destinationFolder+"\\"+fileName+".html", "utf-8");
            parser.writeText(document, output);
            output.close();
            if (document != null) {
                document.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public static void convertPdfToImage(String fileName,String sourceFolder ,String destinationFolder,PDDocument document){
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int page = 0;
        BufferedImage bim = null;
        try {
            bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ImageIOUtil.writeImage(bim, fileName + ".png", 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;

public class worker {
    public static void main(String[] args) {
        convertPdf("", "");
    }

    public static int convertPdf(String url, String action) {
        String[] arrSplit = url.split("/", 1);
        String name = arrSplit[arrSplit.length - 1];
        int CONNECT_TIMEOUT = 1000;
        int READ_TIMEOUT = 1000;
        try {
            FileUtils.copyURLToFile(
                    new URL(url),
                    new File(name),
                    CONNECT_TIMEOUT,
                    READ_TIMEOUT);
        } catch () {


            switch (action) {
                case "ToImage":
                    break;
                case "ToHTML":
                    break;
                case "ToText":
                    convertPdfToText(file);
            }
            return 0;
        }
    }

    public static int convertPdfToText(String fileName){
        try{
            File file = new File(fileName);
            PDDocument document = PDDocument.load(file);
            //Instantiate PDFTextStripper class
            PDFTextStripper pdfStripper = new PDFTextStripper();

            //Retrieving text from PDF document
            String text = pdfStripper.getText(document);
            System.out.println(text);

            //Closing the document
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return 0;
    }
    private void generateHTMLFromPDF(String filename) {
        PDDocument pdf = PDDocument.load(new File(filename));
        Writer output = new PrintWriter("src/output/pdf.html", "utf-8");
        new PDFDomTree().writeText(pdf, output);

        output.close();
    }
    

}

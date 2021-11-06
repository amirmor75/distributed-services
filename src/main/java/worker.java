import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class worker {
    public static void main(String[] args) {

        convertPdfToImage("amir.pdf","C:\\Users\\Amir\\Desktop\\distributed operating systems 1\\amir.pdf");
    }

/*
    public static int convertPdf(String url,String action){
        String[] arrSplit=url.split("/",1);
        String name = arrSplit[arrSplit.length-1];
        int CONNECT_TIMEOUT= 1000;
        int READ_TIMEOUT =1000;
        try{
            FileUtils.copyURLToFile(
                    new URL(url),
                    new File(name),
                    CONNECT_TIMEOUT,
                    READ_TIMEOUT);
        } catch () {
        }
        // load pdf to an object in order to convert it
        File file = new File(pathname);
        PDDocument document = null;
        try {
            document = Loader.loadPDF(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //PDDocument document = PDDocument.load(new File(pdfFilename));
        PDFRenderer pdfRenderer = new PDFRenderer(document);




        //switch case
            switch (action) {
                case "ToImage":
                    break;
                case "ToHTML":
                    break;
                case "ToText":
                    convertPdfToText(file);
            }

    }
        return 0;
    }
    */


    public static int convertPdfToText(PDDocument document){
        try{

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


//    private void convertPdfToHtml(PDDocument pdf) {
//        Writer output = new PrintWriter("src/output/pdf.html", "utf-8");
//        new PDFDomTree().writeText(pdf, output);
//        output.close();
//    }

    public static void convertPdfToImage(String pdfFilename, String pathname){
        File file = new File(pathname);
        PDDocument document = null;
        try {
            document = Loader.loadPDF(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int page = 0;
        BufferedImage bim = null;
        try {
            bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ImageIOUtil.writeImage(bim, pdfFilename + "-" + (page+1) + ".png", 300);
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

import org.apache.commons.io.FileUtils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import javax.swing.text.Document;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class worker {
    public static void main(String[] args) {
        convertPdfToImage("","");
    }

   /* public static int convertPdf(String url,String action){
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
        }catch (){


        switch (action)
        {
            case "ToImage":
                convertPdfToImage(url);
                break;
            case "ToHTML":
                convertPdfToHtml(url);
                break;
            case "ToText":
                convertPdfToText(url);
                break;
        }
        return 0;
    }
}*/

    private static void convertPdfToHtml(String url) {
    }

    public static void convertPdfToImage(String pdfFilename, String pathname){
        File file = new File(pathname);
        PDDocument document = null;
        try {
            document = Loader.loadPDF(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //PDDocument document = PDDocument.load(new File(pdfFilename));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int page = 1;
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

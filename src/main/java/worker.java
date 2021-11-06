import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class worker {
    public static void main(String[] args) {
        convertPdf("","");
    }

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
        }catch (){


        switch (action)
        {
            case "ToImage":
                break;
            case "ToHTML":
                break;
            case "ToText":
                break;
        }
        return 0;
    }
}

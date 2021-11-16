//import javafx.util.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class LocalApplication {

    public static void main(String[] args) throws IOException {
        parseInputFile("input-sample-1.txt");
    }


    public static void parseInputFile(String fileName) throws IOException {
        LinkedList<MessageOperationUrl> listOperationUrl = new LinkedList<>();
        Map<String, String> map = new HashMap<String, String>();
        String splitarray[];
        MessageOperationUrl messageOperationUrl;
        BufferedReader readbuffer = new BufferedReader(new FileReader(fileName));
        String strRead;
        String operation;
        String pdfUrl;
        while ((strRead=readbuffer.readLine())!=null){
            splitarray = strRead.split("\t");
            operation = splitarray[0];
            pdfUrl = splitarray[1];
            messageOperationUrl = new MessageOperationUrl(operation,pdfUrl);
            listOperationUrl.add(messageOperationUrl);
            map.put(pdfUrl,operation);
        }
        //System.out.println(map);
        System.out.println(map.size());
        System.out.println(listOperationUrl.size());
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodetest;

import com.barchart.udt.ExceptionUDT;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 杨德中
 */
public class NodeTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
//        String server_host = "172.16.0.215";
        String server_host = "119.29.61.231";
//        String server_host = "127.0.0.1";

        String s = "", temp = "";
        for(int i = 0; i < 1024; ++i)
            temp = temp + "h";
        for(int i = 0; i < 765; ++i)
            s = s + temp;

//        String server_host = "10.70.0.201";
        int server_port = 23333;
        Node client = null;
        Request obj = new Request(1, 1, "2333");
        obj.setShard(s.getBytes());
        try {
            try {
                client = new Node("test", server_host, server_port);
            } catch (ExceptionUDT | PackException ex) {
                Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NodeException ex) {
            Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        int count = 0;
        while(count < 1)
        {
            try 
            {
                client.transfer_message(obj, "test");
                ++count;
            }
            catch (Exception e) 
            {
            //    e.printStackTrace();
            }
        }
    
/*
        Request obj = new Request(1, 1, "2333");
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream so = null;
        try 
        {
            so = new ObjectOutputStream(bo);
            so.writeObject(obj);
            so.flush();
            so.close();
            bo.close();
	} 
        catch (IOException ex) 
        {
            Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
	String str = new String(bo.toByteArray(), Charset.forName("ISO-8859-1"));
	System.out.println(str);	
        ObjectInputStream oin = null;
        try 
        {
            //得到完整内容
            oin = new ObjectInputStream(new ByteArrayInputStream(str.getBytes(Charset.forName("ISO-8859-1"))));
            obj = (Request)oin.readObject();
            System.out.println(obj.getRequestType() + "\n" + obj.getRequestMethod() + "\n" + obj.getReplyTo());
        } 
        catch (IOException ex) 
        {
//            Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("get object failed...");
        } 
        catch (ClassNotFoundException ex) 
        {
            Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }    
    */
    }
}

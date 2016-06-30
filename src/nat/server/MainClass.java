/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author 杨德中
 */
public class MainClass 
{
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // TODO code application logic here
        int threadNum = 10;
//        String host = "10.0.0.4";
        String host = "10.104.13.233";
//        String host = "127.0.0.1";
//        String host = "10.70.0.201";
        int port = 23333;
        MultiServer mt = new MultiServer(threadNum, host, port, true);
        mt.start();
//        System.out.println(InetAddress.getLocalHost());

/*
        String s = "{\"No\":\"0\",\"Content\":\"?í\\u0000\\u0005t\\u0001?hhhhhhhhhhhhhh\",\"type\":\"Data\"}";
        String temp = "{\"No\":\"0\",\"Content\":\"?í\\u0000\\u0005t\\u0001?hhhhhhhhhhhhhh\",\"type\":\"Data\"}" + s;
        Matcher m = Pattern.compile("\\{\"No\":\"\\d{1,3}\",\"Content\":\"\\S+?\",\"type\":\"Data\"}").matcher(temp);
        while(m.find())
            System.out.println(m.group());
*/
/*
        List<Integer> il = new ArrayList<>();
        il.add(1);
        il.add(2);
        System.out.println(il.get(1));
*/
/*
        String s = "{\"\"}{\"\"}{\"\"}";
        String st [] = s.split("\"}");
        for(String temp : st)
        System.out.println(temp);
*/
/*
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
	ObjectOutputStream so = new ObjectOutputStream(bo);
	so.writeObject("test...");
	so.flush();
	so.close();
	bo.close();
        byte [] arr = bo.toByteArray();
        ByteArrayInputStream bi = new ByteArrayInputStream(arr);
        ObjectInputStream in = new ObjectInputStream(bi);
        String test = in.readObject().toString();
        System.out.println(test);
*/
/*
        SocketUDT sock = new SocketUDT(TypeUDT.STREAM);
        sock.connect(new InetSocketAddress("127.0.0.1", 6666));
        File f = new File("C:\\Users\\ydz\\Desktop\\1.pdf");
        sock.sendFile(f, 0, f.length());
        System.out.println(f.length());
*/
/*
        SocketUDT sock1 = new SocketUDT(TypeUDT.STREAM);
        sock1.bind(new InetSocketAddress("127.0.0.1", 6666));
        sock1.listen(5);
        SocketUDT sock2 = sock1.accept();
        File f1 = new File("test.pdf");
        if(!f1.exists())
            f1.createNewFile();
        sock2.receiveFile(f1, 0, 1480338);
*/
        
/*
        String local = "10.104.13.233";
        int port = 23333;
        
        SocketUDT sock1 = new SocketUDT(TypeUDT.STREAM);
        sock1.bind(new InetSocketAddress(local, port));
        sock1.listen(5);
        SocketUDT sock = sock1.accept();
        byte [] arr = new byte[1024 * 128];
        sock.receive(arr);
*/
//        File f = new File("C:\\Users\\ydz\\Desktop\\23333333.txt");
//        FileOutputStream out = new FileOutputStream(f);
//        out.write(arr);
/*        
        DatagramSocket ds = new DatagramSocket(6666);
        String send = "test";
        DatagramPacket dp = new DatagramPacket(send.getBytes(), send.length(), new InetSocketAddress("218.22.21.23", 23333));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        ds.send(dp);
        System.out.println("send...");
        ds.receive(dp);
        System.out.println(dp);
*/    }
    
}

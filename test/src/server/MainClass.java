/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        int threadNum = 10;
//        String host = "10.0.0.4";
        String host = "10.104.13.233";
//        String host = "127.0.0.1";
        int port = 23333;
        MultiServer mt = new MultiServer(threadNum, host, port, true);
        mt.start();
//        System.out.println(InetAddress.getLocalHost());


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

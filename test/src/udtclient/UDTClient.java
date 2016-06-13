/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udtclient;
import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.MonitorUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import com.barchart.udt.net.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 杨德中
 */
public class UDTClient 
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ExceptionUDT, IOException, PackException 
    {
        // TODO code application logic here
    /*    
        String host = "127.0.0.1";
        int port = 6666;
        Gson gson = new Gson();
        Type JSON_TYPE = new TypeToken<Map<String, String>>(){}.getType();
        SocketUDT socket = new NetSocketUDT().socketUDT();
        socket.bind(new InetSocketAddress(host, 23333));
        socket.connect(new InetSocketAddress(host, port));

        //sock.close();
        System.out.println("first bind success");
        SocketUDT sock = new SocketUDT(TypeUDT.STREAM);
        sock.bind(new InetSocketAddress(host, 23333));
        System.out.println("second bind success");
        
        sock.connect(new InetSocketAddress(host, port));
        
        Date d1 = new Date();
        
        Map<String, String> info = new HashMap<>();
        info.put("type", "NodeI");
        info.put("type_d", "01");
        info.put("Insertion", "Insertion");
        byte arr[] = new byte[1024];
        
        sock.send(gson.toJson(info).getBytes(Charset.forName("ISO-8859-1")));
        
        sock.receive(arr);
        System.out.println(new String(arr));
        Map<String, String> get_ID = gson.fromJson((new String(arr)).trim(), JSON_TYPE);
        
        info.put("type_d", "03");
        info.remove("Insertion");
        info.put("UName", "test");
        
        sock.receive(arr);
        
        System.out.println(new String(arr));
        sock.close();
        Date d2 = new Date();
        System.out.println(d2.getTime() - d1.getTime());
    */
    
//        String server_host = "172.16.0.215";
        String server_host = "119.29.61.231";
//        String server_host = "127.0.0.1";
        int server_port = 23333;
        try {
            Node client = new Node("test", server_host, server_port);
        } catch (NodeException ex) {
            Logger.getLogger(UDTClient.class.getName()).log(Level.SEVERE, null, ex);
        }

/*
        SocketUDT sock = new SocketUDT(TypeUDT.STREAM);
        sock.connect(new InetSocketAddress(server_host, server_port));
        System.out.println(sock.getLocalSocketAddress());
*/
//        System.out.println(InetAddress.getLocalHost().getHostAddress());
/*    
        String to = "218.22.21.23";
        String local = "114.214.179.151";
        int port1 = 6666, port2 = 23333;
        SocketUDT sock2 = new SocketUDT(TypeUDT.STREAM);
        myMonitor monitor = new myMonitor(sock2);
//        sock2.bind(new InetSocketAddress(local, port2));
//        sock2.listen(10);
//        sock2.setRendezvous(true);
//        while(!sock2.isConnected())
//        {
        sock2.connect(new InetSocketAddress(to, port1));
//        }
//        byte []arr = new byte[1024];
//        sock2.receive(arr);
//        System.out.println(new String(arr));
        if(sock2.isConnected())
            sock2.send(" ".getBytes());
//        SocketUDT sock = sock2.accept();
        System.out.println(sock2.isConnected());
        System.out.println("lost rate:" + monitor.get_lostRate() * 100 + "%\nbandwidth:" + monitor.get_mbpsBandwidth() + "mbps\nRTT:" + monitor.get_msRTT() + "ms");
        System.out.println(sock2.getRemoteInetAddress().toString() + sock2.getRemoteInetPort());
*/    
/*      DatagramSocket ds = new DatagramSocket(23333);
        String send = "test";
        DatagramPacket dp = new DatagramPacket(send.getBytes(), send.length(), new InetSocketAddress("218.22.21.23", 6666));
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

class myMonitor extends MonitorUDT
{
    public myMonitor(SocketUDT sock)
    {
        super(sock);
    }
    
    public double get_mbpsBandwidth()
    {
        return this.mbpsBandwidth;
    }
    
    public double get_msRTT()
    {
        return this.msRTT;
    }
    
    public double get_lostRate()
    {
        return (double)(this.pktRecvTotal + this.pktSndLossTotal) / (this.pktRecvTotal + this.pktSentTotal);
    }
    
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodetest;

/**
 *
 * @author 杨德中
 */

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import com.barchart.udt.net.NetInputStreamUDT;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.nashorn.internal.parser.TokenType;

/**
 *
 * @author 杨德中
 */
public class NodeLink implements Runnable
{
    private final Node node;
    private final String ID_p;
    private final SocketUDT socket;
    private NetInputStreamUDT in;
    /**
      * @param nodeID_peer
      * @param socket
      * @param node
    */
    public NodeLink(String ID_p, SocketUDT socket, Node node) 
    {
        this.ID_p = ID_p;
        this.socket = socket;
        this.node = node;
        this.in = new NetInputStreamUDT(socket);
    }
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        Map<String, String> pac;
        List<String> path = new ArrayList<>();
        byte [] recv = new byte[4096];
        byte[] arr;
        String str;
        List<String> packageSend = null;
        try
        {
            while (!Thread.currentThread().isInterrupted()) 
            {
                try
                {
                    //得到路由包
//                    socket.receive(recv);
                    in.read(recv);
                }
                catch (ExceptionUDT ex) 
                {
                //    Logger.getLogger(NodeLink.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(NodeLink.class.getName()).log(Level.SEVERE, null, ex);
                }
                str = new String(recv, Charset.forName("ISO-8859-1")).trim();
                try
                {
                    Node.empty_arr(str.length(), recv);
                    pac = Packer.unpack(str);
//                    System.out.println("NodeLink:" + str);
                    if(pac.get("type").trim().equals("HEARTBEAT"))
                        return;
                    int pack_cnt = Integer.parseInt(pac.get("PackCnt").trim());
                    arr = new byte[pack_cnt * 70 * 1024];
                    String From = pac.get("From");
                    String To = pac.get("To");
                    String No = pac.get("No");
                    int Len = Integer.parseInt(pac.get("Len")); 
                    int packCntOriginal = Integer.parseInt(pac.get("Cnt").trim());
                    int temp = 0, packTemp = 0;
                    Timer timer = new Timer(3000);
                    try 
                    {
                        while(temp < Len)
                        {
                            in.read(arr, temp, 16 * 1024);
                            temp += 16 * 1024;
                            try 
                            {
                                Thread.sleep(500);
                            } 
                            catch (Exception e) 
                            {
                            }
                        }
                        in.close();
                    }
                    catch (ExceptionUDT e) 
                    {
                        //e.printStackTrace();
                        //收到的包不全，请求Server重发
                        System.out.println("not complete...");
                        Map<String, String> info = new HashMap<>();
                        info.put("ID", From);
                        info.put("ID_target", To);
                        info.put("No", No);
                        info.put("Cnt", packCntOriginal + "");
                        SocketUDT sock;
                        try 
                        {
                            sock = new SocketUDT(TypeUDT.STREAM);
                            sock.connect(new InetSocketAddress(node.server_host, node.server_port));
                            sock.send(Packer.pack("DataR", info).getBytes(Charset.forName("ISO-8859-1")));
                        } 
                        catch (ExceptionUDT | PackException ex) 
                        {
                            Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                    } 
                    catch (IOException ex) 
                    {
                        Logger.getLogger(NodeLink.class.getName()).log(Level.SEVERE, null, ex);
                    }                            
                    System.out.println(temp);
                    int beginNo = Integer.parseInt(pac.get("NoBeg").trim());
                    int nodeCnt = Integer.parseInt(pac.get("HopCnt").trim());
                    for(int i = 2; i <= nodeCnt; ++i)
                        path.add(pac.get("Hop_" + i).trim());
                    String totalStr = new String(arr, Charset.forName("ISO-8859-1")).trim();
                    String pack[] = totalStr.split("}");
                    System.out.println(pack_cnt + "\n" + pack.length);
                    if(pack.length < pack_cnt)
                    {
                        System.out.println("not complete...");
                        Map<String, String> info = new HashMap<>();
                        info.put("ID", From);
                        info.put("ID_target", To);
                        info.put("No", No);
                        info.put("Cnt", packCntOriginal + "");
                        SocketUDT sock;
                        try 
                        {
                            sock = new SocketUDT(TypeUDT.STREAM);
                            sock.connect(new InetSocketAddress(node.server_host, node.server_port));
                            sock.send(Packer.pack("DataR", info).getBytes(Charset.forName("ISO-8859-1")));
                        } 
                        catch (ExceptionUDT | PackException ex) 
                        {
                            Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                    }
                    for(int i = 0; i < pack_cnt; ++i)
                        pack[i] = pack[i] + "}";
                    if(Integer.parseInt(pac.get("HopCnt")) == 1)
                    {
                        //此处为终点节点
                        System.out.println("终点...");
                        if((!node.data_receiver.containsKey(Integer.parseInt(No))) || node.data_receiver.get(Integer.parseInt(No)) == null)
                        {
                            DataReceiver tempDR = new DataReceiver(From, packCntOriginal, Integer.parseInt(No), this.node);
                            Thread iThread = new Thread(tempDR);
                            iThread.start();
                            node.data_receiver.put(Integer.parseInt(No), tempDR);
                        }
                        for(int i = 0; i < pack_cnt; ++i)
                        {
                            pac = Packer.unpack(pack[i]);
                            node.data_receiver.get(Integer.parseInt(No)).pack.put(Integer.parseInt(pac.get("No").trim()), pac.get("Content").trim());
                        }
                        //xq method调用
                        Map<String, String> informServerMap = new HashMap<>();
                        informServerMap.put("type", "DataF");
                        informServerMap.put("From", From);
                        informServerMap.put("To", To);
                        informServerMap.put("No", No);
                        SocketUDT sockInform = null;
                        try 
                        {
                            sockInform = new SocketUDT(TypeUDT.STREAM);
                            sockInform.connect(new InetSocketAddress(node.server_host, node.server_port));
                            sockInform.send((new Gson()).toJson(informServerMap).getBytes(Charset.forName("ISO-8859-1")));
                        } 
                        catch (ExceptionUDT e) 
                        {
                            System.out.println("Inform server failed...");
                        }
                        finally
                        {
                            try 
                            {
                                sockInform.close();
                            } 
                            catch (NullPointerException e) 
                            {
                                System.out.println("sockInform hava been closed...");
                            }
                        }
                    }
                    else
                    {       
                        //路由包
                        Map<String, String> sendNext = new HashMap<>();
                        sendNext.put("From", From);
                        sendNext.put("To", To);
                        sendNext.put("No", No);
                        sendNext.put("Cnt", packCntOriginal + "");
                        sendNext.put("NoBeg", beginNo + "");
                        sendNext.put("HopCnt", "" + (nodeCnt - 1));
                        sendNext.put("PackCnt", pack_cnt + "");
                        for (int i = 1; i < nodeCnt; ++i)
                            sendNext.put("Hop_" + i, path.get(i));
                        packageSend = new ArrayList<>();
                        packageSend.add(Packer.pack("RoutD", "02", sendNext));
//                        for (int i = 0; i < pack_cnt; ++i)
//                            packageSend.add(new String(arr[i], Charset.forName("ISO-8859-1")).trim());
                        for(int i = 0; i < pack_cnt; ++i)
                        {
                            packageSend.add(pack[i]);
                        }
                        DataSender2.Sender(this.node, this.ID_p, packageSend);
                    }
                }
                catch (NullPointerException e)
                {
                    //由line77触发
//                    System.out.println("not for transfer...");
                }
                catch (PackException e) 
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }            
        catch (ExceptionUDT e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            while(true)
            {
                try 
                {
                    Thread.sleep(1000);
                    DataSender2.Sender(this.node, this.ID_p, packageSend);
                    break;
                } 
                catch (InterruptedException | ExceptionUDT ex) 
                {
                    //重发
                }
            }
        } 
        finally 
        {
            try 
            {
                socket.close();
            } 
            catch (ExceptionUDT e) 
            {
                // TODO Auto-generated catch block
                //socket已关闭...
                e.printStackTrace();
            }
        }
    }
}

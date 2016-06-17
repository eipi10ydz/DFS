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
import com.google.gson.Gson;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 杨德中
 */
public class NodeLink implements Runnable
{
    private final Node node;
    private final String ID_p;
    private final SocketUDT socket;
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
        byte [] recv = new byte[1024];
        byte[][] arr;
        String str;
        List<String> packageSend = null;
        try
        {
            while (!Thread.currentThread().isInterrupted()) 
            {
                try
                {
                    //得到路由包
                    socket.receive(recv);
                }
                catch (ExceptionUDT ex) 
                {
                //    Logger.getLogger(NodeLink.class.getName()).log(Level.SEVERE, null, ex);
                }
                str = new String(recv, Charset.forName("ISO-8859-1")).trim();
                try
                {
                    node.empty_arr(str.length(), recv);
                    pac = Packer.unpack(str);
                    if(pac.get("type").trim().equals("HEARTBEAT"))
                        return;
                    int pack_cnt = Integer.parseInt(pac.get("PackCnt").trim());
                    arr = new byte[pack_cnt][1024];
                    for(int i = 0; i < pack_cnt; ++i)
                    {
                        try 
                        {
                            socket.receive(arr[i]);
                        } 
                        catch (ExceptionUDT e) 
                        {
                            //收到的包不全，是否请求Server重发
                        }                            
                    }
                    int packCntOriginal = Integer.parseInt(pac.get("Cnt").trim());
                    int beginNo = Integer.parseInt(pac.get("NoBeg").trim());
                    int nodeCnt = Integer.parseInt(pac.get("HopCnt").trim());
                    for(int i = 2; i <= nodeCnt; ++i)
                        path.add(pac.get("Hop_" + i).trim());
                    String From = pac.get("From");
                    String To = pac.get("To");
                    String No = pac.get("No");
                    if(pac.get("HopCnt").trim().equals("0"))
                    {
                        //此处为终点节点
                        String content = "";
                        String [] pack = new String[pack_cnt];
                        if((!node.dataReceiver.containsKey(Integer.parseInt(No))) || node.dataReceiver.get(Integer.parseInt(No)) == null)
                        {
                            node.dataReceiver.put(Integer.parseInt(No), new DataReceiver(From, packCntOriginal, Integer.parseInt(No), this.node));
                        }    
                        for(int i = 0; i < pack_cnt; ++i)
                        {
                            pac = Packer.unpack(new String(arr[i], Charset.forName("ISO-8859-1")).trim());
                            node.dataReceiver.get(Integer.parseInt(No)).pack.put(Integer.parseInt(pac.get("No").trim()), pac.get("Content").trim());
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
                        sendNext.put("type_d", "02");
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
                        packageSend.add(Packer.pack("RoutD", sendNext));
                        for (int i = 0; i < pack_cnt; ++i)
                            packageSend.add(new String(arr[i], Charset.forName("ISO-8859-1")).trim());
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

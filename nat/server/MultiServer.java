import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.net.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import static java.lang.Integer.parseInt;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author 杨德中, 苏潇
 */

public class MultiServer
{
    int threadNum = 10;
    public MultiServer(int threadNum)
    {
        this.threadNum = threadNum;
    }
    
    public void start() throws IOException
    {
        MultiServerImplementation mt = new MultiServerImplementation();
        ArrayList<Thread> threads = new ArrayList<>();
        for(int i = 0; i < threadNum; ++i)
        {
            Thread iThread = new Thread(mt);
            iThread.start();
            threads.add(iThread);
        }
        threads.stream().forEach((iThread) -> {
            try
            {
                iThread.join();
            }
            catch (InterruptedException e) 
            {
            }
        });
    }
}

class MultiServerImplementation implements Runnable
{
    Map<Client, List<Client>> route_table = new HashMap<>();
    Map<String, HashMap.Entry<Client, Client>> keyMap = new HashMap<>();
    Map<HashMap.Entry<Client,Client>, String> record = new HashMap<>();
    long clientNum = 0;
    SocketUDT serverSocket;
    Gson gson_fromJson;
    Gson gson_toJson;
    boolean check_on = false;
    Lock lock = new ReentrantLock();
    Date early = null;
    Type JSON_TYPE = new TypeToken<Map<String, String>>(){}.getType();
    
    final String host = "127.0.0.1";
    final int port = 6666;
    final int CLIENTNUM = 10;
    final String NAT_TYPE = "TNAT";
    
    public MultiServerImplementation() throws IOException 
    {
        this.gson_fromJson = new Gson();
        this.gson_toJson = new GsonBuilder().create();
        this.serverSocket = new NetServerSocketUDT().socketUDT();
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.listen(CLIENTNUM);
        early = new Date();
        log("Waiting for connection...");
    }
    
    public void dual_with(SocketUDT sock) throws ExceptionUDT
    {
        byte arrRecv[] = new byte[1024];
        log("Accept new connection ");
        sock.receive(arrRecv);
        Map<String, String> info;
        info = this.gson_fromJson.fromJson((new String(arrRecv)).trim(), this.JSON_TYPE);
        switch(info.get("type").trim())
        {
            case "LinkE":
            {
                switch(info.get("type_d").trim())
                {
                    case "01":
                    //    key_send(info, sock);
                        nat_request(info, sock);
                        break;
                }
            }
            break;
            case "LinkC":
            {
                record_info(info, sock);
            }
            break;
            case "Regis":
            {
                switch(info.get("type_d").trim())
                {
                    case "01":
                        register(info, sock);
                }
            }
            break;
        }
    }
    
    public void register(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        Client client = null;
        lock.lock();
        try 
        {
            client = new Client(info.get("username"), this.clientNum++, sock.getRemoteInetAddress(), sock.getRemoteInetPort());
        }
        finally 
        {
            lock.unlock();
        }
        Client newClient = client;
        //需要返回上线用户的ID和username
        //注意不要搞一模一样的用户名，会覆盖
        Map<String, String> infoSend = new HashMap<>();
        infoSend.put("type", "NodeI");
        infoSend.put("type_d", "01");
        infoSend.put("ID", client.ID);
        sock.send(gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1")));
        infoSend.put("type", "NodeT");
        infoSend.remove("type_d");
        Map<String, String> informInfo = new HashMap<>();
        informInfo.put("type", "NodeI");
        informInfo.put("type_d", "02");
        informInfo.put("ID", client.ID);
        int clientNum = 0;
        for (Iterator<Client> it = route_table.keySet().iterator(); it.hasNext();) 
        {
            client = it.next();
            try 
            {
                SocketUDT sockInform = new NetSocketUDT().socketUDT();
                sockInform.connect(new InetSocketAddress(client.IP, parseInt(client.port)));                
                sockInform.send(gson_toJson.toJson(informInfo).getBytes(Charset.forName("ISO-8859-1")));
                infoSend.put("UName_" + (clientNum + 1), client.userName);
                infoSend.put("ID_" + (++clientNum), client.ID);
            }
            catch (ExceptionUDT e) 
            {
                log("cannot connect...");
            }
            log(client.toString());
        }
        this.route_table.put(newClient, null);
        infoSend.put("cnt", clientNum + "");
        sock.send(gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1")));
        sock.close();
    }
    //两个重载的成员函数，用于两种方式查找client
    private Client find_client(String destination)
    {
        Client client_to = null;
        boolean found = false;
        for (Iterator<Client> it = this.route_table.keySet().iterator(); it.hasNext();) 
        {
            client_to = it.next();
            if(client_to.ID.equals(destination.trim()))
            {
                found = true;
                break;
            }
        }
        return found ? client_to : null;
    }
    
    private Client find_client(String IP_from, String port_from)
    {
        Client client_from = null;
        boolean found = false;
        for (Iterator<Client> it = this.route_table.keySet().iterator(); it.hasNext();) 
        {
            client_from = it.next();
            if(client_from.IP.equals(IP_from) && client_from.port.equals(port_from))
            {
                found = true;
                break;
            }
        }
        return found ? client_from : null;
    }
    
    public void check_online() throws InterruptedException, ExceptionUDT, IOException
    {
        Date later;
        while(true)
        {
            later = new Date();
            if(later.getTime() - this.early.getTime() >= 1000 * 60 * 5) //毫秒级(此处为5分钟)
            {
                this.early = later;
                check_implementation();
            }
        }
    }
    //没加锁，可能会出问题
    //暂时不管keyMap，因为暂时没用...
    private void check_implementation() throws ExceptionUDT
    {
        Client client;
        SocketUDT sock;
        Map<String, String> info = new HashMap<>();
        info.put("type", "NodeD");
        //需要完善检测掉线的包结构
        Iterator it = this.route_table.keySet().iterator();
        while(it.hasNext())
        {
            client = (Client)it.next();
            sock = new NetSocketUDT().socketUDT(); //如果抛出异常就是创建socket的锅
            try
            {
                sock.connect(new InetSocketAddress(client.IP, parseInt(client.port)));
            }
            catch(ExceptionUDT ex)
            {
                log("cannot connect...check failed..." + client.toString());
                sock.close();
                List<Client> list = this.route_table.get(client);
                for (Iterator<Client> pointer = list.iterator(); it.hasNext();) 
                {
                    Client temp = pointer.next();
                    this.route_table.get(temp).remove(client);
                }
                info.put("IP", client.IP);
                for (Client next : this.route_table.keySet())
                {
                    if(!next.equals(client))
                    {
                        SocketUDT sockInform = new NetSocketUDT().socketUDT();
                        try
                        {
                            sockInform.connect(new InetSocketAddress(next.IP, parseInt(next.port)));
                            sockInform.send(gson_toJson.toJson(info).getBytes(Charset.forName("ISO-8859-1")));
                        }
                        catch (NumberFormatException | ExceptionUDT e) 
                        {
                            log("cannot connect...");
                        }
                    }
                }
                remove_client(client);
                it.remove();
            }
        }
    }
    
    private void remove_client(Client client) //只是从record中删掉关联项
    {
        Iterator pointer = this.record.keySet().iterator();
        while(pointer.hasNext())
        {
            HashMap.Entry<Client,Client> pair = (Map.Entry<Client, Client>)pointer.next();
            if(pair.getKey().equals(client) || pair.getValue().equals(client))
                pointer.remove();
        }
    }
    
    public void nat_request(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        String destination = info.get("ID");
        String IP_from = sock.getRemoteInetAddress().toString().split("/")[1];
        String port_from = sock.getRemoteInetPort() + "";
        Client client_from = find_client(IP_from, port_from);
        Client client_to = find_client(destination);
        Map<String, String> send_info = new HashMap<>();
        if(client_to == null || client_from == null)
        {
            log("Not online...");
        }
        else if(client_from.isConnecting || client_to.isConnecting)
        {
            log("is connecting...cannot connect to another...");
            send_info.put("type", "ERR");
            send_info.put("type_d", "01");
            sock.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
        }
        else
        {
            lock.lock();
            try 
            {
                if(client_from.isConnecting || client_to.isConnecting)
                {
                    log("is connecting...cannot connect to another...");
                    send_info.put("type", "ERR");
                    send_info.put("type_d", "01");
                    sock.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
                    return;
                }
                client_from.isConnecting = true;
                client_to.isConnecting = true;
            }
            finally 
            {
                lock.unlock();
            }
            //没问题后发包交换两节点信息，先发给to，防止connect时候失败
            SocketUDT sock_to = new NetSocketUDT().socketUDT();
            try
            {
                sock_to.connect(new InetSocketAddress(client_to.IP, parseInt(client_to.port)));
            }
            catch(ExceptionUDT ex)
            {
                //应该向client_from发送错误包
                log("cannot connect...nat_request failed...");
                return;
            }
            send_info.put("type", "LinkE");
            send_info.put("type_d", "03");
            send_info.put("IP", client_from.IP);
            send_info.put("Port", client_from.port);
            //否则向client_to发包成功
            sock_to.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
            sock_to.close();
            send_info.replace("IP", client_to.IP);
            send_info.replace("Port", client_to.port);
            sock.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
        }
        sock.close();
    }
    
    public void key_send(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        String destination = info.get("ID");
        String IP_from = sock.getRemoteInetAddress().toString().split("/")[1];
        String port_from = sock.getRemoteInetPort() + "";
        Client client_from = find_client(IP_from, port_from);
        Client client_to = find_client(destination);
        if(client_to == null || client_from == null)
        {
            log("Not online...");
        }
        else
        {
            int temp = (client_to.userName + client_from.userName).hashCode();
            if(temp < 0)
                temp = -temp;
            String key = temp + "";
            Map<String, String> send_info = new HashMap<>();
            send_info.put("type", "LinkE");
            send_info.put("type_d", "02");
            send_info.put("ID", client_from.ID);
            send_info.put("key", key);
            SocketUDT sock_to = new NetSocketUDT().socketUDT();
            try
            {
                sock_to.connect(new InetSocketAddress(client_to.IP, parseInt(client_to.port)));
            }
            catch(ExceptionUDT ex)
            {
                //应该向client_from发送错误包
                log("cannot connect...nat_request failed...");
                return;
            }
            sock_to.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
            sock_to.close();
            send_info.replace("ID", client_to.ID);
            sock.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
            Map.Entry<Client, Client> pair = new AbstractMap.SimpleEntry<>(client_from, client_to);
            this.keyMap.put(key, pair);
        }
        sock.close();
    }
    
    public void record_info(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        String connectivity = info.get("Connectivity");
        String IP_from = sock.getRemoteInetAddress().toString().split("/")[1];
        String port_from = sock.getRemoteInetPort() + "";
        String destination = info.get("ID");
        Client client_from = find_client(IP_from, port_from);
        Client client_to = find_client(destination);
        List<Client> connect_list;
        Map.Entry<Client, Client> pair_from;
        if(client_from == null || client_to == null)
        {
            log("Not online...");
        }
        else
        {
            pair_from = new AbstractMap.SimpleEntry<>(client_from, client_to);
            if("true".equals(connectivity.trim()))
            {
                connect_list = route_table.get(client_from);
                if(connect_list == null)
                {
                    connect_list=new ArrayList<>();
                    connect_list.add(client_to);
                    route_table.put(client_from, connect_list);
                }
                else
                {
                    if(connect_list.indexOf(client_to) == -1)
                        connect_list.add(client_to);
                }//add B to A's connectlist
                connect_list = route_table.get(client_to);
                if(connect_list == null)
                {
                    connect_list=new ArrayList<>();
                    connect_list.add(client_from);
                    route_table.put(client_to, connect_list);               
                }
                else
                {
                    if(connect_list.indexOf(client_from) == -1)
                        connect_list.add(client_from);
                }
                record.put(pair_from,"true");
            }//if connectivity=true
            else
            {
                record.put(pair_from,"false");
            }//do nothing?
            client_from.isConnecting = false;
            client_to.isConnecting = false;
        }   
    }
    
    @Override
    public void run()
    {
        SocketUDT sock;
        if(!this.check_on)  //让一个线程用来检测定时在线
        {
            lock.lock();
            try
            {
                this.check_on = true;
            }
            finally
            {
                lock.unlock();
            }
            try 
            {
                check_online();
            } 
            catch (InterruptedException | ExceptionUDT ex) 
            {
                Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        while(true)
        {
            try 
            {
                sock = this.serverSocket.accept();
                dual_with(sock);
            } 
            catch (ExceptionUDT ex) 
            {
                Logger.getLogger(MultiServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    //    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private void log(String message)
    {
        System.out.println(message);
    }
    
}

class Client
{
    String ID = null;
    String userName = null;
    String IP = null;
    String port = null;
    boolean isConnecting = false;
    public Client(String userName, long clientNum, InetAddress IP, int port)
    {
        this.userName = userName;
        this.ID = String.format("%05d", clientNum);
        this.IP = IP.toString().split("/")[1];
        this.port = port + "";
    }
    @Override
    public String toString()
    {
        return this.userName + "\n" + this.ID + "\n" + this.IP + "\n" + this.port;
    }
}


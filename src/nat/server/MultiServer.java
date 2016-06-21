package server;



import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
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
    private int threadNum = 10;
    private final String host;
    private final int port;
    private final boolean debug;
    public MultiServer(int threadNum, String host, int port, boolean debug)
    {
        this.threadNum = threadNum;
        this.host = host;
        this.port = port;
        this.debug = debug;
    }
    
    public void start() throws IOException
    {
        MultiServerImplementation mt = new MultiServerImplementation(this.host, this.port, this.debug);
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
    private final Map<Client, List<Client>> route_table = new HashMap<>();
    private final Map<HashMap.Entry<Client,Client>, String> record = new HashMap<>();
    private final Map<String, SocketUDT> nat_new = new HashMap<>();
    private final Floyd_implementation fi;
    private long clientNum = 0;
    private final SocketUDT serverSocket;
    private final Gson gson_fromJson;
    private final Gson gson_toJson;
    private boolean check_on = false;
    private final Lock lock = new ReentrantLock();
    private Date early = null;
    private final Type JSON_TYPE = new TypeToken<Map<String, String>>(){}.getType();
    private int mapVertices = 0;
//    private String host = "10.0.0.4";
    private String host = "10.104.13.233";
//    private String host = "127.0.0.1";
    private int port = 23333;
    private final double INFINITY = 66666666;
    private final int CLIENTNUM = 10;
    private final int MAX_CLIENT = 10;
    private final boolean debug;
    
    public MultiServerImplementation(String host, int port, boolean debug) throws IOException 
    {
        this.debug = debug;
        this.host = host;
        this.port = port;
        this.gson_fromJson = new Gson();
        this.gson_toJson = new GsonBuilder().create();
        this.serverSocket = new NetServerSocketUDT().socketUDT();
        this.fi = new Floyd_implementation(MAX_CLIENT);
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.listen(CLIENTNUM);
        early = new Date();
        log("Waiting for connection...");
    }
    
    private void dual_with(SocketUDT sock) throws ExceptionUDT
    {
        byte arrRecv[] = new byte[1024];
        sock.receive(arrRecv);
        Map<String, String> info;
        info = this.gson_fromJson.fromJson((new String(arrRecv)).trim(), this.JSON_TYPE);
        log((new String(arrRecv)).trim());
        switch(info.get("type").trim())
        {
            case "LinkE" :
            {
                switch(info.get("type_d").trim())
                {
                    case "01" :
                    //    key_send(info, sock);
                        pre_nat_request(info, sock);
                        break;
                    case "02" :
                        nat_request(info, sock);
                        break;
                    case "06" :
                        informDirectConnect(info, sock);
                        break;
                }
            }
                break;
            case "LinkC" :
            {
                record_info(info, sock);
            }
                break;
            case "NodeI" :
            {
                switch(info.get("type_d").trim())
                {
                    case "00" :
                        register(info, sock);
                        break;
                }
            }
                break;
            case "RoutQ" : 
            {
                return_route(info, sock);
                break;
            }
            case "RoutI" : 
            {
                map_edge_change(info, sock);
            }
                break;
            case "DataF" :
            {
                informSendSuccess(info, sock);
                break;
            }
            case "DataR" :
            {
                informResend(info, sock);
                break;
            }
        }
    }
    
    private void register(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        Client client;
        long temp_clientNum;
        lock.lock();
        try 
        {
            temp_clientNum = this.clientNum++;
            ++this.mapVertices;
        }
        finally 
        {
            lock.unlock();
        }
        Map<String, String> infoSend = new HashMap<>();
        infoSend.put("type", "NodeI");
        infoSend.put("type_d", "01");
        infoSend.put("ID", String.format("%05d", temp_clientNum));
        sock.send(gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1")));
        byte[] arr = new byte[1024];
        sock.receive(arr);
        info = this.gson_fromJson.fromJson((new String(arr)).trim(), this.JSON_TYPE);
        client = new Client(info.get("UName"), temp_clientNum, info.get("LIP"), sock.getRemoteInetAddress(), sock.getRemoteInetPort());
        Client newClient = client;
        //需要返回上线用户的ID和username
        //注意不要搞一模一样的用户名，会覆盖
        infoSend.put("type", "NodeT");
        infoSend.remove("type_d");
        infoSend.remove("ID");
        Map<String, String> informInfo = new HashMap<>();
        informInfo.put("type", "NodeI");
        informInfo.put("type_d", "02");
        informInfo.put("UName", client.userName);
        informInfo.put("ID", client.ID);
        informInfo.put("LIP", client.IP_local);
        int clientNumSend = 0;
        for (Iterator<Client> it = route_table.keySet().iterator(); it.hasNext();) 
        {
            client = it.next();
            try 
            {
//                SocketUDT sockInform = client.link_maintain;
                SocketUDT sockInform = new SocketUDT(TypeUDT.STREAM);
                sockInform.bind(new InetSocketAddress(host, port));
                sockInform.connect(new InetSocketAddress(client.IP_maintain, Integer.parseInt(client.port_maintain)));
                sockInform.send(gson_toJson.toJson(informInfo).getBytes(Charset.forName("ISO-8859-1")));
                infoSend.put("UName_" + (clientNumSend + 1), client.userName);
                infoSend.put("ID_" + (clientNumSend + 1), client.ID);
                infoSend.put("LIP_" + (++clientNumSend), client.IP_local);
            }
            catch (ExceptionUDT e)
            {
                log("cannot connect...");
            }
        }
        this.route_table.put(newClient, null);
        infoSend.put("cnt", clientNumSend + "");
        sock.send(gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1")));
//        sock.close();
        log(new String(gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1"))));
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
            if(client_from.IP_maintain.equals(IP_from) && client_from.port_maintain.equals(port_from))
            {
                found = true;
                break;
            }
        }
        return found ? client_from : null;
    }
    
    private void check_online() throws InterruptedException, ExceptionUDT, IOException
    {
        Date later;
        while(true)
        {
            later = new Date();
            if(later.getTime() - this.early.getTime() >= 1000 * 60 * 2) //毫秒级(此处为2分钟)
            {
                this.early = later;
                check_implementation();
            }
        }
    }
    //没加锁，可能会出问题

    private void check_implementation() throws ExceptionUDT
    {
        Client client;
        SocketUDT sock;
        Map<String, String> heartBeat = new HashMap<>();
        Map<String, String> info = new HashMap<>();
        info.put("type", "NodeD");
        heartBeat.put("type", "HEARTBEAT");
        heartBeat.put("Cardiopalmus", "Cardiopalmus");
        heartBeat.put("Palpitation", "Palpitation");
        Iterator it = this.route_table.keySet().iterator();
        while(it.hasNext())
        {
            client = (Client)it.next();
            sock = new SocketUDT(TypeUDT.STREAM); //如果抛出异常就是创建socket的锅
            //现在还不是知道要不要发包收包检测是否掉线
            sock.bind(new InetSocketAddress(this.host, this.port));
            try
            {
                sock.connect(new InetSocketAddress(client.IP_maintain, parseInt(client.port_maintain)));
                sock.send(gson_toJson.toJson(heartBeat).getBytes(Charset.forName("ISO-8859-1")));
            }
            catch(ExceptionUDT ex)
            {
                log("cannot connect...check failed..." + client.toString());
                sock.close();
                List<Client> list = this.route_table.get(client);
                for (Iterator<Client> pointer = list.iterator(); pointer.hasNext();) 
                {
                    Client temp = pointer.next();
                    this.route_table.get(temp).remove(client);
                }
                info.put("ID", client.ID);
                for (Client next : this.route_table.keySet())
                {
                    if(!next.equals(client))
                    {
                        map_edge_delete(parseInt(client.ID), parseInt(next.ID));
                        SocketUDT sockInform = new NetSocketUDT().socketUDT();
                        try
                        {
                            sockInform.bind(new InetSocketAddress(this.host, this.port));
                            sockInform.connect(new InetSocketAddress(next.IP_maintain, parseInt(next.port_maintain)));
                            sockInform.send(gson_toJson.toJson(info).getBytes(Charset.forName("ISO-8859-1")));
                        }
                        catch (NumberFormatException | ExceptionUDT e) 
                        {
                            log("cannot connect while checking connection...will be delete afterwards...");
                        }
                    }
                }
                remove_client(client);
                it.remove();
                --this.mapVertices;
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
 
    private void pre_nat_request(Map<String, String> info, SocketUDT sock)
    {
        String destination = info.get("ID_target");
        String ID_from = info.get("ID");
        String IP_from = sock.getRemoteInetAddress().getHostAddress();
        String port_from = sock.getRemoteInetPort() + "";
        Client client_from = find_client(ID_from);
        Client client_to = find_client(destination);
        Map<String, String> infoSend = new HashMap<>();
        if(client_to == null || client_from == null)
        {
            log("client not in routetable");
        }
        lock.lock();
        if(client_from.isConnecting || client_to.isConnecting)
        {
            log("is connecting...cannot connect to another...");
            infoSend.put("type", "ERR");
            infoSend.put("type_d", "01");
            try 
            {
                sock.send(this.gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1")));
            } 
            catch (ExceptionUDT ex) 
            {
                Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            client_from.isConnecting = true;
            client_to.isConnecting = true;
            lock.unlock();
        }      
        infoSend.put("type", "LinkE");
        infoSend.put("type_d", "03");
        infoSend.put("ID", ID_from);
        infoSend.put("IP", IP_from);
        infoSend.put("Port", port_from);
        nat_new.put(ID_from, sock);
        try 
        {
            SocketUDT sockTo = new SocketUDT(TypeUDT.STREAM);
            sockTo.bind(new InetSocketAddress(host, port));
            sockTo.connect(new InetSocketAddress(client_to.IP_maintain, Integer.parseInt(client_to.port_maintain)));
//            SocketUDT sockTo = client_to.link_maintain;
            System.out.println(gson_toJson.toJson(infoSend));
            sockTo.send(gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1")));
        }
        catch (ExceptionUDT ex)
        {
            log(ID_from + " connect to " + destination + " failed...");
        }
    }
    
    private void nat_request(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        String destination = info.get("ID_target");
        String ID_from = info.get("ID");
        String IP_from = sock.getRemoteInetAddress().toString().split("/")[1];
        String port_from = sock.getRemoteInetPort() + "";
        Client client_from = find_client(ID_from);
        Client client_to = find_client(destination);
        Map<String, String> send_info = new HashMap<>();
        if(client_to == null || client_from == null)
        {
            log("client not in routetable");
        }
        else
        {
            //没问题后发包交换两节点信息，先发给to，防止connect时候失败
            SocketUDT sock_to = this.nat_new.get(destination);
//            sock_to.bind(new InetSocketAddress(this.host, this.port));
            send_info.put("type", "LinkE");
            send_info.put("type_d", "03");
            send_info.put("IP", IP_from);
            send_info.put("Port", port_from);
            send_info.put("ID", ID_from);
            //否则向client_to发包成功
            try
            {
//                sock_to.connect(new InetSocketAddress(client_to.IP, parseInt(client_to.port)));
                sock_to.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
                sock_to.close();
                this.nat_new.remove(destination);
            }
            catch(ExceptionUDT ex)
            {
                //应该向client_from发送错误包
                log("cannot connect...nat_request failed...");
                return;
            }
//            sock_to.close();
            send_info.remove("IP");
            send_info.remove("Port");
            send_info.remove("ID");
            send_info.replace("type_d", "05");
            send_info.put("content", "begin");
            sock.send(this.gson_toJson.toJson(send_info).getBytes(Charset.forName("ISO-8859-1")));
        }
        sock.close();
    }
    
    private void informDirectConnect(Map<String, String> info, SocketUDT sock)
    {
        String destination = info.get("ID_target");
        Client client_to = find_client(destination);
        SocketUDT sockInform = null;
        info.remove("ID_target");
        info.replace("type_d", "07");
        try 
        {
            sockInform = new SocketUDT(TypeUDT.STREAM);
            sockInform.bind(new InetSocketAddress(host, port));
            sockInform.connect(new InetSocketAddress(client_to.IP_maintain, parseInt(client_to.port_maintain)));
            sockInform.send(this.gson_toJson.toJson(info).getBytes(Charset.forName("ISO-8859-1")));
        } 
        catch (ExceptionUDT e) 
        {
            //向申请方发错误包么
            log("cannot connect destination " + destination + "...direct connect application failed...");
        }
    }
    
    private void informSendSuccess(Map<String, String> info, SocketUDT sock)
    {
        String destination = info.get("To");
        Client client_to = find_client(destination);
        try 
        {
            SocketUDT sockInform = new SocketUDT(TypeUDT.STREAM);
            sockInform.bind(new InetSocketAddress(host, port));
            sockInform.connect(new InetSocketAddress(client_to.IP_maintain, Integer.parseInt(client_to.port_maintain)));
            sockInform.send(this.gson_toJson.toJson(info).getBytes(Charset.forName("ISO-8859-1")));
        }
        catch (ExceptionUDT ex) 
        {
            Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void informResend(Map<String, String> info, SocketUDT sock)
    {
        return_route(info, sock);
    }
    
    private void record_info(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        String connectivity = info.get("Connectivity");
        String IP_from = null, port_from = null;
        try
        {
            IP_from = sock.getRemoteInetAddress().getHostAddress();
            port_from = sock.getRemoteInetPort() + "";
        }
        catch(NullPointerException e)
        {
            log("sock null");
        }     
        String destination = info.get("ID_target");
        String ID_source = info.get("ID");
        Client client_from = find_client(ID_source);
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
                map_edge_change(parseInt(destination), parseInt(ID_source));
            }//if connectivity=true
            else
            {
                record.put(pair_from,"false");
                int from = Integer.parseInt(info.get("ID"));
                int to = Integer.parseInt(info.get("ID_target"));
		map_edge_delete(from, to);//收到"LinkC false直接delete边"
            }//do nothing?
            client_from.isConnecting = false;
            client_to.isConnecting = false;
        }   
    }
    
/*      
    public void map_change(Map<String, String> info, SocketUDT sock) 
    {
        int ID = Integer.parseInt(info.get("ID"));
        double RTT, bandwidth, lostRate, weight;
        try {
            lock.lock();
            //++this.mapVertices;//加到register处,掉线时要--，不知道为什么把check注释掉了所以先不管
            for (int i = 0; i < Integer.parseInt(info.get("cnt")); ++i) {
                if (info.get("RTT_" + i).trim().equals("Infinity")) {
                    continue;
                }
                RTT = Double.parseDouble(info.get("RTT_" + i));
                bandwidth = Double.parseDouble(info.get("bandwidth_" + i));
                lostRate = Double.parseDouble(info.get("lostRate_" + i));
                weight = 1.0 * RTT + 0.2 * lostRate + 1.0 * bandwidth;//比例问题，因为这三项的单位不同，或许应该给每项设定一个标准值，然后用其和标准值的比来表示weight
                if (this.fi.weight[ID][i] == INFINITY && this.fi.weight[i][ID] == INFINITY) {
                    this.fi.weight[ID][i] = this.fi.weight[i][ID] = weight;
                } else {
                    this.fi.weight[ID][i] = this.fi.weight[i][ID] = (this.fi.weight[ID][i] + weight) / 2;
                    //有问题，更新的时候，如果要平均应该是和新到的平均，应该不能和原来的数据平均，现在这样写的话，更新时先到的会和原来的取平均。而如果要控制它只和
                    //另外一边更新的取平均(也就是让先到的更新包直接取代后到的，后到的则和先到的平均)，这样的话似乎比较难写，所以在想要不要就不取平均了...
                }
            }
            this.fi.res_init();
            this.fi.path_init();
            this.fi.floyd();
        } catch (Exception e) {

        } finally {
            lock.unlock();
        }
    }
*/
    private void map_edge_change(int from, int to)
    {
        try
        {
            this.lock.lock();
            this.fi.weight[from][to] = this.fi.weight[to][from] = 2333;
            this.fi.res_init();
            this.fi.path_init();
            this.fi.floyd();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            try 
            {
                this.lock.unlock();
            } 
            catch (Exception e) 
            {
            
            }
        }
    }
    
    private void map_edge_change(Map<String, String> info, SocketUDT sock) 
    {
        double border = 123;//定义一个border,旧的和新的差距超过这个border,就直接新代旧，否则取平均      
        int from = Integer.parseInt(info.get("ID"));
        int to = Integer.parseInt(info.get("ID_target"));
        double RTT, bandwidth, lostRate, weight;
        try {
            lock.lock();
            if(info.get("lostRate").trim().equals("NaN"))
            {
                //此处需要把RTT, bandwidth, lostRate变为默认值
                map_edge_change(from, to);
                return;
            }
            RTT = Double.parseDouble(info.get("RTT"));
            bandwidth = Double.parseDouble(info.get("bandwidth"));
            lostRate = Double.parseDouble(info.get("lostRate"));
            weight = 1.0 * RTT + 0.2 * lostRate + 1.0 * bandwidth;
            if (Math.abs(this.fi.weight[from][to] - weight) > border && Math.abs(this.fi.weight[to][from] - weight) > border) 
            {
                this.fi.weight[from][to] = this.fi.weight[to][from] = weight;
            } //差距超过border直接取代
            else 
            {
                this.fi.weight[from][to] = this.fi.weight[to][from] = (this.fi.weight[from][to] + weight) / 2;
            }//否则取平均

            this.fi.res_init();
            this.fi.path_init();
            this.fi.floyd();
        } 
        catch (Exception e) 
        {

        } 
        finally 
        {
            lock.unlock();
        }
    }
    
    private void map_edge_delete(int from, int to) 
    {
        //double RTT, bandwidth, lostRate, weight;
        try 
        {
            lock.lock();
            this.fi.weight[from][to] = this.fi.weight[to][from] = INFINITY;
            this.fi.res_init();
            this.fi.path_init();
            this.fi.floyd();
        } 
        catch (Exception e) 
        {

        } 
        finally 
        {
            lock.unlock();
        }
    }

        private void return_route(Map<String, String> info, SocketUDT sock) {
        List<Integer> route;
        List<Integer> judge;
        List<List<Integer>> route_list = new ArrayList<>();
        List<String> used = new ArrayList<>();//记录发送过的边
        String pair;
        int from = Integer.parseInt(info.get("ID"));
        int to = Integer.parseInt(info.get("ID_target"));
        int cnt = Integer.parseInt(info.get("Cnt"));
        int Rout1, Rout2, Rout3, Rout1cnt = 0, Rout2cnt = 0, Rout3cnt = 0;
        int Routcnt;
        Map<String, String> sendBack;
        sendBack = new HashMap<>();
        if (this.fi.res[from][to] == this.INFINITY) {
            sendBack.put("type", "ERR");
            sendBack.put("type_d", "02");
            try {
                sock.send(this.gson_toJson.toJson(sendBack).getBytes(Charset.forName("ISO-8859-1")));
                //发送错误包，无法连接
            } catch (ExceptionUDT ex) {
                //    Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
                log("No route between two nodes...");
            }
        } //否则发送路径
        else {
            int[] able = new int[mapVertices];//用来存和from直连的顶点
            int count = 0;
            for (int i = 0; i < mapVertices; i++) {
                if (fi.weight[from][i] > 0 && fi.weight[from][i] != INFINITY) {
                    able[count++] = i;//统计与from直接相连的node数
                }
            }
            int[] a = new int[count];//用一个类似bool的int数组来表示是否算过(因为bool不知道怎么初始化)，1代表算过
            double min;
            int temp = (int) INFINITY;//循环时每次选出的顶点编号
            int chosen = (int) INFINITY;//循环时每次选出的顶点在able数组中的编号 
            for (int i = 0; i < count; i++) {
                route = new ArrayList<>();
                judge = new ArrayList<>();
                min = INFINITY;
                for (int j = 0; j < count; j++) {
                    if (a[j] == 0) {
                        if (fi.weight[from][able[j]] + fi.res[able[j]][to] < min) {
                            min = fi.weight[from][able[j]] + fi.res[able[j]][to];
                            temp = able[j];
                            chosen = j;
                        }
                    }
                }
                a[chosen] = 1;
                route.add(from);
                this.fi.output_toList(temp, to, route);
                this.fi.output_toList(temp, to, judge);
                if (!judge.contains(from)) {
                    boolean repeated = false;
                    for (int m = 0; m < route.size() - 1; m++) {
                        pair = route.get(m) + " " + route.get(m + 1);
                        if (used.contains(pair)) {
                            repeated = true;
                            break;
                        }
                    }
                    if (!repeated) {
                        route_list.add(route);
                        for (int m = 0; m < route.size() - 1; m++) {
                            pair = route.get(m) + " " + route.get(m + 1);
                            used.add(pair);
                        }
                    }
                }//如果中间经过from中转,或者路径中的边在之前用过了，则不要
            }//得到排好序的route_list,最短的在最前面,以此类推
            //Random ran = new Random();
            //int r = ran.nextInt(100);
            if (route_list.size() >= 3) {
                Routcnt = 3;
                Rout1cnt = route_list.get(0).size() - 1;
                Rout2cnt = route_list.get(1).size() - 1;
                Rout3cnt = route_list.get(2).size() - 1;
            } else if (route_list.size() == 2) {
                Routcnt = 2;
                Rout1cnt = route_list.get(0).size() - 1;
                Rout2cnt = route_list.get(1).size() - 1;
            } else if (route_list.size() == 1) {
                Routcnt = 1;
                Rout1cnt = route_list.get(0).size() - 1;
            } else {
                Routcnt = 0;
            }
            sendBack.put("type", "RoutD");
            sendBack.put("type_d", "01");
            sendBack.put("From", String.format("%05d", from));
            sendBack.put("To", String.format("%05d", to));
            sendBack.put("RoutCnt", Routcnt + "");
            sendBack.put("No", info.get("No"));
            log("" + Routcnt);
            switch (Routcnt) {
                case 3:
                    Rout1 = (int) Math.round(cnt * 0.5);
                    Rout2 = (int) Math.round(cnt * 0.3);
                    Rout3 = (int) Math.round(cnt * 0.2);
                    sendBack.put("Rout1Cnt", Rout1cnt + "");
                    sendBack.put("Rout1", Rout1 + "");
                    for (int i = 1; i <= Rout1cnt; i++) {
                        sendBack.put("Rout1Hop_" + i, String.format("%05d", route_list.get(0).get(i)));
                    }
                    sendBack.put("Rout2Cnt", Rout2cnt + "");
                    sendBack.put("Rout2", Rout2 + "");
                    for (int i = 1; i <= Rout2cnt; i++) {
                        sendBack.put("Rout2Hop_" + i, String.format("%05d", route_list.get(1).get(i)));
                    }
                    sendBack.put("Rout3Cnt", Rout3cnt + "");
                    sendBack.put("Rout3", Rout3 + "");
                    for (int i = 1; i <= Rout3cnt; i++) {
                        sendBack.put("Rout3Hop_" + i, String.format("%05d", route_list.get(2).get(i)));
                    }
                    break;
                case 2:
                    Rout1 = (int) Math.round(cnt * 0.7);
                    Rout2 = (int) Math.round(cnt * 0.3);
                    Rout3 = 0;
                    sendBack.put("Rout1Cnt", Rout1cnt + "");
                    sendBack.put("Rout1", Rout1 + "");
                    for (int i = 1; i <= Rout1cnt; i++) {
                        sendBack.put("Rout1Hop_" + i, String.format("%05d", route_list.get(0).get(i)));
                    }
                    sendBack.put("Rout2Cnt", Rout2cnt + "");
                    sendBack.put("Rout2", Rout2 + "");
                    for (int i = 1; i <= Rout2cnt; i++) {
                        sendBack.put("Rout2Hop_" + i, String.format("%05d", route_list.get(1).get(i)));
                    }
                    break;
                case 1:
                    Rout1 = cnt;
                    Rout2 = Rout3 = 0;
                    sendBack.put("Rout1Cnt", Rout1cnt + "");
                    sendBack.put("Rout1", Rout1 + "");
                    for (int i = 1; i <= Rout1cnt; i++) {
                        sendBack.put("Rout1Hop_" + i, String.format("%05d", route_list.get(0).get(i)));
                    }
                    break;
                default:
                    sendBack = new HashMap<>();
                    sendBack.put("type", "ERR");
                    sendBack.put("type_d", "02");
                    break;
            }
            System.out.println(this.gson_toJson.toJson(sendBack));
            try {
                sock.send(this.gson_toJson.toJson(sendBack).getBytes(Charset.forName("ISO-8859-1")));
                //发送错误包，无法连接
            } catch (ExceptionUDT ex) {
                //    Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
                log("return route failed...");
            }
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
        if(this.debug == true)
            System.out.println(message);
    }
    
}

class Client
{
    String ID = null;
    String userName = null;
    String IP_local = null;
    String IP_maintain = null;
    String port_maintain = null;
    boolean isConnecting = false;
    public Client(String userName, long clientNum, String IP_local, InetAddress IP_maintain, int port_maintain)
    {
        this.userName = userName;
        this.ID = String.format("%05d", clientNum);
        this.IP_local = IP_local;
        this.IP_maintain = IP_maintain.getHostAddress();
        this.port_maintain = port_maintain + "";
    }
    @Override
    public String toString()
    {
        return this.userName + "\n" + this.ID + "\n" + this.IP_maintain + "\n" + this.port_maintain;
    }
}

class Floyd_implementation {

    public double[][] res;
    public double[][] weight;
    double INF = 66666666;
    public int[][] path_temp;

   public Floyd_implementation(int size) {
        this.res = new double[size][size];
        this.weight = new double[size][size];
        this.path_temp = new int[size][size];
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    this.res[i][j] = this.weight[i][j] = INF;
                } else {
                    this.res[i][j] = this.weight[i][j] = 0;
                }
            }
        }
    }

    public void res_init() {
        for (int i = 0; i < res.length; ++i) {
            for (int j = 0; j < res.length; ++j) {
                if (i != j) {
                    this.res[i][j] = this.weight[i][j];
                } else {
                    this.res[i][j] = 0;
                }
            }
        }
    }

    public void weight_init(double[][] w) {
        for (int i = 0; i < weight.length; ++i) {
            for (int j = 0; j < weight.length; ++j) {
                if (i != j) {
                    this.weight[i][j] = w[i][j];
                } else {
                    this.weight[i][j] = 0;
                }
            }
        }
    }

    public void path_init() {
        for (int i = 0; i < path_temp.length; ++i) {
            for (int j = 0; j < path_temp.length; ++j) {
                if (weight[i][j] == INF) {
                    path_temp[i][j] = (int) INF;
                } else {
                    path_temp[i][j] = j;
                }
            }
        }

    }

    public void floyd() {
        int i, j, k;
        for (k = 0; k < this.res.length; ++k) {
            for (i = 0; i < this.res.length; ++i) {
                for (j = 0; j < this.res.length; ++j) {
                    if (this.res[i][k] + this.res[k][j] < this.res[i][j]) {
                        this.res[i][j] = this.res[i][k] + this.res[k][j];
                        this.path_temp[i][j] = k;//path_temp存的是i到j的第一个中转点
                    }
                }
            }
        }
    }

    public void output_toList(int i, int j, List<Integer> res) {
        res.add(i);
        output_toList_implement(i, j, res);
    }

    private int output_toList_implement(int i, int j, List<Integer> res) {
        if (i == j) {
            return i;
        }
        if (this.path_temp[i][j] == j)//代表不通过中转
        {
            res.add(j);
            return j;
        } else {
            output_toList_implement(i, path_temp[i][j], res);//否则递归
            output_toList_implement(path_temp[i][j], j, res);
        }
        return 0;
    }

    public void output(int i, int j) {
        if (i == j) {
            return;
        }
        if (this.path_temp[i][j] == j) {
            System.out.print(j);
        } else {
            output(i, path_temp[i][j]);
            System.out.print("->");
            output(path_temp[i][j], j);
        }
    }

    public void count() {
        for (int i = 0; i < this.res.length; ++i) {
            for (int j = 0; j < this.res.length; ++j) {
                if (this.path_temp[i][j] == INF || i == j)
                    ; else {
                    System.out.print(i + "->");
                    output(i, j);
                    System.out.println();
                }
            }
        }
    }
}
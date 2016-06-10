import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.net.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
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
    Map<String, SocketUDT> nat_new = new HashMap<>();
    Floyd_implementation fi;
    long clientNum = 0;
    SocketUDT serverSocket;
    Gson gson_fromJson;
    Gson gson_toJson;
    boolean check_on = false;
    Lock lock = new ReentrantLock();
    Date early = null;
    Type JSON_TYPE = new TypeToken<Map<String, String>>(){}.getType();
    int mapVertices = 0;
    
//    final String host = "10.104.13.233";
    final String host = "127.0.0.1";
    final int port = 23333;
    final double INFINITY = 66666666;
    final int CLIENTNUM = 10;
    final int MAX_CLIENT = 10;
    final String NAT_TYPE = "TNAT";
    
    public MultiServerImplementation() throws IOException 
    {
        this.gson_fromJson = new Gson();
        this.gson_toJson = new GsonBuilder().create();
        this.serverSocket = new NetServerSocketUDT().socketUDT();
        this.fi = new Floyd_implementation(MAX_CLIENT);
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.listen(CLIENTNUM);
        early = new Date();
        log("Waiting for connection...");
    }
    
    public void dual_with(SocketUDT sock) throws ExceptionUDT
    {
        byte arrRecv[] = new byte[1024];
        sock.receive(arrRecv);
        Map<String, String> info;
        info = this.gson_fromJson.fromJson((new String(arrRecv)).trim(), this.JSON_TYPE);
        log((new String(arrRecv)).trim());
        switch(info.get("type").trim())
        {
            case "LinkE":
            {
                switch(info.get("type_d").trim())
                {
                    case "01":
                    //    key_send(info, sock);
                        pre_nat_request(info, sock);
                        break;
                    case "02":
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
            case "NodeI":
            {
                switch(info.get("type_d").trim())
                {
                    case "00":
                        register(info, sock);
                }
            }
            break;
        }
    }
    
    public void register(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
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
        client = new Client(info.get("UName"), temp_clientNum, sock.getRemoteInetAddress(), sock.getRemoteInetPort(), sock);
        Client newClient = client;
        //需要返回上线用户的ID和username
        //注意不要搞一模一样的用户名，会覆盖
        infoSend.put("type", "NodeT");
        infoSend.remove("type_d");
        infoSend.remove("ID");
        Map<String, String> informInfo = new HashMap<>();
        informInfo.put("type", "NodeI");
        informInfo.put("type_d", "02");
        informInfo.put("ID", client.ID);
        int clientNumSend = 0;
        for (Iterator<Client> it = route_table.keySet().iterator(); it.hasNext();) 
        {
            client = it.next();
            try 
            {
                SocketUDT sockInform = client.link_maintain;
                sockInform.send(gson_toJson.toJson(informInfo).getBytes(Charset.forName("ISO-8859-1")));
                infoSend.put("UName_" + (clientNumSend + 1), client.userName);
                infoSend.put("ID_" + (++clientNumSend), client.ID);
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
    public Client find_client(String destination)
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
    
    public void check_online() throws InterruptedException, ExceptionUDT, IOException
    {
        Date later;
        while(true)
        {
            later = new Date();
            if(later.getTime() - this.early.getTime() >= 1000 * 60 * 5) //毫秒级(此处为5分钟)
            {
                this.early = later;
//                check_implementation();
            }
        }
    }
    //没加锁，可能会出问题
    //暂时不管keyMap，因为暂时没用...
/*    private void check_implementation() throws ExceptionUDT
    {
        Client client;
        SocketUDT sock;
        Map<String, String> info = new HashMap<>();
        info.put("type", "NodeD");
        Iterator it = this.route_table.keySet().iterator();
        while(it.hasNext())
        {
            client = (Client)it.next();
            sock = client.link_maintain; //如果抛出异常就是创建socket的锅
            //现在还不是知道要不要发包收包检测是否掉线
            sock.bind(new InetSocketAddress(this.host, this.port));
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
                            sockInform.bind(new InetSocketAddress(this.host, this.port));
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
 */ 
    public void pre_nat_request(Map<String, String> info, SocketUDT sock)
    {
        String destination = info.get("ID_target");
        String ID_from = info.get("ID");
        String IP_from = sock.getRemoteInetAddress().toString().split("/")[1];
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
            log(client_to.link_maintain.getRemoteSocketAddress().toString());
            client_to.link_maintain.send(gson_toJson.toJson(infoSend).getBytes(Charset.forName("ISO-8859-1")));
        } 
        catch (ExceptionUDT ex) 
        {
            log(ID_from + " connect to " + destination + " failed...");
        }
    }
    
    public void nat_request(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
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

        public void map_change(Map<String, String> info, SocketUDT sock) {
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

    public void map_edge_insert(Map<String, String> info, SocketUDT sock) {
        int from = Integer.parseInt(info.get("ID"));
        int to = Integer.parseInt(info.get("to"));
        double RTT, bandwidth, lostRate, weight;
        try {
            lock.lock();
            RTT = Double.parseDouble(info.get("RTT_"));
            bandwidth = Double.parseDouble(info.get("bandwidth_"));
            lostRate = Double.parseDouble(info.get("lostRate_"));
            weight = 1.0 * RTT + 0.2 * lostRate + 1.0 * bandwidth;
            if (this.fi.weight[from][to] == INFINITY && this.fi.weight[to][from] == INFINITY) {
                this.fi.weight[from][to] = this.fi.weight[to][from] = weight;
            } else {
                this.fi.weight[from][to] = this.fi.weight[to][from] = (this.fi.weight[from][to] + weight) / 2;
            }

            this.fi.res_init();
            this.fi.path_init();
            this.fi.floyd();
        } catch (Exception e) {

        } finally {
            lock.unlock();
        }
    }

    public void map_edge_delete(Map<String, String> info, SocketUDT sock) {
        int from = Integer.parseInt(info.get("ID_source"));
        int to = Integer.parseInt(info.get("ID"));
        //double RTT, bandwidth, lostRate, weight;
        try {
            lock.lock();
            this.fi.weight[from][to] = this.fi.weight[to][from] = INFINITY;
            this.fi.res_init();
            this.fi.path_init();
            this.fi.floyd();
        } catch (Exception e) {

        } finally {
            lock.unlock();
        }
    }

    public void return_route(Map<String, String> info, SocketUDT sock) {
        List<Integer> best ;
        List<List<Integer>> route_list =new ArrayList<>();
        int from = Integer.parseInt(info.get("ID"));
        int to = Integer.parseInt(info.get("ID_target"));
        //未完待续
        int [] able=new int[mapVertices];
        int count = 0;
        for (int i = 0; i < mapVertices; i++) {
            if (fi.weight[from][i] > 0 || fi.weight[from][i] != INFINITY) {
                able[count++]=i;//统计与from直接相连的node数
            }
        }
        int[] a = new int[count];//用一个类似bool数组来表示是否算过，0代表算过

        double min = INFINITY;
        int temp=(int)INFINITY;
        for (int i = 0; i < count; i++) {
            best = new ArrayList<>();
            for (int j = 0; j < count; j++) {
                if (a[j]!=0) {
                    if(fi.weight[from][able[j]]+fi.res[able[j]][to]<min)
                        min=fi.weight[from][able[j]]+fi.res[able[j]][to];
                        temp=able[j];
                        a[j]=1;
                }
            }
            best.add(from);
            this.fi.output_toList(temp,to,best);
            route_list.add(best);
            }//得到排好序的route_list,最短的在最前面,未完待续        
                 
       //this.fi.output_toList(Integer.parseInt(info.get("ID")), Integer.parseInt(info.get("ID_target")), best);
       // Map<String, String> sendBack;
        //sendBack = new HashMap<>();
        if (this.fi.res[Integer.parseInt(info.get("ID"))][Integer.parseInt(info.get("ID_target"))] == this.INFINITY) {
            //发送错误包，无法连接
        } else {
            //发送路径
        }
    }

    public void record_info(Map<String, String> info, SocketUDT sock) throws ExceptionUDT {
        String connectivity = info.get("Connectivity");
        String IP_from = null, port_from = null;
        try {
            IP_from = sock.getRemoteInetAddress().toString().split("/")[1];
            port_from = sock.getRemoteInetPort() + "";
        } catch (NullPointerException e) {
            log("sock null");
        }
        String destination = info.get("ID");
        String ID_source = info.get("ID_source");
        Client client_from = find_client(ID_source);
        Client client_to = find_client(destination);
        List<Client> connect_list;
        Map.Entry<Client, Client> pair_from;
        if (client_from == null || client_to == null) {
            log("Not online...");
        } else {
            pair_from = new AbstractMap.SimpleEntry<>(client_from, client_to);
            if ("true".equals(connectivity.trim())) {
                connect_list = route_table.get(client_from);
                if (connect_list == null) {
                    connect_list = new ArrayList<>();
                    connect_list.add(client_to);
                    route_table.put(client_from, connect_list);
                } else if (connect_list.indexOf(client_to) == -1) {
                    connect_list.add(client_to);
                }//add B to A's connectlist
                connect_list = route_table.get(client_to);
                if (connect_list == null) {
                    connect_list = new ArrayList<>();
                    connect_list.add(client_from);
                    route_table.put(client_to, connect_list);
                } else if (connect_list.indexOf(client_from) == -1) {
                    connect_list.add(client_from);
                }
                record.put(pair_from, "true");
            }//if connectivity=true
            else {
                record.put(pair_from, "false");
                map_edge_delete(info, sock);//收到"LinkC false直接delete边"
            }//do nothing?
            client_from.isConnecting = false;
            client_to.isConnecting = false;
        }
    }

    @Override
    public void run() {
        SocketUDT sock;
        if (!this.check_on) //让一个线程用来检测定时在线
        {
            lock.lock();
            try {
                this.check_on = true;
            } finally {
                lock.unlock();
            }
            try {
                check_online();
            } catch (InterruptedException | ExceptionUDT ex) {
                Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MultiServerImplementation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        while (true) {
            try {
                sock = this.serverSocket.accept();
                dual_with(sock);
            } catch (ExceptionUDT ex) {
                Logger.getLogger(MultiServer.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    private void log(String message) {
        System.out.println(message);
    }

}

class Client {

    String ID = null;
    String userName = null;
    String IP_maintain = null;
    String port_maintain = null;
    SocketUDT link_maintain;
    boolean isConnecting = false;

    public Client(String userName, long clientNum, InetAddress IP_maintain, int port_maintain, SocketUDT link_maintain) {
        this.userName = userName;
        this.ID = String.format("%05d", clientNum);
        this.IP_maintain = IP_maintain.toString().split("/")[1];
        this.port_maintain = port_maintain + "";
        this.link_maintain = link_maintain;
    }

    @Override
    public String toString() {
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
            for (int j = 0; j < size; ++j) {
                this.res[i][j] = this.weight[i][j] = INF;
            }
        }

    }

    public void res_init() {
        for (int i = 0; i < res.length; ++i) {
            for (int j = 0; j < res.length; ++j) {
                res[i][j] = weight[i][j];
            }
        }
    }

    public void weight_init(double[][] w) {
        for (int i = 0; i < weight.length; ++i) {
            for (int j = 0; j < weight.length; ++j) {
                weight[i][j] = w[i][j];
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
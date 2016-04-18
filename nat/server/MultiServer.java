import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.net.NetServerSocketUDT;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
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
 * @author 杨德中
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
    long clientNum = 0;
    SocketUDT serverSocket;
    Gson gson_fromJson;
    Gson gson_toJson;
    Lock lock = new ReentrantLock();
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
        log("Waiting for connection...");
    }
    
    public void dual_with(SocketUDT sock) throws ExceptionUDT
    {
        byte arrRecv[] = new byte[1024];
        log("Accept new connection ");
        sock.receive(arrRecv);
        Map<String, String> info = null;
        info = this.gson_fromJson.fromJson((new String(arrRecv)).trim(), this.JSON_TYPE);
        switch(info.get("type").trim())
        {
            case "LinkE":
            {
                switch(info.get("type_d").trim())
                {
                    case "01":
                        ;
                }
            }
            case "RegiS":
            {
                switch(info.get("type_d").trim())
                {
                    case "01":
                        register(info, sock);
                }
            }
        }
    }
    
    public void register(Map<String, String> info, SocketUDT sock) throws ExceptionUDT
    {
        Client client = null;
        lock.lock();
        try 
        {
            client = new Client(info.get("username"), this.clientNum++);
        } 
        finally 
        {
            lock.unlock();
        }
        this.route_table.put(client, null);
        //需要返回上线用户的ID和username
        //注意不要搞一模一样的用户名，会覆盖
        Map<String, String> clientOnline = new HashMap<>();
        for (Iterator<Client> it = route_table.keySet().iterator(); it.hasNext();) 
        {
            client = it.next();
            clientOnline.put(client.userName, client.ID);
        }
        sock.send(gson_toJson.toJson(clientOnline).getBytes(Charset.forName("ISO-8859-1")));
    }
    
    @Override
    public void run()
    {
        SocketUDT sock = null;
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
    public Client(String userName, long clientNum)
    {
        this.userName = userName;
        this.ID = String.format("%05d", clientNum);
    }
    @Override
    public String toString()
    {
        return this.userName + "\n" + this.ID;
    }
}

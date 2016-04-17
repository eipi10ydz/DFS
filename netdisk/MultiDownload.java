/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.locks.*;
/**
 *
 * @author 杨德中
 */
class MDImplementation implements Runnable
{
    String [] local_path = null;
    String [] path = null;
    int allNum = 0;
    int cnt = 0;
    Vdisk vd;
    Lock lock = new ReentrantLock();
    public MDImplementation(String[]local_path, String[]path , Vdisk vd)
    {
        this.local_path = local_path;
        this.path = path;
        this.allNum = path.length;
        this.vd = vd;           
    }
        
    @Override
    public void run() 
    {
        int temp = 0;
        for(; this.cnt < this.allNum; )
        {
            try 
            {
                lock.lock();
                temp = this.cnt++;
            }
            finally
            {    
                lock.unlock();
                try
                {            
                    //需要根据延迟来改变sleep的时间。。。肯定是没充会员限制我并发量，口亨，太快竟然不响应
                    //没想到上传比下载快那么多。。上传最多两个线程，延迟9000，下载不延迟也没出问题。。开10个线程。。。
                    this.vd.download_file(this.path[temp], this.local_path[temp]);            
                }
                catch (URISyntaxException | IOException ex)
                {
                    Logger.getLogger(MultiUpload.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    //    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

public class MultiDownload 
{
    int ThreadNum = 10;
    String [] local_path = null;
    String [] path = null;
    Vdisk vd = new Vdisk();
    String access_token = null;
    public MultiDownload(int ThreadNum, String[]local_path, String[]path, String access_token)
    {
        if(ThreadNum > 0)
            this.ThreadNum = ThreadNum;
        if(access_token == null)
            vd.get_access_token();
        else
            vd.access_token = access_token;
        this.local_path = local_path;
        this.path = path;
    }
    void start()
    {
        MDImplementation mt = new MDImplementation(this.local_path, this.path, vd);
        Vector<Thread> threads = new Vector<>();
        for(int i = 0; i < this.ThreadNum; ++i)
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

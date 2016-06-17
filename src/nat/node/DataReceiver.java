/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodetest;

import java.util.Map;

/**
 *
 * @author 杨德中
 */
public class DataReceiver implements Runnable
{
    protected String from;
    protected int cnt;
    protected int No;
    protected String res;
    protected Map<Integer, String> pack;
    protected Timer timer;
    
    public DataReceiver(String from, int cnt, int No)
    {
        this.from = from;
        this.cnt = cnt;
        this.No = No;
        this.timer = new Timer(300000); //5分钟?
    }
    
    /**
     *
     */
    @Override
    public void run()
    {
        while(this.pack.size() < this.cnt && (!this.timer.isExpired()))
            ;
        if(this.pack.size() < this.cnt)
        {
            //向服务器请求重发
            return;
        }
        for(int i = 0; i < this.cnt; ++i)
        {
            res = res + this.pack.get(i);
        }
        //得到完整内容
    }

}

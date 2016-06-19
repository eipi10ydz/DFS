/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodetest;

import com.barchart.udt.ExceptionUDT;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 杨德中
 */
public class NodeTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
//        String server_host = "172.16.0.215";
        String server_host = "119.29.61.231";
//        String server_host = "127.0.0.1";
        int server_port = 23333;
        Node client = null;
        try {
            try {
                client = new Node("test1", server_host, server_port);
            } catch (ExceptionUDT | PackException ex) {
                Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NodeException ex) {
            Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(true)
        {
            try 
            {
                client.transfer_message("我就测试一下...", "test1");
                return;
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }
    
}

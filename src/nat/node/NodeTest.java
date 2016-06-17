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
        try {
            try {
                Node client = new Node("test", server_host, server_port);
            } catch (ExceptionUDT | PackException ex) {
                Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NodeException ex) {
            Logger.getLogger(NodeTest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
}

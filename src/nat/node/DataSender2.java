package nodetest;

import java.nio.charset.Charset;
import java.util.List;
import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.net.NetOutputStreamUDT;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lyx
 *
 */
class DataSender2 {
    /**
     * static method for sending packages
     * @param node the node itself
     * @param ID_p the ID of the destination
     * @param packets list of packages to be send, the first package contains route info, and others contain data
     * @throws ExceptionUDT failed to establish socket or send data
     */
	protected static void Sender(Node node, String ID_p, List<String> packets) throws ExceptionUDT{
		SocketUDT sock = null;
                NetOutputStreamUDT out = null;
            try {
                sock = node.link_establisher.establish_link_m(ID_p); //建立连接
                while (true)
                {                    
                    if(sock == null)
                    {
                        sock = node.link_establisher.establish_link_m(ID_p);
                    }
                    else
                        break;
                }
                out = new NetOutputStreamUDT(sock);
            } catch (NodeException | PackException ex) 
            {
                //建立连接的锅
                Logger.getLogger(DataSender2.class.getName()).log(Level.SEVERE, null, ex);
            }
		for(int i = 0; i < packets.size(); ++i){
			try{
                            out.write(packets.get(i).getBytes(Charset.forName("ISO-8859-1")));
                            out.flush();
//                            sock.send(str.getBytes(Charset.forName("ISO-8859-1")));
			}catch(ExceptionUDT e){
				Logger.getLogger(DataSender2.class.getName()).log(Level.SEVERE, null, e);
				throw e;
			} catch (IOException ex) {
                        Logger.getLogger(DataSender2.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        if(i == 0)
			try{
				Thread.sleep(700);
			}catch(InterruptedException e1){
				Thread.currentThread().interrupt();
				return ;
			}
		} // 传送数据
		try {
			InfoSender.Info_send(node, node.ID, ID_p, sock);// Update router
															// info
		} catch (ExceptionUDT e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

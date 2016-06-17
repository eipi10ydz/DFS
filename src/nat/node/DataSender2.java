package nodetest;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lyx
 *
 */
public class DataSender2 {
	static public void Sender(Node node, String ID_p, List<String> packets) throws ExceptionUDT{
		SocketUDT sock = null; 
            try {
                sock = node.link_establisher.establish_link_m(ID_p); //建立连接
            } catch (NodeException | ExceptionUDT | PackException ex) 
            {
                //建立连接的锅
                Logger.getLogger(DataSender2.class.getName()).log(Level.SEVERE, null, ex);
            }
		for(String str : packets){
			try{
                            sock.send(str.getBytes(Charset.forName("ISO-8859-1")));	
			}catch(ExceptionUDT e){
				e.printStackTrace();
				throw e;
			}
			try{
				Thread.sleep(1000);
			}catch(InterruptedException e1){
				e1.printStackTrace();
				return ;
			}
		} // 传送数据
		InfoSender.Info_send(node, node.ID, ID_p, sock); //更新信息
	}
}

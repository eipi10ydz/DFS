package nodetest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
/**
 * @author lyx
 *
 */
public class InfoSender {
	static void Info_send(Node node,String ID, String ID_target,SocketUDT socket) throws ExceptionUDT{//socket  两个节点ID给 
			String str = new String();
			SocketUDT server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			try{
				server.connect(new InetSocketAddress(node.server_host, node.server_port));
			}catch (ExceptionUDT e ){
				e.printStackTrace();
			}
			Map<String, String> pac = new ConcurrentHashMap<String, String>();;
			Mymonitor monitor = new Mymonitor(socket);
			double RTT    = 0;
			double bandwidth = 0;
			double lostRate = 0; 
			RTT = monitor.currentMillisRTT();
			bandwidth =	monitor.currentMbpsBandwidth();
			lostRate = monitor.get_lostRate();
			pac.put("ID", ID);
			pac.put("ID_target", ID_target);
			pac.put("RTT", Double.toString(RTT));
			pac.put("bandwidth", Double.toString(bandwidth));
			pac.put("lostRate", Double.toString(lostRate));
			try{
				str = Packer.pack("RoutI", pac);
			}catch(PackException e){
				e.printStackTrace();
			}
			try {
				server.send(str.getBytes(Charset.forName("ISO-8859-1")));
			} catch (ExceptionUDT e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
}
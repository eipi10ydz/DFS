/**
 * 
 */
package nodetest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

/**
 * @author lenovo
 *
 */
class LinkEstablisherThreadS1 implements Runnable {

	private Node node;

	/**
	 * @param node
	 */
	public LinkEstablisherThreadS1(Node node) {
		this.node = node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		byte[] arr = new byte[1024];
		String str = new String();
		Map<String, String> pac;		
                try {
			SocketUDT accepter = new SocketUDT(TypeUDT.STREAM);
			accepter.setBlocking(true);
			accepter.bind(new InetSocketAddress(node.IP_local, 2333));
                        accepter.listen(5);
                        while (!Thread.currentThread().isInterrupted()) {
                            System.out.println("bind local : " + node.IP_local + ":" + 2333);
                            arr = new byte[1024];
                            SocketUDT sock = accepter.accept();
                            sock.receive(arr);// receive packet LinkE04
                            str = new String(arr, Charset.forName("ISO-8859-1")).trim();
                            Node.empty_arr(str.length(), arr);
                            pac = Packer.unpack(str);
                            if (pac.containsKey("type") && pac.get("type").equals("LinkE") && pac.containsKey("type_d")) {
                                    if (pac.get("type_d").equals("04") && node.nodeIDs.contains(pac.get("ID"))) {
                                            if (node.link_establisher.establish_link_s1(pac.get("ID"), sock)) {
                                                    node.link_timers.remove(pac.get("ID"));
                                            }
                                } else if (pac.get("type_d").equals("08") && node.nodeIDs.contains(pac.get("ID"))) {
                                    node.link_establish_socks.put(pac.get("ID"), sock);
                                } else {
                                    // TODO Something wrong
                                }
                            } 
                            else {
                                // TODO Something wrong
                            }
                        }
                    } catch (ExceptionUDT e) {
			e.printStackTrace();
                    } catch (NodeException e) {
			e.printStackTrace();
                    } catch (PackException e) {
			e.printStackTrace();
                    }
		
	}
}

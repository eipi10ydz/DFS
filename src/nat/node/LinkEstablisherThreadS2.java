package nodetest;

import java.util.Map;

import com.barchart.udt.ExceptionUDT;

class LinkEstablisherThreadS2 implements Runnable {

	private Node node;

	/**
	 * @param node
	 */
	public LinkEstablisherThreadS2(Node node) {
		this.node = node;
	}

	@Override
	public void run() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				// check if other Nodes try to establish links with this
				while (!node.messages_from_server.get("Link").isEmpty()) {
					Map<String, String> pac = node.messages_from_server.get("Link").poll();
					if (pac.get("type").equals("LinkE")) {
						if (pac.get("type_d").equals("03")) {
							try {
								if (node.link_establisher.establish_link_s2(pac.get("ID"), pac.get("IP"),
										Integer.parseInt(pac.get("Port")))) {
									node.link_timers.remove(pac.get("ID"));
								}
							} catch (ExceptionUDT e) {
								e.printStackTrace();
							} catch (NodeException e) {
								e.printStackTrace();
							} catch (PackException e) {
								e.printStackTrace();
							}
						} else if (pac.get("type_d").equals("07")) {
							try {
								if (node.link_establisher.establish_link_s3(pac.get("ID"))) {
									node.link_timers.remove(pac.get("ID"));
								}
							} catch (ExceptionUDT e) {
								e.printStackTrace();
							}
						} else {
							// TODO Something wrong
						}
					} else {
						// TODO Something wrong
					}
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return;
	}

}

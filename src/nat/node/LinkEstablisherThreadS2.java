package data_transferor;

import java.util.Map;

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
					try {
						if (pac.get("type").equals("LinkE") && pac.containsKey("type_d")) {
							if (pac.get("type_d").equals("03")) {
								try {
									if (node.link_establisher.establish_link_s2(pac.get("ID"), pac.get("IP"),
											Integer.parseInt(pac.get("Port")))) {
										node.link_timers.remove(pac.get("ID"));
									}
								} catch (NumberFormatException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (LinkException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else if (pac.get("type_d").equals("07")) {
								try {
									if (node.link_establisher.establish_link_s3(pac.get("ID"))) {
										node.link_timers.remove(pac.get("ID"));
									}
								} catch (LinkException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								throw new LinkException("Unexpected packet from the server.");
							}
						} else {
							throw new LinkException("Unexpected packet from the server.");
						}
					} catch (LinkException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return;
	}

}

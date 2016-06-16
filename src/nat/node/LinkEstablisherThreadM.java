package data_transferor;

import java.util.Map;
import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;

/**
 * 
 */

/**
 * @author wzy
 *
 */
class LinkEstablisherThreadM implements Runnable {

	private Node node;

	/**
	 * @param node
	 */
	public LinkEstablisherThreadM(Node node) {
		this.node = node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		establish_links();
		while (!Thread.currentThread().isInterrupted()) {
			// check link timers
			try {
				for (Map.Entry<String, Timer> entry : node.link_timers.entrySet()) {
					String nodeID = entry.getKey();
					Timer timer = entry.getValue();
					if (!node.nodeIDs.contains(nodeID)) {// the peer node has
															// dropped
						node.link_timers.remove(nodeID);
					} else if (timer.isExpired()) {
						try {
							SocketUDT sock = node.link_establisher.establish_link_m(nodeID);
							if (sock == null) {
								timer.postpone(20000 * timer.getCnt());
								timer.start();
							} else {
								node.link_timers.remove(nodeID);
								// TODO send a packet
								try {
									sock.close();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} catch (PackException e) {
							timer.postpone(20000 * timer.getCnt());
							timer.start();
							e.printStackTrace();
						} catch (ExceptionUDT e) {
							timer.postpone(20000 * timer.getCnt());
							timer.start();
							e.printStackTrace();
						} catch (NodeException e) {
							timer.postpone(20000 * timer.getCnt());
							timer.start();
							throw e;
						}
					}
				}
			} catch (NodeException e) {
				e.printStackTrace();
			}
			// check if new node inserted
			while (!node.node_inserted_lm.isEmpty())
				new_link_timer(node.node_inserted_lm.poll());
			// check if node deleted
			while (!node.node_deleted_lm.isEmpty()) {
				String nodeID = node.node_deleted_lm.poll();
				node.link_timers.remove(nodeID);
			}
		}
	}

	/**
	 * Try to establish links with every Node in the NodeID table.
	 */
	private void establish_links() {
		node.nodeIDs.forEach(nodeID -> {
			try {
				SocketUDT sock = node.link_establisher.establish_link_m(nodeID);
				if (sock == null) {
					new_link_timer(nodeID);
				} else {
					// TODO send a packet
					try {
						sock.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (PackException e) {
				new_link_timer(nodeID);
				e.printStackTrace();
			} catch (ExceptionUDT e) {
				new_link_timer(nodeID);
				e.printStackTrace();
			} catch (NodeException e) {
				new_link_timer(nodeID);
				e.printStackTrace();
			}
		});
		return;
	}

	/**
	 * @param nodeID
	 */
	private void new_link_timer(String nodeID) {
		if (nodeID == null)
			return;
		if (!node.nodeIDs.contains(nodeID))
			return;
		Timer timer = new Timer(10000, 3600000);
		timer.start();
		node.link_timers.put(nodeID, timer);
		return;
	}

}

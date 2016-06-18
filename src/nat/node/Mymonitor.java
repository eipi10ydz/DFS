package data_transferor;

import com.barchart.udt.MonitorUDT;
import com.barchart.udt.SocketUDT;

/**
 * @author lyx
 *
 */
class MyMonitor extends MonitorUDT {
	public MyMonitor(SocketUDT sock) {
		super(sock);
	}

	public double get_mbpsBandwidth() {
		return this.mbpsBandwidth;
	}

	public double get_msRTT() {
		return this.msRTT;
	}

	public double get_lostRate() {
		return (double) (this.pktRecvTotal + this.pktSndLossTotal) / (this.pktRecvTotal + this.pktSentTotal);
	}
}

package nodetest;

import com.barchart.udt.MonitorUDT;
import com.barchart.udt.SocketUDT;

class Mymonitor extends MonitorUDT{
	public Mymonitor(SocketUDT sock){
		super(sock);
	}
	public double get_mbpsBandwidth(){
		return this.mbpsBandwidth;
	}
	public double get_msRTT(){
		return this.msRTT;
	}
	public double get_lostRate(){
		return (double)(this.pktRecvTotal + this.pktSndLossTotal) / (this.pktRecvTotal + this.pktSentTotal);
	}
}


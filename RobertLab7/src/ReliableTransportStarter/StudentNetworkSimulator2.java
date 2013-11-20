package ReliableTransportStarter;

/* Maxime ROBERT 20/11/2013 */

public class StudentNetworkSimulator2 extends NetworkSimulator {
	/*
	 * Predefined Constants (static member variables):
	 * 
	 * int MAXDATASIZE : the maximum size of the Message data and Packet payload
	 * 
	 * int A : a predefined integer that represents entity A int B : a
	 * predefined integer that represents entity B
	 * 
	 * Predefined Member Methods:
	 * 
	 * void stopTimer(int entity): Stops the timer running at "entity" [A or B]
	 * void startTimer(int entity, double increment): Starts a timer running at
	 * "entity" [A or B], which will expire in "increment" time units, causing
	 * the interrupt handler to be called. You should only call this with A.
	 * void toLayer3(int callingEntity, Packet p) Puts the packet "p" into the
	 * network from "callingEntity" [A or B] void toLayer5(String dataSent)
	 * Passes "dataSent" up to layer 5 double getTime() Returns the current time
	 * in the simulator. Might be useful for debugging. int getTraceLevel()
	 * Returns TraceLevel void printEventList() Prints the current event list to
	 * stdout. Might be useful for debugging, but probably not.
	 * 
	 * 
	 * Predefined Classes:
	 * 
	 * Message: Used to encapsulate a message coming from layer 5 Constructor:
	 * Message(String inputData): creates a new Message containing "inputData"
	 * Methods: boolean setData(String inputData): sets an existing Message's
	 * data to "inputData" returns true on success, false otherwise String
	 * getData(): returns the data contained in the message Packet: Used to
	 * encapsulate a packet Constructors: Packet (Packet p): creates a new
	 * Packet that is a copy of "p" Packet (int seq, int ack, int check, String
	 * newPayload) creates a new Packet with a sequence field of "seq", an ack
	 * field of "ack", a checksum field of "check", and a payload of
	 * "newPayload" Packet (int seq, int ack, int check) chreate a new Packet
	 * with a sequence field of "seq", an ack field of "ack", a checksum field
	 * of "check", and an empty payload Methods: boolean setSeqnum(int n) sets
	 * the Packet's sequence field to "n" returns true on success, false
	 * otherwise boolean setAcknum(int n) sets the Packet's ack field to "n"
	 * returns true on success, false otherwise boolean setChecksum(int n) sets
	 * the Packet's checksum to "n" returns true on success, false otherwise
	 * boolean setPayload(String newPayload) sets the Packet's payload to
	 * "newPayload" returns true on success, false otherwise int getSeqnum()
	 * returns the contents of the Packet's sequence field int getAcknum()
	 * returns the contents of the Packet's ack field int getChecksum() returns
	 * the checksum of the Packet int getPayload() returns the Packet's payload
	 */

	/*
	 * PLEASE USE THE FOLLOWING VARIABLES IN YOUR ROUTINES AS APPROPRIATE. int
	 * WindowSize : the window size double RxmtInterval : the retransmission
	 * timeout int LimitSeqNo : when sequence number reaches this value, it
	 * wraps around
	 */

	public static final int FirstSeqNo = 0;
	private int WindowSize;
	private double RxmtInterval;
	private int LimitSeqNo;

	// Add any necessary class variables here. Remember, you cannot use
	// these variables to send messages error free! They can only hold
	// state information for A or B.

	int astate = 0;
	Packet astored_pkt;
	int bstate = 0;
	int bOnceThru = 0;
	Packet bstored_pkt;

	// This is the constructor. Don't touch!
	public StudentNetworkSimulator2(int numMessages, double loss,
			double corrupt, double avgDelay, int trace, int seed, int winsize,
			double delay) {
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
		WindowSize = winsize;
		LimitSeqNo = winsize + 1;
		RxmtInterval = delay;
	}

	/**
	 * This routine will be called whenever the upper layer at the sender [A]
	 * has a message to send. It is the job of your protocol to insure that the
	 * data in such a message is delivered in-order, and correctly, to the
	 * receiving upper layer.
	 **/
	protected void aOutput(Message message) {
		if (astate == 0) {
			int seqno = 0;
			int checksum = calculateChecksum(message.getData(), seqno, 0);
			astored_pkt = new Packet(seqno, 0, checksum, message.getData());
			this.toLayer3(A, astored_pkt);
			System.out.println("A: sending packet " + seqno);
			this.startTimer(A, 5);
			astate = 1;
		} else if (astate == 2) {
			int seqno = 1;
			int checksum = calculateChecksum(message.getData(), seqno, 0);
			astored_pkt = new Packet(seqno, 0, checksum, message.getData());
			this.toLayer3(A, astored_pkt);
			System.out.println("A: sending packet " + seqno);
			this.startTimer(A, 5);
			astate = 3;
		} else {
			// Refuse packet, warn //timers need to be longer
		}
	}

	/**
	 * This routine will be called whenever a packet sent from the B-side (i.e.
	 * as a result of a toLayer3() being done by a B-side procedure) arrives at
	 * the A-side. "packet" is the (possibly corrupted) packet sent from the
	 * B-side.
	 **/
	protected void aInput(Packet packet) {
		if (astate == 0) {
		}
		if (astate == 1) {
			int checksum = calculateChecksum(packet.getPayload(),
					packet.getSeqnum(), packet.getAcknum());
			if (packet.getChecksum() != checksum) {
				System.out.println("A: ACK corrupt");
			}
			if (packet.getAcknum() == 1) {
				System.out.println("A: got ACK1, we're waiting for ACK0");
			}
			if (packet.getAcknum() == 0) {
				this.stopTimer(A);
				astate = 2;
				System.out.println("A: got ACK0");
			}
		}
		if (astate == 2) {
		}
		if (astate == 3) {
			int checksum = calculateChecksum(packet.getPayload(),
					packet.getSeqnum(), packet.getAcknum());
			if (packet.getChecksum() != checksum) {
				System.out.println("A: ACK corrupt");
			}
			if (packet.getAcknum() == 0) {
				System.out.println("A: got ACK0, we're waiting for ACK1");
			}
			if (packet.getAcknum() == 0) {
				this.stopTimer(A);
				astate = 0;
				System.out.println("A: got ACK1");
			}
		}
	}

	/**
	 * This routine will be called when A's timer expires (thus generating a
	 * timer interrupt). You'll probably want to use this routine to control the
	 * retransmission of packets. See startTimer() and stopTimer(), above, for
	 * how the timer is started and stopped.
	 **/
	protected void aTimerInterrupt() {
		System.out.println("A: Timer interrupt, resending packet");
		this.toLayer3(A, astored_pkt);
		this.startTimer(A, 5);
	}

	/**
	 * This routine will be called once, before any of your other A-side
	 * routines are called. It can be used to do any required initialization
	 * (e.g. of member variables you add to control the state of entity A).
	 **/
	protected void aInit() {
		// You probably won't need to put any code in this method. It is from
		// another assignment.
	}

	/**
	 * This routine will be called whenever a packet sent from the B-side (i.e.
	 * as a result of a toLayer3() being done by an A-side procedure) arrives at
	 * the B-side. "packet" is the (possibly corrupted) packet sent from the
	 * A-side.
	 **/
	protected void bInput(Packet packet) {
		int checksum = calculateChecksum(packet.getPayload(),
				packet.getSeqnum(), packet.getAcknum());
		if (packet.getChecksum() != checksum) {
			if (bOnceThru == 1) {
				this.toLayer3(B, packet);
			}
		} else {
			if (((packet.getSeqnum() == 0) && (bstate == 0))
					|| ((packet.getSeqnum() == 1) && (bstate == 1))) {
				this.toLayer5(packet.getPayload());
				System.out.println("B: got packet " + packet.getSeqnum());
				checksum = calculateChecksum("", 0, packet.getAcknum());
				bstored_pkt = new Packet(0, packet.getAcknum(), checksum);
				this.toLayer3(B, bstored_pkt);
				System.out.println("B: sending ACK" + packet.getAcknum());
				bstate = (bstate + 1) % 2;
				bOnceThru = 1;
			} else {
				if ((bstate == 1) || (bOnceThru == 1)) {
					checksum = calculateChecksum("", 0, packet.getAcknum());
					bstored_pkt = new Packet(0, packet.getAcknum(), checksum);
					this.toLayer3(B, bstored_pkt);
					System.out.println("B: sending ACK" + packet.getAcknum());
				}
			}
		}
	}

	/**
	 * This routine will be called once, before any of your other B-side
	 * routines are called. It can be used to do any required initialization
	 * (e.g. of member variables you add to control the state of entity B).
	 **/
	protected void bInit() {
		// You probably won't need to put any code in this method. It is from
		// another assignment.
	}

	private int calculateChecksum(String payload, int seqno, int ackno) {
		int singlecharval;
		int sum = 0;

		// calculate checksum
		int max = payload.length();

		for (int i = 0; i < max; i++) {
			singlecharval = payload.charAt(i);
			sum += singlecharval;
		}

		sum += seqno;
		sum += ackno;
		return sum;

	}
}

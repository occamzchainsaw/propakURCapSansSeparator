package com.bi.propakSansSeparator.pickups;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

// THIS IS A SAMPLE ON HOW TO READ DATA THROUGHT THE RTDE
// DO NOT USE WITH THE URCAP

public class RobotDataReader {
	private final String TCP_IP;
	private final int TCP_port;
	
	public RobotDataReader() {
		this.TCP_IP = "127.0.0.1";
		this.TCP_port = 30003;
	}
	
	public RobotDataReader(String ip) {
		this.TCP_IP = ip;
		this.TCP_port = 30003;
	}
	
	private double[] realTimeMessage;
	
	public void readNow() {
		readSocket();
	}
	
	private void readSocket() {
		try {
			// Create a new Socket Client
			Socket rt = new Socket(TCP_IP, TCP_port);
			if (rt.isConnected()){
				System.out.println("Connected to UR Realtime Client");
			}
			
			// Create stream for data
			DataInputStream in;
			in = new DataInputStream(rt.getInputStream());
			
			// Read the integer available in stream
			int length = in.readInt();
			System.out.println("Length is "+length);
			
			// Initialize size of RealtimeMessage be using received length
			realTimeMessage = new double[length];
			// Add length integer to output array
			realTimeMessage[0] = length;
			
			// Calculate how much data is available from the length
			int data_available = (length-4)/8;
			System.out.println("There are "+data_available+" doubles available");
			
			// Loop over reading the available data
			int i = 1;
			while (i <= data_available){
				realTimeMessage[i] = in.readDouble();
				System.out.println("Index "+i+" is "+realTimeMessage[i]);
				i++;
			}
			
			// Perform housekeeping 
			in.close();
			rt.close();
			System.out.println("Disconnected from UR Realtime Client");
		}
		catch (IOException e){
			System.out.println(e);
		}
	}
	
	private enum RTinfo {
		digital_in (86, 1),
		digital_out (131, 1);
		
		private final int index;
		private final int count;
		RTinfo(int index, int count) {
			this.index = index;
			this.count = count;
		}
		private int index() {return index;}
		private int count() {return count;}
	}
	
	public void getDigitalInputs() {
		double inputs = realTimeMessage[RTinfo.digital_in.index()];
		int inputs_int = (int) inputs;
		System.out.println(Integer.toBinaryString(inputs_int));
	}
	
	public void getDigitalOutputs() {
		double outputs = realTimeMessage[RTinfo.digital_out.index()];
		int outputs_int = (int) outputs;
		System.out.println(Integer.toBinaryString(outputs_int));
	}
}

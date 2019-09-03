package edu.ecnu.woodpecker.tools;

import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

public class Client
{
	//IOPS; size; ip address; time
	public static void main(String[] args) throws FileNotFoundException {		
		new Thread(new NettyClient(args[0], args[1])).start();
		final int IOPS = Integer.parseInt(args[2]);
		final int size = Integer.parseInt(args[3]);
		final int time = Integer.parseInt(args[4]);
		while(true){
			if(NettyClient.isAllWritable()){
				break;				
			}
			try {
				Thread.sleep(1000);	
			} catch (InterruptedException e) {
					e.printStackTrace();
			}
		}
		new Timer().schedule(new TimerTask() {	
			int count = 0;
			public void run() 
			{
				count ++;
			    int sc = (size*1024*1024/IOPS - 20)/8;
				for(int i=0; i<IOPS; i++ )
				{
					MessageProto.Message.Builder Message = MessageProto.Message.newBuilder();
					char a[]= new char[sc];
					String str=new String(a);
					Message.setMsg(str);
					NettyClient.WorkOrder(Message.build());
					
				}
				if (count >= time)
				{
					cancel();
				}
			}
		}, 0, 1000);
		
		try {
			Thread.sleep((time+2)*1000);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.exit(0);
	}
}

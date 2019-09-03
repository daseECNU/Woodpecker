package edu.ecnu.woodpecker.tools;

import java.io.FileNotFoundException;

public class Server
{	
	public static void main(String[] args) throws FileNotFoundException {
		//		//开启服务端监听
		new Thread(new NettyServer(args[0])).start();
		try {
			Thread.sleep(Integer.parseInt(args[1])*1000);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
//				
	}
}

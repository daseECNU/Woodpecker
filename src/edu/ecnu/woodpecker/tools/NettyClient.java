package edu.ecnu.woodpecker.tools;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class NettyClient implements Runnable{
	private String host;
	private String port;
	private Bootstrap b = null;
	private static Channel channel = null;
	private static volatile boolean allWritable = false;
	public NettyClient(String host, String port) {
		super();
		this.host = host;
		this.port = port;
		try {
			connect();
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}

    public void connect() throws Exception 
    {
		// 配置客户端NIO线程组
		EventLoopGroup group = new NioEventLoopGroup();
	    b = new Bootstrap();
	    b.group(group).channel(NioSocketChannel.class)
		    .option(ChannelOption.TCP_NODELAY, true)
		    .handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch)
				throws Exception {
			    //处理半包
				ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
				//protobuf编解码
				ch.pipeline().addLast(new ProtobufDecoder(MessageProto.
						Message.getDefaultInstance()));
				ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
				ch.pipeline().addLast(new ProtobufEncoder());
				//IO事件的处理类
			    ch.pipeline().addLast(new NettyClientHandler());
			}
		    });
	    //发起异步链接操作
    	try{
    		channel = b.connect(host, Integer.parseInt(port)).sync().channel();
    		allWritable = true;
    	}catch(Exception e){	
    		//System.out.println("@@@@@@Channel#######not open");
    	} 
    }
  	public static void WorkOrder(MessageProto.Message workOrder) {
		if(channel!=null&&NettyClient.isAllWritable())
		{
			channel.writeAndFlush(workOrder);   
		}
  	}
  	
    public void run() {
		while(true) {
			try {
				Thread.sleep(1000);
				if(channel==null) {
					allWritable = false;
					channel = b.connect(host, Integer.
							parseInt(port)).sync().channel();
					allWritable = true;
					System.out.println("###Channel#######reopen");
				}
			} catch (Exception e) {
			}
		}
	}

	public static boolean isAllWritable() {
		return allWritable;
	}
}

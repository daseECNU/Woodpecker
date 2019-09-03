package edu.ecnu.woodpecker.tools;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyServerHandler extends ChannelInboundHandlerAdapter 
{
	public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
    	try
		{
    		MessageProto.Message message = (MessageProto.Message) msg;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) 
    {
		cause.printStackTrace();
		ctx.close();
    }
}

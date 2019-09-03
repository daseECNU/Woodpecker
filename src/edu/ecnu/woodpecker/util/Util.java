package edu.ecnu.woodpecker.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.log.WpLog;
import expectj.ExpectJ;
import expectj.Spawn;
import expectj.TimeoutException;

public class Util
{
    /**
     * Remove all blank element in array, include 0 length and null
     * 
     * @param arrays
     * @return
     */
    public static String[] removeBlankElement(String[] arrays)
    {
        Stream<String> stream = Stream.of(arrays);
        return stream.filter(ele -> ele != null).map(ele -> ele.trim()).filter(ele -> ele.length() != 0)
                .collect(() -> new ArrayList<>(), (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2))
                .toArray(new String[0]);
    }

    /**
     * Remote execute shell command
     * 
     * @param host The IP of specified host
     * @param user 账户名
     * @param psw 密码
     * @param port 端口，SSH连接默认22
     * @param command 命令
     * @return 命令的返回结果字符串
     */
    public static String exec(String host, String user, String psw, int port, String command)
    {
        StringBuilder result = new StringBuilder();
        Session session = null;
        ChannelExec openChannel = null;
        try
        {
            JSch jsch = new JSch();

            session = jsch.getSession(user, host, port);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(psw);
            int timeout = 60000000;
            session.setTimeout(timeout);
            session.connect();

            openChannel = (ChannelExec) session.openChannel("exec");
            openChannel.setCommand(command);
            openChannel.connect();
            InputStream in = openChannel.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String buf = null;
            while ((buf = reader.readLine()) != null)
            {
                result.append(new String(buf.getBytes("gbk"), FileConstant.UTF_8)).append(FileConstant.LINUX_LINE_FEED);
            }
            reader.close();
        }
        catch (JSchException | IOException e)
        {
            result.append(WpLog.getExceptionInfo(e));
        }
        finally
        {
            if (openChannel != null && !openChannel.isClosed())
                openChannel.disconnect();
            if (session != null && session.isConnected())
                session.disconnect();
        }
        return result.toString();
    }

    /**
     * 用于服务器间上传文件
     * 
     * @param host The IP of specified host
     * @param user 账户名
     * @param psw 密码
     * @param port 端口，SSH连接默认22
     */
    public static void put(String host, String user, String psw, int port, String src, String dst)
    {
        Session session = null;
        ChannelSftp openChannel = null;
        try
        {
            JSch jsch = new JSch();

            session = jsch.getSession(user, host, port);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(psw);
            int timeout = 60000000;
            session.setTimeout(timeout);
            session.connect();

            openChannel = (ChannelSftp) session.openChannel("sftp");
            openChannel.connect();
			openChannel.put(src, dst, ChannelSftp.OVERWRITE);
        }
        catch (JSchException | SftpException e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            e.printStackTrace();
        }
        finally
        {
            if (openChannel != null && !openChannel.isClosed())
                openChannel.disconnect();
            if (session != null && session.isConnected())
                session.disconnect();
        }
        return;
    }
    
    /**
     * 用于服务器间复制文件
     * 
     * @param srcHost 发起复制命令的服务器IP
     * @param port
     * @param user 两台服务器用户名一致
     * @param psw 目标服务器密码
     * @param command 复制文件的具体shell命令
     * @param destHost The IP of target host
     */
    public static void scp(String srcHost, int port, String user, String psw, String command,
            String destHost)
    {
        // false表示不将运行过程中服务器返回的信息输出到控制台
        ExpectJ expect = new ExpectJ(60, false);
        Spawn shell = null;
        try
        {
            shell = expect.spawn(srcHost, port, user, psw);
            shell.send(command + FileConstant.WIN_LINE_FEED_STR);
            try
            {
                // 有可能需要判断是否连接，没有的话等超时继续执行输入密码
                shell.expect("Are you sure you want to continue connecting (yes/no)?", 2);
                shell.send("yes\n");
            }
            catch (TimeoutException e)
            {
                // 超时说明不需要判断连接
                String[] tmp = shell.getCurrentStandardOutContents().split(
                        FileConstant.WIN_LINE_FEED_STR);
                // if (tmp[tmp.length - 1].trim().equals(user + SignConstant.AT + destHost + "'s password:"))
                if (tmp[tmp.length - 1].trim().equals(String.format("%s@%s's password:", user, destHost)))
                {
                    // 没有配置免密码登录
                    WpLog.recordLog(LogLevelConstant.DEBUG, tmp[tmp.length - 1].trim());
                    shell.send(psw + "\n");
                }
                return;
            }

            shell.expect(String.format("%s@%s's password:", user, destHost));
            // shell.expect(user + SignConstant.AT + destHost + "'s password:");
            shell.send(psw + FileConstant.WIN_LINE_FEED_STR);
        }
        catch (IOException | TimeoutException e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            e.printStackTrace();
        }
    }

    /**
     * Return local host IP address, which is not one of
     * site local, link local, virtual, loopback
     * 
     * @return
     * @throws SocketException
     */
    public static Optional<String> getLocalHostAddress() throws SocketException
    {
        for (Enumeration<NetworkInterface> localNetworkInterfaces = NetworkInterface.getNetworkInterfaces(); localNetworkInterfaces
                .hasMoreElements();)
        {
            NetworkInterface networkInterface = localNetworkInterfaces.nextElement();
            if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp())
                continue;
            for (Enumeration<InetAddress> inetAddrEnum = networkInterface.getInetAddresses(); inetAddrEnum.hasMoreElements();)
            {
                try
                {
                    Inet4Address inetAddress = (Inet4Address) inetAddrEnum.nextElement();
                    if (!inetAddress.isSiteLocalAddress() && !inetAddress.isLinkLocalAddress())
                        return Optional.of(inetAddress.getHostAddress());
                }
                catch (Exception e)
                {}
            }
        }
        return Optional.empty();
    }
}

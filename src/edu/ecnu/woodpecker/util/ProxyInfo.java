package edu.ecnu.woodpecker.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;

import edu.ecnu.woodpecker.constant.DataValueConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.log.WpLog;

/**
 * Keep the proxy info
 *
 */
public class ProxyInfo
{
    private static ProxyType proxyType = null;
    private static String proxyHost = null;
    private static int proxyPort;
    private static String user = null;
    private static String password = null;

    private ProxyInfo()
    {}

    // Single instance pattern
    private static class ProxyBeanHolder
    {
        private static ProxyInfo instance = new ProxyInfo();
    }

    public static ProxyInfo getInstance()
    {
        return ProxyBeanHolder.instance;
    }

    /**
     * Set proxy for whole project
     */
    public static void setGlobalProxyServer() throws Exception
    {
        Properties properties = System.getProperties();
        switch (proxyType)
        {
        case SOCKS5:
            properties.setProperty("socksProxyHost", proxyHost);
            properties.setProperty("socksProxyPort", String.valueOf(proxyPort));
            break;
        case SOCKS4:
            properties.setProperty("socksProxyHost", proxyHost);
            properties.setProperty("socksProxyPort", String.valueOf(proxyPort));
            break;
        default:
            WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported proxy type: %s", proxyType);
            throw new Exception();
        }
        if (user != null && password != null)
            Authenticator.setDefault(new MyAuthenticator(user, password));
    }

    static class MyAuthenticator extends Authenticator
    {
        private String user = null;
        private String password = null;

        public MyAuthenticator(String user, String password)
        {
            this.user = user;
            this.password = password;
        }

        public PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(user, password.toCharArray());
        }
    }
    /**
     * ProxyType is empty, return true
     * 
     * @return
     */
    public static boolean isEmpty()
    {
        return proxyType == null ? true : false;
    }

    /**
     * Return a Proxy object according to ProxyInfo's field
     * 
     * @return
     */
    public static Proxy getProxy() throws Exception
    {
        Proxy proxy = null;
        switch (proxyType)
        {
        case SOCKS5:
            proxy = new ProxySOCKS5(proxyHost, proxyPort);
            if (user != null && password != null)
                ((ProxySOCKS5) proxy).setUserPasswd(user, password);
            break;
        case SOCKS4:
            proxy = new ProxySOCKS4(proxyHost, proxyPort);
            if (user != null && password != null)
                ((ProxySOCKS4) proxy).setUserPasswd(user, password);
            break;
        default:
            WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported proxy type: %s", proxyType);
            throw new Exception();
        }
        return proxy;
    }

    public ProxyType getProxyType()
    {
        return proxyType;
    }

    public static void setProxyType(String proxyType)
    {
        ProxyInfo.proxyType = ProxyType.of(proxyType);
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public static void setProxyHost(String proxyHost)
    {
        ProxyInfo.proxyHost = proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public static void setProxyPort(String proxyPort)
    {
        ProxyInfo.proxyPort = Integer.parseInt(proxyPort);
    }

    public String getUser()
    {
        return user;
    }

    public static void setUser(String user)
    {
        if (!user.equals(DataValueConstant.NULL_LOWER) && !user.equals(DataValueConstant.NULL_UPPER))
            ProxyInfo.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public static void setPassword(String password)
    {
        if (!password.equals(DataValueConstant.NULL_LOWER) && !password.equals(DataValueConstant.NULL_UPPER))
            ProxyInfo.password = password;
    }
}
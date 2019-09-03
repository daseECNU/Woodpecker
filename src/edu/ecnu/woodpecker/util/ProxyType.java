package edu.ecnu.woodpecker.util;

import java.util.stream.Stream;

public enum ProxyType
{
    SOCKS5("socks5"), SOCKS4("socks4");

    private String value = null;

    private ProxyType(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
    
    public String toString()
    {
        return value;
    }

    /**
     * Return a ProxyType object according specified value
     * 
     * @param value Ignore upper case or lower case
     * @return
     */
    public static ProxyType of(String value)
    {
        String trimValue = value.trim().toLowerCase();
        Stream<ProxyType> stream = Stream.of(ProxyType.class.getEnumConstants());
        return stream.filter(ele -> ele.getValue().equals(trimValue)).findAny().get();
    }
}
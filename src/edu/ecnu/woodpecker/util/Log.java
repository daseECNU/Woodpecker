package edu.ecnu.woodpecker.util;

/**
 * 与日志相关的类
 */
public final class Log
{
    /**
     * 返回调用函数所在类，函数名及文件行号
     * 
     * @return
     */
    public static String getRecordMetadata()
    {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        return String.format("%s->%s:%d", element.getClassName(), element.getMethodName(), element.getLineNumber());
    }
}

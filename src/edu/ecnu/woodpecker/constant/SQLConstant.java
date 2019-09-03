package edu.ecnu.woodpecker.constant;

/**
 * 涉及SQL的关键字、操作等常量
 *
 */
public final class SQLConstant
{
    // 事务开始和提交
    public final static String START = "start";
    public final static String COMMIT = "commit";
    public final static String ROLLBACK = "rollback";
    // 存储过程参数输入和输出类型
    public final static String IN = "in";
    public final static String OUT = "out";
    public final static String IN_OUT = "inout";
    // MySQL中执行功能测试的数据库名
    public final static String TEST_DB = "woodpecker";
}

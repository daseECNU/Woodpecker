package edu.ecnu.woodpecker.systemfunction;

import java.sql.Connection;
import java.sql.Statement;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SQLConstant;
import edu.ecnu.woodpecker.controller.CedarOperation;
import edu.ecnu.woodpecker.controller.clusterinfo.ClustersInfo;
import edu.ecnu.woodpecker.log.Recorder;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.sql.DbmsBrand;
import edu.ecnu.woodpecker.util.Log;
import edu.ecnu.woodpecker.util.Util;

public class Shell
{

    /**
     * 暂停当前线程指定的时间
     * 
     * @param time 时间
     * @param timeUnit 时间单位
     */
    public static void sleep(int time, String timeUnit)
    {
        if (timeUnit.equals("microsecond"))
        {
            time = time / 1000;
            sleep(time);
        }
        else if (timeUnit.equals("millisecond"))
        {
            sleep(time);
        }
        else if (timeUnit.equals("second"))
        {
            time = time * 1000;
            sleep(time);
        }
        else if (timeUnit.equals("minute"))
        {
            time = time * 1000 * 60;
            sleep(time);
        }
        else if (timeUnit.equals("hour"))
        {
            time = time * 1000 * 60 * 60;
            sleep(time);
        }
        else
        {
            System.out.println("单位错误");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "time unit's input is wrong",
                    LogLevelConstant.ERROR);
        }
    }

    /**
     * 表示在指定ip地址上执行shell命令，默认路径是用户主目录。
     * 
     * @param command shell命令
     * @param ip ip地址
     */
    public static void shellCommand(String command, String ip)
    {

        Util.exec(ip, ClustersInfo.getUserName(), ClustersInfo.getPassword(),
                ClustersInfo.getConnectionPort(), command);

    }

    private static void sleep(int time)
    {
        try
        {
            Thread.sleep(time);
            Recorder.FunctionRecord(Log.getRecordMetadata(), "SLEEP " + time, LogLevelConstant.INFO);
        }
        catch (InterruptedException e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
        }
    }

    /**
     * 建表是否成功
     * 
     * @param IP
     * @param port
     * @return
     * @throws Exception
     */
    public static boolean isCreateTable(String IP, String port)
    {
        Connection conn=null;
        try
        {

            conn = BasicSQLOperation.getConnection(IP, port, SQLConstant.TEST_DB,
                    CedarOperation.getDatabaseUser(), CedarOperation.getDatabasePassword(),
                    DbmsBrand.CEDAR);
            Statement stmt = BasicSQLOperation.getStatement(conn);
            // 建表
            String sql1="set @@session.ob_query_timeout=9000000000;";
            BasicSQLOperation.stmtExecuteUpdate(stmt, sql1, false);
            String sql = "create table woodpecker_test_pyc (id int primary key , c1 int,c2 varchar(20))";
            BasicSQLOperation.stmtExecuteUpdate(stmt, sql, false);
            stmt.close();
            conn.close();
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "%s %s", IP, WpLog.getExceptionInfo(e));
        }

        return true;
    }

    /**
     * 插入删除是否成功
     * 
     * @param IP
     * @param port
     * @return
     */
    public static boolean isUpdateTable(String IP, String port)
    {
        Connection conn;
        int rows = -1;
        try
        {
            conn = BasicSQLOperation.getConnection(IP, port, SQLConstant.TEST_DB,
                    CedarOperation.getDatabaseUser(), CedarOperation.getDatabasePassword(),
                    DbmsBrand.CEDAR);
            Statement stmt = BasicSQLOperation.getStatement(conn);
            // 插入数据
            String sql1="set @@session.ob_query_timeout=9000000000;";
            BasicSQLOperation.stmtExecuteUpdate(stmt, sql1, false);
            String sql = "replace into woodpecker_test_pyc values(1,1,'aaaa')";
            rows = BasicSQLOperation.stmtExecuteUpdate(stmt, sql, false);
            stmt.close();
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "%s %s", IP, WpLog.getExceptionInfo(e));
        }
        if (rows != 1)
        {
            return false;
        }

        rows = -1;
        try
        {
            conn = BasicSQLOperation.getConnection(IP, port, SQLConstant.TEST_DB,
                    CedarOperation.getDatabaseUser(), CedarOperation.getDatabasePassword(),
                    DbmsBrand.CEDAR);
            String sql = "delete from woodpecker_test_pyc where id = 1";
            Statement stmt1 = BasicSQLOperation.getStatement(conn);
            rows = BasicSQLOperation.stmtExecuteUpdate(stmt1, sql, false);
            stmt1.close();
            conn.close();
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "%s %s", IP, WpLog.getExceptionInfo(e));
        }
        if (rows != 1)
        {
            return false;
        }
        return true;
    }

    /**
     * 判断删表是否成功
     * 
     * @param IP
     * @param port
     * @return
     */
    public static boolean isDeleteTable(String IP, String port)
    {
        Connection conn;
        int rows = -1;
        try
        {
            conn = BasicSQLOperation.getConnection(IP, port, SQLConstant.TEST_DB,
                    CedarOperation.getDatabaseUser(), CedarOperation.getDatabasePassword(),
                    DbmsBrand.CEDAR);
            Statement stmt = BasicSQLOperation.getStatement(conn);
            // 删表
            String sql1="set @@session.ob_query_timeout=9000000000;";
            BasicSQLOperation.stmtExecuteUpdate(stmt, sql1, false);
            String sql = "drop table woodpecker_test_pyc ";
            rows = BasicSQLOperation.stmtExecuteUpdate(stmt, sql, false);
            stmt.close();
            conn.close();
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "%s %s", IP, WpLog.getExceptionInfo(e));
        }
        if (rows == 0)
        {
            return true;
        }
        return false;
    }

}

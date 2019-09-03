package edu.ecnu.woodpecker.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.ResultSupport;

import com.alipay.oceanbase.OBGroupDataSource;

import edu.ecnu.woodpecker.constant.DataType;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.ProcedureParameterIO;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.log.WpLog;

/**
 * 支持SQL功能测试 主要功能：获取数据库连接，获取SQL和存储过程的执行器， 执行SQL和存储过程，事务控制，数据库实例的导入和删除
 */
public class BasicSQLOperation
{

    /**
     * 考虑到建立不同数据库连接的方式不同以及引用的jar包不同，这里还有需要讨论的地方
     * 
     * @param IP 数据库服务IP
     * @param port 数据库服务端口
     * @param dbName 数据库实例名称 （对于CEDAR则不使用）
     * @param userName 用户名
     * @param password 密码
     * @param dbType 数据库管理系统类型
     * @return Connection 数据库连接
     */
    private static Map<Connection, String> connExists=new HashMap<>();
    public static Connection getConnection(String IP, String port, String dbName, String userName, String password, DbmsBrand dbType)
            throws Exception
    {
        Connection conn = null;
        switch (dbType)
        {
        case CEDAR_DATA_SOURCE:
            Map<String, String> confParas = new HashMap<String, String>();
            confParas.put("username", userName);
            confParas.put("password", password);
            confParas.put("clusterAddress", IP + SignConstant.COLON_CHAR + port);
            OBGroupDataSource obGroupDataSource = new OBGroupDataSource();
            obGroupDataSource.setDataSourceConfig(confParas);
            obGroupDataSource.init();
            conn = obGroupDataSource.getConnection();
            break;
        case CEDAR:
            String driver = "com.mysql.jdbc.Driver";
            String URL = "jdbc:mysql://" + IP + SignConstant.COLON_CHAR + port + "/mysql?useServerPrepStmts=true";
            Class.forName(driver);
            conn = DriverManager.getConnection(URL, userName, password);
            break;
        case MYSQL:
            URL = "jdbc:mysql://" + IP + SignConstant.COLON_CHAR + port + "/" + dbName;
            try
            {
                conn = DriverManager.getConnection(URL, userName, password);
            }
            catch (SQLException e)
            {
                if (e.getMessage().matches("Unknown database.*"))
                {
                    // 默认是woodpecker库，没有库则建库
                    conn = DriverManager.getConnection("jdbc:mysql://" + IP + SignConstant.COLON_CHAR + port, userName, password);
                    Statement statement = conn.createStatement();
                    statement.executeUpdate("create database " + dbName);
                    statement.executeQuery("use " + dbName);
                }
                else
                    throw e;
            }
            break;
        case POSTGRESQL:
            driver = "org.postgresql.Driver";
            URL = "jdbc:postgresql://" + IP + SignConstant.COLON_CHAR + port + "/" + dbName;
            Class.forName(driver);
            conn = DriverManager.getConnection(URL, userName, password);
        default:
            WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported DBMS: %s", dbType);
            throw new Exception("Unsupported DBMS");
        }

        return conn;
    }

    /**
     * 建立一个普通执行器 Statement
     * 
     * @param conn 数据库连接
     * @return 返回一个普通执行器 Statement
     */
    public static Statement getStatement(Connection conn) throws Exception
    {
        return conn.createStatement();
    }

    /**
     * 建立一个预编译的执行器 PreparedStatement
     * 
     * @param conn 数据库连接
     * @param sql 待预编译的SQL
     * @return 返回一个预编译的执行器 PreparedStatement
     */
    public static PreparedStatement getPreparedStatement(Connection conn, String sql) throws Exception
    {
        return conn.prepareStatement(sql);
    }

    /**
     * 建立一个存储过程执行器 CallableStatement
     * 
     * @param conn 数据库连接
     * @param procedure 存储过程名
     * @return 返回一个存储过程执行器 CallableStatement
     */
    public static CallableStatement getCallableStatement(Connection conn, String procedure) throws Exception
    {
        return conn.prepareCall(procedure);
    }

    /**
     * 使用普通执行器执行一个SQL语句，有返回结果集
     * 
     * @param stmt 普通执行器
     * @param sql 待执行SQL语句
     * @param hasException 是否会抛出异常
     * @return 返回结果集
     */
    public static Result stmtExecuteQuery(Statement stmt, String sql, boolean hasException) throws Exception
    {
        if (hasException)
        {
            try
            {
                stmt.executeQuery(sql);
            }
            catch (Exception e)
            {
                // 意料之中的异常，记录日志INFO
                WpLog.recordLog(LogLevelConstant.INFO, "Expected exception in statement query");
                Executor.setExceptionString(e.toString());
                return null;
            }
            // 上面一句能正确执行说明该抛异常却没抛，表示案例错误
            WpLog.recordLog(LogLevelConstant.ERROR, "It should fail but succeed");
            throw new Exception("there is no expected exception");
        }
        else
        {
            try
            {
                return ResultSupport.toResult(stmt.executeQuery(sql));
            }
            catch (SQLException e)
            {
                // 不该有异常却抛异常，有错误此案例错误，记录日志
                WpLog.recordLog(LogLevelConstant.ERROR, "Unexpected exception in statement query");
                throw new Exception(WpLog.getExceptionInfo(e));
            }
        }
    }

    /**
     * 使用普通执行器执行一个SQL语句，无返回结果集
     * 
     * @param stmt 普通执行器
     * @param sql 待执行SQL语句
     * @param hasException 是否会抛出异常
     * @return 影响行数
     */
    public static int stmtExecuteUpdate(Statement stmt, String sql, boolean hasException) throws Exception
    {
        if (hasException)
        {
            try
            {
                stmt.executeUpdate(sql);
            }
            catch (Exception e)
            {
                // 意料之中的异常，记录日志INFO
                WpLog.recordLog(LogLevelConstant.INFO, "expected exception in statement update");
                Executor.setExceptionString(e.toString());
                return -1;
            }
            // 上面一句能正确执行说明该抛异常却没抛，表示案例错误
            WpLog.recordLog(LogLevelConstant.ERROR, "it should fail but succeed");
            throw new Exception("there is no expected exception");
        }
        else
        {
            try
            {
                return stmt.executeUpdate(sql);
            }
            catch (SQLException e)
            {
                // 不该有异常却抛异常，有错误此案例错误，记录日志
                WpLog.recordLog(LogLevelConstant.ERROR, "unexpected exception in statement update");
                throw new Exception(WpLog.getExceptionInfo(e));
            }
        }
    }

    /**
     * 使用预编译执行器执行一个SQL语句，有返回结果集
     * 
     * @param pstmt 预编译执行器
     * @param dataTypes 需要设置的参数 数据类型
     * @param objects 需要设置的参数具体数值，因为各个参数数据类型可能不一样，故采用Object类型
     * @return 返回结果集
     */
    public static Result pstmtExecuteQuery(PreparedStatement pstmt, DataType[] dataTypes, boolean hasException, Object... objects)
            throws Exception
    {
        if (hasException)
        {
            try
            {
                if (dataTypes != null && objects != null)
                    setParameters(pstmt, dataTypes, objects);
                pstmt.executeQuery();
            }
            catch (Exception e)
            {
                // 意料之中的异常，记录日志INFO
                WpLog.recordLog(LogLevelConstant.INFO, "expected exception in prepared statement query");
                Executor.setExceptionString(e.toString());
                return null;
            }
            // 上面一句能正确执行说明该抛异常却没抛，表示案例错误
            WpLog.recordLog(LogLevelConstant.ERROR, "it should fail but succeed");
            throw new Exception("there is no expected exception");
        }
        else
        {
            try
            {
                // 向预编译执行器中设置各个参数数值
                if (dataTypes != null && objects != null)
                    setParameters(pstmt, dataTypes, objects);
                return ResultSupport.toResult(pstmt.executeQuery());
            }
            catch (SQLException e)
            {
                // 不该有异常却抛异常，有错误此案例错误，记录日志
                WpLog.recordLog(LogLevelConstant.ERROR, "unexpected exception in prepared statement query");
                throw new Exception(WpLog.getExceptionInfo(e));
            }
        }
    }

    /**
     * 使用预编译执行器执行一个SQL语句，无返回结果集
     * 
     * @param pstmt 预编译执行器
     * @param dataTypes 需要设置的参数 数据类型
     * @param objects 需要设置的参数具体数值
     * @return 影响行数
     */
    public static int pstmtExecuteUpdate(PreparedStatement pstmt, DataType[] dataTypes, boolean hasException, Object... objects) throws Exception
    {
        if (hasException)
        {
            try
            {
                // 向预编译执行器中设置各个参数数值
                if (dataTypes != null && objects != null)
                    setParameters(pstmt, dataTypes, objects);
                pstmt.executeUpdate();
            }
            catch (Exception e)
            {
                // 意料之中的异常，记录日志INFO
                WpLog.recordLog(LogLevelConstant.INFO, "expected exception in prepared statement update");
                Executor.setExceptionString(e.toString());
                return -1;
            }
            // 上面一句能正确执行说明该抛异常却没抛，表示案例错误
            WpLog.recordLog(LogLevelConstant.ERROR, "it should fail but succeed");
            throw new Exception("there is no expected exception");
        }
        else
        {
            try
            {
                // 向预编译执行器中设置各个参数数值
                if (dataTypes != null && objects != null)
                    setParameters(pstmt, dataTypes, objects);
                return pstmt.executeUpdate();
            }
            catch (SQLException e)
            {
                // 不该有异常却抛异常，有错误此案例错误，记录日志
                WpLog.recordLog(LogLevelConstant.ERROR, "unexpected exception in prepared statement update");
                throw new Exception(WpLog.getExceptionInfo(e));
            }
        }
    }

    /**
     * 使用存储过程执行器执行一个存储过程，有返回结果集
     * 
     * @param cstmt 存储过程执行器
     * @param dataTypes 需要设置的参数 数据类型
     * @param paraTypes 需要设置的参数 输入输出类型（in、out、inOut）
     * @param objects 需要设置的参数的具体值
     * @param parameters 需要设置的参数名
     * @return 返回结果集
     */
    public static Result cstmtExecuteQuery(CallableStatement cstmt, DataType[] dataTypes, ProcedureParameterIO[] paraTypes, Object[] objects,
            String[] parameterNames) throws Exception
    {
        if (dataTypes != null && objects != null)
        {
            // 向存储过程执行器中设置各个参数数值
            setParameters(cstmt, dataTypes, paraTypes, objects);
            // 对存储过程的out和inOut参数进行注册
            registerParameters(cstmt, dataTypes, paraTypes, objects);
        }
        // 执行存储过程
        Result result = ResultSupport.toResult(cstmt.executeQuery());
        if (dataTypes != null && objects != null)
        {
            // 获取存储过程的out和inOut参数
            getParameters(cstmt, dataTypes, paraTypes, parameterNames);
        }
        return result;
    }

    /**
     * 使用存储过程执行器执行一个存储过程，无返回结果集
     * 
     * @param cstmt 存储过程执行器
     * @param dataTypes 需要设置的参数 数据类型
     * @param paraTypes 需要设置的参数 输入输出类型（in、out、inOut）
     * @param objects 需要设置的参数的具体值
     * @param parameterNames 需要设置的参数名
     * @return 影响行数
     */
    public static int cstmtExecuteUpdate(CallableStatement cstmt, DataType[] dataTypes, ProcedureParameterIO[] paraTypes, Object[] objects,
            String[] parameterNames) throws Exception
    {
        if (dataTypes != null && objects != null)
        {
            // 向存储过程执行器中设置各个参数数值
            setParameters(cstmt, dataTypes, paraTypes, objects);
            // 对存储过程的out和inOut参数进行注册
            registerParameters(cstmt, dataTypes, paraTypes, objects);
        }
        // 执行存储过程
        int rows = cstmt.executeUpdate();
        if (dataTypes != null && objects != null)
        {
            // 获取存储过程的out和inOut参数
            getParameters(cstmt, dataTypes, paraTypes, parameterNames);
        }
        return rows;
    }

    /**
     * 事务控制
     * 
     * @param connection 数据库连接
     * @param operator 事务操作语义
     */
    public static void runTransaction(Connection connection, TransactionOperator operator) throws Exception
    {
        switch (operator)
        {
        case START:
            connection.setAutoCommit(false);
            if(connExists.containsKey(connection))
            {
                throw new Exception("ERROR");
            }
            connExists.put(connection, connection.toString());
            //connection.commit();
            break;
        case COMMIT:
            connExists.remove(connection);
            connection.commit();
            connection.setAutoCommit(true);
            break;
        case ROLLBACK:
            connExists.remove(connection);
            connection.rollback();
            connection.setAutoCommit(true);
            break;
        default:
            WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported transaction operator: %s", operator);
            throw new Exception("Unsupported transaction operator");
        }
    }

    /**
     * 导入一个数据库实例 文件中分为两部分：第一部分为建表和插入数据的SQL语句；第二部分为删表SQL语句。
     * 两部分之间的分隔符为“[-]+”，SQL语句之间的分隔符为“\n”或者“\r\n” 执行第一部分SQL
     * 
     * @param file 数据源文件
     * @param encodingFormat 文件编码类型
     * @param conn 数据库链接
     * @return 导入成功则返回True，否则返回False
     */
    public static boolean importDBI(File file, String encodingFormat, Connection conn) throws Exception
    {
        boolean result = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encodingFormat));
        String inputLine = null;
        Statement stmt = conn.createStatement();
        while ((inputLine = reader.readLine()) != null)
        {
            if (inputLine.matches("(\\s*)|(\\s*#.*)"))
            {
                // 空行或者注释
                continue;
            }

            if (inputLine.matches("[-]+"))
                break;
            stmt.executeUpdate(inputLine);
        }
        result = true;

        if (reader != null)
            reader.close();
        return result;
    }

    /**
     * 删除一个数据库实例 文件中分为两部分：第一部分为建表和插入数据的SQL语句；第二部分为删表SQL语句。
     * 两部分之间的分隔符为“[-]+”，SQL语句之间的分隔符为“\n”或者“\r\n” 执行第二部分SQL
     * 
     * @param file 数据源文件
     * @param encodingFormat 文件编码类型
     * @param conn 数据库链接
     * @return 删除成功则返回True，否则返回False
     */
    public static boolean clearDBI(File file, String encodingFormat, Connection conn) throws Exception
    {
        boolean result = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encodingFormat));
        String inputLine = null;
        Statement stmt = conn.createStatement();
        boolean flag = false;
        while ((inputLine = reader.readLine()) != null)
        {
            if (inputLine.matches("(\\s*)|(\\s*#.*)"))
            {
                // 空行或者注释
                continue;
            }

            if (inputLine.matches("[-]+"))
            {
                flag = true;
                continue;
            }

            if (flag)
                stmt.executeUpdate(inputLine);
        }
        result = true;

        if (reader != null)
            reader.close();

        return result;
    }

    /**
     * 向预编译执行器中设置各个参数数值
     * 
     * @param pstmt 预编译执行器
     * @param dataTypes 需要设置的参数
     * @param objects 需要设置的参数具体数值
     */
    private static void setParameters(PreparedStatement pstmt, DataType[] dataTypes, Object[] objects) throws Exception
    {
        for (int i = 0; i < dataTypes.length; i++)
        {
            switch (dataTypes[i])
            {
            case INT:
                pstmt.setInt(i + 1, Integer.parseInt(objects[i].toString()));
                break;
            case LONG:
                pstmt.setLong(i + 1, Long.parseLong(objects[i].toString()));
                break;
            case FLOAT:
                pstmt.setFloat(i + 1, Float.parseFloat(objects[i].toString()));
                break;
            case DOUBLE:
                pstmt.setDouble(i + 1, Double.parseDouble(objects[i].toString()));
                break;
            case STRING:
                pstmt.setString(i + 1, objects[i].toString());
                break;
            case DECIMAL:
                pstmt.setBigDecimal(i + 1, new BigDecimal(objects[i].toString()));
                break;
            case BOOLEAN:
                pstmt.setBoolean(i + 1, Boolean.parseBoolean(objects[i].toString()));
                break;
            case TIMESTAMP:
                pstmt.setTimestamp(i + 1, Timestamp.valueOf(objects[i].toString()));
                break;
            default:
                WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported data type: %s", dataTypes[i]);
                throw new Exception(String.format("Unsupported data type: %s", dataTypes[i]));
            }
        }
    }

    /**
     * 向存储过程执行器中设置各个参数数值
     * 
     * @param cstmt 存储过程执行器
     * @param dataTypes 需要设置的参数
     * @param paraTypes 需要设置的参数 输入输出类型（in、out、inOut）
     * @param objects 需要设置的参数具体数值
     */
    private static void setParameters(CallableStatement cstmt, DataType[] dataTypes, ProcedureParameterIO[] paraTypes, Object[] objects) throws Exception
    {
        // 只有in、inOut类型参数需要设置数值
        for (int i = 0; i < dataTypes.length
                && (paraTypes[i] == ProcedureParameterIO.IN || paraTypes[i] == ProcedureParameterIO.IN_OUT); i++)
        {
            switch (dataTypes[i])
            {
            case INT:
                cstmt.setInt(i + 1, Integer.parseInt(objects[i].toString()));
                break;
            case LONG:
                cstmt.setLong(i + 1, Long.parseLong(objects[i].toString()));
                break;
            case FLOAT:
                cstmt.setFloat(i + 1, Float.parseFloat(objects[i].toString()));
                break;
            case DOUBLE:
                cstmt.setDouble(i + 1, Double.parseDouble(objects[i].toString()));
                break;
            case STRING:
                cstmt.setString(i + 1, objects[i].toString());
                break;
            case DECIMAL:
                cstmt.setBigDecimal(i + 1, new BigDecimal(objects[i].toString()));
                break;
            case BOOLEAN:
                cstmt.setBoolean(i + 1, Boolean.parseBoolean(objects[i].toString()));
                break;
            case TIMESTAMP:
                cstmt.setTimestamp(i + 1, Timestamp.valueOf(objects[i].toString()));
                break;
            default:
                WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported data type: %s", dataTypes[i]);
                throw new Exception(String.format("Unsupported data type: %s", dataTypes[i]));
            }
        }
    }

    /**
     * 对存储过程的out和inOut参数进行注册
     * 
     * @param cstmt 存储过程执行器
     * @param dataTypes 需要设置的参数
     * @param paraTypes 需要设置的参数 输入输出类型（in、out、inOut）
     * @param objects 需要设置的参数具体数值
     */
    private static void registerParameters(CallableStatement cstmt, DataType[] dataTypes, ProcedureParameterIO[] paraTypes, Object[] objects) throws Exception
    {
        // 只有out、inOut类型参数需要进行注册
        for (int i = 0; i < dataTypes.length; i++)
        {
            if (paraTypes[i] == ProcedureParameterIO.OUT || paraTypes[i] == ProcedureParameterIO.IN_OUT)
            {
                switch (dataTypes[i])
                {
                case INT:
                    cstmt.registerOutParameter(i + 1, Types.INTEGER);
                    break;
                case LONG:
                    cstmt.registerOutParameter(i + 1, Types.BIGINT);
                    break;
                case FLOAT:
                    cstmt.registerOutParameter(i + 1, Types.FLOAT);
                    break;
                case DOUBLE:
                    cstmt.registerOutParameter(i + 1, Types.DOUBLE);
                    break;
                case STRING:
                    cstmt.registerOutParameter(i + 1, Types.VARCHAR);
                    break;
                case DECIMAL:
                    cstmt.registerOutParameter(i + 1, Types.DECIMAL);
                    break;
                case BOOLEAN:
                    cstmt.registerOutParameter(i + 1, Types.BOOLEAN);
                    break;
                case TIMESTAMP:
                    cstmt.registerOutParameter(i + 1, Types.TIMESTAMP);
                    break;
                default:
                    WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported data type: %s", dataTypes[i]);
                    throw new Exception(String.format("Unsupported data type: %s", dataTypes[i]));
                }
            }
        }
    }

    /**
     * 获取存储过程的out和inOut参数
     * 
     * @param cstmt 存储过程执行器
     * @param dataTypes 需要设置的参数
     * @param paraTypes 需要设置的参数 输入输出类型（in、out、inOut） 0：in；1：out；2：inOut
     * @param parameterNames 需要设置的参数名
     */
    private static void getParameters(CallableStatement cstmt, DataType[] dataTypes, ProcedureParameterIO[] paraTypes, String[] parameterNames) throws Exception
    {
        Map<String, Object> varValueMap = Executor.getVarValueMap();
        for (int i = 0; i < dataTypes.length; i++)
        {
            // 只有out、inOut类型参数需要进行处理
            if (paraTypes[i] == ProcedureParameterIO.OUT || paraTypes[i] == ProcedureParameterIO.IN_OUT)
            {
                switch (dataTypes[i])
                {
                case INT:
                    varValueMap.put(parameterNames[i], cstmt.getInt(i + 1));
                    break;
                case LONG:
                    varValueMap.put(parameterNames[i], cstmt.getLong(i + 1));
                    break;
                case FLOAT:
                    varValueMap.put(parameterNames[i], cstmt.getFloat(i + 1));
                    break;
                case DOUBLE:
                    varValueMap.put(parameterNames[i], cstmt.getDouble(i + 1));
                    break;
                case STRING:
                    varValueMap.put(parameterNames[i], cstmt.getString(i + 1));
                    break;
                case DECIMAL:
                    varValueMap.put(parameterNames[i], cstmt.getBigDecimal(i + 1));
                    break;
                case BOOLEAN:
                    varValueMap.put(parameterNames[i], cstmt.getBoolean(i + 1));
                    break;
                case TIMESTAMP:
                    varValueMap.put(parameterNames[i], cstmt.getTimestamp(i + 1));
                    break;
                default:
                    WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported data type: %s", dataTypes[i]);
                    throw new Exception(String.format("Unsupported data type: %s", dataTypes[i]));
                }
            }
        }
    }

}

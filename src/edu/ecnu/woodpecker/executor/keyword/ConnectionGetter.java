package edu.ecnu.woodpecker.executor.keyword;

import java.sql.Connection;

import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SQLConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.controller.CedarOperation;
import edu.ecnu.woodpecker.controller.MySQLOperation;
import edu.ecnu.woodpecker.controller.PostgreSQLOperation;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.controller.clusterinfo.ClustersInfo;
import edu.ecnu.woodpecker.controller.clusterinfo.MergeServerInfo;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.sql.DbmsBrand;
import edu.ecnu.woodpecker.util.Util;

/**
 * The class handles get_conn keyword
 *
 */
public class ConnectionGetter extends Executor implements Keyword
{
    public ConnectionGetter()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        switch (type)
        {
        case FIRST_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.WARN, "Need variable to storage connection");
            break;
        case SECOND_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_CONN: %s", keyword);
            // Index 0 is variable name, 1 is keyword
            String[] keywordParts = Util.removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
            handleSecondGrammar(keywordParts[0], keywordParts[1]);
            break;
        case THIRD_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_CONN: %s", keyword);
            // Index 0 is data type and variable name, 1 is keyword
            keywordParts = Util.removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
            // Index 0 is data type, 1 is variable name
            String[] decVar = Util.removeBlankElement(keywordParts[0].split("\\s"));
            handleThirdGrammar(decVar[0], decVar[1], keywordParts[1]);
            break;
        default:
            throw new Exception("Grammar error");
        }
    }

    /**
     * 
     * @param variableName  The name of variable which will be assigned by keyword
     * @param keyword   Doesn't include variableName
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        if (!varValueMap.containsKey(variableName))
        {
            WpLog.recordLog(LogLevelConstant.ERROR,
                    "Use variable without declaring in %s line %d", caseFileName, lineNumber);
            throw new Exception();
        }
        String category = Util.removeBlankElement(keyword.split("\\[|]"))[1];
        //varValueMap.put(variableName, getConnection(category));  update for remove conn and stat in SQL
        Connection connection = getConnection(category);
        varValueMap.put(variableName, connection);
        connMap.put(variableName, connection);
        curConn = connection;
    }

    /**
     * 
     * @param dataType  The data type of variable
     * @param variableName
     * @param keyword
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        String category = Util.removeBlankElement(keyword.split("\\[|]"))[1];
      //varValueMap.put(variableName, getConnection(category));  update for remove conn and stat in SQL
        Connection connection = getConnection(category);
        varValueMap.put(variableName, connection);
        connMap.put(variableName, connection);
        curConn = connection;
    }
    
    /**
     * @param MSCategory The role of MS in cluster
     * @return JDBC connection which establish in specified MS
     * @throws Exception 
     */
    public static Connection getConnection(String MSCategory) throws Exception
    {
        String IP = null;
        String port = null;
        String DBName = SQLConstant.TEST_DB;
        String userName = null;
        String password = null;

        DbmsBrand brand = null;
        String database = TestController.getDatabase().getBrand();
        switch (database)
        {
        case ConfigConstant.CEDAR:
            brand = DbmsBrand.CEDAR;
            userName = CedarOperation.getDatabaseUser();
            password = CedarOperation.getDatabasePassword();
            if (MSCategory.equals(ConfigConstant.MASTER_LOWER)) 
            {
                MergeServerInfo msi = ClustersInfo.getMasterMS();
                IP = msi.getIP();
                port = String.valueOf(msi.getMySQLPort());
            }
            else
            {
                MergeServerInfo msi = ClustersInfo.getSlaveMS();
                IP = msi.getIP();
                port = String.valueOf(msi.getMySQLPort());
            }
            break;

        case ConfigConstant.MYSQL:
            brand = DbmsBrand.MYSQL;
            IP = MySQLOperation.getIP();
            port = String.valueOf(MySQLOperation.getPort());
            userName = MySQLOperation.getDatabaseUser();
            password = MySQLOperation.getDatabasePassword();
            break;

        case ConfigConstant.POSTGRESQL:
            brand = DbmsBrand.POSTGRESQL;
            PostgreSQLOperation instance = PostgreSQLOperation.getInstance();
            IP = instance.getIP();
            port = String.valueOf(instance.getPort());
            userName = instance.getDatabaseUser();
            password = instance.getDatabasePassword();
            break;
        case ConfigConstant.VOLTDB:
            brand = DbmsBrand.VOLTDB;
            // TODO
            break;
        }
        return connection = BasicSQLOperation.getConnection(IP, port, DBName, userName, password, brand);
    }
}

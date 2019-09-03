package edu.ecnu.woodpecker.executor.keyword;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.util.Util;

/**
 * The class handle get_pstat keyword
 *
 */
public class PreparedStatementGetter extends Executor implements Keyword
{
    public PreparedStatementGetter()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        switch(type)
        {
        case FIRST_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.WARN, "Need variable to storage prepared statement");
            break;
        case SECOND_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_PSTAT: %s", keyword);
            // Index 0 is variable name, 1 is keyword
            String[] keywordParts = Util.removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
            handleSecondGrammar(keywordParts[0], keywordParts[1]);
            break;
        case THIRD_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_PSTAT: %s", keyword);
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
     * @param variableName
     * @param keyword
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        if (!varValueMap.containsKey(variableName))
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "Use variable without declaring in %s line %d", caseFileName, lineNumber);
            throw new Exception();
        }
        // Index 0 is keyword, 1 is connection name, 2 is prepared sql, 3 is data type of
        // parameters in prepared sql if exists
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|\"|]"));
        pstatExecuteMap.put(variableName, parts[2]);
        varValueMap.put(variableName, getPreparedStatement(parts[1], parts[2]));
        // Exists prepared sql parameters
        if (parts.length == 4)
            preparedParametersMap.put(variableName, Arrays.asList(parts[3].split(SignConstant.COMMA_STR)));
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        // Index 0 is keyword, 1 is connection name, 2 is prepared sql, 3 is data type of
        // parameters in prepared sql if exists
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|\"|]"));
        pstatExecuteMap.put(variableName, parts[2]);
        varValueMap.put(variableName, getPreparedStatement(parts[1], parts[2]));
        // Exists prepared sql parameters
        if (parts.length == 4)
            preparedParametersMap.put(variableName, Arrays.asList(parts[3].split(SignConstant.COMMA_STR)));
    }
    
    /**
     * 
     * @param connectionName The name of JDBC connection
     * @param sql Prepared sql
     * @return 
     * @throws Exception 
     */
    private PreparedStatement getPreparedStatement(String connectionName, String sql) throws Exception
    {
        Connection connection = (Connection) varValueMap.get(connectionName);
        return BasicSQLOperation.getPreparedStatement(connection, sql);
    }
}

package edu.ecnu.woodpecker.executor.keyword;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.Arrays;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.util.Util;

/**
 *  The class handle get_cstat keyword
 *
 */
public class CallableStatementGetter extends Executor implements Keyword
{
    public CallableStatementGetter()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        switch(type)
        {
        case FIRST_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.WARN, "Need variable to storage callable statement");
            break;
        case SECOND_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_CSTAT: %s", keyword);
            // Index 0 is variable name, 1 is keyword
            String[] keywordParts = Util.removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
            handleSecondGrammar(keywordParts[0], keywordParts[1]);
            break;
        case THIRD_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_CSTAT: %s", keyword);
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
            WpLog.recordLog(LogLevelConstant.ERROR,
                    "Use variable without declaring in %s line %d", caseFileName, lineNumber);
            throw new Exception();
        }
        // Index 0 is keyword, 1 is connection name, 2 is callable sql, 3 is data type of
        // parameters in callable sql if exists
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|\"|]"));
        varValueMap.put(variableName, getCallableStatement(parts[1], parts[2]));
        // Exists callable sql parameters
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
        // Index 0 is keyword, 1 is connection name, 2 is callable sql, 3 is data type of
        // parameters in callable sql if exists
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|\"|]"));
        varValueMap.put(variableName, getCallableStatement(parts[1], parts[2]));
        // Exists callable sql parameters
        if (parts.length == 4)
            preparedParametersMap.put(variableName, Arrays.asList(parts[3].split(SignConstant.COMMA_STR)));
    }
    
    /**
     * 
     * @param connectionName 
     * @param sql Callable sql
     * @return
     * @throws Exception 
     */
    private CallableStatement getCallableStatement(String connectionName, String sql) throws Exception
    {
        Connection connection = (Connection) varValueMap.get(connectionName);
        return BasicSQLOperation.getCallableStatement(connection, sql);
    }
}

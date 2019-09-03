package edu.ecnu.woodpecker.executor.keyword;

import java.sql.Connection;
import java.sql.Statement;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.util.Util;

/**
 * The class handle get_stat keyword
 *
 */
public class StatementGetter extends Executor implements Keyword
{
    public StatementGetter()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        switch (type)
        {
        case FIRST_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.WARN, "Need variable to storage statement");
            break;
        case SECOND_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_STAT: %s", keyword);
            // Index 0 is variable name, 1 is keyword
            String[] keywordParts = Util.removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
            handleSecondGrammar(keywordParts[0], keywordParts[1]);
            break;
        case THIRD_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.INFO, "GET_STAT: %s", keyword);
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
     * @param keyword Doesn't include variableName
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        if (!varValueMap.containsKey(variableName))
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "Use variable without declaring in %s line %d", caseFileName, lineNumber);
            throw new Exception();
        }
        String connectionName = Util.removeBlankElement(keyword.split("\\[|]"))[1];
        //varValueMap.put(variableName, getStatement(connectionName));  update for remove conn and stat in SQL
        Statement statement = getStatement(connectionName);
        varValueMap.put(variableName, getStatement(connectionName));
        statMap.put(variableName, statement);
        curStat = statement;
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
        String connectionName = Util.removeBlankElement(keyword.split("\\[|]"))[1];
      //varValueMap.put(variableName, getStatement(connectionName)); update for remove conn and stat in SQL
        Statement statement = getStatement(connectionName);
        varValueMap.put(variableName, getStatement(connectionName));
        statMap.put(variableName, statement);
        curStat = statement;
    }

    /**
     * 
     * @param connectionName The name of JDBC connection
     * @return The statement from connection
     * @throws Exception
     */
    private Statement getStatement(String connectionName) throws Exception
    {
        Connection conn = (Connection) varValueMap.get(connectionName);
        return BasicSQLOperation.getStatement(conn);
    }
}

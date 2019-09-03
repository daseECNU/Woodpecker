package edu.ecnu.woodpecker.executor.keyword;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.executor.SQLKeywordOperator;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.util.Util;

public class ProcedureDefiner extends Executor implements Keyword
{
    public ProcedureDefiner()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "DEF_PROC: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            try
            {
                String[] parts = Util.removeBlankElement(keyword.split("\\[|]|;"));
                defineProcedure(parts[1], parts[2]);
            }
            catch (Exception e)
            {
                throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
            }
            break;
        case SECOND_GRAMMAR:
            // Index 0 is variable name, 1 is keyword
            String[] keywordParts = Util.removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
            handleSecondGrammar(keywordParts[0], keywordParts[1]);
            break;
        case THIRD_GRAMMAR:
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
     * @param keyword Starts with "def_proc["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "2th grammar, DEF_PROC keyword, useless variable");
        try
        {
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]|;"));
            defineProcedure(parts[1], parts[2]);
        }
        catch (Exception e)
        {
            throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
        }
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "def_proc["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "3th grammar, DEF_PROC keyword, useless variable");
        try
        {
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]|;"));
            defineProcedure(parts[1], parts[2]);
        }
        catch (Exception e)
        {
            throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
        }
    }

    /**
     * 
     * @param variableName 存放存储过程定义的变量名
     * @param statementName 执行器变量名
     */
    private static void defineProcedure(String variableName, String statementName) throws Exception
    {
        String sql = (String) varValueMap.get(variableName);
        // 如果存在双引号则消除sql前后的双引号
        if (sql.charAt(0) == SignConstant.DOUBLE_QUOTE_CHAR && sql.charAt(sql.length() - 1) == SignConstant.DOUBLE_QUOTE_CHAR)
            sql = sql.substring(1, sql.length() - 1);
        //SQLProcessor.executeSQL(sql, statementName, SQLKeywordOperator.WRITE);
        SQLProcessor.executeSQL(sql, curStat, SQLKeywordOperator.WRITE);
    }
}

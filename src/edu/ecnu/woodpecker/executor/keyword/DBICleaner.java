package edu.ecnu.woodpecker.executor.keyword;

import java.io.File;
import java.sql.Connection;

import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.util.Util;

/**
 *  The class handle CLEAR_DBI keyword
 *
 */
public class DBICleaner extends Executor implements Keyword
{
    public DBICleaner()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "CLEAR_DBI: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]"));
            clearDBI(parts[1]);
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
     * @param keyword Starts with "clear_dbi["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        try
        {
            WpLog.recordLog(LogLevelConstant.WARN, "2th grammar, CLEAR_DBI keyword, useless variable");
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]"));
            clearDBI(parts[1]);
        }
        catch (Exception e)
        {
            throw new Exception(String.format("%s in line %d", WpLog.getExceptionInfo(e), lineNumber));
        }
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "clear_dbi["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        try
        {
            WpLog.recordLog(LogLevelConstant.WARN, "3th grammar, CLEAR_DBI keyword, useless variable");
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]"));
            clearDBI(parts[1]);
        }
        catch (Exception e)
        {
            throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
        }
    }

    /**
     * 
     * @param DBIName DBI file name without suffix
     * @throws Exception
     */
    private void clearDBI(String DBIName) throws Exception
    {
        String filePath = TestController.getDatabaseInstancePath() + currentGroup + FileConstant.FILE_SEPARATOR
                + DBIName.substring(1, DBIName.length() - 1) + FileConstant.DBI_FILE_SUFFIX;
        Connection conn = ConnectionGetter.getConnection(ConfigConstant.MASTER_LOWER);
        boolean isSucceful = BasicSQLOperation.clearDBI(new File(filePath), FileConstant.UTF_8, conn);
        if (!isSucceful)
            throw new Exception(String.format("Clean database instance unsuccessfully in %s line %d", caseFileName, lineNumber));
    }
}

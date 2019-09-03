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
 * The class handle IMPORT_DBI keyword
 *
 */
public class DBIImporter extends Executor implements Keyword
{
    public DBIImporter()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "IMPORT_DBI: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]"));
            importDBI(parts[1]);
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
     * @param keyword Starts with "import_dbi["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        try
        {
            WpLog.recordLog(LogLevelConstant.WARN, "2th grammar, IMPORT_DBI keyword, useless variable");
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]"));
            importDBI(parts[1]);
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
     * @param keyword Starts with "import_dbi["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        try
        {
            WpLog.recordLog(LogLevelConstant.WARN, "3th grammar, IMPORT_DBI keyword, useless variable");
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]"));
            importDBI(parts[1]);
        }
        catch (Exception e)
        {
            throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
        }
    }

    /**
     * The database instance file's suffix is ".dbi"
     * 
     * @param DBIName DBI file name without suffix
     * @throws Exception
     */
    private void importDBI(String DBIName) throws Exception
    {
        // Get the dbi file path
        String filePath = TestController.getDatabaseInstancePath() + currentGroup + FileConstant.FILE_SEPARATOR
                + DBIName.substring(1, DBIName.length() - 1) + FileConstant.DBI_FILE_SUFFIX;
        Connection conn = ConnectionGetter.getConnection(ConfigConstant.MASTER_LOWER);
        boolean isSucceful = BasicSQLOperation.importDBI(new File(filePath), FileConstant.UTF_8, conn);
        if (!isSucceful)
        {
            // Import unsuccessfully
            throw new Exception(String.format("Import database instance unsuccessfully in %s line %d", caseFileName, lineNumber));
        }
    }
}

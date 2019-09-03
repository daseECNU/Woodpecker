package edu.ecnu.woodpecker.executor.keyword;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.systemfunction.CedarSystemOperator;
import edu.ecnu.woodpecker.util.Util;

/**
 * The class handle SLEEP keyword
 *
 */
public class SleepProcessor extends Executor implements Keyword
{
    public SleepProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "SLEEP: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
            executeSleep(Integer.parseInt(parts[1]), parts[2]);
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
     * @param keyword Starts with "sleep["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        if (!varValueMap.containsKey(variableName))
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "Use variable without declaring in %s line %d", caseFileName, lineNumber);
            throw new Exception();
        }

        WpLog.recordLog(LogLevelConstant.WARN, "2th grammar, SLEEP keyword, useless variable");
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        executeSleep(Integer.parseInt(parts[1]), parts[2]);
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "sleep["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "3th grammar, SLEEP keyword, useless variable");
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        executeSleep(Integer.parseInt(parts[1]), parts[2]);
    }

    /**
     * 
     * @param number Integer
     * @param timeUnit Second, minute, hour etc
     * @throws Exception
     */
    private void executeSleep(int number, String timeUnit) throws Exception
    {
        CedarSystemOperator.sleep(number, timeUnit);
    }
}

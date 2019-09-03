package edu.ecnu.woodpecker.executor.keyword;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.util.Util;

public class ContainProcessor extends Executor implements Keyword
{
    public ContainProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "CONTAIN: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            try
            {
                String[] parts = Util.removeBlankElement(keyword.split("\\[|]|;"));
                contain(parts[1], parts[2], parts[3]);
            }
            catch (Exception e)
            {
                WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
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
     * @param keyword Starts with "contain["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "2th grammar, CONTAIN keyword, useless variable");
        try
        {
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]|;"));
            contain(parts[1], parts[2], parts[3]);
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
        }
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "contain["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "3th grammar, CONTAIN keyword, useless variable");
        try
        {
            String[] parts = Util.removeBlankElement(keyword.split("\\[|]|;"));
            contain(parts[1], parts[2], parts[3]);
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
        }
    }

    /**
     * 
     * @param firstParameter First parameter of CONTAIN
     * @param secondParameter Second parameter of CONTAIN
     * @param thirdParameter Third parameter of CONTAIN
     */
    private void contain(String firstParameter, String secondParameter, String thirdParameter) throws Exception
    {
        firstParameter = firstParameter.matches("\".*") ? firstParameter.substring(1, firstParameter.length() - 1)
                : (String) varValueMap.get(firstParameter);
        secondParameter = secondParameter.matches("\".*") ? secondParameter.substring(1, secondParameter.length() - 1)
                : (String) varValueMap.get(secondParameter);
        long number = thirdParameter.matches("(\\p{Digit})+") ? Long.parseLong(thirdParameter) : (long) varValueMap.get(thirdParameter);
        long i = 0;
        StringBuilder origin = new StringBuilder(firstParameter);
        for (int fromIndex = 0; fromIndex < origin.length(); i++)
        {
            fromIndex = origin.indexOf(secondParameter, fromIndex);
            if (fromIndex == -1)
                break;
            fromIndex += secondParameter.length();
        }
        if (i != number)
        {
            throw new Exception(String.format("The number of pattern in origin string is'nt equal to specified number in %s line %d",
                    caseFileName, lineNumber));
        }
        WpLog.recordLog(LogLevelConstant.INFO, "The number of pattern in origin string is equal to specified number");
    }
}

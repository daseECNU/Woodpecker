package edu.ecnu.woodpecker.executor.keyword;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.util.Util;

public class CalculateProcessor extends Executor implements Keyword
{
    public CalculateProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "CAL: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            calculate(keyword);
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
     * @param keyword Starts with "CAL["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "2th grammar CAL keyword, useless variable");
        calculate(keyword);
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "CAL["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "3th grammar CAL keyword, useless variable");
        calculate(keyword);
    }

    /**
     * 
     * @param input The whole line in middle result except line number and grammar type
     */
    private static void calculate(String input) throws Exception
    {
        String expression = input.substring(4, input.length() - 1).trim();
        int assignIndex = expression.indexOf(SignConstant.ASSIGNMENT_CHAR);
        String targetVariable = expression.substring(0, assignIndex).trim();
        if (!varValueMap.containsKey(targetVariable))
            throw new Exception("undefined variable in " + input);
        varValueMap.put(targetVariable, calExpression(expression.substring(assignIndex + 1)));
    }
}

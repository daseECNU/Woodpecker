package edu.ecnu.woodpecker.executor.keyword;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.util.Util;

/**
 * Handle GRD keyword TODO
 *
 */
public class RandomDataGenerator extends Executor implements Keyword
{
    public RandomDataGenerator()
    {
    }
    
    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "GRD: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            handleFirstGrammar(keyword);
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
     * @param keyword Starts with "grd["
     */
    private void handleFirstGrammar(String keyword)
    {
        
    }
    
    /**
     * 
     * @param variableName
     * @param keyword Starts with "grd["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        
    }
    
    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "grd["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        
    }
}

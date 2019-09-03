package edu.ecnu.woodpecker.executor.keyword;

import java.util.List;

import javax.servlet.jsp.jstl.sql.Result;

import edu.ecnu.woodpecker.constant.DataTypeConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.IdealResultSet;
import edu.ecnu.woodpecker.util.Util;

public class IndexOfProcessor extends Executor implements Keyword
{
    public IndexOfProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "INDEX_OF: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            WpLog.recordLog(LogLevelConstant.WARN, "1th grammar, INDEX_OF keyword");
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
     * @param keyword Starts with "index_of["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "2th grammar INDEX_OF keyword");
        varValueMap.put(variableName, indexOf(keyword));
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "index_of["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "3th grammar INDEX_OF keyword");
        varValueMap.put(variableName, indexOf(keyword));
    }

    /**
     * 
     * @param keyword The whole line in middle result except line number and grammar type
     * @return The value of specified position
     */
    private Object indexOf(String keyword)
    {
        // 下标0是关键字，1是变量名，2是行，3是列，4是数据类型
        String[] components = Util.removeBlankElement(keyword.split("\\[|;|]|,"));
        if (components.length == 3)
        {
            // List
            @SuppressWarnings("rawtypes")
            List list = (List) varValueMap.get(components[1]);
            return list.get(Integer.parseInt(components[2]));
        }
        // IdealResultSet or ResultSet
        int row = Integer.parseInt(components[2]);
        int column = Integer.parseInt(components[3]);
        return varTypeMap.get(components[1]).startsWith(DataTypeConstant.IDEAL_RESULT_SET)
                ? ((IdealResultSet) varValueMap.get(components[1])).getDataByIndex(row, column)
                : ((Result) varValueMap.get(components[1])).getRowsByIndex()[row][column];
    }
}

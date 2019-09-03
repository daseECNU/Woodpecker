package edu.ecnu.woodpecker.executor.keyword;

import java.util.List;

import javax.servlet.jsp.jstl.sql.Result;

import edu.ecnu.woodpecker.constant.DataTypeConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.Comparer;
import edu.ecnu.woodpecker.sql.IdealResultSet;
import edu.ecnu.woodpecker.util.Util;

/**
 * The class handle VERIFY keyword
 *
 */
public class VerifyProcessor extends Executor implements Keyword
{
    public VerifyProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "VERIFY: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
            if (!parts[parts.length-1].toString().equals("set_type"))
            {
                // Default strict
                executeVerify(parts[1], getOperater(parts[2]), parts[3], false);
            }
            else
                executeVerify(parts[1], getOperater(parts[2]), parts[3], true);
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
     * @param keyword Starts with "verify["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        if (!varValueMap.containsKey(variableName))
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "Use variable without declaring in %s line %d", caseFileName, lineNumber);
            throw new Exception();
        }

        WpLog.recordLog(LogLevelConstant.WARN, "2th grammar, VERIFY keyword, useless variable");
        try
        {
            String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
            if (!parts[parts.length-1].toString().equals("set_type"))
            {
                // Default strict
                executeVerify(parts[1], getOperater(parts[2]), parts[3], false);
            }
            else
                executeVerify(parts[1], getOperater(parts[2]), parts[3], true);
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
     * @param keyword Starts with "verify["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.WARN, "3th grammar, VERIFY keyword, useless variable");
        try
        {
            String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
            if (!parts[parts.length-1].toString().equals("set_type"))
            {
                // Default strict
                executeVerify(parts[1], getOperater(parts[2]), parts[3], false);
            }
            else
                executeVerify(parts[1], getOperater(parts[2]), parts[3], true);
        }
        catch (Exception e)
        {
            throw new Exception(String.format("%s in %s line %d", WpLog.getExceptionInfo(e), caseFileName, lineNumber));
        }
    }

    /**
     * 
     * @param variable1
     * @param variable2
     * @param relationOperator
     * @param isSetType True means the result set's row order is not matter
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeVerify(String variable1, String relationOperator, String variable2, boolean isSetType) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "%s verify %s",variable1, variable2);
        // 结果集对比，IdealResultSet类型必须放在第一个参数
        if (varTypeMap.get(variable1).startsWith(DataTypeConstant.IDEAL_RESULT_SET)
                && varTypeMap.get(variable2).equals(DataTypeConstant.RESULT_SET))
        {
            IdealResultSet irsVar1 = (IdealResultSet) varValueMap.get(variable1);
            Result rsVar2 = (Result) varValueMap.get(variable2);
            if (Comparer.verify(irsVar1, relationOperator, rsVar2, isSetType))
                WpLog.recordLog(LogLevelConstant.INFO, "Verify true");
            else
                throw new Exception(String.format("Verify false in %s line %d", caseFileName, lineNumber));
            return;
        }
        if (varTypeMap.get(variable2).startsWith(DataTypeConstant.IDEAL_RESULT_SET)
                && varTypeMap.get(variable1).equals(DataTypeConstant.RESULT_SET))
        {
            IdealResultSet irsVar2 = (IdealResultSet) varValueMap.get(variable2);
            Result rsVar1 = (Result) varValueMap.get(variable1);
            if (Comparer.verify(irsVar2, relationOperator, rsVar1, isSetType))
                WpLog.recordLog(LogLevelConstant.INFO, "Verify true");
            else
                throw new Exception(String.format("Verify false in %s line %d", caseFileName, lineNumber));
            return;
        }
        // 对比的两个结果集都是真实查询到的
        if (varTypeMap.get(variable1).startsWith(DataTypeConstant.RESULT_SET)
                && varTypeMap.get(variable2).startsWith(DataTypeConstant.RESULT_SET))
        {
            Result result1 = (Result) varValueMap.get(variable1);
            Result result2 = (Result) varValueMap.get(variable2);
            if (Comparer.verify(result1, relationOperator, result2, isSetType))
                WpLog.recordLog(LogLevelConstant.INFO, "Verify true");
            else
                throw new Exception(String.format("Verify false in %s line %d", caseFileName, lineNumber));
            return;
        }
        // List的数据对比
        if (varTypeMap.get(variable1).startsWith(DataTypeConstant.LIST) && varTypeMap.get(variable2).startsWith(DataTypeConstant.LIST))
        {
            if (Comparer.verify((List) varValueMap.get(variable1), relationOperator, (List) varValueMap.get(variable2)))
                WpLog.recordLog(LogLevelConstant.INFO, "Verify true");
            else
                throw new Exception(String.format("Verify false in %s line %d", caseFileName, lineNumber));
            return;
        }

        // TODO 数组的数据对比

        // 非List或数组的数据对比
        if (Comparer.verify(getComparableObject((String type) ->
        {
            return DataTypeConstant.typeMap.get(varTypeMap.get(type));
        }, variable1), relationOperator, varValueMap.get(variable2), false))
        {
            WpLog.recordLog(LogLevelConstant.INFO, "Verify true");
            return;
        }
        else
            throw new Exception(String.format("Verify false in %s line %d", caseFileName, lineNumber));
    }
    private String getOperater(String str)
    {
        String ret=null;
        if(str.equals("=="))
            ret=SignConstant.EQUAL;
        if(str.equals("<"))
            ret=SignConstant.LT_STR;
        if(str.equals(">"))
            ret=SignConstant.GT_STR;
        if(str.equals("!="))
            ret=SignConstant.NON_EQUAL;
        if(str.equals("<="))
            ret=SignConstant.LTOE;
        if(str.equals(">="))
            ret=SignConstant.GTOE;
        if(str.equals("all_are"))
            ret=SignConstant.ALL_ARE;
        if(str.equals("contains"))
            ret=SignConstant.CONTAIN;
        return ret;
    }
}

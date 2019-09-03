package edu.ecnu.woodpecker.executor.keyword;

import java.sql.CallableStatement;

import edu.ecnu.woodpecker.constant.DataTypeConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.ProcedureParameterIO;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.executor.SQLKeywordOperator;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.util.Util;

public class CallableSQLProcessor extends Executor implements Keyword
{
    public CallableSQLProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "CSQL: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
            String[] variablesType = parts.length == 4 ? Util.removeBlankElement(parts[3].split(SignConstant.COMMA_STR)) : null;
            executeCallableSQL(parts[1], SQLKeywordOperator.valueOf(Integer.parseInt(parts[2])), variablesType);
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
     * @param keyword   Starts with "csql["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        if (!varValueMap.containsKey(variableName))
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "Use variable without declaring in %s line %d", caseFileName, lineNumber);
            throw new Exception();
        }

        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        String[] variablesType = parts.length == 4 ? Util.removeBlankElement(parts[3].split(SignConstant.COMMA_STR)) : null;
        SQLKeywordOperator operator = SQLKeywordOperator.valueOf(Integer.parseInt(parts[2]));
        switch (operator)
        {
        case READ:
            // executeQuery
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.RESULT_SET))
            {
                throw new Exception(String.format("Variable type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            executeCallableSQL(parts[1], operator, variablesType);
            varValueMap.put(variableName, result);
            result = null;
            break;
        case WRITE:
            // executeUpdate
            String dataType = varTypeMap.get(variableName);
            if (!dataType.equals(DataTypeConstant.INT_SHORT) || !dataType.equals(DataTypeConstant.LONG_SHORT)
                    || !dataType.equals(DataTypeConstant.FLOAT_SHORT) || !dataType.equals(DataTypeConstant.DOUBLE_SHORT))
            {
                throw new Exception(String.format("Variable type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            varValueMap.put(variableName, executeCallableSQL(parts[1], operator, variablesType));
            break;
        default:
            throw new Exception(String.format("Use undefined type in %s line %d", caseFileName, lineNumber));
        }
    }
    
    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword   Starts with "csql["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        String[] variablesType = parts.length == 4 ? Util.removeBlankElement(parts[3].split(SignConstant.COMMA_STR)) : null;
        SQLKeywordOperator operator = SQLKeywordOperator.valueOf(Integer.parseInt(parts[2]));
        switch (operator)
        {
        case READ:
            // 1ÊÇexecuteQuery
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.RESULT_SET))
            {
                throw new Exception(String.format("Variable type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            executeCallableSQL(parts[1], operator, variablesType);
            varValueMap.put(variableName, result);
            result = null;
            break;
        case WRITE:
            // 2ÊÇexecuteUpdate
            if (!dataType.equals(DataTypeConstant.INT_SHORT) || !dataType.equals(DataTypeConstant.LONG_SHORT)
                    || !dataType.equals(DataTypeConstant.FLOAT_SHORT) || !dataType.equals(DataTypeConstant.DOUBLE_SHORT))
            {
                throw new Exception(String.format("Variable type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            varValueMap.put(variableName, executeCallableSQL(parts[1], operator, variablesType));
            break;
        default:
            throw new Exception(String.format("Use undefined type in %s line %d", caseFileName, lineNumber));
        }
    }
    
    /**
     * 
     * @param callableStatementName 
     * @param operator 
     * @param variablesType Each element is the component of variable and input output type, such as "a in"
     * @return Rows if type is update, otherwise return -1
     * @throws Exception
     */
    private int executeCallableSQL(String callableStatementName, SQLKeywordOperator operator, String... variablesType) throws Exception
    {
        int rows = -1;
        ProcedureParameterIO[] IOTypes = null;
        String[] variables = null;
        if (variablesType != null)
        {
            // Create input output array for procedure
            IOTypes = new ProcedureParameterIO[variablesType.length];
            variables = new String[variablesType.length];
            String[] parts = null;
            for (int i = 0; i < IOTypes.length; i++)
            {
                parts = variablesType[i].split("\\s");
                variables[i] = parts[0].trim();
                IOTypes[i] = ProcedureParameterIO.of(parts[1].trim().toLowerCase());
            }
        }
        // Determine execute type
        CallableStatement cstmt = (CallableStatement) varValueMap.get(callableStatementName);
        switch (operator)
        {
        case READ:
            result = BasicSQLOperation.cstmtExecuteQuery(cstmt, getParamTypes(variables, callableStatementName), IOTypes, getParamValues(variables),
                    variables);
            break;
        case WRITE:
            rows = BasicSQLOperation.cstmtExecuteUpdate(cstmt, getParamTypes(variables, callableStatementName), IOTypes, getParamValues(variables),
                    variables);
            break;
        default:
            throw new Exception(String.format("Wrong CSQL type in %s line %d", caseFileName, lineNumber));
        }
        return rows;
    }
}

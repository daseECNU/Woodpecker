package edu.ecnu.woodpecker.executor.keyword;

import java.sql.PreparedStatement;

import edu.ecnu.woodpecker.constant.DataType;
import edu.ecnu.woodpecker.constant.DataTypeConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.executor.SQLKeywordOperator;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.BasicSQLOperation;
import edu.ecnu.woodpecker.util.Util;

/**
 * The class handle PSQL keyword
 *
 */
public class PreparedSQLProcessor extends Executor implements Keyword
{
    public PreparedSQLProcessor()
    {
    }

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "PSQL: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
            String[] variables = Util.removeBlankElement(parts[2].split(SignConstant.COMMA_STR));
            int operator = SQLProcessor.getSQLOperator(pstatExecuteMap.get(parts[1]), parts);
            if (parts.length >= 3 && parts[parts.length - 2].equals("ERROR"))
            {
                executePreparedSQL(parts[1], SQLKeywordOperator.valueOf(operator), variables);
                String[] error = parts[parts.length - 1].split("\"");
                SQLProcessor.contain(exceptionString, error[1].toString());
            }
            else
            {
                executePreparedSQL(parts[1], SQLKeywordOperator.valueOf(operator), variables);
            }

            // executePreparedSQL(parts[1], SQLKeywordOperator.valueOf(operator),
            // variables);
            break;
        case SECOND_GRAMMAR:
            // Index 0 is variable name, 1 is keyword
            String[] keywordParts = Util
                    .removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
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
     * @param keyword Starts with "psql["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        if (!varValueMap.containsKey(variableName))
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "Use variable without declaring in %s line %d",
                    caseFileName, lineNumber);
            throw new Exception();
        }

        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        String[] variables = null;
        if(parts.length == 2 )
        {
            variables = null;
        }
        else
        {
            variables = Util.removeBlankElement(parts[2].split(SignConstant.COMMA_STR));  
        }SQLKeywordOperator operator = SQLKeywordOperator
                .valueOf(SQLProcessor.getSQLOperator(pstatExecuteMap.get(parts[1]), parts));
        switch (operator)
        {
        case READ:
            // executeQuery
            executePreparedSQL(parts[1], operator, variables);
            SQLProcessor.dealDiffType(variableName, result);
            // varValueMap.put(variableName, result);
            // result = null;
            break;

        case WRITE:
            // executeUpdate
            String dataType = varTypeMap.get(variableName);
            if (!dataType.equals(DataTypeConstant.INT_SHORT)
                    || !dataType.equals(DataTypeConstant.LONG_SHORT)
                    || !dataType.equals(DataTypeConstant.FLOAT_SHORT)
                    || !dataType.equals(DataTypeConstant.DOUBLE_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            varValueMap.put(variableName, executePreparedSQL(parts[1], operator, variables));
            break;

        case READ_ERROR:
            // executeQuery with exception
            if (parts.length >= 3 && parts[parts.length - 2].equals("ERROR"))
            {
                executePreparedSQL(parts[1], operator, variables);
                String[] error = parts[parts.length - 1].split("\"");
                SQLProcessor.contain(exceptionString, error[1].toString());
            }
            else
            {
                executePreparedSQL(parts[1], operator, variables);
            }
            // executePreparedSQL(parts[1], operator, variables);
            // varValueMap.put(variableName, exceptionString);
            exceptionString = null;
            result = null;
            break;

        case WRITE_ERROR:
            // executeUpdate with exception
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            if (parts.length >= 3 && parts[parts.length - 2].equals("ERROR"))
            {
                executePreparedSQL(parts[1], operator, variables);
                String[] error = parts[parts.length - 1].split("\"");
                SQLProcessor.contain(exceptionString, error[1].toString());
            }
            else
            {
                executePreparedSQL(parts[1], operator, variables);
            }
            // executePreparedSQL(parts[1], operator, variables);
            // varValueMap.put(variableName, exceptionString);
            exceptionString = null;
            break;

        case CEDAR_EXPLAIN:
            // CEDAR explain
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            executePreparedSQL(parts[1], operator, variables);
            varValueMap.put(variableName, result.getRowsByIndex()[0][0]);
            result = null;
            break;

        case CEDAR_EXPLAIN_ERROR:
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            if (parts[parts.length - 2].equals("ERROR"))
            {
                executePreparedSQL(parts[1], operator, variables);
                String[] error = parts[parts.length - 1].split("\"");
                SQLProcessor.contain(exceptionString, error[1].toString());
            }
            else
            {
                executePreparedSQL(parts[1], operator, variables);
            }
            // varValueMap.put(variableName, exceptionString);
            exceptionString = null;
            break;

        default:
            throw new Exception(String.format("Use PSQL undefined type in %s line %d", caseFileName,
                    lineNumber));
        }
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword   Starts with "psql["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        String[] vars = null;
        if(parts.length == 2 )
        {
            vars = null;
        }
        else
        {
            vars = Util.removeBlankElement(parts[2].split(SignConstant.COMMA_STR));  
        }
        SQLKeywordOperator operator = SQLKeywordOperator.valueOf(SQLProcessor.getSQLOperator(pstatExecuteMap.get(parts[1]), parts));
        switch (operator)
        {
        case READ:
            // 1是executeQuery
            executePreparedSQL(parts[1], operator, vars);
            SQLProcessor.dealDiffType(variableName, result);
            break;
        case WRITE:
            // 2是executeUpdate
            if (!dataType.equals(DataTypeConstant.INT_SHORT) || !dataType.equals(DataTypeConstant.LONG_SHORT) || !dataType.equals(DataTypeConstant.FLOAT_SHORT) || !dataType.equals(DataTypeConstant.DOUBLE_SHORT))
            {
                throw new Exception(String.format("Variable's data type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            varValueMap.put(variableName, executePreparedSQL(parts[1], operator, vars));
            break;
        case READ_ERROR:
            // -1是带有异常的executeQuery
            if (parts.length >= 3 && parts[parts.length - 2].equals("ERROR"))
            {
                executePreparedSQL(parts[1], operator, vars);
                String[] error = parts[parts.length - 1].split("\"");
                SQLProcessor.contain(exceptionString, error[1].toString());
            }
            else
            {
                executePreparedSQL(parts[1], operator, vars);
            }
            exceptionString = null;
            result = null;
            break;
        case WRITE_ERROR:
            // -2是带有异常的executeUpdate
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
            {
                throw new Exception(String.format("Variable's data type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            if (parts.length >= 3 && parts[parts.length - 2].equals("ERROR"))
            {
                executePreparedSQL(parts[1], operator, vars);
                String[] error = parts[parts.length - 1].split("\"");
                SQLProcessor.contain(exceptionString, error[1].toString());
            }
            else
            {
                executePreparedSQL(parts[1], operator, vars);
            }
//            executePreparedSQL(parts[1], operator, vars);
//            varValueMap.put(variableName, exceptionString);
            exceptionString = null;
            break;
        case CEDAR_EXPLAIN:
            // 3是CEDAR explain语句，返回的是一个字符串
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
            {
                throw new Exception(String.format("Variable's data type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            executePreparedSQL(parts[1], operator, vars);
            varValueMap.put(variableName, result.getRowsByIndex()[0][0]);
            result = null;
            break;
        case CEDAR_EXPLAIN_ERROR:
            // -3是CEDAR explain预期有异常的语句
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
            {
                throw new Exception(String.format("Variable's data type don't match keyword in %s line %d", caseFileName, lineNumber));
            }
            if (parts.length >= 3 && parts[parts.length - 2].equals("ERROR"))
            {
                executePreparedSQL(parts[1], operator, vars);
                String[] error = parts[parts.length - 1].split("\"");
                SQLProcessor.contain(exceptionString, error[1].toString());
            }
            else
            {
                executePreparedSQL(parts[1], operator, vars);
            }
//            varValueMap.put(variableName, exceptionString);
            exceptionString = null;
            break;
        default:
            throw new Exception(String.format("Use PSQL undefined type in %s line %d", caseFileName, lineNumber));
        }
    }

    /**
     * 
     * @param preparedstatementName
     * @param operator
     * @param variables Variable name or value needed in prepared sql
     * @return Rows if type is update, otherwise return -1
     * @throws Exception
     */
    private int executePreparedSQL(String preparedstatementName, SQLKeywordOperator operator,
            String... variables) throws Exception
    {
        PreparedStatement pstmt = (PreparedStatement) varValueMap.get(preparedstatementName);
        DataType[] dataTypes = getParamTypes(variables, preparedstatementName);
        Object[] objects = getParamValues(variables);
        int rows = -1;
        switch (operator)
        {
        case READ:
            result = BasicSQLOperation.pstmtExecuteQuery(pstmt, dataTypes, false, objects);
            break;
        case WRITE:
            rows = BasicSQLOperation.pstmtExecuteUpdate(pstmt, dataTypes, false, objects);
            break;
        case READ_ERROR:
            BasicSQLOperation.pstmtExecuteQuery(pstmt, dataTypes, true, objects);
            break;
        case WRITE_ERROR:
            BasicSQLOperation.pstmtExecuteUpdate(pstmt, dataTypes, true, objects);
            break;
        case CEDAR_EXPLAIN:
            result = BasicSQLOperation.pstmtExecuteQuery(pstmt, dataTypes, false, objects);
            break;
        case CEDAR_EXPLAIN_ERROR:
            BasicSQLOperation.pstmtExecuteQuery(pstmt, dataTypes, true, objects);
            break;
        default:
            throw new Exception(
                    String.format("Wrong PSQL type in %s line %d", caseFileName, lineNumber));
        }
        return rows;
    }
}

package edu.ecnu.woodpecker.executor.keyword;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import javax.servlet.jsp.jstl.sql.Result;
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
 * The class handle SQL keyword
 *
 */
public class SQLProcessor extends Executor implements Keyword
{
    public SQLProcessor()
    {
    }

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "SQL: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\"|;|]"));
            // Avoid erasing semicolon in sql
            StringBuilder sql = new StringBuilder(parts[1]).append(SignConstant.SEMICOLON_CHAR);
            if (parts[parts.length - 2].equals("ERROR"))
            {
                for (int i = 2; i < parts.length - 3; i++)
                {
                    sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
                }
                executeSQL(sql.toString(), curStat,
                        SQLKeywordOperator.valueOf(getSQLOperator(sql.toString(), parts)));
                contain(exceptionString, parts[parts.length - 1]);
            }
            else if (parts[parts.length - 1].equals("ERROR"))
            {
                for (int i = 2; i < parts.length - 2; i++)
                {
                    sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
                }
                executeSQL(sql.toString(), curStat,
                        SQLKeywordOperator.valueOf(getSQLOperator(sql.toString(), parts)));
            }
            else
            {
                for (int i = 2; i < parts.length - 1; i++)
                {
                    sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
                }
                executeSQL(sql.toString(), curStat,
                        SQLKeywordOperator.valueOf(getSQLOperator(sql.toString(), parts)));
            }
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
     * @param keyword Starts with "sql["
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

        String[] parts = Util.removeBlankElement(keyword.split("\"|;|]"));
        // Avoid erasing semicolon in sql
        StringBuilder sql = new StringBuilder(parts[1]).append(SignConstant.SEMICOLON_CHAR);
        if (parts[parts.length - 2].equals("ERROR"))
        {
            for (int i = 2; i < parts.length - 3; i++)
            {
                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
            }
        }
        else if (parts[parts.length - 1].equals("ERROR"))
        {
            for (int i = 2; i < parts.length - 2; i++)
            {
                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
            }
        }
        else
        {
            for (int i = 2; i < parts.length - 1; i++)
            {
                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
            }
        }
        // for (int i = 2; i < parts.length - 2; i++)
        // sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
        SQLKeywordOperator operator = SQLKeywordOperator
                .valueOf(getSQLOperator(sql.toString(), parts));

        switch (operator)
        {
        case READ:
            // executeQuery
            executeSQL(sql.toString(), curStat, operator);
            dealDiffType(variableName, result);
//            varValueMap.put(variableName, result);
//            result = null;
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
            varValueMap.put(variableName,
                    executeSQL(sql.toString(), curStat, operator));
            break;

        case READ_ERROR:
            // -1 is executeQuery and exist exception
            executeSQL(sql.toString(), curStat, operator);
            if (parts[parts.length - 2].equals("ERROR"))
            {
                varValueMap.put(variableName, exceptionString);
                contain(exceptionString, parts[parts.length - 1]);
            }
            exceptionString = null;
            result = null;
            break;

        case WRITE_ERROR:
            // -2 is executeUpdate and exist exception
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            executeSQL(sql.toString(), curStat, operator);
            if (parts[parts.length - 2].equals("ERROR"))
            {
                varValueMap.put(variableName, exceptionString);
                contain(exceptionString, parts[parts.length - 1]);
            }
            exceptionString = null;
            break;

        case CEDAR_EXPLAIN:
            // CEDAR explain statement return string
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            executeSQL(sql.toString(), curStat, operator);
            varValueMap.put(variableName, result.getRowsByIndex()[0][0]);
            result = null;
            break;

        case CEDAR_EXPLAIN_ERROR:
            // -3 is CEDAR explain and exist exception
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            executeSQL(sql.toString(), curStat, operator);
            if (parts[parts.length - 2].equals("ERROR"))
            {
                varValueMap.put(variableName, exceptionString);
                contain(exceptionString, parts[parts.length - 1]);
            }
            exceptionString = null;
            break;

        default:
            throw new Exception(String.format("Use SQL undefined type in %s line %d", caseFileName,
                    lineNumber));
        }
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "sql["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword)
            throws Exception
    {
        String[] parts = Util.removeBlankElement(keyword.split("\"|;|]"));
        // Avoid erasing semicolon in sql
        StringBuilder sql = new StringBuilder(parts[1]).append(SignConstant.SEMICOLON_CHAR);
        if (parts[parts.length - 2].equals("ERROR"))
        {
            for (int i = 2; i < parts.length - 3; i++)
            {
                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
            }
        }
        else if (parts[parts.length - 1].equals("ERROR"))
        {
            for (int i = 2; i < parts.length - 2; i++)
            {
                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
            }
        }
        else
        {
            for (int i = 2; i < parts.length - 1; i++)
                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
        }
        // for (int i = 2; i < parts.length - 2; i++)
        // sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
        SQLKeywordOperator operator = SQLKeywordOperator
                .valueOf(getSQLOperator(sql.toString(), parts));

        switch (operator)
        {
        case READ:
            executeSQL(sql.toString(), curStat, operator);
            dealDiffType(variableName, result);
            break;

        case WRITE:
            // executeUpdate
            if (!dataType.equals(DataTypeConstant.INT_SHORT)
                    && !dataType.equals(DataTypeConstant.LONG_SHORT)
                    && !dataType.equals(DataTypeConstant.FLOAT_SHORT)
                    && !dataType.equals(DataTypeConstant.DOUBLE_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            varValueMap.put(variableName,
                    executeSQL(sql.toString(), curStat, operator));
            break;

        case READ_ERROR:
            // executeQuery and exist exception
            //SQL[sql;ERROR;err_message]
            executeSQL(sql.toString(), curStat, operator);
            if (parts[parts.length - 2].equals("ERROR"))
            {
                varValueMap.put(variableName, exceptionString);
                contain(exceptionString, parts[parts.length - 1]);
            } 
            exceptionString = null;
            result = null;
            break;

        case WRITE_ERROR:
            // executeUpdate and exist exception
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            executeSQL(sql.toString(), curStat, operator);
            if (parts[parts.length - 2].equals("ERROR"))
            {
                varValueMap.put(variableName, exceptionString);
                contain(exceptionString, parts[parts.length - 1]);
            }
            exceptionString = null;
            break;

        case CEDAR_EXPLAIN:
            // CEDAR explain
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            executeSQL(sql.toString(), curStat, operator);
            varValueMap.put(variableName, result.getRowsByIndex()[0][0]);
            result = null;
            break;

        case CEDAR_EXPLAIN_ERROR:
            // CEDAR explain and exist exception
            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
                throw new Exception(
                        String.format("Variable's data type don't match keyword in %s line %d",
                                caseFileName, lineNumber));
            executeSQL(sql.toString(), curStat, operator);
            if (parts[parts.length - 2].equals("ERROR"))
            {
                varValueMap.put(variableName, exceptionString);
                contain(exceptionString, parts[parts.length - 1]);
            }
            exceptionString = null;
            break;

        default:
            throw new Exception(String.format("Use SQL undefined type in %s line %d", caseFileName,
                    lineNumber));
        }
    }

    /**
     * @param firstParameter
     * @param secondParameter
     * @throws Exception
     */
   public static void contain(String firstParameter, String secondParameter) throws Exception
    {
        StringBuilder origin = new StringBuilder(firstParameter);
        int fromIndex = 0;
        fromIndex = origin.indexOf(secondParameter, fromIndex);
        if (fromIndex == -1)
        {
            throw new Exception("The error is different from expected error!");
        }  
//        WpLog.recordLog(LogLevelConstant.INFO,
//                "The origin string is equal to specified number");
        return;
    }

    /**
     * @param sql
     * @return sqlOperator
     */
    public static int getSQLOperator(String sql, String[] parts) throws Exception
    {
        String sqlType = sql.substring(sql.indexOf("\"") + 1, sql.indexOf(" "));
        int sqlOperator = 0;
        if (sqlType.contains("select"))
        {
            if (parts[parts.length - 1].equals("ERROR") || parts[parts.length - 2].equals("ERROR")){
                sqlOperator = -1;
            }
            else{
                sqlOperator = 1;
            }
            return sqlOperator;
        }
        else if (sqlType.contains("explain"))
        {
            if (parts[parts.length - 1].equals("ERROR") || parts[parts.length - 2].equals("ERROR")){
                sqlOperator = -3;
            }
            else{
                sqlOperator = 3;
            }
            return sqlOperator;
        }
        else{
            if (parts[parts.length - 1].equals("ERROR") || parts[parts.length - 2].equals("ERROR")){
                sqlOperator = -2;
            }
            else{
                sqlOperator = 2;
            }
            return sqlOperator;
        }
    }

    public static String shellExec(String str) throws Exception
    {
        String sql = str;
        int count = 0;
        for(int i = 0; i < sql.length(); i++)
            if(str.charAt(i) == '$')
                count++;
        for(int i = 0; i < count; i++)
        {
            int index_$ = sql.indexOf("$");
            int index_blank = sql.indexOf(" ", index_$);  //need to be improved
            String var = sql.substring(index_$ + 1, index_blank);
            if (varValueMap.containsKey(var))
            {
                sql = sql.replace('$' + var, varValueMap.get(var).toString());
            }
            else
            {
                throw new Exception("variable not exists!!!");
            } 
        }
        return sql;
    }
    
    /**
     * 
     * @param sql
     * @param curStat
     * @param operator Execute type
     * @return Rows if type is executeUpdate, otherwise return -1
     * @throws Exception
     */
    public static int executeSQL(String sql, Statement curStat, SQLKeywordOperator operator)
            throws Exception
    {
        /**
         * ��ģ��shell�б�����Ŀǰֻ֧��һ��������������ʽ��$׃����ͬ�r׃����һ��Ҫ��һ���ո�
         */
        int index_$ = sql.indexOf("$");
        int index_blank = sql.indexOf(" ", index_$);
        String var = sql.substring(index_$ + 1, index_blank);
        if (varValueMap.containsKey(var))
        {
            sql = sql.replace('$' + var, varValueMap.get(var).toString());
        }

        int rows = -1;
        switch (operator)
        {
        case READ:
            result = BasicSQLOperation.stmtExecuteQuery(curStat, sql, false);
            break;
        case WRITE:
            rows = BasicSQLOperation.stmtExecuteUpdate(curStat, sql, false);
            break;
        case READ_ERROR:
            // executeQuery and will throw exception
            BasicSQLOperation.stmtExecuteQuery(curStat, sql, true);
            break;
        case WRITE_ERROR:
            // executeUpdate and will throw exception
            BasicSQLOperation.stmtExecuteUpdate(curStat, sql, true);
            break;
        case CEDAR_EXPLAIN:
            // The goal is row 0 column 0 of result
            result = BasicSQLOperation.stmtExecuteQuery(curStat, sql, false);
            break;
        case CEDAR_EXPLAIN_ERROR:
            // CEDAR explain and will throw exception
            BasicSQLOperation.stmtExecuteQuery(curStat, sql, true);
            break;
        default:
            throw new Exception(
                    String.format("Wrong type in %s line %d", caseFileName, lineNumber));
        }
        return rows;
    }
    

    public static void dealDiffType(String varName, Result result) throws Exception
    {
        SortedMap[] sortMap=result.getRows();
        String columnAndValue=sortMap[0].toString().substring(1, sortMap[0].toString().length()-1);
        String[] value=columnAndValue.split("=");
        switch (varTypeMap.get(varName))
        {
        case DataTypeConstant.INT_SHORT:
            varValueMap.put(varName, new Integer(value[1].toString()));
            result=null;
            break;
        case DataTypeConstant.BOOLEAN_SHORT:
            varValueMap.put(varName, new Boolean(value[1].toString()));
            result=null;
            break;
        case DataTypeConstant.LONG_SHORT:
            varValueMap.put(varName, new Long(value[1].toString()));
            result=null;
            break;
        case DataTypeConstant.FLOAT_SHORT:
            varValueMap.put(varName, new Float(value[1].toString()));
            result=null;
            break;
        case DataTypeConstant.DOUBLE_SHORT:
            varValueMap.put(varName, new Double(value[1].toString()));
            result=null;
            break;
        case DataTypeConstant.STRING_SHORT:
            varValueMap.put(varName, value[1].toString());
            result=null;
            break;
        case DataTypeConstant.DECIMAL_SHORT:
            varValueMap.put(varName, new BigDecimal(value[1].toString()));
            result=null;
            break;
        case DataTypeConstant.RESULT_SET:
            varValueMap.put(varName, result);
            result=null;
            break;
        case DataTypeConstant.LIST_INTGER:
            List<Integer> listInteger=new ArrayList<Integer>();
            sortMap=result.getRows();
            for(int i=0;i<result.getRowCount();i++)
            {
                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
                value=columnAndValue.split("=");
                if(value.length>2)
                {
                    throw new Exception("Resultset is more than one column!!!");
                }
                listInteger.add(new Integer(value[1].toString()));
            }
            varValueMap.put(varName, listInteger);
            result=null;
            break;
        case DataTypeConstant.LIST_FLOAT:
            List<Float> listFloat=new ArrayList<Float>();
            sortMap=result.getRows();
            for(int i=0;i<result.getRowCount();i++)
            {
                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
                value=columnAndValue.split("=");
                if(value.length>2)
                {
                    throw new Exception("Resultset is more than one column!!!");
                }
                listFloat.add(new Float(value[1].toString()));
            }
            varValueMap.put(varName, listFloat);
            result=null;
            break;
        case DataTypeConstant.LIST_DOUBLE:
            List<Double> listDouble=new ArrayList<Double>();
            sortMap=result.getRows();
            for(int i=0;i<result.getRowCount();i++)
            {
                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
                value=columnAndValue.split("=");
                if(value.length>2)
                {
                    throw new Exception("Resultset is more than one column!!!");
                }
                listDouble.add(new Double(value[1].toString()));
            }
            varValueMap.put(varName, listDouble);
            result=null;
            break;
        case DataTypeConstant.LIST_BOOLEAN:
            List<Boolean> listBoolean=new ArrayList<Boolean>();
            sortMap=result.getRows();
            for(int i=0;i<result.getRowCount();i++)
            {
                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
                value=columnAndValue.split("=");
                if(value.length>2)
                {
                    throw new Exception("Resultset is more than one column!!!");
                }
                listBoolean.add(new Boolean(value[1].toString()));
            }
            varValueMap.put(varName, listBoolean);
            result=null;
            break;
        case DataTypeConstant.LIST_BIGDECIMAL:
            List<BigDecimal> listBigDecimal=new ArrayList<BigDecimal>();
            sortMap=result.getRows();
            for(int i=0;i<result.getRowCount();i++)
            {
                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
                value=columnAndValue.split("=");
                if(value.length>2)
                {
                    throw new Exception("Resultset is more than one column!!!");
                }
                listBigDecimal.add(new BigDecimal(value[1].toString()));
            }
            varValueMap.put(varName, listBigDecimal);
            result=null;
            break;
        case DataTypeConstant.LIST_LONG:
            List<Long> listLong=new ArrayList<Long>();
            sortMap=result.getRows();
            for(int i=0;i<result.getRowCount();i++)
            {
                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
                value=columnAndValue.split("=");
                if(value.length>2)
                {
                    throw new Exception("Resultset is more than one column!!!");
                }
                listLong.add(new Long(value[1].toString()));
            }
            varValueMap.put(varName, listLong);
            result=null;
            break;
        case DataTypeConstant.LIST_STRING:
            List<String> listStirng=new ArrayList<String>();
            sortMap=result.getRows();
            for(int i=0;i<result.getRowCount();i++)
            {
                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
                value=columnAndValue.split("=");
                if(value.length>2)
                {
                    throw new Exception("Resultset is more than one column!!!");
                }
                listStirng.add(value[1].toString());
            }
            varValueMap.put(varName, listStirng);
            result=null;
            break;
        default:
            result=null;
            throw new Exception ("Unsupported Datatype!");
        }
    }
    
//    update for remove conn and stat in SQL 
//    @Override
//    public void handle(String keyword, GrammarType type) throws Exception
//    {
//        WpLog.recordLog(LogLevelConstant.INFO, "SQL: %s", keyword);
//        switch (type)
//        {
//        case FIRST_GRAMMAR:
//            String[] parts = Util.removeBlankElement(keyword.split("\"|;|]"));
//            // Avoid erasing semicolon in sql
//            StringBuilder sql = new StringBuilder(parts[1]).append(SignConstant.SEMICOLON_CHAR);
//            if (parts[parts.length - 2].equals("ERROR"))
//            {
//                for (int i = 2; i < parts.length - 3; i++)
//                {
//                    sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//                }
//                executeSQL(sql.toString(), parts[parts.length - 3],
//                        SQLKeywordOperator.valueOf(getSQLOperator(sql.toString(), parts)));
//                contain(exceptionString, parts[parts.length - 1]);
//            }
//            else if (parts[parts.length - 1].equals("ERROR"))
//            {
//                for (int i = 2; i < parts.length - 2; i++)
//                {
//                    sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//                }
//                executeSQL(sql.toString(), parts[parts.length - 2],
//                        SQLKeywordOperator.valueOf(getSQLOperator(sql.toString(), parts)));
//            }
//            else
//            {
//                for (int i = 2; i < parts.length - 1; i++)
//                {
//                    sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//                }
//                executeSQL(sql.toString(), parts[parts.length - 1],
//                        SQLKeywordOperator.valueOf(getSQLOperator(sql.toString(), parts)));
//            }
//            break;
//        case SECOND_GRAMMAR:
//            // Index 0 is variable name, 1 is keyword
//            String[] keywordParts = Util
//                    .removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
//            handleSecondGrammar(keywordParts[0], keywordParts[1]);
//            break;
//        case THIRD_GRAMMAR:
//            // Index 0 is data type and variable name, 1 is keyword
//            keywordParts = Util.removeBlankElement(keyword.split(SignConstant.ASSIGNMENT_STR, 2));
//            // Index 0 is data type, 1 is variable name
//            String[] decVar = Util.removeBlankElement(keywordParts[0].split("\\s"));
//            handleThirdGrammar(decVar[0], decVar[1], keywordParts[1]);
//            break;
//        default:
//            throw new Exception("Grammar error");
//        }
//    }
//
//    /**
//     * 
//     * @param variableName
//     * @param keyword Starts with "sql["
//     * @throws Exception
//     */
//    private void handleSecondGrammar(String variableName, String keyword) throws Exception
//    {
//        if (!varValueMap.containsKey(variableName))
//        {
//            WpLog.recordLog(LogLevelConstant.ERROR, "Use variable without declaring in %s line %d",
//                    caseFileName, lineNumber);
//            throw new Exception();
//        }
//
//        String[] parts = Util.removeBlankElement(keyword.split("\"|;|]"));
//        // Avoid erasing semicolon in sql
//        StringBuilder sql = new StringBuilder(parts[1]).append(SignConstant.SEMICOLON_CHAR);
//        if (parts[parts.length - 2].equals("ERROR"))
//        {
//            for (int i = 2; i < parts.length - 3; i++)
//            {
//                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//            }
//        }
//        else if (parts[parts.length - 1].equals("ERROR"))
//        {
//            for (int i = 2; i < parts.length - 2; i++)
//            {
//                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//            }
//        }
//        else
//        {
//            for (int i = 2; i < parts.length - 1; i++)
//            {
//                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//            }
//        }
//        // for (int i = 2; i < parts.length - 2; i++)
//        // sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//        SQLKeywordOperator operator = SQLKeywordOperator
//                .valueOf(getSQLOperator(sql.toString(), parts));
//
//        switch (operator)
//        {
//        case READ:
//            // executeQuery
//            executeSQL(sql.toString(), parts[parts.length - 1], operator);
//            dealDiffType(variableName, result);
////            varValueMap.put(variableName, result);
////            result = null;
//            break;
//
//        case WRITE:
//            // executeUpdate
//            String dataType = varTypeMap.get(variableName);
//            if (!dataType.equals(DataTypeConstant.INT_SHORT)
//                    || !dataType.equals(DataTypeConstant.LONG_SHORT)
//                    || !dataType.equals(DataTypeConstant.FLOAT_SHORT)
//                    || !dataType.equals(DataTypeConstant.DOUBLE_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            varValueMap.put(variableName,
//                    executeSQL(sql.toString(), parts[parts.length - 1], operator));
//            break;
//
//        case READ_ERROR:
//            // -1 is executeQuery and exist exception
//            if (parts[parts.length - 2].equals("ERROR"))
//            {
//                executeSQL(sql.toString(), parts[parts.length - 3], operator);
//                varValueMap.put(variableName, exceptionString);
//                contain(exceptionString, parts[parts.length - 1]);
//            }
//            else
//            {
//                executeSQL(sql.toString(), parts[parts.length - 2], operator);
//            }
//            exceptionString = null;
//            result = null;
//            break;
//
//        case WRITE_ERROR:
//            // -2 is executeUpdate and exist exception
//            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            if (parts[parts.length - 2].equals("ERROR"))
//            {
//                executeSQL(sql.toString(), parts[parts.length - 3], operator);
//                varValueMap.put(variableName, exceptionString);
//                contain(exceptionString, parts[parts.length - 1]);
//            }
//            else
//            {
//                executeSQL(sql.toString(), parts[parts.length - 2], operator);
//            }
//            exceptionString = null;
//            break;
//
//        case CEDAR_EXPLAIN:
//            // CEDAR explain statement return string
//            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            executeSQL(sql.toString(), parts[parts.length - 1], operator);
//            varValueMap.put(variableName, result.getRowsByIndex()[0][0]);
//            result = null;
//            break;
//
//        case CEDAR_EXPLAIN_ERROR:
//            // -3 is CEDAR explain and exist exception
//            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            if (parts[parts.length - 2].equals("ERROR"))
//            {
//                executeSQL(sql.toString(), parts[parts.length - 3], operator);
//                varValueMap.put(variableName, exceptionString);
//                contain(exceptionString, parts[parts.length - 1]);
//            }
//            else
//            {
//                executeSQL(sql.toString(), parts[parts.length - 2], operator);
//            }
//            exceptionString = null;
//            break;
//
//        default:
//            throw new Exception(String.format("Use SQL undefined type in %s line %d", caseFileName,
//                    lineNumber));
//        }
//    }
//
//    /**
//     * 
//     * @param dataType The data type of variable
//     * @param variableName
//     * @param keyword Starts with "sql["
//     * @throws Exception
//     */
//    private void handleThirdGrammar(String dataType, String variableName, String keyword)
//            throws Exception
//    {
//        String[] parts = Util.removeBlankElement(keyword.split("\"|;|]"));
//        // Avoid erasing semicolon in sql
//        StringBuilder sql = new StringBuilder(parts[1]).append(SignConstant.SEMICOLON_CHAR);
//        if (parts[parts.length - 2].equals("ERROR"))
//        {
//            for (int i = 2; i < parts.length - 3; i++)
//            {
//                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//            }
//        }
//        else if (parts[parts.length - 1].equals("ERROR"))
//        {
//            for (int i = 2; i < parts.length - 2; i++)
//            {
//                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//            }
//        }
//        else
//        {
//            for (int i = 2; i < parts.length - 1; i++)
//                sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//        }
//        // for (int i = 2; i < parts.length - 2; i++)
//        // sql.append(parts[i]).append(SignConstant.SEMICOLON_CHAR);
//        SQLKeywordOperator operator = SQLKeywordOperator
//                .valueOf(getSQLOperator(sql.toString(), parts));
//
//        switch (operator)
//        {
//        case READ:
//            executeSQL(sql.toString(), parts[parts.length - 1], operator);
//            dealDiffType(variableName, result);
//            break;
//
//        case WRITE:
//            // executeUpdate
//            if (!dataType.equals(DataTypeConstant.INT_SHORT)
//                    && !dataType.equals(DataTypeConstant.LONG_SHORT)
//                    && !dataType.equals(DataTypeConstant.FLOAT_SHORT)
//                    && !dataType.equals(DataTypeConstant.DOUBLE_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            varValueMap.put(variableName,
//                    executeSQL(sql.toString(), parts[parts.length - 1], operator));
//            break;
//
//        case READ_ERROR:
//            // executeQuery and exist exception
//            //SQL[sql;stat;ERROR;err_message]
//            if (parts[parts.length - 2].equals("ERROR"))
//            {
//                executeSQL(sql.toString(), parts[parts.length - 3], operator);
//                varValueMap.put(variableName, exceptionString);
//                contain(exceptionString, parts[parts.length - 1]);
//            }
//            else
//                executeSQL(sql.toString(), parts[parts.length - 2], operator);
//            exceptionString = null;
//            result = null;
//            break;
//
//        case WRITE_ERROR:
//            // executeUpdate and exist exception
//            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            if (parts[parts.length - 2].equals("ERROR"))
//            {
//                executeSQL(sql.toString(), parts[parts.length - 3], operator);
//                varValueMap.put(variableName, exceptionString);
//                contain(exceptionString, parts[parts.length - 1]);
//            }
//            else
//                executeSQL(sql.toString(), parts[parts.length - 2], operator);
//            exceptionString = null;
//            break;
//
//        case CEDAR_EXPLAIN:
//            // CEDAR explain
//            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            executeSQL(sql.toString(), parts[parts.length - 1], operator);
//            varValueMap.put(variableName, result.getRowsByIndex()[0][0]);
//            result = null;
//            break;
//
//        case CEDAR_EXPLAIN_ERROR:
//            // CEDAR explain and exist exception
//            if (!varTypeMap.get(variableName).equals(DataTypeConstant.STRING_SHORT))
//                throw new Exception(
//                        String.format("Variable's data type don't match keyword in %s line %d",
//                                caseFileName, lineNumber));
//            if (parts[parts.length - 2].equals("ERROR"))
//            {
//                executeSQL(sql.toString(), parts[parts.length - 3], operator);
//                varValueMap.put(variableName, exceptionString);
//                contain(exceptionString, parts[parts.length - 1]);
//            }
//            else
//                executeSQL(sql.toString(), parts[parts.length - 2], operator);
//            exceptionString = null;
//            break;
//
//        default:
//            throw new Exception(String.format("Use SQL undefined type in %s line %d", caseFileName,
//                    lineNumber));
//        }
//    }
//
//    /**
//     * @param firstParameter
//     * @param secondParameter
//     * @throws Exception
//     */
//   public static void contain(String firstParameter, String secondParameter) throws Exception
//    {
//        StringBuilder origin = new StringBuilder(firstParameter);
//        int fromIndex = 0;
//        fromIndex = origin.indexOf(secondParameter, fromIndex);
//        if (fromIndex == -1)
//        {
//            throw new Exception("The error is different from expected error!");
//        }  
////        WpLog.recordLog(LogLevelConstant.INFO,
////                "The origin string is equal to specified number");
//        return;
//    }
//
//    /**
//     * @param sql
//     * @return sqlOperator
//     */
//    public static int getSQLOperator(String sql, String[] parts) throws Exception
//    {
//        String sqlType = sql.substring(sql.indexOf("\"") + 1, sql.indexOf(" "));
//        int sqlOperator = 0;
//        if (sqlType.contains("select"))
//        {
//            if (parts[parts.length - 1].equals("ERROR") || parts[parts.length - 2].equals("ERROR")){
//                sqlOperator = -1;
//            }
//            else{
//                sqlOperator = 1;
//            }
//            return sqlOperator;
//        }
//        else if (sqlType.contains("explain"))
//        {
//            if (parts[parts.length - 1].equals("ERROR") || parts[parts.length - 2].equals("ERROR")){
//                sqlOperator = -3;
//            }
//            else{
//                sqlOperator = 3;
//            }
//            return sqlOperator;
//        }
//        else{
//            if (parts[parts.length - 1].equals("ERROR") || parts[parts.length - 2].equals("ERROR")){
//                sqlOperator = -2;
//            }
//            else{
//                sqlOperator = 2;
//            }
//            return sqlOperator;
//        }
//    }
//
//    public static String shellExec(String str) throws Exception
//    {
//        String sql = str;
//        int count = 0;
//        for(int i = 0; i < sql.length(); i++)
//            if(str.charAt(i) == '$')
//                count++;
//        for(int i = 0; i < count; i++)
//        {
//            int index_$ = sql.indexOf("$");
//            int index_blank = sql.indexOf(" ", index_$);  //need to be improved
//            String var = sql.substring(index_$ + 1, index_blank);
//            if (varValueMap.containsKey(var))
//            {
//                sql = sql.replace('$' + var, varValueMap.get(var).toString());
//            }
//            else
//            {
//                throw new Exception("variable not exists!!!");
//            } 
//        }
//        return sql;
//    }
//    
//    /**
//     * 
//     * @param sql
//     * @param statementName
//     * @param operator Execute type
//     * @return Rows if type is executeUpdate, otherwise return -1
//     * @throws Exception
//     */
//    public static int executeSQL(String sql, String statementName, SQLKeywordOperator operator)
//            throws Exception
//    {
//        /**
//         * ��ģ��shell�б�����Ŀǰֻ֧��һ��������������ʽ��$׃����ͬ�r׃����һ��Ҫ��һ���ո�
//         */
//        int index_$ = sql.indexOf("$");
//        int index_blank = sql.indexOf(" ", index_$);
//        String var = sql.substring(index_$ + 1, index_blank);
//        if (varValueMap.containsKey(var))
//        {
//            sql = sql.replace('$' + var, varValueMap.get(var).toString());
//        }
//
//        Statement statement = (Statement) varValueMap.get(statementName);
//        int rows = -1;
//        switch (operator)
//        {
//        case READ:
//            result = BasicSQLOperation.stmtExecuteQuery(statement, sql, false);
//            break;
//        case WRITE:
//            rows = BasicSQLOperation.stmtExecuteUpdate(statement, sql, false);
//            break;
//        case READ_ERROR:
//            // executeQuery and will throw exception
//            BasicSQLOperation.stmtExecuteQuery(statement, sql, true);
//            break;
//        case WRITE_ERROR:
//            // executeUpdate and will throw exception
//            BasicSQLOperation.stmtExecuteUpdate(statement, sql, true);
//            break;
//        case CEDAR_EXPLAIN:
//            // The goal is row 0 column 0 of result
//            result = BasicSQLOperation.stmtExecuteQuery(statement, sql, false);
//            break;
//        case CEDAR_EXPLAIN_ERROR:
//            // CEDAR explain and will throw exception
//            BasicSQLOperation.stmtExecuteQuery(statement, sql, true);
//            break;
//        default:
//            throw new Exception(
//                    String.format("Wrong type in %s line %d", caseFileName, lineNumber));
//        }
//        return rows;
//    }
//    
//
//    public static void dealDiffType(String varName, Result result) throws Exception
//    {
//        SortedMap[] sortMap=result.getRows();
//        String columnAndValue=sortMap[0].toString().substring(1, sortMap[0].toString().length()-1);
//        String[] value=columnAndValue.split("=");
//        switch (varTypeMap.get(varName))
//        {
//        case DataTypeConstant.INT_SHORT:
//            varValueMap.put(varName, new Integer(value[1].toString()));
//            result=null;
//            break;
//        case DataTypeConstant.BOOLEAN_SHORT:
//            varValueMap.put(varName, new Boolean(value[1].toString()));
//            result=null;
//            break;
//        case DataTypeConstant.LONG_SHORT:
//            varValueMap.put(varName, new Long(value[1].toString()));
//            result=null;
//            break;
//        case DataTypeConstant.FLOAT_SHORT:
//            varValueMap.put(varName, new Float(value[1].toString()));
//            result=null;
//            break;
//        case DataTypeConstant.DOUBLE_SHORT:
//            varValueMap.put(varName, new Double(value[1].toString()));
//            result=null;
//            break;
//        case DataTypeConstant.STRING_SHORT:
//            varValueMap.put(varName, value[1].toString());
//            result=null;
//            break;
//        case DataTypeConstant.DECIMAL_SHORT:
//            varValueMap.put(varName, new BigDecimal(value[1].toString()));
//            result=null;
//            break;
//        case DataTypeConstant.RESULT_SET:
//            varValueMap.put(varName, result);
//            result=null;
//            break;
//        case DataTypeConstant.LIST_INTGER:
//            List<Integer> listInteger=new ArrayList<Integer>();
//            sortMap=result.getRows();
//            for(int i=0;i<result.getRowCount();i++)
//            {
//                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
//                value=columnAndValue.split("=");
//                if(value.length>2)
//                {
//                    throw new Exception("Resultset is more than one column!!!");
//                }
//                listInteger.add(new Integer(value[1].toString()));
//            }
//            varValueMap.put(varName, listInteger);
//            result=null;
//            break;
//        case DataTypeConstant.LIST_FLOAT:
//            List<Float> listFloat=new ArrayList<Float>();
//            sortMap=result.getRows();
//            for(int i=0;i<result.getRowCount();i++)
//            {
//                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
//                value=columnAndValue.split("=");
//                if(value.length>2)
//                {
//                    throw new Exception("Resultset is more than one column!!!");
//                }
//                listFloat.add(new Float(value[1].toString()));
//            }
//            varValueMap.put(varName, listFloat);
//            result=null;
//            break;
//        case DataTypeConstant.LIST_DOUBLE:
//            List<Double> listDouble=new ArrayList<Double>();
//            sortMap=result.getRows();
//            for(int i=0;i<result.getRowCount();i++)
//            {
//                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
//                value=columnAndValue.split("=");
//                if(value.length>2)
//                {
//                    throw new Exception("Resultset is more than one column!!!");
//                }
//                listDouble.add(new Double(value[1].toString()));
//            }
//            varValueMap.put(varName, listDouble);
//            result=null;
//            break;
//        case DataTypeConstant.LIST_BOOLEAN:
//            List<Boolean> listBoolean=new ArrayList<Boolean>();
//            sortMap=result.getRows();
//            for(int i=0;i<result.getRowCount();i++)
//            {
//                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
//                value=columnAndValue.split("=");
//                if(value.length>2)
//                {
//                    throw new Exception("Resultset is more than one column!!!");
//                }
//                listBoolean.add(new Boolean(value[1].toString()));
//            }
//            varValueMap.put(varName, listBoolean);
//            result=null;
//            break;
//        case DataTypeConstant.LIST_BIGDECIMAL:
//            List<BigDecimal> listBigDecimal=new ArrayList<BigDecimal>();
//            sortMap=result.getRows();
//            for(int i=0;i<result.getRowCount();i++)
//            {
//                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
//                value=columnAndValue.split("=");
//                if(value.length>2)
//                {
//                    throw new Exception("Resultset is more than one column!!!");
//                }
//                listBigDecimal.add(new BigDecimal(value[1].toString()));
//            }
//            varValueMap.put(varName, listBigDecimal);
//            result=null;
//            break;
//        case DataTypeConstant.LIST_LONG:
//            List<Long> listLong=new ArrayList<Long>();
//            sortMap=result.getRows();
//            for(int i=0;i<result.getRowCount();i++)
//            {
//                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
//                value=columnAndValue.split("=");
//                if(value.length>2)
//                {
//                    throw new Exception("Resultset is more than one column!!!");
//                }
//                listLong.add(new Long(value[1].toString()));
//            }
//            varValueMap.put(varName, listLong);
//            result=null;
//            break;
//        case DataTypeConstant.LIST_STRING:
//            List<String> listStirng=new ArrayList<String>();
//            sortMap=result.getRows();
//            for(int i=0;i<result.getRowCount();i++)
//            {
//                columnAndValue=sortMap[i].toString().substring(1, sortMap[0].toString().length()-1);
//                value=columnAndValue.split("=");
//                if(value.length>2)
//                {
//                    throw new Exception("Resultset is more than one column!!!");
//                }
//                listStirng.add(value[1].toString());
//            }
//            varValueMap.put(varName, listStirng);
//            result=null;
//            break;
//        default:
//            result=null;
//            throw new Exception ("Unsupported Datatype!");
//        }
//    }
}

package edu.ecnu.woodpecker.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.jstl.sql.Result;

import org.wltea.expression.ExpressionEvaluator;
import org.wltea.expression.datameta.Variable;

import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.DataType;
import edu.ecnu.woodpecker.constant.DataTypeConstant;
import edu.ecnu.woodpecker.constant.DataValueConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.executor.keyword.ConnectionGetter;
import edu.ecnu.woodpecker.executor.keyword.Keyword;
import edu.ecnu.woodpecker.executor.keyword.STKeywordProcessRealize;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.DbmsBrand;
import edu.ecnu.woodpecker.stresstest.Dispatcher;
import edu.ecnu.woodpecker.stresstest.PerformResult;
import edu.ecnu.woodpecker.util.Util;

/**
 * 对解析器生成的中间结果集进行执行，静态类
 * 
 */
public class Executor
{
    /**
     * 按行存放一个中间结果集文件里所有Statement，避免其他函数处理过程中产生IO Exception
     */
    private static String[] midResultStatement = null;

    /**
     * midResultStatement的下标索引，决定中间结果集的执行顺序
     */
    public static int index;

    /**
     * Use it to save time when load class into jvm
     */
    private static Map<String, Class<?>> cache = null;

    /**
     * 变量-值表，key为变量名，value为变量对应的值，值为字符串时带了双引号，使用时需去掉
     */
    protected static Map<String, Object> varValueMap = null;

    /**
     * 变量-类型表，key为变量名，value为变量的数据类型
     */
    protected static Map<String, String> varTypeMap = null;

    /**
     * key为预编译或存储过程执行器变量名，value是参数类型，与存储过程或预编译参数位置一一对应
     */
    protected static Map<String, List<String>> preparedParametersMap = null;
    
    /**
     * key为预编译时执行器变量名，value为对应的sql语句
     */
    protected static Map<String, String> pstatExecuteMap = null; 
    
    /**
     * 数据库的查询结果集，用jstl的Result类存储ResultSet的内容
     */
    protected static Result result = null;

    /**
     * 当前执行案例的名字
     */
    protected static String caseFileName = null;

    /**
     * 记录压力测试的负载编号，案例结束时需要停止还在运行的压力测试
     */
    protected static List<Integer> stressTestList = new ArrayList<Integer>();

    /**
     * 记录性能测试的负载编号，案例结束时需要等待所有的性能测试结束
     */
    protected static List<Integer> performTestList = new ArrayList<Integer>();

    /**
     * 用于处理MySQL的异常
     */
    protected static Connection connection = null;

    /**
     * 当前正在执行案例所在的组别名称
     */
    protected static String currentGroup = null;

    /**
     * SQL、PSQL关键字中意料之中的异常，存放此次异常的原因
     */
    protected static String exceptionString = null;

    /**
     * 行号
     */
    protected static int lineNumber;

    
    
    /**
     * 用于存放所有定义过的connection
     */
    protected static HashMap<String, Connection> connMap = new HashMap<>();
    
    /**
     * 当前正在执行SQL时用的connection
     */
    protected static Connection curConn = null;
    
    /**
     * 用于存放所有定义过的statement
     */
    protected static HashMap<String, Statement> statMap = new HashMap<>();
    
    /**
     * 当前正在执行SQL时用的statement
     */
    protected static Statement curStat = null;
    
    
    public Executor()
    {}

    /**
     * 执行解析器生成的中间结果集文件
     * 
     * @param midResult 中间结果集文件
     * @return 此案例是否通过
     */
    public static boolean execute(File midResult, Map<String, Object> varValueMap, Map<String, String> varTypeMap)
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Start to execute %s", midResult.getName());
        caseFileName = midResult.getName().substring(0, midResult.getName().indexOf(SignConstant.DOT_CHAR)) + FileConstant.CASE_FILE_SUFFIX;
        Executor.currentGroup = TestController.getCurrentGroup();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(midResult), FileConstant.UTF_8)))
        {
            // Initialize some Executor parameters
            initialize(varValueMap, varTypeMap);
            // Initialize stress test module's dispatcher
            Dispatcher.initialize(TestController.getServerUserName(), TestController.getServerPassword());

            List<String> mrStaList = new ArrayList<String>();
            String line = null;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                // 空行略过
                if (line.length() != 0)
                    mrStaList.add(line);
            }
            midResultStatement = (String[]) mrStaList.toArray(new String[0]);
            index = 0;

            for (; index < midResultStatement.length; index++)
                assignStatement();

            // 处理压力测试和性能测试
            if (!stressTestList.isEmpty())
            {
                WpLog.recordLog(LogLevelConstant.INFO, "Stop all stress test task");
                for (int ele : stressTestList)
                    Dispatcher.stopStressTest(ele);
            }
            if (!performTestList.isEmpty())
            {
                WpLog.recordLog(LogLevelConstant.INFO, "Get all perform test result");
                List<PerformResult> performResults = Dispatcher.getAllPerformResult(performTestList);
                // 将结果写入日志，以及存储展示结果
                for (int i = 0; i < performResults.size(); i++)
                {
                    WpLog.recordStressTestResult(performResults.get(i), String.format("Workload number = %d", performTestList.get(i)));
                }
            }
        }
        catch (Exception e)
        {
            // 接到异常说明有步骤执行失败
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            // 如果是MySQL则进行异常处理
            if (TestController.getDatabase() == DbmsBrand.MYSQL)
            {
                WpLog.recordLog(LogLevelConstant.ERROR, "Handle exception in MySQL, drop database woodpecker");
                try
                {
                    connection.createStatement().executeUpdate("Drop database if exists woodpecker");
                }
                catch (SQLException e1)
                {
                    e1.printStackTrace();
                }
            }
            return false;
        }
        finally
        {
            // 一个案例结束将建立的所有连接释放
            WpLog.recordLog(LogLevelConstant.INFO, "Close all connections which are created in %s", caseFileName);
            for (Map.Entry<String, String> entry : varTypeMap.entrySet())
            {
                if (entry.getValue().equals(DataTypeConstant.CONNECTION))
                {
                    Connection connection = (Connection) varValueMap.get(entry.getKey());
                    if (connection != null)
                    {
                        try
                        {
                            connection.close();
                        }
                        catch (SQLException e)
                        {}
                    }
                }
            }
        }
        return true;
    }

    /**
     * 利用工具计算此表达式的真假
     * 
     * @param expression 表达式
     * @return true or false
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
    protected static Object calExpression(String expression) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Calculate %s", expression);
        // 求表达式所含有的所有变量名
        List<String> list = new ArrayList<String>();
        String[] parts = expression.split("\\p{Punct}");
        for (String ele : parts)
        {
            if (!ele.trim().matches("^[-+]?(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)$") && !ele.trim().equals(DataValueConstant.TRUE)
                    && !ele.trim().equals(DataValueConstant.FALSE) && !ele.trim().equals(DataValueConstant.NULL_LOWER))
            {
                list.add(ele.trim());
            }
        }
        return useIKExpression(expression, list.toArray(new String[0]));
    }

    /**
     * 根据输入的变量名数组，返回BasicSQLOperation类对应的数据类型编号数组，数组中的参数类型与getParamValues方法返回的数组一一对应
     * 
     * @param variables 变量名或值的数组
     * @param statement 预编译或存储过程执行器名称
     * @return
     */
    protected static DataType[] getParamTypes(String[] variables, String statementName) throws Exception
    {
        if (variables == null)
            return null;
        DataType[] paramTypes = new DataType[variables.length];
        String type = null;
        for (int i = 0; i < paramTypes.length; i++)
        {
            type = varTypeMap.containsKey(variables[i]) ? varTypeMap.get(variables[i]).trim()
                    : preparedParametersMap.get(statementName).get(i);
            paramTypes[i] = DataType.of(type);
        }
        return paramTypes;
    }

    /**
     * 根据输入的变量名数组，返回变量对应的值的数组
     * 
     * @param variables 变量名数组
     * @return
     */
    protected static Object[] getParamValues(String[] variables)
    {
        if (variables == null)
            return null;
        Object[] paramValues = new Object[variables.length];
        for (int i = 0; i < paramValues.length; i++)
        {
            paramValues[i] = varValueMap.containsKey(variables[i].trim()) ? varValueMap.get(variables[i].trim())
                    : (variables[i].matches("(\"|').*(\"|')") ? variables[i].substring(1, variables[i].length() - 1) : variables[i]);
        }
        return paramValues;
    }

    /**
     * 练习使用Lambda表达式
     * 
     */
    @FunctionalInterface
    protected interface Action
    {
        /**
         * 返回某个变量名的真实数据类型全称
         * 
         * @param varName 某个数据类型名称
         * @return
         */
        public String getRealType(String varName);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T getComparableObject(Action action, String varName) throws ClassNotFoundException
    {
        return (T) Class.forName(action.getRealType(varName)).cast(varValueMap.get(varName));
    }
    
    /**
     * Initialize some parameters in Executor class
     * 
     * @param varValueMap
     * @param varTypeMap
     */
    private static void initialize(Map<String, Object> varValueMap, Map<String, String> varTypeMap)
    {
        // Set variable map
        Executor.varValueMap = varValueMap;
        Executor.varTypeMap = varTypeMap;
        Executor.preparedParametersMap = new HashMap<>();
        Executor.pstatExecuteMap = new HashMap<>();
        lineNumber = -1;
        cache = new HashMap<>();
        
        try
        {
            connection = ConnectionGetter.getConnection(ConfigConstant.MASTER_LOWER);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 根据文法类型，调用相应的文法处理函数
     * 
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
    public static void assignStatement() throws Exception
    {
        if (midResultStatement[index].matches("[0-9]+,0:.*$"))
        {
            // 0文法
            variableStatement();
            return;
        }
        if (midResultStatement[index].matches("[0-9]+,1:.*$"))
        {
            // 1文法
            functionStatement();
            return;
        }
        if (midResultStatement[index].matches("[0-9]+,2:.*$"))
        {
            // 2文法
            variableFunctionStatement();
            return;
        }
        if (midResultStatement[index].matches("[0-9]+,3:.*$"))
        {
            // 3文法
            declareFunctionStatement();
            return;
        }
        if (midResultStatement[index].matches("[0-9]+,4:.*$"))
        {
            // 4文法
            ifStatement();
            return;
        }
        if (midResultStatement[index].matches("[0-9]+,5:.*$"))
        {
            // 5文法
            whileStatement();
            return;
        }
        if (midResultStatement[index].matches("[0-9]+,7:.*$"))
        {
            // 7文法，处理st中涉及的关键字
            WpLog.recordLog(LogLevelConstant.INFO, "seven grammar");
            String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_CHAR) + 1).trim();
            //lineNumber = getLineNumber();
            STKeywordProcessRealize.stList.add(keyword);
        }
        if (midResultStatement[index].matches("[0-9]+,8:.*$"))
        {
            //8文法，遇到txn_loading时开始执行stmap中的相关操作
            WpLog.recordLog(LogLevelConstant.INFO, "eight grammar");
            String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_CHAR) + 1).trim();
            //lineNumber = getLineNumber();
            STKeywordProcessRealize.stList.add(keyword);
            STKeywordProcessRealize.execute();
            return;
        }
    }

    /**
     * 处理0文法，要么是只有变量声明，要么是变量声明同时有赋值
     * 
     * @throws Exception
     */
    private static void variableStatement() throws Exception
    {
        String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_STR) + 1).trim();
        lineNumber = getLineNumber();
        try
        {
            if (keyword.matches("[\\p{Alnum}<,\\s]+>?(\\s)+[\\p{Alnum}_$]+(\\s)*=.*"))
            {
                WpLog.recordLog(LogLevelConstant.INFO, keyword);
                // 此时是声明加赋值
                int equIndex = keyword.indexOf(SignConstant.ASSIGNMENT_CHAR);
                // leftPart有且仅有两个元素，0元素是变量类型，1元素是变量名，变量类型里面肯定不含空格
                String[] leftPart = Util.removeBlankElement(keyword.substring(0, equIndex).split("\\s+"));
                String rightPart = keyword.substring(equIndex + 1).trim();
                // 给变量赋值
                assignValueToVarValueMap(leftPart[0], leftPart[1], rightPart);
                return;
            }
        }
        catch (Exception e)
        {
            throw new Exception(String.format("lineNumber: %d has errors", lineNumber));
        }

        if (keyword.matches("[\\p{Alnum}<,\\s]+>?(\\s)+[\\p{Alnum}_$]+(\\s)*"))
        {
            WpLog.recordLog(LogLevelConstant.INFO, keyword);
            // 此时只有声明，变量加入varTypeMap和varValueMap已经在解析器阶段完成，执行器此时无动作
            
            // 需要更改当前conn or stat，根据varTypeMap来判断变量类型。  update for remove conn and stat in SQL
            String[] part = Util.removeBlankElement(keyword.split("\\s"));
            if(part[0].equalsIgnoreCase("connection") || part[0].equalsIgnoreCase("conn"))
            {
                if(connMap.containsKey(part[1]))
                {
                    curConn = connMap.get(part[1]);
                }
                else
                {
                    throw new Exception(String.format("lineNumber: %d has errors:connection undefined before!", lineNumber));
                }
            }
            if(part[0].equalsIgnoreCase("statement") || part[0].equalsIgnoreCase("stat"))
            {
                if(statMap.containsKey(part[1]))
                {
                    curStat = statMap.get(part[1]);
                }
                else
                {
                    throw new Exception(String.format("lineNumber: %d has errors:statement undefined before!", lineNumber));
                }
            }
            return;
        }
    }

    /**
     * Handle first grammar which only possesses keyword
     * 
     * @throws Exception
     */
    private static void functionStatement() throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "First grammar");
        String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_CHAR) + 1).trim();
        lineNumber = getLineNumber();
        // Get the class with keyword
        String className = null;
        //用以处理error这样关键字后没有[的情况
        if(keyword.indexOf(SignConstant.LBRACKET) != -1)
        {
            className = KeywordConstant.keywordClassMap.get(keyword.substring(0, keyword.indexOf(SignConstant.LBRACKET)));
        }
        else
        {
            className = KeywordConstant.keywordClassMap.get(keyword.toString());
        }
        Class<?> keywordClass = getClass(className);
        // Use state pattern
        Keyword state = (Keyword) keywordClass.newInstance();
        state.handle(keyword, GrammarType.FIRST_GRAMMAR);
    }

    /**
     * 处理2文法，即用关键字对已经声明的变量进行赋值的语句
     * 
     * @throws Exception
     */
    private static void variableFunctionStatement() throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Second grammar");
        String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_STR) + 1).trim();
        lineNumber = getLineNumber();
        // Get the class with keyword
        int beginIndex = keyword.indexOf(SignConstant.ASSIGNMENT_CHAR) + 1;
        int endIndex = keyword.indexOf(SignConstant.LBRACKET);
        String className = KeywordConstant.keywordClassMap.get(keyword.substring(beginIndex, endIndex).trim());
        Class<?> keywordClass = getClass(className);
        // Use state pattern
        Keyword state = (Keyword) keywordClass.newInstance();
        state.handle(keyword, GrammarType.SECOND_GRAMMAR);
    }

    /**
     * 处理3文法，即声明变量并用关键字赋值的语句
     * 
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
    protected static void declareFunctionStatement() throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Third grammar");
        // Remove line number and grammar type
        String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_CHAR) + 1).trim();
        lineNumber = getLineNumber();
        // Get class with keyword
        int beginIndex = keyword.indexOf(SignConstant.ASSIGNMENT_CHAR) + 1;
        int endIndex = keyword.indexOf(SignConstant.LBRACKET);
        String className = KeywordConstant.keywordClassMap.get(keyword.substring(beginIndex, endIndex).trim());
        Class<?> keywordClass = getClass(className);
        // Use state pattern
        Keyword state = (Keyword) keywordClass.newInstance();
        state.handle(keyword, GrammarType.THIRD_GRAMMAR);
    }

    /**
     * 处理4文法，即if语句，连同附属的else一起处理
     * 
     * @param index midResultStatement的下标索引
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
    private static void ifStatement() throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "4th grammar, if statement");
        lineNumber = getLineNumber();
        String keywords = midResultStatement[index].substring(midResultStatement[index].indexOf(":") + 1).trim();
        // 下标1为if语句中表达式字符串
        String[] parts = Util.removeBlankElement(keywords.split("\\[|]|\\{"));
        if (calExpression(parts[1]).equals(true) ? true : false)
        {
            WpLog.recordLog(LogLevelConstant.INFO, "%s expression is true", keywords);
            // 表达式为真
            blockStatement();

            if (index + 1 < midResultStatement.length)
            {
                // 避免越界
                if (midResultStatement[index + 1].matches("[0-9]+,4\\s*:\\s*else\\s*\\{\\s*$"))
                {
                    // 略过else块
                    index++;
                    int braceCount = 1;
                    for (; braceCount > 0;)
                    {
                        index++;
                        if (midResultStatement[index].matches(".*\\{\\s*$"))
                        {
                            // 遇到'{'
                            braceCount++;
                            continue;
                        }
                        if (midResultStatement[index].matches("[0-9]+,6\\s*:\\s*}\\s*$"))
                        {
                            // 遇到'}'
                            braceCount--;
                        }
                    }
                }
            }
        }
        else
        {
            WpLog.recordLog(LogLevelConstant.INFO, "%s expression is false", keywords);
            // 表达式为假，略过if语句块
            int braceCount = 1;
            for (; braceCount > 0;)
            {
                index++;
                if (midResultStatement[index].matches(".*\\{\\s*$"))
                {
                    // 遇到'{'
                    braceCount++;
                    continue;
                }
                if (midResultStatement[index].matches("[0-9]+,6\\s*:\\s*}\\s*$"))
                {
                    // 遇到'}'
                    braceCount--;
                }
            }

            if (index + 1 < midResultStatement.length)
            {
                // 避免越界
                if (midResultStatement[index + 1].matches("[0-9]+,4\\s*:\\s*else\\s*\\{\\s*$"))
                {
                    // 存在else块
                    index++;
                    blockStatement();
                }
            }
        }
    }

    /**
     * 处理5文法，即while语句
     * 
     * @param index midResultStatement的下标索引
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
//    private static void whileStatement() throws Exception
//    {
//        WpLog.recordLog(LogLevelConstant.INFO, "5th grammar, while statement");
//        String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_STR) + 1);
//        // 下标1为循环次数
//        String[] parts = Util.removeBlankElement(keyword.split("\\[|]|\\{"));
//        int loopCount = Integer.parseInt(parts[1]);
//
//        int begin = blockStatement(true);
//        for (loopCount--; loopCount > 0; loopCount--)
//            blockStatement(begin, index);
//    }
    private static void whileStatement() throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "5th grammar, while statement");
        String keyword = midResultStatement[index].substring(midResultStatement[index].indexOf(SignConstant.COLON_STR) + 1);
        // 下标1为循环次数
        String[] parts = Util.removeBlankElement(keyword.split("\\[|]|\\{"));

        int begin = blockStatement(true);
        while(calExpression(parts[1]).equals(true))
        {
            blockStatement(begin, index);
        }
            
    }

    /**
     * block语句的做法1，丢给相应文法函数，遇到'}'返回，开始时index位置在'{'行，结束时index在'}'行
     * 
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
    private static void blockStatement() throws Exception
    {
        int braceCount = 1;
        for (; braceCount > 0;)
        {
            index++;
            // 6文法，遇到右大括号
            if (midResultStatement[index].matches("[0-9]+,6\\s*:\\s*\\}\\s*$"))
                braceCount--;
            else
                assignStatement();
        }
    }

    /**
     * block语句的做法2，执行一遍此block里所有语句，并保存语句起始位置，开始时index位置在'{'行，结束时index在'}'行
     * 
     * @param store 如果为false则与block语句做法1一样
     * @return 此block里语句的起始位置，即'{'所在行
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
    private static int blockStatement(boolean store) throws Exception
    {
        if (!store)
        {
            // 目前此分支不会使用
            blockStatement();
            return -1;
        }

        int begin = index;
        int braceCount = 1;
        for (; braceCount > 0;)
        {
            index++;
            // 6文法，遇到右大括号
            if (midResultStatement[index].matches("[0-9]+,6\\s*:\\s*\\}\\s*$"))
                braceCount--;
            else
                assignStatement();
        }
        return begin;
    }

    /**
     * block语句的做法3，执行一遍给定区间的语句一次
     * 
     * @param begin
     * @param end
     * @throws Exception 用于向execute函数反馈执行是否成功
     */
    private static void blockStatement(int begin, int end) throws Exception
    {
        int storeIndex = index;
        index = begin + 1;
        for (; index < end; index++)
            assignStatement();
        index = storeIndex;
    }

   

    /**
     * 获取当前index指向行的测试案例中的行号，用于日志记录所需的行号
     * 
     * @return
     */
    private static int getLineNumber()
    {
        return Integer.parseInt(midResultStatement[index].substring(0, midResultStatement[index].indexOf(SignConstant.COMMA_STR)));
    }

    /**
     * 用IK Expression计算表达式的值
     * 
     * @param expression 表达式字符串
     * @param variables 表达式所含变量名
     * @return 表达式的值
     */
    private static Object useIKExpression(String expression, String[] variables) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Start to use IK Expression");
        // 构造IK Expression输入参数
        List<Variable> varValues = new ArrayList<Variable>();
        for (String ele : variables)
        {
            if (!varValueMap.containsKey(ele) || varValueMap.get(ele) == null)
                throw new Exception(String.format("%s is undefined or null", ele));
            switch (varTypeMap.get(ele))
            {
            case DataTypeConstant.INT_SHORT:
                varValues.add(Variable.createVariable(ele, (int) varValueMap.get(ele)));
                break;
            case DataTypeConstant.BOOLEAN_SHORT:
                varValues.add(Variable.createVariable(ele, (boolean) varValueMap.get(ele)));
                break;
            case DataTypeConstant.CHAR_SHORT:
                varValues.add(Variable.createVariable(ele, (char) varValueMap.get(ele)));
                break;
            case DataTypeConstant.LONG_SHORT:
                varValues.add(Variable.createVariable(ele, (long) varValueMap.get(ele)));
                break;
            case DataTypeConstant.FLOAT_SHORT:
                varValues.add(Variable.createVariable(ele, (float) varValueMap.get(ele)));
                break;
            case DataTypeConstant.DOUBLE_SHORT:
                varValues.add(Variable.createVariable(ele, (double) varValueMap.get(ele)));
                break;
            case DataTypeConstant.STRING_SHORT:
                String value = (String) varValueMap.get(ele);
                if (value.charAt(0) == SignConstant.DOUBLE_QUOTE_CHAR && value.charAt(value.length() - 1) == SignConstant.DOUBLE_QUOTE_CHAR)
                    value = value.substring(1, value.length() - 1);
                varValues.add(Variable.createVariable(ele, value));
                break;
            case DataTypeConstant.DECIMAL_SHORT:
                // 用Java中的BigDecimal与SQL中的Decimal对应
                varValues.add(Variable.createVariable(ele, (BigDecimal) varValueMap.get(ele)));
                break;
            default:
                // Object类型
                varValues.add(Variable.createVariable(ele, varValueMap.get(ele)));
                break;
            }
        }
        // 计算表达式
        return ExpressionEvaluator.evaluate(expression, varValues);
    }

    /**
     * 根据变量类型将变量的值以正确的类型存入varValueMap中，只支持int, long, boolean, char, String, Decimal, List
     * <E>, float, double；其余类型不处理，varValueMap里value依旧是null
     * 
     * @param dataType 变量类型
     * @param varName 变量名
     * @param value 变量的值，传入前必须先经过trim
     */
    private static void assignValueToVarValueMap(String dataType, String varName, String value) throws Exception
    {
        // value是整型
        if (dataType.equals(DataTypeConstant.INT_SHORT))
        {
            try
            {
                // value是10进制整型的值
                if (value.matches("-?[0-9]([0-9])*"))
                    varValueMap.put(varName, Integer.parseInt(value));

                // value是16进制整型的值
                if (value.matches("-?0[x,X]([0-9,a-f,A-F])+"))
                {
                    value = value.substring(2);
                    varValueMap.put(varName, Integer.parseInt(value, 16));
                }

                // value是8进制整型的值
                if (value.matches("-?0[1-9]([0-9])*"))
                    varValueMap.put(varName, Integer.parseInt(value, 8));
            }
            catch (Exception e1)
            {
                // 超过int范围,记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是长整形
        if (dataType.equals(DataTypeConstant.LONG_SHORT))
        {
            try
            {
                // value是10进制整型的值
                if (value.matches("-?[1-9]([0-9])*"))
                    varValueMap.put(varName, Long.parseLong(value));

                // value是16进制整型的值
                if (value.matches("-?0[x,X]([0-9,a-f,A-F])+"))
                {
                    value = value.substring(2);
                    varValueMap.put(varName, Long.parseLong(value, 16));
                }

                // value是8进制整型的值
                if (value.matches("-?0[1-9]([0-9])*"))
                    varValueMap.put(varName, Long.parseLong(value, 8));
            }
            catch (Exception e1)
            {
                // 超过long范围,记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是float类型的值
        if (dataType.equals(DataTypeConstant.FLOAT_SHORT))
        {
            try
            {
                varValueMap.put(varName, Float.parseFloat(value));
            }
            catch (Exception e)
            {
                // 超出float范围，记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是double类型的值
        if (dataType.equals(DataTypeConstant.DOUBLE_SHORT))
        {
            try
            {
                varValueMap.put(varName, Double.parseDouble(value));
            }
            catch (Exception e)
            {
                // 超出double范围，记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是boolean类型的值
        if (dataType.equals(DataTypeConstant.BOOLEAN_SHORT))
        {
            try
            {
                varValueMap.put(varName, Boolean.parseBoolean(value));
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是char类型的值
        if (dataType.equals(DataTypeConstant.CHAR_SHORT))
        {
            try
            {
                varValueMap.put(varName, value.charAt(1));
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是String类型的值，顺便去掉双引号
        if (dataType.equals(DataTypeConstant.STRING_SHORT))
        {
            try
            {
                varValueMap.put(varName, value.substring(1, value.length() - 1));
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是Decimal类型的值
        if (dataType.equals(DataTypeConstant.DECIMAL_SHORT))
        {
            try
            {
                varValueMap.put(varName, new BigDecimal(value));
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是List类型的值
        if (dataType.startsWith(DataTypeConstant.LIST))
        {
            // 默认dataType里面不包含任何空格，最紧凑的类型
            String[] values = Util.removeBlankElement(value.split("\\s+|,|\\[|]"));
            switch (dataType.substring(5, dataType.length() - 1).trim())
            {
            case DataTypeConstant.INT_SHORT:
                List<Integer> intList = new ArrayList<Integer>();
                for (String ele : values)
                    intList.add((int) convert(DataTypeConstant.INT_SHORT, ele));
                varValueMap.put(varName, intList);
                break;

            case DataTypeConstant.LONG_SHORT:
                List<Long> longList = new ArrayList<Long>();
                for (String ele : values)
                    longList.add((long) convert(DataTypeConstant.LONG_SHORT, ele));
                varValueMap.put(varName, longList);
                break;

            case DataTypeConstant.BOOLEAN_SHORT:
                List<Boolean> booleanList = new ArrayList<Boolean>();
                for (String ele : values)
                    booleanList.add((boolean) convert(DataTypeConstant.BOOLEAN_SHORT, ele));
                varValueMap.put(varName, booleanList);
                break;

            case DataTypeConstant.CHAR_SHORT:
                List<Character> charList = new ArrayList<Character>();
                for (String ele : values)
                    charList.add((char) convert(DataTypeConstant.CHAR_SHORT, ele));
                varValueMap.put(varName, charList);
                break;

            case DataTypeConstant.FLOAT_SHORT:
                List<Float> floatList = new ArrayList<Float>();
                for (String ele : values)
                    floatList.add((float) convert(DataTypeConstant.FLOAT_SHORT, ele));
                varValueMap.put(varName, floatList);
                break;

            case DataTypeConstant.DOUBLE_SHORT:
                List<Double> doubleList = new ArrayList<Double>();
                for (String ele : values)
                    doubleList.add((double) convert(DataTypeConstant.DOUBLE_SHORT, ele));
                varValueMap.put(varName, doubleList);
                break;

            case DataTypeConstant.STRING_SHORT:
                List<String> stringList = new ArrayList<String>();
                for (String ele : values)
                    stringList.add((String) convert(DataTypeConstant.STRING_SHORT, ele));
                varValueMap.put(varName, stringList);
                break;

            case DataTypeConstant.DECIMAL_SHORT:
                List<BigDecimal> bdList = new ArrayList<BigDecimal>();
                for (String ele : values)
                    bdList.add((BigDecimal) convert(DataTypeConstant.DECIMAL_SHORT, ele));
                varValueMap.put(varName, bdList);
                break;
            case DataTypeConstant.TIMESTAMP_SHORT:
                List<Timestamp> timestampList = new ArrayList<>();
                for (String ele : values)
                    timestampList.add((Timestamp) convert(DataTypeConstant.TIMESTAMP_SHORT, ele));
                varValueMap.put(varName, timestampList);
                break;
            default:
                throw new Exception(String.format("lineNumber: %d, List data type is wrong in %s", lineNumber, caseFileName));
            }
            return;
        }

        // value是Timestamp类型的值，格式符合必须yyyy-[m]m-[d]d [h]h:[m]m:[s]s[.f...]
        if (dataType.equals(DataTypeConstant.TIMESTAMP_SHORT))
        {
            varValueMap.put(varName, Timestamp.valueOf(value.substring(1, value.length() - 1)));
            return;
        }

        // 其余类型直接返回，不处理
        return;
    }

    /**
     * 将字符串转换为指定数据类型的值
     * 
     * @param dataType 数据类型，只支持int,long,char,boolean,float,double,String,Decimal,Timestamp
     * @param value 值，一次只有一个
     * @return 不正确时返回null
     */
    private static Object convert(String dataType, String value) throws Exception
    {
        // value是整型
        if (dataType.equals(DataTypeConstant.INT_SHORT))
        {
            try
            {
                // value是10进制整型的值
                if (value.matches("-?[1-9]([0-9])*"))
                    return Integer.parseInt(value);
                // value是16进制整型的值
                if (value.matches("-?0[x,X]([0-9,a-f,A-F])+"))
                {
                    value = value.substring(2);
                    return Integer.parseInt(value, 16);
                }
                // value是8进制整型的值
                if (value.matches("-?0[1-9]([0-9])*"))
                    return Integer.parseInt(value, 8);
            }
            catch (Exception e1)
            {
                // 超过int范围,记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
            return null;
        }
        // value是长整形
        if (dataType.equals(DataTypeConstant.LONG_SHORT))
        {
            try
            {
                // value是10进制整型的值
                if (value.matches("-?[1-9]([0-9])*"))
                    return Long.parseLong(value);
                // value是16进制整型的值
                if (value.matches("-?0[x,X]([0-9,a-f,A-F])+"))
                {
                    value = value.substring(2);
                    return Long.parseLong(value, 16);
                }
                // value是8进制整型的值
                if (value.matches("-?0[1-9]([0-9])*"))
                    return Long.parseLong(value, 8);
            }
            catch (Exception e1)
            {
                // 超过long范围,记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
            return null;
        }
        // value是float类型的值
        if (dataType.equals(DataTypeConstant.FLOAT_SHORT))
        {
            try
            {
                return Float.parseFloat(value);
            }
            catch (Exception e)
            {
                // 超出float范围，记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
        }
        // value是double类型的值
        if (dataType.equals(DataTypeConstant.DOUBLE_SHORT))
        {
            try
            {
                return Double.parseDouble(value);
            }
            catch (Exception e)
            {
                // 超出double范围，记日志，给个错误，说此值超过限度，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value is out of range in %s", lineNumber, caseFileName));
            }
        }
        // value是boolean类型的值
        if (dataType.equals(DataTypeConstant.BOOLEAN_SHORT))
        {
            try
            {
                return Boolean.parseBoolean(value);
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
        }
        // value是char类型的值
        if (dataType.equals(DataTypeConstant.CHAR_SHORT))
        {
            try
            {
                return value.charAt(1);
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
        }
        // value是String类型的值，顺便去掉双引号
        if (dataType.equals(DataTypeConstant.STRING_SHORT))
        {
            try
            {
                return value.substring(1, value.length() - 1);
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
        }
        // value是Decimal类型的值
        if (dataType.equals(DataTypeConstant.DECIMAL_SHORT))
        {
            try
            {
                return new BigDecimal(value);
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
        }
        // value是Timestamp类型的值，顺便去掉双引号
        if (dataType.equals(DataTypeConstant.TIMESTAMP_SHORT))
        {
            try
            {
                return Timestamp.valueOf(value.substring(1, value.length() - 1));
            }
            catch (Exception e)
            {
                // 记日志，给个错误，说格式有错，此case直接错误
                throw new Exception(String.format("lineNumber: %d the value's format is wrong in %s", lineNumber, caseFileName));
            }
        }
        return null;
    }

    /**
     * Get the keyword class according to class name, if it doesn't exists in cache, it
     * should be load into jvm
     * 
     * @param className Include package name
     * @return
     */
    private static Class<?> getClass(String className)
    {
        if (cache.containsKey(className))
            return cache.get(className);
        else
        {
            try
            {
                Class<?> keywordClass = Class.forName(className);
                cache.put(className, keywordClass);
                return keywordClass;
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
                // It shouldn't happen, system must exit when it happen
                System.exit(1);
            }
            return null;
        }
    }

    public static void setVarValueMap(Map<String, Object> varValueMap)
    {
        Executor.varValueMap = varValueMap;
    }

    public static Map<String, Object> getVarValueMap()
    {
        return Executor.varValueMap;
    }

    public static void setVarTypeMap(Map<String, String> varTypeMap)
    {
        Executor.varTypeMap = varTypeMap;
    }

    public static void setExceptionString(String exceptionString)
    {
        Executor.exceptionString = exceptionString;
    }
    
    public static Connection getConnection()
    {
        return Executor.connection;
    }
    
    public static String getMIdresult(int index)
    {
    	String str = null;
    	if(index >= midResultStatement.length)
    		str = "outofindex";
    	else
    		str = midResultStatement[index].toString();
        return str;
    }
}
package edu.ecnu.woodpecker.log;

import javax.servlet.jsp.jstl.sql.Result;

import edu.ecnu.woodpecker.sql.IdealResultSet;
import edu.ecnu.woodpecker.stresstest.PerformResult;

public interface WpLog
{
    /**
     * input general log and log level
     * @param logLevel INFO, ERROR, WARN, DEBUG, FATAL
     * @param logInfo 
     * @param args output to logInfo
     */
    public static void recordLog(String logLevel, String logInfo, Object... args)
    {
        Recorder.FunctionRecord(getRecordMetadata(), String.format(logInfo, args), logLevel);
    }

    /**
     * input ideal result set
     * @param idealResultSet
     */
    public static void recordIdealResultSet(String logLevel, IdealResultSet idealResultSet)
    {
        Recorder.SQLRecord(idealResultSet, getRecordMetadata(), logLevel);
    }

    /**
     * input query result set
     * @param result
     */
    public static void recordQueryResultSet(String logLevel, Result result)
    {
        Recorder.SQLRecord(result, getRecordMetadata(), logLevel);
    }

    /**
     * input query result set and ideal result set
     */
    public static void recordQueryAndIdealResultSet(String logLevel, Result result, IdealResultSet idealResultSet)
    {
        Recorder.SQLRecord(result, idealResultSet, getRecordMetadata(), logLevel);
    }

    /**
     * @param logInfo include group name and case file name, or cluster start stop info
     * @param args output to logInfo
     */
    public static void recordTestflow(String logInfo, Object... args)
    {
        Recorder.WorkflowControllerRecord(getRecordMetadata(), logInfo);
    }

    /**
     * get exception info and occur line
     * 
     * @return exception info and line number
     */
    public static String getExceptionInfo(Exception exception)
    {
        return String.format("%s ; %s", exception.toString(), exception.getStackTrace()[0].toString());
    }

    /**
     * input perform result 
     * 
     * @param performResult 
     * @param stressInfo 
     */
    public static void recordStressTestResult(PerformResult performResult, String stressInfo)
    {
        Recorder.StressTestRecord(performResult, getRecordMetadata(), stressInfo);
    }

    /**
     * generate general report
     */
    public static void generateReport()
    {
        RecordReport.recordReport();
    }

    /**
     * generate stress report
     */
    public static void generateStressReport()
    {
        RecordReport.stressRecordReport();
    }

    /**
     * @deprecated
     */
    public static void deleteLog()
    {
        RecordAnalysis.deleteLog();
    }
    
    /**
     * get module info, include class name, method name and line number
     * @return module info
     */
    public static String getRecordMetadata()
    {
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];// 3 means higher 2 level
        return String.format("%s->%s:%d", element.getClassName(), element.getMethodName(), element.getLineNumber());
    }
}

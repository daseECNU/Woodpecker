package edu.ecnu.woodpecker.executor.keyword;

import java.util.concurrent.Future;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.executor.StressTestOperator;
import edu.ecnu.woodpecker.executor.StressTestWorkload;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.stresstest.Dispatcher;
import edu.ecnu.woodpecker.stresstest.PerformResult;
import edu.ecnu.woodpecker.util.Util;

public class StressTestProcessor extends Executor implements Keyword
{
    public StressTestProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "ST: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
            stressTestList.add(Integer.parseInt(parts[1]));
            testStress(StressTestWorkload.valueOf(Integer.parseInt(parts[1])), Integer.parseInt(parts[2]), StressTestOperator.of(parts[3]),
                    StressTestOperator.of(parts[4]));
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
     * @param keyword Starts with "st["
     * @throws Exception
     */
    private void handleSecondGrammar(String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "2th grammar, ST keyword");
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        performTestList.add(Integer.parseInt(parts[1]));
        Future<PerformResult> future = testStress(StressTestWorkload.valueOf(Integer.parseInt(parts[1])), Integer.parseInt(parts[2]),
                StressTestOperator.of(parts[3]), StressTestOperator.of(parts[4]));
        varValueMap.put(variableName, future);
    }

    /**
     * 
     * @param dataType The data type of variable
     * @param variableName
     * @param keyword Starts with "st["
     * @throws Exception
     */
    private void handleThirdGrammar(String dataType, String variableName, String keyword) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "3th grammar, ST keyword");
        String[] parts = Util.removeBlankElement(keyword.split("\\[|;|]"));
        performTestList.add(Integer.parseInt(parts[1]));
        Future<PerformResult> future = testStress(StressTestWorkload.valueOf(Integer.parseInt(parts[1])), Integer.parseInt(parts[2]),
                StressTestOperator.of(parts[3]), StressTestOperator.of(parts[4]));
        varValueMap.put(variableName, future);
    }

    /**
     * 
     * @param workload
     * @param runTimes
     * @return FutureTask
     * @throws Exception
     */
    private Future<PerformResult> testStress(StressTestWorkload workload, int runTimes, StressTestOperator resultHandle,
            StressTestOperator execType) throws Exception
    {
        return Dispatcher.startStressTest(workload, runTimes, resultHandle, execType);
    }
}

package edu.ecnu.woodpecker.executor.keyword;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;

public class ErrorProcessor extends Executor implements Keyword
{
    public ErrorProcessor()
    {}

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.ERROR, "ERROR keyword");
        throw new Exception(String.format("Occur ERROR keyword in line %d", lineNumber));
    }
}

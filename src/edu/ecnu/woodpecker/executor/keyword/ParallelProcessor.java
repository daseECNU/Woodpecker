package edu.ecnu.woodpecker.executor.keyword;

import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.GrammarType;
import edu.ecnu.woodpecker.log.WpLog;


public class ParallelProcessor extends Executor implements Runnable,Keyword
{
    public static int threadnum = 0;
    public static CountDownLatch countDownLatch ;
    public static int containSqlNum = 0;
    
    /**
     * 记录并行块执行结束后的index值
     */
    public static int para_index = 0;
    
    public Vector<Integer> SQLvector = new Vector<>();
    public Vector<Vector<Integer>> addSQLvector = new Vector<Vector<Integer>>();
    
    
    public ParallelProcessor()
    {}
    
    public ParallelProcessor(Vector<Integer> vector,CountDownLatch countDownLatch2)
    {
        this.SQLvector = vector;
        ParallelProcessor.countDownLatch = countDownLatch2;
    }
    
    
    @Override
    public void run()
    {
        // TODO Auto-generated method stub
        //int index = 0;
        for(int i=0; i<SQLvector.size(); i++)
        {
            synchronized(this)
            {
                Executor.index = SQLvector.get(i);
                try
                {
                    Executor.assignStatement();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
//            try
//            {
//                executeStatement(SQLvector.get(i));
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
        }
        countDownLatch.countDown();
    }
    
    public void execute()
    {
        Vector<Integer> vector = new Vector<>();
        CountDownLatch countDownLatch = new CountDownLatch(addSQLvector.size());
        for(int i=0; i < addSQLvector.size(); i++)
        {
            vector = addSQLvector.get(i);
            new Thread(new ParallelProcessor(vector,countDownLatch)).start();
        }
        try
        {
            countDownLatch.await();
            Executor.index = para_index;
            addSQLvector = new Vector<Vector<Integer>>();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(String keyword, GrammarType type) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "PSQL: %s", keyword);
        switch (type)
        {
        case FIRST_GRAMMAR:
            int para_begin_index = Executor.index;
            para_index = para_begin_index+1;
            while(!getMIdresult(para_index).contains("end_parall"))
            {
                if(getMIdresult(para_index).contains("mid_parall"))
                {
                    addSQLvector.add(SQLvector);
                    SQLvector = new Vector<Integer>();
                    para_index++;
                    continue;
                }
                SQLvector.add(para_index);
                para_index++;
            }
            addSQLvector.add(SQLvector);
            SQLvector = new Vector<Integer>();
            execute();
            break;
        case SECOND_GRAMMAR:
            //TODO
            break;
        case THIRD_GRAMMAR:
            //TODO
            break;
        default:
            throw new Exception("Grammar error");
        }
    }

}

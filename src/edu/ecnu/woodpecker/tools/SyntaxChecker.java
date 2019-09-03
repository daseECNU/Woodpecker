package edu.ecnu.woodpecker.tools;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.executor.parser.WoodpeckerParser;

/**
 * check case's syntax, include woodpecker syntax and sql syntax
 * Usage: java -jar Woodpecker.jar -syntax_check [file.case, directory]
 * Attention: must config Woodpecker.conf
 */
public class SyntaxChecker
{
    private Map<String, Object> varValueMap = null;
    private Map<String, String> varTypeMap = null;
    private File middleResultOutputFile = null;

    public SyntaxChecker()
    {
        varValueMap = new HashMap<String, Object>();
        varTypeMap = new HashMap<String, String>();
        WoodpeckerParser.setVarMap(varValueMap, varTypeMap);
        middleResultOutputFile = new File(FileConstant.SYNTAX_CHECKER_OUTPUT_PATH);
    }

    /**
     * process each file under directory recursively
     * @param file
     */
    private void processEachFile(File file)
    {
        if (file.isDirectory())
        {
            File[] fileList = file.listFiles();
            for (File ele : fileList)
                processEachFile(ele);
        }
        else
        {
            // process one file
            try (PrintWriter midResultPW = new PrintWriter(middleResultOutputFile, FileConstant.UTF_8);)
            {
                WoodpeckerParser.setMidResultPW(midResultPW);
                boolean pass = WoodpeckerParser.parseCaseFile(file);
                midResultPW.flush();
                if (pass)
                    System.out.println(file.getAbsolutePath() + " ------ pass");
                else
                    System.out.println(file.getAbsolutePath() + " ------ fail");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Entry of checker
     * 
     * @param path [in] case file or directory's path
     */
    public void check(String path)
    {
        File file = new File(path);
        if (!file.exists())
        {
            System.out.println("file or directory does not exist");
            return;
        }
        processEachFile(file);
        // delete check.mr
        middleResultOutputFile.delete();
    }
}

package edu.ecnu.woodpecker.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.controller.TestController;

public class RecordReport
{
    /**
     * 生成报告
     */
    public static void recordReport()
    {
        String filename = "case_report.md";
        writeHeaderFile(filename);
        ArrayList<ArrayList<String>> content = RecordAnalysis.readRecord();
        int rowNum = content.size();
        int time = 0;
        int cloumnNum = 0;
        for (int i = 0; i < rowNum; i++)
        {
            for (int j = 1; j <= 6; j++)
            {
                // System.out.print(content.get(i).get(j) + " ");
                write("|" + content.get(i).get(j), filename);
            }
            // System.out.println();
            write("|" + FileConstant.LINUX_LINE_FEED, filename);
            time = Integer.valueOf(content.get(i).get(2));
            cloumnNum = content.get(i).size();
            if (time > 1)
            {
                for (int k = time - 1; k > 0; k--)
                {
                    for (int j = 0; j < 3; j++)
                    {
                        // System.out.print(" ");
                        write("|    ", filename);
                    }
                    // System.out.print(content.get(i).get(cloumnNum - 3 * k) + " ");
                    // System.out.print(content.get(i).get(cloumnNum - 3 * k + 1) + " ");
                    // System.out.print(content.get(i).get(cloumnNum - 3 * k + 2) + " ");
                    // System.out.println();
                    write("|" + content.get(i).get(cloumnNum - 3 * k), filename);
                    write("|" + content.get(i).get(cloumnNum - 3 * k + 1), filename);
                    write("|" + content.get(i).get(cloumnNum - 3 * k + 2), filename);
                    write("|" + "\r\n", filename);
                }
            }

        }
        write("\r\n\r\n\r\n", filename);
    }

    /**
     * 生成压测报告
     */
    public static void stressRecordReport()
    {
        String filename = "stress_report.md";
        writeStressHeaderFile(filename);
        ArrayList<ArrayList<String>> content = RecordAnalysis.readStressRecord();
        int rowNum = content.size();
        for (int i = 0; i < rowNum; i++)
        {
            // System.out.println("----" + content.get(i).size());
            for (int j = 1; j < content.get(i).size(); j++)
            {
                // System.out.print(content.get(i).get(j) + " ");
                write("|" + content.get(i).get(j), filename);
            }
            // System.out.println();
            write("|" + "\r\n", filename);

        }
        write("\r\n\r\n\r\n", filename);
    }

    /**
     * 报告格式写入
     */
    public static void writeStressHeaderFile(String filename)
    {
        // String fieName;
        File file = new File(TestController.getReportPath(), filename);

        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        String aa = "|case_id|execute_time|stressInfo|avgTPS|avgQPS|avgResponseTime|TP50ResponseTime|TP90ResponseTime|TP99ResponseTime|"
                + "\r\n";
        writeHead(aa, filename);
        aa = "| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |"
                + "\r\n";
        write(aa, filename);
    }

    /**
     * 报告格式写入
     */
    public static void writeHeaderFile(String filename)
    {
        // String fieName;
        File file = new File(TestController.getReportPath(), filename);
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        String aa = "|case_id|execute_time|final_state|execute_order|state|timestamp|" + "\r\n";
        writeHead(aa, filename);
        aa = "| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |" + "\r\n";
        write(aa, filename);
    }

    /**
     * 写入报告文件
     * 
     * @param aa 写入信息
     */
    private static void write(String aa, String filename)
    {
        // String fieName;
        File file = new File(TestController.getReportPath() + filename);
        try
        {
            FileWriter fileWritter = new FileWriter(file.getName(), true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            // System.out.println("----------------" + aa);
            bufferWritter.write(aa);
            bufferWritter.flush();
            bufferWritter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public static void main(String[] args)
    {
        stressRecordReport();
    }

    /**
     * 写入报告文件
     * 
     * @param aa 写入信息
     */
    private static void writeHead(String aa, String filename)
    {
        // String fieName;
        File file = new File(TestController.getReportPath() + filename);
        try
        {
            FileWriter fileWritter = new FileWriter(file.getName(), false);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            // System.out.println("----------------" + aa);
            bufferWritter.write(aa);
            bufferWritter.flush();
            bufferWritter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

}

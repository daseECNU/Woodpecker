package edu.ecnu.woodpecker.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.controller.TestController;

public class RecordAnalysis
{

    private final static String EQUAL_SIGN = "=";
    private final static String OPEN_SQUARE_BRACKETS = "\\[";
    private final static String CLOSE_SQUARE_BRACKETS = "\\]";
    private final static String OPEN_BRACE = "\\{";
    private final static String CLOSE_BRACE = "\\}";
    private final static String SIGN = "->";
    private final static String COLON = ":";
    // private final static String CASE_SIGN_LEFT = "---[";
    // private final static String CASE_SIGN_RIGHT = "]---";
    private static String logAdress = null;

    private static ArrayList<ArrayList<String>> content = new ArrayList<ArrayList<String>>();
    private static ArrayList<ArrayList<String>> stressContent = new ArrayList<ArrayList<String>>();
    private static String clusterName = null;
    private static String caseName = null;
    private static String lastcaseName = "";
    private static String state = "pass";
    private static String time = null;
    private static int execTime = 0;
    private static String avgQPS = null;
    private static String avgTPS = null;
    private static String avgResponseTime = null;
    private static String TP50ResponseTime = null;
    private static String TP90ResponseTime = null;
    private static String TP99ResponseTime = null;
    private static String StressInfo = null;
    static int i = 0;

    public static ArrayList<ArrayList<String>> readRecord()
    {
        logAdress = readLogAddress();
        File file = new File(logAdress);

        if (file.isFile() && file.exists())
        {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "GB2312")))
            {
                String lineTxt = null;
                while ((lineTxt = bufferedReader.readLine()) != null)
                {
                    if (lineTxt.matches("(\\s*#.*)|(\\s*)"))
                    {
                        continue;
                    }

                    String confItemValue[] = lineTxt.split(SIGN);

                    if (confItemValue.length > 1)
                    {

                        ArrayList<String> list = new ArrayList<String>();
                        String confItem[] = lineTxt.split(OPEN_SQUARE_BRACKETS);
                        String item[] = confItem[1].split(CLOSE_SQUARE_BRACKETS);
                        if (item[0].trim().equals(LogLevelConstant.INFO))
                        {
                            if (confItemValue[1].matches("\\w+\\s+[-]+\\w+\\s\\w+\\s\\w+[-]+"))
                            {
                                execTime = 0;
                                clusterName = null;
                                caseName = null;
                                lastcaseName = "";
                                state = "pass";
                                time = null;
                            }
                            String details[] = item[1].split(OPEN_BRACE);
                            if (details.length > 1)
                            {
                                // System.out.println(lineTxt);

                                String sign[] = details[1].split(CLOSE_BRACE);
                                if (sign[0].trim().startsWith(
                                        TestController.getTestEnvironmentConfigPath()))
                                // "./config/"))
                                {
                                    clusterName = sign[0];
                                }
                                else if (sign[0].trim().split(COLON)[0].equals("caseid"))
                                {
                                    time = confItem[0];
                                    caseName = sign[0].split(COLON)[1];
                                    if (!lastcaseName.equals(caseName))
                                    {
                                        execTime = 0;
                                    }
                                    lastcaseName = caseName;
                                    execTime++;
                                    state = "pass";
                                    if (execTime == 1)
                                    {
                                        list.add(clusterName);
                                        list.add(caseName);
                                        list.add(String.valueOf(execTime));
                                        list.add(state);
                                        list.add(String.valueOf(execTime));
                                        list.add(state);
                                        list.add(time);
                                    }
                                    else
                                    {
                                        list = content.get(content.size() - 1);
                                        list.set(2, String.valueOf(execTime));
                                        list.set(3, state);
                                        list.add(String.valueOf(execTime));
                                        list.add(state);
                                        list.add(time);
                                        content.remove(content.size() - 1);
                                    }
                                }
                            }
                        }
                        else if (item[0].trim().equals(LogLevelConstant.ERROR))
                        {
                            state = "error";
                            time = confItem[0];
                            // System.out.println(item[1]);
                            if (content.size() >= 1)
                            {

                                if ((content.get(content.size() - 1).get(1)).equals(caseName)
                                        && (content.get(content.size() - 1).get(content.get(
                                                content.size() - 1).size() - 2)).equals("pass")
                                        && state.equals("error")
                                        && Integer.valueOf(content.get(content.size() - 1).get(2)) == execTime)
                                {
                                    list = content.get(content.size() - 1);
                                    list.set(3, state);
                                    list.set(content.get(content.size() - 1).size() - 3,
                                            String.valueOf(execTime));
                                    list.set(content.get(content.size() - 1).size() - 2, state);
                                    list.set(content.get(content.size() - 1).size() - 1, time
                                            + " : " + item[1]);
                                    content.remove(content.size() - 1);
                                }
                                else if ((content.get(content.size() - 1).get(1)).equals(caseName)
                                        && (content.get(content.size() - 1).get(content.get(
                                                content.size() - 1).size() - 2)).equals("error")
                                        && Integer.valueOf(content.get(content.size() - 1).get(2)) == execTime)
                                {
                                    String a = content.get(content.size() - 1).get(
                                            content.get(content.size() - 1).size() - 1)
                                            + ";" + time + " : " + item[1];
                                    list = content.get(content.size() - 1);
                                    list.set(content.get(content.size() - 1).size() - 1, a);
                                    content.remove(content.size() - 1);
                                }

                                else if ((content.get(content.size() - 1).get(1)).equals(caseName)
                                        && (content.get(content.size() - 1).get(3)).equals("error")
                                        && Integer.valueOf(content.get(content.size() - 1).get(2)) < execTime)
                                {
                                    list = content.get(content.size() - 1);
                                    list.set(2, String.valueOf(execTime));
                                    list.set(3, state);
                                    list.add(String.valueOf(execTime));
                                    list.add(state);
                                    list.add(time + " : " + item[1]);
                                    content.remove(content.size() - 1);
                                }
                            }
                        }
                        if (list.size() != 0)
                        {
                            content.add(list);
                        }
                    }

                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        // if (content.size() > 0)
        // {
        // for (int i = 0; i < content.size(); i++)
        // {
        // for (int j = 0; j < content.get(i).size(); j++)
        // {
        // System.out.print(content.get(i).get(j) + " ");
        // }
        // System.out.println();
        // }
        // }
        return content;
    }

    public static ArrayList<ArrayList<String>> readStressRecord()
    {
        logAdress = readLogAddress();
        File file = new File(logAdress);

        if (file.isFile() && file.exists())
        {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "GB2312")))
            {
                String lineTxt = null;
                while ((lineTxt = bufferedReader.readLine()) != null)
                {
                    if (lineTxt.matches("(\\s*#.*)|(\\s*)"))
                    {
                        continue;
                    }

                    String confItemValue[] = lineTxt.split(SIGN);

                    if (confItemValue.length > 1)
                    {

                        String confItem[] = lineTxt.split(OPEN_SQUARE_BRACKETS);
                        String item[] = confItem[1].split(CLOSE_SQUARE_BRACKETS);
                        if (item[0].trim().equals("INFO"))
                        {
                            if (confItemValue[1].matches("\\w+\\s+[-]+\\w+\\s\\w+\\s\\w+[-]+"))
                            {
                                clusterName = null;
                                caseName = null;
                                time = null;
                            }
                            String details[] = item[1].split(OPEN_BRACE);
                            if (details.length > 1)
                            {
                                // System.out.println(lineTxt);

                                String sign[] = details[1].split(CLOSE_BRACE);
                                if (sign[0].trim().startsWith(// WorkflowController.getTestEnvironmentConfigPath()))
                                        "./config/"))
                                {
                                    clusterName = sign[0];
                                }
                                else if (sign[0].trim().split(COLON)[0].equals("caseid"))
                                {
                                    time = confItem[0];
                                    caseName = sign[0].split(COLON)[1];
                                    i = 0;
                                }
                            }

                        }
                    }
                    confItemValue = lineTxt.split(COLON);
                    if (confItemValue.length == 2)
                    {
                        if (confItemValue[0].trim().equals("StressInfo"))
                        {
                            StressInfo = confItemValue[1].trim();
                            i++;
                            // System.out.println("aaa" + lineTxt + "i=" + i);
                        }
                        else if (confItemValue[0].trim().equals("AvgTPS"))
                        {
                            avgTPS = confItemValue[1].trim();
                            i++;
                            // System.out.println("aaa" + lineTxt + "i=" + i);
                        }
                        else if (confItemValue[0].trim().equals("AvgQPS"))
                        {
                            avgQPS = confItemValue[1].trim();
                            i++;
                            // System.out.println("aaa" + lineTxt + "i=" + i);
                        }
                        else if (confItemValue[0].trim().equals("TP50ResponseTime"))
                        {
                            TP50ResponseTime = confItemValue[1].trim();
                            i++;
                            // System.out.println("aaa" + lineTxt + "i=" + i);
                        }
                        else if (confItemValue[0].trim().equals("TP90ResponseTime"))
                        {
                            TP90ResponseTime = confItemValue[1].trim();
                            i++;
                            // System.out.println("aaa" + lineTxt + "i=" + i);
                        }
                        else if (confItemValue[0].trim().equals("TP99ResponseTime"))
                        {
                            TP99ResponseTime = confItemValue[1].trim();
                            i++;
                            // System.out.println("aaa" + lineTxt + "i=" + i);
                        }
                        else if (confItemValue[0].trim().equals("AvgResponseTime"))
                        {
                            avgResponseTime = confItemValue[1].trim();
                            i++;
                            // System.out.println("aaa" + lineTxt + "i=" + i);
                        }
                    }
                    if (i == 6)
                    {
                        // System.out.println("-----------");
                        ArrayList<String> list = new ArrayList<String>();
                        list.add(clusterName);

                        list.add(caseName);
                        list.add(time);
                        list.add(StressInfo);
                        list.add(avgTPS);
                        list.add(avgQPS);
                        list.add(avgResponseTime);
                        list.add(TP50ResponseTime);
                        list.add(TP90ResponseTime);
                        list.add(TP99ResponseTime);
                        stressContent.add(list);
                        i = 0;
                        // for (int k = 0; k < list.size(); k++)
                        // {
                        // System.out.print(list.get(k) + " ");
                        // }
                        // System.out.println("-----------");
                    }
                }
                // if (stressContent.size() > 0)
                // {
                // for (int i = 0; i < content.size(); i++)
                // {
                // for (int j = 0; j < content.get(i).size(); j++)
                // {
                // System.out.print(content.get(i).get(j) + " ");
                // }
                // System.out.println();
                // }
                // }
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return stressContent;
    }

    public static void deleteLog()
    {
        logAdress = readLogAddress();
        File file = new File(logAdress);
        if (file.isFile() && file.exists())
        {
            if (file.isDirectory())
            {
                String[] children = file.list();
                for (int i = 0; i < children.length; i++)
                {
                    deleteDir(new File(file, children[i]));
                }
            }
            else
            {
                file.delete();
            }
        }

    }

    private static boolean deleteDir(File file)
    {
        return file.delete();
    }

    private static String readLogAddress()
    {
        File file = new File("./config/log4j.properties");

        if (file.isFile() && file.exists())
        {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "GB2312")))
            {
                String lineTxt = null;
                while ((lineTxt = bufferedReader.readLine()) != null)
                {
                    if (lineTxt.matches("(\\s*#.*)|(\\s*)"))
                    {
                        continue;
                    }
                    String confItemValue[] = lineTxt.split(EQUAL_SIGN);
                    String confItem = confItemValue[0].toLowerCase().trim();
                    String content = confItemValue[1].trim();
                    if (confItem.toLowerCase().equals("log4j.appender.a2.file"))
                    // .matches("\\w+a2.file\\w+"))
                    {
                        logAdress = content;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return logAdress;
            }
        }
        // System.out.println(logAdress);
        return logAdress;
    }

    public static void main(String[] args)
    {
        readRecord();
        // deleteLog();
        // readStressRecord();
    }
}

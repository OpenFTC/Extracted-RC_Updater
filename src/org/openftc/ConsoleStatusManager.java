package org.openftc;

public class ConsoleStatusManager
{
    private int lengthOfLastStepMsg = 0;
    private static final int STD_TERMINAL_COLUMNS = 80;

    void fail()
    {
        System.out.println(rightAlignResult("[FAIL]"));

        System.exit(1);
    }

    void fail(Exception e)
    {
        System.out.print((char)27 + "[31m"); //red
        System.out.println(rightAlignResult("[FAIL]"));
        System.out.print((char)27 + "[0m"); //end red

        System.out.println("Stacktrace:");

        e.printStackTrace();

        System.exit(1);
    }

    void ok()
    {
        System.out.print((char)27 + "[32m"); //green
        System.out.println(rightAlignResult("[OK]"));
        System.out.print((char)27 + "[0m"); //end green
    }

    void na()
    {
        System.out.print((char)27 + "[33m"); //yellow
        System.out.println(rightAlignResult("[N/A]"));
        System.out.print((char)27 + "[0m"); //end yellow
    }

    String rightAlignResult(String result)
    {
        StringBuilder builder = new StringBuilder();

        for(int i = lengthOfLastStepMsg; i < STD_TERMINAL_COLUMNS-result.length(); i++)
        {
            builder.append(" ");
        }

        builder.append(result);

        return builder.toString();
    }

    void stepMsg(String msg)
    {
        msg = "> " + msg + "...";
        System.out.print(msg);
        lengthOfLastStepMsg = msg.length();
    }
}

package org.openftc;

public class ConsoleStatusManager
{
    private int lengthOfLastStepMsg = 0;
    private static final int STD_TERMINAL_COLUMNS = 80;

    void fail()
    {
        String failMsg = "[FAIL]";

        System.out.println(rightAlignResult(failMsg));

        System.exit(1);
    }

    void fail(Exception e)
    {
        String failMsg = "[FAIL]";

        System.out.println(rightAlignResult(failMsg));

        System.out.println("Stacktrace:");

        e.printStackTrace();

        System.exit(1);
    }

    void ok()
    {
        String okMsg = "[OK]";

        System.out.println(rightAlignResult(okMsg));
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
        msg = "> " + msg + "... ";
        System.out.print(msg);
        lengthOfLastStepMsg = msg.length();
    }
}

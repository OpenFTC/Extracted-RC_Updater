/*
 * Copyright (c) 2019 OpenFTC Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

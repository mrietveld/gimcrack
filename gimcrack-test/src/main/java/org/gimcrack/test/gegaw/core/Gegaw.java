package org.gimcrack.test.gegaw.core;

public class Gegaw {

    public static String unsupported(Object... args) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        StringBuilder op = 
                new StringBuilder(ste.getClassName())
            .append(".")
            .append(ste.getMethodName());

        op.append("(");
        if (args.length > 0) {
            op.append(args[0].getClass().getSimpleName());
            for (int i = 1; i < args.length; ++i) {
                op.append(", ").append(args[i].getClass().getSimpleName());
            }
        }
        op.append(")");
        return op.toString();
    }
}

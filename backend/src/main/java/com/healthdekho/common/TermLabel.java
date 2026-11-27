package com.healthdekho.common;

/** Turns a policy term in months into a label such as "20 years", "1 year" or "18 months". */
public final class TermLabel {

    private TermLabel() {
    }

    public static String of(int termMonths) {
        if (termMonths % 12 == 0) {
            int years = termMonths / 12;
            return years == 1 ? "1 year" : years + " years";
        }
        return termMonths == 1 ? "1 month" : termMonths + " months";
    }
}

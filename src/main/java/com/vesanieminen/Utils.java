package com.vesanieminen;

public class Utils {

    public static int getInt(String string) {
        return Integer.parseInt(string.replace(" ", ""));
    }

    public static int getMonth(String romanNumeral) {
        return switch (romanNumeral) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            case "XI" -> 11;
            case "XII" -> 12;
            default -> 0;
        };
    }

}

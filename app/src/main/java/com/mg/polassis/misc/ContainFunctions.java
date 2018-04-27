/*
Polassis: personal voice assistant for Android devices
Copyright (C) 2018 Maksymilian Graczyk

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.mg.polassis.misc;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainFunctions {
    public static boolean contains(ArrayList<String> list, String value) {
        boolean found = false;
        value = value.toLowerCase();

        for (int i = 0; i < list.size() && !found; i++) {
            String element = list.get(i).toLowerCase();
            if (element.startsWith("__w__"))
                found = containsWord(value, element.replace("__w__", ""));
            else found = value.contains(element.replace("__p__", ""));
        }

        return found;
    }

    public static boolean containsDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.toString(text.charAt(i)).matches("\\d")) return true;
        }
        return false;
    }

    public static boolean containsDigitOnly(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.toString(text.charAt(i)).matches("\\d")) return false;
        }

        return true;
    }

    public static boolean containsMathematicalSymbol(String text) {
        return text.contains("+") || text.contains("-") || text.contains("/") || text.contains("*") || text.contains("^");
    }


    public static boolean containsURL(String text) {
        String regex = "^.*((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }


    public static boolean containsWord(String text, String wordToBeLookedFor) {
        String[] words = text.split(" ");
        for (String word : words) {
            if (word.equals(wordToBeLookedFor)) return true;
        }

        return false;
    }
}

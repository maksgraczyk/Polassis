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

import android.content.Context;

import java.util.ArrayList;

public class Numbers {
    public static String processText(Context context, String text)
    {
        String[] numbers = Translations.getStringArrayResource(context, "numbers");
        ArrayList<String> words = new ArrayList<>();
        String[] textWords = text.split(" ");

        for (int i = 0; i < textWords.length; i++)
        {
            int sizeBefore = words.size();
            for (int j = 0; j < numbers.length; j++)
            {
                String[] numberWords = numbers[j].split("\\|");
                for (int k = 0; k < numberWords.length; k++)
                {
                    if (textWords[i].equalsIgnoreCase(numberWords[k]))
                    {
                        words.add(Integer.toString(j));
                        k = numberWords.length;
                        j = numbers.length;
                    }
                }
            }

            if (words.size() == sizeBefore) words.add(textWords[i]);
        }

        String newText = "";

        for (int i = 0; i < words.size(); i++)
        {
            newText += words.get(i);
            if (i < words.size()-1) newText += " ";
        }

        return newText;
    }
}

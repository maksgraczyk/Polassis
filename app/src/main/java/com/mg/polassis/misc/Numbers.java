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

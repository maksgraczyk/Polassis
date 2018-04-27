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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.mg.polassis.R;

import org.apache.commons.io.IOUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by maksymilian on 27/07/17.
 */

public class Translations {
    private static String language = null;

    private static boolean isLanguageLoaded(Context context, String language)
    {
        return language != null && new File(context.getFilesDir(), "lang/" + language).exists();
    }

    public static boolean isLanguageSet(Context context)
    {
        SharedPreferences languagePreferences = PreferenceManager.getDefaultSharedPreferences(context);
        language = languagePreferences.getString("language", null);

        return language != null;
    }

    public static String[] getLanguageEntryValues(Context context)
    {
        ArrayList<String> entryValuesList = new ArrayList<>();
        entryValuesList.add("pl-PL");
        entryValuesList.add("en-GB");
        File file = new File(context.getFilesDir(), "lang/");
        for (File fileInside : file.listFiles())
        {
            String fileName = fileInside.getName();
            if (!fileName.equals("pl-PL") && !fileName.equals("en-GB")) entryValuesList.add(fileName);
        }

        String[] entryValues = new String[entryValuesList.size()];
        entryValuesList.toArray(entryValues);
        return entryValues;
    }

    public static String[] getLanguageEntries(Context context) throws XmlPullParserException, IOException
    {
        ArrayList<String> entriesList = new ArrayList<>();
        entriesList.add("Polski");
        entriesList.add("English (UK)");
        File file = new File(context.getFilesDir(), "lang/");
        for (File fileInside : file.listFiles())
        {
            if (!fileInside.getName().equals("pl-PL") && !fileInside.getName().equals("en-GB")) {
                File languageFile = new File(fileInside, "language.xml");
                XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
                xmlPullParserFactory.setNamespaceAware(true);
                XmlPullParser xmlParser = xmlPullParserFactory.newPullParser();

                xmlParser.setInput(new FileInputStream(languageFile), "UTF-8");

                int eventType = xmlParser.next();
                String languageName = "";

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("string") && xmlParser.getAttributeValue(null, "name").equals("language_name")) {
                        xmlParser.next();
                        languageName = xmlParser.getText();
                    }

                    eventType = xmlParser.next();
                }

                entriesList.add(languageName);
            }
        }

        String[] entries = new String[entriesList.size()];
        entriesList.toArray(entries);
        return entries;
    }

    public static String getLanguageSet(Context context)
    {
        SharedPreferences languagePreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return languagePreferences.getString("language", null);
    }

    private static void prepareLanguage(Context context, String language, @Nullable String url) throws IOException
    {
        String languageToUrl;
        File file = new File(context.getFilesDir(), "lang/" + language);
        if (url == null) 
        {
            languageToUrl = language;
            url = context.getString(R.string.language_page);
        }
        else languageToUrl = "";

        if (file.exists())
        {
            for (File fileInside : file.listFiles())
            {
                fileInside.delete();
            }

            file.delete();
        }

        if (file.mkdirs()) {
            OutputStream answersFileOutput = new FileOutputStream(new File(context.getFilesDir(), "lang/" + language + "/answers.xml"));
            InputStream answersFile = NetworkHandler.querySync(url + languageToUrl + "/answers.xml", "GET", null, null, null);
            IOUtils.copy(answersFile, answersFileOutput);

            answersFileOutput.close();
            answersFile.close();

            OutputStream languageFileOutput = new FileOutputStream(new File(context.getFilesDir(), "lang/" + language + "/language.xml"));
            InputStream languageFile = NetworkHandler.querySync(url + languageToUrl + "/language.xml", "GET", null, null, null);
            IOUtils.copy(languageFile, languageFileOutput);

            languageFileOutput.close();
            languageFile.close();

            OutputStream stringsFileOutput = new FileOutputStream(new File(context.getFilesDir(), "lang/" + language + "/strings.xml"));
            InputStream stringsFile = NetworkHandler.querySync(url + languageToUrl + "/strings.xml", "GET", null, null, null);
            IOUtils.copy(stringsFile, stringsFileOutput);

            stringsFileOutput.close();
            stringsFile.close();

            OutputStream syntaxFileOutput = new FileOutputStream(new File(context.getFilesDir(), "lang/" + language + "/syntax.xml"));
            InputStream syntaxFile = NetworkHandler.querySync(url + languageToUrl + "/syntax.xml", "GET", null, null, null);
            IOUtils.copy(syntaxFile, syntaxFileOutput);

            syntaxFileOutput.close();
            syntaxFile.close();
        }
        else throw new IOException("Could not create directories");
    }

    public static void setLanguage(Context context, String language, @Nullable String url, boolean forcePrepare) throws IOException
    {
        if (forcePrepare || !isLanguageLoaded(context, language)) prepareLanguage(context, language, url);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("language", language).commit();
        Translations.language = language;
    }

    private static String getStringResourceFromXml(Context context, XmlPullParser parser, String id) throws XmlPullParserException, IOException
    {
        int eventType = parser.next();

        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            if (eventType == XmlPullParser.START_TAG)
            {
                if (parser.getName().equals("string") && parser.getAttributeValue(null, "name").equals(id))
                {
                    parser.next();
                    return parser.getText().replace("\\n", "\n");
                }
            }

            eventType = parser.next();
        }

        return id;
    }

    public static String getStringResource(Context context, String id)
    {
        if (language == null) language = PreferenceManager.getDefaultSharedPreferences(context).getString("language", null);

        if (id.equals("%s")) return "%s";
        else if (id.endsWith("%") && !id.endsWith("%%")) return id.replace("%", "%%");
        else
        {
            try {
                FileInputStream file = new FileInputStream(new File(context.getFilesDir(), "lang/" + language + "/strings.xml"));
                XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
                xmlFactory.setNamespaceAware(true);
                XmlPullParser xmlParser = xmlFactory.newPullParser();
                xmlParser.setInput(file, "UTF-8");

                String resource = getStringResourceFromXml(context, xmlParser, id);
                if (resource.equals(id))
                {
                    file = new FileInputStream(new File(context.getFilesDir(), "lang/" + language + "/language.xml"));
                    xmlFactory = XmlPullParserFactory.newInstance();
                    xmlFactory.setNamespaceAware(true);
                    xmlParser = xmlFactory.newPullParser();
                    xmlParser.setInput(file, "UTF-8");

                    return getStringResourceFromXml(context, xmlParser, id);
                }
                else return resource;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Toast.makeText(context, "An error occurred when getting a text in a specified language.", Toast.LENGTH_LONG).show();
                return id;
            }
        }
    }

    private static String[] getStringArrayResourceFromXml(Context context, XmlPullParser parser, String id)
    {
        try {
            int eventType = parser.next();
            boolean found = false;

            ArrayList<String> strings = new ArrayList<>();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("string-array") && parser.getAttributeValue(null, "name").equals(id))
                        found = true;
                    else if (found && parser.getName().equals("item")) {
                        parser.next();
                        strings.add(parser.getText());
                    }

                    eventType = parser.next();
                } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("string-array") && found)
                    eventType = XmlPullParser.END_DOCUMENT;
                else eventType = parser.next();
            }

            String[] toBeReturned = new String[strings.size()];
            strings.toArray(toBeReturned);

            return toBeReturned;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(context, "An error occurred when getting a text array in a specified language.", Toast.LENGTH_LONG).show();
            return new String[] {id};
        }
    }

    public static String[] getStringArrayResource(Context context, String id)
    {
        if (language == null) language = PreferenceManager.getDefaultSharedPreferences(context).getString("language", null);

        try
        {
            FileInputStream file = new FileInputStream(new File(context.getFilesDir(), "lang/" + language + "/strings.xml"));
            XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            XmlPullParser xmlParser = xmlFactory.newPullParser();

            xmlParser.setInput(file, "UTF-8");

            String[] toBeReturned = getStringArrayResourceFromXml(context, xmlParser, id);
            if (toBeReturned[0].equals(id))
            {
                file = context.openFileInput("lang/" + language + "/language.xml");
                xmlFactory = XmlPullParserFactory.newInstance();
                xmlFactory.setNamespaceAware(true);
                xmlParser = xmlFactory.newPullParser();

                xmlParser.setInput(file, "UTF-8");
                return getStringArrayResourceFromXml(context, xmlParser, id);
            }
            else return toBeReturned;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(context, "An error occurred when getting a text array in a specified language.", Toast.LENGTH_LONG).show();
            return new String[] {id};
        }
    }

    private static String getAnswerFromCodes(Context context, String[] answerCodes, int attempt)
    {
        Random random = new Random();

        int index = random.nextInt(answerCodes.length);

        String answerCode = answerCodes[index];
        String[] codeParts = answerCode.split("\\_\\_");

        String answer = "";

        for (String codePart : codeParts)
        {
            if (codePart.startsWith("{"))
            {
                codePart = codePart.substring(1, codePart.length()-1);
                String[] words = codePart.split("\\|");
                random = new Random();
                index = random.nextInt(words.length);

                if (words[index].matches("\\P{L}") && answer.length() >= 1) answer = answer.substring(0, answer.length()-1);

                answer += words[index] + " ";
            }
            else if (codePart.startsWith("["))
            {
                codePart = codePart.substring(1, codePart.length()-1);
                random = new Random();
                boolean toBeIncluded = random.nextBoolean();

                if (toBeIncluded)
                {
                    String[] words = codePart.split("\\|");
                    random = new Random();
                    index = random.nextInt(words.length);

                    if (words[index].matches("\\P{L}") && answer.length() >= 1) answer = answer.substring(0, answer.length()-1);

                    answer += words[index] + " ";
                }
            }
            else
            {
                if (codePart.matches("\\P{L}") && answer.length() >= 1) answer = answer.substring(0, answer.length()-1);
                answer += codePart + " ";
            }
        }

        SharedPreferences previousAnswerPreferences = context.getSharedPreferences("previous_answer", Context.MODE_PRIVATE);
        String previousAnswer = previousAnswerPreferences.getString("previous_answer", null);

        if (previousAnswer == null)
        {
            previousAnswerPreferences.edit().putString("previous_answer", answer).apply();
            return answer;
        }
        else
        {
            String[] previousAnswerWords = previousAnswer.split(" ");
            String[] answerWords = answer.split(" ");

            int commonWords = 0;

            for (String previousAnswerWord : previousAnswerWords)
            {
                for (String answerWord : answerWords)
                {
                    if (previousAnswerWord.equals(answerWord)) commonWords += 1;
                }
            }

            double similarity = (double)commonWords/(double)answerWords.length;

            if ((attempt <= 25 && similarity > 0.5) || (attempt <= 50 && similarity > 0.66) || (attempt <= 75 && similarity > 0.85) || (attempt <= 100 && similarity > 0.95))
            {
                return getAnswerFromCodes(context, answerCodes, attempt+1);
            }
            else
            {
                previousAnswerPreferences.edit().putString("previous_answer", answer).apply();
                return answer;
            }
        }
    }

    public static String getAnswer(Context context, String id)
    {
        if (language == null) language = PreferenceManager.getDefaultSharedPreferences(context).getString("language", null);

        try
        {
            FileInputStream stringsFile = new FileInputStream(new File(context.getFilesDir(), "lang/" + language + "/answers.xml"));
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();

            parser.setInput(stringsFile, "UTF-8");

            String[] answerCodes = getStringArrayResourceFromXml(context, parser, id);
            return getAnswerFromCodes(context, answerCodes, 1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(context, "An error occurred when getting a text in a specified language.", Toast.LENGTH_LONG).show();
            return id;
        }
    }
}

package com.mg.polassis.misc;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class News {
    private String newsId;
    private long newsDate;
    private String newsContent;
    private String newsAndroidVersions;
    private String newsPolassisVersions;
    private boolean isNewsImportant;
    private Context context;

    public News(Context context, String newsId, long newsDate, String newsContent, String newsAndroidVersions, String newsPolassisVersions, boolean isNewsImportant) {
        this.context = context;
        this.newsId = newsId;
        this.newsDate = newsDate;
        this.newsContent = newsContent;
        this.newsAndroidVersions = newsAndroidVersions;
        this.newsPolassisVersions = newsPolassisVersions;
        this.isNewsImportant = isNewsImportant;
    }

    public String getId() {
        return newsId;
    }

    public String getNewsDate() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(newsDate);
        String date = Translations.getStringResource(context, "date");
        switch (calendar.get(GregorianCalendar.MONTH)) {
            case 0:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "january"));
                break;

            case 1:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "february"));
                break;

            case 2:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "march"));
                break;

            case 3:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "april"));
                break;

            case 4:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "may"));
                break;

            case 5:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "june"));
                break;

            case 6:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "july"));
                break;

            case 7:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "august"));
                break;

            case 8:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "september"));
                break;

            case 9:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "october"));
                break;

            case 10:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "november"));
                break;

            case 11:

                date = date.replace("%%MONTH%%", Translations.getStringResource(context, "december"));
                break;
        }

        date = date.replace("%%DAY%%", Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));

        date = date.replace("%%YEAR%%", Integer.toString(calendar.get(Calendar.YEAR)));
        date += ", " + DateFormat.format("HH:mm", calendar);
        return date;
    }

    public String getNewsContent() {
        return newsContent;
    }

    public boolean isValidForAndroidVersion(int api) {
        if (newsAndroidVersions.equals("*")) return true;
        else if (newsAndroidVersions.endsWith("+")) {
            int lower = Integer.parseInt(newsAndroidVersions.replace("+", ""));
            if (api >= lower) return true;
            else return false;
        }
        else if (newsAndroidVersions.endsWith("-")) {
            int upper = Integer.parseInt(newsAndroidVersions.replace("-", ""));
            if (api <= upper) return true;
            else return false;
        }
        else if (newsAndroidVersions.contains("-")) {
            int lower = Integer.parseInt(newsAndroidVersions.split("\\-")[0]);
            int upper = Integer.parseInt(newsAndroidVersions.split("\\-")[1]);

            if (api >= lower && api <= upper) return true;
            else return false;
        }
        else {
            if (api == Integer.parseInt(newsAndroidVersions)) return true;
            else return false;
        }
    }

    public boolean isValidForAssistantVersion(Context context) {
        try {
            if (newsPolassisVersions.equals("*")) return true;
            else {
                int code = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
                if (newsPolassisVersions.endsWith("+")) {
                    int lower = Integer.parseInt(newsPolassisVersions.replace("+", ""));
                    if (code >= lower) return true;
                    else return false;
                }
                else if (newsPolassisVersions.endsWith("-")) {
                    int upper = Integer.parseInt(newsPolassisVersions.replace("-", ""));
                    if (code <= upper) return true;
                    else return false;
                }
                else if (newsPolassisVersions.contains("-")) {
                    int lower = Integer.parseInt(newsPolassisVersions.split("\\-")[0]);
                    int upper = Integer.parseInt(newsPolassisVersions.split("\\-")[1]);

                    if (code >= lower && code <= upper) return true;
                    else return false;
                }
                else {
                    if (code == Integer.parseInt(newsPolassisVersions)) return true;
                    else return false;
                }
            }
        }
        catch (Exception e) {
            return true;
        }
    }

    public boolean isNewsImportant() {
        return isNewsImportant;
    }
}

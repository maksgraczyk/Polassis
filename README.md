# Polassis
[Author's statement in Polish (29 April 2018) / Oświadczenie autora (29 kwietnia 2018)](https://github.com/maksgraczyk/Polassis/blob/master/STATEMENT.pl-PL.md)

[Read in Polish / Przeczytaj po polsku](https://github.com/maksgraczyk/Polassis/blob/master/README.pl-PL.md)

A personal voice assistant for Android devices. Polish is the only supported language at the moment (in both GUI and command recognition).

## License
(C) 2018 Maksymilian Graczyk. Polassis is licensed under the GNU General Public License v3.0. Please see LICENSE for details.

Every copyrighted work included in the program is used in compliance with appropriate licenses. These works belong to their authors. Please see "Third party resources used" for details.

### Contributing to the code
If you want to contribute to the code, feel free to do so: just fork this project to your GitHub account and make appropriate changes afterwards.

**Unfortunately, direct contributions to the original code (except for contributions made by myself) are not possible due to legal reasons (I would like to avoid any copyright issues).**

## Support
This project is currently frozen. Therefore, I provide very limited support. I will not answer e-mails with comments about either stable or development versions (except for urgent issues from people with disabilities and copyright/acknowledgement issues: see "Third party resources used"). I will try to answer any other e-mails within maximum 10 days. If you find any bugs or have any suggestions, feel free to report them in "Issues", but I cannot guarantee that I will respond to the reports within reasonable time.

If you want to get in touch with me directly (remember the above), my address is max.graczyk+polassis@gmail.com. Any previous e-mail addresses you might have encountered before (in the outlook.com and polassis.pl domains) are **no longer valid**.

## Installation instructions
The source code is the most up-to-date **development** version and does not have any ads and donation possibilities. Please note that bugs, empty screens and unfinished functions are *normal* here. However, as I aimed for fixing major bugs present in the latest stable version (1.0.7) before publishing the source code, the development version **may** work better than the stable one in your device. If this is the case, you are lucky!

The development version runs on Android 4.1+. There are also some stable and old beta versions available for download as APK files (see: Pre-bulit releases). Android versions supported by them are stated in their description.

The permissions required are discussed in Polish at https://pastebin.com/cv7r22Za (the English translation will be provided soon).

### With Android Studio
If you are familiar with Android programming and want to work with the source code, this is the quickest and recommended method. If not, see "Pre-built releases". The instructions below apply to Android Studio 3.0.1, but they should work flawlessly in other versions as well (minor changes might be required, please refer to the manual or online resources for your version of the IDE if necessary).

1. Clone the repository:
`git clone https://github.com/maksgraczyk/Polassis`
2. Open the folder "Polassis" as a project in Android Studio.
3. Wait until all required resources are prepared.
4. You are ready to go: have fun!

### Without Android Studio
(under construction)

### Pre-built releases
**Note: All pre-built releases I have access to will have been published by 6 May 2018.**

There are also pre-built releases which can be installed directly on Android devices without compiling the source code. They are available as APK files in "Releases". The development version is included. Your device must allow installing applications from unknown sources: you can change this behaviour in Android settings.

## How to use
The GUI is designed to be as easy-to-use and accessible as possible. To begin, tap a green microphone button at the bottom of the screen: when it turns red, say a command. A system speech recognition service (usually Google) processes your voice and returns the text which in turn is interpreted by the application. Alternatively, enter a command manually (using a big text box next to the microphone button) and press ENTER or tap the microphone button.

If you want to access settings, tap the gearwheel icon in the bottom-left corner and select "Settings" (in Polish: "Ustawienia").

As stated in the introduction, only Polish is supported at the moment.

### Functions
This is the list of activities the development version of Polassis can/could do (all examples in Polish):
* Saying a specified phrase (e.g. "powiedz ja mam kota")
* Reading the last received SMS (e.g. "przeczytaj ostatniego SMS-a")
* Determining the last incoming call (e.g. "kto do mnie ostatnio dzwonił")
* Determining the last outcoming call (e.g. "do kogo dzwoniłem")
* Determining the last call overall (e.g. "ostatnie połączenie")
* Writing an e-mail message (e.g. "wyślij e-maila")
* Notes:
  * Adding (e.g. "stwórz notatkę")
  * Editing (e.g. 'zmień notatkę "Test"')
  * Deleting (e.g. 'usuń notatkę "Test"')
  * Reading (e.g. 'czytaj notatkę "Lista zakupów"')
  * Showing the list of notes (e.g. "pokaż wszystkie notatki")
* Writing an SMS (e.g. "napisz SMS-a do Jarka")
* Taking a photo (e.g. "wykonaj zdjęcie")
* Obtaining a weather forecast (e.g. "jaka pogoda za tydzień Berlin") (**a Dark Sky API key is required: it is not provided in the code, see app/src/main/res/values/constants.xml**)
* Timer:
  * Setting (e.g. odmierz 3 godziny 30 minut")
  * Pausing/Resuming (e.g. pausing: "zatrzymaj minutnik", resuming: "wznów minutnik")
  * Checking the time left (e.g. "ile jeszcze mam czasu")
* Calling (e.g. "chcę zadzwonić pod numer 123456")
* Doing mathematical calculations (e.g. "siedem do kwadratu przemnożone przez cztery podzielić na sześć")
* Determining the day of week on a specified date (e.g. "który to był 3 stycznia 2015 roku")
* Controlling a music player:
  * Pausing/Resuming (e.g. pausing: "pauza", resuming: "wznów odtwarzanie")
  * Repeating (e.g. "odtwórz jeszcze raz")
  * Next track (e.g. "kolejna piosenka")
  * Previous track (e.g. "wróć do poprzedniej muzyki")
* Playing music on Spotify (e.g. "odtwórz Too Many Broken Hearts") (**not implemented**)
* Turning on/off a torch (e.g. turning on: "włącz latarkę", turning off: "nie chcę latarki")
* Triggering Wi-Fi/Bluetooth (e.g. Wi-Fi on: "włącz Wi-Fi", Bluetooth on: "włącz Bluetooth", Wi-Fi off: "możesz wyłączyć Wi-Fi", Bluetooth off: "wyłącz Bluetooth")
* Turning on the silent mode (e.g. "wycisz telefon")
* Turning off the silent mode (e.g. "wyłącz tryb cichy")
* Opening a webpage (e.g. "otwórz google.com")
* Starting an application (e.g. "uruchom Facebook")
* Setting a sat-nav (e.g. "jak dojechać do Warszawy, al. Ujazdowskie 1")
* Determining your location (e.g. "gdzie jestem")
* Determining the location of a particular place on a map (e.g. "gdzie znajduje się Madryt")
* Obtaining information from Wikipedia (e.g. "kto to jest Justyna Kowalczyk")
* Searching on the Internet (e.g. "szukaj inkwizycja")
* Determining the current battery level (e.g. "ile mam baterii")
* Alarms:
  * Setting (e.g. "ustaw budzik na 7:30")
  * Deleting (e.g. "nie chcę alarmu o 22:33")
* Setting the default number for a particular contact that Polassis should always choose (e.g. "ustaw domyślny numer dla Jarka")
* Saying the current time (e.g. "która godzina")
* Saying the current day of week (e.g. "jaki mamy dzień tygodnia")
* Saying the current date (e.g. "którego mamy dzisiaj")
* Setting reminders (e.g. "przypomnij mi")
* Rebooting the device (e.g. "zrestartuj telefon") (**root required**)
* Turning off the device (e.g. "wyłącz telefon") (**root required**)
* Turning off the assistant itself (e.g. "zamknij się")
* Dictating to the clipboard (e.g. "podyktuj do schowka")
* Presenting the possibilities of Polassis (**not implemented**)

Nearly all stable and old beta releases have also a possibility to "redirect" some functions to a PC/Mac (e.g. if you want to check something on Wikipedia, Polassis can tell your computer to open the website). If you want to use that, you must trigger the appropriate option in the Polassis settings and set up the server in your computer. The server is called Polassis Server: PolassisServer.jar (its source code will be published under GNU GPL v3 by the end of July 2018 if I still have access to it).

### Custom commands
**The development version**: You can add/edit/remove custom commands in the Polassis settings.

**All stable and old beta releases**: You can add custom commands by saying the specific phrase, more details are available in the "Assistant possibilities" ("Możliwości asystenta" in Polish) screen in the application.

### Intents
Polassis has a few intents which can be called externally, e.g. by Tasker or Llama:
* Starting the assistant:
  * Type: start activity
  * Package name: com.mg.polassis
  * Class name: com.mg.polassis.misc.Assistant
  * Extras: none
* Activating the assistant (starting the assistant and a speech recognition service):
  * Type: start service
  * Package name: com.mg.polassis
  * Class name: com.mg.polassis.service.BackgroundSpeechRecognitionService
  * Extras: none
* Activating the assistant in the classic mode (via the activity):
  * Type: start activity
  * Package name: com.mg.polassis
  * Class name: com.mg.polassis.misc.Assistant
  * Extras:
    * Boolean: "activation" = true
* Starting the assistant and telling it a specific command:
  * Type: start activity
  * Package name: com.mg.polassis
  * Class name: com.mg.polassis.misc.Assistant
  * Extras:
    * String: "command" = any command, e.g. "włącz YouTube" (in Polish)
* Starting the assistant and telling it a specific command requiring the separate confirmation:
  * Type: start activity
  * Package name: com.mg.polassis
  * Class name: com.mg.polassis.misc.Assistant
  * Extras:
    * String: "command" = any command, e.g. "włącz YouTube" (in Polish)
    * Boolean: "ask_for_confirmation" = true

### Troubleshooting
* If the microphone button turns grey and is stuck in this state:
  * Check whether speech recognition is installed and set up properly in your device. You might have to grant the speech recognition service an audio recording permission if you use Android 6.0+.
  * Experimenting with the Polassis settings is worthwhile (e.g. triggering the option "Wymusz. silnika Google" which is responsible for forcing using the Google speech-to-text engine)
  * If you use text-to-speech, check whether a TTS service is installed and set up properly in your device.
  * If you use Android 6.0+, check what permissions Polassis has been granted in your device.

## Language
(under construction)

## Third party resources used
If you see that your work is used in any format in Polassis without the credit below, please let me know: I will add the acknowledgement or modify the code so that it does not use your work illegally. Thank you!

### Libraries
* Apache Commons IO 2.6: under [the Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0)
* PocketSphinx: under [its license](https://github.com/maksgraczyk/Polassis/blob/master/pocketsphinx-android-5prealpha-nolib/LICENSE)
* exp4j 0.4.4: under [the Apache 2.0 license](https://www.objecthunter.net/exp4j/license.html)

### Icons
* [Icon made by Elegant Themes](http://www.flaticon.com/free-icon/mic_10032): under [the CC BY 3.0 license](https://creativecommons.org/licenses/by/3.0/)
* Icons made by Google: under [the CC BY 4.0 license](https://creativecommons.org/licenses/by/4.0/)
* Icons made by jxnblk: under [the MIT license](https://opensource.org/licenses/MIT)

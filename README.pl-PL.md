# Polassis
[Przeczytaj po angielsku / Read in English](https://github.com/maksgraczyk/Polassis/blob/master/README.md)

Osobisty asystent głosowy dla urządzeń z Androidem. Język polski jest obecnie jedynym wspieranym językiem (zarówno w interfejsie użytkownika, jak i w rozpoznawaniu poleceń).

## Wsparcie
Ten projekt jest w tym momencie zamrożony, w związku z czym zapewniam bardzo ograniczone wsparcie. Nie odpowiem na wiadomości e-mail z komentarzami zarówno o stabilnych, jak i rozwojowych wersjach (z wyjątkiem kwestii praw autorskich / uznania czyjejś pracy: zobacz "Użyta twórczość osób trzecich"). Jeżeli znajdziesz jakiekolwiek błędy lub masz jakiekolwiek sugestie, możesz je zaraportować w części "Issues", ale nie mogę zagwarantować odpowiedzi na raporty w rozsądnym czasie.

## Instrukcja instalacji
Udostępniony kod źródłowy jest najnowszą wersją **rozwojową**, która nie ma żadnych reklam ani możliwości dotacji. Proszę zwrócić uwage na to, że błędy, puste ekrany i niedokończone funckej są tutaj *normalne*. Jednakże, z racji tego, że celowałem w naprawienie głównych błędów występujących w najnowszej wersji stabilnej (1.0.7) przed opublikowaniem kodu źródłowego, wersja rozwojowa **może** działać lepiej na twoim urządzeniu niż wersja stabilna. Jeśli tak będzie u ciebie, to gratuluję szczęścia!

Wersja rozwojowa działa na Androidzie 4.1+. Niektóre stare wersje beta i wersje stabilne są również dostępne do pobrania jako pliki APK (zobacz: Prekompilowane wydania). Wersje Androida wspierane przez te wydania są wspomniane w ich opisie.

Wymagane uprawnienia są omówione pod adresem https://pastebin.com/cv7r22Za.

### Z wykorzystaniem Android Studio
Jeżeli programowanie na Androida jest tobie znajome i chcesz pracować z kodem źródłowym, to jest najszybsza i zalecana metoda. W przeciwnym razie zobacz "Prekompilowane wydania". Poniższe instrukcje mają zastosowanie do Android Studio 3.0.1, ale powinny też bez problemu działać w innych wersjach (konieczne mogą być małe zmiany, proszę odnieść się do instrukcji lub zasobów online dla twojej wersji IDE w razie potrzeby).

1. Sklonuj to repozytorium:
`git clone https://github.com/maksgraczyk/Polassis`
2. Otwórz w Android Studio folder "Polassis" jako projekt.
3. Poczekaj aż wszystkie wymagane zasoby zostaną przygotowane.
4. Możesz już pracować z kodem: miłej zabawy!

### Bez wykorzystania Android Studio
(w budowie)

### Prekompilowane wydania
Dostępne są również prekompilowane wydania, które mogą zostać bezpośrednio zainstalowane na urządzeniach z Androidem bez kompilowania kodu źródłowego. Można je pobrać jako pliki APK w części "Releases". Wersja rozwojowa jest tu uwzględniona. Twoje urządzenie musi zezwalać na instalację aplikacji z nieznanych źródeł: możesz zmienić to zachowanie w ustawieniach Androida.

## Jak korzystać
Interfejs użytkownika jest zaprojektowany tak, aby był jak najprostszy do użytku i jak najprzystępniejszy. Aby zacząć, dotknij zielony przycisk mikrofonu na dole ekranu: kiedy zmieni on kolor na czerwony, powiedz polecenie. Systemowa usługa rozpoznawania mowy (z reguły Google) przetworzy twój głos i zwróci tekst, który zostanie zinterpretowany przez aplikację. Alternatywnie wprowadź polecenie ręcznie (używając dużego pola tekstowego obok przycisku mikrofonu) i naciśnij ENTER lub dotknij przycisk mikrofonu.

Jeśli chcesz uzyskać dostęp do ustawień, dotknij ikonę koła zębatego w lewym dolnym rogu i wybierz "Ustawienia".

Jak wspomniałem we wstępie, jedynie język polski jest obsługiwany.

### Funkcje
Oto lista czynności, jakie może/mogłaby wykonać wersja rozwojowa Polassisa:
* Powiedzenie określonego tekstu (np. "powiedz ja mam kota")
* Przeczytanie ostatnio odebranego SMS-a (np. "przeczytaj ostatniego SMS-a")

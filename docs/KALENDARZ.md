# Codzienna obsługa kalendarza

Po zalogowaniu otwiera się kalendarz. Na komputerze miesiąc ma 7 kolumn. Wpis pokazuje godzinę, usługę i klienta; po trzech wpisach przycisk **+N kolejnych** otwiera pełny plan dnia. Zlecenia są ułożone według godziny, również te wykonane lub anulowane (z opisem statusu).

Na telefonie i tablecie (do 900 px) domyślnie widać pasek tygodnia i karty zleceń wybranego dnia. **Wybierz datę** rozwija przegląd miesiąca. Strzałki tygodnia przechodzą również do sąsiedniego miesiąca. Przycisk **+ Zlecenie** pozostaje w nagłówku podczas przewijania. Wybrany dzień zostaje w adresie strony, również po zapisie i odświeżeniu.

## Zlecenie bez kreatora

1. Wybierz dzień i **+ Zlecenie**.
2. Wybierz kategorię, ustaw ilości usług przyciskami −/+ lub wpisz liczbę. Ceny i suma są widoczne od razu.
3. Wpisz imię i telefon albo wyszukaj klienta po imieniu/telefonie i wybierz z listy. Dane i adres zostaną uzupełnione. Kontakt oraz adres możesz poprawić dla tej wizyty bez zmiany kartoteki i dawnych zleceń.
4. Ustaw godzinę i zapisz. Data jest już wybrana. Zapis zamyka formularz i pokazuje zlecenie w kalendarzu.

Błąd zapisu pozostawia formularz i wpisane dane. Podczas wysyłania ponowny zapis jest blokowany; token rezerwacji zabezpiecza również przed ponowieniem tego samego żądania. Zmiana cennika nie przelicza wcześniejszych zleceń.

Kliknij zlecenie, aby zobaczyć kontakt, klikalny numer telefonu, adres, usługi i kwotę. W tym panelu edytujesz termin i status. Edycję kartoteki oraz cennika otwierasz z głównej nawigacji.

## Kolory i obszary

Na komputerze kliknij numer dnia, a w panelu rozwiń **Przypisz obszary pracy**. Na telefonie wybierz **Obszary dnia**. Zaznacz kilka obszarów i zapisz; wyczyszczenie wyboru usuwa oznaczenia tego dnia.

Dzień otrzymuje delikatne tło pierwszego obszaru, osobne paski kolorów oraz etykiety wszystkich nazw. Pasek tygodnia również pokazuje kolory. Obszary są ręcznym oznaczeniem, nie dobierają terminów.

W **Obszary pracy** dodajesz i edytujesz nazwę oraz HEX (`#RRGGBB`). Dezaktywacja wyłącza nowe przypisania i zachowuje historię. Zamknięcie panelu lub Escape bez zapisu nie zmienia danych.

## Dane i wymagania

Interakcje paneli wymagają JavaScript. Dane są przechowywane w istniejącym PostgreSQL, bez localStorage i dodatkowego prototypu. Zmiana kalendarza nie wymaga nowej migracji; V1–V3 pozostają niezmienione. Restart aplikacji lub kontenera z zachowanym wolumenem nie usuwa danych.

Odczyt i zapis wymagają konta OWNER. Wszystkie zapisy wymagają CSRF. Aplikacja nie korzysta z produkcyjnej bazy nclean.pl. Uruchomienie na Windows: [WINDOWS.md](WINDOWS.md).

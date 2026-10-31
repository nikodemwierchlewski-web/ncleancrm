# Obsługa kalendarza

Po zalogowaniu otwiera się kalendarz miesiąca. Na komputerze dni są ułożone w 7 kolumnach; na telefonie są czytelną listą. Każdy przypisany obszar ma etykietę z nazwą i kolorem. Nie trzeba otwierać dnia, żeby zobaczyć jego obszary.

## Przypisanie obszarów do dnia

1. Kliknij dzień lub wybierz go klawiaturą.
2. W panelu zaznacz dowolne potrzebne obszary pracy.
3. Kliknij **Zapisz**. Zmiana trafia do PostgreSQL, a etykiety pojawiają się przy dniu.

Ponowne otwarcie dnia pokazuje zapisane zaznaczenia. Zmień je i zapisz, aby zastąpić przypisanie. Odznaczenie wszystkich obszarów i zapis pozostawia dzień bez oznaczenia. Zamknięcie panelu bez zapisu nie zmienia danych.

Przyciski nawigacji oraz wybór miesiąca pozwalają zmienić oglądany okres. Bieżący dzień jest wyróżniony. Panel można zamknąć przyciskiem zamknięcia lub klawiszem Escape. Bez JavaScript kliknięcie dnia otwiera formularz po przejściu na stronę tego dnia; zapis nadal działa.

## Ustawienia → Obszary pracy

- **Dodawanie:** wpisz nazwę i kolor w formacie `#RRGGBB`, np. `#208BE3`, i zapisz.
- **Edycja:** wybierz obszar, zmień nazwę lub kolor i zapisz. Etykiety już przypisanych dni pokazują aktualną nazwę i kolor.
- **Dezaktywacja:** wyłącza obszar z nowych przypisań i pozostawia jego wcześniejsze przypisania. Nieaktywny obszar jest opisany przy zapisanym dniu; można go zachować albo odznaczyć.

Nazwy nie mogą być puste ani dłuższe niż 100 znaków. Nazwa istniejącego obszaru nie może być użyta ponownie, również ze zmienioną wielkością liter. Niepoprawny kolor HEX albo nieznany identyfikator obszaru nie zostaną zapisane.

## Zapis i historia

Dane pozostają po restarcie aplikacji i kontenera PostgreSQL, gdy zachowasz jego wolumen. Aktualizacja z wersji V1 uruchamia migrację V2, która zachowuje dotychczasowe przypisania i pozwala dodać kolejne obszary do tych samych dni. Wykonana migracja V1 nie jest modyfikowana.

Edycja i zapis wymagają zalogowania właściciela. Formularze są chronione przez CSRF. Przypisanie obszarów jest ręcznym oznaczeniem organizacyjnym; nie tworzy zleceń ani nie dobiera terminów.

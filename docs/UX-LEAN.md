# Codzienna obsługa — dopracowanie interfejsu

Gałąź bazowa: `feat/customer-context-photos`. Zawiera najnowsze, jeszcze niescalone funkcje. Nie zastępuje ich prototypem. Bez migracji i zmian historycznych cen, pomiarów lub płatności.

## Audyt i zmiany

- Nagłówek na telefonie łamał się do drugiego wiersza. Teraz jest jednorzędowy, przyciski wyszukiwania i wylogowania są widoczne. Wspólny układ obejmuje również tablet.
- Wejście przez `/` prowadzi do kalendarza. Dotychczasowy `/today` pozostaje dostępny.
- Miesiąc na komputerze ma siedem kolumn i panel dnia. Wpis zawiera godzinę, skrót usługi i klienta; nadmiar prowadzi do pełnej listy. Na telefonie pozostaje tydzień z listą, zamiast pomniejszonej tabeli.
- Zlecenie ma czytelny termin bez sekund, adres, telefon i notatkę dotyczącą wizyty. Skróty „Wykonanie”, „Zdjęcia” i „Płatność” prowadzą od razu do odpowiedniej sekcji; ponowne kliknięcie również ją rozwija.
- Nieaktywny timer zajmuje jeden zwarty wiersz. Aktywny pozostaje rozwinięty. Cena i czas są obok siebie; źródło zapisanego czasu jest jawne i aktualizowane po przesłaniu zdjęć.
- Na telefonie przyciski zapisania wykonania i przejścia do płatności są dostępne w stopce sekcji. Zapis wykonania nie oznacza automatycznej wpłaty.
- Źródło klienta i notatka podczas umawiania są w jednym opcjonalnym rozwinięciu. Wybór usług nadal zachowuje pozycje z innych kategorii, ceny uzgodnione i prognozy chemii.
- Podsumowanie pokazuje osobno otrzymane wpłaty, przychód ze zleceń i wynik po kosztach. Wykres kołowy zastępują opisane kwoty i paski udziału kosztów. Brak danych nadal oznacza niepełny wynik, nie zerowe koszty.
- Ustawienia mają grupy „Praca i wyniki” i „Konfiguracja”. Eksport jest schowany w osobnej sekcji. Nazwy, pola, odstępy i kolory są wspólne dla klientów, cennika, obszarów i paliwa.
- Pola w zwiniętych sekcjach ujawniają się przy błędzie walidacji. Link pominięcia nawigacji i fokus są dostępne z klawiatury. Zmniejszenie VisualViewport podczas edycji chowa dolną nawigację, aby zwolnić miejsce.

## Obsługa

1. Otwórz kalendarz i wybierz dzień. Na telefonie dotknij daty, aby rozwinąć miesiąc.
2. „+ Zlecenie”: wybierz usługi, klienta, adres i godzinę. Notatkę, źródło i prognozę chemii rozwiń w razie potrzeby.
3. Otwórz zapisane zlecenie. Użyj skrótów do zdjęć, zakończenia pracy i wpłaty. Dodatkowe korekty pozostają poniżej.
4. W Ustawieniach przejdź do podsumowania, paliwa, cennika lub obszarów. Wszystkie dotychczasowe adresy pozostają dostępne.

## Weryfikacja

`mvnw verify`: **67 testów**, 0 błędów i niepowodzeń, 0 pominiętych; 23 jednostkowe/MVC i 44 integracyjne z rzeczywistym PostgreSQL przez Testcontainers.

Przeglądarka Chromium, syntetyczne dane i osobny tymczasowy schemat PostgreSQL usuwany po sprawdzeniu:

- szerokości 360, 390, 768 i 1440 px: kalendarz z ośmioma zleceniami, podsumowanie, ustawienia, cennik, kartoteka, paliwo i wykonanie;
- nowy klient z poleceniem i notatką; powracający klient, usługi z dwóch kategorii, cena uzgodniona, adres tylko tej wizyty;
- symulowany HTTP 503 podczas rezerwacji: formularz zachowuje dane i po ponowieniu zapisuje obie pozycje;
- zmiana godziny, zakończenie zlecenia i wpłata częściowa;
- prywatne zdjęcia, automatyczne 40 minut, korekta do 60 minut i zachowanie korekty po kolejnym zdjęciu; zachowanie roboczej notatki podczas aktualizacji galerii;
- wyszukiwanie po telefonie, historia kontaktu i odroczenie ponownego kontaktu;
- ustawienie parametrów paliwa, dziennik 120 000 km / 2 h 15 min agregatu i tankowanie za 200 zł;
- przypisane dwa obszary w panelu dnia; brak poziomego przewijania;
- po restarcie nadal dostępne zdjęcia i ręcznie zapisany czas. Testy integracyjne obejmują również rzeczywistą chemię, edycję obszarów, ich kolory i dezaktywację.

Pomiar przy 390 px, identyczne zlecenie z trzema zdjęciami, sekcje domyślnie zamknięte; pozycje liczone względem własnego viewportu strony:

| Pomiar | Przed | Po |
| --- | ---: | ---: |
| Wysokość nagłówka | 117 px | 60 px |
| Początek sekcji wykonania od góry | 658 px | 587 px |
| Wymagane pola HTML rezerwacji nowego klienta z jedną usługą | 6 | 6 |
| Wymagane pola wykonania | 1 | 1 |
| Ekrany formularza rezerwacji | 1 | 1 |

Nie dodano obowiązkowych danych. Data i cena są wstępnie wypełnione; liczba wymaganych pól HTML nie oznacza liczby ręcznie wpisywanych wartości. Nie mierzono czasu użytkownika ani nie deklaruje się procentowego przyspieszenia pracy. Fizyczna klawiatura ekranowa iOS/Android wymaga sprawdzenia na telefonie; testy szerokości w Chromium nie zastępują tego sprawdzenia.

## Podgląd

HTML powstaje z odpowiedzi aktualnych szablonów Thymeleaf, CSS i modułów JS. Używa wyłącznie syntetycznych danych. Działa nawigacja, kategorie, wybór dnia, formularze oraz lokalne zdjęcia. Zapis demonstracyjny nie wykonuje transakcji PostgreSQL, a przejście między stronami może przywrócić dane przykładowe. Trwałość, autoryzację OWNER i CSRF sprawdzono w aplikacji, nie w pliku HTML.

Środowisko zgłasza brak funkcji udostępniania serwera (`capabilities: []`). Aplikację zweryfikowano lokalnie w chmurze przez przeglądarkę, bez publicznego wystawiania portu. HTML sprawdzono osobno, także w zagnieżdżonym iframe z `sandbox="allow-scripts"`.

# ncLEANEK: pomoc kontekstowa, samouczki i przypomnienia

ncLEANEK prowadzi właściciela przez istniejący CRM i wskazuje dokumentację do uzupełnienia. W nagłówku pojawia się jeden przycisk pomocy; znaki ? wyjaśniają mniej oczywiste opcje po najechaniu, fokusie lub dotknięciu. Przypięta pomoc i krótkie samouczki działają także wewnątrz formularza bez zapisywania i kasowania pól.

Zachowano obecną grafikę i dodano chodzącą postać w scenie bazy. Nazwy Tycoon/Pixel Tycoon usunięto z ekranów. Dotychczasowe techniczne identyfikatory plików, API, preferencji ruchu i tabel są zgodne wstecznie.

Przypomnienia korzystają z istniejącego DayReviewService i jego sygnatury zamknięcia. Puste i przyszłe dni nie tworzą własnych zaległości; zamknięcie po korekcie jest ponownie sprawdzane. Dla dzisiejszego planu regułą jest 18:00 Europe/Warsaw lub wcześniejsze rozstrzygnięcie wszystkich statusów. Wyciszenie/odroczenie jest świadome; brak automatycznych popupów, SMS i powiadomień systemowych.

## Walidacja

- Maven Wrapper verify: **121/121** (36 unit/MVC +85 PostgreSQL/Testcontainers), 0 failures/errors/skipped.
- Nowe testy reguł: pora dnia, pusty/przyszły/anulowany/zamknięty dzień, historyczny brak i ponowne otwarcie, właściwy ID, brak zmiany rekordów po odczycie, restart, OWNER i no-store.
- Rzeczywisty UI: 360/390/768/1440 px, 8 wizyt, pomoc i fokus/Escape, formularz z zachowanymi polami oraz prawdziwy zapis, preferencje/wyciszenie, krótki viewport390×430, ruch i jego ograniczenie.
- Dotychczasowy scenariusz rezerwacji, wykonania, zdjęć, chemii, kosztów, notatek i zamknięcia dnia przeszedł z restartem.
- axe: 16 kombinacji pomocnika, samouczka, formularza i preferencji, 0 zgłoszonych naruszeń. Incomplete raportowane jawnie, bez zaliczania jako poprawne.
- DEMO62 rzeczywistych widoków: pomoc, samouczek, zapis rezerwacji, preferencje między ekranami, Klienci/Ustawienia i zagnieżdżona ramka sandbox; bez błędów JS.

Instrukcja i wyniki: docs/NCLEANEK.md, docs/audit/ncleanek-*.json. Aktualny klikalny podgląd: preview/ncleanek-demo.zip — wypakuj i otwórz index.html. Zapis DEMO działa w pamięci, dane są syntetyczne.

## Zakres i ograniczenia

Bazą jest pełna aplikacja release/tycoon-0.1; PR należy porównywać z tą gałęzią. Bez migracji i zmian zasad zapisu, cen historycznych, OWNER, CSRF lub danych produkcyjnych. Preferencje nie przechowują kontaktów ani treści formularzy. Pomoc nie korzysta z zewnętrznego AI i nie blokuje CRM przy błędzie odczytu.

Fizyczny telefon/klawiatura Android/iOS, pełny czytnik ekranu i starsze przeglądarki bez Popover API nie zostały sprawdzone. Panel podglądu Codexa jest niedostępny; HTML testowany oddzielnie. Nie wykonano wdrożenia ani połączenia z nclean.pl.

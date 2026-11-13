# ncLEAN CRM 0.1: pełny Tycoon i stabilny zapis

Przyszłe wizyty dało się oznaczać jako wykonane przez edycję i starszy formularz, a nieprawidłowy identyfikator zamknięcia dnia otwierał inną wizytę. CRM teraz odrzuca te operacje atomowo, odnawia CSRF po ponownym zalogowaniu i zachowuje szkic przy błędzie. Korekta kwoty/czasu w pakiecie ujawnia konflikt podziału i wymaga świadomej decyzji.

Tycoon jest stałym wyglądem całej aplikacji, wraz z logowaniem, wylogowaniem i błędami. Klienci i legacy lista zleceń mają paginację, lookup jest ograniczony w SQL, pozycje zleceń są pobierane zbiorczo. Zdjęcia JPEG/PNG zachowują prawidłową orientację EXIF przed usunięciem metadanych. OWNER, CSRF, PostgreSQL, historia cen i migracje V1–V14 pozostają zachowane.

Gałąź zawiera również wcześniejszą pełną implementację CRM z `ux/lean-workspace`, która nie znajduje się jeszcze w starszym main. To PR całego kandydata 0.1; nie należy traktować go jako małej zmiany wyłącznie CSS. Bez nowych modułów w tej stabilizacji, bez produkcji nclean.pl i bez automatycznego wdrożenia.

## Walidacja

- Maven Wrapper: pełne `verify` **114/114**, 0 failures/errors/skipped; 30 MVC/unit + 84 PostgreSQL/Testcontainers. Po końcowych zmianach ponowione właściwe zestawy: 30 unit/MVC + 2 IT, również zaliczone.
- Prawdziwy UI i PostgreSQL: nowy/powracający klient, dwie kategorie/ceny/adres, osiem wizyt, pomiar/zdjęcia, koszty/notatki, zamknięcie dnia i restart.
- Relogowanie w drugiej karcie i utrata odpowiedzi po commicie: jedno zlecenie, jeden klient, dwie pozycje; bez utraty szkicu. Korekta pakietu i aktywny timer: atomowa walidacja z widocznym rozwiązaniem konfliktu.
- 76 wejść przy 360/390/768/1440 px; oczekiwane statusy i brak poziomego przewijania. axe: 30 kombinacji, zero zgłoszonych naruszeń; incomplete pozostaje jawne i nie jest zaliczone.
- Dump/restore do oddzielnej bazy: zgodne rekordy i prywatne BLOB-y, uruchomienie tego samego JAR i logowanie na restore.
- Klienci 303 / zlecenia 503: powtarzane pomiary i JDBC w raporcie. Pięć scenariuszy samodzielnego DEMO, także w ramce sandbox.

## Obsługa

Kalendarz → dzień → + Zlecenie. Wybierz usługi z kilku kategorii, klienta/adres i zapisz. Po wizycie użyj Wykonania albo Uzupełnij dzień. Gdy suma różni się od podziału, popraw pozycje lub świadomie usuń bieżący podział. Przy timeout sesji pozostaw kartę, zaloguj się w nowej i ponów zapis.

Instrukcje: `docs/TYCOON-0.1.md`, `docs/WINDOWS.md`, `deploy/README.md`. Raport i R01–R12: `docs/TYCOON-0.1-VALIDATION.md`. DEMO: `preview/nclean-tycoon-0.1-demo.zip`; po wypakowaniu otwórz index.html, zapis tylko w pamięci. Commit/sumy w `preview/build.json`.

## Odbiór przed produkcją

**NIEGOTOWE do formalnego odbioru produkcyjnego**: brak próby na własnym VPS, Windowsie i fizycznym telefonie; ręczna dostępność do uzupełnienia. OSV zablokowane przez sieciowe403 — brak potwierdzenia CVE nie oznacza braku podatności. Panel podglądu Codexa jest niedostępny; klikalny HTML testowany oddzielnie. PR pozostaje draftem do odbioru.

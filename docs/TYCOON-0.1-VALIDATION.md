# Cleaning Tycoon 0.1 — wynik stabilizacji, 13.11.2026

**Kandydat do pilotażu. NIEGOTOWE do formalnego odbioru produkcyjnego:** nie wykonano prób na własnym VPS, fizycznym telefonie i Windowsie; pełna analiza CVE została zablokowana przez sieć. Wyniki automatycznej dostępności zawierają pozycje wymagające ręcznej oceny. Te ograniczenia nie są oznaczone jako zaliczone.

Potwierdzone P1 z audytu zostały poprawione i mają regresje. Tycoon jest wspólnym, stałym wyglądem, bez flagi startowej. Nie wdrażano aplikacji, nie scalano gałęzi i nie łączono się z produkcją nclean.pl.

## Baza i zakres zmiany

Gałąź `release/tycoon-0.1` pochodzi z `audit/final-tycoon-readiness@5bfab90`, opartej na pełnej aplikacji `ux/lean-workspace@900ea19` (kod `ffabed1`). Świadomie zachowano pełny CRM zamiast cofnięcia do starszego main. Identyfikatory końcowego kodu, JAR i paczki są w [preview/build.json](../preview/build.json).

Java 17.0.20.1, Spring Boot 3.5.16, Thymeleaf, PostgreSQL/Testcontainers 17.11; lokalny PostgreSQL działa przez Compose na loopbackie. Pozostały OWNER, CSRF, CSP, prywatne pliki, transakcje, blokady wersji i dotychczasowe tokeny idempotencji. Migracje V1–V14 nie zostały zmienione; nie było potrzeby dodawać schematu.

## Testy i rzeczywiste dowody

- Pełne Maven Wrapper `verify`: **30 jednostkowych/MVC + 84 PostgreSQL/Testcontainers = 114**, 0 porażek, 0 błędów, 0 pominiętych. Log pełnej serii: prywatny `tycoon-release/verify-final.log`. Po końcowych poprawkach stron 404 wykonano ponownie 30 testów jednostkowych/MVC oraz odpowiednie testy integracyjne, bez porażek. Powtórzeń nie doliczamy do liczby unikalnych testów.
- Na początku wykonano bazowe 107 testów. W trakcie poprawiono dwie stare oczekiwane asercje (klasyczny motyw i przyszły DONE), zdublowaną nazwę w nowym zestawie danych oraz niekompletny fixture obrazu. Końcowy fixture limitu to pełny syntetyczny PNG 6000×4000. Nie ukrywano pominiętych testów ani nie wyłączano walidacji.
- Rzeczywista aplikacja: nowy i powracający klient, dwie kategorie/cena indywidualna/adres, zdjęcia i czas, pomiar chemii, anulowanie/przełożenie, notatki/cele, koszty, zamknięcie dnia, konflikt dwóch kart i restart. Eksportowano 62 rzeczywiste widoki po tych próbach.
- Osobny scenariusz: wygasła sesja, zachowany szkic, login w drugiej karcie; pierwsza odpowiedź została zagubiona **po commicie**, następnie świadome ponowienie. Wynik: **1 klient, 1 rezerwacja, 2 pozycje usług**, ten sam token. Także formularz kategorii przetrwał relogowanie. Dowód: [browser.json](audit/tycoon-0.1-browser.json).
- Korekta pakietu 200 → 210 zł: komunikat przy kwocie, rozwinięcie podziału, brak zapisu przed decyzją, świadome usunięcie bieżącego podziału, zapis 210 zł i zachowana historia. Dodatkowo pakiet z aktywnym timerem i ręcznym niezgodnym podziałem czasu: atomowe odrzucenie, pokazanie sekcji, świadome wyczyszczenie i zapis rzeczywistego łącznego czasu. [timer.json](audit/tycoon-0.1-timer.json).
- 19 tras × 4 szerokości **360/390/768/1440 = 76 wejść**: oczekiwane statusy, brak poziomego przewijania; także login, logout, offline i brakująca strona. Nie ma to znaczenia testu każdego wariantu danych lub fizycznego urządzenia.
- ZIP: 5 scenariuszy Playwright — rezerwacja/edycja, obszary/cennik/klienci, koszty/notatki, postęp/animacje, wspólne wykonanie i niezapisany szkic przy zmianie widoku. Sprawdzono również zagnieżdżoną ramkę sandbox. Brak błędów JavaScript w zaliczonych scenariuszach.

Polecenie pełnej serii: `./mvnw -B -ntp verify` (w tym środowisku użyto również lokalnego mirroru/cache Maven i `-o`). Testcontainers uruchamia własny PostgreSQL. `./mvnw test` nie zastępuje testów integracyjnych.

## R01–R12 — zamknięcie ustaleń

| ID | Wynik | Dowód / ograniczenie |
| --- | --- | --- |
| R01 | Naprawiono | Wspólna reguła daty Europe/Warsaw blokuje przyszły DONE w edycji, legacy complete i wspólnym wykonaniu, również przełożenie wykonanego na przyszły DONE. Testy porównują cały rekord/pozycje i brak wpisu historii/odznaki po odrzuceniu; dziś/wczoraj dozwolone. |
| R02 | Naprawiono | Jawny błędny lub niepasujący ID daje 404, bez formularza zastępczego klienta. POST nadal wiąże datę i wersję. Stary link po przełożeniu daje 404. Po przeniesieniu ostatniej wizyty poprawny zapis nie kieruje do nieistniejącego już ID w starym dniu. |
| R03 | Naprawiono badane obejście | Wspólny moduł odczytuje chroniony `/session` przed zapisem, odnawia CSRF po zalogowaniu, zachowuje szkic i nie odtwarza mutacji automatycznie. Stary token/utracona odpowiedź: regresja rzeczywistej przeglądarki. Blokady wersji nadal działają. To ochrona otwartej karty, nie trwały offline. |
| R04 | Naprawiono | Wskazanie przy sumie, rozwinięcie sekcji, fokus i jawne usunięcie podziału; także odpowiedź walidacji czasu serwera. Bez proporcjonalnych domysłów i bez usuwania walidacji. |
| R05 | Naprawiono | Jeden stały Tycoon: normalny start, login/logout, bezpieczne błędy, moduły i publiczna strona offline. Znikła etykieta TEST z produktu. Uaktualniono Windows/README; historyczny profil jest niepotrzebny. DEMO pozostaje osobnym artefaktem. |
| R06 | Naprawiono wykryte naruszenia | Czytelniejsze opisy obszarów, HEX/statusy, nagłówki i dni tygodnia. Ponowny axe: 30 kombinacji, zero zgłoszonych naruszeń; incomplete opisano poniżej. |
| R07 | Naprawiono | Jeden skip-link i rzeczywisty cel main; H1 kalendarza dostępny także na telefonie; search/callbacks otrzymały wspólną obsługę. Miały już fragment theme — audyt błędnie sugerował brak wszystkich assetów. |
| R08 | Naprawiono | PWA start_url = `/calendar`. Cache obejmuje wyłącznie publiczny, samowystarczalny komunikat offline; żadnych stron klientów, API, zdjęć ani rachunków. |
| R09 | Naprawiono badane zasoby | Klient/zlecenie/obszar/plik bez zasobu → 404; także edycja nieistniejącego zlecenia. Ekrany błędu bez SQL i stack trace. Konflikt wersji nadal jest odrzuceniem edycji. |
| R10 | Naprawiono wskazane pobieranie | Klienci i legacy lista zleceń: strony 25, lookup max50 już w SQL, stabilna kolejność. `/orders` używa 3 zbiorczych odczytów pozycji/chemii/pomiarów zamiast 3 na każde zlecenie. Profil klienta i powtórzenie wizyty pobierają właściwy rekord niezależnie od stron listy. Pomiary niżej. |
| R11 | Uporządkowano | Jeden handler hash/rozwijania w interface.js, wspólny fragment theme z session.js i lean.js; brak drugiego publicznego motywu klasycznego. Nie wykonano ryzykownego przepisywania wszystkich szablonów tylko dla porządku. |
| R12 | Częściowo zweryfikowano, ograniczenie środowiska | Fresh/upgrade/restart i dump/restore z uruchomieniem aplikacji przechodzą. Windows/PowerShell, fizyczny telefon i rzeczywisty VPS pozostają do odbioru. OSV: sieciowe 403, brak pełnej analizy CVE. |

## Macierz zachowanych ścieżek

| Scenariusze promptu | Weryfikacja |
| --- | --- |
| A–C: nowy/powracający, wiele kategorii, indywidualna cena, idempotencja | Rzeczywisty UI + PostgreSQL; błąd503/ponowienie i utrata odpowiedzi po commicie; adres wizyty nie nadpisuje domyślnego. |
| D–F: sesja, osiem wizyt, statusy/ID | Rzeczywiste relogowanie, osiem kart, cztery szerokości; regresje HTTP/domain i dwie karty. |
| G–I: wykonanie, podział, zdjęcia | Wspólny formularz, rzeczywiste ilości/zero/brak, notatki/wyjątki, JPEG/PNG, nazwy/czas/timer, korekty i prywatność. Obrót ośmiu orientacji EXIF, wyjście bez EXIF i limit24 MP testowane na syntetycznych fixture'ach. HEIC odrzucany, nie obsługiwany. Fizycznego aparatu nie badano. |
| J: wpłaty/korekty | PostgreSQL: wpłaty częściowe, powtórzenie tokenu, korekty, anulowanie z nierozliczoną wpłatą i cena poniżej wpłat odrzucane. |
| K–M: paliwo/koszty/cennik/chemia | Testy liczbowe PostgreSQL: odczyty, luki/korekty, tankowanie, rachunki, zakup/zużycie/inwestycja, miesięczne, historyczne ceny, mediany/propozycje i wymagana próba. |
| N–P: obszary/klienci/notatki/osiągnięcia | Wiele obszarów, HEX, tygodnie/wyjątki, dezaktywacja z historią; źródło, kontakt, memo/zadania/cele, prawdziwy postęp i transakcyjne odznaki. |
| Q: zabezpieczenia | MVC + real HTTP: anonimowy, brak OWNER/CSRF, prywatne pliki, eksport/CSV i walidacja. Nie jest to pełny test penetracyjny. |
| R–S: migracja/restore i wygląd | Świeża V1–V14, upgrade V1/V6/V12, restart i restore do nowej bazy, logowanie, hash rekordów/BLOB. Wszystkie główne widoki Tycoon; publiczne ścieżki i responsywność. Brak formalnej próby błędu500 wywołanego awarią infrastruktury. |

## Dostępność i mobilność

axe-core 4.10.3 z ograniczonym ruchem: **30 kombinacji**, **0 zgłoszonych naruszeń**. [Surowy raport](audit/tycoon-0.1-accessibility.json). Zawiera też **incomplete we wszystkich 30 kombinacjach**: kontrast, a w części aria-prohibited-attr/link-in-text-block. Nie uznano ich za zaliczone. Wymagają ręcznej oceny, zwłaszcza na grafice/tle i w dialogach.

Rzeczywisty UI sprawdzono przy 360/390/768/1440 px, z klawiaturą desktopową, zamknięciem dialogu/fokusem, ograniczeniem animacji i krótkim viewportem. Kontrolowano czytelność pierwszej akcji i kart bez poziomego przewijania. Nie wykonano fizycznego telefonu, klawiatury Android/iOS, czytnika ekranu ani pełnego ręcznego odbioru WCAG/zoom dla wszystkich formularzy. Brak naruszeń axe nie jest certyfikacją dostępności.

## Wydajność i odtworzenie danych

[Surowe pomiary i restore](audit/tycoon-0.1-performance-restore.json): 303 klientów i 503 zlecenia syntetyczne. Dla każdej trasy pierwszy pomiar i trzy kolejne, mediana ciepłych pomiarów; lokalne HTTP i DEBUG JDBC bez logowania parametrów. To obserwacja tego środowiska, **nie SLA ani symulacja LTE**. Kalendarz nadal renderuje cały miesiąc. Dane o FCP są pojedynczą obserwacją, nie gwarantowanym czasem użyteczności.

| Trasa | HTML (B) | Mediana 3 ciepłych odczytów | JDBC na odczyt |
| --- | ---: | ---: | ---: |
| /calendar | 445549 | 295.1 ms | 12 |
| /customers | 39295 | 20.1 ms | 1 |
| /dashboard?range=month | 65893 | 150.0 ms | 29 |
| /orders | 55850 | 88.4 ms | 14 |

`pg_dump -Fc` → nowa, oddzielna baza → `pg_restore --single-transaction`. Porównano liczby i hashe pełnych wierszy w 8 tabelach: 503 rezerwacje,303 klientów,503 pozycje oraz wpłata, historia, memo, rachunek i zdjęcie. Następnie ten sam JAR uruchomiono na restore, zalogowano się i odczytano kalendarz/notatkę. Źródłowej bazy nie nadpisano. Testowe kopie i bazy usunięto; zawierały wyłącznie dane syntetyczne. To test procedury, nie wdrożony system zaszyfrowanych kopii poza serwerem.

## Dostarczenie i dalszy odbiór

[Instrukcja właściciela](TYCOON-0.1.md), [Windows](WINDOWS.md), [własny staging/VPS/HTTPS/sekrety/backup](../deploy/README.md), [paczka DEMO i identyfikacja](../preview/README.md).

Próba panelu Codexa: środowisko zgłasza `capabilities: []`, brak narzędzia sterowania współdzieloną przeglądarką. `open_in_codex` odpowiedział „This app tool is unavailable through dynamic tools”. Potwierdzono Chromium/Playwright i klikalny HTML, **nie potwierdzono działającego panelu podglądu Codexa**. Paczkę należy pobrać, wypakować i otworzyć lokalnie.

OSV querybatch: proxy zwróciło **403 Forbidden**. [Raport zależności](audit/tycoon-0.1-dependencies.json) podaje zinwentaryzowane JAR/koordynaty i ograniczenie; brak odpowiedzi nie oznacza braku CVE. Nie instalowano ciężkiego skanera ani nie zmieniano frameworka na podstawie domysłów.

Przed formalnym zamknięciem produkcyjnym: odbiór na Windows i fizycznym telefonie, ręczne pozycje dostępności, pełna analiza zależności, własny staging z HTTPS/reverse proxy oraz rzeczywista retencja/kopie poza serwerem. Nowe moduły i integracje z CMS pozostają poza tym zadaniem.

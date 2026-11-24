# Audyt ncLEAN CRM — 24 listopada 2026

Audytowana aplikacja: **TEST.6, `c1a2a9ba134ac5d3b5eedd86c227069f907c0376`**, gałąź `feat/customer-value-and-returning`. Sprawdzono zdalne referencje; nie znaleziono nowszego wydania aplikacji. Punktem odniesienia nie był `main`. Praca audytowa odbyła się na `audit/crm-test6-2026-11-24`, przy początkowo czystym katalogu Git.

**Ocena:** aplikacja ma dobrą bazę techniczną i działające główne procesy. Najpierw warto poprawić niezawodność zapisu i zgodność migracji, następnie przygotować kolejne wydanie do dłuższego użytkowania. Masowe przepisywanie kodu albo wymiana technologii nie ma obecnie uzasadnienia. Zielone testy nie wykrywają wszystkich istotnych przypadków: dodatkowe próby odtworzyły pięć błędów zachowania, a przegląd operacyjny wykazał dalsze luki.

To raport i propozycje napraw. **Nie zmieniono kodu aplikacji, migracji, konfiguracji wdrożenia ani tagów TEST/FINAL.** Nie łączono się z nclean.pl ani bazą użytkownika. Wszystkie uruchomienia wykorzystywały nowe, izolowane bazy i dane syntetyczne. Nie inicjalizowano istniejącej instalacji i nie usuwano jej `.local`, zdjęć ani wolumenów.

## Wyniki i granice weryfikacji

| Obszar | Rzeczywiście wykonana weryfikacja | Wynik |
|---|---|---|
| Java i istniejące regresje | Pełne Maven `verify`, Java 17, prawdziwy PostgreSQL/Testcontainers | **250 testów: 76 jednostkowych/MVC/architektury + 174 integracyjne; 0 błędów, 0 pominiętych** |
| Nowe scenariusze audytowe | 7 prób PostgreSQL: migracja zamkniętego dnia z pozycją, zmienione ponowienia, paliwo, korekta chemii, stoper, backup/restore | 7/7 odtworzyło oczekiwane obserwacje; pięć problemów opisano niżej. Są to testy wykazujące aktualne błędy, a nie potwierdzenie poprawności |
| Backup Docker PC | Uruchomiony rzeczywisty `docker/pc/database.sh`, dump i odtworzenie do innej bazy | **58 tabel publicznych: zgodne hashe zbiorów wszystkich wierszy, zachowane bajty syntetycznego zdjęcia**; błędna suma kopii odrzucona przed utworzeniem bazy docelowej; baza źródłowa bez zmian |
| Statystyki klientów | Chromium: typ prywatny/biznesowy, średnia/mediana kategorii, podział ceny, powroty, historia, konflikty, utrata odpowiedzi | PASS, 20 widoków responsywnych, reduced motion i odczyt bez JS |
| Źródła klientów | Chromium: klienci/zlecenia/przychód, braki, aktualne źródło profilu, okresy, korekty, klawiatura | PASS, 36 widoków, reduced motion i odczyt bez JS |
| Standardy realizacji | Chromium: telefon zamiast imienia, obowiązkowa notatka przy tworzeniu, standard chemii/czasu, korekty, sprzęt, offline, konflikty | PASS, 28 widoków |
| Sprzęt i serwis | Chromium: utworzenie, liczniki, plan, wykonanie, pojedynczy koszt mimo ponowienia, archiwizacja/przywrócenie | PASS, 12 widoków |
| Amortyzacja | Chromium: 3600/36 = 100, włączanie w raporcie, historia rewizji, zachowanie zamkniętych miesięcy i groszy | PASS, 12 widoków |
| Przegląd ekranów | 22 trasy × 360/390/768/1440 px, osobno `/` i `/crm` | PASS, po 88 widoków; bez przewijania poziomego, odpowiedzi 200, CSP i `no-store` |
| Ochrona sesji | Dostęp anonimowy do tras, zdjęć, CSV i API; POST bez CSRF; stara sesja po wylogowaniu | Dostęp zablokowany; wyniki i konkretne ścieżki w plikach dowodowych |
| Dostępność i HTML | axe-core 4.10.3, WCAG 2 A/AA i 2.1 AA, 10 ekranów przy 390 px; zapis tekstu przypominającego HTML | **0 automatycznie wykrytych naruszeń** w tych widokach; istnieją punkty wymagające ręcznej oceny. HTML wyświetlany literalnie, brak wstrzykniętego elementu/wykonania handlera |
| Przypomnienia | Trzy pomiary dla 0, 100 i 1000 archiwalnych powiązań | Potwierdzony wzrost kosztu odpytywania; A10 |
| Kod przeglądarki/skrypty | Test granic URL/service workera; składnia 32 JS, 29 Python i 3 shell | PASS; składnia nie jest testem działania skryptów Windows |
| Zależności | Rozwiązane zależności runtime z Maven; zapytanie OSV z nazwą i dokładną wersją, pobranie pełnych advisory | 78 artefaktów, 22 dopasowania w 6 artefaktach; warunki wykorzystania omówione w A09 |
| Budowanie obrazu | Próba `docker build --tag ncleancrm-audit:c1a2a9b .` | **Nieukończona:** rootless Docker/VFS wyczerpał miejsce w środowisku 7,9 GiB na kroku Dockerfile:5, przed kompilacją aplikacji. Usunięto wyłącznie identyfikatory cache utworzone przez tę próbę, odzyskując miejsce |

Szczegóły maszynowe: [results.json](results.json), [dependency-advisories.json](dependency-advisories.json). Wyniki przeglądarki i dostępności są dołączone obok raportu.

Nie jest to test produkcyjnego serwera, pełny pentest, certyfikacja WCAG ani test obciążenia na docelowym komputerze. Nie potwierdzono rzeczywistego podłączenia bazy właściciela: środowisko audytowe nie zawierało jego działającej instalacji PC. Nie zmieniano `compose.pc.yaml`. Nie wykonano PowerShell na Windows, pełnego uruchomienia obrazu PC po jego zbudowaniu, testów Safari/iOS ani fizycznego telefonu. Odtworzenie kopii zweryfikowało dane tabel i zdjęcia, ale nie porównało osobno stanu sekwencji ani wszystkich uprawnień/konfiguracji serwera. Kopia testowa zawierała syntetyczne dane, nie pełną wieloletnią bazę.

## Ustalenia według pilności

P1 — poprawić przed następnym wydaniem FINAL; P2 — ważne poprawki niezawodności lub przygotowania wydania; P3 — mniejsza dokładność i usprawnienia. Oceny odnoszą się do skutku w tym CRM, a nie wyłącznie etykiety biblioteki.

| ID | Priorytet | Problem | Dowód |
|---|---|---|---|
| A01 | P1 | Aktualizacja V22 → V24 zmienia podpis zamkniętego dnia z pozycjami zlecenia | Odtworzenie PostgreSQL |
| A02 | P1 | Ponowiony formularz ze zmienionymi danymi może dostać sukces, chociaż zmiany nie są zapisane | Odtworzenie dla zlecenia, notatki i kosztu |
| A03 | P2 | Starsza ścieżka paliwa dopuszcza nowy zakup benzyny do samochodu i diesla do agregatu | Odtworzenie PostgreSQL |
| A04 | P2 | Korekta rzeczywistej chemii nie zostawia historii poprzednich ilości | Odtworzenie + przegląd zapisu |
| A05 | P3 | Każde zatrzymanie stopera zaokrągla czas w górę osobno | Odtworzenie 3 × 10 s |
| A06 | P2 | Push na używane gałęzie `feat/**` nie uruchamia automatycznie CI | Konfiguracja workflow |
| A07 | P2 | Dokument aktualizacji nadal kieruje do starego TEST.1 | Konkretne polecenia w instrukcji wydań |
| A08 | P2 | Niepowodzenie backupu/builda w aktualizacji Windows pozostawia aplikację zatrzymaną | Kolejność poleceń i propagacja błędu w PowerShell; bez wykonania na Windows |
| A09 | P2 | Zależności runtime zawierają wersje objęte znanymi advisory | Maven + OSV; brak potwierdzenia wykorzystania w CRM |
| A10 | P2 | Odpytywanie przypomnień przebudowuje stan dla całej historii, również archiwalnej | Kod + pomiar HTTP na 1000 powiązań |

### A01. Zamknięty dzień zmienia status po migracji bez zmiany rozliczenia

Źródło: [DayReviewService.java:37](../../../src/main/java/pl/nclean/crm/work/DayReviewService.java#L37), [V23](../../../src/main/resources/db/migration/V23__job_equipment_usage.sql). Podpis dnia obejmuje `to_jsonb(i)` całego `booking_item`. V23 dodaje kolumny standardów/źródła rzeczywistego zużycia i uzupełnia `actual_source`. Samo rozszerzenie wiersza zmienia hash. Istniejący `day_review.fingerprint` pozostaje stary. Wyłączenie `customer_type` w podpisie zlecenia nie rozwiązuje zmian pozycji.

Próba: baza V22, wykonane zlecenie z pozycją, potwierdzoną ceną/czasem/chemią i wymaganiami zamknięcia; zapis podpisu dnia; migracja do V24. **Podpis po migracji jest inny mimo zachowania danych finansowych.** Dzień będzie traktowany jako zmieniony i może wymagać ponownego zamknięcia. To nie dowód utraty pieniędzy lub usunięcia zleceń.

Propozycja: ustalić jawny, wersjonowany zestaw pól wpływających na zamknięcie zamiast całego JSON wiersza. Przejście wykonać nową migracją/zgodnym mechanizmem podpisów, który nie zatwierdza automatycznie faktycznych zmian użytkownika. Nie edytować opublikowanej V23. Testować także już zaktualizowane bazy ze starym zapisanym podpisem, aktualizację od FINAL.1 oraz rzeczywistą późniejszą korektę zlecenia. Dotychczasowy test starego zamknięcia z anulowanym zleceniem bez pozycji nie pokrywał tego przypadku.

### A02. Zmienione ponowienie zgłasza powodzenie i pomija edycję

Źródło: [CrmRecords.java:116](../../../src/main/java/pl/nclean/crm/records/CrmRecords.java#L116), [NoteService.java:77](../../../src/main/java/pl/nclean/crm/notes/NoteService.java#L77), [CostService.java:65](../../../src/main/java/pl/nclean/crm/costs/CostService.java#L65). Dla istniejącego `request_token` kod zwraca identyfikator wcześniejszego zapisu bez porównania treści polecenia.

Próba: zapisać zlecenie za 100 zł z notatką A, następnie ponowić ten sam token, ale z 900 zł i notatką B. **Usługa zwraca ten sam identyfikator bez błędu, zapis nadal zawiera 100 zł i notatkę A.** Analogicznie notatka zachowuje tytuł A zamiast B, a koszt 10 zamiast 90 zł. Realny scenariusz to utrata odpowiedzi, poprawienie formularza przez użytkownika i ponowne kliknięcie zapisu.

Propozycja: porównywać znormalizowaną treść polecenia dla tokena; identyczne ponowienie zwraca poprzedni wynik, różne dane zwracają jawny konflikt z linkiem do utworzonego wpisu i zachowaniem formularza. Wzorować się na istniejącym porównywaniu payloadów w modułach sprzętu/amortyzacji. Nie rozwiązywać problemu losowaniem nowego tokena po każdym błędzie — powodowałoby duplikaty. Dla starych tokenów ustalić zgodną politykę, szczególnie gdy zapisany wpis został później edytowany. Osobno sprawdzić pozostałe starsze polecenia korzystające z tokenów, w tym legacy paliwa.

### A03. Reguła samochód=diesel, agregat=benzyna nie obowiązuje na wszystkich zapisach

Źródło: [FuelJournalService.java:118](../../../src/main/java/pl/nclean/crm/fuel/FuelJournalService.java#L118), [FuelJournalService.java:134](../../../src/main/java/pl/nclean/crm/fuel/FuelJournalService.java#L134). Walidacja pary paliwo/pojazd jest częścią warunku `meteredFull`. Starszy/szybki zapis tworzy zakup poza tym trybem. Nadal istnieje starsza trasa zapisu dla zalogowanego OWNER.

Próba: `saveQuickPurchase` przyjmuje nowe `CAR/PETROL` oraz `GENERATOR/DIESEL` i zapisuje je w bazie. Główny formularz pełny–pełny ma mocniejsze ograniczenia; błąd dotyczy alternatywnej ścieżki serwerowej, nie obejścia uwierzytelnienia.

Propozycja: wspólna reguła domenowa przy każdym nowym zakupie i zmianie celu/paliwa. Zachować stare wpisy i ich audyt; nie przepisywać historycznych wartości automatycznie. Dopuszczenie uzupełniania historycznych braków powinno być jawną, odrębną regułą, a nie ogólnym pominięciem walidacji. Zachować jednorazowy koszt całej kwoty zakupu w dniu zakupu; statystyka pełny–pełny nie tworzy drugiego kosztu.

### A04. Historia korekt chemii jest niepełna

Źródło: [ActualUsageService.java:42](../../../src/main/java/pl/nclean/crm/records/ActualUsageService.java#L42), zapis przez usunięcie i ponowne wstawienie `booking_actual_chemical`. Licznik `actual_version` chroni konflikt, ale nie jest historią wartości. Operacja nie dopisuje audytu poprzednich pozycji ani autora. Istniejący audyt samego wiersza `booking` ich nie zawiera.

Próba: standard 10, rzeczywiste zużycie 8, korekta do 9. Pozostaje 9 i prognoza 10, ale **wcześniejszej wartości rzeczywistej 8 nie można odczytać z audytu aplikacji**; liczba wpisów `booking_audit` przy korekcie nie wzrasta. Cena jednostkowa aktualnego zapisu jest zachowywana przez istniejący kod — nie jest to zarzut przeliczania wszystkiego dzisiejszym cennikiem.

Propozycja: transakcyjny audyt przed/po obejmujący ilości, jednostki, ceny snapshotów, źródło danych i autora dla pomiaru, wyzerowania i przywrócenia standardu. Opcjonalny powód korekty i prosty podgląd historii. Nie blokować potrzebnych poprawek i nie zmieniać prognoz historycznych.

### A05. Stoper zawyża sumę przy częstych przerwach

Źródło: [WorkService.java:26](../../../src/main/java/pl/nclean/crm/work/WorkService.java#L26) i zapis zatrzymania na linii 34. Każdy fragment jest osobno zaokrąglany przez `(seconds + 59) / 60` i dodawany do minut.

Próba: trzy odcinki po 10 sekund dają **3 minuty**, zamiast 30 sekund, czyli 1 minuty po jednorazowym zaokrągleniu. Czas w próbie przesunięto w izolowanej bazie; nie czekano sztucznie między zapisami.

Propozycja: przechowywać skumulowane sekundy, a zaokrąglenie wykonywać przy prezentacji/ostatecznym zatwierdzeniu. Zachować dotychczasowe ręczne minuty i wyraźne rozróżnienie standard/pomiar/zdjęcia.

### A06. Używane gałęzie funkcji nie dostają automatycznej weryfikacji po push

Źródło: [.github/workflows/verify.yml](../../../.github/workflows/verify.yml). Push jest ograniczony do `main`, `refactor/**`, `audit/**`. Gałąź obecnego TEST.6 to `feat/customer-value-and-returning`; inne ostatnie etapy również korzystały z `feat/**`. PR i ręczne uruchomienie są obsługiwane, ale przy przyjętym trybie bez PR zwykły push nowej funkcji nie wyzwala workflow.

Propozycja: objąć weryfikacją gałęzie używane do wydań oraz nowe tagi TEST/FINAL. Wydanie powinno wskazywać udany wynik dla dokładnego SHA. Dodać wybrane testy prawdziwej przeglądarki i skan zależności. Nie oznacza to, że wcześniejsze lokalne testy nie zostały wykonane — w tym audycie cały zestaw przeszedł.

### A07. Instrukcja może prowadzić do starej wersji aplikacji

Źródło: [docs/WYDANIA-TEST-FINAL.md:92](../../WYDANIA-TEST-FINAL.md#L92) oraz sekcja instrukcji na linii 118. Początek dokumentu wymienia TEST.6, ale polecenia aktualizacji i gotowa instrukcja nadal wybierają `nclean-v0.1-test.1`. W tej samej instrukcji powrót do FINAL.1 nie ma pełnej analizy zgodności późniejszego schematu.

Skutek: operator może uruchomić starszy kod lub próbować cofnąć aplikację na bazie po nowszych migracjach. Nie wykonano takiego cofnięcia na danych użytkownika.

Propozycja: jedna czytelna tabela bieżącego wydania i dokładnego SHA; stare instrukcje wyraźnie oznaczone jako historyczne. Aktualizacja i odzyskanie po problemie to oddzielne procedury. Sam checkout starego kodu nie jest bezpiecznym rollbackiem schematu. Nie przesuwać istniejących tagów.

### A08. Aktualizacja PC nie odzyskuje dostępności po błędzie sprzed migracji

Źródło: [scripts/CRM-PC.ps1:66](../../../scripts/CRM-PC.ps1#L66). `Update` wykonuje stop aplikacji, backup, build i dopiero start. Błąd backupu lub builda przerywa dalsze kroki; stary kontener pozostaje zatrzymany.

Propozycja: sprawdzenie miejsca i konfiguracji przed stopem; przygotowanie obrazu przed oknem niedostępności; backup po zatrzymaniu zapisu; w razie błędu **przed rozpoczęciem migracji** wznowienie poprzedniego kontenera. Po rozpoczęciu migracji nie robić automatycznego cofania kodu. Przetestować na Windows i przy wymuszonej awarii backupu oraz builda. To ryzyko dostępności ustalone z kodu, a nie odtworzona awaria na Windows ani dowód uszkodzenia bazy.

### A09. Zależności wymagają kolejnego przeglądu bezpieczeństwa

Źródło: rozwiązane zależności runtime z `pom.xml` i OSV na dzień audytu. [Pełne identyfikatory i zakresy](dependency-advisories.json). Dopasowania nie były wycofane w pobranych rekordach.

| Artefakt | Wersja w aplikacji | Liczba dopasowań | Ocena warunków w tym repozytorium |
|---|---|---|---|
| `log4j-api` | 2.24.3 | 1 | Dotyczy JSON `MapMessage` z wartościami niefinitywnymi. Nie znaleziono takiego użycia w kodzie aplikacji; obecny backend logowania to Logback |
| `jackson-databind` | 2.21.4 | 9 | Specyficzne typy/annotacje i deserializacja. Nie znaleziono w kodzie aplikacji wskazanych `JsonView`, `JsonTypeInfo`, `JsonIdentityInfo`, `InetAddress` ani analogicznych kontraktów wejściowych |
| `tomcat-embed-core` | 10.1.55 | 7 | Część etykiet to CRITICAL, lecz opisy dotyczą m.in. kontenerowych FORM/DIGEST, `web.xml`, rewrite valve i HTTP/2/TLS. CRM używa filtrów Spring Security; brak potwierdzonej osiągalności opisanych ataków w lokalnym HTTP PC |
| `spring-webmvc` | 6.2.19 | 2 | Dotyczy `XsltView` oraz SSE z fragmentami. Nie znaleziono ich użycia; aplikacja renderuje Thymeleaf |
| `jackson-core` | 2.21.4 | 2 | DoS parsera `DataInput` i kosztownego rozpoznawania liczby. Brak własnego użycia tych API w kodzie aplikacji; nie przeprowadzono dowodu osiągalności przez całą bibliotekę/framework |
| `postgresql` | 42.7.11 | 1 | Dotyczy `channelBinding=require` i określonego TLS. Lokalna konfiguracja PC nie korzysta z tego trybu; przyszły hosting wymaga osobnej oceny |

Przykłady: [Tomcat DIGEST](https://osv.dev/vulnerability/GHSA-9xv2-5v5q-p794), [Spring XSLT](https://osv.dev/vulnerability/GHSA-pc63-qcmh-9cmg), [JDBC channel binding](https://osv.dev/vulnerability/GHSA-j92g-9f8w-j867).

Propozycja: osobna, mała aktualizacja obsługiwanego BOM Spring Boot i sterownika oraz ponowne sprawdzenie faktycznie rozwiązanych wersji. Nie kopiować samej etykiety CRITICAL jako dowodu przejęcia CRM i nie podnosić pojedynczych bibliotek w ciemno poza zgodny zestaw. Po aktualizacji pełne testy, przeglądarka, migracja starszej bazy i skan powtórny. Dodać automatyczny monitoring zależności. Nie skanowano ukończonego obrazu ani pakietów systemowych, bo budowa obrazu nie dobiegła końca.

### A10. Koszt przypomnień rośnie wraz z całą historią

Źródło: [EntityReminderService.java:53](../../../src/main/java/pl/nclean/crm/notifications/EntityReminderService.java#L53). `due()` i potwierdzenia uruchamiają `reconcileLocked()`: unieważnienie wszystkich aktywnych wystąpień i iteracja po całym `reminder_owner`. Dla każdego właściciela `plan()` wykonuje trzy odczyty, zanim kod sprawdzi, czy źródło nadal jest aktywne. Także archiwalne zadania generują ten koszt. Polling w przeglądarce odbywa się co 60 sekund, przy wejściu i zmianie widoczności. Operacje używają jednej blokady doradczej.

Próba HTTP na izolowanym PostgreSQL, archiwalne zadania z jednym przypomnieniem, trzy odczyty na poziom:

| Liczba archiwalnych powiązań | Czasy [ms] | Mediana [ms] |
|---:|---|---:|
| 0 | 15,6 / 10,9 / 7,1 | 10,9 |
| 100 | 101,7 / 113,9 / 102,7 | 102,7 |
| 1000 | 1060,2 / 698,1 / 586,6 | 698,1 |

To pomiar porównawczy na współdzielonym środowisku audytowym, nie prognoza czasu na komputerze właściciela ani test wielu równoczesnych użytkowników. Kod potwierdza przyczynę niezależnie od wahań pomiaru.

Propozycja: uzgadniać wystąpienia przy zmianie konkretnego źródła/planu, a odczyt terminów oprzeć na indeksowanym zapytaniu. Co najmniej wykluczać nieaktywne źródła w SQL i usunąć N+1 odczytów. Zachować historię odczytania/odroczenia i atomowe przejęcie komunikatu przez jedną kartę. Dodać test liczby zapytań/skali dla historii, a nie kruchy limit milisekund w CI.

## Co działa i czego warto pilnować

- Spring Security wymaga OWNER dla danych, zdjęć i eksportów. CSRF jest włączony, wylogowanie unieważnia sesję; zapytania starą sesją zostały zablokowane. Cookies mają `HttpOnly` i `SameSite=Strict`, profil `prod` dodatkowo `Secure`.
- Nagłówki CSP ograniczają skrypty do własnego pochodzenia, blokują osadzanie i obiekty. Przykłady nginx mają ograniczenie prób logowania; rzeczywistego nginx właściciela nie badano.
- Eksport CSV ma ochronę przed formułami arkusza i poprawne cytowanie. Eksport nie jest pełnym backupem: nie zawiera wszystkich tabel i zdjęć.
- Service worker przechowuje publiczny ekran offline, nie prywatne odpowiedzi CRM. Testy granic URL/scope przeszły; dodatkowo działa uruchomienie w `/crm` oraz w korzeniu. To nie zastępuje testu współistnienia z produkcyjną stroną nclean.pl.
- Szkice formularzy nie zapisują danych klientów do `localStorage`; pamięć lokalna służy preferencjom pomocnika. Wygaśnięcie sesji zachowuje pola w bieżącej karcie. To świadomy kompromis użyteczności, nie dowód trwałego wycieku danych z formularza.
- Upload JPEG/PNG ma ograniczenie 8 MB i 20 MP, sprawdza rzeczywisty obraz i ponownie koduje JPEG. Zdjęcia są w bazie i wymagają logowania. Limity równoczesnego przetwarzania dużych obrazów warto sprawdzić na komputerze docelowym.
- Flyway ma wyłączone `clean`; Hibernate waliduje schemat, nie tworzy go automatycznie. Historyczne snapshoty cen i wersjonowane korekty pozostają istotnym zabezpieczeniem.
- Koszty paliwa są oparte o zakup, a pełny–pełny pozostaje statystyką. Amortyzacja jest opcją raportu; nie generuje ponownego wydatku. Nie ma potrzeby wracania do osobnej ewidencji wpłat.
- Compose PC wskazuje zewnętrzny `ncleancrm-pc-data`, aplikację na loopback i brak publicznego portu PostgreSQL. Obraz aplikacji działa jako nie-root, z ograniczeniem uprawnień i systemem plików tylko do odczytu. Są healthchecki i rotacja logów. `.dockerignore` dopuszcza tylko potrzebne źródła, bez `.local`, sekretów i backupów.
- Testy ArchUnit potwierdzają obecne granice modułów. To dobry punkt wyjścia do małych zmian, nie powód do budowania nowej aplikacji.

## Dalsze usprawnienia, bez pilnego przepisywania CRM

1. **Dane niepełne i historyczne:** zachować rozróżnienie historyczne/bieżące, standard/pomiar/brak danych. Obok wykresów pokazywać liczbę obserwacji i zakres dat. Dodać wygodny filtr wiarygodnego okresu, zamiast usuwać stare wpisy albo traktować braki jako zero. Źródło pozyskania i kategoria są obecnie interpretowane zgodnie z bieżącym profilem/katalogiem; etykiety mówią o tym. Jeżeli potrzebny będzie historyczny marketing, dodać snapshot źródła przy wizycie jako osobne wymaganie.
2. **Czytelny eksport:** dodać `customer_type` do CSV klientów i zleceń; oznaczyć zakres eksportu i odróżnić go od pełnej kopii bazy. Nie obiecywać odzyskania zdjęć i sprzętu z obecnego CSV.
3. **Czytelność kodu:** stopniowo wydzielać z `CrmRecords` obsługę rezerwacji, klientów i katalogu przez istniejące fasady. Zastępować krytyczne `Map<String,Object>` małymi typowanymi projekcjami. Formatować tylko dotykane klasy, oddzielnie od zmian zachowania. Nie zmieniać Spring/Thymeleaf na React.
4. **Spójność formularzy:** wspólny kontrakt konfliktu wersji/powtórzenia, zachowania pól i odnawiania CSRF. Szczególnie ważne dla telefonu i dwóch kart. Oprzeć się na działających mechanizmach sesji, zamiast dodawać kolejnego klienta HTTP obok nich.
5. **Obraz i odtwarzalność:** przypiąć wersję/digest obrazu JDK i okresowo go aktualizować. Runtime obecnie zawiera JDK i kompiluje/uruchamia pomocnicze narzędzia, więc przejście na JRE wymaga sprawdzenia inicjalizatora i healthchecka. Dodać skan gotowego obrazu i test uruchomienia dokładnego artefaktu wydania na CI z dostatecznym dyskiem.
6. **Backup poza komputerem:** mieć drugą, chronioną kopię na innym nośniku i okresowo wykonywać próbne odtworzenie. Suma SHA-256 wykrywa przypadkowe uszkodzenie, ale nie szyfruje danych i nie uwierzytelnia kopii, gdy ktoś może zmienić zarówno dump, jak i sidecar. To propozycja organizacji backupów; audyt nie wysyłał bazy nigdzie.

## Proponowana kolejność prac

**Etap 1 — bezpieczny zapis i migracja:** A01 + A02, osobne testy utraty odpowiedzi ze zmienionymi polami oraz migracji realistycznie zamkniętego dnia. Kryterium: żadnego cichego sukcesu dla innych danych i żadnego ponownego otwarcia wyłącznie przez dodanie metadanych. Przy okazji A03 ze wspólną regułą paliwa. Nie ruszać historycznych migracji ani starych cen.

**Etap 2 — przygotowanie wydania:** aktualne instrukcje, CI dla rzeczywistych gałęzi/tagów, bezpieczniejsze kroki aktualizacji PC i zgodna aktualizacja zależności. Weryfikacja na Windows/Docker PC z kopią syntetycznej starszej bazy, pełna budowa obrazu i odtworzenie do nowej bazy. Utrzymać trwały wolumen. Opublikować nowe wydanie TEST z nowym tagiem dopiero na wyraźne polecenie.

**Etap 3 — historia i skala:** A04, A10, następnie A05 oraz dostępność/eksport według wyników poniżej. Wydzielanie kodu wykonywać przy tych konkretnych poprawkach. Na końcu ponowna regresja procesów użytkownika: zaplanowanie → realizacja → wynik dnia/miesiąca, wprowadzanie starych zleceń, tankowanie, sprzęt i amortyzacja.

## Powtórzenie prób audytowych

`AuditEvidencePostgresIT.java` celowo znajduje się w dokumentacji i **nie jest częścią zwykłego CI**: jego asercje potwierdzają występowanie błędów TEST.6. Po naprawach należy napisać testy poprawnego zachowania, a nie utrzymywać oczekiwanie błędu. W osobnym, jednorazowym checkout/worktree można go skopiować do `src/test/java/pl/nclean/crm/audit/` i uruchomić:

```sh
./mvnw -Dtest=NoUnitTests -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=AuditEvidencePostgresIT verify
```

Wymagane Java 17 i Docker. Kontener jest nowy, hasło generowane na czas testu. Próba backupu wywołuje rzeczywisty skrypt repozytorium wyłącznie we własnym kontenerze. Logi, hasła QA i obrazy danych nie zostały dodane do Git. W repozytorium zapisano tylko kod prób i zanonimizowane wyniki. Standardowe testy aplikacji: `./mvnw verify`; test URL/PWA: `node scripts/test-web-boundaries.cjs`. Testy funkcji UI: `scripts/verify-equipment-live.py` z odpowiednią flagą, bez korzystania z instalacji użytkownika.

Dołączony `run-browser-audit.py` rozszerza istniejący izolowany runner o przegląd tras, ochronę sesji i pomiar przypomnień. Przykład, po zbudowaniu JAR-a i zainstalowaniu Python Playwright/bcrypt oraz Chromium:

```sh
python docs/audits/2026-11-24/run-browser-audit.py \
  --work-dir /tmp/nclean-audit-root --customer-sources-only --context-path ''
```

Opcja `--audit-accessibility` zamiast tego uruchamia axe i próbę escapowania HTML po wybranym scenariuszu funkcjonalnym. Wymaga pobranego `axe-core@4.10.3/axe.min.js` z https://unpkg.com/axe-core@4.10.3/axe.min.js do pliku `axe-4.10.3.min.js` w wskazanym `--work-dir`. To narzędzie audytu, nie nowa zależność aplikacji. Katalog wynikowy zawiera tymczasowe dane logowania QA, dlatego musi pozostać poza Gitem. Nie wskazywać żadnego katalogu instalacji właściciela.

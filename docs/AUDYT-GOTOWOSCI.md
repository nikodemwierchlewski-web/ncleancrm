# Audyt CRM i gotowości do wdrożenia

Raport zamknięcia: 12.11.2026 (Europe/Warsaw). Zakres: aktualny CRM na gałęzi `audit/release-readiness`, bazujący na `feat/day-closeout` (`5b816f4`), wraz z ostatnimi niescalonymi funkcjami. Próby techniczne rozpoczęto 11.11.2026.

**Werdykt: kandydat do pilotażu na odizolowanym stagingu. Wdrożenie na serwer użytkownika i integracja z aktualnym CMS nie zostały wykonane ani uznane za gotowe.** Kod CRM, testy, konfigurację HTTPS i odtworzenie danych sprawdzono lokalnie w chmurze. Brakuje połączenia VPN do serwera, potwierdzenia jego konfiguracji i bieżącego źródła CMS oraz zakończonego skanowania zależności pod kątem podatności.

## Co jest zgodne z ustaleniami

| Obszar | Ustalenie i dowód | Stan |
| --- | --- | --- |
| Technologia | Java 17, Spring Boot 3.5.16, Thymeleaf/HTML/CSS/JS, PostgreSQL 17, Flyway; bez React | Sprawdzone w kodzie i kompilacji |
| Kalendarz | Start w kalendarzu, miesiąc w 7 kolumnach na PC, panel dnia, tydzień i lista na telefonie; osiem wpisów jednego dnia | Testy integracyjne i Chromium |
| Obszary | Kilka obszarów, własne kolory, plan tygodnia i wyjątki, zachowanie historii po dezaktywacji | Testy PostgreSQL; nie ma automatycznego planowania |
| Umawianie | Jeden formularz, kilka kategorii, ilości, ceny uzgodnione, klient i adres wizyty | Przeglądarka; naprawiono utratę ręcznego adresu |
| Klienci | Wyszukiwanie i wybór, źródło pozyskania, historia wizyt i ponowny kontakt; bez automatycznego scalania po telefonie | Kod i scenariusze przeglądarkowe |
| Cennik i chemia | Edytowalne kategorie/usługi, standardy, prognozy, osobne pomiary; propozycje z mediany potwierdzonych danych | Testy jednostek i PostgreSQL |
| Historia cen | Rezerwacje i pomiary przechowują historyczne ceny; nowy cennik nie przepisuje wcześniejszych kwot | Testy PostgreSQL |
| Zdjęcia | Prywatne BLOB-y, przed/po, walidacja i ponowne kodowanie JPEG/PNG, czas z nazw; pierwszeństwo korekty ręcznej/timera | Testy i przeglądarka |
| Notatki | `work_note` pozostaje historyczną notatką/ustaleniami; `completion_note` po wykonaniu jest oddzielna | Testy i historia klienta; nie reinterpretowano starych danych |
| Zamknięcie dnia | Status, finalna kwota, chemia, zdjęcia/wyjątki i notatka/Bez uwag; trwałe zamknięcie i wykrycie późniejszych zmian | Testy, restart i dwie karty |
| Finanse | Przychód, wpłaty i należności osobno; marketing/koszty miesięczne bez podwójnego naliczania; niepełne dane oznaczone | Testy; poprawiono etykietę niepotwierdzonej ceny i analizę zera chemii |
| Paliwo | Odczyt licznika, agregat, zakupy; zakup paliwa nie jest drugim kosztem zużycia; luki i pierwszy odczyt nie udają zera | Testy i zapis w przeglądarce |
| Trwałość | MigracjeV1–V11 nie zmienione; restart; pełny dump/restore do innej bazy z uruchomieniem aplikacji | Sprawdzone na syntetycznych danych |
| Zabezpieczenia | OWNER, CSRF, sesja, prywatne zdjęcia, escapowany tekst i parametry SQL | Kod, testy MVC/HTTP i próba HTTPS |
| Zakres odłożony | Brak SMS, GPS, publicznego API integracji i inteligentnego planowania | Zgodne z dotychczasowym etapem |

## Naprawione ustalenia

| ID / priorytet | Problem | Poprawka i weryfikacja |
| --- | --- | --- |
| A01 / wysoki | Zwykła zmiana statusu umożliwiała anulowanie wizyty z nierozliczoną wpłatą, mimo blokady w zamknięciu dnia | Wspólna walidacja w `CrmRecords.reschedule`; test odrzucenia, zachowania wpłaty i anulowania po korekcie |
| A02 / średni | Wybór klienta nadpisywał ręczny adres konkretnej wizyty | Podpowiedź zastępuje pusty lub poprzednio automatycznie podany adres; Chromium: adres wpisany **przed** wyszukaniem pozostaje po wyborze klienta |
| A03 / średni | Równoczesne żądania z tym samym tokenem rezerwacji mogły kończyć się błędem unikalności zamiast zwróceniem tego samego ID | Serializacja sprawdzenia tokenu i zapisu; dwa wątki otrzymują jedno ID, powstaje jeden klient i jedno zlecenie |
| A04 / średni | Kompletne koszty mogły wyglądać jak kompletny wynik, choć cena końcowa pochodziła jeszcze z wyceny | Osobne `financialComplete` i licznik niepotwierdzonych cen; wynik orientacyjny i lista cen do potwierdzenia, bez zmiany historycznych kwot |
| A05 / średni | Jawnie potwierdzone zero chemii bez wierszy materiałów dawało pustą analizę kosztu usługi | Znany pusty zbiór sumuje się do 0; brak potwierdzenia nadal daje niepełne dane; test wyniku 120 zł po zerowym koszcie |
| A06 / średni | Zwykła korekta ceny mogła usunąć wcześniej świadomie wybrane „Bez uwag” | Pusty niezmieniony tekst zachowuje ten stan; wpisanie notatki go zastępuje; test obu operacji |
| A07 / wdrożenie | Profil prod miał Secure cookie, ale bez jawnej konfiguracji zaufanego proxy | Zaufanie tylko do loopbacka; przykład nginx nadpisuje nagłówki klienta; test rzeczywistego HTTPS, cookie i HSTS |
| A08 / bezpieczeństwo | Brak jawnej polityki źródeł skryptów i referera | CSP bez skryptów inline/eval, blokada osadzania, Referrer-Policy same-origin; testy nagłówków i przeglądarkowe wykrywanie naruszeń CSP |
| A09 / jakość | Brak workflow CI obejmującego PostgreSQL | Dodano workflow Java 17/Maven/Testcontainers, ograniczone uprawnienia i akcje przypięte do odczytanych SHA; uruchomienie w GitHub Actions pozostaje niesprawdzone |
| A10 / operacje | Instrukcja kopii nie uwzględniała nowszych zdjęć i zamknięć; README wskazywał stary ekran startowy | Uzupełniono skrypty/opis i instrukcję; pełna próba odtworzenia także wpłaty i zamknięcia dnia |

Nie zmieniano migracjiV1–V11, produkcyjnej bazy, CMS, DNS ani konfiguracji rzeczywistego serwera. Nie zmieniono historii cen i wcześniejszych zleceń.

## Faktycznie wykonane sprawdzenia

**Czysta kompilacja `./mvnw clean verify`: 80 testów — 24 jednostkowe/MVC i 56 integracyjnych z rzeczywistym PostgreSQL/Testcontainers. Zero błędów, niepowodzeń i pominiętych.** Testy obejmują także równoczesne zdjęcie/zamknięcie, równoczesną rezerwację i zachowanie „Bez uwag” po korekcie ceny. Docker i Java 17 były faktycznie użyte; nie pomijano integracji.

Przeglądarka Chromium, odizolowany i usunięty po próbie schemat PostgreSQL:

- nowy i powracający klient, usługi z dwóch kategorii, cena uzgodniona i adres wizyty;
- ręczny adres wpisany przed wybraniem podpowiedzi klienta zostaje zachowany;
- kontrolowany HTTP 503, ponowienie bez utraty danych, zmiana terminu, wykonanie i częściowa wpłata;
- osiem zleceń, dwa obszary, kalendarz, dashboard, ustawienia, cennik, klienci, paliwo i panel wykonania przy 360/390/768/1440px bez poziomego przewijania;
- prywatne zdjęcia, wyliczenie czasu i zachowanie korekty ręcznej oraz roboczej notatki;
- wyszukiwanie i odroczenie kontaktu, dziennik licznika/agregatu i zakup paliwa;
- restart zachowuje zapis; brak błędów JavaScript i zarejestrowanych naruszeń CSP.

Próba profilu **prod** w osobnych kontenerach, na prywatnym porcie hosta, przez nginx 1.28 z lokalnym jednodniowym certyfikatem:

- `nginx -t` i `systemd-analyze verify` przeszły; usługi systemd nie instalowano na serwerze użytkownika;
- klient HTTPS weryfikował certyfikat przez wskazany lokalny urząd zaufania; nie wyłączano weryfikacji TLS;
- faktyczne logowanie OWNER, sesja Secure/HttpOnly/SameSite=Strict, brak dostępu do CRM anonimowo i odrzucenie zapisu bez CSRF;
- CSP, HSTS, DENY, nosniff, no-store i same-origin; nagłówki przekazane przez klienta nie wyłączają HTTPS;
- limit logowania zwrócił HTTP 429;
- syntetyczna baza otrzymała 11 migracji, klienta, zlecenie, zdjęcie usługi, obraz rachunku, częściową wpłatę i zamknięty dzień;
- `pg_dump -Fc` i `pg_restore --single-transaction --exit-on-error` do **nowej bazy** zachowały dane, wpłatę, zamknięcie i skróty binarnych zdjęć/rachunków;
- aplikacja uruchomiona na odtworzonej bazie pozwoliła się zalogować, otworzyć kartotekę/zlecenie i zobaczyć nadal zamknięty dzień;
- tymczasowe bazy i kontenery aplikacji/proxy zostały usunięte. Zwykła lokalna baza developmentu pozostała zachowana.

Kopie syntetyczne przechowywano wyłącznie w ignorowanym katalogu roboczym, nie w repozytorium. Raport dowodów bez sekretów: `docs/audit/runtime-evidence.json`.

## Repozytorium i scalenie

Lokalnie znany `main`/`origin/main`: `7d26eb039bbf04e2481b8574040d214f377aefba`. Ostatnia implementacja zawierała14 kolejnych commitów nad tym main. Audyt jest osobną gałęzią obejmującą cały ten ciąg i poprawki, więc nie trzeba ręcznie cherry-pickować poszczególnych modułów. Nie kasowano gałęzi i nie przepisywano historii.

Końcowe ponowne sprawdzenie `git ls-remote origin refs/heads/main` potwierdziło na GitHubie dokładnie ten sam SHA co lokalny main: `7d26eb039bbf04e2481b8574040d214f377aefba`. Początkowy brak uwierzytelnienia Git ustąpił w trakcie zadania. GitHub API nadal wymaga osobnej weryfikacji dostępu; dotychczas zwracało Forbidden. Przed scaleniem należy ponownie odświeżyć referencje, przejrzeć pełny diff, uruchomić CI i dopiero wtedy wykonać scalenie. Nie scalano lokalnego ani zdalnego main w tej pracy.

Docelowo jeden przegląd kandydata względem main, ze sprawdzeniem wcześniejszych PR-ów i wykluczeniem podwójnego wprowadzenia zmian. Plik poprawki dołączony do zadania pozwala przejrzeć lokalnie przygotowany zakres również bez aktywnego dostępu GitHub.

## Bezpieczeństwo, zależności i granice audytu

- Jawne wartości poświadczeń nie są elementem konfiguracji repozytorium; `.local`, wypełnione sekrety, dumpy i certyfikaty są ignorowane. Proste skanowanie wybranych wzorców kluczy prywatnych, tokenów GitHub i AWS nie wykryło dopasowań w bieżących plikach i 711 obiektach lokalnej historii. To nie jest gwarancja braku wszystkich rodzajów sekretów ani audyt niedostępnej historii zdalnej.
- Wygenerowano CycloneDX SBOM z 78 komponentami. Nie zmieniano zależności aplikacji w ramach audytu.
- Sprawdzenie advisory API OSV nie powiodło się: polityka sieci zwróciła 403. **Nie ma podstaw, aby uznać wszystkie wersje zależności za wolne od znanych podatności.** Przed wdrożeniem wykonać aktualny skan SBOM/dependency scannerem z dostępem do aktualnych advisory, ocenić wyniki i ponownie przetestować ewentualne aktualizacje.
- Nie wykonywano testów penetracyjnych produkcji, prób na prawdziwych klientach ani fizycznych testów telefonu. Windows/PowerShell i klawiatura/aparat iOS/Android nadal wymagają sprawdzenia na urządzeniu.
- Baza ze zdjęciami wymaga planu pojemności; obecne limity pozwalają na 40 zdjęć po 8 MiB na zlecenie. Automatycznej retencji danych klientów/zdjęć nie dodano.
- Lokalna kopia nie zapewnia odporności na utratę serwera. Harmonogram, retencja, szyfrowanie i kopia poza serwerem muszą zostać skonfigurowane na właściwym hostingu.
- Kategoriom i obszarom nie dodawano w tej pracy nowego mechanizmu rozwiązywania równoczesnych edycji. Zamknięcie, zlecenia i pomiary mają kontrolę wersji; katalog/obszary nadal wymagają ostrożności przy edycji tego samego elementu w kilku kartach.
- Koszty stałe wcześniejszych miesięcy pozostają zablokowane do edycji zgodnie z obecną logiką. Ewentualne księgowanie spóźnionych kosztów jest decyzją produktową, nie zostało dopisane podczas audytu.

## Związek z nclean.pl i serwerem VPN

Użytkownik potwierdził własny serwer dostępny przez VPN i wcześniejsze udostępnienie kodu/repozytorium na koncie xazi091. W tej sesji dostępny był raport z przekazanego `NcleanCMS-master.zip`, nie aktualny checkout CMS. Raport wskazuje Java 17, Spring Boot 3.2.3, Thymeleaf i PostgreSQL. To uzasadnia zgodny stos CRM, ale nie potwierdza bieżącej konfiguracji produkcji.

`https://nclean.pl` zostało zablokowane przez proxy środowiska (CONNECT 403), a VPN nie jest skonfigurowany. Próba domniemanego adresu `xazi091/NcleanCMS` zwróciła Repository not found — nie dowodzi to braku repozytorium; dokładny adres i uprawnienia nie zostały potwierdzone. Nie połączono się z serwerem ani z bazą CMS.

W starym raporcie CMS znajdują się ustalenia o literalnych danych SMTP, publicznym mailingu i regule permitAll. Ich aktualność na produkcji jest **nieznana**. Należy zweryfikować je w bieżącym repo przed integracją; nie przenosić konfiguracji ani współdzielonych sekretów do CRM.

Rekomendacja: `crm.nclean.pl` jako osobny vhost, użytkownik systemowy, proces, baza, role, sekrety i kopie, początkowo dostępny przez VPN. Publiczna strona może prowadzić do logowania CRM. Przekazywanie zgłoszeń przez API jest osobnym etapem i wymaga ustalenia kontraktu, kontroli dostępu i zasad obsługi danych — samo kopiowanie tabel lub cookies nie realizuje integracji.

## Bramka wdrożeniowa

| Warunek | Stan |
| --- | --- |
| Kod kandydata, migracje i regresje CRM | Sprawdzone lokalnie; poprawki gotowe do przeglądu |
| HTTPS/prod, proxy i odtworzenie kopii | Sprawdzone w izolowanym teście, nie na serwerze użytkownika |
| CI i spójne scalenie do aktualnego main | Konfiguracja przygotowana; zdalne uruchomienie/scalenie niewykonane |
| Aktualne repo CMS i jego konfiguracja | Brak potwierdzonego dostępu |
| Połączenie VPN, OS/zasoby, porty, DNS i certyfikat docelowy | Niesprawdzone |
| Aktualny skan podatności zależności | Niewykonany z powodu niedostępności advisory API |
| Kopia poza serwerem, harmonogram, monitoring i obsługa awarii | Do skonfigurowania na serwerze |
| Próba na fizycznym telefonie i użytkowa praca na stagingu | Do wykonania |

Konkretny plan i przykładowe pliki do przeglądu: [deploy/README.md](../deploy/README.md). Dopiero po zamknięciu tych warunków można zatwierdzić wdrożenie produkcyjne. Nie potrzeba kolejnego przepisywania CRM ani wspólnej bazy z CMS.

# Diagnoza i naprawy CRM — 12.11.2026

Gałąź naprawcza: `fix/whole-crm-consistency`, od `feat/costs-investments` (`a979a27`). Obejmuje wcześniejsze funkcje bez kopiowania plików między gałęziami. Nie scalano do main i nie łączono się z produkcją.

## Ustalenia i przyczyny

| Problem | Dowód | Naprawa / wynik |
| --- | --- | --- |
| Podejrzenie błędu kompilacji | Czyste budowanie wersji wyjściowej zakończyło się sukcesem: 94 testy | Nie odtworzono błędu kompilatora. Potwierdzono poniższe błędy interfejsu i problemy uruchomienia/podglądu |
| Różne wersje kodu | Zdalny main ma SHA `7d26eb0`; pełna wersja `a979a27` jest 18 commitów dalej. Wszystkie lokalne gałęzie `feat/*` są jej przodkami | Jedna gałąź naprawcza zawiera całą tę historię. Przy sprawdzaniu funkcji nie należy uruchamiać starego main |
| Stara JVM wskazywała przebudowywany JAR w target | Proces na porcie 18083 używał `target/nclean-crm-0.0.1-SNAPSHOT.jar`. `/login` zwracało 200, ale `/calendar` przekroczyło 30 s; log tego serwera zawiera `NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy` | Zatrzymano zidentyfikowaną JVM, zachowano bazę. Nową aplikację budowano i uruchamiano osobno. Nie ma podstaw, żeby utożsamiać ten błąd z nieudaną kompilacją. Nadpisanie JAR podczas działania JVM jest prawdopodobnym źródłem problemu, lecz nie odtworzono dokładnego momentu jego wystąpienia |
| Wyszukiwanie klientów pozornie nie działało | Dwa kontakty Alfa/Beta: po szukaniu „Alfa” nadal dwa rekordy, puste pole i URL `/customers`. Wspólny handler POST odrzucał HTML wyników i przeładowywał niefiltrowaną stronę | Formularz korzysta z GET `/customers?query=...`. Wyniki i zapytanie zostają po odświeżeniu. Stary POST `/customers/search` przekierowuje na ten adres. Zachowano OWNER, CSRF dla starego POST, limit 120 znaków i escapowanie |
| Nowa pozycja kosztu pozostawała niewidoczna | Usunięcie pierwszego wiersza, następnie „+ Pozycja kosztu”: nadal dwa widoczne wiersze zamiast trzech. Klon dziedziczył `hidden` | Ponownie wykorzystujemy usunięty wiersz; nowe klony są widoczne. Odtworzenie działa także przy limicie 50 pozycji |
| Podwójne zatwierdzenie zwykłego formularza | Dwa zdarzenia submit podczas trwającego żądania wysyłały dwa POST-y | Wspólny handler blokuje równoczesne zgłoszenia danego formularza, oznacza `aria-busy`, zwalnia blokadę po błędzie i zachowuje pola. Usunięto konkurencyjne blokowanie przycisków z dziennika paliwa i ustawień kosztów |
| HTML nie odpowiadał całemu CRM | Ostatni eksport kosztów zawierał ograniczoną obsługę innych ekranów, wyłączał moduł notatek i wczytywał globalnie wszystkie style | Nowy eksport korzysta z jednego syntetycznego zestawu PostgreSQL, właściwych stylów każdego ekranu i oznaczenia wersji. Obejmuje 60 przechwyconych widoków; nadal jest symulacją HTML, nie działającym backendem |

Blokada równoczesnego wysłania formularza nie jest automatycznym scalaniem klientów po numerze telefonu. Osobne, świadomie dodane kontakty pozostają osobne. Gdy odpowiedź serwera zaginie już po wykonaniu zapisu nowego klienta, należy sprawdzić listę przed ponownym dodaniem; sam frontend nie zapewnia idempotencji przez restart. Rezerwacje, wydatki i notatki zachowują istniejące tokeny tworzenia.

## Wyniki końcowe

**Maven Wrapper `clean verify`: 96 testów — 26 jednostkowych/MVC i 70 integracyjnych PostgreSQL/Testcontainers. Zero niepowodzeń, błędów i pominiętych.** Użyto Java 17.0.20.1, Maven 3.9.11, Spring Boot 3.5.16 i lokalnego Dockera. Budowanie wykonano offline, ze wcześniej pobranych zależności; nie pomijano testów ani walidacji.

Pierwsza próba rozszerzonego zestawu wykazała błąd nowej asercji: liczyła także techniczny rekord Flyway utworzenia schematu. Poprawiono ją na liczbę migracji typu SQL; końcowe pełne budowanie powtórzono z sukcesem.

Nowe testy integracyjne sprawdzają filtr klientów, odświeżenie, kompatybilność starego POST, walidację/escapowanie oraz **V12 → V13 na kopii z syntetycznymi danymi**. Aktualizacja zachowała klienta, pełny rekord rezerwacji i pozycji, cenę, memo, obraz rachunku, tankowanie i koszt miesięczny. Dotychczasowe testy obejmują także pustą bazę i aktualizacje starszych migracji. Nie edytowano V1–V13; poprawki nie wymagały zmiany schematu.

Prawdziwa aplikacja, Chromium, oddzielne schematy PostgreSQL usuwane po próbach:

- właściciel, kalendarz, osiem wizyt, dwie kategorie usług, ilości/cena uzgodniona, nowy/powracający klient, poprawiony adres i zmiana terminu;
- kategoria, usługa, środek i standard utworzone przez interfejs; zmiana katalogu nie przelicza starej ceny i prognozy zlecenia;
- kolor HEX, nazwa obszaru, kilka przypisań, plan tygodnia i historia po dezaktywacji;
- zdjęcia przed/po, czas z nazwy, pierwszeństwo czasu ręcznego, zachowanie roboczej notatki, wyszukiwanie i odroczenie kontaktu;
- cena końcowa, rzeczywista chemia, anulowanie/przełożenie, wyjątki zdjęć, notatka/Bez uwag, zamknięcie dnia, konflikt dwóch kart i ponowne otwarcie po zmianie danych;
- dashboard dzień/tydzień/miesiąc, plan/wykonanie, paliwo, agregat, tankowania, historia i prywatne rachunki;
- koszty, inwestycje, chemia bez przeszacowania katalogu, rachunek do kosztu miesięcznego, edycja, anulowanie i brak podwójnego naliczenia;
- notatki, zadania, cele, kroki, przypięcia, archiwum/przywrócenie oraz dashboard;
- kontrolowany HTTP503 i ponowienie bez utraty danych, konflikty wersji i odczyt danych/załączników po rzeczywistym restarcie JVM;
- kalendarz i główne widoki przy **360, 390, 768, 1440 px**, bez poziomego przewijania; formularze przy zmniejszonej wysokości. Nie wykryto błędów JS; w głównym przepływie sprawdzono także naruszenia CSP.

Kwoty testowe obejmowały m.in. dwa opakowania chemii kupione za 80 zł bez zmiany katalogowej ceny 40 zł, rachunek marketingowy powiązany z konfiguracją 300 zł, oddzielny marketing 50 zł i inwestycję 2500 zł poza wynikiem operacyjnym. Miesięczne i roczne zakupy oraz operacyjne koszty porównywano z oczekiwanymi sumami. Braki pomiarów pozostały brakami, nie zerami.

## Powtarzanie regresji przeglądarkowych

W repozytorium jest `scripts/verify-browser.py`. Wymaga przygotowanej lokalnej konfiguracji i bazy z instrukcji Windows, zbudowanego JAR, Pythona, pakietu Playwright i Chromium. Plik właściciela to **prywatny** JSON z polami `username` i `password` zgodnymi z lokalnym kontem; umieść go pod ignorowanym `.local/`. Nie dodawaj go do Git ani opisu PR.

```text
python scripts/verify-browser.py --owner-file .local/qa-owner.json
```

Skrypt tworzy losowy schemat `browser_qa_*` wyłącznie w lokalnej bazie `127.0.0.1:55433/nclean_crm`. Uruchamia na loopbacku oddzielną JVM z kopią JAR poza target. Sprawdza filtr po odświeżeniu, usuwanie/dodawanie wierszy aż do limitu 50, dwa zatwierdzenia, HTTP503, zachowanie notatki i skuteczne ponowienie. Zatrzymuje własną aplikację i usuwa wyłącznie własny schemat. Prywatne logi pozostają w `.local/`. Wykonano ten skrypt w Linux; nie deklaruje to weryfikacji Windows/PowerShell.

## Spójne uruchomienie

1. Sprawdź `git status`, gałąź i `git log -1`. Zatrzymaj poprzednią JVM przed nadpisaniem używanego przez nią JAR. Nie kasuj danych w ramach naprawy.
2. Wykonaj Maven Wrapper `clean verify`. Raporty są w `target/surefire-reports` i `target/failsafe-reports`.
3. Dla codziennego lokalnego developmentu użyj `scripts/Start-Local.ps1`. Serwer uruchamia się na komputerze użytkownika pod `127.0.0.1:8081`; ten adres chmurowego procesu nie jest adresem na komputerze użytkownika.
4. Przy uruchamianiu gotowego JAR kopiuj go do osobnego katalogu uruchomienia; kolejne `clean/package` nie może zmieniać pliku, który czyta działająca JVM. Zanotuj commit i SHA-256 pliku.
5. Sprawdź logowanie oraz kalendarz po starcie. Po aktualizacji nie otwieraj starego pliku HTML jako dowodu stanu aplikacji.

Zweryfikowany artefakt: `nclean-crm-0.0.1-SNAPSHOT.jar`, SHA-256 `be7c1f180a57defcb443d1f9eebf23da4c39e69da36d2624192507b49e3532aa`. JAR był użyty do końcowych prób, w tym regresji przeglądarkowych i przechwycenia widoków eksportu.

## Granice

Platforma w tej sesji nie deklaruje możliwości wspólnego pulpitu ani przekierowania portów (`capabilities: []`). Sprawdzenie Playwright w chmurze nie oznacza dostępu użytkownika do localhost. Eksport HTML przeszedł osobną próbę: dodanie i edycja terminu zlecenia, obszary dnia, klienci/wyszukiwanie/historia, nawigacja po ustawieniach i cenniku, koszty/edycja/sprzęt, notatki/dashboard, cztery szerokości i zapis w zagnieżdżonym iframe `sandbox="allow-scripts"`, bez błędów JS. Nie jest to potwierdzenie dostępności natywnego hostingu podglądu Codexa.

Dostarczony plik HTML nie wymaga logowania; ma syntetyczne dane, zmiany w pamięci i przykładowe podsumowania. Nie służy do zapisywania prawdziwych danych klientów ani weryfikacji obliczeń backendu.

Nie sprawdzono fizycznego telefonu, jego klawiatury/aparatu, Windows, produkcji ani zdalnego CI. API GitHuba odrzuciło odczyt listy PR (`Forbidden`); samo sprawdzenie zdalnych gałęzi przez Git działało. Pozostałe ograniczenia wdrożeniowe z wcześniejszego audytu nadal obowiązują. Nie dodano nowych modułów i nie wyłączono zabezpieczeń CRM.

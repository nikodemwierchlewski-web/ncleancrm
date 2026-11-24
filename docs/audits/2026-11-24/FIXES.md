# Poprawki po audycie TEST.6

Gałąź: `fix/audit-test6`, punkt startowy: `1c5f28a` (audyt aplikacji `c1a2a9b`). Bez zmiany wcześniejszych tagów, V1–V24, danych instalacji właściciela i konfiguracji podłączenia `compose.pc.yaml`. Kod nadal korzysta ze Spring Boot 3.5, Java 17, Thymeleaf i PostgreSQL.

| Ustalenie | Zmiana |
|---|---|
| A01 | Jawna lista pól podpisu dnia. V25 przelicza wyłącznie podpisy dokładnie pasujące do niezmienionych danych TEST.6; podpisy sprzed V23 pozostają zgodne. Zachowane daty i autorzy zamknięcia. Faktyczne korekty nadal otwierają dzień, a dopisywanie historii nie przywraca przypomnienia zwykłego dnia. |
| A02 | Transakcyjny zapis oryginalnej treści polecenia dla zlecenia, notatki, kosztu i tankowania. Identyczne ponowienie zwraca ten sam wynik także po późniejszej edycji wpisu; inna treść daje konflikt 409, zachowane pola i link do istniejącego wpisu. Starszy token bez zapisanej treści również wymaga sprawdzenia wpisu zamiast cichego sukcesu. |
| A03 | Reguła samochód=diesel i agregat=benzyna obowiązuje również na starszych ścieżkach zapisu. Niepoprawna historyczna para nie jest przepisywana; można poprawić inne pola istniejącego wpisu. |
| A04 | Audyt ilości, jednostek, cen i źródła chemii przed/po pomiarze, wyzerowaniu i standardzie, z autorem i datą. Czytelna historia ostatnich 50 operacji w szczegółach zlecenia; starsze operacje pozostają w bazie. Nie odtworzono utraconej wcześniej historii. |
| A05 | Stoper sumuje sekundy, a zaokrągla dopiero sumę. Obejmuje pauzę, zakończenie i zmianę statusu. Wcześniejsze zapisane minuty pozostają zachowane, bez zgadywania dawnych sekund. |
| A06 | CI dla wszystkich gałęzi oraz tagów TEST/FINAL; przeglądarka, skan zależności, testy odzyskania aktualizacji PowerShell i budowanie obrazu. Harmonogram tygodniowy zacznie działać po włączeniu workflow do domyślnej gałęzi. Nie wykonano scalania ani PR. |
| A07 | Instrukcja wybiera dokładny SHA gałęzi poprawek i oddziela aktualizację od odzyskiwania po problemie. Usunięto polecenie cofania bieżącej instalacji do TEST.1/FINAL.1 oraz starą gotową wiadomość wdrożeniową. |
| A08 | Aktualizacja sprawdza rzeczywisty mount PostgreSQL przed zmianami. Build odbywa się przed zatrzymaniem aplikacji. Awaria przed startem migracji wznawia stary kontener po jego ID, bez odtwarzania go z nowego obrazu. Po rozpoczęciu migracji brak automatycznego rollbacku. |
| A09 | Jackson 2.21.9, Tomcat 10.1.62, Log4j 2.26.1 i JDBC PostgreSQL 42.7.15; zachowane linie zgodne z Boot 3.5/Java 17. 20 wcześniejszych dopasowań advisory zniknęło. Dwa zgłoszenia Spring opisano poniżej. Obraz bazowy Java 17 przypięty digestem. |
| A10 | Jeden odczyt aktywnych planów zamiast odczytów każdego archiwalnego właściciela. Aktualizowane są zmienione wystąpienia; odczyt gotowych komunikatów nie wykonuje N+1 zapytań o tytuły. Zachowano blokadę, stany odczytania i odroczenia oraz semantykę czasu Warszawy/DST. |

## Zależności — jawne ograniczenie

Maven Central nie udostępnia poprawionej wersji Spring Framework 6.2 dla `GHSA-j9f9-w8pj-32f8` i `GHSA-pc63-qcmh-9cmg`. Dostępne poprawki dotyczą Spring 7; podmiana samego Springa poza zgodną linię Boot nie jest bezpieczną aktualizacją. CRM nie używa wymaganych do wykorzystania tych błędów SSE z fragmentami ani XSLT.

Skan 79 rozwiązanych zależności runtime: **0 nierozstrzygniętych zgłoszeń, 2 jawnie opisane jako nieosiągalne w obecnym kodzie**. To nie oznacza braku advisory w bibliotekach. Wyjątki są ograniczone do dokładnych identyfikatorów i wersji, z ponownym przeglądem do **24.12.2026**. Skan blokuje dodanie wskazanych funkcji, nowe advisory, upłynięcie terminu i brak wiarygodnej odpowiedzi OSV. Konfiguracja: [dependency-applicability.json](../../../config/dependency-applicability.json).

## Weryfikacja

**Pełne `verify`: 258/258; następnie rozszerzona klasa regresji 10/10 — łącznie 260 unikalnych testów, bez błędów i pominięć.** Wyniki końcowe i liczniki: [fix-validation.json](fix-validation.json). Testy PostgreSQL obejmują aktualizację z V22 i V24, zachowanie zamknięć i historii, rzeczywiste późniejsze korekty, zmienione ponowienia, paliwo, historię chemii, precyzję stopera oraz backup i odtworzenie skryptem Docker PC do nowej bazy. Starsze testy migracji zachowują porównanie wszystkich dotychczasowych pól i checksumów; uwzględniają dodatkową kolumnę i V25. Fixture paliwa stosuje prawidłową parę, a osobny historyczny przypadek sprawdza odczyt dawnych niepoprawnych danych.

Przeglądarka na prawdziwym Spring/PostgreSQL: konflikt po utracie odpowiedzi i brak duplikatu w `/` i `/crm`, historia chemii, obowiązkowa notatka i standardy, statystyki klientów oraz źródeł; telefon 360/390 px, tablet 768 i desktop 1440. Dodatkowo 22 trasy × 4 szerokości, zabezpieczenia logowania/CSRF/wylogowania i ponowny pomiar przypomnień. Przy 1000 archiwalnych powiązań mediana spadła w środowisku QA z **698,1 do 8,9 ms**. To porównanie syntetyczne, nie gwarancja czasu na komputerze właściciela. [Dane pomiaru](fix-browser-validation.json).

Rzeczywisty PowerShell 7.4.13 na Linux, z atrapą Docker CLI: sześć scenariuszy sukcesu, błędnego mountu, awarii builda, backupu przy działającej/zatrzymanej aplikacji i startu po backupie. To test logiki skryptu, nie wykonane wdrożenie na Windows/Docker Desktop. Obrazu lokalnie nie budowano ponownie po wcześniejszym wyczerpaniu miejsca przez Docker VFS; dodano taki krok do CI. Nie należy utożsamiać lokalnych testów JAR-a z udanym zbudowaniem i wdrożeniem obrazu.

## Aktualizacja użytkownika

**Wymagana przebudowa obrazu aplikacji.** V25 wykona się przez Flyway przy uruchomieniu nowej wersji. Użyć istniejącego katalogu instalacji, oryginalnych sekretów i tego samego wolumenu, z backupem sprzed aktualizacji. Nie uruchamiać `Init`, nie kasować bazy i nie cofać starego kodu na nowszym schemacie. Szczegóły: [instrukcja aktualizacji](../../WYDANIA-TEST-FINAL.md).

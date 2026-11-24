# TEST → FINAL: wersja kodu, ta sama trwała baza

## Najnowszy TEST — wartości, powroty i rodzaj klienta

`nclean-v0.1-test.6`, gałąź `feat/customer-value-and-returning`: średnia i mediana
wartości według kategorii, statystyki powrotów i wybór prywatny/biznesowy
przy dodawaniu zlecenia. Nowa migracja V24 pozostawia stare typy nieustalone.
[Zasady i weryfikacja](verification/2026-11-22-customer-insights-test.md).
Wdrożenie wymaga backupu i przebudowy obrazu, z zachowaniem istniejącej bazy,
zewnętrznego wolumenu i `.local`. Dotychczasowych tagów nie przesuwano.

## Poprzedni TEST — źródła klientów

`nclean-v0.1-test.5`, gałąź `feat/customer-source-statistics`: słupki liczby
klientów, zleceń i przychodu według źródła pozyskania w Podsumowaniu.
Wspólny okres i tryb Plan/Wykonanie; bez nowych migracji i zmian danych.
[Zasady i weryfikacja](verification/2026-11-22-customer-sources-test.md).
Wdrożenie wymaga przebudowy obrazu aplikacji z zachowaniem bazy i `.local`.
Istniejące tagi TEST/FINAL pozostają niezmienione.

## Poprzedni TEST — standardy zleceń i godziny sprzętu

`nclean-v0.1-test.4`, gałąź `feat/job-equipment-usage`: obowiązkowa notatka
przy dodawaniu, opcjonalne imię, domyślna chemia i czas oraz szacowane godziny
sprzętu ze zleceń. Nowa migracja V23; istniejące tagi pozostają niezmienione.
[Zasady i weryfikacja](verification/2026-11-21-job-defaults-test.md).
Wdrożenie wymaga backupu oraz przebudowy obrazu z zachowaniem obecnego
podłączenia bazy, wolumenu i `.local`. Nie wykonano wdrożenia na komputerze właściciela.

## Poprzedni TEST — amortyzacja ekonomiczna

`nclean-v0.1-test.3`, gałąź `feat/economic-depreciation`: parametry i historia
amortyzacji sprzętu oraz opcjonalne uwzględnienie jej w Podsumowaniu i na ekranie
DZISIAJ. Nowa migracja V22; bez zapisywania dodatkowych wydatków.
[Zasady i weryfikacja](verification/2026-11-20-depreciation-test.md).
Poprzednie TEST/FINAL pozostają niezmienne. Instalacja PC wymaga backupu
oraz przebudowy obrazu z wybranego taga, z zachowaniem istniejącej bazy i `.local`.
To wydanie nie dodaje jeszcze automatycznego naliczania godzin ze zleceń.

## Kolejne wydanie TEST — sprzęt i serwis

`nclean-v0.1-test.2` rozszerza FINAL o działającą kartotekę sprzętu i pojazdów,
niezależne liczniki, historię i przeglądy na ekranie DZISIAJ oraz u ncLEANKA.
Gałąź: `feat/equipment-maintenance-test`. Nowa migracja: V21.
[Zakres i weryfikacja](verification/2026-11-19-equipment-test.md).

Poprzednie tagi TEST/FINAL i gałąź backupowa pozostają bez zmian. Nowy TEST
nie oznacza wdrożenia na komputerze właściciela. Aktualizacja istniejącej instalacji
wymaga backupu i przebudowy obrazu z tego taga, przy zachowaniu rzeczywistego
podłączenia PostgreSQL, dotychczasowego wolumenu i `.local`, zgodnie z procedurą poniżej.
Nie uruchamiaj inicjalizacji ani środowiska QA na bazie właściciela.

## Oznaczenia pierwszego wydania

| Etap | Nieprzesuwany tag Git | Commit |
| --- | --- | --- |
| TEST | `nclean-v0.1-test.6` | `c1a2a9ba134ac5d3b5eedd86c227069f907c0376` |
| FINAL (historyczny) | `nclean-v0.1-final.1` | Odczytaj przez `git rev-parse 'nclean-v0.1-final.1^{commit}'` |

TEST jest istniejącym commitem sprawdzonym w poprzednim zadaniu: 192 testy bez błędów i pominięć oraz rzeczywista aplikacja w Chromium przy 360, 390, 768 i 1440 px. Wyniki: [raport weryfikacji](verification/2026-11-17-proste-rozliczenia.md).

FINAL tej pary zawiera **ten sam kod aplikacji, migracje i konfigurację kontenerów**. Dodaje instrukcję wydawania i aktualizacji. Nie tworzymy kolejnego sztucznego commita TEST ani nie usuwamy starej historii. Kolejne zmiany po tym wydaniu otrzymają własny punkt kontrolny i nowe oznaczenie wydania; tagi powyżej nie będą przesuwane.

Tag oznacza źródła w GitHubie, nie opublikowany obraz kontenera. Obraz trzeba zbudować z wybranego commita. Obrazy można nazwać SHA przez `CRM_PC_VERSION`; sama ta zmienna nie pobiera kodu.

## Co dzieje się z wpisanymi danymi

TEST nie jest demonstracją ani profilem testowym Springa. Działa normalna aplikacja z logowaniem, OWNER, CSRF i PostgreSQL. Można zapisywać zlecenia, zdjęcia, klientów, tankowania i pozostałe dane. Nie uruchamiaj skryptów QA ani profili DEMO/test na bazie właściciela.

W obsługiwanej instalacji `compose.pc.yaml` dane znajdują się w zewnętrznym wolumenie **`ncleancrm-pc-data`**. Hasła i konfiguracja są w istniejącym `.local`. Przejście z TEST na FINAL w **tej samej instalacji**, z zachowaniem wolumenu i sekretów, pozostawia wszystkie wpisy, także wprowadzone podczas TEST. Git nie przenosi ani nie czyści bazy. Fikcyjne wpisy nie znikną samodzielnie po przejściu na FINAL.

Ta zasada nie przenosi automatycznie danych ze starego `compose.yaml`, innego wolumenu ani innego komputera. Jeżeli obecna instalacja używa innego podłączenia bazy, najpierw ustal je i wykonaj backup. Nie uruchamiaj nowej pustej bazy jako zastępstwa; migracja jest opisana w [DOCKER-PC.md](DOCKER-PC.md#przeniesienie-danych-z-dotychczasowego-lokalnego-developmentu).

Nie usuwaj wolumenu, `.local`, bazy ani kontenera bazy jako sposobu aktualizacji. Nie używaj `down -v`, `volume prune`, `system prune --volumes`, resetu Docker Desktop, `git clean -fdx` ani ponownej inicjalizacji sekretów. Backup kopiuj również poza dysk komputera.

## Aktualizacja istniejącej instalacji PC przez PowerShell

Najnowsze opublikowane wydanie aplikacji to TEST.6 (`c1a2a9b`); TEST.1 i FINAL.1 są historyczne. Poprawki audytu są na `fix/audit-test6`; nie utworzono ani nie przesunięto tagu wydania. Dokładny commit poprawek podano w podsumowaniu zadania. Wybierz świadomie ten commit, a nie starsze przykłady aktualizacji.

Wykonuj w **dotychczasowym katalogu instalacji** z oryginalnym `.local`. Sprawdź `git status --short` i zachowaj własne zmiany. Ustal, czy istniejący PostgreSQL rzeczywiście używa `ncleancrm-pc-data` i bazy `nclean_crm`. Nowy skrypt `Update` odrzuca inną konfigurację zamiast podłączać pusty wolumen.

Najpierw wykonaj backup narzędziem obecnie działającej instalacji:

```powershell
.\scripts\CRM-PC.ps1 -Action Backup
```

Sprawdź powodzenie i obecność `.dump` oraz `.sha256`; skopiuj je na inny nośnik. Jeżeli obecna instalacja nie ma tego narzędzia, użyj jej istniejącej procedury backupu. Nie inicjalizuj jej ponownie.

Wybór dokładnego commita z gałęzi poprawek (przed aktualizacją porównaj wyświetlony SHA z podsumowaniem zadania):

```powershell
git fetch origin
$targetSha = (git rev-parse origin/fix/audit-test6).Trim()
Write-Host "Docelowy commit: $targetSha"
git switch --detach $targetSha
$env:CRM_PC_VERSION = (git rev-parse --short=12 HEAD).Trim()
.\scripts\CRM-PC.ps1 -Action Update
.\scripts\CRM-PC.ps1 -Action Status
```

`Update` sprawdza rzeczywisty mount istniejącej bazy i buduje obrazy, gdy poprzednia aplikacja nadal działa. Następnie zatrzymuje zapis, wykonuje kolejny backup i uruchamia przygotowane obrazy z tym samym wolumenem. Jeżeli stop/backup zawiedzie przed uruchomieniem migracji, próbuje wznowić poprzedni kontener bez jego odtwarzania. Po rozpoczęciu migracji nie cofa automatycznie kodu ani schematu.

Po starcie sprawdź logowanie, wcześniejsze zlecenie i zdjęcie, zamknięte dni oraz nowy zapis. Sam restart lub nowa etykieta starego obrazu nie aktualizują kodu. Odłączony HEAD jest zamierzony; nie wykonuj w nim `git pull`.

## Odzyskanie po nieudanej aktualizacji

Nie przechodź automatycznie do FINAL.1 ani TEST.1 na bazie z nowszymi migracjami. Nie jest to zgodny rollback. Zachowaj dump sprzed aktualizacji, obecny wolumen i sekrety. Sprawdź kopię przez `RestoreTest` do nowej bazy `nclean_restore_...`; dopiero po ustaleniu wersji schematu i zgodnego kodu opracuj właściwe odtworzenie. Brak danych po starcie oznacza konieczność sprawdzenia podłączenia bazy, nigdy ponownej inicjalizacji ani kasowania wolumenu.

Polecenia dotyczą wyłącznie istniejącej instalacji PC. Nie łącz się z produkcyjną bazą nclean.pl. Instrukcje historycznych wydań w Git nie są poleceniem cofnięcia bieżącej instalacji.

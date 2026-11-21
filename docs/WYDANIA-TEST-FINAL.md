# TEST → FINAL: wersja kodu, ta sama trwała baza

## Najnowszy TEST — standardy zleceń i godziny sprzętu

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
| TEST | `nclean-v0.1-test.1` | `9787523fa73eeaf7a7b719ba72a0401835ab19ff` |
| FINAL | `nclean-v0.1-final.1` | Odczytaj przez `git rev-parse 'nclean-v0.1-final.1^{commit}'` |

TEST jest istniejącym commitem sprawdzonym w poprzednim zadaniu: 192 testy bez błędów i pominięć oraz rzeczywista aplikacja w Chromium przy 360, 390, 768 i 1440 px. Wyniki: [raport weryfikacji](verification/2026-11-17-proste-rozliczenia.md).

FINAL tej pary zawiera **ten sam kod aplikacji, migracje i konfigurację kontenerów**. Dodaje instrukcję wydawania i aktualizacji. Nie tworzymy kolejnego sztucznego commita TEST ani nie usuwamy starej historii. Kolejne zmiany po tym wydaniu otrzymają własny punkt kontrolny i nowe oznaczenie wydania; tagi powyżej nie będą przesuwane.

Tag oznacza źródła w GitHubie, nie opublikowany obraz kontenera. Obraz trzeba zbudować z wybranego commita. Obrazy można nazwać SHA przez `CRM_PC_VERSION`; sama ta zmienna nie pobiera kodu.

## Co dzieje się z wpisanymi danymi

TEST nie jest demonstracją ani profilem testowym Springa. Działa normalna aplikacja z logowaniem, OWNER, CSRF i PostgreSQL. Można zapisywać zlecenia, zdjęcia, klientów, tankowania i pozostałe dane. Nie uruchamiaj skryptów QA ani profili DEMO/test na bazie właściciela.

W obsługiwanej instalacji `compose.pc.yaml` dane znajdują się w zewnętrznym wolumenie **`ncleancrm-pc-data`**. Hasła i konfiguracja są w istniejącym `.local`. Przejście z TEST na FINAL w **tej samej instalacji**, z zachowaniem wolumenu i sekretów, pozostawia wszystkie wpisy, także wprowadzone podczas TEST. Git nie przenosi ani nie czyści bazy. Fikcyjne wpisy nie znikną samodzielnie po przejściu na FINAL.

Ta zasada nie przenosi automatycznie danych ze starego `compose.yaml`, innego wolumenu ani innego komputera. Jeżeli obecna instalacja używa innego podłączenia bazy, najpierw ustal je i wykonaj backup. Nie uruchamiaj nowej pustej bazy jako zastępstwa; migracja jest opisana w [DOCKER-PC.md](DOCKER-PC.md#przeniesienie-danych-z-dotychczasowego-lokalnego-developmentu).

Nie usuwaj wolumenu, `.local`, bazy ani kontenera bazy jako sposobu aktualizacji. Nie używaj `down -v`, `volume prune`, `system prune --volumes`, resetu Docker Desktop, `git clean -fdx` ani ponownej inicjalizacji sekretów. Backup kopiuj również poza dysk komputera.

## Aktualizacja istniejącej instalacji PC przez PowerShell

Wykonuj w **dotychczasowym katalogu instalacji**, nie w nowym klonie pozbawionym `.local`. Najpierw sprawdź `git status --short`: jeżeli są własne zmiany, zachowaj je i wyjaśnij przed zmianą wersji. Nie resetuj plików. Ustal też, czy aktualny PostgreSQL rzeczywiście korzysta z `ncleancrm-pc-data` i tej samej bazy `nclean_crm`.

Przed zmianą kodu wykonaj kopię przy użyciu narzędzia **obecnie działającej instalacji**. Dla instalacji PC jest to:

```powershell
.\scripts\CRM-PC.ps1 -Action Backup
```

Sprawdź, że polecenie zakończyło się powodzeniem i powstał `.dump` z plikiem `.sha256`; zachowaj je również poza instalacją. Jeżeli stara instalacja nie ma tego skryptu, nie kontynuuj na ślepo: użyj jej procedury backupu PostgreSQL.

Wybierz TEST:

```powershell
git fetch origin --tags
git switch --detach nclean-v0.1-test.1
git rev-parse HEAD
$env:CRM_PC_VERSION = (git rev-parse --short=12 HEAD).Trim()
.\scripts\CRM-PC.ps1 -Action Update
.\scripts\CRM-PC.ps1 -Action Status
```

`Update` zatrzymuje aplikację, robi kolejny backup, buduje obrazy z bieżącego checkoutu i odtwarza kontenery z zachowaniem wolumenu. Nie wykonuj kolejnych kroków, jeżeli którykolwiek zgłasza błąd. Po starcie zaloguj się na dotychczasowe konto i potwierdź wcześniejsze zlecenie, zdjęcie oraz zapis nowego wpisu.

Kiedy chcesz używać FINAL, ponownie sprawdź czystość katalogu, wykonaj backup i w **tym samym katalogu**:

```powershell
git fetch origin --tags
git switch --detach nclean-v0.1-final.1
git rev-parse HEAD
$env:CRM_PC_VERSION = (git rev-parse --short=12 HEAD).Trim()
.\scripts\CRM-PC.ps1 -Action Update
.\scripts\CRM-PC.ps1 -Action Status
```

Odłączenie HEAD jest tutaj zamierzone: uruchamiasz niezmienny punkt wydania. Nie wykonuj `git pull` w celu przesunięcia tak wybranego wydania. W razie przyszłych aktualizacji wybieraj nowy, jawnie wskazany tag/commit. Nie cofaj w ciemno kodu po przyszłych migracjach bazy — wcześniejsza wersja może nie obsługiwać nowszego schematu.

Gdy przejście zakończy się prawidłowo, dane wprowadzone w TEST mają być widoczne w FINAL. Brak danych oznacza, że należy zatrzymać aplikację i sprawdzić podłączenie bazy; nie inicjalizować ani nie usuwać wolumenu.

## Wiadomość do Gordona

> Zaktualizuj istniejącą instalację repozytorium nikodemwierchlewski-web/ncleancrm do taga `nclean-v0.1-test.1`, czyli commita `9787523fa73eeaf7a7b719ba72a0401835ab19ff`. Najpierw sprawdź rzeczywiste podłączenie bazy i wolumen PostgreSQL, obecny katalog instalacji oraz obecność lokalnych sekretów, bez wypisywania ich treści. Zrób sprawdzony backup dotychczasowej bazy. Jeżeli instalacja nie korzysta z `compose.pc.yaml` i `ncleancrm-pc-data`, ustal bezpieczną ścieżkę zgodnie z docs/DOCKER-PC.md; nie twórz pustej bazy zamiast obecnej. Zachowaj istniejący wolumen, bazę, zdjęcia i `.local`. Pobierz tagi, wybierz dokładny commit, ustaw `CRM_PC_VERSION` na jego skrócony SHA i zbuduj obraz z tych źródeł. Dla standardowej instalacji PC użyj scripts/CRM-PC.ps1 -Action Update. Sam restart ani samo oznaczenie starego obrazu nową nazwą nie wystarcza. Nie używaj profilu test/DEMO, poleceń czyszczenia wolumenów, resetu Git ani ponownej inicjalizacji haseł. Po uruchomieniu sprawdź zdrowie kontenerów, logowanie i wcześniejsze dane oraz podaj SHA źródeł i identyfikator obrazu uruchomionego kontenera. Późniejsze przejście na `nclean-v0.1-final.1` wykonaj z backupem i dokładnie tą samą bazą, żeby pozostały wpisy z TEST. Nie łącz się z produkcyjną bazą nclean.pl.

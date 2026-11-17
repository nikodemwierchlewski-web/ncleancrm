# ncLEAN CRM na komputerze — Docker

Wariant dla jednego właściciela na PC. Działa prawdziwy Spring Boot, Thymeleaf
i PostgreSQL, bez demonstracji HTML. Nie łączy się z bazą nclean.pl.
Publiczne wdrożenie pod `https://nclean.pl/crm/` nadal wymaga konfiguracji
administratora opisanej w [deploy](../deploy/README.md).

## Co pobrać

W GitHubie właściciel repozytorium dodaje znajomego jako współpracownika
(`Settings → Collaborators`, jeżeli ta opcja jest dostępna dla jego uprawnień).
Znajomy przyjmuje zaproszenie i używa własnego konta GitHub. Nie przesyłaj mu
swojego tokena ani hasła. Sam push nie nadaje dostępu do prywatnego repozytorium.

W PowerShell:

```powershell
git clone --branch feat/completed-means-paid https://github.com/nikodemwierchlewski-web/ncleancrm.git
cd ncleancrm
git status --short --branch
git log -1 --oneline
```

Ta gałąź zawiera wcześniejsze moduły i poprawki. Nie trzeba scalać wszystkich
gałęzi funkcjonalnych. `main` jest starszy; nie wybieraj go zamiast tego wydania.

## Wymagania

- Docker Desktop z kontenerami Linux, Compose v2 (zalecana aktualna wersja), Git.
- Dostęp do internetu przy pierwszym budowaniu. Java 17 i Maven są w obrazie;
  nie muszą być instalowane na Windows.
- Wolny port 8081. Inny port można ustawić w bieżącym PowerShell:
  `$env:CRM_HTTP_PORT = '8082'`.

## Pierwszy start na Windows

Uruchom Docker Desktop. W PowerShell, w katalogu repozytorium:

```powershell
.\scripts\CRM-PC.ps1 -Action Init
.\scripts\CRM-PC.ps1 -Action Start
```

Jeżeli Windows blokuje lokalne skrypty, w tym jednym oknie można użyć
`Set-ExecutionPolicy -Scope Process RemoteSigned`.

`Init` buduje obraz, pyta dwukrotnie o własne hasło `local-owner` i tworzy
lokalne sekrety. Minimum 12 znaków, maksimum 72 bajty UTF-8. Hasło jest wpisywane
w interaktywnym terminalu i nie jest zapisywane w postaci jawnej. Inicjalizator
odmawia nadpisania istniejących plików. Przy kompletnym wcześniejszym `.local`
skrypt wykorzystuje te same sekrety, ale **nowy wolumen PC ma osobną bazę**.
Nie kopiuje automatycznie danych z deweloperskiego Compose.

Otwórz **http://127.0.0.1:8081**. Login: **local-owner**, hasło wybrane podczas
inicjalizacji. Ten adres działa na komputerze uruchamiającym Docker Desktop.
Telefon nie uzyska dostępu przez swój własny localhost. Udostępnienie w sieci
wymaga osobnej konfiguracji dostępu i HTTPS; ta konfiguracja pozostaje lokalna.

## Gdzie są dane

| Element | Lokalizacja |
| --- | --- |
| Zlecenia, klienci, ceny, zdjęcia, rachunki i pozostałe dane | Zewnętrzny wolumen Dockera `ncleancrm-pc-data` |
| Hasła bazy i hash właściciela | `.local/postgres-admin-password`, `.local/postgres-password`, `.local/application-local.properties` |
| Kopie zapasowe | `.local/pc-backups/*.dump` oraz odpowiadające pliki `.sha256` |
| Kod i instrukcje | GitHub |

Nazwa wolumenu nie zależy od nazwy katalogu checkoutu. Compose wymaga jego
wcześniejszego utworzenia i nie usuwa go przy zwykłym odtworzeniu kontenerów.
PostgreSQL nie publikuje portu na komputer. CRM publikuje wyłącznie loopback.
OWNER, logowanie i CSRF pozostają włączone. Aplikacja działa bez roota,
z systemem plików tylko do odczytu i osobnym katalogiem tymczasowym.

**Aktualizacja kodu lub obrazu nie kasuje bazy.** Flyway stosuje brakujące
migracje, a Hibernate tylko sprawdza schemat. Nie kasuj wolumenu, `.local`,
nie resetuj Docker Desktop do ustawień fabrycznych i nie używaj czyszczenia
wolumenów jako sposobu naprawy. Wolumen nie zastępuje backupu — awaria dysku
może uszkodzić oba, jeżeli kopie są przechowywane tylko na tym komputerze.

## Codzienna obsługa i kopie

```powershell
.\scripts\CRM-PC.ps1 -Action Status
.\scripts\CRM-PC.ps1 -Action Logs
.\scripts\CRM-PC.ps1 -Action Stop
.\scripts\CRM-PC.ps1 -Action Start
.\scripts\CRM-PC.ps1 -Action Backup
```

Kopiuj plik `.dump` i jego `.sha256` na oddzielny nośnik. Zabezpiecz dostęp:
kopia zawiera dane klientów, zdjęcia i rachunki. Sekrety `.local` zachowaj
osobno w bezpiecznym miejscu, aby po awarii móc odtworzyć dostęp. Żaden z tych
plików nie powinien trafić do GitHuba. Backup nie ma jeszcze automatycznego
harmonogramu — uruchamiaj go po dniu pracy i przed aktualizacją.

## Aktualizacja bez utraty danych

Najpierw sprawdź, że nie masz własnych niezapisanych zmian:

```powershell
git status --short
git pull --ff-only origin feat/completed-means-paid
.\scripts\CRM-PC.ps1 -Action Update
```

`Update` zatrzymuje aplikację, robi sprawdzony backup bazy, buduje nowy obraz
i dopiero uruchamia kontenery. Błąd backupu lub budowania przerywa dalsze kroki.
Sekrety i wolumen pozostają te same. Otwórz CRM i potwierdź zlecenia, ceny
oraz zdjęcia. Po nieudanej migracji nie cofaj automatycznie samego obrazu;
obejrzyj logi i przygotuj naprawę lub odtworzenie kopii do osobnej instalacji.
Jeżeli aplikacja nie startuje po aktualizacji, zatrzymaj ją przez
`docker compose -f compose.pc.yaml stop app` do czasu diagnozy.

Nie zmieniaj głównej wersji PostgreSQL (np. 17 na 18) przez samą podmianę
numeru obrazu. To oddzielna operacja migracji bazy, wymagająca backupu i testu.

## Sprawdzenie odtwarzania

Podstaw nazwę pliku wypisaną przez Backup:

```powershell
.\scripts\CRM-PC.ps1 -Action RestoreTest -BackupFile 'nclean-YYYYMMDD-HHMMSS-XXXXXX.dump' -TargetDatabase nclean_restore_proba1
```

Suma SHA-256 i format archiwum są sprawdzane przed utworzeniem bazy. Odtwarzanie
działa wyłącznie do **nowej** bazy `nclean_restore_...`; istniejący cel jest
odrzucany. Nie nadpisuje codziennego `nclean_crm`. Skrypt pokazuje liczbę zleceń,
zdjęć i rachunków; pełna próba powinna obejmować też sprawdzenie danych w CRM
na odtworzonej bazie. Nie usuwa automatycznie baz testowych.

## Przeniesienie danych z dotychczasowego lokalnego developmentu

Wykonaj ten krok **przed pierwszym Start** nowej instalacji PC. Jeśli stara baza
ma prawdziwe dane, najpierw zachowaj jej osobną, sprawdzoną kopię. Nie uruchamiaj
przypadkowo skryptów starego `compose.yaml` zamiast `compose.pc.yaml`.

1. W dotychczasowej instalacji uruchom `scripts/Backup-Local.ps1`. Zatrzymaj
   dotychczasową aplikację. Nie kasuj jej bazy ani sekretów.
2. Przygotuj nową instalację PC przez `CRM-PC.ps1 -Action Init`, bez Start.
3. Skopiuj dump do `.local/pc-backups`. Utwórz obok sumę dla tego pliku:

```powershell
$backup = 'stara-baza.dump'
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath ".local/pc-backups/$backup").Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $PWD ".local/pc-backups/$backup.sha256"), "$hash  $backup`n", [Text.Encoding]::ASCII)
.\scripts\CRM-PC.ps1 -Action ImportEmpty -BackupFile $backup
.\scripts\CRM-PC.ps1 -Action Start
```

Import odmawia działania, gdy baza docelowa zawiera już tabele. Używa jednej
transakcji; nie usuwa istniejących danych ani nie oferuje opcji wymuszenia.
Jeżeli już uruchomiono CRM na nowym wolumenie, nie kasuj go dla importu —
przygotuj odrębną instalację z administratorem.

## Linux i macOS — te same kontenery bez PowerShell

Z katalogu projektu, w interaktywnym terminalu:

```sh
# Ustaw UID/GID właściciela plików w tym terminalu.
export CRM_PC_UID="$(id -u)" CRM_PC_GID="$(id -g)"
mkdir -p .local/pc-backups
chmod 700 .local .local/pc-backups
docker compose -f compose.pc.yaml build app postgres
docker compose -f compose.pc.yaml --profile tools run --rm --no-deps init
docker volume create ncleancrm-pc-data
docker compose -f compose.pc.yaml up -d --no-build --wait --wait-timeout 180
```

Jeśli sekrety już istnieją, nie uruchamiaj ponownie inicjalizatora.
Jeżeli wolumen PC istnieje, a sekretów brakuje, przywróć oryginalne sekrety
zamiast tworzyć nowe. Aktualizacja: `stop app`, `ops backup`, `build app postgres`,
`up -d --no-build --wait`, w tej kolejności, z przerwaniem po każdym błędzie.

```sh
docker compose -f compose.pc.yaml --profile tools run --rm ops backup
docker compose -f compose.pc.yaml --profile tools run --rm \
  -e BACKUP_FILE=nclean-YYYYMMDD-HHMMSS-XXXXXX.dump \
  -e RESTORE_DATABASE=nclean_restore_proba1 ops restore-test
```

## Weryfikacja wydania

Wyniki tej konfiguracji: [DOCKER-PC-TESTY.md](DOCKER-PC-TESTY.md).
Budowanie obrazu pakuje aplikację, ale nie zastępuje testów Maven.
Deweloper powinien uruchomić `./mvnw verify` i test kontenerowy
`python3 scripts/verify-docker-pc.py` na odizolowanych danych.

Instrukcje serwerowe/systemd w `deploy/` są osobnym wariantem. Nie zmieniaj
produkcyjnego CMS ani bazy nclean.pl podczas lokalnej próby.

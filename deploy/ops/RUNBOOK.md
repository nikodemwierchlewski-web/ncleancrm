# ncLEAN CRM — instalacja, aktualizacja i odzyskiwanie danych

Wariant tego pakietu: Linux, Java 17 jako usługa systemd, prywatny PostgreSQL 17
(na hoście albo w trwałym kontenerze), istniejący nginx i publiczny adres
`https://nclean.pl/crm/`. Nie zastępuj konfiguracji publicznego CMS. To instrukcja
do dopasowania przez administratora, nie potwierdzenie dostępu do jego serwera.

## 1. Przed pierwszą instalacją

1. Potwierdź system, wolny RAM/dysk, porty, certyfikat, proxy i repozytorium CMS.
   Zachowaj kopię obecnej konfiguracji. Apache wymaga równoważnej konfiguracji;
   fragmentów nginx nie wolno wklejać do Apache.
2. Wybierz konkretny commit pełnego CRM, przeczytaj raport `docs/final-audit/REPORT.md`.
   Nie zakładaj, że main jest najnowszym kandydatem.
3. W odizolowanym środowisku budowania wykonaj JDK17/Maven Wrapper `./mvnw verify`
   (Docker dla Testcontainers) oraz `node scripts/test-web-boundaries.cjs`.
   Nie uruchamiaj testów z poświadczeniami produkcji. Wynikowy JAR jest w target/.
4. Zainstaluj Java17, Python3, klienty PostgreSQL17 (`psql`, `pg_dump`, `pg_restore`,
   `createdb`), systemd i nginx. Node/Docker nie są wymagane do samego procesu Java.
5. Utwórz oddzielne konto systemowe `nclean-crm`, bez zwykłego logowania.
   Katalog `/opt/nclean-crm/releases` ma mieć właściciela root i grupę nclean-crm,
   tryb0750. Backupy: `/var/backups/nclean-crm`, tryb0700, tylko dla operatora.
6. Wybierz osobną bazę `nclean_crm`, nigdy bazę CMS. Utwórz oddzielne role runtime,
   migracji, backupu i odtworzenia. Hasła ustaw interaktywnie, nie w historii powłoki.

## 2. Baza, role i trwałość

- Proces PostgreSQL musi mieć stały katalog danych lub jawnie nazwany zewnętrzny
  wolumen. Przy Dockerze administrator tworzy wolumen tylko przy pierwszej
  instalacji i deklaruje `external: true` oraz stałe `name`. Zmiana katalogu
  wydania nie może zmieniać tożsamości bazy. Nie publikuj portu bazy w Internecie.
- `compose.yaml` w repo jest WYŁĄCZNIE lokalnym developmentem. Nie instaluj go jako
  produkcji i nie podmieniaj istniejących usług CMS.
- Runtime wymaga CONNECT, USAGE schematu i DML tabel oraz uprawnień sekwencji.
  Migrator jest właścicielem schematu/tabel i tworzy migracje. DBA powinien ustawić
  granty i ALTER DEFAULT PRIVILEGES dla obiektów tworzonych przez migratora,
  żeby nowe tabele i sekwencje były dostępne runtime i kontu backupu.
- Backup wymaga odczytu wszystkich tabel i sekwencji. Konto restore ma CREATEDB
  oraz możliwość SET ROLE do `restore_owner`; nie jest kontem aplikacji.
- Po restore `--no-acl` DBA musi odtworzyć wymagane granty i własność przed użyciem
  przywróconej bazy przez aplikację/migrator. Nie zakładaj, że sam restore daje
  poprawne uprawnienia runtime. Próba kontrolna nie przełącza aplikacji.
- W bazie są zlecenia, ceny, zużycie, notatki, zdjęcia usług i obrazy rachunków.
  Pełny dump obejmuje je wszystkie. CSV to eksport wybranych danych, nie backup.
- Nie używaj `docker compose down -v`, `docker volume prune`, Flyway clean,
  `ddl-auto=create/update` ani kasowania schematu. Starszych migracji nie edytujemy.
- Pojemność: zdjęcia do 8MiB/szt. i do40/zlecenie; planuj dysk, rozmiar backupów
  i czas odtworzenia. Aktualizacja głównej wersji PostgreSQL to osobne zadanie.

## 3. Sekrety i konfiguracja

Skopiuj szablony poza repo:

- `deploy/secrets.properties.example` → `/etc/nclean-crm/secrets.properties`,
  root:nclean-crm,0640. Ustaw właściwe URL-e JDBC, runtime/migratora oraz OWNER
  i hash BCrypt. Bez haseł w argumentach procesu. Nie zmieniaj sekretów przy update.
- `ops.json.example` → `/etc/nclean-crm/ops.json`,0600. Dostosuj ścieżki i role.
  `expected_database` musi wskazywać oddzielną bazę CRM.
- `pg_service.conf.example` → `/etc/nclean-crm/pg_service.conf`,0600.
  Połączenie backup/restore może mieć inne konto, ale ma dotyczyć tej samej bazy.
- Hasła libpq → osobny plik `/etc/nclean-crm/pgpass`,0600, zgodny z formatem
  `.pgpass`; nie do GitHuba i nie do ZIP-a. Operator ustawia poniższe zmienne:

```sh
export PGSERVICEFILE=/etc/nclean-crm/pg_service.conf
export PGPASSFILE=/etc/nclean-crm/pgpass
```

Narzędzie sprawdza `current_database()` przed backupem i aktualizacją. DBA nadal
odpowiada za host/port wskazane w service file; nazwa bazy nie dowodzi tożsamości
serwera. Nie kierować tych plików do CMS.

## 4. Pierwsze uruchomienie i proxy

1. Umieść sprawdzony JAR w `/opt/nclean-crm/releases/<COMMIT>/nclean-crm.jar`.
   Katalog0750 i plik0640, root:nclean-crm. Porównaj SHA256 po przesłaniu.
2. Utwórz symlink `/opt/nclean-crm/current` do tego katalogu. Nie nadpisuj istniejącej
   instalacji; pierwsza instalacja i aktualizacja to różne operacje.
3. Zainstaluj `deploy/nclean-crm.service.example` jako usługę `nclean-crm`.
   Zawiera profil prod, `/crm`, prywatny port i zaufane lokalne proxy.
4. Zweryfikuj `systemd-analyze verify` oraz konfigurację nginx. Dopisz fragmenty
   `nginx-crm-path.*.example` w odpowiednich kontekstach istniejącego serwera.
   Nie zmieniaj `location /` ani certyfikatu CMS. W systemd wariancie proxy łączy
   się z loopbackiem; ruch do bazy pozostaje prywatny.
5. Wykonaj `systemctl daemon-reload`, uruchom usługę i sprawdź chronione logi.
   Flyway stosuje tylko brakujące migracje; Hibernate waliduje schemat.
6. Po testach włącz start po restarcie. Sprawdź HTTPS/login, OWNER/CSRF, zdjęcia,
   wszystkie ekrany, wylogowanie i publiczną stronę CMS. Fizyczny Android:
   klawiatura, galeria/aparat, upload, przewijanie, instalacja PWA i utrata sieci.

## 5. Backup i odtworzenie kontrolne

```sh
python3 /opt/nclean-crm/tools/crm_ops.py --config /etc/nclean-crm/ops.json backup
python3 /opt/nclean-crm/tools/crm_ops.py --config /etc/nclean-crm/ops.json restore-check \
  --backup /var/backups/nclean-crm/NAZWA.dump --database nclean_restore_proba
```

Nazwa kontrolna: małe litery/cyfry/podkreślenia po `nclean_restore_`.
Istniejąca baza nie jest nadpisywana. Do odtworzenia zachowaj parę `.dump` i `.json`
(z SHA256); uszkodzona kopia jest odrzucana przed utworzeniem bazy. Restore używa
jednej transakcji i raportuje liczbę wpisów/zdjęć/rachunków oraz skróty obrazów.
Na końcu uruchom osobną instancję CRM na odtworzonej bazie i sprawdź dane oraz
zdjęcia. Kontrolną bazę usuwa później administrator po przeglądzie; skrypt nie
wykonuje DROP. Przy błędzie restore zostaje ona do diagnostyki.

Zaplanuj codzienny backup w timerze/cronie z powyższymi zmiennymi i prywatnymi
logami. Skrypt nie ustawia sam harmonogramu, retencji ani szyfrowania. Zorganizuj
kopię poza serwerem, kontrolę dostępu, alert o błędzie i regularny restore-check.
Zabezpiecz osobno konfigurację i sekrety. Lokalny wolumen nie zastępuje backupu.

## 6. Aktualizacja z obowiązkowym backupem

Przed aktualizacją zapewnij przerwę w pracy (bez równoczesnego wpisywania danych),
sprawdzony JAR, jego SHA256, changelog migracji, wolne miejsce i działający backup.
Operator wykonuje jako konto uprawnione do katalogu i systemd:

```sh
python3 /opt/nclean-crm/tools/crm_ops.py --config /etc/nclean-crm/ops.json update \
  --jar /SCIEZKA/DO/SPRAWDZONEGO.jar --release COMMIT_HEX --sha256 SHA256_PLIKU
```

Skrypt blokuje równoczesne operacje, sprawdza identyfikator i JAR, przygotowuje
niezmienne wydanie, zatrzymuje CRM, wykonuje zweryfikowany backup, atomowo
przełącza `current` i uruchamia CRM. Sprawdza lokalną stronę logowania po starcie.
Nie zmienia PostgreSQL, wolumenu ani sekretów. Wydania o innej zawartości nie są
nadpisywane. Po update wykonaj test zlecenia/zdjęcia/CSRF i sprawdź CMS.

- Błędny checksum/połączenie: nie dochodzi do przełączenia aplikacji.
- Błąd backupu: stare wydanie jest uruchamiane ponownie, bo migracja nie ruszyła.
- Błąd nowego startu/readiness: CRM zostaje zatrzymany. Kopia i oba wydania
  pozostają. Nie uruchamiamy automatycznie starego kodu na potencjalnie nowym
  schemacie i nie odtwarzamy kopii nad zapisami użytkownika.
- Awaria hosta po zatrzymaniu: operator sprawdza `current`, ostatnią kopię,
  logi i flyway_schema_history, zanim wznowi usługę. Nie ma gwarancji działania
  systemd na nieznanym serwerze bez jego odbioru.

## 7. Powrót po nieudanej aktualizacji

Zatrzymaj zapisy, zachowaj także kopię bieżącej bazy i logi. Ustal, czy migracje
ruszyły i czy istnieją nowe zapisy po backupie. Jeśli poprzedni kod jest zgodny
ze schematem, DBA może zatwierdzić przełączenie na poprzednie wydanie.
W przeciwnym razie odtwórz kopię do NOWEJ bazy, sprawdź dane/zdjęcia/granty,
uzgodnij odzyskanie ewentualnych nowszych wpisów i dopiero przełącz połączenie.
Nigdy nie kasuj starej bazy ani nie wykonuj restore --clean w ciemno.

## 8. Odbiór i zakres potwierdzenia

Próba w Codexie obejmuje prawdziwy PostgreSQL, aplikację, HTTPS z publicznym stubem
oraz rzeczywisty upgrade V15→V16 i odtworzenie zdjęć. Sterowanie procesem systemd
w próbie operacyjnej zastępuje adapter uruchamiający prawdziwą Javę; nie instalowano
systemd ani reguł na serwerze użytkownika. Konfiguracja jego CMS/proxy/VPN i backupu
poza serwerem nadal wymaga odbioru administratora. Nie ma wdrożenia produkcyjnego.

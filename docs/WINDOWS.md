# Lokalny ncLEAN CRM na Windows

Wymagane są JDK 17, Git oraz uruchomiony Docker Desktop z kontenerami Linux i Docker Compose v2. Ustaw `JAVA_HOME` na katalog JDK 17. Maven jest pobierany przez dołączony Wrapper; nie trzeba instalować go osobno. Pierwsze uruchomienie wymaga połączenia z internetem.

Otwórz PowerShell w katalogu repozytorium. Jeżeli polityka PowerShell blokuje lokalne skrypty, ustaw ją wyłącznie dla tego okna poleceniem `Set-ExecutionPolicy -Scope Process RemoteSigned`.

## Pierwsze uruchomienie

```powershell
.\scripts\Initialize-Local.ps1
.\scripts\Start-Local.ps1
```

Pierwszy skrypt poprosi dwukrotnie o własne hasło konta `local-owner`: minimum 12 znaków, maksymalnie 72 bajty UTF-8. Hasło nie jest widoczne podczas wpisywania, nie trafia do historii poleceń i nie jest zapisywane w postaci jawnej. Skrypt generuje lokalne hasła bazy, zapisuje hash BCrypt właściciela oraz konfigurację w katalogu `.local/`, który jest ignorowany przez Git. Nie dodawaj tych plików do repozytorium ani nie udostępniaj ich innym osobom.

Drugi skrypt uruchomi izolowany PostgreSQL, poczeka na jego gotowość i uruchomi CRM z profilem `local`. Argumenty startowe wymuszają lokalny adres zarówno dla aplikacji, jak i Flyway, z pierwszeństwem przed zmiennymi środowiska Spring. Baza `nclean_crm` jest dostępna tylko pod `127.0.0.1:55433`. Konto aplikacji `crm_dev` nie jest superużytkownikiem. Ta konfiguracja nie korzysta z produkcyjnej bazy nclean.pl.

Po pojawieniu się komunikatu o uruchomieniu aplikacji wejdź na **http://127.0.0.1:8081**, zaloguj się jako `local-owner` i wpisz wybrane wcześniej hasło. Kalendarz jest dostępny pod `/calendar`. Interfejs graficzny i pomoc ncLEANEK działają od razu; dodatkowy parametr wyglądu nie jest potrzebny.

## Sprawdzenie zapisu po restarcie

1. W kalendarzu kliknij konkretny dzień. W panelu zaznacz kilka obszarów i kliknij **Zapisz**. Przy dniu powinny pojawić się ich nazwy i kolory.
2. Otwórz ponownie ten dzień, zmień zaznaczenie i zapisz. Odznaczenie wszystkich obszarów i zapis usuwa przypisania tego dnia.
3. Zapisz oznaczenie i zatrzymaj aplikację przez `Ctrl+C` w PowerShell.
4. Ponownie uruchom `.\scripts\Start-Local.ps1`, zaloguj się i otwórz ten sam miesiąc. Oznaczenie powinno pozostać zapisane.

Ponowne uruchamianie inicjalizatora nie jest potrzebne: odmawia on nadpisania istniejących sekretów. Hasła i dane bazy pozostają w lokalnym środowisku.

Nazwy, kolory HEX i aktywność obszarów zmienisz na ekranie **Ustawienia → Obszary pracy**. Dezaktywacja zachowuje wcześniejsze przypisania. Krótki opis interfejsu: [KALENDARZ.md](KALENDARZ.md).

## Zatrzymywanie i testy

`Ctrl+C` zatrzymuje aplikację. Polecenie `docker compose down` zatrzymuje i usuwa kontener bazy, zachowując dane w nazwanym wolumenie. Kolejne uruchomienie `Start-Local.ps1` korzysta z tych danych. **Nie używaj `docker compose down -v`, jeśli chcesz zachować zapisane oznaczenia** — flaga `-v` usuwa wolumen z bazą.

Testy MVC bez Dockera:

```powershell
.\mvnw.cmd test
```

Pełna kompilacja, testy MVC i integracyjne na oddzielnym PostgreSQL Testcontainers:

```powershell
.\mvnw.cmd clean verify
```

Przy `verify` Docker Desktop musi działać. Baza testowa jest tworzona automatycznie w osobnym kontenerze i nie używa danych z lokalnego kalendarza.

Gdy port 55433 jest zajęty, zatrzymaj aplikację, która go używa, i ponów start. Profil `local` wymaga pliku `.local/application-local.properties`; brak tego pliku oznacza konieczność pierwszej inicjalizacji. Profil ustawia lokalny adres bazy i konto `crm_dev`. Skrypt odrzuca zewnętrzne `SPRING_DATASOURCE_URL` oraz osobne `SPRING_FLYWAY_URL`. Jeśli masz ustawione zmienne `CRM_DB_PASSWORD`, `CRM_ADMIN_*` lub inne `SPRING_DATASOURCE_*` z innego środowiska, usuń te nadpisania z bieżącego terminala przed startem — mogą zastąpić lokalne hasła i dane logowania.

Skrypty PowerShell przygotowano dla Windows; ich uruchomienie na rzeczywistym Windows wymaga osobnej weryfikacji. Wyniki testów wykonanych w środowisku roboczym są opisane w Pull Request.

## Wersja 0.1: dzień pracy i migracja

Po uruchomieniu dotychczasowymi skryptami wejdź na `http://127.0.0.1:8081`. Po zalogowaniu startuje **Kalendarz**. Flyway zastosuje brakujące migracje V1–V13; nie kasuj wolumenu ani migracji. Wykonaj kopię przed aktualizacją. Obsługa: [WERSJA-0.1.md](WERSJA-0.1.md).

## Kopia zapasowa i odtworzenie do osobnej bazy

Przy działającym lokalnym PostgreSQL, z katalogu projektu:

```powershell
.\scripts\Backup-Local.ps1
# Lub wskaż nowy plik poza repozytorium:
.\scripts\Backup-Local.ps1 -Destination 'D:\KopieCRM\nclean-2026-11-11.dump'
```

Skrypt używa `pg_dump` z lokalnego kontenera Compose i kopiuje plik przez `docker compose cp`, nie przekierowuje binarnego strumienia przez PowerShell. Hasło nie jest podawane w argumentach ani zapisywane w Git. Backup zawiera bazę, audyt, wpłaty i prywatne zdjęcia. Istniejący plik nie jest nadpisywany. Skrypt wypisuje SHA-256. Przechowuj kopie z ograniczonym dostępem, również na osobnym nośniku; nie polegaj na pojedynczym pliku na dysku aplikacji.

Próba odtworzenia wymaga nowej nazwy zaczynającej się od `nclean_restore_`:

```powershell
.\scripts\Restore-Local-Test.ps1 -Backup 'D:\KopieCRM\nclean-2026-11-11.dump' -TargetDatabase nclean_restore_proba1
```

Skrypt tworzy osobną bazę dla `crm_dev`. Istniejący cel jest odrzucany; nie używa `--clean` i nigdy nie nadpisuje `nclean_crm`. Niepełne odtworzenie pozostawia osobny cel do diagnostyki. Po sukcesie pokazuje liczbę zleceń, wpłat i zdjęć.

Aby sprawdzić odtworzoną bazę w aplikacji, uruchom **oddzielny proces** na innym porcie (zachowując lokalne sekrety i tę samą nazwę bazy w obu adresach):

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local' '-Dspring-boot.run.arguments=--server.port=8082 --spring.datasource.url=jdbc:postgresql://127.0.0.1:55433/nclean_restore_proba1 --spring.flyway.url=jdbc:postgresql://127.0.0.1:55433/nclean_restore_proba1'
```

Otwórz `http://127.0.0.1:8082`, sprawdź klienta, zlecenie, wpłaty, pomiary i zdjęcie. Nie zmieniaj konfiguracji zwykłego `Start-Local.ps1`, aby przypadkowo nie przełączyć codziennej pracy na kopię. Zatrzymanie procesu przez Ctrl+C zachowuje obie bazy.

Test automatyczny w środowisku Linux potwierdza działanie równoważnych poleceń `pg_dump/pg_restore` oraz start aplikacji na odtworzonej bazie. Skrypty PowerShell nie były wykonywane w tej sesji, ponieważ nie ma runtime PowerShell/Windows. To ograniczenie weryfikacji, a nie deklaracja testu na Windowsie.

## PWA na telefonie

W obsługiwanej przeglądarce opcja instalacji może pojawić się w Więcej. Dostęp z telefonu do adresu LAN po zwykłym HTTP nie zapewnia wymaganego bezpiecznego kontekstu. Zdalne wdrożenie wymaga HTTPS z zaufanym certyfikatem, zabezpieczonego dostępu i osobnego przygotowania serwera. Nie wyłączaj logowania/CSRF ani nie wystawiaj lokalnego PostgreSQL do internetu. PWA nie zapisuje danych CRM offline.

## Aktualizacja sprawdzonej wersji

Zatrzymaj poprzednią aplikację przez `Ctrl+C` przed budowaniem i upewnij się, że pracujesz na gałęzi zawierającej ostatnie moduły. Samo pobranie starego `main` nie dodaje funkcji z niescalonych gałęzi. Nie nadpisuj pliku JAR używanego przez działającą JVM.

```powershell
.\mvnw.cmd clean verify
.\scripts\Start-Local.ps1
```

Nie kasuj `.local` ani wolumenu bazy. Instrukcja diagnozy, identyfikacji artefaktu i testów przeglądarkowych: [DIAGNOZA-I-NAPRAWY.md](DIAGNOZA-I-NAPRAWY.md).

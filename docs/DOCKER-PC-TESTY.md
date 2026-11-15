# Docker PC — rzeczywiste wyniki weryfikacji

Data: **15 listopada 2026**. Gałąź: `feat/docker-pc-deployment`.
Pełny dotychczasowy CRM pochodzi z `38d47f7`; ten etap dodaje opakowanie
kontenerowe, konfigurację połączenia Flyway i instrukcję administratora.
Nie zmienia migracji V1–V19 ani reguł biznesowych.

## Wykonane próby

| Próba | Wynik |
| --- | --- |
| Maven Wrapper `verify`, Java 17 | **PASS: 66 testów jednostkowych/MVC/architektury i 119 PostgreSQL IT, 0 błędów, 0 pominięć** |
| `node scripts/test-web-boundaries.cjs` | **PASS** |
| Budowanie aplikacji i obrazu PostgreSQL | **PASS** |
| Inicjalizator w kontenerze, interaktywne podanie hasła i odmowa nadpisania sekretów | **PASS** |
| Nowy wolumen, gotowość bazy i aplikacji, wszystkie 19 migracji | **PASS** |
| Prawdziwe logowanie, przekierowanie niezalogowanego, odmowa POST bez CSRF | **PASS** |
| Kalendarz, dashboard, klienci i ustawienia przy 360, 390, 768 i 1440 px, brak poziomego przewijania | **PASS** |
| Zapis zlecenia i zdjęcia przez UI | **PASS** |
| Restart oraz wymuszone odtworzenie kontenerów, ponowne logowanie i pobranie zdjęcia | **PASS** |
| Zgodność odcisków tabel i zawartości binarnych po restarcie oraz odtworzeniu kontenerów | **PASS** |
| Backup i odtworzenie do nowej bazy, zgodność odcisków danych | **PASS** |
| Import istniejącego dumpa do pustej instalacji PC i start CRM na tych danych | **PASS** |
| Odmowa odtworzenia nad istniejącym celem, importu do niepustego CRM i użycia uszkodzonej kopii | **PASS** |
| Zewnętrzne wolumeny pozostają po `compose down` | **PASS** |
| Baza bez publikowanego portu, CRM na loopback, system plików aplikacji tylko do odczytu | **PASS** |

Próba kontenerowa: `python3 scripts/verify-docker-pc.py`. Tworzy losowe,
odizolowane projekty/wolumeny, osobne sekrety i syntetycznego klienta. Zdjęcie
jest wgrywane przez formularz; dodatkowy binarny rachunek jest fixture SQL.
Porównywane tabele obejmują migracje, obszary/przypisania, klientów, zlecenia,
pozycje usług, zdjęcia i rachunki. Po teście usuwane są wyłącznie zasoby
utworzone przez tę próbę, z losowymi nazwami `ncleancrm-pc-qa-...`.

Końcowy log:

```text
PASS interactive initializer and refusal to overwrite secrets
PASS login, CSRF, four widths, order/photo save, restart and forced recreation with binary data intact
PASS backup/restore binary fingerprints, import into empty PC, existing target/import/corruption refusal and external volumes survive down
```

## Znalezione i poprawione błędy

- Skrypt inicjalizacji PostgreSQL nie był czytelny dla użytkownika `postgres`
  przy uprawnieniach hosta 0600. Wariant PC kopiuje skrypty do własnego obrazu
  z jawnymi uprawnieniami, zamiast polegać na bind mountach kodu z hosta.
- Osobne URL i użytkownik Flyway wymagały jawnego przekazania hasła.
  Profil `docker` pobiera je z tego samego prywatnego pliku co datasource.
- Budowanie Wrappera wymaga `unzip`, aby sprawdzić przypiętą sumę ZIP zamiast
  pobierać inny format archiwum z inną sumą. Zależność jest w etapie build.

Po poprawkach ponownie wykonano pełną próbę kontenerową opisaną wyżej.

## Ograniczenia

Testy wykonano na **Linux**, Docker 28.4.0, Compose 2.40.3 i Chromium/Playwright
1.62.0. Nie wykonano skryptu `CRM-PC.ps1` na fizycznym Windows/Docker Desktop.
U znajomego trzeba potwierdzić pierwszy start i zapisy na jego komputerze.

Środowisko chmurowe wymaga proxy i własnych certyfikatów. Przy budowaniu użyto
tymczasowej kopii Dockerfile z montowaniem ustawień Mavena i magazynów zaufania
przez sekrety BuildKit oraz rozwiązaniem nazwy tego samego proxy. Nie wyłączano
TLS i nie zapisano konfiguracji proxy ani lokalnych sekretów w repozytorium.
Polecenia kompilacji i zawartość obrazów aplikacji odpowiadają konfiguracji PC.

Próba odtworzenia kontenerów dotyczy bieżącego wydania i schematu V19. Nie jest
gwarancją poprawności przyszłych migracji — każde wydanie wymaga ponownej próby
i backupu. Nie wykonano wdrożenia na serwerze nclean.pl ani testu publicznego
HTTPS. Ta konfiguracja udostępnia CRM lokalnie na komputerze.

Instrukcja startu, aktualizacji i przenoszenia danych: [DOCKER-PC.md](DOCKER-PC.md).

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

Po pojawieniu się komunikatu o uruchomieniu aplikacji wejdź na **http://127.0.0.1:8081**, zaloguj się jako `local-owner` i wpisz wybrane wcześniej hasło. Kalendarz jest dostępny pod `/calendar`.

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

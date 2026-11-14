# Wdrożenie ncLEAN CRM pod https://nclean.pl/crm/

Adres docelowy jest bez subdomeny. Bieżący kandydat obsługuje prefix `/crm`:
HTML/JS/CSS/JSON, login, cookies, zdjęcia, eksporty i izolowane PWA. Zaliczył
lokalną próbę HTTPS z publicznym stubem. **Nie wdrożono go na serwer użytkownika**
i nie zweryfikowano aktualnego CMS, systemu, proxy ani dostępu VPN.

Pełna instrukcja: **[RUNBOOK operatora](ops/RUNBOOK.md)**.
Wyniki testów i ograniczenia: [audyt](../docs/final-audit/REPORT.md).
[Poprzedni negatywny audyt ścieżek](SAME-DOMAIN-AUDIT.md) jest historycznym
uzasadnieniem wykonanych poprawek, nie aktualnym wynikiem próby.

## Układ

- Publiczne `/` i istniejące adresy: dotychczasowy CMS.
- `/crm/`: odrębny proces Spring Boot i osobna baza PostgreSQL.
- Przykład systemd: Java17 na prywatnym `127.0.0.1:8081`, context-path `/crm`.
- Sesja `NCLEAN_CRM_SESSION`, Path `/crm`, Secure/HttpOnly/SameSite=Strict.
- Zdjęcia i rachunki są w PostgreSQL razem z danymi operacyjnymi.
- Źródłem wydania jest sprawdzony commit/gałąź pełnego CRM, nie automatycznie main.

## Materiały administratora

| Plik | Zastosowanie |
| --- | --- |
| `nclean-crm.service.example` | Konto systemowe, start po restarcie, prywatne uruchomienie w `/crm` |
| `secrets.properties.example` | Wzór konfiguracji poza checkoutem, bez prawdziwych haseł |
| `nginx-crm-path.http.conf.example` | Fragment kontekstu http: logi i limit logowania |
| `nginx-crm-path.locations.conf.example` | Fragment ISTNIEJĄCEGO vhosta HTTPS, zachowujący CMS |
| `nginx-crm-path-proxy.conf.example` | Zachowanie prefixu i kontrolowane nagłówki proxy |
| `ops/crm_ops.py` | Backup, restore do nowej bazy, aktualizacja JAR-a z obowiązkowym backupem |
| `ops/ops.json.example`, `ops/pg_service.conf.example` | Jawna tożsamość instalacji i połączeń, do uzupełnienia poza Git |

Lokalny Compose pozostaje developmentem. Administrator może użyć trwałego
PostgreSQL w Dockerze lub osobnej usługi bazy. Wariant aplikacji z tej instrukcji
to systemd; nie wymaga Dockera na telefonie ani zmiany CMS na kontenery.
Starsze przykłady subdomenowe pozostają archiwalne i nie są konfiguracją docelową.

## Warunki przełączenia na serwerze

1. Osobna baza/role i trwały katalog lub zewnętrzny wolumen o stałej nazwie.
2. Potwierdzone proxy, certyfikat i prywatne porty; żadnego podmieniania CMS.
3. Zweryfikowane migracje V1–V16, granty i prywatne sekrety.
4. Backup ze zdjęciami, restore do nowej bazy i kontrola danych przez aplikację.
5. Próba restartu, aktualizacji i awarii; procedura powrotu bez kasowania bazy.
6. Fizyczny Android, bieżący skan zależności i odbiór administratora.
7. Harmonogram kopii poza serwerem, retencja, kontrola dysku/logów i alerty.

Nie używaj `down -v`, Flyway clean, `ddl-auto=create/update` ani automatycznego
restore nad działającą bazą. Po nieudanej migracji cofnięcie samego kodu może
nie być bezpieczne. Szczegóły i polecenia są w RUNBOOK.

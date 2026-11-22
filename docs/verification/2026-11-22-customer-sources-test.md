# TEST — źródła klientów w Podsumowaniu

Baza: `nclean-v0.1-test.4` (`c9b0c56`). Gałąź:
`feat/customer-source-statistics`. Nowy tag: `nclean-v0.1-test.5`.

- Trzy przełączane wykresy słupkowe: różni klienci, zlecenia, przychód;
  wartości i udziały procentowe. Wspólny okres dzień/tydzień/miesiąc i tryb
  Plan/Wykonanie z istniejącego Podsumowania.
- Klient jest liczony raz według kartoteki, także gdy powraca. Przychód
  wykonania to kwota końcowa (przy jej braku dotychczasowa wycena), według
  dnia usługi. Anulowane i usunięte wizyty są pomijane.
- Źródło pochodzi z obecnej kartoteki. Jego korekta zmienia również podział
  dawnych wizyt. Braki pozostają „Nieustalone”; wizyty bez kartoteki zwiększają
  liczbę zleceń i przychód, bez zgadywania liczby różnych klientów.
- Tylko odczyt. Bez nowych migracji, zmian V1–V23, danych, zdjęć, cen,
  reguł paliwa, konfiguracji Dockera i `.local`.

## Weryfikacja

Java 17 / Maven: końcowe wyniki 74 testów jednostkowych, architektury
oraz uprawnień i 161 testów PostgreSQL — łącznie **235 poprawnych**,
bez pominięć. W pełnym `verify` wystąpił konflikt portu testowego Dockera;
ponowiony zestaw `JobDefaultsPostgresIT` (15 testów) przeszedł poprawnie.
Nowe 8 testów sprawdza m.in. wielokrotne wizyty i pozycje, granice okresu,
Plan/Wykonanie, niepełne dane, archiwalne kartoteki, korektę źródła,
usunięcie/przywrócenie i zgodność przychodu z podsumowaniem.

Chromium: `/` i `/crm`, po 36 widoków (360/390/768/1440 px, trzy miary,
wykonanie/plan/pusty okres), przełączanie okresów, korekta źródła, klawiatura,
ograniczenie animacji, odczyt bez JS i ochrona logowania — poprawnie,
bez błędów JavaScript. Wyłącznie izolowany PostgreSQL i syntetyczne dane.

Nie wykonano wdrożenia. Aktualizacja PC wymaga przebudowy obrazu aplikacji
z zachowaniem obecnego podłączenia bazy, wolumenu i `.local`.

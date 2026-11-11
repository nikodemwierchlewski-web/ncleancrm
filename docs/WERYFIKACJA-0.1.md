# Odbiór wersji 0.1 — 11.11.2026

## Repozytorium i dane

Sprawdzono `main` oraz zależny ciąg gałęzi do `feat/fuel-log-receipts` (`658240a`). Gałąź `feat/crm-v01-day-workflow` zachowuje wcześniejsze implementacje i dodaje migrację V9. Nie scalano do `main`, nie zmieniano migracji V1–V8, produkcyjnego CMS ani produkcyjnej bazy nclean.pl. Stan istniejących danych nie jest resetowany.

## Automatyczne testy

Java 17, Maven Wrapper 3.9.11, `verify`, z zapisywalnym osobnym cache Maven w trybie offline:

- **20 testów jednostkowych/MVC**: role, CSRF, dostęp do danych i eksportu, publiczne zasoby PWA bez danych klientów, neutralizacja CSV oraz dotychczasowe reguły bezpieczeństwa.
- **39 testów integracyjnych z prawdziwym PostgreSQL/Testcontainers**: migracje i aktualizacja starego schematu, zachowanie danych, poprzednie funkcje kalendarza/chemii/paliwa, cena końcowa niezależna od wyceny, częściowe wpłaty i audyt korekty, tokeny/konflikty, jeden pomiar czasu i restart, podział pakietu oraz jawne potwierdzanie chemii, niezależność dnia wpłaty od dnia usługi, renderowanie nowych ekranów i eksportu.
- Razem **59 testów: 0 porażek, 0 błędów, 0 pominiętych**. Raporty XML: `target/surefire-reports`, `target/failsafe-reports`.

Pierwszy przebieg wykrył błąd warunku Thymeleaf dla nieobecnych parametrów zapisu; został poprawiony i cały zestaw przeszedł ponownie. Test przeglądarkowy wykrył też kolizję nazwy pola `action` z właściwością formularza, utratę kotwic w przekierowaniach fetch oraz błąd składni service workera. Wszystkie zostały poprawione i sprawdzone w aplikacji.

## Przeglądarka — rzeczywista aplikacja

Chromium/Playwright, osobna tymczasowa baza PostgreSQL z syntetycznymi danymi:

1. Dodanie nowego klienta i usług z dwóch kategorii, zmiana sugerowanej ceny: wycena 160 zł.
2. Powracający klient podpowiedziany przez wyszukiwanie; adres wizyty zmieniony bez zmiany kartoteki.
3. Osiem zleceń jednego dnia i dwa obszary pracy.
4. Pomiar czasu uruchomiony, strona odświeżona, pomiar zatrzymany; znacznik czasu przetrwał odświeżenie.
5. Cena końcowa 200 zł, oryginalna wycena nadal 160 zł, czas 90 min, jawny podział kwot i czasu na dwie pozycje.
6. Potwierdzone rzeczywiste zużycie chemii dla obu pozycji: koszt łącznie 5 zł.
7. Nadpłata odrzucona z zachowaniem pól; wpłata 50 zł w dniu usługi i 150 zł następnego dnia. Należność 0 zł; przychód dnia usługi 200 zł, wpłaty tego dnia 50 zł.
8. Awaria połączenia podczas zapisu wykonania: notatka pozostała w formularzu, ponowienie zapisane.
9. Zmiana terminu oraz anulowanie przez istniejący formularz.
10. Ponowne umówienie klienta: bieżąca cena 90 zł zamiast kopiowania starej ceny, bez utworzenia rezerwacji przed potwierdzeniem i zapisem.
11. Licznik, agregat oraz zdjęcie rachunku; przerwanie uploadu i ponowienie. Rachunek 60 zł pokazany oddzielnie, nie odjęty ponownie.
12. Wynik kontrolny: 200 − 5 − 63,75 − 10 = **121,25 zł**. Rzeczywisty czas 90 min, przychód/h i wynik/h dostępne przy kompletnych danych.
13. Eksporty CSV autoryzowane. Manifest poprawny, service worker aktywny, `CacheStorage` pusty — bez prywatnych danych offline.
14. Widoki Dzisiaj, kalendarz, wykonanie, podsumowanie i klient sprawdzone przy **360, 390, 768 i 1440 px**: brak poziomego przewijania, dolna nawigacja na telefonie z polami co najmniej 44 px, brak błędów JavaScript.

To emulowane szerokości w Chromium, nie test na fizycznym iPhonie/Androidzie. Instalowalność PWA na telefonie użytkownika zależy od HTTPS i osiągalnego wdrożenia; nie została zadeklarowana jako przetestowana.

## Restart i kopia zapasowa

Po zatrzymaniu i ponownym uruchomieniu aplikacji odczytano wpłaty i zdjęcie. Następnie wykonano pełny `pg_dump -Fc --no-owner --no-acl` wyłącznie testowej bazy i `pg_restore --single-transaction --exit-on-error --role=crm_dev` do nowej osobnej bazy `nclean_restore_*`.

Porównano liczby zleceń, sumę wpłat, pomiary chemii, odczyty, tankowania i MD5 binarnej zawartości zdjęcia. Aplikacja uruchomiła się również na odtworzonej bazie, przeszła logowanie i wyświetliła plan oraz autoryzowane zdjęcie. Obie tymczasowe bazy i plik dump usunięto po teście. Zwykła lokalna baza nie była nadpisywana.

Polecenia PostgreSQL i scenariusz odtworzenia zostały wykonane w Linux. Skrypty `.ps1` przygotowano do Windows z bezpiecznymi ograniczeniami celu, lecz nie wykonano ich w PowerShell — runtime jest niedostępny. Nie udajemy testu tego systemu.

## Ograniczenia

- Podgląd współdzielonego serwera/portów w tej sesji jest niedostępny (`capabilities: []`). Zrzuty pochodzą z rzeczywistej aplikacji sprawdzonej w Chromium, nie z demonstracyjnego HTML.
- Brak zapisu offline, kolejki synchronizacji i trwałego odzyskiwania niezapisanych formularzy po zamknięciu karty.
- Brak księgowości/podatków/faktur, nadpłat i automatycznego podziału kosztów wspólnych na usługi. Korekta wpłaty jest jawnym zapisem przeciwnym, a nie integracją bankową.
- Wcześniejsze wykonane zlecenia wymagają potwierdzenia ceny końcowej, czasu i wpłat; migracja nie tworzy tych danych.
- Publiczne wdrożenie HTTPS, testy fizycznych telefonów i wykonanie skryptów w Windows pozostają osobnym odbiorem środowiska docelowego.

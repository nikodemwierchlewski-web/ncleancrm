# Audyt codziennej pracy i pewności zapisu ncLEAN

15.11.2026, Europe/Warsaw. Baza: `8462cb1`, gałąź `feat/nclean-daily-reliability-audit`. [Zastosowany prompt](../PROMPT-daily-reliability-audit.md).

## Werdykt

Największe znalezione problemy dotyczyły utraty niezapisanych szkiców i rozbieżności między stanem bazy a interfejsem. Naprawy wykorzystują istniejące formularze i dane. Nie dodano nowych modułów CRM, zależności, migracji ani automatycznych zapisów w tle. Ceny historyczne, PostgreSQL, OWNER i CSRF pozostają.

## Znalezione problemy i poprawki

| ID | Skutek dla właściciela | Dowód i zastosowana poprawka |
|---|---|---|
| R1 | Po zapisaniu godzin w sekcji zdjęć start pomiaru odrzucany jako konflikt innej karty | W starej wersji formularz timera zachował wersję 0, baza miała już 1. Odświeżanie używało `form.action`, przesłoniętego przez pole `action`. Wspólny `Nclean.formTarget` czyta atrybuty. Po zmianie zdjęć/godzin odświeżają się wersja i widok timera, z zachowaniem wpisanych danych wykonania. |
| R2 | Zamknięcie edytora i ponowne otwarcie usuwało niezapisany tekst | Odtworzono pusty tytuł po zamknięciu szkicu. Wspólny magazyn szkiców przechowuje rzeczywiste węzły formularza w pamięci strony, osobno dla każdego edytowanego wpisu. Zachowuje także plik rachunku. Komunikat jasno odróżnia szkic od zapisu w bazie; jest jawne „Odrzuć szkic”. |
| R3 | Zapis był w bazie, ale błąd odczytu listy zostawiał otwarty edytor i zachęcał do ponownego zapisu | W próbie stary edytor pozostał otwarty mimo jednego zapisanego wpisu w PostgreSQL. Teraz potwierdzenie zapisu zamyka edytor i czyści szkic; odświeżenie listy jest osobnym odczytem. Jego błąd daje komunikat „Dane zapisane”, nie ponawia mutacji i nie blokuje kolejnego wpisu. Numerowane odczyty zapobiegają nadpisaniu nowszej listy starszą odpowiedzią. |
| R4 | Dwa mechanizmy wysyłania formularzy i podatność na nazwy `action`, `method`, `id` | Zwykły timer i płatności działały dzięki wcześniejszej obsłudze w `app.js`; nie opisujemy ich jako ogólnie niedziałających. Usunięto nakładanie mechanizmów: `session.js` odpowiada za formularze natywne, odzyskanie sesji, CSRF, blokadę podwójnego wysłania i błędy. Moduły zachowują własne operacje AJAX. Adres, metoda i ID są czytane z atrybutów, niezależnie od nazw pól. Zachowane kotwice płatności i paliwa. |
| R5 | Zmiana listy lub opóźniony odczyt mogły utrudnić dalszą pracę po skutecznym zapisie | Zapis i odczyt nie współdzielą już blokady formularza. Test celowo wstrzymuje listę, otwiera kolejny wpis i dopiero później zwraca błąd odczytu. Nowy formularz pozostaje używalny. |

[Obserwacje starej wersji](before.json). Wprowadzono też jawne oznaczanie zmian po dodaniu/usunięciu kroku i przypomnienia oraz czyszczenie stanu szkicu dopiero po potwierdzeniu operacji. Dane prywatne nie są zapisywane w localStorage; szkice nie opuszczają bieżącej strony bez „Zapisz”.

## Ocena czterech perspektyw

**Praktyk ncLEAN:** do umawiania wystarczają termin, zakres, kontakt i adres. Notatki, źródło kontaktu i prognozy pozostają w istniejących opcjach. W historycznej usłudze można pozostawić nieznanego klienta; brak danych nie staje się fikcyjną osobą ani zerowym zużyciem. Zakończenie i dokumentacja są osobnym krokiem. Timer i zdjęcia muszą dawać spójny czas — to naprawiono.

**UX LEAN:** zachowano plan dnia przed analizami i zwijane opcje. Zamknięcie szkicu pozwala wrócić do niego w tej samej stronie. Ponowne wpisywanie danych po utracie sieci nie jest konieczne. Po skutecznym zapisie można od razu otworzyć następny wpis, nawet gdy lista odpowiada wolno. Powiadomienie o szkicu pojawia się dopiero przy przywróceniu.

**Tester niezawodności:** sprawdzono awarię przed wysłaniem, utratę odpowiedzi po rzeczywistym zapisie, wygaśnięcie sesji i ponowienie po zalogowaniu w drugiej karcie, konflikty, restart i odtworzenie. Ponowienie tej samej wpłaty z tym samym tokenem pozostawiło dwie wpłaty 20 + 10, zamiast trzech. Odczytanie UI nie zastępowało kontroli bazy/API.

**Developer:** jeden transport formularzy natywnych, osobny wspólny komponent szkiców, zachowane modułowe operacje AJAX. Bez nowych bibliotek i bez zmian backendu/migracji. Pozostawiono transakcje, wersjonowanie i identyfikatory ponowień. CSV pozostaje eksportem wybranych danych, nie pełną kopią bazy.

## Zakres sprawdzenia

- Kalendarz/obszary, nowy i powracający klient, kilka kategorii i zmienna cena, wpisy historyczne, wyszukiwanie, cennik/chemia i zachowanie cen dawnych usług.
- Wykonanie, zdjęcia, ręczne godziny, timer, wpłaty/korekty, zamknięcie dnia i późniejsze poprawki.
- Dzisiaj, zakresy i koło, koszty, rachunki, paliwo/agregat, notatki/cele i przypomnienia.
- OWNER/CSRF, `/crm`, odzyskanie sesji, aktualizacja JAR, backup i odtworzenie do nowej bazy.

Nie zmieniano reguł finansowych: wycena ≠ potwierdzona cena, przychód ≠ wpłata, zakup paliwa/chemii ≠ ponowne naliczenie ich zużycia. Brakujące dane nadal oznaczają wynik orientacyjny.

## Wyniki weryfikacji

- Maven Wrapper `verify`: **185 testów**, 66 jednostkowych/MVC/architektury oraz 119 na rzeczywistym PostgreSQL 17 przez Testcontainers, bez błędów i pominięć.
- Przeglądarka: **360/390/768/1440 px**. Osobne regresje `scripts/browser_reliability_checks.py`: synchronizacja zdjęcia/godziny/timer, brak internetu, utracona odpowiedź po zapisie i idempotentne ponowienie, szkic/odrzucenie/przełączenie edytorów, zapis przy wstrzymanym/błędnym odczycie listy, plik rachunku po przywróceniu szkicu.
- Rzeczywisty `/crm`: utrata sesji bez utraty pól, logowanie w drugiej karcie oraz świadome ponowienie zakończone pojedynczym zapisem.
- Aktualizacja V19→V19, restart i restore do NOWEJ bazy: zgodność cen, notatek, binarnych zdjęć/rachunków oraz definicji i stanu przypomnień. Odrzucone: błędna suma JAR, uszkodzona kopia i istniejący cel odtworzenia. Awaria backupu zachowała stare wydanie; awaria gotowości pozostawiła usługę zatrzymaną bez niebezpiecznego rollbacku. Sterowanie systemd było adapterem testowym uruchamiającym rzeczywistą Javę. [Wyniki operacyjne](operations.json).
- axe: **81 skanów, 0 wykrytych naruszeń**, także w przywróconym szkicu. [Raport](accessibility.json).
- Node: adresowanie i formularze przy root, `/crm` i zagnieżdżonym kontekście; ochrona zakresu cache/PWA. Składnia JS/Pythona i `git diff --check`.

[Zebrane wyniki](results.json).

## Granice i dalszy etap

Szkic jest pamięcią bieżącej strony, nie zapisem serwerowym i nie gwarancją odzyskania po zamknięciu karty lub ubiciu przeglądarki przez Androida. Przed przeładowaniem istnieje ostrzeżenie o niezapisanych danych; trwały zapis następuje po „Zapisz”. Nie uruchomiono offline queue ani przechowywania danych klientów w localStorage.

Nie testowano fizycznego telefonu Android, aparatu producenta, zachowania systemu przy usypianiu ani infrastruktury docelowego serwera. Nie kontaktowano się z produkcją i nie wykonywano wdrożenia. Następny etap: kontrolowany pilotaż z kilkoma historycznymi zleceniami w prawdziwej aplikacji, próba aparatu/klawiatury na telefonie i kopia bazy po próbie. Android, push i SMS pozostają osobnym etapem.

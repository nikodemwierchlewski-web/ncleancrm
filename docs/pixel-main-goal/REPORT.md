# Pixelowy interfejs i główny cel — 15.11.2026

Zmiany w prawdziwej aplikacji, na gałęzi `feat/pixel-motion-main-goal`,
od istniejącej pełnej implementacji `e9edb78`. Kod zmiany: `6a36fec`.
Nie tworzono PR, nie scalano gałęzi i nie wdrażano na produkcję.

## Co zmieniono

- Osobny, jawny wybór głównego celu w Notatkach i celach. Przypięcie notatki
  pozostaje niezależne. Jeden cel jest chroniony blokadą zapisu i unikalnym
  indeksem w PostgreSQL. Edycja korzysta z istniejącej wersji wpisu i tokenu.
- Dashboard: karta celu z pixelowym pierścieniem, licznikiem kroków, terminem,
  następnym krokiem do odhaczenia i edycją. Kalendarz: mały, zwijany skrót.
  Usunięto powtarzający się automatyczny wybór celu z panelu osiągnięć.
- Postęp pochodzi wyłącznie z kroków. Bez kroków nie ma procentu. Odhaczenie
  ostatniego kroku nie kończy celu; potrzebne jest świadome potwierdzenie.
  Ukończony główny cel pozostaje widoczny. Ponowna edycja jego kroków wymaga
  przywrócenia do aktywnych. Archiwizacja zwalnia wybór, zachowując historię;
  przywrócenie z archiwum nie wybiera celu automatycznie.
- Autorska ilustracja 8-bit z ncLEANKIEM, delikatny ruch i pixelowe błyski,
  reakcja wykresu na zmianę postępu oraz uśmiech ncLEANKA po zapisie.
  Niebieskie gwiazdki korzystają ze wspólnego koordynatora i pojawiają się
  po potwierdzeniu ukończenia przez serwer, bez powtarzania po odświeżeniu.
  Odznaki odświeżają się również po zapisach z karty głównego celu.
- Ruch respektuje systemowe ograniczenie animacji i przełącznik aplikacji,
  ukrytą kartę i położenie poza ekranem. Rozmiar i dolna oś ncLEANKA pozostają
  stałe. Przy kolizji z kontrolką ustępuje kliknięciu bez zmiany położenia.
- Po odhaczeniu kroku fokus klawiatury przechodzi do następnej czynności.
  Błąd zapisu nie zmienia wyświetlanego postępu i pozwala ponowić żądanie.

## Obsługa

1. Otwórz **Więcej → Notatki i cele**, utwórz wpis rodzaju **Cel** i dodaj kroki.
2. Zaznacz **Główny cel** w edytorze albo **Ustaw jako główny cel** na karcie.
   Poprzedni wybór zostanie zwolniony; sam wpis pozostanie zapisany.
3. W **Podsumowaniu** odhaczaj kolejny krok. Po ostatnim potwierdź osiągnięcie.
   W kalendarzu rozwiń skrót celu, żeby wykonać te same czynności.
4. Możesz usunąć oznaczenie głównego celu, wybrać inny lub zarchiwizować wpis.

## Rzeczywista weryfikacja

| Sprawdzenie | Wynik |
|---|---|
| Maven Wrapper `verify`, Java 17 | 50 testów jednostkowych/MVC/architektury + 106 integracyjnych; 0 błędów, 0 pominiętych |
| PostgreSQL 17 / Testcontainers | V1–V17; upgrade z V16 zachowuje wszystkie kolumny dawnych memo i kroki |
| Zapis i restart | Wybór celu, postęp i historia pozostają po restarcie prawdziwej aplikacji |
| Konflikty i powtórzenia | Dwa równoczesne wybory, stara wersja, ponowienie tokenu, ponowienie ukończenia; jeden główny cel i brak powtórzonej celebracji |
| Chromium 360 / 390 / 768 / 1440 px | Zlecenia (w tym osiem wizyt), klienci, cennik, zdjęcia, koszty, zamykanie dnia, cele i nawigacja; brak poziomego przewijania |
| Nowy cel w prawdziwym UI | Jawny wybór, kroki, błąd 503 i ponowienie, fokus, ukończenie, odznaka bez przeładowania, archiwizacja, cel bez procentu, ograniczenie ruchu |
| axe | 66 skanów, 0 naruszeń; również rozwinięty cel i edytor na czterech szerokościach |
| HTTPS `/crm` | 60 kombinacji ekranu/szerokości; logowanie OWNER, CSRF, zapis celu, dane historyczne, zdjęcia, eksport, wylogowanie i izolacja PWA |
| Samodzielny HTML | Cztery szerokości; cel, kroki, ukończenie, archiwizacja, nowy cel, klienci/ustawienia i zagnieżdżony sandbox |
| Regresja HTML | Zlecenia, usuwanie/anulowanie/przywracanie, cennik, koszty, ograniczenie ruchu i skończona liczba gwiazdek |
| JavaScript i adresy | Kontrola składni; `test-web-boundaries.cjs` — PASS |

W pierwszej iteracji poprawiono separator parametrów JDBC w nowym teście
migracji oraz przesłanianie `form.action` przez pole o tej nazwie w nowym
formularzu. Test zdjęć czeka teraz na nową wersję zapisu, zamiast poprzedniego
komunikatu „Zapisano”. Kontrola axe wykryła zasłonięty przez ncLEANKA przycisk
usuwania kroku na telefonie; po poprawce ponowne 66 skanów nie wykazało naruszeń.
Test sandboxu korzysta z hosta HTTP, zachowując obie ramki bez `allow-same-origin`.

Automatyczne testy nie zastępują próby na fizycznym Androidzie ani testu czytnika
ekranu. Nie wykonywano testów obciążeniowych produkcji ani jej wdrożenia.

## Baza i aktualizacja

Nowa migracja `V17__explicit_main_goal.sql` dodaje pole i ograniczenia.
Wcześniejsze migracje pozostają bez zmian. Nie usuwa wpisów ani kroków i nie
wybiera celu za właściciela. Dane biznesowe pozostają w PostgreSQL.
Przed rzeczywistą aktualizacją zastosuj [procedurę backup/update/restore](../../deploy/ops/RUNBOOK.md).
Nie cofaj samego JAR-a bez sprawdzenia zgodności ze schematem V17.
Dotychczasowa paczka administratora w `preview/` jest archiwalna (V1–V16).

## Podgląd i ograniczenie platformy

[HTML](../../preview/nclean-pixel-main-goal.html) i [ZIP](../../preview/nclean-pixel-main-goal.zip)
powstały z 63 widoków prawdziwej aplikacji i tych samych modułów interfejsu.
Transport zapisu jest wyraźnie demonstracyjny, w pamięci karty. Odświeżenie go
resetuje. Menu `⋯` umożliwia eksport/import ćwiczenia JSON, bez zdjęć i szkiców.
**Nie służy do wprowadzania prawdziwych danych klientów.**

Rzeczywistą aplikację sprawdzono w Chromium z odizolowanymi schematami PostgreSQL,
na lokalnych portach środowiska. Nie opublikowano tych portów użytkownikowi.
Środowisko deklaruje pulpit na żądanie, ale ta sesja nie udostępnia narzędzia
przeglądarki pulpitu ani przekierowania portu. Wywołanie `open_in_codex` dla pliku
zwróciło „This app tool is unavailable through dynamic tools”. Dlatego nie
potwierdzam działającego panelu podglądu Codexa. Działającą alternatywą jest
sprawdzony HTML/ZIP, otwierany w przeglądarce z JavaScript.

[Wyniki axe](accessibility.json) · [Wyniki podglądu](preview-results.json) ·
[Metadane i sumy plików](build.json) · [Telefon](dashboard-390.png) · [Komputer](dashboard-1440.png).

Powtarzalne testy: `scripts/verify-browser.py --owner-file <prywatny-plik.json>
--pixel-tycoon`, `scripts/verify-pixel-preview.py` oraz
`scripts/verify-removal-preview.py` (ostatnie dwa z lokalnym URL syntetycznego HTML).
Nie zapisuj lokalnych sekretów ani logów startowych w repozytorium.

# Audyt domknięcia ncLEAN CRM — 13.11.2026

Perspektywy: codzienna praca właściciela ncLEAN, lean, UX/UI, dostępność, integralność danych, rozwój i uruchamianie aplikacji. To audyt techniczny i scenariuszowy, nie certyfikacja bezpieczeństwa ani dowód braku wszystkich możliwych błędów.

**Werdykt: dobra baza do domknięcia, jeszcze nie wydanie finalne.** Dotychczasowe 107 testów przechodzi, ale dodatkowe próby znalazły ścieżki nieobjęte zestawem. Brak potwierdzonego P0 w badanym zakresie; są P1 wymagające poprawy przed zamknięciem wersji.

**Decyzja właściciela z tego zadania:** Tycoon jest docelowym wyglądem całego produktu. Poprzednia zasada „klasyczny domyślnie, Tycoon tylko eksperymentalnie” przestaje obowiązywać w planie finalizacji. W audycie nie przełączano jeszcze konfiguracji aplikacji i nie wykonywano promptu naprawczego.

Baza: `ux/lean-workspace@900ea19`; kod aplikacji z `ffabed1`, późniejsze zmiany w tej bazie dotyczyły dokumentacji/paczki. Obecny main może nie zawierać całego CRM — nie wolno rozpoczynać finalizacji od starej, niepełnej wersji.

## Dowody i zakres

- Inwentaryzacja 36 plików Java i 36 szablonów; przegląd kontrolerów, kluczowych reguł zapisu, konfiguracji, migracji V1–V14, skryptów, kodu JS/CSS oraz istniejącej dokumentacji. Inwentaryzacja 73 jawnych handlerów GET/POST; nie obejmuje automatycznych endpointów Spring Security ani wszystkich wariantów parametrów.
- Świeże Maven Wrapper `clean verify`: **27 jednostkowych/MVC + 80 integracyjnych PostgreSQL/Testcontainers = 107**, zero błędów, porażek i pominięć.
- Powtórzony scenariusz rzeczywistej aplikacji na osobnym schemacie PostgreSQL: osiem wizyt, nowy/powracający klient, dwie kategorie, cena indywidualna, adres wizyty, zmiana cennika bez zmiany historii, wykonanie, zdjęcia/czas/chemia, anulowanie/przełożenie, notatki/cele, koszty i restart.
- Dodatkowe 15 widoków × 4 szerokości (360/390/768/1440): **60 wejść**, HTTP 200 i brak poziomego przewijania w próbie. Nie jest to test każdego wariantu danych.
- Dodatkowe reprodukcje API/UI opisane poniżej, w tym rzeczywista zmiana sesji i ponowne logowanie w drugiej karcie.
- axe-core 4.10.3: 30 kombinacji widoku i szerokości, w tym formularz zlecenia, logowanie i wylogowanie; po ponownej weryfikacji kontrastu w ograniczonym ruchu **10 kombinacji z naruszeniami**. 43 wskazania kontrastu obejmują powtórzenia tych samych elementów na różnych szerokościach. Wyniki incomplete nie są zaliczone jako poprawne.
- Zestaw wolumenowy: 503 zlecenia, 303 klientów w osobnym schemacie. Pojedyncze lokalne pomiary: Kalendarz 450881 B / 1752 ms; Klienci 433984 B / 1364 ms; Podsumowanie miesiąca 66492 B / 106 ms; lista /orders 859637 B / 769 ms. To obserwacje środowiska, nie SLA i nie mediana.
- Dump i restore do nowej testowej bazy: zgodne liczby rekordów i MD5 syntetycznego BLOB-u; zwykła baza nie została nadpisana. Nie uruchamiano w tej próbie aplikacji na odtworzonej bazie. Testy startu na restore były opisywane we wcześniejszych audytach i wymagają powtórzenia przy wydaniu.

Wyniki maszynowe: [final-tycoon-readiness.json](audit/final-tycoon-readiness.json). [Inwentarz handlerów](audit/final-route-inventory.json). Testowano wyłącznie dane syntetyczne i izolowane bazy; bez produkcji nclean.pl.

## Co działa i warto zachować

Nawigacja Dzisiaj / Kalendarz / Klienci / Więcej, oddzielenie konfiguracji, kompaktowe trzy liczby i jedna akcja, wspólny formularz wykonania, rozdział prognozy i pomiaru, zapisane ceny historyczne, wpłaty oddzielone od przychodu, zakupy oddzielone od zużycia, wersjonowanie, idempotencja rezerwacji/płatności, prywatne zdjęcia i ochrona OWNER/CSRF. Aktualne scenariusze potwierdziły zachowanie szkicu po503, konflikty kart, rozpoznanie czasu zdjęć i trwałość po restarcie.

W kodzie są już: transakcja tworzenia/edycji klienta razem ze źródłem, produkcyjny profil secure-cookie i zaufanego lokalnego proxy, CSP, brak cache prywatnych zdjęć/eksportów, neutralizacja formuł CSV, walidacja i ponowne kodowanie obrazów. Nie zgłaszam ich jako brakujących. Nie wykonywano pełnej analizy CVE zależności ani zewnętrznego testu penetracyjnego.

## Ustalenia do domknięcia

| ID | Priorytet | Potwierdzony stan / wymaganie | Oczekiwany rezultat |
| --- | --- | --- | --- |
| R01 | P1 dane | Przyszła wizyta + POST /orders/{id} z DONE: HTTP 200, baza DONE. Także stary /work/{id}/complete:302 i DONE. DayReview blokuje przyszłość, inne wejścia ją obchodzą. | Jedna reguła domenowa dla wszystkich wejść; brak przyszłego DONE, atomowe odrzucenie i brak odznaki/finansów wykonania. |
| R02 | P1 identyfikacja | /day-closeout?date=…&order=999999999 zwraca 200 i formularz /day-closeout/1. Nieistniejący jawny ID jest zamieniany na inną wizytę. | Jawny błędny lub niepasujący ID nie może wybierać innego klienta;404 lub czytelny stan bez formularza. Automatyczny wybór pierwszego braku tylko bez ID. |
| R03 | P1 codzienny zapis | Szkic w karcie A, wylogowanie/ponowny login w karcie B, ponowienie w A:403, szkic zostaje i0 zapisów. Po wymianie wyłącznie CSRF:200 i1 zapis. | Bezpieczne odnowienie tokenu po zalogowaniu i ponowienie bez utraty danych/zdjęć oraz bez duplikacji. Nie wyłączać CSRF. |
| R04 | P1 UX korekty | Pakiet200, podział100+100, zmiana kwoty na210 przy zwiniętych opcjach: walidacja poprawnie odrzuca zapis, ale niewidoczny podział blokuje prostą korektę. Kwota w bazie nadal200. | Pokazać konflikt przy kwocie, otworzyć właściwe pola i dać świadomy wybór korekty albo wyczyszczenia podziału. Nie usuwać walidacji ani nie wymyślać proporcji. Analogicznie czas. |
| R05 | P1 decyzja produktu | Tycoon nadal zależy od flagi false/profilu i jest opisany jako TEST. /search i /callbacks nie ładują wspólnego fragmentu assetów. Login/logout są generowane przez Spring, PWA/offline mają inny wygląd. | Tycoon domyślny i spójny wszędzie; wspólny layout; bez publicznego trybu klasycznego/testowego w normalnym CRM. Demo nadal jawnie oznaczone jako demo. |
| R06 | P1/P2 czytelność | Stabilne naruszenia kontrastu: Obszary pracy (np. 3,70/3,86:1 dla drobnego tekstu), desktopowe dni tygodnia (~3,93:1). | Minimum4,5:1 dla zwykłego tekstu; większe pomocnicze opisy i czytelne stany na kolorach obszaru. |
| R07 | P2 dostępność | Mobilny kalendarz nie ma widocznego dla narzędzi H1; wyszukiwanie/ponowny kontakt mają skip-link bez celu; kalendarz ma dwa skróty pominięcia. Publiczne strony nie mają spójnych landmarków. | Jedna poprawna ścieżka klawiatury, H1/main, etykiety i przywracanie fokusu. Nie deklarować zgodności WCAG wyłącznie po axe. |
| R08 | P2 wejście | / kieruje do kalendarza, manifest PWA startuje /today. | Jedno świadome wejście zgodne z ustaleniem Kalendarz jako start, zachowane deeplinki. |
| R09 | P2 błędy | Brakujący /work/ID zwraca400, /customers/ID 404; część błędów/publicznych stanów jest poza wspólną oprawą. | Spójne404/400/konflikt, komunikaty z bezpiecznym powrotem i zachowaniem szkicu. |
| R10 | P2 wydajność | Klienci i /orders są bez stronicowania; lookup obcina po pobraniu całego zbioru. /orders ładuje pozycje/pomiary w pętlach; kalendarz ma już zbiorcze pobieranie części danych. | Paginacja/LIMIT w bazie, zmierzone ograniczenie zbędnego HTML/zapytań. Nie przepisywać wydajnego kodu bez pomiaru. |
| R11 | P2 porządek | Duplikowane obsługi hash/details, rozproszone heady/asset-y, stare opcjonalne etykiety i część nieużywanych atrybutów modeli. | Jeden layout i wspólne komponenty, usunięcie wyłącznie potwierdzonego martwego kodu, aktualna dokumentacja. |
| R12 | bramka operacyjna | Nie badano rzeczywistego Windows/PowerShell, fizycznego Androida/iPhone'a, docelowego VPS, domeny i połączenia z nclean.pl. Nie ma podstaw do deklaracji wdrożenia. | Udokumentowany build/restore/staging, test na docelowym środowisku przed produkcją. Bez łączenia CRM z bazą CMS. |

P1 oznacza zadanie wymagane przed zamknięciem wersji, nie automatycznie krytyczną lukę bezpieczeństwa. R04 jest przede wszystkim utrudnieniem UX przy działającej ochronie danych. R05 wynika z nowej decyzji właściciela, a nie ze złamania wcześniejszego wymagania o opcjonalnym motywie.

## Uwagi do dostępności i zdjęć

Pierwszy skan formularza w trakcie wejścia animacji wskazał kontrast, którego nie potwierdził skan po wyłączeniu ruchu. Nie zaliczono tych przejściowych wskazań jako trwałych defektów formularza. Pozostałe opisane naruszenia są potwierdzone. axe nie zastępuje czytnika ekranu, klawiatury ani rzeczywistego telefonu.

Serwer przyjmuje JPEG/PNG do 8 MB i 20 MP, ponownie koduje zdjęcie i zachowuje nazwę do rozpoznawania czasu. Nie potwierdzono wygody z rzeczywistymi zdjęciami HEIC/50 MP ani orientacji EXIF. Przed pilotem potrzebne są rzeczywiste próbki telefonu i jasna obsługa ograniczeń; nie należy dodawać OCR ani nowego modułu zdjęć.

## Co dodać teraz, a czego nie rozbudowywać

Teraz: wspólny docelowy Tycoon, odzyskanie sesji/szkicu, przewidywalne korekty pakietów, pełne powiązanie ID i daty, czytelność, spójne stany i minimum operacyjnej gotowości. To poprawki istniejącego produktu.

Nie dokładać na tym etapie SMS, GPS, fakturowania, publicznego bookingu, magazynu, integracji z CMS, nowych punktów/rankingów ani kolejnego dashboardu. Zamknięcie oznacza stabilne wydanie do kontrolowanego używania przez właściciela, nie obietnicę aplikacji bez przyszłych błędów.

## Artefakt wykonawczy

[MEGA prompt domykający wersję](FINAL-TYCOON-PROMPT.md). Został przygotowany na podstawie tego audytu; **nie jest jeszcze wykonany**. W tej gałęzi zmieniono wyłącznie dokumentację audytu i dane dowodowe. Nie naprawiano ukradkiem aplikacji podczas ustalania stanu bazowego.

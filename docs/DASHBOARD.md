# Podsumowanie i koszty

## Obsługa

Po zalogowaniu wybierz **Podsumowanie** w głównej nawigacji (`/dashboard`). Kalendarz pozostaje ekranem startowym. Domyślnie podsumowanie pokazuje dzisiejszy dzień w strefie Europe/Warsaw. Wybierz Dzień, Tydzień (pon.–niedz.) lub Miesiąc, strzałki albo datę. Przycisk **+ Zlecenie** otwiera istniejący formularz kalendarza z wybraną datą. Kliknięcie wiersza zlecenia prowadzi do jego pozycji na istniejącej liście z możliwością zmiany terminu/statusu i zapisania pomiarów.

- **Plan:** wszystkie nieanulowane zlecenia; kwoty uzgodnione podczas rezerwacji i prognozy chemii zapisane w zleceniach.
- **Wykonanie:** tylko wykonane zlecenia i potwierdzone zużycie chemii. Status wykonania nie oznacza otrzymania płatności.
- **Zostaje po kosztach:** przychód ze zleceń minus chemia, paliwo, marketing i pozostałe koszty stałe. To wynik przed podatkami, bez amortyzacji ani wynagrodzenia właściciela, chyba że wpisano je jawnie jako koszt miesięczny. Nie jest zyskiem netto ani saldem gotówki.

Wykres pierścieniowy pokazuje strukturę **znanych kosztów**, a nie udziały przychodu i ujemnego wyniku. Legenda zawiera kwoty i procenty; dotknięcie pozycji rozwija sposób naliczania. Przy zerowych kosztach pozostaje neutralny pierścień. Gdy brakuje danych, wynik jest jawnie opisany jako niepełny. Lista zleceń obejmuje cały okres, z opisanym statusem także anulowanych; te ostatnie nie zwiększają przychodu ani kosztu chemii.

## Koszty miesięczne

Ustawienia → Koszty (`/settings/costs`). Wybierz miesiąc, wpisz nazwę, kategorię i kwotę każdej pozycji. Kategorie: marketing, leasing, ubezpieczenie, księgowość, telefon, oprogramowanie i inne. Dodawaj/usuń wiersze, następnie **Zapisz koszty miesiąca**. Zapis bez pozycji jest świadomym potwierdzeniem kosztu 0 zł; brak zapisu oznacza brak danych.

Każdy zapis dotyczy wyłącznie wybranego miesiąca. Koszty nie powielają się automatycznie w kolejnych miesiącach. Bieżący i przyszłe miesiące są edytowalne; zakończone miesiące są dostępne tylko do odczytu. Zmiany przyszłego miesiąca nie wpływają na starsze okresy. W tej wersji nie ma osobnej procedury otwierania zamkniętego miesiąca ani wprowadzania zaległych kosztów miesięcznych.

Kwota miesięczna jest dzielona na dni kalendarzowe, niezależnie od liczby zleceń. Dzienny udział to różnica zaokrąglonych kwot narastających: `round(kwota × numer_dnia / liczba_dni, 2) − round(kwota × (numer_dnia − 1) / liczba_dni, 2)`. Dzięki temu suma dni wynosi dokładnie kwotę miesięczną, również dla lutego i ułamkowych groszy. Tydzień na granicy miesięcy sumuje udziały z obu miesięcy. Marketing jest wyodrębnioną kategorią i nie jest ponownie dodawany do pozostałych kosztów stałych.

## Paliwo

Wybierz dzień; podaj spalanie, cenę litra oraz kilometry planowane/rzeczywiste. Parametry są zapisywane dla konkretnego dnia. Ostatnie parametry są tylko podpowiedzią dla nowego dnia i wymagają zapisania; nie przeliczają innych dni.

Koszt dnia = `km × l/100 km × cena litra / 100`, zaokrąglony do groszy. Wpisana kwota rzeczywistego kosztu zastępuje wyliczenie rzeczywiste z km, bez sumowania obu wartości; plan nadal używa planowanych km. Puste km oznaczają brak danych, **0 km** dzień bez przejazdów. Każdy dzień podsumowania wymaga jawnych danych paliwa, także dzień wolny. Przy dłuższych okresach brakujące dni powodują oznaczenie wyniku jako niepełnego. Nie ma GPS ani automatycznej wyceny tras.

## Historyczne ceny chemii

V7 dodaje cenę jednostkową i wartość do rzeczywistych pomiarów. Nowy pomiar zapisuje cenę w chwili jego potwierdzenia. Korekta ilości istniejącego pomiaru w tej samej jednostce zachowuje zapisaną cenę. Zmiana cennika środka nie przelicza tych pomiarów ani prognoz.

Przy aktualizacji z V6 cena starego pomiaru jest uzupełniana tylko ze zgodnego historycznego wiersza prognozy (ta sama pozycja, środek i jednostka). Brak takiego źródła pozostaje niewyceniony — migracja nie używa dzisiejszej ceny jako rzekomo historycznej. Ponowne jawne zapisanie niewycenionego pomiaru wycenia go po bieżącej cenie; interfejs podaje tę zasadę w pomiarach. Nie jest to magazyn ani koszt konkretnej partii zakupu.

Brak pomiaru, brak ceny historycznej lub pominięcie środka z dodatniej prognozy oznacza niepełne dane. Jawne 0 ml jest prawidłowym pomiarem. Koszty częściowo znane są uwzględniane, ale nie przedstawiane jako kompletny wynik. Zakończenie miesiąca blokuje jego konfigurację kosztów miesięcznych; późniejsza korekta statusu zlecenia, pomiarów lub paliwa nadal może zmienić wynik historyczny. Pełnego zamknięcia księgowego okresu nie ma.

## Bezpieczeństwo i uruchomienie

Zachowano OWNER, Spring Security, CSRF i PostgreSQL; nie dodano publicznego trybu DEMO do aplikacji. Wszystkie zmiany kosztów przechodzą przez POST i serwerowe sprawdzanie wersji. Ponowne lub równoległe przesłanie starej wersji jest odrzucane. Błąd walidacji lub konflikt zachowuje dane formularza; przy konflikcie odśwież stronę przed kolejną edycją. V1–V6 pozostają niezmienione, Flyway dopisuje V7. Uruchomienie na Windows: [WINDOWS.md](WINDOWS.md), po starcie `/dashboard`.

## Weryfikacja 11.11.2026

- Java 17, Maven Wrapper 3.9.11: `./mvnw verify` (w środowisku z osobnym zapisywalnym cache Maven, tryb offline) — **44 testy, 0 błędów, 0 porażek, 0 pominiętych**: 17 jednostkowych/MVC i 27 integracyjnych z Testcontainers/PostgreSQL.
- Obejmuje m.in. role/CSRF, rozdział planu i wykonania, wykluczenie anulowanych, brak/zero/częściowe pomiary, zachowanie ceny chemii przy korekcie, migrację V6→V7, alokację kosztów i granice miesięcy/roku, rok przestępny, ujemny wynik, walidację, konflikty wersji oraz restart.
- Playwright na rzeczywistej aplikacji: **360, 390, 768 i 1440 px**; 8 zleceń dziennie, nawigacja okresów, wykres, edycja paliwa i kosztów, błąd oraz ponowienie bez utraty pól, dodanie/usunięcie wiersza kosztu, skrót formularza zlecenia i szczegóły zleceń. Brak poziomego przewijania i błędów JavaScript. Potwierdzony zapis po ponownym uruchomieniu aplikacji.
- Przykład kontrolny: plan 1260 zł − chemia 35 zł − paliwo 48 zł − marketing 10 zł − stałe 5 zł = **1162 zł**. Wykonanie: 900 − 30 − 24 − 10 − 5 = **831 zł**.
- Testy przeglądarkowe używają tymczasowego schematu w lokalnym PostgreSQL; schemat usunięto po teście. Testcontainers ma oddzielny kontener i losowe poświadczenia. Bez kontaktu z produkcją nclean.pl.
- Środowisko nie udostępnia współdzielonej przeglądarki ani przekierowania portów. Oddzielny plik demonstracyjny dashboardu powstał z wyrenderowanych widoków aplikacji, zawiera wyłącznie syntetyczne dane i lokalne obliczenia; nie jest połączeniem z działającym serwerem CRM. Pełna aplikacja została osobno sprawdzona w Chromium.

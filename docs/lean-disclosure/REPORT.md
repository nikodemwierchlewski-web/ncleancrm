# LEAN: opcje na żądanie

15.11.2026 · gałąź `feat/lean-progressive-disclosure`, bazująca na pełnej implementacji przypomnień (`f7ce860`).

## Zmiana obsługi

- **Dzisiaj:** plan dnia przed analizami. Na komputerze zlecenia i przypięte notatki po lewej, cel i wynik firmy po prawej; kolumny nie rozciągają sobie pustych obszarów. Na telefonie plan na początku, skrót „Wynik firmy” prowadzi do analizy. „+ Zlecenie”, nawigacja, status i zamknięcie dnia pozostają widoczne.
- **Wynik firmy:** „Zmień zakres” domyślnie zwinięte. Nazwa zakresu, daty, kwoty, koło i ostrzeżenia pozostają na wierzchu. Zastosowanie filtrów ponownie zwija panel. Zamknięcie bez zastosowania zachowuje wartości. „Szczegóły dni” odsłania wybór dnia i kwoty; kliknięcie części koła otwiera tę sekcję.
- **Notatki/cele:** podstawowe pola i zapis widoczne. Termin, godzina i przypięcia w jednej sekcji z krótkim podsumowaniem. Przypomnienia osobno, z liczbą wpisów. Dawne cykle zachowane i oznaczone jako włączone. Kroki celu pozostają dostępne przy rodzaju GOAL.
- **Zlecenie:** przypomnienia zwinięte, liczba widoczna. Dane wizyty, kontakt, wykonanie, zdjęcia i płatności zachowują dotychczasowe miejsca.
- **Koszty:** jeden zwijany formularz okresu/wyszukiwania/anulowanych. Aktywne filtry są widoczne także po zwinięciu; działa wyczyszczenie. Zmiana zakładki i okresu zachowuje wyszukiwanie oraz wybór anulowanych.
- **Paliwo:** szybkie tankowanie pozostaje widoczne. Spalanie pełny–pełny i ostatnie odczyty zwinięte. Historia zakupów, rachunki, agregat i parametry dostępne.

Wykorzystano istniejącą obsługę walidacji, błędów i kotwic z `interface.js`, bez dublowania jej w drugim module. Zwinięcie samo w sobie nie zapisuje, nie resetuje ani nie wyłącza pól. Błąd walidacji odsłania sekcję przed nadaniem pola fokusowi. Błąd zapisu zachowuje wartości.

## Pozostałe ekrany

Przejrzano kalendarz, klientów, cennik i ustawienia w prawdziwej aplikacji i skanach dostępności. Kalendarz już ma jeden panel dnia oraz dialog zlecenia; klienci mają zwijane dodawanie/edycję; cennik ma kategorie i osobne edycje, a ustawienia prowadzą do modułów. Nie dodawano tam kolejnych warstw menu. W Dzisiaj usunięto powtórzony skrót do notatek, dostępnych z Więcej oraz kart przypiętych, zastępując go skrótem do wyniku.

Bez zmian w modelach, zależnościach, migracjach, cenach historycznych, regułach finansowych, PostgreSQL, OWNER i CSRF. Nie łączono się z produkcją.

## Porównanie

Ta sama syntetyczna baza: jedna wizyta, główny cel z dwoma krokami, pusty formularz nowej notatki. Pomiar Chromium 390 × 844, animacje ograniczone; wysokość treści w pikselach CSS. Wszystkie 40 pomiarów (5 ekranów × 4 szerokości × przed/po) w [comparison.json](comparison.json).

| Pomiar | Przed | Po |
|---|---:|---:|
| Początek planu dnia od góry dokumentu | 1416 px | 249 px |
| Treść nowej notatki | 1045 px | 701 px |
| Widoczne podstawowe pola nowej notatki | 7 | 3 |
| Widoczne pola filtrów firmy | 3 | 0 |
| Widoczne pola filtrów kosztów | 4 | 0 |
| Wysokość treści kosztów | 869 px | 683 px |
| Wysokość treści paliwa | 1374 px | 1195 px |

Otwarcie filtrów, terminu lub przypomnień kosztuje jeden dodatkowy klik. Dodanie zwykłej notatki, rozpoczęcie dodawania zlecenia i szybkie tankowanie nie wymagają nowych kroków. Nie chowano danych potrzebnych w tych podstawowych czynnościach. Oddzielnie sprawdzono dzień z ośmioma zleceniami; podane wysokości dotyczą opisanej, powtarzalnej próby z jedną wizytą.

## Wyniki

- Maven Wrapper `verify`: **185 testów, 0 błędów, 0 pominiętych**: 66 jednostkowych/MVC/architektury + 119 integracyjnych z rzeczywistym PostgreSQL 17 przez Testcontainers. Końcowe zasoby spakowane do JAR.
- Prawdziwa aplikacja: **360/390/768/1440 px**; zwijanie i wartości niewysłanych filtrów, zakres własny i etykieta, odsłanianie szczegółów koła, notatka z terminem/przypięciem/przypomnieniem zapisana przy zamkniętych sekcjach, ponowne otwarcie, walidacja ujemnej wartości schowanego pola i fokus, błąd 503 oraz ponowienie, koszty: zakładki/okres/wyczyszczenie, kotwica analizy paliwa.
- Regresje: osiem wizyt, obszary, kalendarz mobilny i desktop, nowy/powracający klient, kilka kategorii, ceny historyczne, cel, zdjęcia, koszty miesięczne i szybkie tankowanie. Bez błędów JavaScript. Dodatkowa końcowa próba scenariuszy LEAN używała istniejącego `interface.js` jako jedynej obsługi walidacji i kotwic.
- axe: **81 skanów, 0 wykrytych naruszeń**, w tym rozwinięte filtry, edytor notatki i przypomnienia; [accessibility.json](accessibility.json).
- `node scripts/test-web-boundaries.cjs`, składnia JS/Pythona i `git diff --check`: PASS.
- HTML DEMO: cztery szerokości, koło i filtry, przypomnienia przy zleceniu/notatce, odsłanianie schowanego błędnego pola, koszty: filtr/zakładka/wyczyszczenie, kalendarz i dwa zagnieżdżone sandboxy. Bez błędów JavaScript.

Testy szerokości wykonano w Chromium, nie na fizycznym Androidzie. W tej sesji brak działającego narzędzia otwarcia podglądu serwera Codexa; HTML jest odrębną demonstracją 81 przechwyconych widoków serwera z tymi samymi modułami UI oraz transportem DEMO. Zapis próby jest w pamięci karty, a JSON nie jest kopią PostgreSQL ani zdjęć. Pełna aplikacja nadal zapisuje w PostgreSQL.

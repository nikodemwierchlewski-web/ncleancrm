# Przegląd UX/UI — 11.11.2026

## Zakres i diagnoza

Praca na `feat/ux-coherent-mobile` od zintegrowanej `feat/crm-v01-day-workflow`. Audyt dotyczy prawdziwych szablonów i działającej aplikacji, bez zmian modeli, migracji, historycznych cen, uprawnień i zasad obliczeń.

Najpoważniejszym problemem podglądu była niespójność z aplikacją: wcześniejsze demonstracje łączyły różne ekrany i oddzielne komponenty wykresu. Zostały zastąpione nowym plikiem wyrenderowanych widoków tej wersji.

Problemy utrudniające pracę:

- Dzisiaj powtarzał najbliższe zlecenie w osobnej dużej karcie i na liście; dodatkowe statystyki oraz skróty poprzedzały plan.
- Wykonanie pokazywało naraz wycenę, timer, cały formularz i płatności. Formularze różnych etapów miały podobną wagę.
- Podsumowanie zawierało również dziennik paliwa oraz długie analizy, więc nie miało jasnego końca ani hierarchii.
- Informacje pomocnicze, puste stany i ustawienia konkurowały z najczęstszymi działaniami.
- Style nawigacji były powtarzane w kilku plikach. Duża liczba podobnych kart i akcji utrudniała orientację.

Problemy wizualne: duże puste przestrzenie, powtarzane niebieskie powierzchnie, niespójne odstępy i rozmiary nagłówków, różne sposoby prezentacji formularzy. Nie wykryto potrzeby zmiany stosu ani bazy.

## Wdrożony kierunek

Jasny interfejs z białymi powierzchniami i niebieskim akcentem ncLEAN. Wspólne tokeny i reguły w `interface.css`; usunięto powielone końcowe reguły nawigacji z `records.css` i `dashboard.css`. Zachowano komponentowe style układu i działające skrypty formularzy.

- **Dzisiaj:** data, jedna akcja dodania, obszary i od razu lista. Najbliższa niezakończona pozycja jest wyróżniona na tej samej liście. Zmiana daty i zadania do uzupełnienia są rozwijane. Statystyki są dostępne przez Podsumowanie, a dziennik przez krótki link.
- **Zlecenie:** wycena jest rozwijana; główny formularz wykonania ma jedno wyróżnione miejsce. Timer jest spokojniejszą akcją. Płatności są osobną rozwijaną sekcją. Link `#payments` i powrót po wpłacie automatycznie ją otwierają.
- **Podsumowanie:** wynik, przychód/liczba, struktura kosztów i brakujące dane. Wpłaty/czas, porównanie i analiza usług są rozwijane. Usunięto powtórzony dziennik paliwa z analizy; pozostaje dostępny w swoim widoku.
- **Wykres:** SVG z geometrią okręgu, poprawnym otworem i oddzielnym środkiem. Łuki są proporcjonalne do danych; po zaokrągleniach procentów normalizują się do pełnego obwodu. Dla zera nie ma kolorowych łuków. Kwoty i procenty pozostają w czytelnej, klikalnej legendzie. Ujemny wynik nie jest fragmentem koła.
- **Pozostałe widoki:** te same przyciski, formularze, nagłówki, obramowania i nawigacja. Zachowano kategorie cennika, klientów, wiele obszarów oraz dziennik/zdjęcia.

Pierścień pozostawiono jako drugorzędne porównanie czterech kategorii, z kwotami w legendzie. Wynik i przychód są tekstowymi wartościami głównymi, więc koło nie jest konieczne do odczytania danych. Bez biblioteki wykresów.

## Koszt interakcji i przewijanie

Nie przeprowadzano badań użytkowników ani pomiarów czasu; nie deklarujemy procentowej poprawy szybkości. Porównanie zrzutów sprzed zmiany i geometrii nowej aplikacji daje:

| Zadanie | Wcześniej | Po zmianie |
|---|---|---|
| Zobaczyć pierwszy wiersz planu na telefonie | Lista po karcie następnego zlecenia, metrykach i skrótach; na zachowanym zrzucie poniżej dolnego paska | W teście czterech szerokości początek pierwszego wiersza poniżej 500 px; bez przewijania |
| Otworzyć zlecenie z listy | 1 kliknięcie | 1 kliknięcie |
| Dodać zlecenie z dnia | 1 kliknięcie otwarcia | 1 kliknięcie otwarcia; ten sam formularz i dane |
| Zmienić datę Dzisiaj | Widoczne pole i zatwierdzenie | Dodatkowe otwarcie sekcji, w zamian za mniej treści w codziennym widoku |
| Otworzyć płatności ze szczegółów | Formularz stale widoczny | 1 rozwinięcie; wejście z kotwicą/po zapisie otwiera sekcję automatycznie |
| Przeczytać szczegółową analizę | Całość stale rozwinięta | 1 rozwinięcie wybranego tematu, bez dodatkowej strony |

To jawne kompromisy: rzadsze operacje wymagają otwarcia, a plan i wykonanie otrzymują więcej miejsca. Liczby oznaczają liczbę aktywacji kontrolki w danej ścieżce, bez liczenia pisania i gestów klawiatury. Zrzuty przed/po mają różne syntetyczne dane; nie są kontrolowanym badaniem A/B.

## Weryfikacja

- Java 17, Maven Wrapper `verify`: **59 testów: 20 jednostkowych/MVC + 39 PostgreSQL/Testcontainers; bez błędów, porażek i pominięć**.
- Chromium, prawdziwa aplikacja, **360, 390, 768 i 1440 px**: plan ośmiu zleceń, kalendarz, wykonanie, podsumowanie, cennik i paliwo bez poziomego przewijania. Wyrenderowano 30 wariantów stron i okresów z syntetycznymi danymi.
- SVG: suma długości łuków jest równa obwodowi z tolerancją 0,01 px; wszystkie łuki dla zerowego okresu są niewidoczne. Cztery kolory i legenda zgodne z danymi.
- Formularze: dodanie zlecenia z dwóch kategorii, zachowanie daty, wpłata i automatyczne otwarcie Płatności po zapisie, odrzucona cena z zachowaną notatką, poprawienie i zapis wykonania. Pole edycji przy zmniejszonym widoku 360×420 pozostaje nad nawigacją po przewinięciu do środka.
- Długą nazwę usługi i dużą kwotę sprawdzono dodatkowo na tym samym DOM/CSS wyeksportowanych widoków we wszystkich czterech szerokościach — bez poziomego przewijania.
- Podgląd pliku sprawdzony w zwykłej przeglądarce oraz w zagnieżdżonym sandboxie `allow-scripts`: nawigacja, szczegóły, lokalne potwierdzenie formularza, SVG, okresy, kategorie i ilości zlecenia oraz klienci. Poprawiono wysokość opakowania, aby dolna nawigacja nie była obcięta w ramce.

## Podgląd i ograniczenia

Plik podglądu powstaje z rzeczywistych odpowiedzi Thymeleaf finalnej aplikacji oraz jej CSS i skryptów. Nie jest kolejną sklejką poprzednich demonstracji. Zawiera fikcyjne dane i demonstracyjne potwierdzenia formularzy; nie wykonuje zapisów do PostgreSQL, logowania, kopii, eksportu ani pełnych rozliczeń. Nie wszystkie daty poza przygotowanym zestawem mają osobne dane. Zmiany nie są synchronizowane z aplikacją. Dane formularza są przechowywane w pamięci sesji podglądu, bez localStorage. Zdjęcie można podejrzeć lokalnie, bez wysyłania.

Serwerowy podgląd Codexa jest nadal niedostępny (`capabilities: []`). Testy rzeczywistej aplikacji wykonano w Chromium na odizolowanym PostgreSQL; schemat testowy został usunięty. Nie łączono z produkcją. Instalacja na fizycznym telefonie i Windows pozostają ograniczeniami opisanymi w dokumentacji 0.1.

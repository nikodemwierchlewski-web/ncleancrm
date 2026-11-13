# Lean workspace — audyt i mapa przed zmianami

Baza: experiment/tycoon-interactions (ecdeb57), pełny niescalony CRM. Kalendarz pozostaje ekranem startowym. Audyt źródeł wszystkich szablonów i mobilnego podglądu: Podsumowanie miało około 3734 px przy 390 px szerokości (viewport ramki 770 px); sekcja „Do uzupełnienia” zaczynała się około 2432 px.

Problemy: operacje ukryte w Ustawieniach; Podsumowanie oznaczone jako Ustawienia; drugi, uproszczony algorytm braków w Dzisiaj; powtórzone formularze wykonania i chemii; formularze danych klienta kopiowane dla tworzenia/edycji; finanse i cele wypychające działanie w dół; ciężkie ramki i rozbudowane filtry.

Docelowa mapa:
- Kalendarz: terminy, obszary, jeden istniejący formularz rezerwacji.
- Dzisiaj: następna wizyta, rzeczywiste braki z DayReviewService, plan dnia; odczyt paliwa prowadzi do istniejącego /fuel.
- Klienci: kontakt, wspólne pola edycji, historia, ponowny kontakt.
- Więcej (/more): Podsumowanie, Koszty i inwestycje, Samochód i agregat, Notatki i cele, Ustawienia.
- Ustawienia: cennik/chemia, obszary, koszty miesięczne/parametry i wygląd. Istniejące adresy zachowane.
- Wykonanie: wspólny formularz dokumentacji na /work/{id} i /day-closeout. Te same pola, pomiary, zdjęcia i walidacja. Opcjonalny podział ceny/czasu zachowany. Dawne endpointy pozostają kompatybilne.
- Podsumowanie: kompaktowy okres, trzy liczby, kompletność i jedna akcja; analizy oraz cele na żądanie.

Zasady: bez migracji i zmiany znaczenia danych; wpłata nie warunkuje dokumentacji; pomiar nie jest prognozą; rachunek nie jest kosztem zużycia; skrót otwiera ten sam formularz. Wspólny układ obu motywów, Tycoon opcjonalny, klasyczny domyślny.

## Wdrożone zmiany i obsługa

Nowa nawigacja jest wspólna: Dzisiaj, Kalendarz, Klienci, Więcej. /more jest wyłącznie menu istniejących funkcji, nie nowym modułem biznesowym. Ustawienia zawierają konfigurację; podstawowe operacje dostępne są z Więcej lub kontekstowego skrótu.

Dzisiaj korzysta bezpośrednio z DayReviewService i pokazuje następny termin oraz pierwszy brak przed pełną listą. Płatności pozostają oddzielne. Podsumowanie ma trzy kwoty: przychód, otrzymane wpłaty i wynik po znanych kosztach. Okres i tryb są kompaktowe; dane niepełne są jawnie oznaczone. Koszty, analizy i panel osobisty są rozwijane. Link do ukrytej sekcji otwiera także jej nadrzędne details.

Wykonanie zlecenia w szczegółach wizyty i w zamknięciu dnia używa work/completion-form. Zapis przebiega przez tę samą walidację DayReviewService, a parametr return_to wybiera wyłącznie jeden z dwóch wewnętrznych widoków. Zdjęcia używają istniejącego wspólnego fragmentu. Opcjonalne kwoty/czasy usług zachowano; błędna suma wycofuje całą transakcję. Dawny endpoint /work/{id}/complete nadal działa dla kompatybilności. Skróty chemii z kalendarza/listy prowadzą do wykonania zamiast otwierać kolejny formularz pomiarów. Dane prognozy wyświetlają się jako pomoc, nie są przepisywane do realnego pomiaru. Kartoteka współdzieli pola dodawania i edycji klienta.

Telefon: lista dnia w zamknięciu jest rozwijana nad formularzem; na komputerze pozostaje bocznym panelem. Ramki i cienie są spokojniejsze w obu motywach. Kwoty kosztów są zwarte, a ich znaczenie wyjaśnia rozwijana sekcja. Ręczny poprzedni model paliwa zachowano w korektach zaawansowanych; codzienne odczyty i tankowania mają dotychczasowe wspólne formularze /fuel. Nie zmieniono reguł rozliczeń, identyfikatorów, historii ani migracji.

## Porównanie przed / po

| Czynność / ekran | Przed | Po |
| --- | --- | --- |
| Podsumowanie, próbka 390 px | ok. 3734 px wysokości | 1150 px domyślnie; akcja kończy się na 544 px |
| Podstawowe kwoty | rozbudowany wynik i kolejne karty przed analizami | trzy zwarte kwoty na pierwszym ekranie |
| Braki dokumentacji | dwa algorytmy: Dzisiaj i zamknięcie | jeden DayReviewService |
| Wykonanie / rzeczywista chemia | osobne formularze w kilku kontekstach | jeden współdzielony formularz, skróty do niego |
| Dodawanie / edycja klienta | kopiowane pola kartoteki | jeden fragment pól |
| Dostęp do wyników / konfiguracji | Ustawienia → wybrana funkcja | Więcej → funkcja; Ustawienia wyłącznie konfiguracja |
| Braki w Dzisiaj | pod planem wizyt i dodatkowymi sekcjami | bez przewijania listy wizyt |

Dane wysokości są orientacyjnym porównaniem dwóch syntetycznych próbek, a nie pomiarem szybkości użytkowników. Liczba przejść do wyniku pozostaje taka sama (menu i wybór); poprawiono nazewnictwo i rozdzielenie funkcji. Konfiguracja ma dodatkowe wejście przez Więcej jako rzadziej używana czynność. Z poziomu wizyty formularz wykonania pozostaje na miejscu, bez dodatkowego przejścia do osobnego kreatora.

## Weryfikacja

Maven Wrapper verify: 27 testów jednostkowych/MVC i 80 integracyjnych PostgreSQL/Testcontainers, łącznie 107, bez błędów i pominięć. Naprawiono konfigurację testu MVC po dodaniu zależności od DayReviewService. Nowe testy obejmują rozdział menu, uprawnienia /more, zgodność braków, wspólny formularz oraz zachowanie podziału ceny/czasu i atomowe odrzucenie błędnej sumy.

Chromium na prawdziwej aplikacji, osobne schematy lokalnego PostgreSQL: osiem wizyt przed anulowaniem/przełożeniem, nowy i powracający klient, dwie kategorie, cena indywidualna, korekta adresu bez zmiany domyślnego, wykonanie z wizyty, zmiana cennika bez zmiany zapisanej ceny, zdjęcia/czas/chemia,503 i ponowienie, konflikt kart, notatki/cele, zamknięcie i ponowne otwarcie po korekcie oraz restart. Scenariusze poprawiono tam, gdzie zakładały dawny układ menu lub próbowały manipulować zamykanym jeszcze dialogiem.

Szerokości 360/390/768/1440 px: brak poziomego przewijania; trzy kwoty i akcja podsumowania oraz akcja Dzisiaj powyżej 760 px. Domyślna wysokość Podsumowania: 1150 px na telefonie, 1059 px na komputerze; dół głównej akcji 544/569 px. Sprawdzono oba motywy, klawiaturę/fokus, ograniczony ruch i mały viewport. Powtarzalny scripts/verify-browser.py sprawdza oba motywy i nową organizację; plik właściciela pozostaje prywatny.

Nie użyto bazy produkcyjnej. Nie dodano modułów biznesowych ani migracji. Klasyczny wygląd pozostaje domyślny w aplikacji i nowej paczce HTML; Pixel Tycoon można włączyć w podglądzie przełącznikiem.

Podgląd HTML: 62 rzeczywiście wyrenderowane widoki i te same moduły UI, z jawnymi adapterami symulacji. Sprawdzono tryb klasyczny jako domyślny, przełączanie Tycoon, nowe menu, wspólny formularz wykonania i utrzymanie wartości po demonstracyjnym zapisie, nawigację/formularze/cele/odznaki, cztery szerokości i zagnieżdżoną ramkę z nieprzezroczystym originem. Poprawiono serializację aktualnych wartości kontrolek podczas przejść w demonstracji. Dane i zapis HTML są syntetyczne, w pamięci karty. Pobrano ZIP z GitHuba: HTTP 200, zgodność SHA-256 z testowanym plikiem i metadanymi.

[Wyniki i sumy migracji](audit/lean-workspace-evidence.json), [paczka i instrukcja](../preview/README.md). Brak natywnego pulpitu/przekierowania portów w tej sesji; lokalny HTTP nie jest adresem publicznym dla użytkownika.

Próba `gh pr create --draft` zakończyła się `POST https://api.github.com/graphql: Forbidden` (403). Gałąź jest wysłana; [opis PR](LEAN-WORKSPACE-PR.md) i [porównanie zmian](https://github.com/nikodemwierchlewski-web/ncleancrm/compare/experiment/tycoon-interactions...ux/lean-workspace?expand=1) są gotowe do przeglądu.

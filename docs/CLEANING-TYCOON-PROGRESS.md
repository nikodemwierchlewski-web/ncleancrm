# Cleaning Tycoon — animacje i rzeczywisty postęp (eksperyment)

Nowsze rozszerzenie: [interakcje i reakcje na klikanie](TYCOON-INTERACTIONS.md).

Gałąź `experiment/cleaning-tycoon-progress` rozwija istniejący `experiment/pixel-tycoon` wraz z niepołączonym jeszcze pełnym CRM. Klasyczny interfejs pozostaje domyślny. Nie zmieniono produkcji ani wykonanych migracji V1–V13.

## Obsługa

Windows: po standardowym przygotowaniu lokalnego CRM uruchom `./scripts/Start-Local.ps1 -PixelTycoon`. Powrót: Ctrl+C i uruchomienie tego skryptu bez przełącznika. Profil `tycoon` włącza wyłącznie wygląd; logowanie OWNER, CSRF i baza pozostają normalne.

W kalendarzu i podsumowaniu jest własna izometryczna grafika bazy firmy sprzątającej: pracownik, ekstraktor, kanapa, butelki, samochód i pęcherzyki. Delikatny ruch pracownika, bąbelki i błysk są animowane w CSS. Wybranie dnia uruchamia pojedyncze podskoczenie samochodu. Grafika jest dekoracyjna i nie przedstawia rzeczywistej lokalizacji ani statusu pracy.

W podsumowaniu „Twój postęp” pokazuje kompletność dokumentacji wybranego dnia, niezależną od zapłaty. Przy braku zleceń nie ma sztucznego procentu. Poniżej jest jeden aktywny, przypięty cel z istniejących Notatek, wybrany według terminu i ostatniego przypięcia. Link otwiera zwykły edytor. Kroki celu pozostają ręczne; zaznaczenie wszystkich kroków nie kończy celu automatycznie. Cel bez kroków nie dostaje fikcyjnego paska postępu.

„Odznaki i historia” zawiera pięć etapów:

- 1, 10 i 50 wykonanych zleceń, których termin już nadszedł;
- pierwszy sprawdzony, niepusty dzień z kompletną dokumentacją;
- pierwszy cel świadomie oznaczony jako ukończony.

Odznaka pojawia się po udanym zapisie. Komunikat nie przejmuje fokusu, można go zamknąć; czeka na zamknięcie otwartego formularza. Historia jest dostępna także po zniknięciu komunikatu. Cofnięcie statusu aktualizuje bieżący licznik, ale nie usuwa historycznie osiągniętej odznaki.

Przycisk „Ogranicz animacje” jest w podsumowaniu i ustawieniach. Zapamiętuje wyłącznie preferencję ruchu w przeglądarce. Ustawienie systemowe `prefers-reduced-motion` zawsze ma pierwszeństwo. Animacje świata zatrzymują się poza widokiem i w ukrytej karcie. Przy niedostępności JavaScript lub odczytu SVG zostaje statyczna ilustracja.

## Zapis i migracja

Flyway V14 dodaje `tycoon_award` i blokadę zapisu. Odznaki są wspólne dla firmy z obecnym jednym kontem OWNER. Zapis dołącza do transakcji wykonania zlecenia, zamknięcia dnia lub ukończenia celu: wycofanie operacji wycofuje również odznakę. Klucz odznaki i `ON CONFLICT` zapobiegają duplikatom. Dane pozostają w PostgreSQL po restarcie, także przy klasycznym wyglądzie.

V14 rozpoznaje istniejące ukończone zlecenia i cele bez modyfikacji ich danych. Takie odznaki mają pochodzenie HISTORY; data jest datą rozpoznania podczas aktualizacji. Nie wysyła się serii komunikatów za historię. Zamknięć dawnych dni nie odtwarza się uproszczonym SQL — wymagają aktualnej pełnej walidacji dnia.

POST potwierdzający powiadomienie wymaga OWNER i CSRF. Atomowo rezerwuje komunikat, dzięki czemu dwie karty i restart nie powtarzają go. Jest to obsługa „najwyżej raz”: przy przerwanym połączeniu po zapisie potwierdzenia komunikat może nie dotrzeć, ale odznaka pozostanie w historii. Odczyt `/tycoon/progress` nie przyznaje odznak.

Nie dodano punktów za kliknięcia, kar za przerwy, serii dni, rankingów, dźwięków ani nowej listy celów. Kwoty, standardy chemii, ceny historyczne i rozliczenia nie są zmieniane przez ten moduł. Własne SVG i lokalne fonty nie wymagają zewnętrznych serwisów.

## Weryfikacja

13 listopada 2026, Java 17, PostgreSQL 17 w Testcontainers:

- Maven Wrapper `clean verify`: **27 testów jednostkowych/MVC + 76 integracyjnych = 103**, zero błędów, porażek i pominięć.
- Nowe testy: transakcyjne wycofanie wykonania i odznaki, progi i korekta licznika, przyszłe wizyty, równoległe potwierdzenie w dwóch kartach, restart, walidacja dat/kodów, OWNER/CSRF, niepusty kompletny dzień, świadome ukończenie celu i zachowanie danych przy migracji V13 → V14.
- Chromium na prawdziwej aplikacji z osobnym schematem lokalnego PostgreSQL: osiem wizyt, zdjęcia/czas/chemia, anulowanie i przełożenie, błąd503 i ponowienie, konflikt dwóch kart, kroki celu, zamknięcie i ponowne otwarcie dnia po korekcie, restart.
- 360, 390, 768 i 1440 px: kalendarz, podsumowanie i ustawienia; brak poziomego przewijania; działający SVG, przełącznik ruchu po przejściu strony i odświeżeniu, systemowy ograniczony ruch, odroczenie odznaki podczas otwartego modala.
- Rozszerzono powtarzalny test `scripts/verify-browser.py --owner-file <prywatny-json> --pixel-tycoon`. Uruchamia własny schemat Compose i kopię JAR, sprawdza też klientów, edycję kosztów i zachowanie formularza po503. Plik danych właściciela pozostaje lokalny, poza Git.

Pierwsze próby nowych testów ujawniły błędy przygotowania danych testowych (powtórzona nazwa usługi, brak klienta i błędny separator parametru schematu w URL). Poprawiono je i ponowiono pełny zestaw. W JS poprawiono powtarzające się aktualizacje obserwatora, kolejkę powiadomień, timer zamykania oraz jawny UTC dla dat odznak.

## Podgląd

Pobieralny ZIP i instrukcja: [preview/README.md](../preview/README.md). Podgląd zawiera 60 widoków wyrenderowanych przez rzeczywistą aplikację i te same moduły CSS/JS. Operacje w pliku HTML są jawnie symulowane na syntetycznych danych w pamięci karty; rzeczywisty PostgreSQL sprawdzono oddzielnie. Zmiana preferencji animacji w ramce podglądu jest przechowywana w pamięci tego podglądu.

Podgląd HTML sprawdzono w Chromium również w zagnieżdżonej ramce z nieprzezroczystym originem: nawigacja i zapis wydatku, pięć obrazków odznak, kroki celu, postęp dnia, jednorazowa odznaka za przykładowe zamknięcie, preferencja ruchu między ekranami, przełączanie wyglądu oraz cztery szerokości. Brak błędów JavaScript. [Zestawienie wyników i sum migracji](audit/cleaning-tycoon-evidence.json).

Bieżące środowisko Codexa nie udostępnia obsługi pulpitu ani przekierowania portów. Lokalny HTTP jest narzędziem testów przeglądarkowych, nie publicznym linkiem dla użytkownika. Nie deklarujemy dostępności interaktywnego hostingu w czacie.

Próba utworzenia draft PR przez `gh pr create` zakończyła się `POST https://api.github.com/graphql: Forbidden` (403). Gałąź została wysłana. Gotowy opis: [CLEANING-TYCOON-PR.md](CLEANING-TYCOON-PR.md). [Otwórz porównanie do bazowego eksperymentu](https://github.com/nikodemwierchlewski-web/ncleancrm/compare/experiment/pixel-tycoon...experiment/cleaning-tycoon-progress?expand=1).

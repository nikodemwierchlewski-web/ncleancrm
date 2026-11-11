# Samochód, agregat i rachunki

## Codzienna obsługa

Na dashboardzie wybierz dzień i **Wpisz licznik i czas agregatu**. Podaj stan licznika samochodu na koniec dnia oraz godziny i minuty pracy agregatu. Dla dnia bez jazdy powtórz licznik, a dla nieużywanego agregatu wpisz 0 h 0 min. Przycisk **Zapisz dzień** wraca do dashboardu z zachowaniem daty.

Pierwszy odczyt jest punktem początkowym, nie dystansem od zera. Przy kolejnych codziennych wpisach różnica licznika wyznacza dzienny dystans. Gdy pominięto dni, aplikacja podaje zakres odczytów i łączny dystans między nimi. Nie przypisuje całego odcinka do ostatniego dnia ani nie rozdziela go automatycznie. Koszt samochodu dla takiej luki jest niepełny również w podsumowaniu tygodniowym/miesięcznym, dopóki nie uzupełnisz odczytów. Znane odcinki i czas agregatu nadal są uwzględniane.

Korekta wymaga zaznaczenia potwierdzenia w sekcji **Korekta lub nietypowy wpis**. Formularz pokazuje wpływ zmiany licznika na sąsiednie odcinki. Po zapisaniu zmienione odcinki przeliczają się, ale zachowują normy i ceny zapisane z ich dniami. Potwierdzenia wymaga też jazda powyżej średnio 1000 km/dobę między odczytami lub agregat powyżej 16 h/dobę. Nie można zatwierdzić malejącego licznika, odczytu sprzecznego z tankowaniami ani czasu powyżej 24 h/dobę.

## Parametry

Ustawienia → Koszty → **Dziennik licznika, agregat i zdjęcia rachunków**, następnie **Spalanie i ceny**; można też wejść przez **Tankowania i historia** na dashboardzie (`/fuel`).

Podaj spalanie samochodu w l/100 km, zużycie agregatu w l/h, rodzaj paliwa i cenę litra osobno dla każdego urządzenia. Obsługiwane są benzyna, olej napędowy i LPG, jeden samochód i jeden agregat. Nowe parametry mogą obowiązywać od dziś lub przyszłej daty. Nie nadpisują wcześniejszych wersji. Jeśli kilka wersji obowiązuje od tej samej daty, nowe wpisy stosują ostatnią; zapisane dni zachowują swoje wartości.

Pierwszy odczyt można zapisać po ustawieniu parametrów obowiązujących w tym dniu. Nie ma w tej wersji retroaktywnego wprowadzania norm z datą wcześniejszą niż dziś. Dziennik zaczynamy od punktu początkowego; wcześniejsze ręczne koszty pozostają dostępne.

- Samochód: `km × l/100 km ÷ 100`.
- Agregat: `liczba minut ÷ 60 × l/h`, bez zaokrąglania czasu do pełnych godzin.
- Szacowany koszt urządzenia: litry × jego zapisana cena litra, zaokrąglony do groszy.

## Szacunki, zakupy i wcześniejszy formularz

Dashboard oddzielnie pokazuje znane kilometry, czas agregatu, szacowane litry/koszty każdego urządzenia i wydatki na zakupy paliwa dla wybranego okresu. **Rachunki nie są ponownie odejmowane od przychodu.** Zakup do baku lub zapasu nie oznacza zużycia paliwa w tym dniu.

Dotychczasowy formularz w Ustawienia → Koszty nadal działa i zachowuje dane. Kolejność źródeł jest jawna:

1. W trybie Wykonanie ręczna kwota `Rzeczywisty koszt` zastępuje **cały** koszt paliwa dnia (samochód i agregat).
2. Bez tej kwoty wcześniejsze ręczne kilometry mają pierwszeństwo przed odczytem licznika w odpowiednim trybie Plan/Wykonanie. Do ich kosztu dodaje się agregat dokładnie raz.
3. Gdy nie podano ręcznych km, używany jest znany dzienny dystans z licznika i czas agregatu.

Przy aktywnej konfiguracji agregatu brak dziennika nie jest automatycznie zerowym zużyciem. Wynik jest oznaczony jako niepełny, chyba że zastępuje go jawna kwota całego dnia. Dla starszych okresów bez konfiguracji urządzeń zachowano dotychczasowe wyliczenia. W karcie dziennika pokazany jest komunikat o wcześniejszych ręcznych danych i link do ich poprawienia/usunięcia. Szacunki urządzeń są wyświetlane niezależnie od kosztu zastępczego, z odpowiednim opisem.

## Tankowania i porównanie spalania

**+ Tankowanie / rachunek** pozwala wpisać datę i godzinę, przeznaczenie oraz kwotę. Samą kwotę można zapisać bez litrów i zdjęcia. Litry, cena litra, licznik, tankowanie do pełna i potwierdzenie kompletności są w rozwijanej sekcji pomiaru. Cena litra wynika z kwoty/litrów; przy wpisaniu wszystkich wartości serwer sprawdza ich zgodność z tolerancją 0,02 zł/l. Korekta tankowania jest dostępna w historii miesiąca. Chronologia jest ustalana według czasu i identyfikatora, więc wiele tankowań jednego dnia nie jest sumowanych przypadkowo jako jedno zdarzenie.

Porównanie **pełny–pełny**:

- Początkowe pełne tankowanie rozpoczyna pomiar; jego litry są wyłączone z sumy.
- Liczymy kolejne tankowania samochodu, w tym częściowe i końcowe pełne.
- Dystans pochodzi z liczników przy obu pełnych tankowaniach.
- Końcowe tankowanie musi mieć zaznaczone potwierdzenie wpisania wszystkich zakupów od poprzedniego pełnego tankowania.
- Brak litrów, niepotwierdzona kompletność, zerowy dystans lub różne rodzaje paliwa oznaczają niepełny pomiar. Wydatki pozostają zapisane.
- Przy kompletności danych: `suma litrów × 100 / dystans`. Jeżeli istnieje jedna jednoznaczna norma dla całego odcinka, pokazujemy również oczekiwane litry, różnicę w litrach i procentach. Zmiana normy/rodzaju paliwa w trakcie odcinka może uniemożliwić to porównanie bez rozdzielenia kilometrów.
- Wzrost powyżej 10% powoduje neutralną podpowiedź sprawdzenia danych i warunków jazdy; nie jest diagnozą wycieku ani kradzieży.

Agregat ma na tym etapie wyłącznie szacunek na podstawie czasu. Do rzeczywistego bilansu potrzebny byłby pomiar ilości dolanej i stanu początkowego/końcowego. Zakupy do **wspólnego zapasu** są pokazane jako wydatki, ale nie przypisują automatycznie zużycia urządzeniom. Nie ma magazynu, OCR ani GPS.

## Zdjęcia rachunków

Można zrobić zdjęcie aparatem telefonu lub wybrać plik. Obsługiwane są JPEG i PNG do 8 MiB, do 20 megapikseli. Serwer sprawdza zawartość i wymiary przed pełnym dekodowaniem, następnie ponownie koduje obraz do JPEG bez oryginalnych metadanych. Sama nazwa i deklarowany MIME nie decydują o przyjęciu pliku; HEIC trzeba wcześniej przekonwertować.

Zdjęcie jest wysyłane osobno, więc błąd uploadu nie kasuje formularza; przycisk **Ponów przesyłanie** wykorzystuje wybrany plik. Po udanym uploadzie jego identyfikator jest zachowany także przy błędzie walidacji tankowania. W trakcie uploadu zapis i wybór kolejnego pliku są blokowane. Podgląd, usunięcie i zastąpienie zdjęcia nie wymagają opuszczania formularza.

Obrazy są przechowywane jako prywatne `BYTEA` w PostgreSQL, pod losowym UUID, poza publicznymi zasobami aplikacji. Nie ma ścieżek plików opartych na nazwach użytkownika. Endpoint wymaga OWNER; odpowiedź ma `no-store`, typ `image/jpeg` i `nosniff`. Odłączenie lub zastąpienie zdjęcia usuwa stary obraz dopiero po poprawnym zapisie tankowania. Nieużyte uploady starsze niż 24 h są sprzątane przy kolejnym udanym uploadzie. Kopia zapasowa PostgreSQL musi uwzględniać również tabelę zdjęć.

## Zapis, migracja i Windows

Flyway dopisuje **V8**, bez zmiany V1–V7 i bez kasowania wcześniejszych kosztów. Licznik oraz tankowania mają kontrolę wersji, wpisy zmian zapisują audyt, a tankowania i parametry mają tokeny zabezpieczające przed podwójnym utworzeniem. Modyfikacje dziennika, zakupów i usuwanie zdjęć używają wspólnej blokady transakcyjnej, aby równoległe żądania nie naruszały chronologii ani przypisań zdjęć.

Nie dodano publicznego trybu demo ani dodatkowych haseł. Spring Security, OWNER i CSRF pozostają aktywne. Dane produkcji nclean.pl nie są używane.

Uruchomienie na Windows pozostaje zgodne z [WINDOWS.md](WINDOWS.md): `scripts/Initialize-Local.ps1`, `scripts/Start-Local.ps1`. Po zalogowaniu otwórz Podsumowanie. Nie jest potrzebny nowy katalog załączników ani dodatkowy wolumen — zdjęcia zapisują się w istniejącym PostgreSQL. Nie usuwaj wolumenu bazy podczas restartu.

## Weryfikacja 11.11.2026

Java 17, Maven Wrapper 3.9.11, `verify`: **52 testy, 0 porażek, 0 błędów, 0 pominięć** — 18 jednostkowych/MVC i 34 integracyjne z PostgreSQL/Testcontainers. Obejmują początkowy odczyt, luki, uzupełnianie dni, korekty i historię, walidację, nietypowe wpisy, normy/ceny historyczne, przeliczenie godzin i minut, pełny–pełny z częściowymi zakupami, kwotę bez litrów, paliwa i wspólny zapas, duplikaty/konflikty, prywatność i treść zdjęć, brak podwójnego naliczania oraz restart.

Playwright w prawdziwej aplikacji: **360, 390, 768 i 1440 px**. Zapis dnia i korekty; generator 1 h 30 min = 2,25 l przy 1,5 l/h; samochód 120 km = 9,6 l przy 8 l/100 km; koszt 57,60 + 15,75 = 73,35 zł. Pełny–pełny z częściowym tankowaniem: 10 l / 120 km = 8,333 l/100 km, +4,17% względem normy. Przerwanie uploadu i ponowienie, błąd kwoty/ceny zachowujący obraz i notatkę, dostęp do JPEG po zalogowaniu, brak dostępu anonimowego, brak podwójnego odliczenia rachunków i brak poziomego przewijania. Odczyty, porównania i zdjęcie pozostały po pełnym restarcie aplikacji.

Testy przeglądarkowe używają wyłącznie tymczasowego schematu w lokalnym PostgreSQL, usuwanego po sprawdzeniu. Testcontainers uruchamia osobny kontener z losowymi poświadczeniami. Środowisko nie ma udostępnionego podglądu serwera w panelu Codexa; testy wykonano w Chromium, nie w publicznej przeglądarce użytkownika.

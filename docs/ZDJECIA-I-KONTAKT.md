# Zdjęcia, źródło klienta i ponowny kontakt

## Codzienna obsługa

- **Umawianie:** wybierz usługi i klienta. Dla nowego klienta można wybrać źródło: Google/Mapy, Facebook, polecenie, OLX, inne lub nieustalone. Polecenie ma opcjonalne „od kogo”. Wybór istniejącego klienta ukrywa te pola; kolejne zlecenie nie zmienia pierwszego źródła. Źródło można świadomie poprawić w edycji kartoteki.
- **Notatka:** w rozwijanych ustaleniach formularza zlecenia można wpisać np. piętro, parking, wodę/prąd. Jest to istniejąca notatka konkretnej wizyty (`work_note`), dostępna potem w wykonaniu i historii klienta. Nie dodano osobnego obowiązkowego formularza adresowego. Korekta notatki przy zapisie wykonania jest objęta istniejącym audytem.
- **Zdjęcia:** w szczegółach zlecenia rozwiń „Zdjęcia przed i po”. Wybierz etap, zdjęcia z galerii lub aparat i dodaj. Można przesyłać kilka plików kolejno, do 40 zdjęć na zlecenie. Sekcja ma licznik i pozostaje rozwinięta podczas pracy; reszta ekranu nie jest stale zajęta galerią.
- **Szukaj:** krótki przycisk w nagłówku i w kartotece prowadzi do jednego wyszukiwania klientów oraz zleceń po nazwie, telefonie lub adresie. Minimum dwa znaki. Wynik od razu otwiera klienta albo wizytę. Nie łączymy osób na podstawie telefonu.
- **Ponowny kontakt:** Klienci → Ponowny kontakt (także skrót w Więcej). Lista zawiera aktywnych klientów, których ostatnia wykonana usługa była co najmniej sześć miesięcy temu, bez zaplanowanej wizyty dziś lub później. Można zadzwonić, otworzyć historię/ponowne umówienie oraz zapisać notatkę i przyszłą datę kolejnego kontaktu. Zapis odkłada klienta do tej daty. Historia kontaktu jest w kartotece. Nie ma automatycznej wysyłki.

Źródło pozostaje „Nieustalone” dla dawnych klientów. Nie przypisujemy im wstecznie kanałów ani dat kontaktu. Eksport klientów CSV zawiera nowe pola źródła i kontaktu; kopia PostgreSQL obejmuje również zdjęcia.

## Czas z nazw zdjęć

Obsługiwane przykłady:

- `IMG_20261111_103025.jpg`
- `PXL_20261111_103025.123.jpg`
- `2026-11-11_10-30-25.png`

Z nazw odczytywana jest pełna data i godzina. Nie używamy godziny przesłania jako godziny usługi ani nie zgadujemy dat z `IMG_1234.jpg`. EXIF nie jest źródłem czasu w tej wersji.

Najwcześniejsze zdjęcie Przed jest początkiem, najpóźniejsze Po końcem. Początek musi dotyczyć dnia wizyty, koniec nie może poprzedzać początku ani być od niego dalszy niż 24 godziny. Nazwy są interpretowane jako lokalne godziny Europe/Warsaw; niejednoznaczne/nieistniejące godziny zmiany czasu wymagają ręcznej korekty. Przejście między prawidłowymi przesunięciami jest liczone według rzeczywistych sekund. Rozpoczęta minuta jest zaokrąglana w górę.

Jeżeli choć część zdjęć ma nierozpoznaną nazwę, brakuje etapu albo daty są sprzeczne, automatyczny czas pozostaje nieznany. Wpisany wcześniej automatyczny czas jest wtedy wycofywany, z czytelnym komunikatem. Zdjęcia zostają zapisane i można ustawić godziny ręcznie w rozwinięciu galerii lub czas w minutach w wykonaniu.

Automatyka działa wyłącznie dla czasu nieznanego albo wcześniej pochodzącego ze zdjęć. **Ręczne dane i timer mają pierwszeństwo.** Dodanie/usunięcie zdjęcia nie nadpisuje ich. Ręczna korekta godzin zapisuje źródło MANUAL; edycja samej liczby minut w wykonaniu również zmienia źródło, gdy czas został poprawiony. Zachowanie ceny końcowej bez zmiany czasu nie zmienia źródła PHOTO.

Uruchomienie rzeczywistego timera po czasie szacowanym ze zdjęć rozpoczyna pomiar od nowa, zamiast dodawać pomiar do oszacowania. Zmiana dnia wizyty usuwa automatyczny czas ze zdjęć, jeśli nazwy nie odpowiadają nowej dacie; zdjęcia i audyt pozostają. Zmiany czasu aktualizują pojedynczą pozycję usługi lub usuwają stary podział pakietu, aby nie prezentować błędnego wyniku/h.

To przybliżenie na podstawie zdjęć, nie pomiar potwierdzający faktyczną pracę. Błędna godzina aparatu lub zmieniona nazwa może dać błędną sugestię — zawsze można ją poprawić.

## Prywatność, zapis i błędy

Zdjęcia są prywatnymi JPEG-ami w PostgreSQL (`booking_photo`), pod losowym UUID. Oryginalna nazwa jest wyłącznie metadanymi do odczytu czasu i wyświetlenia, nie ścieżką pliku. Wspólny walidator obrazu z rachunkami sprawdza zawartość, rozmiar do 8 MiB i wymiary do 20 MP, następnie koduje JPEG bez oryginalnych metadanych. Obsługiwane JPEG/PNG; HEIC wymaga konwersji. Endpoint obrazu wymaga OWNER, ma no-store i nosniff. Nie dodano publicznych adresów zdjęć.

Uploady mają token na plik i kontrolę wersji zlecenia. Ponowienie po błędzie sieci nie tworzy kolejnej kopii już przyjętego pliku. W czasie przesyłania etap i wybór plików są zablokowane. Po udanym dodaniu, zmianie godzin lub usunięciu galerii odświeżany jest tylko fragment i wersje formularzy — wpisana robocza notatka/cena/ilości pozostają. Przy zmianie zlecenia w innym oknie aplikacja zgłasza konflikt zamiast bezwarunkowo odświeżać wersję lokalnego formularza.

Czas ze zdjęć ma zapis źródła i trafia do audytu zlecenia. Potwierdzenie wykonania, cena i płatności pozostają oddzielnymi czynnościami. Zdjęcia nie oznaczają automatycznie wykonania ani zapłaty.

V10 dodaje pola/tabele; V1–V9 pozostają niezmienione. Zdjęcia usług są osobne od rachunków paliwowych i nie są usuwane przez sprzątanie niepowiązanych uploadów rachunków. Nie korzystano z produkcji nclean.pl.

## Weryfikacja i podgląd

Java 17, Maven Wrapper, **67 testów: 23 jednostkowe/MVC i 44 PostgreSQL/Testcontainers**, bez błędów, porażek i pominięć. Obejmują formaty nazw i błędne daty, oba etapy, automatyczny czas, ręczną korektę i timer, zmianę dnia, prywatny/durable obraz, ponowienia i konflikty wersji, źródło/notatki, wyszukiwanie, odkładanie kontaktu i regresję dotychczasowych funkcji.

Chromium, rzeczywista aplikacja, 360/390/768/1440 px: nowe zlecenie ze źródłem/poleceniem/notatką; zdjęcia dające 40 min, ręczna zmiana do 60 min, kolejne zdjęcie zachowujące ręczny czas i roboczą notatkę; wyszukiwanie telefonu/adresu, historia klienta, ponowny kontakt i odłożenie na przyszłość. Zdjęcia oraz czas pozostały po restarcie. Testowy schemat został usunięty.

Nowy plik HTML powstaje z rzeczywistych widoków i CSS aplikacji. Test obejmuje również zagnieżdżony sandbox allow-scripts: podgląd zdjęcia, lokalne dodanie, ręczny priorytet, wyszukiwanie i formularz kontaktu. W pliku operacje są demonstracyjne; nie zapisuje danych do PostgreSQL ani nie synchronizuje ich z aplikacją. Lokalne zdjęcia nie są wysyłane. Podgląd serwera Codexa pozostaje niedostępny. Prawdziwą aplikację uruchamia się według WINDOWS.md.

Następny etap upraszczania warto oceniać według trzech czynności: szybkie umówienie, udokumentowanie wykonania i krótkie domknięcie dnia. Nowe opcje są już opcjonalne/rozwijane; nie dodano obowiązkowych kroków ani drugiej nawigacji.

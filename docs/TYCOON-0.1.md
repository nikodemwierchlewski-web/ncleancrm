# ncLEAN CRM 0.1 — Cleaning Tycoon

Tycoon jest wspólnym wyglądem aplikacji i działa bez flag. Kalendarz pozostaje ekranem startowym. Stan walidacji wydania i ograniczenia będą zapisane w `TYCOON-0.1-VALIDATION.md`; wcześniejsze audyty opisują poprzednie wersje.

## Codzienna obsługa

1. **Rozmowa:** Kalendarz → dzień → + Zlecenie. Wybierz usługi z dowolnych kategorii, ilości i uzgodnione ceny. Zmiana kategorii zachowuje zaznaczenia. Wyszukaj klienta po telefonie/nazwie albo wpisz nowy kontakt. Skoryguj adres tej wizyty i zapisz raz.
2. **Plan:** telefon pokazuje pasek tygodnia i karty dnia; dotknięcie daty rozwija miesiąc. Komputer pokazuje miesiąc i panel dnia. Obszar dnia jest opcjonalny; kilka obszarów ma nazwy i paski kolorów.
3. **Po wizycie:** szczegóły → Wykonanie. Ten sam formularz działa w zamykaniu dnia: status, kwota, rzeczywista chemia, notatka lub Bez uwag, zdjęcia Przed/Po lub jawny powód braku. Czas jest opcjonalny. Przyszłego terminu nie można oznaczyć jako wykonanego.
4. **Pakiet:** zmiana kwoty/czasu może wymagać poprawienia zapisanego podziału. Komunikat otwiera pozycje. Możesz poprawić je albo świadomie usunąć bieżący podział; historia pozostaje.
5. **Koniec dnia:** Dzisiaj → Uzupełnij dzień. Przejdź przez rzeczywiste braki, a następnie Zamknij dzień. Wpłaty rozliczasz oddzielnie od kompletności dokumentacji.

## Sesja i błędy

Przed świadomym zapisem aplikacja sprawdza sesję i pobiera świeży CSRF. Jeśli sesja wygasła, pozostaw kartę ze szkicem otwartą, użyj linku logowania w nowej karcie, a następnie ponów zapis. Żądanie zapisu nie jest automatycznie odtwarzane. Token rezerwacji pozostaje ten sam; ponowienie po utracie odpowiedzi nie tworzy kolejnej rezerwacji. Konflikt wersji nadal wymaga porównania z aktualnymi danymi; aplikacja nie podstawia nowej wersji za użytkownika.

Szkic i oczekujące pliki pozostają w pamięci otwartej karty. Odświeżenie, zamknięcie przeglądarki lub systemowe usunięcie karty może je utracić. Przy zmienionym formularzu przeglądarka może ostrzec przed wyjściem. CRM nie zapisuje danych klientów ani zdjęć w localStorage i nie ma prywatnego trybu offline.

## Pozostałe miejsca

- **Klienci:** kontakty i historia, źródło pozyskania, wyszukiwanie i ponowny kontakt. Lista ma strony po 25 rekordów; wyszukiwanie obejmuje całą bazę.
- **Więcej → Koszty:** wydatki, inwestycje, samochód/agregat, tankowania i rachunki. Zakup i zużycie pozostają odrębnymi informacjami, bez podwójnego naliczania.
- **Więcej → Notatki:** memo, zadania i cele; przypięcie do podsumowania.
- **Ustawienia:** cennik/kategorie, chemia, obszary i parametry. Edycja standardu nie zmienia historycznych cen ani pomiarów.
- **Ruch:** przycisk ograniczenia animacji; systemowe prefers-reduced-motion ma pierwszeństwo.

## Zdjęcia i uruchamianie

JPEG/PNG do 8 MiB i 20 MP. Obraz jest obracany zgodnie z orientacją EXIF, kodowany ponownie jako JPEG i przechowywany prywatnie w PostgreSQL. Oryginalna nazwa pozostaje do analizy czasu. HEIC trzeba najpierw wyeksportować do JPEG; brak obsługi HEIC nie oznacza pomyślnego dodania zdjęcia.

[Windows i Maven Wrapper](WINDOWS.md). [Staging, HTTPS, sekrety i kopie na własnym serwerze](../deploy/README.md). W tej pracy nie podłączano produkcji nclean.pl i nie wykonywano wdrożenia. Lokalny Compose używa wyłącznie loopbacka i oddzielnej bazy.

Podgląd ZIP jest demonstracją z danymi syntetycznymi i symulowanym zapisem w pamięci. Nie zastępuje działającej aplikacji ani PostgreSQL. Po pobraniu wypakuj ZIP i otwórz `index.html`.

# Proste tankowania i koszt paliwa w wyniku firmy

Aktualizacja z 16.11.2026. Poprzedni wynik rzeczywisty liczył szacowane zużycie
z dziennych odczytów, a zakup paliwa pokazywał osobno. Dlatego dopisany rachunek
z października nie pomniejszał październikowego wyniku.

## Codzienny zapis

1. Wybierz **Samochód — diesel** albo **Agregat — benzyna**.
2. Sprawdź datę tankowania.
3. Podaj aktualny przebieg auta lub **łączny** licznik motogodzin agregatu.
4. Wpisz **litry i kwotę z paragonu**. Cena litra oblicza się automatycznie.
5. Zapisz. Notatka i zdjęcie paragonu są opcjonalne i schowane.

Nowy wpis oznacza tankowanie do pełna; nie trzeba zaznaczać osobnych potwierdzeń
ani wybierać rodzaju paliwa. Przy starcie, kiedy zbiorniki są pełne, wpisz stan
początkowy każdego urządzenia. Stan początkowy sam nie tworzy wydatku.
Jeżeli masz paragon za pierwsze napełnienie, zapisz zakup osobno.

Przykład: auto startuje z 100 000 km, a przy kolejnym pełnym tankowaniu ma
100 500 km i dolano 40 l — spalanie wynosi 8 l/100 km. Agregat startuje ze
100 mth; przy 110 mth dolano 10 l — zużycie wynosi 1 l/mth. Brak przyrostu
licznika nie jest zamieniany w zgadywane spalanie. Zakup nadal jest kosztem.

## Wynik firmy

- **Wykonanie:** zapisane, nieanulowane rachunki za paliwo pomniejszają wynik
  dokładnie w dniu tankowania. Wpis z października obniża październik, także
  jeśli został dopisany później i tego dnia nie było żadnej usługi.
- **Plan:** dawne prognozy pozostają prognozami. Nie są dodawane drugi raz do
  rzeczywistego kosztu zakupu paliwa. Również sam pomiar spalania nie tworzy
  drugiego kosztu.
- Link **Wynik miesiąca** z tankowań otwiera podsumowanie całego wybranego miesiąca.
- **Korekta/usunięcie/przywrócenie:** przelicza odpowiedni dzień i okres.
  Usunięcie jest odwracalne; historia i paragon pozostają zachowane.
- Książeczka kosztów i dashboard korzystają z tych samych wpisów. Nie dopisuj
  ponownie tego samego zakupu jako innego wydatku. Jeśli na jednym paragonie
  są oba paliwa, wpisz osobno kwotę pozycji diesla i osobno benzyny.

Nowa podstawa liczenia może zmienić stare podsumowania: używają zapisanych
rachunków zamiast dawnych norm lub ręcznego kosztu zastępczego. Kwoty i daty
samych historycznych rachunków nie zostały zmienione. Nie trzeba wpisywać ich
ponownie. Pozostałe koszty i historyczne ceny usług liczone są jak wcześniej.

## Zachowanie danych

Flyway **V20** dodaje motogodziny przy tankowaniu i osobne stany początkowe.
Nie zmienia V1–V19, wcześniejszych rachunków ani zdjęć. Dawne odczyty dzienne
i parametry są dostępne w archiwum; nie tworzymy z nich fikcyjnego licznika
motogodzin. Starsze wpisy zachowują zapisany typ paliwa.

Liczniki muszą być spójne z historią. Zapis i korekta są transakcyjne, ponowienie
tego samego zapisu nie tworzy drugiego kosztu, a zmieniony formularz po już
zatwierdzonym zapisie jest odrzucany zamiast potwierdzać inne dane.

## Weryfikacja

- Maven/Java 17/PostgreSQL: **192 testy — 66 jednostkowych/MVC/architektury
  i 126 integracyjnych, bez błędów i pominięć**.
- Chromium: dwa niezależne liczniki, cena z kwoty i litrów, awaria 503 i ponowienie,
  październikowy wynik, edycja, usunięcie i przywrócenie, rzeczywiste spalanie;
  szerokości 360, 390, 768, 1440 px.
- Docker: migracje do V20, restart, odtworzenie kontenerów, backup i import do
  nowej bazy; porównanie danych obejmuje nowe liczniki i tankowanie agregatu.
- Samodzielny HTML: dodawanie/edycja/usuwanie/przywracanie, przełączanie
  październik/listopad, błędny licznik z zachowaniem pól, cztery szerokości,
  izolowany sandbox czatu, bez żądań do sieci i błędów JavaScript.

Testy wykonano na Linux; nie jest to deklaracja próby na fizycznym Windows.
Podgląd [HTML](../preview/nclean-full-tank.html) zawiera wyłącznie syntetyczne
dane przechowywane w pamięci karty. Jest generowany z widoku prawdziwej aplikacji.
Nie służy do zapisywania klientów ani rachunków firmy.

Aktualizacja prawdziwego CRM: [DOCKER-PC.md](DOCKER-PC.md). Przed uruchomieniem
nowego wydania wykonaj backup; skrypt `CRM-PC.ps1 -Action Update` robi go przed
budowaniem i startem aplikacji. Podgląd HTML nie aktualizuje instalacji na PC.

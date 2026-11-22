# TEST — wartość zleceń, powroty i rodzaj klienta

Baza: `nclean-v0.1-test.5` (`92df5e2`). Gałąź:
`feat/customer-value-and-returning`. Nowy tag: `nclean-v0.1-test.6`.

- Podsumowanie: średnia i mediana wartości wykonanych zleceń, ogółem
  i według obecnych kategorii usług, dla wybranego dnia/tygodnia/miesiąca.
  To wartości realizacji/części realizacji, obejmujące ich ilości usług.
- Kilka usług w jednej kategorii tworzy jedną próbkę. Przy kilku kategoriach
  korzystamy z kompletnego podziału kwoty końcowej na pozycje. Brak podziału
  wyklucza tę wizytę z kategorii, z jawnym licznikiem; ogółem pozostaje ujęta.
  Brak ceny końcowej oznacza dotychczasową wycenę i widoczne ostrzeżenie.
- Powroty: nowy klient ma pierwszą zapisaną wykonaną wizytę w okresie;
  powracający ma wcześniejszą. Osobno liczba/przychód kolejnych wizyt,
  również przy powrocie nowego klienta w tym samym okresie, i mediana
  odstępu między zapisanymi wizytami. Niepełna historia może zmienić wyniki.
- Rodzaj klienta: prywatny, biznesowy, nieustalony. Wybór w nowym zleceniu
  zapisuje profil i kopię dla wizyty; wyszukiwanie/ponowienie podpowiada profil.
  Późniejsza korekta kartoteki pozostawia typ dawnych wizyt bez zmian.
  Zapis profilu i wizyty jest wspólną transakcją z kontrolą wersji i ponowienia.
- Filtr rodzaju dotyczy tej sekcji statystyk i typu zapisanego w zleceniu.
  Statystyki wykonania są jawnie oznaczone także w trybie Plan.
- V24 dodaje wyłącznie rodzaj klienta w kartotece i zleceniu; stare dane
  pozostają „Nieustalone”. V1–V23 i ceny, zdjęcia, notatki oraz odciski
  wcześniej zamkniętych dni pozostają zachowane. Konfiguracja Dockera bez zmian.

## Weryfikacja

Java 17, `./mvnw verify`: **250 testów**, 76 jednostkowych/architektury/uprawnień
oraz 174 PostgreSQL, bez błędów, porażek i pominięć. Nowe testy obejmują m.in.
medianę i grosze, ilości/pakiety/kategorie, podział kwot, wcześniejszą historię
innego typu, powrót tego samego dnia, braki danych, usunięcie/przywrócenie,
wersje/replay, pełny rollback i migrację V23→V24 wraz z zamkniętym dniem.

Chromium na prawdziwej aplikacji: `/` i `/crm`, po 20 responsywnych widoków
(360/390/768/1440 px), wybór i podpowiedź typu, utracona odpowiedź/ponowienie,
niezmienność starej wizyty, średnie/mediany, jawny podział kategorii, powroty,
filtry i nawigacja okresów, zachowanie pól przy konflikcie profilu, ograniczenie
animacji i odczyt bez JS — poprawnie, bez błędów JavaScript.
Wyłącznie nowe izolowane bazy i dane syntetyczne; bez połączenia z produkcją.

Nie wykonano wdrożenia. Aktualizacja PC wymaga backupu i przebudowy obrazu
z zachowaniem rzeczywistego podłączenia bazy, istniejącego wolumenu i `.local`.

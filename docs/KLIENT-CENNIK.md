# Klienci, cennik i szybkie umawianie

Na kalendarzu dostępne są przyciski **Zlecenia / szybkie umawianie**, **Klienci** i **Cennik**. W panelu wybranego dnia przycisk **Zlecenia tego dnia** przechodzi do jego rezerwacji.

## Cennik

W **Cennik** można dodać kategorię lub zmienić jej nazwę. Dodając i edytując usługę, wybierasz kategorię, własną nazwę, jednostkę (np. szt., m², pakiet), kwotę do zapłaty PLN i orientacyjny czas na jednostkę. Możesz wyłączyć usługę, zachowując ją w starych zleceniach. Zmiany zapisz przyciskiem **Zapisz usługę**. Pierwszy katalog zawiera 17 przykładowych usług; przed użyciem ustaw własne ceny.

Ceny i ilości mogą mieć dwa miejsca po przecinku. Wycenę zlecenia oblicza serwer na podstawie aktualnego cennika. Zapisane zlecenie przechowuje własne nazwy, jednostki, ilości i ceny. Zmiana cennika oraz terminu lub statusu starego zlecenia nie przelicza jego kwoty. Katalog jest importowany migracją V3 tylko raz; start aplikacji nie nadpisuje zmian w panelu. Kwoty są uzgodnionymi kwotami do zapłaty, bez modułu faktur i rozliczeń VAT.

## Klienci

Kartoteka pozwala dodawać i edytować imię/nazwę, telefon, adres domyślny, notatkę i aktywność. Wyszukiwanie działa po nazwie i telefonie. Polski numer 9-cyfrowy otrzymuje +48; pozostałe numery wymagają prefiksu międzynarodowego. Numer telefonu nie jest unikalny: członkowie rodziny mogą używać jednego numeru. Rekordy nie są automatycznie łączone.

**Historia zleceń** otwiera rezerwacje danego klienta. Edycja kartoteki nie przepisuje kontaktu ani adresu wcześniejszych rezerwacji. Nieaktywnego klienta nie można wybrać przy nowym zapisie. Wersja rekordu chroni przed nadpisaniem zmian wprowadzonych w innym oknie — w razie konfliktu odśwież formularz.

## Szybkie umawianie

1. Wybierz kategorię i ilości usług. Suma przelicza się podczas wpisywania.
2. Wpisz fragment imienia albo telefonu i wybierz klienta z listy. Adres domyślny zostanie uzupełniony; możesz podać inny adres realizacji. Dla nowego klienta wpisz imię i telefon w tym samym formularzu.
3. Wybierz dzień i godzinę, następnie **Zapisz zlecenie**.

Klient tworzony podczas umawiania i zlecenie zapisują się w jednej transakcji. Ponowienie tego samego zapisu nie tworzy drugiej rezerwacji. Numer wpisany w wyszukiwarkę nie trafia do URL. Wymagane są logowanie OWNER i CSRF.

Na liście zleceń można zmienić dzień, godzinę oraz status (zaplanowane, wykonane, anulowane). Edycja pozycji już zapisanego zlecenia, rabaty, wiele adresów w kartotece i historia każdej zmiany katalogu nie należą do tego etapu. Czas w cenniku jest orientacyjną informacją; nie planuje automatycznie zakończenia ani dojazdu.

## Weryfikacja

`./mvnw clean verify` uruchamia testy MVC i PostgreSQL Testcontainers. Obejmują one migracje, uprawnienia/CSRF, zapis klienta i zlecenia, brak duplikatu przy ponowieniu, zachowanie historycznej wyceny i kontaktu po zmianach oraz po restarcie aplikacji. Baza testowa jest odizolowana; nie używa danych produkcji.

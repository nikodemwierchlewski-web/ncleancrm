# Rzeczywiste zużycie i propozycje standardów

## Po wykonaniu zlecenia

Oznacz zlecenie jako **Wykonane**, otwórz jego szczegóły i rozwiń **Rzeczywiste zużycie chemii**. Wpisujesz ilości dla całej pozycji: np. dla 2 foteli łącznie 120 ml. Aplikacja przeliczy to do 60 ml na jeden fotel.

Pierwsze otwarcie podpowiada zapisane ilości z prognozy, ale **nie traktuje ich automatycznie jako rzeczywistych**. Sprawdź i popraw wartości; dopiero przycisk **Zapisz rzeczywiste zużycie** potwierdza je jako faktyczne. Samo oznaczenie zlecenia jako wykonanego nie tworzy pomiarów.

- Puste pole: brak informacji o danym środku, nie jest próbką.
- 0: świadomie potwierdzony brak zużycia, jest próbką.
- **Nie użyto chemii — wpisz 0** uzupełnia zera dla widocznych środków. Wymaga jeszcze zapisu.
- Wyczyszczenie wszystkich pól i zapis usuwa pomiary tej pozycji; nie zamienia ich w zera.

Zapis nie zamyka panelu i nie usuwa szkiców innych usług. Możesz kolejno uzupełnić kilka pozycji. Po zamknięciu i ponownym otwarciu widzisz zapisane wartości; nie są ponownie zastępowane prognozą. Korekta zastępuje wcześniejszy pomiar i nie zwiększa liczby próbek. Wersja rekordu chroni przed nadpisaniem pomiaru edytowanego w innym oknie.

## Propozycje w ustawieniach

W **Ustawienia → Cennik → kategoria → usługa → Propozycje z rzeczywistego zużycia** propozycja pojawia się po co najmniej **5 różnych wykonanych zleceniach z podanymi ilościami danego środka**.

Dla każdej usługi i środka oddzielnie aplikacja:

1. Bierze jawnie zapisane rzeczywiste ilości, również 0.
2. Dzieli ilość przez liczbę jednostek usługi w danej rezerwacji.
3. Oblicza medianę wartości na jednostkę, zaokrągloną do 0,001 jednostki bazowej.
4. Pokazuje medianę, liczbę zleceń, pełny zakres minimum–maksimum i datę ostatniej usługi.

Przykład pięciu obserwacji na fotel: 0, 50, 60, 70 i 1000 ml. Propozycja wynosi 60 ml/fotel, a zakres nadal pokazuje 0–1000 ml. Nietypowe pomiary nie są po cichu usuwane; mediana ogranicza ich wpływ.

Prognozy, brak danych i anulowane/niewykonane zlecenia nie są próbkami. Nie są mieszane różne usługi ani jednostki. Po zmianie jednostki usługi dane z poprzedniej jednostki nie trafiają do aktualnej propozycji. Dla nowego środka w katalogu brak pomiaru w dawnym zleceniu nie jest domyślnym zerem.

**Zastosuj jako standard** przelicza propozycję na serwerze i zmienia standard wybranego środka, pozostawiając pozostałe składniki. Aplikacja nie zmienia standardów samodzielnie. Nowe wyceny korzystają z nowego standardu; dawne prognozy, ceny i rzeczywiste pomiary pozostają bez przeliczenia.

## Dane i ograniczenia

Migracja V6 dodaje oddzielny zapis rzeczywistych ilości. Nie kopiuje do niego starych prognoz. Przeliczenia korzystają z BigDecimal; podstawowe jednostki to ml, g i szt. Zapisane pomiary blokują zmianę rodzaju jednostki środka (np. ml → g); dodaj wtedy osobny środek.

To propozycje na podstawie własnych potwierdzonych danych. Nie jest to fizyczny pomiar z urządzenia ani automatyczne zużycie magazynu. Nie dodano finansów, GPS lub SMS. Wszystko zostaje w PostgreSQL CRM i wymaga OWNER oraz CSRF.

## Sprawdzenie

`./mvnw verify`: **35 testów, 14 MVC/security i 21 PostgreSQL Testcontainers; 0 niepowodzeń, błędów i pominiętych**.

Sprawdzono próg 5, zera i brak pomiaru, dzielenie przez ilość usługi, medianę i zakres, korekty bez podwajania próbek, pomijanie anulowanych zleceń i innych jednostek, zastosowanie wyniku liczonego przez serwer, niezmienione stare prognozy, walidację, konflikty wersji, uprawnienia/CSRF i restart.

Chromium/Playwright z rzeczywistą aplikacją i PostgreSQL w tymczasowym schemacie: wpisanie 5 pomiarów przez UI, zastosowanie propozycji, prognoza kolejnej wizyty według nowego standardu, zapis dwóch usług bez zamykania panelu, ponowne otwarcie/korekta, historia po restarcie oraz 360, 390, 768 i 1440 px. Nie stwierdzono błędów JavaScript, żądań zewnętrznych ani poziomego przewijania. Schemat po teście usunięto; dotychczasowe dane i produkcja pozostały nietknięte.

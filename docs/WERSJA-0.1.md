# ncLEAN CRM 0.1 — dzień pracy

## Co jest zintegrowane

Gałąź integracyjna bazuje na `feat/fuel-log-receipts`, a więc zawiera wcześniejsze etapy klientów/cennika/zleceń, responsywnego kalendarza, planów obszarów, chemii, dashboardu i dziennika paliwa. Nie usunięto danych ani nie zmieniono wykonanych migracji V1–V8. Nowa V9 dodaje wykonanie, czas, wpłaty i audyt. `main` nie jest automatycznie scalany.

To wersja do lokalnego odbioru właściciela, nie gotowe publiczne wdrożenie. PostgreSQL, OWNER, CSRF i logowanie są zachowane. Nie ma SMS, GPS, OCR, podatków, faktur, synchronizacji offline ani danych produkcyjnych nclean.pl.

## Rano i podczas rozmowy

Po zalogowaniu ekranem startowym jest **Dzisiaj** (`/today`). Można wybrać wcześniejszy dzień. Widać obszary pracy, najwcześniejsze niezakończone zlecenie, listę chronologiczną oraz przyciski dodawania, telefonu i szczegółów. Wcześniejsza niewykonana pozycja nie znika tylko dlatego, że minęła jej godzina. Aktywny pomiar czasu ma oddzielny skrót.

Nawigacja: Dzisiaj, Kalendarz, Klienci, Więcej. Na telefonie pasek dolny ma odstęp na bezpieczny obszar systemowy; na komputerze pozostaje u góry. Więcej prowadzi do podsumowania, paliwa, cennika, obszarów, kosztów i eksportów.

**+ Zlecenie** wykorzystuje istniejący formularz kalendarza, z wybraną datą. Usługi z kilku kategorii pozostają w jednej wycenie. Ich ceny są sugerowane i edytowalne. Klienta wyszukuje się raz po nazwie/telefonie. Zmiana adresu wizyty nie zmienia adresu domyślnego kartoteki; adres domyślny edytuj świadomie w Klienci. Telefon nie jest kluczem automatycznie łączącym osoby. Prognoza chemii jest rozwijana i nie blokuje rezerwacji.

Kalendarz zachowuje siedem kolumn i panel dnia na PC oraz tydzień/listę dnia i rozwijany miesiąc na telefonie. Wiele obszarów, ich kolory i historia, reguły tygodniowe i wyjątki pozostają dostępne.

## Wykonanie zlecenia

Otwórz zlecenie z Dzisiaj, z kalendarza przez **Wykonanie i płatności** lub z kartoteki. Jeden panel (`/work/{id}`) zawiera kontakt, adres, link do zewnętrznych map, zakres, wycenę, pomiar czasu, wykonanie i wpłaty. Link do map jest uruchamiany świadomie przez użytkownika; CRM nie śledzi położenia.

Możesz uruchomić opcjonalny pomiar pracy. Zapisuje on znacznik czasu na serwerze i działa po zamknięciu/odświeżeniu karty oraz restarcie aplikacji. Jednocześnie może działać tylko jeden pomiar. Zatrzymanie sumuje rozpoczęte segmenty, zaokrąglając rozpoczętą minutę w górę. Ręczne pole czasu pozwala skorygować wynik albo pozostawić go nieznanym. Zakres to 0–10080 minut. Zmiana statusu na wykonany/anulowany zatrzymuje pomiar i zapisuje audyt.

W **Zakończ zlecenie** potwierdź cenę końcową, czas i opcjonalną notatkę. Wycena rezerwacji pozostaje niezmieniona. Jeśli trwa pomiar i pole czasu pozostawiono puste, zapis uwzględnia zmierzony segment.

Chemia jest opcjonalna przy zakończeniu. Podpowiedzi są prognozami; do zapisania pomiarów trzeba zaznaczyć potwierdzenie dla konkretnej pozycji. Puste pole nie jest zerem, 0 oznacza potwierdzony brak zużycia. Używana jest istniejąca usługa rzeczywistych pomiarów, jej kontrola wersji oraz ceny historyczne. Można wykonać usługę bez kompletnych pomiarów i uzupełnić je później.

Dla pojedynczej usługi cena końcowa i czas jednoznacznie dotyczą tej pozycji. Dla pakietu podział jest opcjonalny: albo wszystkie kwoty/czasy sumują się do całości, albo pozostają puste. Nie przypisujemy całej ceny ani czasu każdej usłudze. Korekta ceny niższej od otrzymanych wpłat jest odrzucana do czasu korekty wpłat.

## Płatności

Wykonanie nie oznacza zapłaty. Wpisz rzeczywistą datę, dodatnią kwotę i metodę: gotówka, przelew lub karta. Wpłaty częściowe są sumowane; status opłacenia wynika z kwot, nie jest niezależną flagą. Nie przyjmujemy nadpłat ani przyszłych wpłat w tym etapie. Można odnotować zaliczkę przed wykonaniem, w granicach uzgodnionej wyceny.

Korekta tworzy przeciwny zapis z datą i powodem; oryginał nie znika. Potem można dopisać poprawną wpłatę. Jedna wpłata może być odwrócona tylko raz. Tokeny zabezpieczają utworzenie wpłaty/korekty przed ponowieniem; blokada wiersza chroni limit należności przy równoległych zapisach.

Historyczne wykonane zlecenia po migracji pozostają bez potwierdzonej ceny końcowej i bez wpłat. Tam, gdzie cena końcowa jest nieznana, należność i przychód mają podstawę w starej wycenie i są odpowiednio opisane. Nic nie zostaje automatycznie oznaczone jako opłacone.

## Wieczorem

Na Dzisiaj pozycje **Do uzupełnienia** prowadzą bezpośrednio do konkretnego zlecenia albo dnia paliwa/kosztów. Obejmują status, cenę końcową, czas i pomiary. Należności oczekujące na przelew są w osobnej sekcji, ponieważ nie są błędem danych. Dzień może być dowolną wcześniejszą datą. Nie ma zaplanowanej wysyłki przypomnień.

Dziennik licznika/agregatu, rachunki i szacunki paliwa działają jak opisano w [PALIWO-I-AGREGAT.md](PALIWO-I-AGREGAT.md). Zakupy paliwa nie są ponownie odliczane od wyniku operacyjnego. Wcześniejsze ręczne kwoty/km zachowują opisaną kolejność pierwszeństwa.

## Podsumowanie i usługi

Plan korzysta z wycen rezerwacji; Wykonanie z ceny końcowej, a dla niepotwierdzonych starych wykonań ze starej wyceny z jawnym komunikatem. Wpłaty są liczone według daty otrzymania/korekty, niezależnie od dnia usługi. Należności to bieżące saldo wykonanych zleceń wybranego okresu, z uwzględnieniem wszystkich obecnych wpłat — nie saldo historyczne na koniec tego okresu.

Przychód i wynik na godzinę są dostępne przy znanym dodatnim czasie i potwierdzonych cenach; wynik/h dodatkowo wymaga kompletnych kosztów. Wynik nie obejmuje podatków i nie jest saldem bankowym ani zyskiem netto.

Porównanie przychodu obejmuje poprzedni okres o tej samej liczbie dni, z podanym zakresem. Dla miesiąca 30-dniowego jest to poprzednie 30 dni, nie zawsze cały poprzedni miesiąc. Przy zerowej podstawie nie ma zmiany procentowej. Niepełne ceny poprzednich wykonań są oznaczone. Porównanie nie udaje porównania kompletnych kosztów.

Analiza usług grupuje zgodne identyfikatory/jednostki i pokazuje liczbę zleceń, ilość, braki ceny/czasu/chemii, przychód oraz kwotę po chemii i po chemii/h przy kompletnych danych. Mała próba jest jawna; nie ma rankingu sugerującego pewność przy jednym zleceniu. Paliwo, marketing i stałe pozostają wspólne. Pakiet bez jawnego podziału nie daje pozornego wyniku poszczególnych usług.

## Klient i ponowne umówienie

Historia klienta zawiera wizyty, adres wizyty, cenę i sumę wpłat; szczegóły otwierają zakres i płatności. **Umów ponownie** przygotowuje nowy formularz z klientem, adresem poprzedniej wizyty i aktywnymi usługami, ale według bieżącego cennika. Nowy termin/ceny wymagają potwierdzenia. Nie kopiujemy wpłat, czasu ani rzeczywistej chemii. Wyłączone usługi trzeba zastąpić aktywnymi.

## Formularze, eksport i PWA

Zapis jest chroniony OWNER i CSRF, wersjami rekordów oraz tokenami tam, gdzie ponowienie tworzyłoby duplikat. Przy walidacji lub awarii połączenia pola pozostają w bieżącej karcie. Nie zapisujemy klientów, notatek ani zdjęć w localStorage. Pozostań na stronie, odzyskaj połączenie i ponów; zamknięcie/odświeżenie niezapisanego formularza nie ma gwarancji zachowania jego pól.

Eksport klientów/zleceń jest dostępny dla OWNER w Więcej. CSV ma UTF-8 z BOM, separator `;`, cytowanie i neutralizację komórek mogących uruchomić formuły w arkuszu. Zawiera dane prywatne — nie dodawaj go do Git.

Manifest, ikony 192/512 i service worker pozwalają przygotować instalację jako PWA. Przycisk pojawia się tylko po zdarzeniu instalacyjnym obsługiwanym przez przeglądarkę. Service worker używa sieci, nie cache'uje prywatnych stron, API ani rachunków i nie kolejkuje zmian. Brak połączenia podczas zapisu pozostawia pola w karcie, a nawigacja offline pokazuje neutralny komunikat.

Prawdziwy telefon wymaga osiągalnego serwera HTTPS i odpowiedniej konfiguracji wdrożenia. Lokalny loopback jest bezpiecznym kontekstem testowym dla Chromium; nie dowodzi instalowalności na urządzeniu użytkownika ani udostępnienia aplikacji w internecie.

Uruchomienie, kopia i odtworzenie na Windows: [WINDOWS.md](WINDOWS.md). Wyniki odbioru: [WERYFIKACJA-0.1.md](WERYFIKACJA-0.1.md).

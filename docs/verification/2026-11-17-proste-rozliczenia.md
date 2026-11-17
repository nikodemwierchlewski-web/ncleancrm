# Weryfikacja uproszczonych rozliczeń — 17.11.2026

Zakres: wykonane zlecenie = zapłacona kwota w dniu usługi; osobny rejestr wpłat pozostaje archiwum tylko do odczytu. Tankowania auta i agregatu obniżają wynik w całości według dnia zakupu, bez dodatkowego kosztu ze spalania.

## Maven i PostgreSQL

`./mvnw verify` na Java 17, z rzeczywistym PostgreSQL 17 w Testcontainers: **BUILD SUCCESS**, 66 testów jednostkowych/MVC/architektury i 126 integracyjnych. Łącznie **192 testy, 0 błędów, 0 niepowodzeń, 0 pominiętych**, czas 3 min 55 s.

Zweryfikowano:

- wykonanie bez dopisywania wpłaty, korektę ceny w dół, niezmienność wyceny i odrzucenie nieaktualnej wersji;
- otrzymaną kwotę według dnia usługi, przeniesienie między okresami i wyłączenie anulowanych/zaplanowanych/usuniętych zleceń;
- zachowanie dawnych transakcji bez podwajania przychodu i bez blokowania korekty/anulowania/usunięcia;
- archiwum tylko do odczytu, wycofane endpointy wpłat (410), nadal wymagane OWNER i CSRF;
- restart aplikacji z zachowaniem danych;
- październikowy przychód 1000 zł i tankowania 200 + 50 zł: wynik 750 zł, po korekcie pierwszego paragonu do 210 zł: 740 zł;
- dzień bez zleceń z kosztem paliwa, różne zakresy wyników, korekty/anulowanie/przywrócenie zakupów i brak podwójnego naliczenia starych szacunków;
- osobne liczniki auta i agregatu, stałe rodzaje paliwa, prawidłowe spalanie i odrzucenie cofniętego licznika, ponowienia oraz zachowanie dawnych danych przy migracjach.

## Pozostałe kontrole

`node scripts/test-web-boundaries.cjs` — PASS. Kontrola składni zmienionych skryptów JavaScript i skryptów testowych Python — PASS. `git diff --check` — PASS.

Do prób używano wyłącznie odizolowanych, syntetycznych danych. Brak połączenia z produkcją nclean.pl. Nie zmieniano migracji, istniejącej bazy użytkownika ani wolumenów instalacji PC. Pakiet po testach ponownie zbudowano, aby uwzględnić ostateczne zasoby interfejsu.

## Prawdziwa aplikacja w Chromium

`scripts/verify-browser.py --settlement-only` — **PASS**, bez błędów JavaScript. Logowanie OWNER, rzeczywisty Spring Boot i osobny jednorazowy schemat lokalnego PostgreSQL. Sprawdzono widoki przy **360, 390, 768 i 1440 px**.

- Wprowadzenie historycznego zlecenia z 10.10.2026, zapis wykonania bez wpłaty, automatyczne „Zapłacono” i przychód w październiku.
- Brak internetu zachowuje kwotę w formularzu; utrata odpowiedzi po zatwierdzeniu na serwerze i świadome ponowienie nie podwajają przychodu. Powtórny zapis starej wersji jest odrzucany, a po odświeżeniu widoczny jest zapisany stan.
- Korekta kwoty 600 → 550 zł aktualizuje podsumowanie bez korekty wpłat.
- Brak formularzy wpłat i list należności; brak poziomego przewijania szczegółów zlecenia, podsumowania i Dzisiaj.
- Dwa stany początkowe, tankowania obu urządzeń, automatyczna cena/litr, zachowanie pól po kontrolowanej awarii 503 i udane ponowienie.
- Spadek wyniku o 250 zł, korekta do 260 zł, anulowanie i przywrócenie paragonu; osobno obliczone 8 l/100 km i 1 l/mth. Odnośnik do pełnego miesiąca poprawnie pokazuje wynik także powyżej 1000 zł.

Pierwsza kontrola wykazała wychodzenie animowanego podkreślenia zakładki poza ekran podczas zmiany szerokości. Ograniczono rysowanie tego elementu do jego nawigacji, zachowując margines na obrys fokusu. Ponowny przebieg czterech szerokości przeszedł. Poprawiono także selektor komunikatu błędu w nowym teście oraz dotychczasowe porównanie kwoty w teście paliwa, aby uwzględniało separator tysięcy.

Zrzuty zostały wizualnie sprawdzone; dowody i logi znajdują się w ignorowanej `.local/`. Nie testowano fizycznego telefonu ani instalacji na komputerze właściciela. Nie opublikowano nowego środowiska ani nie zaktualizowano zdalnie jego kontenerów.

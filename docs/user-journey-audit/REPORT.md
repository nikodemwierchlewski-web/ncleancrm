# Audyt użytkowy ncLEAN CRM — 15.11.2026

Podstawa: pełny CRM `4fde467`, nowa gałąź `audit/full-user-journey`. Main pozostaje
starszy. [Prompt](PROMPT.txt), [scenariusze i wyniki](scenarios.json),
[inwentaryzacja elementów](elements.csv), [style/geometria](controls.json).

Próby wykonano w prawdziwym Spring Boot i izolowanych schematach/bazach PostgreSQL.
Nie kontaktowano się z produkcją. HTML demonstracyjny nie zastępował prób backendu.

## Wynik

151 testów JVM: 50 jednostkowych/MVC/architektury i 101 PostgreSQL, zero błędów
i pominiętych. Node: testy adresów, szablonów/CSS i izolacji cache zaliczone.
58 skanów axe przy 360/390/768/1440 px nie zgłosiło naruszeń. Narzędzie oznacza
również przypadki wymagające ręcznej oceny — szczegóły w accessibility.json.

Przeklikano przepływy: nowy/powracający klient, szukanie, ręczny adres, usługi
z dwóch kategorii, ceny uzgodnione, osiem wizyt, termin, wykonanie/anulowanie/
przełożenie, rzeczywista chemia, zdjęcia i czas, notatki, cele/kroki/przypięcie,
koszty i sprzęt, błędy503 i ponowienie, konflikty dwóch kart, zamknięcie dnia,
późniejsze korekty i restart. Osobny przenośny runner sprawdził filtry klientów,
50 wierszy kosztów, blokadę równoczesnego zapisu i zachowanie szkicu po503.

HTTPS `/crm`: 60 kombinacji ekran/szerokość, logowanie/wylogowanie, historyczny
zapis bez fikcyjnej kartoteki, prywatne zdjęcie, eksport, CSRF403, cookie, manifest
oraz worker. Zero żądań CRM poza `/crm/`; publiczny stub i jego cache pozostały.

Upgrade V15→V16, backup i restore do nowej bazy zachowały dane, ceny, notatkę,
obrazy zdjęć/rachunków. Zweryfikowano odrzucenie złej sumy, istniejącego celu,
uszkodzonego backupu i zachowanie danych przy błędzie backupu/readiness.
Sterowanie usługą było adapterem uruchamiającym prawdziwą Javę; nie instalowano
systemd na serwerze właściciela.

## Zmiana i regresja zdjęć

Obsługa wysyłki zakładała, że w formularzu zawsze istnieje przycisk opisany
literalnym atrybutem `type=submit`. Dodano wykorzystanie `event.submitter`,
rezerwowe wyszukanie zwykłego przycisku oraz bezpieczne włączenie po zakończeniu.

Standardowe renderowane szablony mają przyciski submit: nie przedstawiamy tej
zmiany jako odtworzonej awarii zwykłego formularza. Kontrolowane usunięcie
atrybutu ujawniło kruchość poprzedniego wariantu. Nowy przenośny test
`scripts/browser_photo_checks.py`, wywoływany przez `scripts/verify-browser.py`,
sprawdza upload500/ponowienie, ręczne09:00–09:45 i usunięcie zdjęcia w czterech
szerokościach, również przy przyciskach bez atrybutu. Czas ręczny pozostaje45min
po usunięciu zdjęcia. Bez błędów JS. Nie zmieniono migracji ani reguł cen.

## Pokrycie i jego granice

Zinwentaryzowano 614 elementów w szablonach oraz77 mapowań endpointów.
`controls.json` zawiera rzeczywiste pomiary elementów w wypełnionych widokach:
obramowania, kolory, rozmiary, zaokrąglenia, disabled i przewijanie. Zrzuty:
[telefon](screenshots/phone.png), [komputer](screenshots/desktop.png).

W `elements.csv` każdy element ma status i dowód. Zmierzenie geometrii nie jest
zaliczeniem każdej jego mutacji, walidacji czy kombinacji danych. Elementy bez
odrębnej asercji są jawnie oznaczone, zamiast ogłaszać fikcyjne100% przeklikania.
Scenariusze i testy kontraktów potwierdzają przepływy wymienione powyżej.

Nadal wymagają odbioru: fizyczny Android i jego aparat/klawiatura, rzeczywisty
CMS/proxy/VPN, bieżący skan CVE, obciążenie produkcyjne oraz niewykonane warianty
poszczególnych elementów z inwentaryzacji. Nie zmieniano wyglądu bez wykrytego
problemu ani nie dodawano funkcji tylko po to, żeby audyt wykazał więcej zmian.

## Powtarzanie

`./mvnw verify`, `node scripts/test-web-boundaries.cjs` i
`python scripts/verify-browser.py --owner-file /PRYWATNY/owner.json --pixel-tycoon`.
Runner przeglądarkowy wymaga przygotowanego lokalnego Compose i konfiguracji
właściciela; sam tworzy izolowany schemat. Nie podawać danych produkcyjnych.
Podgląd jest syntetycznym HTML. Trwałe zlecenia i zdjęcia zapisuje właściwy CRM
w PostgreSQL. Bez PR-a, scalenia i wdrożenia produkcyjnego.

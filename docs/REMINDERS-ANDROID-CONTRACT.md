# Przypomnienia i przyszły Android

## Obsługa

Otwórz zlecenie albo edycję notatki/celu → **Przypomnienia** → **+ Przypomnienie**. Wybierz wyprzedzenie (minuty/godziny/dni, również 0) albo konkretną datę i godzinę. Możesz zapisać do 10 przypomnień. Przy notatce/celu względne wyprzedzenie wymaga daty i godziny terminu. Zapis następuje razem z wpisem; przy zleceniu użyj „Zapisz przypomnienia”. Usunięcie wiersza wyłącza definicję po zapisie, zachowując historię.

Wyprzedzenie oznacza rzeczywisty czas, np. 1 dzień = 24 godziny. Daty i godziny formularzy są w Europe/Warsaw. Nieistniejąca godzina przy zmianie czasu jest odrzucana; dwuznaczna godzina jesienna oznacza pierwsze wystąpienie. Zmiana terminu przesuwa względne przypomnienia; przypomnienia na konkretną datę pozostają w tej dacie. Anulowanie/usunięcie/wykonanie wizyty lub ukończenie/archiwizacja celu zatrzymuje dostarczanie. Przywrócenie rekordu może przywrócić przypomnienie, ale nie ponawia odczytanego wystąpienia z tym samym terminem.

Na Dzisiaj nie ma tabeli konfiguracji przypomnień. Podczas korzystania z aplikacji pojawia się pojedynczy komunikat z odnośnikiem do rekordu. „×” ukrywa komunikat, „Odczytane” potwierdza tylko powiadomienie, bez kończenia usługi czy celu. Dotychczasowe cykle są nadal dostępne w edytorze w ustawieniach zaawansowanych i nadal są obsługiwane.

## Trwałość i kontrakt HTTP v1

Migracja V19 dodaje `due_time`, `reminder_owner`, `entity_reminder` oraz `notification_occurrence`. V1–V18 pozostają niezmienione. Wystąpienia mają trwałe UUID i unikalność `(reminder_id, scheduled_at)`. Dane pozostają w PostgreSQL po restarcie i aktualizacji. Wyłączenie definicji nie usuwa wystąpień. Zapis listy stosuje wersję właściciela oraz stabilne UUID wierszy. Ponowienie identycznego zapisu jest bezpieczne.

Endpointy podlegają aktualnej sesji Spring Security, OWNER i CSRF; nie są publicznym API urządzeń. Pod instalacją `/crm` poprzedź wszystkie ścieżki tym prefiksem. Odpowiedzi API mają `Cache-Control: no-store`.

- `GET /api/v1/reminders?sourceType=BOOKING|MEMO&sourceId=123` → `source` (typ, ID, tytuł, odnośnik, termin UTC, aktywność), `version`, `reminders` (`id`, `offsetMinutes` albo `absoluteAt` UTC).
- `POST /api/v1/reminders` (`application/x-www-form-urlencoded`): `sourceType`, `sourceId`, `entity_reminders_present=true`, `entity_version`, powtarzane numerowane pola `entity_id_0`, `entity_mode_0=OFFSET`, `entity_amount_0=30`, `entity_unit_0=minutes|hours|days`. Dla `AT` użyj `entity_at_0=2026-11-18T09:00`. Indeksy 0–9. Pusta lista wyłącza wszystkie. CSRF z `/session` do `_csrf`.
- `GET /api/v1/notifications` → `channel=IN_APP`, `zone`, `notifications` (maks. 100 należnych, uporządkowanych): `id`, `sourceType`, `sourceId`, `title`, `href`, `scheduledAt`, `availableAt`, `version`, `displayed`. Odczyt uzgadnia trwałą kolejkę z aktualnymi terminami i stanami źródeł. Dodatkowo `legacyNotifications` zawiera wcześniejsze cykle w ich zachowanym kontrakcie (ID notatki + revision + occurrence).
- `POST /api/v1/notifications/{uuid}`: `version`, `action=DISPLAYED|READ|SNOOZE`, `minutes=30` (dla odroczenia 1–10080). `DISPLAYED` zwraca `claimed=true` tylko pierwszemu odbiorcy i nie zmienia wersji. `READ` jest idempotentne. Odroczenie podnosi wersję i pozwala na ponowne wyświetlenie po terminie. Stary termin/wersja zostaje odrzucony. Po otrzymaniu błędu pobierz kolejkę ponownie; nie ponawiaj automatycznie mutacji po utracie sesji.

Aktualna przeglądarka odpytuje kolejkę raz na minutę i po powrocie do widocznej karty, odracza wyświetlanie przy otwartym dialogu. Jest to **powiadomienie wewnątrz otwartej aplikacji**, nie gwarantowany alarm przy zamkniętej przeglądarce. Trwała kolejka jest uzgadniana przy zapisie konfiguracji i odczycie/akcji API; obecnie nie ma procesu wysyłającego w tle.

## Kolejny etap integracji

Android powinien używać identyfikatora wystąpienia do deduplikacji, pobierać ponownie terminy po zmianie wizyty i odwoływać nieaktualne alarmy. Nie wolno kopiować hasła właściciela do aplikacji. Przed wdrożeniem urządzeń należy zaprojektować rejestrację urządzenia i odwoływalne uwierzytelnianie, kanały dostarczania, osobne potwierdzenia dla kanałów oraz dostawcę push/worker z retry. Ograniczenia Androida (oszczędzanie baterii, Doze, uprawnienia do powiadomień i dokładnych alarmów) wymagają testów na fizycznym telefonie. Obecny `DISPLAYED` jest wspólnym potwierdzeniem kanału IN_APP, nie niezależną kwitancją każdego urządzenia.

SMS jest odrębną operacją z własną kolejką, autoryzacją, treścią, terminem, deduplikacją i wynikiem dostawcy. Nie należy interpretować `READ` ani odroczenia przypomnienia jako zgody na wysłanie SMS lub zakończenie usługi. Ten etap nie wysyła SMS ani push.

## Kołowe podsumowanie Dzisiaj

Domyślnie miesiąc do wybranego dnia, pozostałe filtry: dzień, tydzień do dnia, cały tydzień, cały miesiąc, własny zakres (1–366 dni). Zakres finansowy nie zmienia wybranego dnia planu. Pikselowe koło pokazuje udziały dni w **przychodzie z wykonanych usług**. Kwota w środku to suma przychodu. Wynik operacyjny obok = przychód minus uwzględnione koszty, przed podatkami; może być ujemny. Nie dodajemy przychodu i zysku jako niezależnych części koła. Tabela zawiera dokładne wartości dzienne, również gdy niewielka część nie zajmuje całego piksela. Braki cen lub kosztów pozostają wyraźnie oznaczone.

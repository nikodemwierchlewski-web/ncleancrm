# ncLEAN CRM — klikalny podgląd Lean

Pobierz **[nclean-pixel-tycoon-test.zip](https://github.com/nikodemwierchlewski-web/ncleancrm/raw/refs/heads/ux/lean-workspace/preview/nclean-pixel-tycoon-test.zip)**. Jeżeli GitHub wymaga dostępu, zaloguj się na konto mające dostęp do repozytorium.

1. Rozpakuj ZIP do zwykłego folderu.
2. Otwórz `index.html` w Chrome, Edge lub Firefox.
3. Zacznij od Kalendarza, następnie sprawdź Dzisiaj i Więcej. Podsumowanie pokazuje trzy kwoty i akcję; analizy oraz notatki rozwija się na żądanie.
4. Klasyczny wygląd jest domyślny. Przycisk Pixel Tycoon pozwala porównać opcjonalny motyw i animacje.

To samodzielny HTML z lokalnymi zasobami. Dane są syntetyczne, zapis jest symulowany w pamięci karty, a odświeżenie resetuje zmiany. Nie jest to połączenie z bazą CRM. Prawdziwą aplikację i PostgreSQL sprawdzono oddzielnie.

Instrukcja zmian i wyniki: [LEAN-WORKSPACE.md](../docs/LEAN-WORKSPACE.md). Tożsamość paczki: [build.json](build.json). Dołączono licencje fontów.

Ta sesja Codexa nie deklaruje pulpitu/przekierowania portów (`capabilities: []`). Lokalny HTTP używany do testów nie jest publicznym adresem na komputerze użytkownika. Dlatego podgląd dostarczamy przez pobieralny ZIP, bez odnośnika do niedostępnej ścieżki `/workspace`.

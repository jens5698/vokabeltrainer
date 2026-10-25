# Vokabeltrainer (Englisch / Französisch → Deutsch)

Android-App, die beim Start die Vokabellisten `Vokabeln_E.xlsx` und `Vokabeln_F.xlsx` aus Google Drive lädt und sie im Karteikasten-Prinzip abfragt.

## Installation

1. Unter **Releases** (rechts auf der Repo-Seite) die neueste `Vokabeltrainer.apk` auf dem Handy herunterladen.
2. Öffnen und die Installation aus „unbekannten Quellen“ für den Browser erlauben.
3. Neue Versionen einfach drüber installieren, der Lernfortschritt bleibt erhalten.

## Einrichtung

1. In Google Drive bei beiden Dateien **Teilen → Allgemeiner Zugriff → „Jeder mit dem Link“ (Betrachter)** einstellen und **Link kopieren**.
2. In der App oben rechts auf das Zahnrad tippen, beide Links einfügen, **Speichern**.

Die App lädt die Dateien bei jedem Start neu (oder über den Pfeil-Knopf). Ohne Internet wird die zuletzt geladene Version verwendet.

## Aufbau der Excel-Dateien

Blatt „Vokabeln“ (sonst das erste Blatt), erste Zeile = Überschrift:

| Thema | Englisch / Französisch | Deutsch | Beispielsatz | Beispielsatz (Deutsch) |
|---|---|---|---|---|

Zeilen ohne Fremdwort oder ohne deutsche Bedeutung werden ignoriert.

## Bedienung

- Oben die Sprache wählen. Jedes Thema zeigt in Klammern die Anzahl der Vokabeln und darunter die Verteilung auf die 5 Stapel.
- Abfragerichtung (Fremdsprache → Deutsch, Deutsch → Fremdsprache oder gemischt), Stapel und Themen wählen, dann **Abfrage starten**.
- Pro Karte „Lösung zeigen“ und dann **Gewusst** oder **Nicht gewusst**.

### Stapel

| Stapel | so oft „Gewusst“ |
|---|---|
| 1 | 0× |
| 2 | 1× |
| 3 | 2× |
| 4 | 3× |
| 5 | 4× oder öfter |

„Nicht gewusst“ wird mitgezählt. In den Einstellungen lässt sich einschalten, dass eine nicht gewusste Vokabel wieder in Stapel 1 fällt. Der Fortschritt wird sofort auf dem Gerät gespeichert und gilt pro Vokabel (Fremdwort + Deutsch), auch wenn sich die Reihenfolge in der Datei ändert.

## Entwicklung

Kotlin + Jetpack Compose, keine weiteren Bibliotheken. GitHub Actions baut bei jedem Push auf `main` die APK und veröffentlicht sie als Release. Die APK wird mit `app/vokabeltrainer.keystore` signiert, damit Updates installierbar bleiben.

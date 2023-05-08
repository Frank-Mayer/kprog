package second;

import java.util.*;

/**
 * Fünf Behälter mit Nummern 1, 2, 3, 4 und 5 fassen in dieser Reihenfolge genau A, B, C, D und E
 * Liter (alles ganze Zahlen). Behälter 1 ist zu Beginn voll Wasser, die anderen sind leer. Bei
 * einem erlaubten Umfüll-Vorgang wird Flüssigkeit von einem Behälter in einen anderen gegossen, bis
 * der ausgießende leer oder der sich füllende voll ist. Dabei wird nichts verschüttet.
 *
 * <p>Es kann aber nur wie folgt zwischen den Behältern umgefüllt werden: 1 -> 2; 1 -> 4; 2 -> 3; 3
 * -> 5; 4 -> 2; 5 -> 4. Die Füllstände der Behälter werden als Fünftupel (a, b, c, d, e) codiert,
 * wobei a, b, c, d und e in dieser Reihenfolge die Füllgrade der Behälter 1, ..., 5 angeben. Der
 * Anfangszustand ist also (A, 0, 0, 0, 0).
 *
 * <p>Schreiben Sie ein Programm Umfuelllisten, das zu Beginn die ganzen Zahlen A, B, C, D und E
 * einliest und dann in sortierter Reihenfolge alle möglichen Fünftupel ausgibt, die
 * Füllungs-Zuständen der Behälter entsprechen, die sich aus dem Anfangszustand erreichen lassen.
 * Die Sortierung ist wie im Beispiel rechts angegeben.
 *
 * <p>Am Ende wird die Anzahl der gelisteten Fünftupel ausgegeben.
 *
 * @author Frank Mayer, Antonia Friese, René Ott
 * @version 1.0 - 04.05.2023
 */
public class Umfuelllisten {
  private static String getErrorMessage() {
    final var r = new Random();
    final var errCode = r.nextInt(10000);
    final var errChar = Integer.toString(r.nextInt(27) + 10, 36).toUpperCase();
    return String.format("Error %s%d: Allgemeiner Fehler :D", errChar, errCode);
  }

  public static void main(String[] args) {
    int[] capacities = new int[5];
    try (final var scanner = new Scanner(System.in)) {
      for (int i = 0; i < 5; ++i) {
        System.out.printf(
            "Bitte Kapazität von Behälter %s eingeben: ",
            Integer.toString(i + 10, 16).toUpperCase());
        capacities[i] = scanner.nextInt();
      }
    } catch (Exception ignore) {
      System.err.println(Umfuelllisten.getErrorMessage());
      return;
    }

    System.out.printf(
        "Kapazitäten sind: ( %d %d %d %d %d )%n",
        capacities[0],
        capacities[1],
        capacities[2],
        capacities[3],
        capacities[4]);

    // TreeSet um alle möglichen Zustände zu speichern
    final var states = new TreeSet<>(
        (Comparator<int[]>) Arrays::compare);

    // Anfangszustand: Behälter 1 voll, andere leer
    int[] initialState = {capacities[0], 0, 0, 0, 0};

    // Mittels Breitensuche alle möglichen Zustände finden
    final var queue = new LinkedList<int[]>();
    queue.add(initialState);
    states.add(initialState);
    var from = 0;
    var to = 0;
    while (!queue.isEmpty()) {
      final var currentState = queue.poll();

      // für alle Elemente des Tupels
      for (var i = 0; i < 6; ++i) {
        final var nextState = currentState.clone();
        switch (i) {
          case 0 -> { from = 0; to = 1; }
          case 1 -> { from = 0; to = 3; }
          case 2 -> { from = 1; to = 2; }
          case 3 -> { from = 2; to = 4; }
          case 4 -> { from = 3; to = 1; }
          case 5 -> { from = 4; to = 3; }
          default -> { System.err.println(Umfuelllisten.getErrorMessage()); return; }
        }

        // Wie viel kann umgefüllt werden?
        final var amount = Math.min(
            currentState[from],
            capacities[to] - currentState[to]);

        // Umfüllen
        nextState[from] -= amount;
        nextState[to] += amount;

        // Zustand noch nicht bekannt?
        if (!states.contains(nextState)) {
          queue.add(nextState);
          states.add(nextState);
        }
      }
    }

    for (final var state : states) {
      System.out.printf(
          "( %d %d %d %d %d )%n",
          state[0], state[1], state[2], state[3], state[4]);
    }

    System.out.printf("Es gibt genau %d Fünftupel", states.size());
  }
}
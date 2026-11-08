package com.traktor.bridge;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeckManager
{
   private static final double BITWIG_CC_MAX = 127.0;
   private static final int MINOR_MODE_CC_VALUE = 10;
   private static final int MAJOR_MODE_CC_VALUE = 0;
   private static final String[] PITCH_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

   private final ControllerHost host;
   private final MidiOut midiOut;
   private boolean isRunning;
   
   private final String[] deckKeys = new String[4];
   private final String[] deckTraktorKeys = new String[4];
   private final int[] deckKeyNotes = new int[4];
   private final int[] deckTransposeValues = new int[4];
   private final boolean[] deckKeyIsMinor = new boolean[4];
   private final boolean[] deckKeyKnown = new boolean[4];
   private final ScaleEngine scaleEngine;
   private volatile int activeDeck = 0;
   private volatile long keySendSequence = 0;
   private boolean[] deckActive = new boolean[4];
   private boolean sendKeyOnDeckLoadEnabled = false;
   
   private static final int[] K2_BUTTONS = {48, 51, 48, 51};
   private static final int[] K2_CHANNELS = {14, 14, 15, 15};
   private static final String[] DECK_NAMES = {"A", "B", "C", "D"};
   
   private static final Map<String, String> CAMELOT_TO_KEY = new HashMap<>();
   private static final Map<String, Integer> CAMELOT_TO_NOTE = new HashMap<>();
   private static final Map<String, Integer> CAMELOT_TO_CC_VALUE = new HashMap<>();
   
   static {
      // Mapping aligned with the Traktor wheel used in this project (Quanta/Open style).
      CAMELOT_TO_KEY.put("1d", "C");    CAMELOT_TO_NOTE.put("1d", 60);
      CAMELOT_TO_KEY.put("2d", "G");    CAMELOT_TO_NOTE.put("2d", 67);
      CAMELOT_TO_KEY.put("3d", "D");    CAMELOT_TO_NOTE.put("3d", 62);
      CAMELOT_TO_KEY.put("4d", "A");    CAMELOT_TO_NOTE.put("4d", 69);
      CAMELOT_TO_KEY.put("5d", "E");    CAMELOT_TO_NOTE.put("5d", 64);
      CAMELOT_TO_KEY.put("6d", "B");    CAMELOT_TO_NOTE.put("6d", 59);
      CAMELOT_TO_KEY.put("7d", "F#");   CAMELOT_TO_NOTE.put("7d", 66);
      CAMELOT_TO_KEY.put("8d", "C#");   CAMELOT_TO_NOTE.put("8d", 61);
      CAMELOT_TO_KEY.put("9d", "G#");   CAMELOT_TO_NOTE.put("9d", 68);
      CAMELOT_TO_KEY.put("10d", "D#");  CAMELOT_TO_NOTE.put("10d", 63);
      CAMELOT_TO_KEY.put("11d", "A#");  CAMELOT_TO_NOTE.put("11d", 70);
      CAMELOT_TO_KEY.put("12d", "F");   CAMELOT_TO_NOTE.put("12d", 65);

      CAMELOT_TO_KEY.put("1m", "Am");         CAMELOT_TO_NOTE.put("1m", 69);
      CAMELOT_TO_KEY.put("2m", "Em");         CAMELOT_TO_NOTE.put("2m", 64);
      CAMELOT_TO_KEY.put("3m", "Bm");         CAMELOT_TO_NOTE.put("3m", 59);
      CAMELOT_TO_KEY.put("4m", "F#m/Gbm");    CAMELOT_TO_NOTE.put("4m", 66);
      CAMELOT_TO_KEY.put("5m", "C#m/Dbm");    CAMELOT_TO_NOTE.put("5m", 61);
      CAMELOT_TO_KEY.put("6m", "G#m/Abm");    CAMELOT_TO_NOTE.put("6m", 68);
      CAMELOT_TO_KEY.put("7m", "D#m/Ebm");    CAMELOT_TO_NOTE.put("7m", 63);
      CAMELOT_TO_KEY.put("8m", "A#m/Bdm");    CAMELOT_TO_NOTE.put("8m", 70);
      CAMELOT_TO_KEY.put("9m", "Fm");         CAMELOT_TO_NOTE.put("9m", 65);
      CAMELOT_TO_KEY.put("10m", "Cm");        CAMELOT_TO_NOTE.put("10m", 60);
      CAMELOT_TO_KEY.put("11m", "Gm");        CAMELOT_TO_NOTE.put("11m", 67);
      CAMELOT_TO_KEY.put("12m", "Dm");        CAMELOT_TO_NOTE.put("12m", 62);

      // Map the 12 chromatic root notes evenly across the complete 7-bit CC range.
      // Major/minor Camelot keys with the same root intentionally produce the same CC16 value.
      for (Map.Entry<String, Integer> entry : CAMELOT_TO_NOTE.entrySet()) {
         final int pitchClass = Math.floorMod(entry.getValue(), 12);
         final int ccValue = (int) Math.round(pitchClass * BITWIG_CC_MAX / 11.0);
         CAMELOT_TO_CC_VALUE.put(entry.getKey(), Math.max(0, Math.min(127, ccValue)));
      }
   }
   
   public DeckManager(final ControllerHost host, final MidiOut midiOut)
   {
      this.host = host;
      this.midiOut = midiOut;
      this.isRunning = true;
      this.scaleEngine = new ScaleEngine();
      
      for (int i = 0; i < deckKeys.length; i++) {
         deckKeys[i] = "C";
         deckKeyNotes[i] = 60;
         deckTransposeValues[i] = 0;
         deckKeyIsMinor[i] = false;
         deckActive[i] = false;
      }
      deckActive[0] = true;
      
      host.println("========================================");
      host.println("DeckManager - FINAL");
      host.println("  CC16 controls Bitwig project Root Key (major/minor ignored)");
      host.println("  Auto-send on track load: OFF (send only on deck-button double-click)");
      host.println("========================================");
   }
   
   public void setDeckKey(final int deckIndex, final String key)
   {
      if (deckIndex < 0 || deckIndex >= deckKeys.length) return;
      if (key == null || key.isEmpty()) return;
      
      String cleanKey = key.replace("~", "").trim();
      
      String standardKey = cleanKey;
      int midiNote = 60;
      String normalizedKey = cleanKey.toLowerCase(Locale.ROOT).trim();
      if (normalizedKey.matches("\\d+[ab]")) {
         // Support classic Camelot notation: A=minor, B=major
         normalizedKey = normalizedKey.substring(0, normalizedKey.length() - 1)
         + (normalizedKey.endsWith("a") ? "m" : "d");
      }
      
      if (normalizedKey.matches("\\d+[dm]")) {
         if (CAMELOT_TO_KEY.containsKey(normalizedKey)) {
            standardKey = CAMELOT_TO_KEY.get(normalizedKey);
            midiNote = CAMELOT_TO_NOTE.get(normalizedKey);
         } else {
            host.println("IGNORED KEY: unsupported Camelot value='" + cleanKey + "'");
            return;
         }
      } else {
         // Traktor can expose enharmonic alternatives such as "C#m/Dbm".
         // Use the first spelling instead of concatenating both note names.
         final String noteName = normalizedKey.split("/", 2)[0]
               .replace("♯", "#")
               .replace("♭", "b")
               .replace("sharp", "#")
               .replace("flat", "b")
               .replaceAll("(?i)\\b(minor|major|maj|min)\\b", "")
               .replaceAll("m$", "")
               .replaceAll("[^a-g#b]", "");
         final String[] noteNames = {"c", "c#", "db", "d", "d#", "eb", "e", "f", "f#", "gb", "g", "g#", "ab", "a", "a#", "bb", "b"};
         final int[] pitchClasses = {0, 1, 1, 2, 3, 3, 4, 5, 6, 6, 7, 8, 8, 9, 10, 10, 11};
         int pitchClass = -1;
         for (int i = 0; i < noteNames.length; i++) {
            if (noteNames[i].equals(noteName)) {
               pitchClass = pitchClasses[i];
               break;
            }
         }
         if (pitchClass < 0) {
            host.println("IGNORED KEY: unsupported Traktor key value='" + cleanKey + "'");
            return;
         }
         midiNote = 60 + pitchClass;
         standardKey = PITCH_NAMES[pitchClass];
         normalizedKey = "note:" + pitchClass;
      }

      final String sourceKey = cleanKey.toLowerCase(Locale.ROOT).trim();
      final String firstKeyName = sourceKey.split("/", 2)[0].trim();
      final boolean isMinorKey = normalizedKey.matches("\\d+m")
            || sourceKey.contains("minor")
            || sourceKey.matches(".*\\bmin\\b.*")
            || firstKeyName.matches("[a-g](?:#|b)?m");

      deckTraktorKeys[deckIndex] = cleanKey;
      if (deckKeyKnown[deckIndex] && deckKeys[deckIndex].equals(standardKey)
            && deckKeyNotes[deckIndex] == midiNote
            && deckKeyIsMinor[deckIndex] == isMinorKey) {
         return;
      }
      
      deckKeys[deckIndex] = standardKey;
      deckKeyNotes[deckIndex] = midiNote;
      deckKeyIsMinor[deckIndex] = isMinorKey;
      deckKeyKnown[deckIndex] = true;
      
      if (deckIndex == activeDeck) {
         scaleEngine.setKey(cleanKey);
      }
      
      final int pitchClass = Math.floorMod(midiNote, 12);
      int ccValue = (int) Math.round(pitchClass * BITWIG_CC_MAX / 11.0);
      deckTransposeValues[deckIndex] = ccValue;

      host.println("DECK " + (deckIndex + 1) + " KEY: " + cleanKey + " -> " + standardKey);
      host.println("  (stored Root Key CC16 value = " + ccValue + ")");
   }

   private void logExpectedBitwigKey(final int deckIndex) {
      if (!deckKeyKnown[deckIndex]) {
         host.println("DECK " + DECK_NAMES[deckIndex]
               + " TRAKTOR KEY: unknown; EXPECTED BITWIG KEY: unknown (no track key received)");
         return;
      }
      final String root = PITCH_NAMES[Math.floorMod(deckKeyNotes[deckIndex], 12)];
      final int modeValue = deckKeyIsMinor[deckIndex] ? MINOR_MODE_CC_VALUE : MAJOR_MODE_CC_VALUE;
      host.println("DECK " + DECK_NAMES[deckIndex] + " TRAKTOR KEY: " + deckTraktorKeys[deckIndex]
            + " -> EXPECTED BITWIG KEY: " + root + (deckKeyIsMinor[deckIndex] ? " Minor" : " Major")
            + " (CC16=" + deckTransposeValues[deckIndex] + ", CC17=" + modeValue + ")");
   }

   private void sendKeyForActiveDeck(final int deckIndex) {
      int ccValue = deckTransposeValues[deckIndex];
      host.println("SEND ACTIVE KEY -> deck=" + DECK_NAMES[deckIndex] + " cc=16 value=" + ccValue + " status=185");
      sendMidiMessage(0xB9, 16, ccValue);
      sendKeyModeForDeck(deckIndex);
   }

   private void sendKeyModeForDeck(final int deckIndex) {
      final int modeValue = deckKeyIsMinor[deckIndex] ? MINOR_MODE_CC_VALUE : MAJOR_MODE_CC_VALUE;
      host.println("SEND ACTIVE KEY MODE -> deck=" + DECK_NAMES[deckIndex]
            + " cc=17 value=" + modeValue + " status=185");
      sendMidiMessage(0xB9, 17, modeValue);
   }
   
   private void setActiveDeck(int deckIndex) {
      if (deckIndex < 0 || deckIndex >= 4) return;
      
      for (int i = 0; i < 4; i++) {
         deckActive[i] = false;
      }
      
      deckActive[deckIndex] = true;
      activeDeck = deckIndex;
      
      scaleEngine.setKey(deckKeys[deckIndex]);
      
      host.println("========================================");
      host.println("ACTIVE DECK: " + DECK_NAMES[deckIndex]);
      host.println("  Key: " + deckKeys[deckIndex]);
      host.println("========================================");
   }
   
   public void handleMidiMessage(final int status, final int data1, final int data2) {
      final int command = status & 0xF0;
      final int channel = (status & 0x0F) + 1;
      
      // XONE:K2 бутони (от IAC)
      if (command == 0x90 && data2 > 0) {
         for (int i = 0; i < 4; i++) {
            if (K2_BUTTONS[i] == data1 && K2_CHANNELS[i] == channel) {
               host.println(">>> K2: Deck " + DECK_NAMES[i]);
               setActiveDeck(i);
               return;
            }
         }
         return;
      }
      
      // NOTE OFF - игнорираме
      if (command == 0x80) {
         return;
      }
      
      // CC - игнорираме нашите CC16-19
      if (command == 0xB0) {
          if (data1 >= 16 && data1 <= 19) {
              return;
          }
          sendMidiMessage(status, data1, data2);
          return;
      }
      
      // Note On канал 10 - quantize
      if (command == 0x90 && data2 > 0 && channel == 10) {
         int originalNote = data1;
         int quantizedNote = scaleEngine.quantizeNote(originalNote);
         if (quantizedNote != originalNote) {
            host.println("Quantized: " + originalNote + " -> " + quantizedNote);
         }
         sendMidiMessage(0x99, quantizedNote, data2);
         return;
      }
      
      sendMidiMessage(status, data1, data2);
   }
   
   public void handleMessage(final String command, final String data) {
      try {
         host.println("Processing command: " + command + " with data: " + data);
         
         if ("setKey".equals(command)) {
            JSONObject json = new JSONObject(data);
            int deck = json.optInt("deck", 1) - 1;
            String key = json.optString("key", "C");
            setDeckKey(deck, key);
         } else if ("enableScale".equals(command)) {
            scaleEngine.setEnabled(true);
            host.println("Scale Engine ENABLED");
         } else if ("disableScale".equals(command)) {
            scaleEngine.setEnabled(false);
            host.println("Scale Engine DISABLED");
         } else if ("setActiveDeck".equals(command)) {
            int deck = Integer.parseInt(data.trim()) - 1;
            if (deck >= 0 && deck < 4) {
               setActiveDeck(deck);
            }
         }
      } catch (Exception e) {
         host.println("Error: " + e.getMessage());
      }
   }

   public void activateDeck(final int deckIndex) {
      if (deckIndex < 0 || deckIndex >= deckKeys.length) return;
      setActiveDeck(deckIndex);
      final long sequence = ++keySendSequence;
      host.println("DECK BUTTON DOUBLE-CLICK: sending stored key for " + DECK_NAMES[deckIndex]);
      logExpectedBitwigKey(deckIndex);
      sendKeyForActiveDeck(deckIndex);
      host.scheduleTask(() -> {
         if (sequence != keySendSequence || activeDeck != deckIndex) {
            host.println("DELAYED KEY SEND skipped: active deck changed");
            return;
         }
         host.println("DELAYED KEY SEND: repeating Root Key CC16 for " + DECK_NAMES[deckIndex]);
         sendKeyForActiveDeck(deckIndex);
      }, 2000);
   }

   public void activateDeckTripleClick(final int deckIndex) {
      if (deckIndex < 0 || deckIndex >= deckKeys.length) return;
      setActiveDeck(deckIndex);
      final long sequence = ++keySendSequence;
      host.println("DECK BUTTON TRIPLE-CLICK: scheduling key mode for " + DECK_NAMES[deckIndex]);
      host.scheduleTask(() -> {
         if (sequence != keySendSequence || activeDeck != deckIndex) {
            host.println("DELAYED KEY MODE SEND skipped: active deck changed");
            return;
         }
         host.println("DELAYED KEY MODE SEND for " + DECK_NAMES[deckIndex]);
         sendKeyModeForDeck(deckIndex);
      }, 2000);
   }

   public void logBridgeEvent(final String message) {
      host.println("TRAKTOR BRIDGE: " + message);
   }

   public void handleDeckLoaded(final int deckIndex) {
      if (deckIndex < 0 || deckIndex >= deckKeys.length) return;
      if (!sendKeyOnDeckLoadEnabled) return;
      if (deckIndex != activeDeck) return;

      sendKeyForActiveDeck(deckIndex);
   }
   
   public void processMessage(final String jsonMessage) {
      try {
         JSONObject json = new JSONObject(jsonMessage);
         String command = json.getString("command");
         String data = json.optString("data", "");
         handleMessage(command, data);
      } catch (Exception e) {
         host.println("Error parsing JSON: " + e.getMessage());
      }
   }
   
   private void sendMidiMessage(final int status, final int data1, final int data2) {
      if (midiOut == null) {
         host.println("MIDI OUT ERROR: IAC output is unavailable; message not sent");
         return;
      }
      midiOut.sendMidi(status, data1, data2);
      host.println("MIDI OUT SENT: status=" + status + " channel=" + ((status & 0x0F) + 1)
            + " CC=" + data1 + " value=" + data2);
   }

   public void shutdown() {
      isRunning = false;
      host.println("DeckManager shutting down...");
   }
   
   public boolean isRunning() { return isRunning; }
}

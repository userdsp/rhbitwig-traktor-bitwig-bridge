# Bitwig Traktor Bridge - Full Tone Range Support

Advanced Bitwig Studio extension for seamless integration with Native Instruments Traktor DJ software via HTTP bridge, with specialized controllers (Allen & Heath K2/K3, Yaeltex, Akai Fire, Novation Launchpad).

## 🎛️ Features

### Traktor Bridge System
- **HTTP Bridge Server** (`localhost:8090`) for real-time communication between Traktor and Bitwig
- **Full Tone Range Support**: Automatic key detection and transposition across all 12 chromatic notes
- **Camelot Wheel Integration**: Maps Traktor's Camelot notation (1A-12B) to Bitwig's Note Transpose
- **Deck Management**: Supports Traktor decks A, B, C, D with individual key tracking
- **MIDI CC Control**: CC16 and CC17 (on MIDI channel 10) for real-time key/mode synchronization

### Supported Controllers
1. **Allen & Heath K2/K3 DJ Setup**
   - 4-deck selector with LED feedback
   - Specialized K2/K3 auto-detection

2. **Yaeltex SEQ ARP 168**
   - Arpeggiator device control via MIDI
   - Sequencer integration

3. **Akai Fire**
   - Drum sequencer/editor
   - Smart copy & paste workflow
   - Sequencing display pipeline

4. **Novation Launchpad Pro Mk3**
   - Drum sequencer/editor
   - Step-based note editing

5. **Novation Launchcontrol**
   - Arpeggiator device control

6. **Lexicon PSP42 v2**
   - Full controller integration

## 📋 System Requirements

### Software
- **Bitwig Studio** 4.0 or later (requires Extension API 24)
- **Bitwig Extension API** 24 (specified in `pom.xml`)
- **Java** 17+ (for compilation and running)
- **Maven** 3.8+ (for building)
- **Native Instruments Traktor** 3/4 (optional, for bridge functionality)
- **[traktor-api-client](https://github.com/ErikMinekus/traktor-api-client)** (required for key/tonality sync with Traktor)

### Hardware
- Your supported MIDI controller (see above)

### macOS Specific
- **IAC Driver** (Inter-Application Communication)
  - Enable in: `Applications > Utilities > Audio MIDI Setup`
  - Create a virtual MIDI port for Traktor ↔ Bitwig communication

### Windows Specific
- Virtual MIDI cable software (e.g., loopMIDI, LoopBe)

## ⚙️ Installation

### Step 1: Build the Extension

Clone this repository and navigate to the project directory:

```bash
git clone https://github.com/userdsp/rhbitwig-traktor-bitwig-bridge.git
cd rhbitwig-traktor-bitwig-bridge
```

Build using Maven:

```bash
mvn clean install
```

The build creates:
- `target/YaeltexArpControl-shaded.jar` (uber-JAR with all dependencies)
- `target/YaeltexArpControl.bwextension` (Bitwig extension format)
- Auto-copies to: `~/Documents/Bitwig Studio/Extensions/YaeltexArpControl.bwextension`

### Step 2: Enable Virtual MIDI (macOS)

```
Open: Applications > Utilities > Audio MIDI Setup
Click: Audio Devices
Double-click: IAC Driver
Add a virtual port (e.g., "Traktor Bitwig")
```

### Step 3: Load Extension in Bitwig

1. Open **Bitwig Studio**
2. **Menu** → **Preferences** → **Controllers**
3. Click **"Add Controller"** and select **"TraktorBridge"**
4. Select the IAC Driver (virtual MIDI port) from the device list
5. Restart Bitwig if needed

### Step 4: Install traktor-api-client

Follow the installation instructions at [https://github.com/ErikMinekus/traktor-api-client](https://github.com/ErikMinekus/traktor-api-client)
- This is required for key/tonality synchronization between Traktor and Bitwig
- Start the traktor-api-client service

### Step 5: Test the Connection

1. Load a track in Traktor deck A (track must have Traktor Open Key metadata)
2. Check Bitwig console for deck key output
3. Double-click K2 deck A button to select the deck
4. Bitwig's global project root key and scale automatically update
5. All instruments in the project now play in the correct key from Traktor!

## 🎛️ Allen & Heath K2/K3 Deck Selector Buttons

The 4 deck selector buttons provide complete deck management and MIDI mapping. All button states are represented by LED colors.

### LED States

| LED Color | Meaning |
|-----------|---------|
| **Red** | Active deck in Traktor (marked deck) |
| **Yellow** | Selected deck in Bitwig |
| **Red + Yellow** | Both Traktor and Bitwig have same deck active (yellow overlays red) |
| **Green** | Shift mode active in Traktor (Press/Hold) |
| **Off** | Deck not active in either application |

### Button Actions

| Action | Timing | Effect |
|--------|--------|--------|
| **Single-click** | — | Preview/temporary change on non-active deck |
| **Double-click** | within 320ms | Select deck in Bitwig, send to bridge, await CC16 mapping input |
| **Triple-click** | — | Await CC17 (mode) mapping input |
| **Press/Hold** | continuous | Shift mode - Green LED in Traktor (used for Traktor mapping modulation) |
| **Traktor deck selection** | — | Red LED appears when deck marked in Traktor |

### Console Output

When double-clicking a deck button, Bitwig console shows:

```
[Bridge] Deck A selected
  Traktor Key: 1d (C)
  Expected Note: 60 (C4)
  Expected Mode: Major
  CC16 value: 64
  CC17 value: 0 (major)
```

## 🎼 Key Mapping: Traktor Open Key → Bitwig

The bridge automatically maps **Traktor Open Key notation** to **Bitwig root key and scale**:

| Traktor Key | Bitwig Root | Bitwig Scale |
|-------------|-------------|--------------|
| **1m** | A | Minor |
| **2m** | E | Minor |
| **3m** | B | Minor |
| **4m** | F# | Minor |
| **5m** | C# | Minor |
| **6m** | G# | Minor |
| **7m** | D# | Minor |
| **8m** | A# (Bb) | Minor |
| **9m** | F | Minor |
| **10m** | C | Minor |
| **11m** | G | Minor |
| **12m** | D | Minor |
| **1d** | C | Major |
| **2d** | G | Major |
| **3d** | D | Major |
| **4d** | A | Major |
| **5d** | E | Major |
| **6d** | B | Major |
| **7d** | F# | Major |
| **8d** | C# | Major |
| **9d** | G# | Major |
| **10d** | D# | Major |
| **11d** | A# (Bb) | Major |
| **12d** | F | Major |

**Legend:**
- `m` = Minor key
- `d` = Major key (Dur)

## 🎚️ MIDI CC Control: CC16 and CC17

The bridge uses two MIDI CC controls on **channel 10** for real-time key and mode synchronization:

### CC16: Tonal Root (C to B)
- **Range**: 0-132 (covering all 12 chromatic notes × 11 octaves)
- **Note Mapping**:
  - CC16 value **60** = C (root note)
  - CC16 value **64** = E
  - CC16 value **71** = B
- **Usage**: Defines the tonal center (root note) for the current track
- **Bitwig Integration**: Sets the global project **root key** (C, C#, D, D#, E, F, F#, G, G#, A, A#, B)

### CC17: Mode (Major vs. Minor)
- **CC17 value 0**: **Major** mode
- **CC17 value 127**: **Minor** mode
- **Usage**: Toggles between major and minor scales
- **Bitwig Integration**: Sets the global project **scale** (Major or Minor)

### MIDI Mapping Configuration

To configure CC16/CC17 mapping in your MIDI controller:

1. **Double-click** a deck button
   - Bridge awaits MIDI input for mapping CC16 (root note)
   - Send any MIDI CC on channel 10 → Mapped to **CC16**

2. **Triple-click** a deck button
   - Bridge awaits MIDI input for mapping CC17 (mode)
   - Send any MIDI CC on channel 10 → Mapped to **CC17**

3. Configuration is **automatically saved** and persists across restarts

**Example Flow**:
```
1. Double-click deck A button
2. Turn a knob/slider on your controller or press a key to map CC16
3. Triple-click deck A button
4. Turn a different knob/slider or press a key to map CC17
5. Now deck A will synchronize root key and mode automatically
```

## 🔧 Configuration Files

### Maven Configuration (`pom.xml`)

Key settings:
- **Java Version**: 17 (configurable via `maven.compiler.release`)
- **Bitwig Extension Directory**: Defaults to `~/Documents/Bitwig Studio/Extensions`
- **Extension API**: Bitwig Extension API v24

Override extension directory:

```bash
mvn clean install -Dbitwig.extension.directory=/custom/path
```

### Building Options

**Full build with tests:**
```bash
mvn clean test package install
```

**Skip tests (faster):**
```bash
mvn clean package install -DskipTests
```

**Only compile, don't package:**
```bash
mvn clean compile
```

## 📝 Project Structure

```
rhbitwig-traktor-bitwig-bridge/
├── src/
│   ├── main/java/
│   │   ├── com/traktor/bridge/
│   │   │   ├── DeckManager.java          # Deck management & key synchronization
│   │   │   ├── HttpBridgeServer.java     # HTTP server for Traktor ↔ Bitwig
│   │   │   ├── ScaleEngine.java          # Camelot/note conversion logic
│   │   │   ├── VirtualMidiInput.java     # Virtual MIDI input handling
│   │   │   ├── TraktorBitwigBridgeExtension.java
│   │   │   └── TraktorBitwigBridgeExtensionDefinition.java
│   │   ├── com/yaeltex/
│   │   │   ├── YaeltexArpControlExtension.java
│   │   │   ├── YaeltexArpControlExtensionDefinition.java
│   │   │   └── ... (controller implementations)
│   │   └── com/akai/, com/novation/, etc.
│   └── test/java/
│       └── ... (unit tests)
├── pom.xml                               # Maven build configuration
├── LICENSE                               # Project license
└── README.md                             # This file
```

## 🔌 HTTP Bridge API

### Global Root Key and Scale Control

The bridge automatically sets **Bitwig's global project root key and scale** based on the track's Traktor Open Key notation:

- When a track is loaded in Traktor, its key metadata is sent to the bridge via [traktor-api-client](https://github.com/ErikMinekus/traktor-api-client)
- The bridge looks up the corresponding Bitwig root and scale from the mapping table

### Bridge Endpoints

**GET /deckKey/{A|B|C|D}**
- Returns the current key for the specified deck
- Response: `{"key": "1d", "note": 60, "mode": "major"}`

**POST /activeDeck/{A|B|C|D}**
- Notifies bridge of active deck change
- K2 controller sends this when deck is marked in Traktor

**GET /status**
- Returns bridge status and connected decks
- Useful for debugging connectivity

## 🐛 Troubleshooting

### Bridge not responding?
- Verify that traktor-api-client is installed and running
- Verify virtual MIDI port is active
- Check Bitwig console for connection errors
- Ensure firewall allows localhost:8090

### Keys not updating in Bitwig?
- Ensure track in Traktor has Traktor Open Key metadata
- Check that bridge received the key via console output
- Verify the mapping matches the Traktor Open Key → Bitwig table
- Restart Bitwig if key mapping doesn't apply
- Double-click K2 deck button to manually trigger key update

### Virtual MIDI not working?
- **macOS**: Open `Audio MIDI Setup` and verify IAC Driver port exists
- **Windows**: Verify loopMIDI or similar is running and configured

### Extension not loading?
- Check that Java 17+ is installed: `java -version`
- Verify Maven 3.8+ is installed: `mvn -version`
- Check Bitwig console (Help → Show Logfile) for detailed errors
- Rebuild: `mvn clean install`

## 📚 Additional Resources

### Camelot Wheel Reference
The bridge uses the Camelot Wheel system which maps musical keys to numbered positions:
- **1A-12A**: Major keys (1A = B major, 12A = C# major)
- **1B-12B**: Minor keys (1B = G# minor, 12B = A# minor)

### Performance Tips
- Bitwig's global key settings update automatically (no MIDI CC required)
- For best results, use tracks with accurate Traktor Open Key metadata
- The bridge runs in the background and does not consume significant CPU

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🎵 Credits

Built for seamless Traktor ↔ Bitwig integration with support for:
- Native Instruments Traktor DJ Software
- Bitwig Studio 4.0+
- Allen & Heath K2/K3 controllers
- Multiple third-party MIDI controllers

For issues, feature requests, or contributions, please visit: [https://github.com/userdsp/rhbitwig-traktor-bitwig-bridge](https://github.com/userdsp/rhbitwig-traktor-bitwig-bridge)

---

**Bitwig Studio Required**: 4.0 or later

**Extension API**: Bitwig Extension API 24

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
   - 4-deck selector with preview/select modes
   - LED feedback for active deck (yellow) and preview state (red)
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

### Step 2: Load Extension in Bitwig

1. Open **Bitwig Studio**
2. **Menu** → **Preferences** → **Controllers**
3. Click **"Add Controller"** and select **"YaeltexArpControl"**
4. Select your MIDI controller from the device list
5. Restart Bitwig if needed

### Step 3: Configure Traktor Bridge (Optional)

To use the Traktor integration:

1. Enable virtual MIDI:
   - **macOS**: Open `Audio MIDI Setup`, create an IAC Driver port
   - **Windows**: Install and configure loopMIDI or similar

2. In Bitwig controller settings:
   - Select **"TraktorBridge"** as controller
   - Assign the virtual MIDI port

3. In Traktor:
   - Install the Traktor API script from `scripts/` directory
   - The bridge auto-connects to `http://localhost:8090`
   - Traktor sends deck key every 2 seconds via `/deckKey/{A|B|C|D}`
   - K2 notifies bridge of active deck via `POST /activeDeck/{A|B|C|D}`

## 🎼 Tone Range & Key Mapping

### Full Chromatic Support (-1 to +10 semitones)

The bridge maps **Camelot notation** to **Bitwig Note Transpose** via MIDI CC16:

| Semitone | CC16 | Example Keys | Root Note |
|----------|------|--------------|-----------|
| -1 | 63 | 6d, 3m | B/Bm |
| 0 | 64 | 1d, 10m | **C/Am** (reference) |
| 1 | 65 | 8d, 5m | C#/C#m |
| 2 | 67 | 3d, 12m | D/Dm |
| 3 | 68 | 10d, 7m | D#/D#m |
| 4 | 69 | 5d, 2m | E/Em |
| 5 | 71 | 12d, 9m | F/Fm |
| 6 | 72 | 7d, 4m | F#/F#m |
| 7 | 73 | 2d, 11m | G/Gm |
| 8 | 75 | 9d, 6m | G#/G#m |
| 9 | 76 | 4d, 1m | A/Am |
| 10 | 77 | 11d, 8m | A#/A#m |

### Camelot Wheel Reference

**Major Keys (Camelot Notation)**
| Camelot | Note | Label |
|---------|------|-------|
| 1d | C4 | C |
| 2d | G3 | G |
| 3d | D4 | D |
| 4d | A4 | A |
| 5d | E4 | E |
| 6d | B3 | B |
| 7d | F#4 | F# |
| 8d | C#4 | C# |
| 9d | G#4 | G# |
| 10d | D#4 | D# |
| 11d | A#4 | A# |
| 12d | F4 | F |

**Minor Keys (Camelot Notation)**
| Camelot | Note | Label |
|---------|------|-------|
| 1m | A3 | Am |
| 2m | E4 | Em |
| 3m | B3 | Bm |
| 4m | F#4 | F#m/Gbm |
| 5m | C#4 | C#m/Dbm |
| 6m | G#4 | G#m/Abm |
| 7m | D#4 | D#m/Ebm |
| 8m | A#4 | A#m/Bbm |
| 9m | F4 | Fm |
| 10m | C4 | Cm |
| 11m | G4 | Gm |
| 12m | D4 | Dm |

## 🎛️ Allen & Heath K2/K3 Usage

### Deck Selection Model

The 4 deck selector buttons follow a two-state interaction model:

| Action | LED State | Effect |
|--------|-----------|--------|
| **Single-click** (non-active deck) | Red (preview) | Temporary preview, active deck unchanged |
| **Double-click** (within 320ms) | Yellow (selected) | Selects deck, sends to bridge |
| **Press** (already selected) | Yellow (selected) | Clears preview, keeps selection |
| **Active deck notification** | — | Bridge receives `POST /activeDeck/{A\|B\|C\|D}` |

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

## 🚀 Quick Start - Traktor to Bitwig Integration

### Prerequisites
1. ✅ Bitwig Studio 24+
2. ✅ Native Instruments Traktor 3/4
3. ✅ Virtual MIDI cable (IAC on macOS, loopMIDI on Windows)
4. ✅ K2/K3 controller (or other supported controller)

### Setup Steps

1. **macOS: Enable IAC Driver**
   ```
   Open: Applications > Utilities > Audio MIDI Setup
   Click: Audio Devices
   Double-click: IAC Driver
   Add a virtual port (e.g., "Traktor Bitwig")
   ```

2. **Build & Load Extension**
   ```bash
   cd /path/to/rhbitwig-traktor-bitwig-bridge
   mvn clean install
   ```

3. **Configure Bitwig**
   - Open Bitwig Studio
   - Preferences → Controllers → Add Controller
   - Select "TraktorBridge" from the list
   - Choose your virtual MIDI port
   - Restart Bitwig

4. **Install Traktor Script**
   - Copy `scripts/traktor-api-script.dn64` to Traktor's Scripts folder
   - Restart Traktor
   - Bridge auto-connects to `localhost:8090`

5. **Test the Connection**
   - Load a track in Traktor deck A
   - Check Bitwig console for deck key output
   - Double-click K2 deck A button
   - Bitwig should receive CC16 and CC17 values
   - The **global project root key** automatically updates via CC16/CC17
   - All instruments in Bitwig will respond to the new root key (no per-track devices needed)

### Troubleshooting

**Bridge not responding?**
- Check that Traktor script is installed and Traktor restarted
- Verify virtual MIDI port is active
- Check Bitwig console for connection errors

**Keys not detected?**
- Ensure track metadata includes Camelot key in Traktor
- Wait 2 seconds for the bridge to receive initial key data
- Restart Traktor if key data is stale

**CC16 values incorrect?**
- Verify MIDI channel is set to 10 in Bitwig
- Check that Note Transpose device is receiving CC16
- Console log shows expected vs. actual CC value

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
├── README.md                             # This file
└── scripts/
    └── traktor-api-script.dn64           # Traktor API script
```

## 🔌 HTTP Bridge API

### Global Root Key Control

The bridge sends **CC16 and CC17** on **MIDI channel 10** to control Bitwig's global project root key:

- **CC16**: Chromatic pitch class (0-127, mapped to 12 equal semitone divisions)
- **CC17**: Mode indicator (10 = minor key, 0 = major key)

This affects all instruments in the Bitwig project globally—no per-track devices required.

### Endpoints

**Deck Key Update (Traktor → Bitwig)**
```
POST /deckKey/{deck}
Content-Type: application/json

{
  "deck": "A",
  "key": "1d",
  "label": "C",
  "mode": "major"
}
```

**Active Deck Notification (K2 → Bitwig)**
```
POST /activeDeck/{deck}

Deck: A, B, C, or D
```

**Status Check**
```
GET /status

Response: 
{
  "bridge": "online",
  "decks": { "A": "1d", "B": "unknown", "C": "unknown", "D": "unknown" }
}
```

## 📚 Development

### Adding Custom Controller Support

Extend `com.bitwig.extension.controller.api.ControllerExtension`:

```java
public class MyControllerExtension extends ControllerExtension {
  @Override
  protected void init() {
    // Initialize your controller
  }

  @Override
  protected void exit() {
    // Cleanup
  }

  @Override
  protected String getExtensionVersion() {
    return "1.0.0";
  }
}
```

Register in corresponding `*ExtensionDefinition` class.

### Running Tests

```bash
mvn test
```

### Debugging

Set breakpoints in your IDE (IntelliJ IDEA recommended) and attach debugger to running Java process.

Enable verbose logging in Bitwig console for bridge messages.

## 🐛 Troubleshooting

### Common Issues

**Extension not appearing in Bitwig?**
- Verify `.bwextension` file exists in `~/Documents/Bitwig Studio/Extensions/`
- Restart Bitwig
- Check Bitwig console for loading errors

**Traktor integration not working?**
- Confirm virtual MIDI port is created and active
- Verify Traktor API script is installed
- Check that Traktor is sending key data (inspect network calls)
- Ensure Bitwig controller is set to "TraktorBridge"

**Wrong keys being sent?**
- Verify Traktor track metadata contains correct Camelot notation
- Check that key mapping table in `DeckManager.java` is correct
- Console log shows all transformations for debugging

## 📜 License

[See LICENSE file](./LICENSE)

## 👨‍💻 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit changes (`git commit -am 'Add feature'`)
4. Push to branch (`git push origin feature/your-feature`)
5. Create Pull Request

## 🤝 Support & Issues

Found a bug? Have a feature request?

- **Report Issues**: [GitHub Issues](https://github.com/userdsp/rhbitwig-traktor-bitwig-bridge/issues)
- **Discussions**: [GitHub Discussions](https://github.com/userdsp/rhbitwig-traktor-bitwig-bridge/discussions)

## 🎵 Credits

Built on:
- Bitwig Extension API
- Native Instruments Traktor
- Community controller scripts

---

**Last Updated**: November 2026  
**Bitwig Studio Required**: 4.0 or later  
**Bitwig Extension API Version**: 24  
**Java Version**: 17+

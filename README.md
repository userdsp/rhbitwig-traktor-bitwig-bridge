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

### Step 2: Load Extension in Bitwig

1. Open **Bitwig Studio**
2. **Menu** → **Preferences** → **Controllers**
3. Click **"Add Controller"** and select **"YaeltexArpControl"**
4. Select your MIDI controller from the device list
5. Restart Bitwig if needed

### Step 3: Configure Traktor Bridge (Optional)

To use the Traktor integration with key/tonality synchronization:

1. Install the **[traktor-api-client](https://github.com/ErikMinekus/traktor-api-client)** by following its documentation
   - This client enables key/tonality sync between Traktor and Bitwig

2. Enable virtual MIDI:
   - **macOS**: Open `Audio MIDI Setup`, create an IAC Driver port
   - **Windows**: Install and configure loopMIDI or similar

3. In Bitwig controller settings:
   - Select **"TraktorBridge"** as controller
   - Assign the virtual MIDI port

4. The bridge auto-connects to Traktor via the traktor-api-client:
   - Receives deck key information from Traktor
   - Automatically synchronizes Bitwig's root key and scale with Traktor

## 🎼 Key Mapping: Traktor Open Key → Bitwig

The bridge automatically maps **Traktor Open Key notation** to **Bitwig root key and scale**:

| Traktor Key | Bitwig Root | Bitwig Scale | Mode |
|-------------|-------------|--------------|------|
| **1m** | A | Minor | Minor |
| **2m** | E | Minor | Minor |
| **3m** | B | Minor | Minor |
| **4m** | F# | Minor | Minor |
| **5m** | C# | Minor | Minor |
| **6m** | G# | Minor | Minor |
| **7m** | D# | Minor | Minor |
| **8m** | A# (Bb) | Minor | Minor |
| **9m** | F | Minor | Minor |
| **10m** | C | Minor | Minor |
| **11m** | G | Minor | Minor |
| **12m** | D | Minor | Minor |
| **1d** | C | Major | Major |
| **2d** | G | Major | Major |
| **3d** | D | Major | Major |
| **4d** | A | Major | Major |
| **5d** | E | Major | Major |
| **6d** | B | Major | Major |
| **7d** | F# | Major | Major |
| **8d** | C# | Major | Major |
| **9d** | G# | Major | Major |
| **10d** | D# | Major | Major |
| **11d** | A# (Bb) | Major | Major |
| **12d** | F | Major | Major |

**Legend:**
- `m` = Minor key
- `d` = Major key (Dur)
- When a track is loaded in Traktor, the bridge sends its key to Bitwig
- Bitwig automatically sets the **global project root key** and **scale** accordingly

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

### Mapping Configuration

To configure CC16/CC17 mapping in your MIDI controller:

1. **Double-click** a deck button → Bridge awaits MIDI input for 2 seconds
   - Send any MIDI CC on channel 10 → Mapped to **CC16** (root note)
   - LED briefly flashes to confirm

2. **Triple-click** a deck button (within 500ms) → Bridge awaits MIDI input for 2 seconds
   - Send any MIDI CC on channel 10 → Mapped to **CC17** (mode)
   - LED briefly flashes to confirm

3. Configuration is **automatically saved** and persists across restarts

**Example Flow**:
```
Controller Setup:
1. Double-click deck A button
2. Within 2 seconds, turn a knob/slider on your controller
3. LED flashes yellow → CC16 mapped!
4. Triple-click deck A button
5. Within 2 seconds, turn a different knob/slider
6. LED flashes yellow → CC17 mapped!
7. Now deck A will synchronize root key and mode automatically
```

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

3. **Install traktor-api-client**
   - Follow the installation instructions at [https://github.com/ErikMinekus/traktor-api-client](https://github.com/ErikMinekus/traktor-api-client)
   - This is required for key/tonality synchronization between Traktor and Bitwig
   - Start the traktor-api-client service

4. **Configure Bitwig**
   - Open Bitwig Studio
   - Preferences → Controllers → Add Controller
   - Select "TraktorBridge" from the list
   - Choose your virtual MIDI port
   - Restart Bitwig

5. **Test the Connection**
   - Load a track in Traktor deck A (track must have Traktor Open Key metadata)
   - Check Bitwig console for deck key output
   - Double-click K2 deck A button to select the deck
   - Bitwig's global project root key and scale automatically update
   - All instruments in the project now play in the correct key from Traktor!

### Troubleshooting

**Bridge not responding?**
- Verify that traktor-api-client is installed and running
- Verify virtual MIDI port is active
- Check Bitwig console for connection errors

**Keys not updating in Bitwig?**
- Ensure track in Traktor has Traktor Open Key metadata
- Check that bridge received the key via console output
- Verify the mapping matches the Traktor Open Key → Bitwig table
- Restart Bitwig if key mapping doesn't apply
- Double-click K2 deck button to manually trigger key update

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
- Bitwig's global key settings update automatically (no MIDI CC required)

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

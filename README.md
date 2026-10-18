# Verifone Serial Printer Connector for PrintCord

This application connects Verifone (and compatible ESC/POS serial receipt printers) to the PrintCord Discord bot system as described in `CONNECTOR.md`.

For complete installation, setup, and background service configuration instructions, see [INSTALL.md](INSTALL.md).

## Features
- **Cross-Platform Configuration Storage**:
  - **Windows**: `%APPDATA%\PrintCord\verifone-connector.json`
  - **macOS**: `~/Library/Application Support/PrintCord/verifone-connector.json`
  - **Linux / Unix**: `~/.config/printcord/verifone-connector.json` (or `$XDG_CONFIG_HOME/printcord/...`)
- **Printer & Serial Configuration**:
  - Interactive setup wizard to select available COM / TTY serial ports.
  - Configurable BAUD rate (e.g. 9600, 19200, 38400, 115200).
  - Configurable receipt column character width (e.g. 24, 32, 40, 48) with automated word wrapping.
  - Line feed customization.
- **Discord Bot Registration**:
  - Interactive prompt for the 8-character registration code (`XXXX-XXXX`) obtained via `/register` in Discord.
  - Automatic token exchange and configuration persistence.
- **Background Polling & Heartbeat**:
  - Periodic polling for incoming messages (`GET /api/connectors/poll`).
  - Automatic fallback heartbeat check (`POST /api/connectors/status`) to maintain active status.
  - Automatic handling of 401 unauthorized / deregistration events.
- **Deregistration**:
  - Supports manual connector-initiated deregistration (`--deregister`).

## Building & Running

### Requirements
- Java 21+
- Gradle (wrapper included)

### Build
```bash
./gradlew build
```

### Run Configuration Wizard
```bash
./gradlew run --args="--setup"
```

### Run Connector
```bash
# Interactive mode:
./gradlew run

# Daemon / background mode:
./gradlew run --args="--background"
```

### Command Line Options
- `--setup` / `--configure`: Launch interactive serial and printer configuration wizard.
- `--url <url>`: Override the PrintCord API base URL.
- `--background`, `-d`: Run in background mode without interactive CLI prompt.
- `--config <path>`: Specify custom configuration file location.
- `--deregister`: Deregister the printer connector from the PrintCord server.
- `--help`: Display available options.

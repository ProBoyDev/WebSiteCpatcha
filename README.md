# WebsiteCaptcha

**WebsiteCaptcha** is a Minecraft server plugin designed to enhance security by requiring players to verify themselves via a web-based CAPTCHA before accessing the normal game world. This plugin integrates with a SQLite database for persistent verification data and offers a variety of customization options to tailor the verification experience.

## Plugin Information

WebsiteCaptcha aims to protect your Minecraft server from unauthorized access by implementing a CAPTCHA verification system. Upon joining, players are sent to a configurable "Limbo" world where they must complete a CAPTCHA challenge hosted on a local web server (default: `localhost:8080`). Once verified, players are granted access to the main world. The plugin features an XP bar countdown timer, configurable game modes, flight options, and visual effects like blindness to manage unverified players effectively.

### Key Features
- **CAPTCHA Verification**: Players must complete a Google reCAPTCHA challenge via a web link.
- **Limbo World**: Unverified players are placed in a separate world with restricted permissions.
- **XP Bar Timer**: Displays remaining verification time, decreasing smoothly by percentage until timeout.
- **Configurable Settings**: Customize timeout duration, game mode, flight, blindness, and inventory hiding via `config.yml`.
- **Kick Delay**: On timeout, displays a configurable screen message for 5 seconds before kicking.
- **Version Detection**: Logs player Minecraft versions using ProtocolLib (supports 1.8 to 1.21.4).
- **Persistent Storage**: Uses SQLite (`verification.db`) to store verification timestamps and states.

### How It Works
1. **Player Joins**: The server logs the player’s IP, name, and version, then checks their verification status.
2. **Verification Check**: If unverified or the grace period has expired, the player is sent to Limbo.
3. **CAPTCHA Prompt**: A clickable chat message provides a link to the CAPTCHA page.
4. **Timeout Mechanism**: An XP bar counts down; if time runs out, a kick message appears for 5 seconds before disconnection.
5. **Success**: Upon CAPTCHA completion, the player is sent to the normal world with restored settings.

### Installation
1. **Dependencies**:
   - **Bukkit/Spigot**: Compatible with versions 1.8 to 1.21.4.
   - **ProtocolLib**: Required for version detection (`getPlayerVersion`).
2. **Steps**:
   - Download the latest `WebsiteCaptcha.jar` from the [Releases](https://github.com/yourusername/WebsiteCaptcha/releases) page.
   - Place it in your server’s `plugins` folder.
   - Install ProtocolLib and ensure it’s loaded.
   - Start the server to generate `config.yml` in `plugins/WebsiteCaptcha/`.
   - Configure `config.yml` with your reCAPTCHA keys and preferences.

### Configuration
The plugin uses a `config.yml` file for customization. Below is the default configuration:

```yaml
options:
  verification_timeout: 40  # Time (in seconds) players have to verify before being kicked, shown as XP levels
  verification_grace_period: 86400000  # Duration (in milliseconds) a verification remains valid
  game_mode: ADVENTURE  # Game mode for unverified players in Limbo (SURVIVAL, SPECTATOR, ADVENTURE, CREATIVE)
  apply_blindness: true  # Apply blindness effect to unverified players
  hide_inventory: true  # Hide inventory of unverified players
  prevent_unverified_player_movement: true  # Prevent movement for unverified players

Web:
  host: "localhost"  # Host for the CAPTCHA web server
  port: 8080  # Port for the CAPTCHA web server

recaptcha:
  site_key: "your_site_key"  # Google reCAPTCHA site key
  secret_key: "your_secret_key"  # Google reCAPTCHA secret key

messages:
  verification_prompt: "&ePlease verify yourself by clicking the link below:"  # Prompt message with verification link
  verification_link_text: "&a[Click Here]"  # Text for the clickable verification link
  verification_success: "&aVerification successful! Welcome to the server."  # Success message after verification
  already_verified: "&aYou are already verified."  # Message for already verified players
  inventory_hidden: "&eYour inventory has been hidden until verification."  # Inventory hidden message
  inventory_restored: "&aYour inventory has been restored."  # Inventory restored message
  verification_timeout_kick: "&cYou took too long to verify!"  # Kick message on timeout
  verification_timeout_kick_screen: "You were kicked because you did not verify in time. Please rejoin and verify."  # Screen message before kick

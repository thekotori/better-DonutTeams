<p align="center">
<img src="" alt="Soon :)" width="1000"/>
</p>

<p align="center">
<div align="center">
    <h1>🍩 justTeams</h1>
    <h3><em>The Ultimate Team Management Experience</em></h3>
</div>
</p>

<p align="center">
<strong>A powerful and modern Minecraft teams plugin, fully customizable for any Paper-based server.</strong>
</p>

<p align="center">
<img src="https://img.shields.io/badge/Version-1.0.0-brightgreen?style=for-the-badge" alt="Version" />
<img src="https://img.shields.io/badge/API-1.21+-blue?style=for-the-badge" alt="API Version" />
<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge" alt="Java" />
</p>

## 👋 Welcome to justTeams!

Welcome to the official documentation for **justTeams**. This guide will walk you through every feature, from basic team creation to advanced cross-server management, ensuring you can harness the full power of the plugin with confidence.

---

## ✨ Features

✅ **High Performance**: Built for modern servers with support for Paper and Folia.  
🛡️ **Advanced Team Management**: Role-based system with Owner, Co-Owner, and Member permissions.  
🌐 **Proxy Support**: Real-time synchronization of chats, banks, and storage across your Velocity/BungeeCord network.  
🏦 **Team Economy**: Shared team bank and ender chest storage with Vault integration.  
🏠 **Teleportation System**: Secure team homes and warps with configurable warmups and cooldowns.  
🎨 **Rich Customization**: Fully configurable messages, GUIs, and command settings with MiniMessage support.  
💬 **Integrated Team Chat**: Easy-to-use team chat mode and quick message commands.  
🧩 **Integrations**: Seamless support for Vault economy and PlaceholderAPI.  

---

## 🚀 Installation

### Basic Setup
1. Download `justTeams.jar`.
2. Place the jar file in your server's `/plugins` directory.
3. Install **Vault** (required for economy) and **PlaceholderAPI** (recommended).
4. Restart the server to generate the configuration files.

### Cross-Server Setup
For network-wide team synchronization:

- Deploy `justTeams.jar` on all backend servers (e.g., Survival, Factions).
- Configure a shared MySQL database in `config.yml` by setting the storage type to `MYSQL`.
- Ensure all servers can connect to the same MySQL instance with the correct credentials.

---

## ⚙️ Configuration

justTeams uses modular configuration files for clean organization:

**Core Files:**
- `config.yml` - Main settings, database options, feature toggles.
- `messages.yml` - All user-facing text output with MiniMessage support.
- `gui.yml` - Configuration for all graphical user interfaces.
- `commands.yml` - Command aliases and settings.

**Key Configuration Options:**

| Setting | Description |
|---|---|
| `settings.max_team_size` | The maximum number of players allowed in a single team. |
| `storage_type` | Set to `H2` for single-server or `MYSQL` for network-wide sync. |
| `team_bank.enabled` | Toggles the shared team bank feature. |
| `team_home.warmup_seconds` | The delay before a player teleports to the team home. |
| `team_home.cooldown_seconds`| The time a player must wait between uses of `/team home`. |
| `default_pvp_status` | The default friendly-fire status for newly created teams. |
| `team_enderchest` | Enables and configures the shared team ender chest. |

---

## ⌨️ Commands

### Player Commands
| Command | Description | Permission |
|---|---|---|
| `/team create <name> <tag>` | Create a new team. | `justteams.user` |
| `/team invite <player>` | Invite a player to your team. | `justteams.user` |
| `/team accept <team>` | Accept a pending team invitation. | `justteams.user` |
| `/team leave` | Leave your current team. | `justteams.user` |
| `/team info` | Display information about your team. | `justteams.user` |
| `/team home` | Teleport to your team's home. | `justteams.user` |
| `/team bank` | Open the shared team bank GUI. | `justteams.user` |
| `/team enderchest` | Open the shared team ender chest. | `justteams.user` |
| `/team chat` | Toggle team-only chat mode. | `justteams.user` |
| `/t <message>` | Send a single message to team chat. | `justteams.user` |

### Admin & Management Commands
| Command | Description | Permission |
|---|---|---|
| `/team kick <player>` | Kick a player from your team. | Owner/Co-Owner |
| `/team promote <player>` | Promote a member to Co-Owner. | Owner/Co-Owner |
| `/team demote <player>` | Demote a Co-Owner to Member. | Owner/Co-Owner |
| `/team transfer <player>` | Transfer ownership of the team. | Owner |
| `/team disband` | Disband your team permanently. | Owner |
| `/team pvp` | Toggle friendly-fire for the team. | Owner/Co-Owner |
| `/team public` | Toggle if your team is open to invites. | Owner/Co-Owner |
| `/team sethome` | Set the team's home location. | Owner/Co-Owner |
| `/team admin` | Admin commands for managing all teams. | `justteams.admin` |

---

## ⚔️ Team Management & Features

Manage your team effectively using a powerful suite of features.

**Role System:**
- **Owner**: Full control over the team, including disbanding and ownership transfer.
- **Co-Owner**: Can manage members, set the team home, and toggle settings like PvP.
- **Member**: Basic permissions to participate in chat, use the bank, and teleport home.

**Team Bank & Storage:**
- Access a shared Vault-based economy balance with `/team bank`.
- Store items in a shared ender chest inventory using `/team enderchest`.
- All balances and items are synchronized across the network when using MySQL.

**Homes & Warps:**
- Set a central home point for your team with `/team sethome`.
- All members can teleport to this location via `/team home`, subject to warmup and cooldown timers.
- Create shared team warps for easy access to important locations with `/team setwarp <name>`.

---

## 🔑 Permissions

### Core Permissions
| Permission | Description | Default |
|---|---|---|
| `justteams.user` | Access to all basic player commands. | true |
| `justteams.admin` | Access to administrative commands to manage all teams. | op |
| `justteams.*` | Grants access to a

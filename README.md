
# 🍩 BetterDonutTeams

A powerful and modern Minecraft teams plugin, built for DonutSMP and fully customizable for any Paper-based server.

<p align="center">
<img src="https://img.shields.io/badge/Author-kotori-lightgrey?style=for-the-badge" alt="Author" />
<img src="https://img.shields.io/badge/API-1.21-brightgreen?style=for-the-badge" alt="API Version" />
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

---

## ✨ Features

- 👥 **Full Team Management** — Create, invite, kick, disband, promote, and demote members.
- 🏦 **Team Bank** — Let teams pool their money with Vault integration.
- 🗄️ **Team Ender Chest** — A shared, persistent inventory for every team.
- 🔐 **Granular Permissions** — Control member actions like bank withdrawal, ender chest access, and home management directly from the GUI.
- 🏆 **Stats & Leaderboards** — Track team kills, deaths, and KDR, and display top teams.
- 🎨 **Highly Customizable** — Change all messages, GUIs, colors, and plugin settings.
- 💾 **Flexible Storage** — Use local H2 or MySQL for multi-server setups.
- ⚙️ **Automatic Migration** — Update the plugin without resetting configs or database schemas.
- 🏠 **Team Homes** — Set and teleport to a team home with a configurable cooldown and warmup.
- ⚔️ **PvP Toggle** — Enable or disable friendly fire on a per-team basis.
- 💬 **Private Team Chat** — Toggle team-only chat or use a command for single messages.
- 🖥️ **Full GUI Interface** — Manage everything visually, from team settings and banking to individual member permissions.
- 💎 **MiniMessage Support** — Use modern text formatting like gradients, click actions, and hover text in all messages and GUIs.
- 🧩 **Full PlaceholderAPI Support** — Display detailed team information anywhere.

---

## 📦 Installation

1. Download the latest BetterDonutTeams.jar.
2. Stop your Minecraft server.
3. Place the file in the `plugins/` folder.
4. (Required for Bank) Install **Vault**.
5. (Recommended) Install **PlaceholderAPI**.
6. Start the server to generate configuration files.
7. Edit `config.yml`, `messages.yml`, and `gui.yml` as desired.
8. Reload the config with `/team reload`.

---

## ⚙️ Configuration

### config.yml

```yaml
# ---------------------------------------------------- #
#           BetterDonutTeams Configuration             #
# ---------------------------------------------------- #
# This file contains all the main settings for the plugin.
# For message customization, please see messages.yml.
# For GUI customization, please see gui.yml.
#
# You can use /team reload to apply changes without a server restart.
# ---------------------------------------------------- #

# CONFIG VERSION - DO NOT CHANGE THIS
config-version: 7

# ---------------------------------------------------- #
#                  Storage Settings                    #
# ---------------------------------------------------- #
storage:
  type: "h2" # Options: "h2" (local) or "mysql" (networked)

  mysql:
    enabled: false
    host: "localhost"
    port: 3306
    database: "donutsmp"
    username: "root"
    password: ""

# ---------------------------------------------------- #
#                   General Settings                   #
# ---------------------------------------------------- #
settings:
  main_color: "#4C9DDE"
  accent_color: "#4C96D2"
  max_team_size: 10
  min_name_length: 3
  max_name_length: 16
  max_tag_length: 6
  max_description_length: 64
  default_pvp_status: true

# ---------------------------------------------------- #
#                 Team Home Settings                   #
# ---------------------------------------------------- #
team_home:
  warmup_seconds: 5
  cooldown_seconds: 300 # 5 minutes

# ---------------------------------------------------- #
#                  Team Bank Settings                  #
# ---------------------------------------------------- #
team_bank:
  enabled: true
  max_balance: 1000000.0

# ---------------------------------------------------- #
#              Team Ender Chest Settings               #
# ---------------------------------------------------- #
team_enderchest:
  enabled: true
  rows: 3 # 3 rows = 27 slots

# ---------------------------------------------------- #
#              Visual and Sound Effects                #
# ---------------------------------------------------- #
effects:
  sounds:
    enabled: true
    success: "ENTITY_PLAYER_LEVELUP"
    error: "ENTITY_VILLAGER_NO"
    teleport: "ENTITY_ENDERMAN_TELEPORT"
  particles:
    enabled: true
    teleport_warmup: "PORTAL"
    teleport_success: "END_ROD"

# ---------------------------------------------------- #
#                   Webhook Settings                   #
# ---------------------------------------------------- #
webhook:
  enabled: true
  server-name: "ServerName"
```

---

### messages.yml (Supports MiniMessage)

```yaml
prefix: "<b><gradient:#4C9DDE:#4C96D2>ᴛᴇᴀᴍs</gradient></b> <dark_gray>| <gray>"

team_created: "<green>You have successfully created the team <white><team></white>."
bank_deposit_success: "<green>You deposited <white><amount></white> into the team bank."
invite_received: |
  You have been invited to join <white><team></white>.
  <click:run_command:/team accept <team>><hover:show_text:'<green>Click to accept!'><green>[Accept]</hover></click> or <click:run_command:/team deny <team>><hover:show_text:'<red>Click to deny!'><red>[Deny]</hover></click>
```

---

## ⌨️ Commands & Permissions

### 🔧 Commands

| Command                                | Aliases                | Description                          |
|--------------------------------------|------------------------|------------------------------------|
| `/team`                              | t, guild, clan, party  | Open the team management GUI.      |
| `/team create <name> <tag>`          | -                      | Create a new team.                  |
| `/team disband`                      | -                      | Disband your team (owner only).    |
| `/team invite <player>`              | -                      | Invite a player to your team.      |
| `/team accept <team>`                | -                      | Accept a team invitation.           |
| `/team deny <team>`                  | -                      | Deny a team invitation.             |
| `/team leave`                       | -                      | Leave your current team.            |
| `/team kick <player>`                | -                      | Kick a member from your team.       |
| `/team promote <player>`             | -                      | Promote a member to Co-Owner.       |
| `/team demote <player>`              | -                      | Demote a Co-Owner to Member.        |
| `/team info [team]`                  | -                      | Show detailed team information.     |
| `/team chat`                        | c                      | Toggle team-only chat.               |
| `/team sethome`                     | -                      | Set the team's home location.       |
| `/team home`                       | -                      | Teleport to the team home.           |
| `/team settag <tag>`                 | -                      | Change your team's tag.              |
| `/team setdesc <desc>`               | setdescription          | Change your team's description.     |
| `/team transfer <player>`            | -                      | Transfer ownership to another member. |
| `/team pvp`                        | -                      | Toggle friendly fire within the team. |
| `/team bank`                        | -                      | Open the team bank GUI.              |
| `/team bank <deposit/withdraw> <amount>` | -                  | Quickly manage the team bank.       |
| `/team enderchest`                  | ec                     | Open the shared team ender chest.   |
| `/team top`                        | leaderboard            | View team leaderboards.              |
| `/team admin`                      | -                      | Open the admin management GUI.      |
| `/team reload`                     | -                      | Reload plugin configs (admin only). |
| `/teammsg <message>`                | tm, tmsg, guildmsg, etc.| Send a single message to your team chat. |

---

### 🔐 Permissions

BetterDonutTeams uses a hybrid permission system:

- **Bukkit Permissions:** Control access to commands.
- **Internal Team Roles (Owner, Co-Owner, Member):** Control team actions.
- **Internal Member Flags:** Specific permissions managed by Owners.
- **Bypass Permissions:** Admin-level nodes overriding checks.

| Permission                          | Description                                      | Default   |
|-----------------------------------|------------------------------------------------|-----------|
| donutteams.*                      | Grants all permissions for the plugin.          | op        |
| donutteams.admin                  | Grants access to all admin commands and bypasses. | op        |
| donutteams.user                   | Grants access to all standard player commands.  | true      |
| donutteams.bypass.bank.withdraw  | Bypass team-specific withdrawal permissions.    | op        |
| donutteams.bypass.enderchest.use | Bypass team-specific ender chest permissions.   | op        |
| donutteams.bypass.home.cooldown  | Bypass the /team home teleport cooldown.         | op        |
| donutteams.command.admin          | Access to /team admin command and GUI.           | op        |
| donutteams.command.reload         | Reload plugin configuration.                      | op        |
| donutteams.command.create         | Create a team.                                    | true      |
| donutteams.command.disband        | Disband a team.                                   | true      |
| donutteams.command.invite         | Invite players to a team.                         | true      |
| donutteams.command.accept         | Accept a team invite.                             | true      |
| donutteams.command.deny           | Deny a team invite.                               | true      |
| donutteams.command.leave          | Leave a team.                                     | true      |
| donutteams.command.kick           | Kick members from a team.                         | true      |
| donutteams.command.info           | View team information.                            | true      |
| donutteams.command.chat           | Toggle team chat.                                 | true      |
| donutteams.command.sethome        | Set the team home.                                | true      |
| donutteams.command.home           | Teleport to the team home.                        | true      |
| donutteams.command.settag         | Change the team tag.                              | true      |
| donutteams.command.setdescription | Change the team description.                      | true      |
| donutteams.command.transfer       | Transfer team ownership.                          | true      |
| donutteams.command.promote        | Promote a team member.                            | true      |
| donutteams.command.demote         | Demote a team member.                             | true      |
| donutteams.command.pvp            | Toggle team PvP.                                  | true      |
| donutteams.command.bank           | Use the team bank.                                | true      |
| donutteams.command.enderchest     | Use the team ender chest.                         | true      |
| donutteams.command.top            | View team leaderboards.                           | true      |
| donutteams.command.teammsg        | Send messages to team chat.                       | true      |

**Recommended Setup:**

- Players: `donutteams.user`
- Admins: `donutteams.admin`

---

## 🧩 PlaceholderAPI

BetterDonutTeams supports PlaceholderAPI out of the box.

| Placeholder                 | Description                    |
|-----------------------------|--------------------------------|
| `%donutteams_name%`          | Player's team name             |
| `%donutteams_tag%`           | Team's tag                    |
| `%donutteams_description%`   | Team description              |
| `%donutteams_owner%`         | Team owner's name             |
| `%donutteams_member_count%`  | Current number of members      |
| `%donutteams_max_members%`   | Max team size                 |
| `%donutteams_members_online%`| Online team members           |
| `%donutteams_role%`          | Player's role (Owner, Co-Owner, or Member) |
| `%donutteams_kills%`         | Total team kills              |
| `%donutteams_deaths%`        | Total team deaths             |
| `%donutteams_kdr%`           | Team K/D ratio                |
| `%donutteams_bank_balance%`  | Formatted team bank balance   |

---

<p align="center">Made with ❤️ by <strong>kotori</strong></p>

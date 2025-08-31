<div align="center">

# 🍩 justTeams

### *The Ultimate Team Management Experience*

[![Author](https://img.shields.io/badge/Author-kotori-lightgrey?style=for-the-badge&logo=github)](https://github.com/kotori)
[![API](https://img.shields.io/badge/API-1.21-brightgreen?style=for-the-badge&logo=java)](https://papermc.io/downloads)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge&logo=opensourceinitiative)](LICENSE)
[![Folia](https://img.shields.io/badge/Folia-Supported-green?style=for-the-badge&logo=serverless)](https://folia.papermc.io/)

*A powerful and modern Minecraft teams plugin, fully customizable for any Paper-based server*

---

</div>

## 🚀 **Quick Start**

```bash
# Download and install
wget https://github.com/kotori/justTeams/releases/latest/download/justTeams.jar
mv justTeams.jar plugins/

# Install dependencies
# - Vault (required for economy)
# - PlaceholderAPI (recommended)
```

---

## ✨ **Why Choose justTeams?**

| Feature | Description | Benefit |
|---------|-------------|---------|
| 🎯 **Advanced Management** | Role-based hierarchy with granular permissions | Complete control over team operations |
| 🌐 **Cross-Server Sync** | Real-time synchronization across your network | Seamless multi-server experience |
| 🎨 **Modern GUI** | Beautiful, responsive interface with animations | Intuitive user experience |
| 🏦 **Team Economy** | Shared banking with Vault integration | Collaborative financial management |
| 🗄️ **Shared Storage** | Team ender chest with cross-server access | Persistent inventory across servers |
| 🏠 **Teleportation** | Team homes and warps with security | Easy team navigation |

---

## 🎯 **Core Features**

### 👥 **Team Management System**
<div align="center">

| Role | Permissions | Management |
|------|-------------|------------|
| **👑 Owner** | Full control | Create, disband, transfer |
| **🔄 Co-Owner** | Elevated access | Promote, demote, kick |
| **👤 Member** | Basic access | View, chat, use features |

</div>

- **Individual member flags** for granular control
- **Anti-spam protection** built-in
- **Security validation** prevents exploits
- **Role inheritance** system

### 🏦 **Team Banking & Economy**
<div align="center">

| Feature | Description |
|---------|-------------|
| 💰 **Shared Funds** | Pool money with your team |
| 🔐 **Permission Control** | Control who can withdraw |
| 📊 **Transaction Logging** | Track all financial activities |
| ⚡ **Quick Commands** | Fast deposit/withdraw from chat |

</div>

### 🗄️ **Team Storage & Teleportation**
<div align="center">

| System | Features |
|--------|----------|
| 📦 **Ender Chest** | 1-6 rows, cross-server sync |
| 🏠 **Team Home** | Warmup/cooldown, cross-server |
| 🎯 **Team Warps** | Up to 5 warps, password protection |
| 🔒 **Security** | Permission-based access control |

</div>

---

## 🎨 **Beautiful GUI Interface**

<div align="center">

### **Main Team GUI**
![Team GUI](https://i.ibb.co/rYxW7wr/AD12-B88-B-901-C-40-F3-884-B-6-EF243082-F4-A.png)

**Features:**
- ✨ **Modern Design** with gradients and animations
- 📱 **Responsive Layout** adapts to any screen
- 🔍 **Smart Sorting** by role, join date, online status
- ⚡ **Quick Actions** for common tasks

</div>

<div align="center">

### **Member Management**

**Capabilities:**
- 👑 **Role Management** promote, demote, transfer
- 🔐 **Permission Editing** individual member flags
- 👁️ **Self-View Mode** owners can view their profile
- 🛡️ **Security Features** prevent self-modification

</div>

---

## ⚙️ **Configuration**

### **Main Configuration (`config.yml`)**
```yaml
# 🎨 Team Settings
settings:
  main_color: "#4C9DDE"           # Primary theme color
  accent_color: "#4C96D2"         # Secondary accent color
  max_team_size: 10               # Maximum team members
  default_pvp_status: true        # Default PvP setting
  
# 🌐 Cross-Server Settings
  enable_cross_server_sync: true
  sync_optimization:
    heartbeat_interval: 60        # Server heartbeat
    cross_server_sync_interval: 15 # Data sync interval

# 🏦 Economy Settings
team_bank:
  enabled: true
  max_balance: 1000000.0         # Maximum bank balance

# 🏠 Teleportation Settings
team_home:
  warmup_seconds: 5              # Teleport warmup
  cooldown_seconds: 300          # Teleport cooldown
```

### **Message Customization (`messages.yml`)**
```yaml
# 🎨 MiniMessage Support
prefix: "<bold><gradient:#4C9DDE:#4C96D2>ᴛᴇᴀᴍs</gradient></bold> <dark_gray>| <gray>"

# ✨ Team Creation
team_created: "<green>✨ Team <white><team></white> has been created successfully!"

# 💰 Bank Transactions
bank_deposit_success: "<green>💰 Deposited <white><amount></white> into team bank"
bank_withdraw_success: "<green>💸 Withdrew <white><amount></white> from team bank"

# 🎯 Interactive Invitations
invite_received: |
  <yellow>🎯 You've been invited to join <white><team></white>!
  
  <click:run_command:/team accept <team>>
  <hover:show_text:'<green>Click to accept!'>🟢 [Accept]</hover>
  </click>
  
  <click:run_command:/team deny <team>>
  <hover:show_text:'<red>Click to deny!'>🔴 [Deny]</hover>
  </click>
```

---

## ⌨️ **Commands Reference**

<div align="center">

### **🔧 Core Commands**

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/team` | `t, g, c, p` | Show team info or help | `justteams.user` |
| `/team create <name> <tag>` | - | Create a new team | `justteams.command.create` |
| `/team invite <player>` | - | Invite a player | `justteams.command.invite` |
| `/team accept <team>` | - | Accept invitation | `justteams.command.accept` |
| `/team leave` | - | Leave your team | `justteams.command.leave` |
| `/team gui` | - | Open team GUI | `justteams.command.gui` |

</div>

<div align="center">

### **🏠 Home & Warp Commands**

| Command | Description | Permission |
|---------|-------------|------------|
| `/team sethome` | Set team home | `justteams.command.sethome` |
| `/team home` | Teleport home | `justteams.command.home` |
| `/team setwarp <name>` | Create warp | `justteams.command.setwarp` |
| `/team warp <name>` | Use warp | `justteams.command.warp` |

</div>

<div align="center">

### **🏦 Economy Commands**

| Command | Description | Permission |
|---------|-------------|------------|
| `/team bank` | Open bank GUI | `justteams.command.bank` |
| `/team bank deposit <amount>` | Quick deposit | `justteams.command.bank` |
| `/team bank withdraw <amount>` | Quick withdraw | `justteams.command.bank` |
| `/team enderchest` | `ec` | Open shared storage | `justteams.command.enderchest` |

</div>

---

## 🔐 **Permission System**

<div align="center">

### **Permission Hierarchy**

```
justteams.* (All permissions)
├── justteams.admin (Admin commands)
├── justteams.user (Basic commands)
└── justteams.bypass.* (Bypass restrictions)
    ├── justteams.bypass.bank.withdraw
    ├── justteams.bypass.enderchest.use
    ├── justteams.bypass.home.cooldown
    └── justteams.bypass.warp.cooldown
```

</div>

**Key Features:**
- 🔒 **Multi-layer security** with role-based access
- 🛡️ **Bypass permissions** for administrators
- 📊 **Granular control** over individual features
- 🔍 **Audit logging** for all actions

---

## 🧩 **PlaceholderAPI Integration**

<div align="center">

### **Available Placeholders**

| Category | Placeholder | Example Output |
|----------|-------------|----------------|
| **Team Info** | `%justteams_name%` | "TeamAlpha" |
| **Team Info** | `%justteams_tag%` | "[TA]" |
| **Team Info** | `%justteams_description%` | "The best team ever!" |
| **Member Info** | `%justteams_role%` | "Owner" |
| **Member Info** | `%justteams_member_count%` | "5" |
| **Statistics** | `%justteams_kills%` | "150" |
| **Statistics** | `%justteams_kdr%` | "2.00" |
| **Economy** | `%justteams_bank_balance%` | "$1,250.00" |

</div>

**Usage Examples:**
```yaml
# Scoreboard
scoreboard:
  title: "Team: %justteams_name%"
  lines:
    - "Role: %justteams_role%"
    - "Members: %justteams_member_count%/%justteams_max_members%"
    - "KDR: %justteams_kdr%"
    - "Balance: %justteams_bank_balance%"

# Chat Format
chat_format: "<%justteams_tag%> %player_name%: %message%"
```

---

## 🌐 **Cross-Server Features**

<div align="center">

### **Multi-Server Architecture**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Survival      │    │    Skyblock     │    │     Prison      │
│   Server        │◄──►│    Server       │◄──►│     Server      │
│                 │    │                 │    │                 │
│ • Team Data     │    │ • Team Data     │    │ • Team Data     │
│ • Real-time     │    │ • Real-time     │    │ • Real-time     │
│ • Sync          │    │ • Sync          │    │ • Sync          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         ▲                       ▲                       ▲
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   Proxy Server  │
                    │  (BungeeCord/   │
                    │    Velocity)    │
                    └─────────────────┘
```

</div>

**Features:**
- ⚡ **Real-time synchronization** across all servers
- 🔄 **Automatic data consistency** maintenance
- 🚀 **Optimized performance** with intelligent caching
- 🛡️ **Secure communication** between servers

---

## 🚀 **Performance & Optimization**

<div align="center">

### **Performance Metrics**

| Optimization | Benefit | Impact |
|--------------|---------|--------|
| **Lazy Loading** | Load data only when needed | ⚡ Faster startup |
| **Smart Caching** | Intelligent cache management | 🚀 Reduced latency |
| **Batch Processing** | Efficient bulk operations | 📈 Better throughput |
| **Connection Pooling** | Optimized database connections | 💾 Lower memory usage |
| **Async Operations** | Non-blocking background tasks | 🎯 Improved responsiveness |

</div>

**Advanced Configuration:**
```yaml
# Performance Tuning
settings:
  sync_optimization:
    heartbeat_interval: 60          # Server heartbeat
    cross_server_sync_interval: 15  # Data sync interval
    critical_sync_interval: 3       # Critical updates
    max_teams_per_batch: 25        # Teams per sync batch
    team_cache_ttl: 180            # Cache lifetime
    enable_optimistic_locking: true # Better concurrency
    max_sync_retries: 3            # Retry failed syncs
    sync_retry_delay: 1000         # Retry delay (ms)

# Database Optimization
storage:
  connection_pool:
    max_size: 16                    # Maximum connections
    min_idle: 4                     # Minimum idle connections
    connection_timeout: 30000       # Connection timeout
    idle_timeout: 600000            # Idle timeout
    max_lifetime: 1800000           # Max connection lifetime
    leak_detection_threshold: 60000 # Leak detection
```

---

## 🛡️ **Security & Protection**

<div align="center">

### **Security Features**

| Protection | Description | Benefit |
|------------|-------------|---------|
| 🔒 **Input Validation** | Comprehensive sanitization | Prevents exploits |
| 🛡️ **Permission Checks** | Multi-layer verification | Secure access control |
| 🚫 **Anti-Spam** | Rate limiting protection | Prevents abuse |
| 🗄️ **SQL Injection** | Prepared statements | Database security |
| 🧹 **Memory Management** | Automatic cleanup | Prevents leaks |

</div>

**Security Configuration:**
```yaml
# Security Settings
settings:
  # Anti-spam protection
  command_spam_protection: true
  message_spam_protection: true
  
  # Input validation
  validate_team_names: true
  validate_player_names: true
  
  # Content filtering
  filter_inappropriate_content: true
  max_message_length: 200
  
  # Rate limiting
  command_rate_limit: 5        # Commands per second
  message_rate_limit: 10       # Messages per second
```

---

## 📊 **Statistics & Analytics**

<div align="center">

### **Team Performance Dashboard**

| Metric | Description | Tracking |
|--------|-------------|----------|
| 🎯 **Combat Stats** | Kills, deaths, KDR | Real-time updates |
| 👥 **Member Activity** | Join dates, activity | Engagement metrics |
| 💰 **Financial Data** | Bank usage, transactions | Economic tracking |
| 📈 **Growth Stats** | Team development | Progress monitoring |

</div>

**Leaderboard Categories:**
- 🏆 **Top Teams by Kills** - Combat performance
- 💰 **Richest Teams** - Financial success
- 👥 **Largest Teams** - Member count
- 📊 **Best KDR** - Kill/Death ratio

---

## 🔄 **Migration & Updates**

<div align="center">

### **Seamless Update Process**

```
🔄 Current Version
    ↓
📦 Download Update
    ↓
💾 Automatic Backup
    ↓
🚀 Database Migration
    ↓
✅ Validation Check
    ↓
🎉 Update Complete
```

</div>

**Features:**
- 🔄 **Automatic migration** of all data
- 📁 **Config updates** without manual intervention
- 🔙 **Rollback protection** on failure
- 📊 **Version checking** and notifications

---

## 🤝 **Support & Community**

<div align="center">

### **Get Help & Connect**

| Resource | Description | Link |
|----------|-------------|------|
| 📚 **Documentation** | Complete API reference | [Soon](https://github.com/kotori/justTeams/wiki) |
| 🐛 **Issue Tracker** | Report bugs & request features | [GitHub Issues](https://github.com/kotori/justTeams/issues) |
| 💬 **Discord** | Community support & discussion | [Join Server](https://discord.gg/Am7D6Qz9) |
| 📖 **Examples** | Sample configurations | [Soon](https://github.com/kotori/justTeams/examples) |

</div>

---

<div align="center">

## 🎉 **Ready to Get Started?**

**justTeams** is the ultimate team management solution for modern Minecraft servers.

[![Download](https://img.shields.io/badge/Download-Latest-brightgreen?style=for-the-badge&logo=download)](https://builtbybit.com/resources/justteams.71401/)
[![Documentation](https://img.shields.io/badge/Documentation-Wiki-blue?style=for-the-badge&logo=book)](https://github.com/kotori/justTeams/wiki)
[![Support](https://img.shields.io/badge/Support-Discord-purple?style=for-the-badge&logo=discord)](https://discord.gg/Am7D6Qz9)

---

**🍩 justTeams** - *The ultimate team management solution for modern Minecraft servers*

*Built with ❤️ by [**kotori**](https://github.com/thekotori)*

</div>

package eu.kotori.justTeams.listeners;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
public class PvPListener implements Listener {
    private final TeamManager teamManager;
    public PvPListener(JustTeams plugin) {
        this.teamManager = plugin.getTeamManager();
    }
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player p) {
                attacker = p;
            }
        }
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        Team victimTeam = teamManager.getPlayerTeam(victim.getUniqueId());
        Team attackerTeam = teamManager.getPlayerTeam(attacker.getUniqueId());
        if (victimTeam == null || attackerTeam == null || victimTeam.getId() != attackerTeam.getId()) {
            return;
        }
        if (!victimTeam.isPvpEnabled()) {
            event.setCancelled(true);
        }
    }
}

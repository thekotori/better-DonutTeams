package eu.kotori.justTeams.listeners;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import me.NoChance.PvPManager.PvPManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPListener implements Listener {

    private final TeamManager teamManager;
    private final PvPManager pvpManager;

    public PvPListener(JustTeams plugin) {
        this.teamManager = plugin.getTeamManager();
        this.pvpManager = plugin.getPvpManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (pvpManager != null) {
            if (!pvpManager.getPlayerHandler().canAttack(attacker, victim)) {
                event.setCancelled(true);
                return;
            }
        }

        Team victimTeam = teamManager.getPlayerTeam(victim.getUniqueId());
        if (victimTeam == null) {
            return;
        }

        Team attackerTeam = teamManager.getPlayerTeam(attacker.getUniqueId());
        if (attackerTeam == null) {
            return;
        }

        if (victimTeam.getId() == attackerTeam.getId()) {
            if (!victimTeam.isPvpEnabled()) {
                event.setCancelled(true);
            }
        }
    }
}
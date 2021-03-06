package bernhard.scharrer.battlemobs.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import bernhard.scharrer.battlemobs.data.MobData;
import bernhard.scharrer.battlemobs.data.PlayerData;
import bernhard.scharrer.battlemobs.mobs.BattleMob;
import bernhard.scharrer.battlemobs.mobs.MobMaster;
import bernhard.scharrer.battlemobs.util.Cooldown;
import bernhard.scharrer.battlemobs.util.DamageHandler;
import bernhard.scharrer.battlemobs.util.Locations;
import bernhard.scharrer.battlemobs.util.MapHandler;
import bernhard.scharrer.battlemobs.util.PlayerUtils;
import bernhard.scharrer.battlemobs.util.Scoreboard;
import bernhard.scharrer.battlemobs.util.Task;

public class PlayerFakeDeathListener extends Listener {
	
	private static final PotionEffect FLASH = new PotionEffect(PotionEffectType.BLINDNESS, 20, 0);
	
	@EventHandler (priority=EventPriority.LOWEST)
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntity().getType() == EntityType.PLAYER) {
			Player p = (Player) e.getEntity();
			if (e.getCause() == DamageCause.FALL) e.setCancelled(true);
			else {
				if (p.getWorld() == Locations.lobby_world) e.setCancelled(true);
				if (e.getDamage() >= p.getHealth()) {
					kill(p, e);
				}
			}
		}
	}
	
	public static void kill(Player p, EntityDamageEvent e) {
		if (e!=null) { e.setCancelled(true); e.setDamage(0); }
		p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		Player k = null;
		k = DamageHandler.getLastDamage(p);
		if (k == null) for (Entity nearby : p.getNearbyEntities(15.0, 15.0, 15.0)) if (nearby instanceof Player) k = (Player) nearby;
		if (p.getWorld() != Locations.lobby_world) onPlayerDeath(p, k);
		p.addPotionEffect(FLASH);
	}
	
	/*
	 * FAKE DEATH EVENT
	 */
	private static void onPlayerDeath(Player p, Player k) {
		
		DamageHandler.triggerDeathListeners(p, k);
		
		PlayerData p_data=MobMaster.getPlayerData(p);
		MobData p_mob = p_data.getCurrentData();
		
		p.sendTitle("�a+3 EXP", "�a+10 EXP "+p_mob.getType().toString(),5,35,5);
		
		p_data.incrementDeaths();
		p_mob.incrementDeaths();
		
		p_data.addEXP(3);
		p_mob.addEXP(10);
		
		Scoreboard.updateScoreboard(p);
		
		if (k != null && k != p) {
			
			PlayerData k_data = MobMaster.getPlayerData(k);
			MobData k_mob = k_data.getCurrentData();
			
			k.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, 1, 1);
			
			k.sendTitle("�a+5 EXP", "�a+10$, +10 EXP "+p_mob.getType().toString(),5,15,5);
			
			k_data.incrementKills();
			k_mob.incrementKills();
			
			k_data.modifyMoney(10);
			k_data.addEXP(5);
			k_mob.addEXP(10);
			
			PlayerUtils.updateEXP(k);
			Scoreboard.updateScoreboard(k);
			Bukkit.broadcastMessage("�a" + k.getName() + "�7 killed �c" + p.getName()+"�7.");
		} else {
			Bukkit.broadcastMessage("�c" + p.getName() + " �7died.");
		}
		
		Cooldown.clearCooldowns(p);
		p.setGameMode(GameMode.SPECTATOR);
		p.setVelocity(new Vector(0, 2, 0));
		p.playSound(p.getLocation(), Sound.ENTITY_IRONGOLEM_DEATH, 1, 1);
		p.getWorld().playEffect(p.getLocation(), Effect.ENDER_SIGNAL, 100);
		p.addPotionEffect(FLASH);
		PlayerUtils.updateEXP(p);

		Task period = new Task(2,1) {
			
			private int seconds = 5;
			
			@Override
			public void run() {
				if (p!=null&&p.isOnline()&&p.getWorld().getName().equals(Locations.map_world.getName())&&p.getGameMode()==GameMode.SPECTATOR) {
					if (seconds>0) {
						p.resetTitle();
						p.sendTitle("�7Respawn in:", seconds+"s", 2,16,2);
						p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 1);
						seconds--;
					}
				}
				
			}
		};
		
		new Task(7) {
			
			@Override
			public void run() {
				if (p!=null&&p.isOnline()&&p.getWorld().getName().equals(Locations.map_world.getName())&&p.getGameMode()==GameMode.SPECTATOR) {
					BattleMob mob = MobMaster.getBattleMob(p_mob.getType());
					PlayerUtils.reset(p);
					p_data.setCurrent(mob.getType());
					MapHandler.teleportIntoMap(p, p_mob.getTier(), mob);
				}
				period.cancel();
				
			}
		};
		
	}
	
}

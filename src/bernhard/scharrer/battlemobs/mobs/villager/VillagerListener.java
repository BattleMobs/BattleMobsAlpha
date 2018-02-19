package bernhard.scharrer.battlemobs.mobs.villager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import bernhard.scharrer.battlemobs.data.PlayerData;
import bernhard.scharrer.battlemobs.mobs.MobListener;
import bernhard.scharrer.battlemobs.mobs.MobMaster;
import bernhard.scharrer.battlemobs.mobs.MobType;
import bernhard.scharrer.battlemobs.util.AsyncTask;
import bernhard.scharrer.battlemobs.util.Cooldown;
import bernhard.scharrer.battlemobs.util.Item;
import bernhard.scharrer.battlemobs.util.Task;
import bernhard.scharrer.battlemobs.util.Tier;
import net.minecraft.server.v1_12_R1.EnumParticle;
import net.minecraft.server.v1_12_R1.PacketPlayOutWorldParticles;

public class VillagerListener extends MobListener {
	
	private static final double BAD_TRADE_DAMAGE = 4;
	private static final double METALWORKING_POWER = 1.2;
	private static final String ANVIL_TAG = "񘿼�8";
	
	private static final double PAYBACK_RADIUS = 5.0;
	private static final int PAYBACK_COOLDOWN = 5;
	private static final float PAYBACK_TIME = 3.0f;
	private static final double PAYBACK_DAMAGE = 6;
	
	private static final double ANVIL_RADIUS = 5;
	private static final double ANVIL_DAMAGE = 10;
	private static final double ANVIL_POWER = 2.2;
	private static final int ANVIL_COOLDOWN = 5;
	private static final PotionEffect CONFUSE_EFFECT = new PotionEffect(PotionEffectType.CONFUSION, 30, 0);

	@EventHandler
	public void onInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Villager) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
			Player p = (Player) event.getDamager();
			LivingEntity enemy = (LivingEntity) event.getEntity();
			if (valid(p, MobType.VILLAGER)) {
				int tier = super.getMobTier(p);
				if (tier!=Tier.UNDEFINED && Item.valid(p.getInventory().getItemInMainHand())) {
					if (p.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains(VillagerItems.ABILITY_1_NAME)) {
						
						event.setCancelled(true);
						enemy.damage(BAD_TRADE_DAMAGE);
						
						Location ploc = p.getLocation();
						Location eloc = enemy.getLocation();
						
						enemy.teleport(ploc);
						
						float angle = (enemy.getLocation().getYaw()+180);
						
						p.teleport(eloc);
						Location loc = new Location(p.getWorld(), eloc.getX(), eloc.getY(), eloc.getZ(), angle, 0);
						p.teleport(loc);
						p.sendMessage("Angle: "+angle);
						
						p.playSound(enemy.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1.5f);
						if (enemy instanceof Player) {
							((Player)enemy).playSound(enemy.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1.5f);
						}
						
					}
					
					if (p.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains(VillagerItems.ABILITY_2_NAME)) {
						event.setCancelled(true);
						new Cooldown(p, 1, PAYBACK_COOLDOWN);
						
						for (Entity nearBy : p.getNearbyEntities(PAYBACK_RADIUS, PAYBACK_RADIUS, PAYBACK_RADIUS)) {
							if (nearBy instanceof LivingEntity) {
								
								new PaybackCurse((LivingEntity) nearBy, p);
								
							}
						}
					}
					
				}
			}
		}
	}
	
	private class PaybackCurse {
		
		public PaybackCurse(LivingEntity enemy, Player p) {
			
			new Task(PAYBACK_TIME) {
				public void run() {
					
					if (enemy!=null && !enemy.isDead()) {
						enemy.damage(PAYBACK_DAMAGE);
						if (enemy instanceof Player) {
							PlayerData edata = MobMaster.getPlayerData((Player) enemy);
							PlayerData pdata = MobMaster.getPlayerData(p);
							if (edata.getMoney()>0) {
								edata.modifyMoney(-1);
								pdata.modifyMoney(1);
							} else {
								
							}
						}
					}
					
				}
			};
			
			
			
		}
		
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (valid(event.getPlayer(), MobType.VILLAGER)) {
			
			Player p = event.getPlayer();
			
			if (Item.valid(p.getInventory().getItemInMainHand())) {
				if (p.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains(VillagerItems.ABILITY_3_NAME)) {
					
					FallingBlock block = p.getWorld().spawnFallingBlock(p.getEyeLocation(), new MaterialData(Material.ANVIL));
					block.setVelocity(p.getLocation().getDirection().normalize().add(new Vector(0, 0.5, 0)).normalize().multiply(METALWORKING_POWER));
					block.setCustomName(ANVIL_TAG+"#"+p.getName());
					block.setCustomNameVisible(false);
					
					new Cooldown(p, 2, ANVIL_COOLDOWN);
					
				}
			}
			
		}
	}
	
	@EventHandler
	public void onChange(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof FallingBlock) {
			FallingBlock fblock = (FallingBlock) event.getEntity();
			if (fblock.getMaterial()==Material.ANVIL && fblock.getCustomName().startsWith(ANVIL_TAG)) {
				String pname = fblock.getCustomName().split("#")[1];
				event.setCancelled(true);
				Location loc = fblock.getLocation();
				PacketPlayOutWorldParticles particles = new PacketPlayOutWorldParticles(EnumParticle.EXPLOSION_LARGE, true, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 1, 0, 0, 0, 0, 0);
				for (Entity nearBy : fblock.getNearbyEntities(25, 25, 25)) {
					if (nearBy instanceof Player) {
						Player p = (Player) nearBy;
						p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1, 1);
						p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
						((CraftPlayer) nearBy).getHandle().playerConnection.sendPacket(particles);
					}
				}
				
				for (Entity nearBy : fblock.getNearbyEntities(ANVIL_RADIUS, ANVIL_RADIUS, ANVIL_RADIUS)) {
					if (nearBy instanceof LivingEntity) {
						LivingEntity enemy = (LivingEntity) nearBy;
						if (!enemy.getCustomName().equals(pname)) {
							enemy.damage(ANVIL_DAMAGE);
							enemy.addPotionEffect(CONFUSE_EFFECT);
							enemy.setVelocity(enemy.getEyeLocation().toVector().subtract(fblock.getLocation().toVector()).normalize().multiply(ANVIL_POWER));
						}
					}
				}
				
			}
		}
	}
	
}

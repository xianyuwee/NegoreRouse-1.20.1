package net.xianyu.prinegorerouse.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Random;

public class ParticleUtils {

    public static void spawnParticle(SimpleParticleType type, LivingEntity player, int num, double rate) {
        Level world = player.level();
        Random rand = new Random();

        for (int i = 0; i < num ; i++){
            double xSpeed = rand.nextGaussian() * 0.02;
            double ySpeed = rand.nextGaussian() * 0.02;
            double zSpeed = rand.nextGaussian() * 0.02;

            double rx = rand.nextDouble();
            double ry = rand.nextDouble();
            double rz = rand.nextDouble();

            world.addParticle(
                    (ParticleOptions) type,
                    player.position().x + ((rx*2 - 1)*player.getBbWidth()  - xSpeed * 10.0)*rate,
                    player.position().y+1,
                    player.position().z + ((rz*2 - 1)*player.getBbWidth()  - zSpeed * 10.0)*rate,
                    xSpeed, ySpeed, zSpeed
            );
        }
    }
}

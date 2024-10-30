package net.xianyu.prinegorerouse.entity;

import net.minecraft.world.entity.Entity;

public interface IShootable {

    void shoot(double x, double y ,double z, float velocity, float inaccuracy);

    Entity getShooter();

    void setShooter(Entity shooter);

    double getDamage();

    boolean onImpact(Entity target, float damage);
}

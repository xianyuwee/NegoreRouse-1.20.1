package net.xianyu.prinegorerouse.utils;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class VectorHelper {
    static public Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3((double) (f3 * f4),(double) (-f5), (double) (f2 * f4));
    }

    /**
     * 将向量绕任意轴旋转指定角度（度数）
     *
     * @param vec 原始向量
     * @param axis 旋转轴（需归一化）
     * @param angleDegrees 旋转角度（度数）
     * @return 旋转后的新向量
     */
    public static Vec3 rotateVectorAroundAxis(Vec3 vec, Vec3 axis, float angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double dot = vec.dot(axis);

        // 罗德里格旋转公式
        double x = vec.x * cos + sin * (axis.y * vec.z - axis.z * vec.y) + axis.x * dot * (1 - cos);
        double y = vec.y * cos + sin * (axis.z * vec.x - axis.x * vec.z) + axis.y * dot * (1 - cos);
        double z = vec.z * cos + sin * (axis.x * vec.y - axis.y * vec.x) + axis.z * dot * (1 - cos);

        return new Vec3(x, y, z);
    }

    static public Vec3i f2i(Vec3 src) {
        return  new Vec3i(Mth.floor(src.x),Mth.floor(src.y),Mth.floor(src.z));
    }

    static public Vec3i f2i(double x,double y,double z) {
        return new Vec3i(Mth.floor(x),Mth.floor(y),Mth.floor(z));
    }

    static public Matrix4f matrix4fFromArray(float[] in) {
        return new Matrix4f(in[0], in[1], in[2], in[3], in[4], in[5], in[6],
                in[7], in[8], in[9], in[10], in[11], in[12], in[13], in[14], in[15]);
    }

    /**
     * 将向量绕Y轴旋转指定角度（度数）
     *
     * @param vec 原始向量
     * @param angleDegrees 旋转角度（度数）
     * @return 旋转后的新向量
     */
    public static Vec3 rotateVectorAroundY(Vec3 vec, float angleDegrees) {
        // 将角度转换为弧度
        double radians = Math.toRadians(angleDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        // 应用旋转矩阵（绕Y轴旋转）
        double newX = vec.x * cos - vec.z * sin;
        double newZ = vec.x * sin + vec.z * cos;

        // 返回新向量（Y分量不变）
        return new Vec3(newX, vec.y, newZ);
    }
}

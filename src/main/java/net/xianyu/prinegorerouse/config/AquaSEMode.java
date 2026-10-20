package net.xianyu.prinegorerouse.config;

/**
 * 特效（SpecialEffect）的时间素消耗模式。
 * <p>所有模式都以“消耗时间素激活”取代原本的经验等级需求；区别在于扣除时机：</p>
 * <ul>
 *   <li>{@link #ONE_TIME}：一次性买断，激活时扣除一次成本，之后永久生效，直到手动解除。</li>
 *   <li>{@link #PER_SECOND}：激活后每秒（20 tick）从时间素池中扣除一次成本，池耗尽自动解除。</li>
 *   <li>{@link #PER_HITS}：激活后每命中设定次数扣除一次成本，池耗尽自动解除。</li>
 *   <li>{@link #PER_SA}：激活后每次释放 SA 扣除一次成本，池耗尽自动解除。</li>
 * </ul>
 */
public enum AquaSEMode {
    ONE_TIME,
    PER_SECOND,
    PER_HITS,
    PER_SA;

    /** 是否为“持续消耗会耗尽”型（除一次性买断外的三种）。 */
    public boolean isDrain() {
        return this != ONE_TIME;
    }
}

package com.fc.v2.service.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 噪声投诉与超标处置办件流转纯口径（不碰库、不沾 Spring）。
 *
 * 四档先后钉死：受件(0)→核查(1)→责令(2)→复查收尾(3)。立件、挪关、封门、
 * 对账四个动作共用这一份口径：值守岗照文办事，走到哪一关一律由这里数出来，
 * 报文自带的关次只作参考、绝不入算。档数与时序只认本件这一趟点出的步，
 * 不先定死档数再回头找路径去配，也不拿邻档旧结论或别的件来套。
 *
 * @author fuce
 */
public final class CaseFlowRules {

    // ---- 四档（先后钉死，不许跳） ----
    /** 受件 */
    public static final int STAGE_INTAKE = 0;
    /** 核查 */
    public static final int STAGE_CHECK = 1;
    /** 责令 */
    public static final int STAGE_ORDER = 2;
    /** 复查收尾（末关） */
    public static final int STAGE_REVIEW = 3;
    /** 末关档数：走满即封 */
    public static final int LAST_STAGE = STAGE_REVIEW;

    /** 关名，按档数取，别处不许另写一套 */
    public static final List<String> STAGE_NAMES =
            Arrays.asList("受件", "核查", "责令", "复查收尾");

    // ---- 正式件情形 ----
    /** 在办（没封住；撤并作废旧档不再用） */
    public static final int STATUS_OPEN = 1;
    /** 已收口（封住/反映撤回/区册并走/末关作废，四者只看不能动） */
    public static final int STATUS_CLOSED = 2;

    // ---- 受件草稿情形 ----
    /** 待立 */
    public static final int INTAKE_PENDING = 0;
    /** 已立（已转正式件） */
    public static final int INTAKE_ESTABLISHED = 1;
    /** 已撤（立起来之前那一小会儿撤的；不进主档、不占没封住名额） */
    public static final int INTAKE_CANCELLED = 2;

    // ---- 收口种类 ----
    /** 走满末关封住 */
    public static final int CLOSE_SEAL = 1;
    /** 递件人受件之后自己撤回去 */
    public static final int CLOSE_WITHDRAW = 2;
    /** 所属区册从区册底里并走 */
    public static final int CLOSE_ZONE_MERGED = 3;
    /** 末关发起的作废 */
    public static final int CLOSE_VOID = 4;

    // ---- 留痕种类（与按语同串压在本件身后） ----
    /** 关次按语 */
    public static final int LOG_NOTE = 0;
    /** 封住 */
    public static final int LOG_SEAL = 1;
    /** 反映撤回 */
    public static final int LOG_WITHDRAW = 2;
    /** 区册并走 */
    public static final int LOG_ZONE_MERGED = 3;
    /** 末关作废 */
    public static final int LOG_VOID = 4;

    // ---- 挪关动作（last_action 只许这几种写法） ----
    public static final String ACTION_ESTABLISH = "立件";
    public static final String ACTION_FORWARD = "推进";
    public static final String ACTION_ROLLBACK = "收回";
    public static final String ACTION_NOTE = "按语";
    public static final String ACTION_ORDER_RECEIVED = "责令核收";
    public static final String ACTION_WITNESS = "见证落笔";
    public static final String ACTION_VOUCHER = "复测凭证写回";
    public static final String ACTION_SEAL = "封住";
    public static final String ACTION_WITHDRAW = "反映撤回";
    public static final String ACTION_ZONE_MERGED = "区册并走";
    public static final String ACTION_VOID = "作废";

    /** 立件四样输入的名（点名缺的是哪一样，按这次序回话） */
    public static final String INPUT_POINT = "点位码";
    public static final String INPUT_ZONE = "所属区册代号";
    public static final String INPUT_SPOT = "处数";
    public static final String INPUT_READING = "核查读数";

    private CaseFlowRules() {
    }

    /** 档数合法：只许 0..3 这四档，第四种写法不认 */
    public static boolean validStage(Integer stage) {
        return stage != null
                && stage.intValue() >= STAGE_INTAKE
                && stage.intValue() <= LAST_STAGE;
    }

    /** 关名回话；档数不认得回「档数不明」，不替它猜 */
    public static String stageName(Integer stage) {
        if (validStage(stage)) {
            return STAGE_NAMES.get(stage.intValue());
        }
        return "档数不明";
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 立件四样输入点名：点位码、所属区册代号、处数、准备折算的那一个核查读数。
     * 少一样就起不起来；回话按固定次序点出缺的每一样，光甩「信息不全」不认。
     * 返回缺样名列表，空表表示四样齐。
     */
    public static List<String> missingInputs(String pointNo, String zoneNo,
                                             Integer spotCount, BigDecimal checkReading) {
        List<String> missing = new ArrayList<String>();
        if (blank(pointNo)) {
            missing.add(INPUT_POINT);
        }
        if (blank(zoneNo)) {
            missing.add(INPUT_ZONE);
        }
        if (spotCount == null) {
            missing.add(INPUT_SPOT);
        }
        if (checkReading == null) {
            missing.add(INPUT_READING);
        }
        return missing;
    }

    /**
     * 往前只许跨紧邻的下一关（+1）；越过中间关直送再下一关不生效，整条留原地。
     */
    public static boolean isAdjacentForward(int from, int to) {
        return to == from + 1 && from >= STAGE_INTAKE && from < LAST_STAGE;
    }

    /**
     * 往后只许收回一关（-1）；已经站在第一关（受件）的，没什么可再收。
     */
    public static boolean isAdjacentBack(int from, int to) {
        return to == from - 1 && from > STAGE_INTAKE && from <= LAST_STAGE;
    }

    /** 点位在底册上还挂着名（撤了名的进不去核查） */
    public static boolean pointListed(Integer pointDelFlag) {
        return pointDelFlag != null && pointDelFlag.intValue() == 0;
    }

    /** 责令关核收记成「可」，复查收尾才许开工；重入责令关从头累计 */
    public static boolean orderAccepted(Integer orderReceived) {
        return orderReceived != null && orderReceived.intValue() == 1;
    }

    /**
     * 两名见证岗各落一笔：两笔都得在，且须是两个岗（同一个人落两笔不算两名）。
     */
    public static boolean witnessesComplete(String witnessA, String witnessB) {
        return !blank(witnessA) && !blank(witnessB) && !witnessA.trim().equals(witnessB.trim());
    }

    /** 复测凭证那一串写回本行才作数（空串当没写） */
    public static boolean voucherWritten(String voucher) {
        return !blank(voucher);
    }

    /**
     * 复查收尾封门的齐套：两名见证岗各落一笔 ＋ 复测凭证写回本行，两样齐了才算封上。
     * 责令核收是「进」复查收尾的门槛，进门之后不重复拦。
     */
    public static boolean sealReady(String witnessA, String witnessB, String voucher) {
        return witnessesComplete(witnessA, witnessB) && voucherWritten(voucher);
    }

    /**
     * 推进到目标关的准入（只回答「这一步迈得动迈不动」，不替它预设结果）：
     * 进核查：点位在底册还挂着名（撤名进不去）；
     * 进责令：只看邻档，无另设门槛；
     * 进复查收尾：责令关核收须已记成「可」。
     * 邻档关系由调用方先用 {@link #isAdjacentForward} 数，这里不看。
     */
    public static boolean canEnter(int targetStage, boolean pointListed, boolean orderAccepted) {
        if (targetStage == STAGE_CHECK) {
            return pointListed;
        }
        if (targetStage == STAGE_ORDER) {
            return true;
        }
        if (targetStage == STAGE_REVIEW) {
            return orderAccepted;
        }
        return false;
    }

    /**
     * 本关按语只留头一句：同一关第二次塞话，第二句盖不上去、也不留副本。
     * 本行已留有非空头句即回 false（不收）；头一句空才收。
     */
    public static boolean acceptsStageNote(String existingFirstNote, String incoming) {
        return !blank(existingFirstNote) ? false : !blank(incoming);
    }

    /**
     * 受理簿逐笔回放（只许 ±1 邻档，从头关起捋）。
     *
     * 一笔一笔点：非邻档的笔一一点名，不并笔、不吞笔；捋完的末档必须正正
     * 落在本件当前档上。只截其中任意一关单独核时，取到达轨迹里该关的入档笔
     * 数（{@link Replay#getArrivals(int)}），照样自洽，不另算一套去凑。
     */
    public static Replay replay(Integer currentStage, List<int[]> moves) {
        Replay r = new Replay();
        if (!validStage(currentStage)) {
            r.consistent = false;
            return r;
        }
        int at = STAGE_INTAKE;
        r.path.add(Integer.valueOf(at));
        int idx = 0;
        for (int[] m : moves) {
            idx++;
            if (m == null || (!isAdjacentForward(m[0], m[1]) && !isAdjacentBack(m[0], m[1]))) {
                // 笔上记的来路跟脚下对不上，或想跨关：这一笔点出来，不许拿它往下配
                r.offenders.add(Integer.valueOf(idx));
                continue;
            }
            if (m[0] != at) {
                // 不是从脚下这一关挪的：时序断了，点名
                r.offenders.add(Integer.valueOf(idx));
                continue;
            }
            at = m[1];
            r.path.add(Integer.valueOf(at));
        }
        r.finalStage = at;
        r.expectedStage = currentStage.intValue();
        r.consistent = r.offenders.isEmpty() && at == currentStage.intValue();
        return r;
    }

    /** 回放结果：末档、该在的档、到关轨迹、点了名的坏笔（受理簿上的第几笔，1 起） */
    public static final class Replay {
        private boolean consistent;
        private int finalStage = STAGE_INTAKE;
        private int expectedStage = STAGE_INTAKE;
        private final List<Integer> path = new ArrayList<Integer>();
        private final List<Integer> offenders = new ArrayList<Integer>();

        public boolean isConsistent() {
            return consistent;
        }

        public int getFinalStage() {
            return finalStage;
        }

        public int getExpectedStage() {
            return expectedStage;
        }

        /** 到关轨迹：含起点受件，随后每笔落一个档 */
        public List<Integer> getPath() {
            return path;
        }

        /** 受理簿上点了名的坏笔序号（1 起） */
        public List<Integer> getOffenders() {
            return offenders;
        }

        /** 某一关的入档笔数（截这一关单独核用；等于轨迹里该档出现次数减起步那一下） */
        public int getArrivals(int stage) {
            int n = 0;
            for (int i = 1; i < path.size(); i++) {
                if (path.get(i).intValue() == stage) {
                    n++;
                }
            }
            return n;
        }
    }
}

package com.fc.v2.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 办件流转纯口径测试：四样点名、邻档、门槛、封门齐套、同关第二句、受理簿回放。
 */
public class CaseFlowRulesTest {

    @Test
    public void fourGatesFixedOrder() {
        assertEquals(Arrays.asList("受件", "核查", "责令", "复查收尾"), CaseFlowRules.STAGE_NAMES);
        assertEquals(3, CaseFlowRules.LAST_STAGE);
        assertTrue(CaseFlowRules.validStage(0));
        assertTrue(CaseFlowRules.validStage(3));
        assertFalse(CaseFlowRules.validStage(4));
        assertEquals("核查", CaseFlowRules.stageName(1));
        assertEquals("档数不明", CaseFlowRules.stageName(9));
    }

    @Test
    public void missingInputsNamedOneByOne() {
        // 四样齐
        assertTrue(CaseFlowRules.missingInputs("P1", "QN00", 1, new BigDecimal("55.0")).isEmpty());
        // 少一样点一样的名，光甩「信息不全」不认
        List<String> noPoint = CaseFlowRules.missingInputs(" ", "QN00", 1, new BigDecimal("55"));
        assertEquals(Arrays.asList(CaseFlowRules.INPUT_POINT), noPoint);
        List<String> noZone = CaseFlowRules.missingInputs("P1", "", 1, new BigDecimal("55"));
        assertEquals(Arrays.asList(CaseFlowRules.INPUT_ZONE), noZone);
        List<String> noSpot = CaseFlowRules.missingInputs("P1", "QN00", null, new BigDecimal("55"));
        assertEquals(Arrays.asList(CaseFlowRules.INPUT_SPOT), noSpot);
        List<String> noReading = CaseFlowRules.missingInputs("P1", "QN00", 1, null);
        assertEquals(Arrays.asList(CaseFlowRules.INPUT_READING), noReading);
        // 一次少两样：按固定次序都点出来
        List<String> two = CaseFlowRules.missingInputs(null, null, 2, new BigDecimal("55"));
        assertEquals(Arrays.asList(CaseFlowRules.INPUT_POINT, CaseFlowRules.INPUT_ZONE), two);
    }

    @Test
    public void onlyAdjacentMoves() {
        assertTrue(CaseFlowRules.isAdjacentForward(0, 1));
        assertTrue(CaseFlowRules.isAdjacentForward(2, 3));
        // 越过中间关直送再下一关：不生效
        assertFalse(CaseFlowRules.isAdjacentForward(0, 2));
        assertFalse(CaseFlowRules.isAdjacentForward(3, 4));
        assertTrue(CaseFlowRules.isAdjacentBack(3, 2));
        assertTrue(CaseFlowRules.isAdjacentBack(1, 0));
        // 站在第一关没什么可再收
        assertFalse(CaseFlowRules.isAdjacentBack(0, -1));
        assertFalse(CaseFlowRules.isAdjacentBack(2, 0));
    }

    @Test
    public void gatesBeforeStages() {
        // 进核查：底册撤了名的进不去
        assertFalse(CaseFlowRules.canEnter(CaseFlowRules.STAGE_CHECK, false, false));
        assertTrue(CaseFlowRules.canEnter(CaseFlowRules.STAGE_CHECK, true, false));
        // 进责令：无另设门槛
        assertTrue(CaseFlowRules.canEnter(CaseFlowRules.STAGE_ORDER, false, false));
        // 进复查收尾：责令核收没记成「可」开不了工
        assertFalse(CaseFlowRules.canEnter(CaseFlowRules.STAGE_REVIEW, true, false));
        assertTrue(CaseFlowRules.canEnter(CaseFlowRules.STAGE_REVIEW, true, true));
        assertFalse(CaseFlowRules.orderAccepted(0));
        assertTrue(CaseFlowRules.orderAccepted(1));
    }

    @Test
    public void sealNeedsTwoWitnessesAndVoucher() {
        assertFalse(CaseFlowRules.witnessesComplete(null, "乙"));
        assertFalse(CaseFlowRules.witnessesComplete("甲", "甲"));
        assertTrue(CaseFlowRules.witnessesComplete("甲", "乙"));
        assertFalse(CaseFlowRules.voucherWritten(" "));
        assertFalse(CaseFlowRules.sealReady("甲", "乙", null));
        assertFalse(CaseFlowRules.sealReady("甲", "甲", "V001"));
        assertTrue(CaseFlowRules.sealReady("甲", "乙", "V001"));
    }

    @Test
    public void secondNoteOnSameStageRejected() {
        // 头一句空：收
        assertTrue(CaseFlowRules.acceptsStageNote(null, "头一句"));
        assertTrue(CaseFlowRules.acceptsStageNote(" ", "头一句"));
        // 已有头一句：第二句盖不上、也不留副本；空句也不收
        assertFalse(CaseFlowRules.acceptsStageNote("头一句", "第二句"));
        assertFalse(CaseFlowRules.acceptsStageNote(null, " "));
    }

    @Test
    public void replayStraightRunMatchesCurrentStage() {
        List<int[]> moves = Arrays.asList(
                new int[]{0, 1}, new int[]{1, 2}, new int[]{2, 3});
        CaseFlowRules.Replay r = CaseFlowRules.replay(3, moves);
        assertTrue(r.isConsistent());
        assertEquals(3, r.getFinalStage());
        assertEquals(0, r.getOffenders().size());
        // 截单关单独核：每关入档笔数自洽
        assertEquals(1, r.getArrivals(1));
        assertEquals(1, r.getArrivals(2));
        assertEquals(1, r.getArrivals(3));
        assertEquals(0, r.getArrivals(0));
    }

    @Test
    public void replayWithRollbacksAndReEntry() {
        // 0→1→2→3，收回两关到 1，再 1→2→3：末档仍落在 3，各关笔数对得上
        List<int[]> moves = Arrays.asList(
                new int[]{0, 1}, new int[]{1, 2}, new int[]{2, 3},
                new int[]{3, 2}, new int[]{2, 1},
                new int[]{1, 2}, new int[]{2, 3});
        CaseFlowRules.Replay r = CaseFlowRules.replay(3, moves);
        assertTrue(r.isConsistent());
        assertEquals(2, r.getArrivals(1));
        assertEquals(3, r.getArrivals(2));
        assertEquals(2, r.getArrivals(3));
    }

    @Test
    public void replayNamesJumpAndBrokenSequence() {
        // 第二笔想从 1 跨到 3：点名第 2 笔，整条不拿它往下配
        CaseFlowRules.Replay jump = CaseFlowRules.replay(2, Arrays.asList(
                new int[]{0, 1}, new int[]{1, 3}, new int[]{1, 2}));
        assertFalse(jump.isConsistent());
        assertEquals(Arrays.asList(2), jump.getOffenders());
        // 时序断了：第三笔不是从脚下这关挪的
        CaseFlowRules.Replay broken = CaseFlowRules.replay(3, Arrays.asList(
                new int[]{0, 1}, new int[]{1, 2}, new int[]{1, 3}));
        assertFalse(broken.isConsistent());
        assertEquals(Arrays.asList(3), broken.getOffenders());
        // 捋出来的末档与当前档对不上：不自洽（不另算一套去凑）
        CaseFlowRules.Replay mismatch = CaseFlowRules.replay(2, Arrays.asList(
                new int[]{0, 1}, new int[]{1, 2}, new int[]{2, 3}));
        assertFalse(mismatch.isConsistent());
        assertEquals(3, mismatch.getFinalStage());
        assertEquals(2, mismatch.getExpectedStage());
    }
}

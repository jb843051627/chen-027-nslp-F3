package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TNslpCaseIntakeMapper;
import com.fc.v2.mapper.auto.TNslpCaseLedgerMapper;
import com.fc.v2.mapper.auto.TNslpCaseFlowMapper;
import com.fc.v2.mapper.auto.TNslpCaseNoteMapper;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.mapper.auto.TNslpZoneBookMapper;
import com.fc.v2.model.auto.TNslpCaseFlow;
import com.fc.v2.model.auto.TNslpCaseIntake;
import com.fc.v2.model.auto.TNslpCaseLedger;
import com.fc.v2.model.auto.TNslpCaseNote;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.auto.TNslpZoneBook;
import com.fc.v2.model.custom.CaseIntakeSubmit;
import com.fc.v2.model.custom.CaseReconcile;
import com.fc.v2.model.custom.CaseTrace;
import com.fc.v2.service.support.CaseFlowRules;

/**
 * 办件流转服务内存账：立（四样点名/报文关次不算数/一点一位）、挪（邻档/门槛/
 * 收回带按语）、封（两名见证+凭证/封后三不接）、捋（追溯串/受理簿对账）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TNslpCaseFlowServiceImplTest {

    @Mock
    private TNslpCaseFlowMapper caseMapper;
    @Mock
    private TNslpCaseIntakeMapper intakeMapper;
    @Mock
    private TNslpCaseNoteMapper noteMapper;
    @Mock
    private TNslpCaseLedgerMapper ledgerMapper;
    @Mock
    private TNslpPointMapper pointMapper;
    @Mock
    private TNslpZoneBookMapper zoneBookMapper;
    @InjectMocks
    private TNslpCaseFlowServiceImpl service;

    private List<TNslpCaseFlow> caseStore;
    private List<TNslpCaseIntake> intakeStore;
    private List<TNslpCaseNote> noteStore;
    private List<TNslpCaseLedger> ledgerStore;
    private List<TNslpPoint> pointStore;
    private List<TNslpZoneBook> zoneStore;
    private long idSeq;

    private static final Pattern EQ = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}");

    private static boolean eqVal(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.equals(b) || String.valueOf(a).equals(String.valueOf(b));
    }

    private static Map<String, Object> eqPairs(QueryWrapper<?> w) {
        w.getSqlSegment();
        Map<String, Object> out = new HashMap<String, Object>();
        Matcher m = EQ.matcher(w.getSqlSegment() == null ? "" : w.getSqlSegment());
        while (m.find()) {
            out.put(m.group(1), w.getParamNameValuePairs().get(m.group(2)));
        }
        return out;
    }

    @BeforeEach
    public void setUp() {
        caseStore = new ArrayList<TNslpCaseFlow>();
        intakeStore = new ArrayList<TNslpCaseIntake>();
        noteStore = new ArrayList<TNslpCaseNote>();
        ledgerStore = new ArrayList<TNslpCaseLedger>();
        pointStore = new ArrayList<TNslpPoint>();
        zoneStore = new ArrayList<TNslpZoneBook>();
        idSeq = 0;

        TNslpPoint p = new TNslpPoint();
        p.setId(1L);
        p.setPointNo("PT0001");
        p.setZoneNo("QN00");
        p.setDelFlag(0);
        pointStore.add(p);
        TNslpPoint gone = new TNslpPoint();
        gone.setId(2L);
        gone.setPointNo("PT0002");
        gone.setZoneNo("QN00");
        gone.setDelFlag(1);
        pointStore.add(gone);

        TNslpZoneBook active = new TNslpZoneBook();
        active.setId(0L);
        active.setZoneNo("QN00");
        active.setStatus(0);
        active.setDelFlag(0);
        zoneStore.add(active);
        TNslpZoneBook merged = new TNslpZoneBook();
        merged.setId(1L);
        merged.setZoneNo("QN01");
        merged.setStatus(1);
        merged.setDelFlag(0);
        zoneStore.add(merged);

        when(intakeMapper.insert(any(TNslpCaseIntake.class))).thenAnswer(inv -> {
            TNslpCaseIntake d = inv.getArgument(0);
            d.setId(Long.valueOf(++idSeq));
            intakeStore.add(d);
            return 1;
        });
        when(intakeMapper.selectById(any())).thenAnswer(inv ->
                intakeStore.stream().filter(d -> d.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(intakeMapper.updateById(any(TNslpCaseIntake.class))).thenAnswer(inv -> 1);

        when(caseMapper.insert(any(TNslpCaseFlow.class))).thenAnswer(inv -> {
            TNslpCaseFlow c = inv.getArgument(0);
            c.setId(Long.valueOf(++idSeq));
            caseStore.add(c);
            return 1;
        });
        when(caseMapper.selectById(any())).thenAnswer(inv ->
                caseStore.stream().filter(c -> c.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(caseMapper.updateById(any(TNslpCaseFlow.class))).thenAnswer(inv -> 1);
        when(caseMapper.selectList(any())).thenAnswer(inv -> {
            Map<String, Object> kv = eqPairs(inv.getArgument(0));
            List<TNslpCaseFlow> out = new ArrayList<TNslpCaseFlow>();
            for (TNslpCaseFlow c : caseStore) {
                boolean ok = true;
                if (kv.containsKey("point_no")) {
                    ok = eqVal(c.getPointNo(), kv.get("point_no"));
                }
                if (ok && kv.containsKey("status")) {
                    ok = eqVal(c.getStatus(), kv.get("status"));
                }
                if (ok) {
                    out.add(c);
                }
            }
            return out;
        });
        when(caseMapper.selectCount(any())).thenAnswer(inv -> {
            Map<String, Object> kv = eqPairs(inv.getArgument(0));
            int n = 0;
            for (TNslpCaseFlow c : caseStore) {
                if (!kv.containsKey("status") || eqVal(c.getStatus(), kv.get("status"))) {
                    n++;
                }
            }
            return Integer.valueOf(n);
        });

        when(noteMapper.insert(any(TNslpCaseNote.class))).thenAnswer(inv -> {
            TNslpCaseNote n = inv.getArgument(0);
            n.setId(Long.valueOf(++idSeq));
            noteStore.add(n);
            return 1;
        });
        when(noteMapper.selectList(any())).thenAnswer(inv -> {
            Map<String, Object> kv = eqPairs(inv.getArgument(0));
            List<TNslpCaseNote> out = new ArrayList<TNslpCaseNote>();
            for (TNslpCaseNote n : noteStore) {
                if (!kv.containsKey("case_id") || eqVal(n.getCaseId(), kv.get("case_id"))) {
                    out.add(n);
                }
            }
            // 按 note_time、id 升序
            out.sort((a, b) -> {
                if (a.getNoteTime() == null || b.getNoteTime() == null) {
                    return a.getId().compareTo(b.getId());
                }
                int c = a.getNoteTime().compareTo(b.getNoteTime());
                return c != 0 ? c : a.getId().compareTo(b.getId());
            });
            return out;
        });

        when(ledgerMapper.insert(any(TNslpCaseLedger.class))).thenAnswer(inv -> {
            TNslpCaseLedger l = inv.getArgument(0);
            l.setId(Long.valueOf(++idSeq));
            ledgerStore.add(l);
            return 1;
        });
        when(ledgerMapper.selectList(any())).thenAnswer(inv -> {
            Map<String, Object> kv = eqPairs(inv.getArgument(0));
            List<TNslpCaseLedger> out = new ArrayList<TNslpCaseLedger>();
            for (TNslpCaseLedger l : ledgerStore) {
                if (!kv.containsKey("case_id") || eqVal(l.getCaseId(), kv.get("case_id"))) {
                    out.add(l);
                }
            }
            out.sort((a, b) -> {
                if (a.getMoveTime() == null || b.getMoveTime() == null) {
                    return a.getId().compareTo(b.getId());
                }
                int c = a.getMoveTime().compareTo(b.getMoveTime());
                return c != 0 ? c : a.getId().compareTo(b.getId());
            });
            return out;
        });

        when(pointMapper.selectOne(any())).thenAnswer(inv -> {
            Map<String, Object> kv = eqPairs(inv.getArgument(0));
            for (TNslpPoint pnt : pointStore) {
                if ((!kv.containsKey("point_no") || eqVal(pnt.getPointNo(), kv.get("point_no")))
                        && (!kv.containsKey("del_flag") || eqVal(pnt.getDelFlag(), kv.get("del_flag")))) {
                    return pnt;
                }
            }
            return null;
        });
        when(zoneBookMapper.selectOne(any())).thenAnswer(inv -> {
            Map<String, Object> kv = eqPairs(inv.getArgument(0));
            for (TNslpZoneBook z : zoneStore) {
                if ((!kv.containsKey("zone_no") || eqVal(z.getZoneNo(), kv.get("zone_no")))
                        && (!kv.containsKey("del_flag") || eqVal(z.getDelFlag(), kv.get("del_flag")))) {
                    return z;
                }
            }
            return null;
        });
    }

    // ------------------------------------------------------------ 助件

    private TNslpCaseIntake draft(String point, String zone, Integer spot, String reading,
                                  Integer refStage, String content) {
        CaseIntakeSubmit s = new CaseIntakeSubmit();
        s.setPointNo(point);
        s.setZoneNo(zone);
        s.setSpotCount(spot);
        s.setCheckReading(reading == null ? null : new BigDecimal(reading));
        s.setRefStage(refStage);
        s.setContent(content);
        return service.intake(s);
    }

    private TNslpCaseFlow establish(String point) {
        TNslpCaseIntake d = draft(point, "QN00", 1, "55.0", null, null);
        return service.establish(d.getId());
    }

    private TNslpCaseFlow reload(Long id) {
        return caseStore.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private void advanceTo(Long id, int target) {
        TNslpCaseFlow c = reload(id);
        while (c.getStage().intValue() < target) {
            if (c.getStage().intValue() == CaseFlowRules.STAGE_ORDER) {
                service.markOrderReceived(id);
            }
            service.advance(id);
            c = reload(id);
        }
    }

    private List<TNslpCaseNote> notesOf(Long caseId) {
        List<TNslpCaseNote> all = new ArrayList<TNslpCaseNote>();
        for (TNslpCaseNote n : noteStore) {
            if (n.getCaseId().equals(caseId)) {
                all.add(n);
            }
        }
        return all;
    }

    // ------------------------------------------------------------ 立

    @Test
    public void establishNeedsFourInputsAndNamesEach() {
        // 少点位码
        TNslpCaseIntake d1 = draft(" ", "QN00", 1, "55", null, null);
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> service.establish(d1.getId()));
        assertTrue(e1.getMessage().contains("点位码"), e1.getMessage());
        // 少处数与核查读数两样，两样都点名，光甩「信息不全」不认
        TNslpCaseIntake d2 = draft("PT0001", "QN00", null, null, null, null);
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> service.establish(d2.getId()));
        assertTrue(e2.getMessage().contains("处数"));
        assertTrue(e2.getMessage().contains("核查读数"));
        // 没立成：主档一条没有，草稿仍待立
        assertEquals(0, caseStore.size());
        assertEquals(CaseFlowRules.INTAKE_PENDING,
                intakeStore.stream().filter(d -> d.getId().equals(d2.getId())).findFirst().get().getStatus().intValue());

        // 四样齐：立在受件，没封住数 +1
        TNslpCaseFlow c = establish("PT0001");
        assertEquals(Integer.valueOf(CaseFlowRules.STAGE_INTAKE), c.getStage());
        assertEquals(Integer.valueOf(CaseFlowRules.STATUS_OPEN), c.getStatus());
        assertEquals(1, service.openCount());
    }

    @Test
    public void reportedStageIsOnlyReference() {
        // 报文自带关次说已经到复查收尾（3）：立件、读档都只认后端数出来的 0
        TNslpCaseIntake d = draft("PT0001", "QN00", 1, "55", 3, null);
        assertEquals(Integer.valueOf(3), d.getRefStage());
        TNslpCaseFlow c = service.establish(d.getId());
        assertEquals(Integer.valueOf(0), c.getStage());
        // 不会跟着报文序号多挪两关，也不会一步不挪——得自己一关一关挪
        service.advance(c.getId());
        assertEquals(Integer.valueOf(1), reload(c.getId()).getStage());
    }

    @Test
    public void oneOpenCasePerPoint() {
        TNslpCaseFlow first = establish("PT0001");
        // 前一条还在中间关（在办的）：第二条立不起来
        service.advance(first.getId());
        TNslpCaseIntake d2 = draft("PT0001", "QN00", 1, "55", null, null);
        assertThrows(IllegalStateException.class, () -> service.establish(d2.getId()));

        // 前一条封住之后，同点位可以再立一条
        advanceTo(first.getId(), 3);
        service.witness(first.getId(), 0, "甲");
        service.witness(first.getId(), 1, "乙");
        service.writeVoucher(first.getId(), "V-001");
        service.seal(first.getId());
        assertEquals(0, service.openCount());
        TNslpCaseFlow second = establish("PT0001");
        assertNotNull(second);
        assertEquals(1, service.openCount());
    }

    // ------------------------------------------------------------ 挪

    @Test
    public void advanceOnlyAdjacentAndGates() {
        TNslpCaseFlow c = establish("PT0001");

        // 撤了名的点位进不去核查（PT0002 在底册 del_flag=1）
        TNslpCaseIntake d = draft("PT0002", "QN00", 1, "55", null, null);
        TNslpCaseFlow gone = service.establish(d.getId());
        assertThrows(IllegalStateException.class, () -> service.advance(gone.getId()));
        assertEquals(Integer.valueOf(0), reload(gone.getId()).getStage());

        // 在册点位一关一关走
        service.advance(c.getId());
        assertEquals(Integer.valueOf(1), reload(c.getId()).getStage());
        service.advance(c.getId());
        assertEquals(Integer.valueOf(2), reload(c.getId()).getStage());

        // 责令核收没记成「可」：复查收尾开不了工，整条留在责令
        assertThrows(IllegalStateException.class, () -> service.advance(c.getId()));
        assertEquals(Integer.valueOf(2), reload(c.getId()).getStage());
        service.markOrderReceived(c.getId());
        service.advance(c.getId());
        assertEquals(Integer.valueOf(3), reload(c.getId()).getStage());

        // 末关前面没有再下一关
        assertThrows(IllegalStateException.class, () -> service.advance(c.getId()));
    }

    @Test
    public void rollbackOneStageCarriesNoteAndResetsOrder() {
        TNslpCaseFlow c = establish("PT0001");
        service.advance(c.getId()); // 到核查
        service.advance(c.getId()); // 到责令
        service.note(c.getId(), "责令头一句");
        service.markOrderReceived(c.getId());
        assertEquals(Integer.valueOf(1), reload(c.getId()).getOrderReceived());

        // 收回一关：头句按语照原时刻转进追溯串；停在核查关时责令核收不作数
        service.rollback(c.getId());
        TNslpCaseFlow back = reload(c.getId());
        assertEquals(Integer.valueOf(1), back.getStage());
        assertNull(back.getContent());
        List<TNslpCaseNote> archived = notesOf(c.getId());
        assertEquals(1, archived.size());
        assertEquals("责令头一句", archived.get(0).getNote());
        assertEquals(Integer.valueOf(CaseFlowRules.STAGE_ORDER), archived.get(0).getStage());

        // 重入责令关那一刻核收从头计：先推进到责令，再试进复查收尾，没重新记「可」进不去
        service.advance(c.getId());
        assertEquals(Integer.valueOf(2), reload(c.getId()).getStage());
        assertEquals(Integer.valueOf(0), reload(c.getId()).getOrderReceived());
        assertThrows(IllegalStateException.class, () -> service.advance(c.getId()));
        service.markOrderReceived(c.getId());
        service.advance(c.getId());
        assertEquals(Integer.valueOf(3), reload(c.getId()).getStage());

        // 站在第一关没什么可再收：撤了名的点位立得住，只是进不去核查；停在受件关收不回
        TNslpCaseIntake d0 = draft("PT0002", "QN00", 1, "55", null, null);
        TNslpCaseFlow firstGate = service.establish(d0.getId());
        assertEquals(Integer.valueOf(0), firstGate.getStage());
        assertThrows(IllegalStateException.class, () -> service.rollback(firstGate.getId()));
    }

    @Test
    public void secondNoteOnSameStageDropped() {
        TNslpCaseFlow c = establish("PT0001");
        assertTrue(service.note(c.getId(), "受件头一句"));
        // 同一刻两条通道各送一句进来：头一句在本关，第二句盖不上也不留副本
        assertFalse(service.note(c.getId(), "第二句"));
        assertEquals("受件头一句", reload(c.getId()).getContent());
        // 追溯串此刻只在本关栏（未离关不转串）
        CaseTrace beforeMove = service.trace(c.getId());
        assertEquals(1, beforeMove.size());
        assertEquals("受件头一句", beforeMove.getEntries().get(0).getNote());

        service.advance(c.getId());
        // 离开受件后，头句转进身后那一串，本关栏清空，第二句始终没落下
        assertNull(reload(c.getId()).getContent());
        List<TNslpCaseNote> archived = notesOf(c.getId());
        assertEquals(1, archived.size());
        // 到核查关再落头一句，新关从头累计
        assertTrue(service.note(c.getId(), "核查头一句"));
        assertFalse(service.note(c.getId(), "核查又一句"));
        CaseTrace t = service.trace(c.getId());
        assertEquals(2, t.size());
        assertEquals("受件头一句", t.getEntries().get(0).getNote());
        assertEquals("核查头一句", t.getEntries().get(1).getNote());
    }

    // ------------------------------------------------------------ 封

    @Test
    public void sealRequiresTwoWitnessesAndVoucherThenLocks() {
        TNslpCaseFlow c = establish("PT0001");
        advanceTo(c.getId(), 3);

        // 两样没齐封不上
        service.witness(c.getId(), 0, "甲");
        assertThrows(IllegalStateException.class, () -> service.seal(c.getId()));
        // 同一个人落两笔不算两名
        assertThrows(IllegalStateException.class, () -> service.witness(c.getId(), 1, "甲"));
        service.witness(c.getId(), 1, "乙");
        assertThrows(IllegalStateException.class, () -> service.seal(c.getId()));
        // 同一侧不接第二笔、凭证写过盖不上
        assertThrows(IllegalStateException.class, () -> service.witness(c.getId(), 0, "甲二"));
        service.writeVoucher(c.getId(), "V-77");
        assertThrows(IllegalStateException.class, () -> service.writeVoucher(c.getId(), "V-88"));

        TNslpCaseFlow sealed = service.seal(c.getId());
        assertEquals(Integer.valueOf(CaseFlowRules.STATUS_CLOSED), sealed.getStatus());
        assertEquals(Integer.valueOf(CaseFlowRules.CLOSE_SEAL), sealed.getCloseKind());
        assertEquals(0, service.openCount());

        // 封住后三种动作一概不接：添按语、更正内容、整条抽回；撤并作废也不接
        assertThrows(IllegalStateException.class, () -> service.note(c.getId(), "封后再说一句"));
        assertFalse(service.updateContent(c.getId(), "改"));
        assertFalse(service.remove(c.getId()));
        assertThrows(IllegalStateException.class, () -> service.advance(c.getId()));
        assertThrows(IllegalStateException.class, () -> service.withdraw(c.getId()));
        assertThrows(IllegalStateException.class, () -> service.voidCase(c.getId()));
        // 名册里照常露脸，追溯串照常取得到，封口事件压在串末
        assertEquals(1, service.selectTNslpCaseFlowList(new QueryWrapper<TNslpCaseFlow>()).size());
        CaseTrace t = service.trace(c.getId());
        assertEquals(Integer.valueOf(CaseFlowRules.LOG_SEAL),
                t.getEntries().get(t.size() - 1).getLogKind());
    }

    @Test
    public void withdrawMergedAndVoidAreSeparateWays() {
        // 撤回：受件之后任何一关都可收口，不再往前挪
        TNslpCaseFlow a = establish("PT0001");
        service.advance(a.getId());
        service.withdraw(a.getId());
        assertEquals(Integer.valueOf(CaseFlowRules.CLOSE_WITHDRAW), reload(a.getId()).getCloseKind());
        assertEquals(Integer.valueOf(CaseFlowRules.STATUS_CLOSED), reload(a.getId()).getStatus());

        // 区册并走：在册区册不能按并走收口
        TNslpCaseFlow b = establish("PT0001");
        assertThrows(IllegalStateException.class, () -> service.closeByZoneMerged(b.getId()));
        // 挂在已并走区册的件可收口（手工改挂，模拟底册先并走后件还开着）
        TNslpCaseFlow bRow = reload(b.getId());
        bRow.setZoneNo("QN01");
        service.closeByZoneMerged(b.getId());
        assertEquals(Integer.valueOf(CaseFlowRules.CLOSE_ZONE_MERGED), reload(b.getId()).getCloseKind());

        // 作废只从最后一关发起
        TNslpCaseFlow cc = establish("PT0001");
        assertThrows(IllegalStateException.class, () -> service.voidCase(cc.getId()));
        advanceTo(cc.getId(), 3);
        service.voidCase(cc.getId());
        assertEquals(Integer.valueOf(CaseFlowRules.CLOSE_VOID), reload(cc.getId()).getCloseKind());
    }

    @Test
    public void cancelOnlyBeforeEstablishAndHoldsNoSlot() {
        TNslpCaseIntake d = draft("PT0001", "QN00", 1, "55", null, null);
        service.cancelIntake(d.getId());
        // 撤掉的不进主档、不占没封住名额
        assertEquals(0, caseStore.size());
        assertEquals(0, service.openCount());
        // 撤过的立不起来，也不撤第二回
        assertThrows(IllegalStateException.class, () -> service.establish(d.getId()));
        assertThrows(IllegalStateException.class, () -> service.cancelIntake(d.getId()));

        // 已立的件撤不回
        TNslpCaseFlow c = establish("PT0001");
        TNslpCaseIntake d2 = intakeStore.get(intakeStore.size() - 1);
        assertThrows(IllegalStateException.class, () -> service.cancelIntake(d2.getId()));
        assertNotNull(c);
    }

    // ------------------------------------------------------------ 捋

    @Test
    public void traceAndLedgerReconcile() {
        TNslpCaseFlow c = establish("PT0001");
        service.note(c.getId(), "受件句");
        service.advance(c.getId());            // 1
        service.note(c.getId(), "核查句");
        service.advance(c.getId());            // 2
        service.markOrderReceived(c.getId());
        service.advance(c.getId());            // 3
        // 收回两关，再从头捋到末关
        service.rollback(c.getId());           // 3→2
        service.rollback(c.getId());           // 2→1
        advanceTo(c.getId(), 3);               // 1→2→3

        // 受理簿笔数：头三进、两收、再两进，共 7 笔；捋出来的末档正正落在 3
        CaseReconcile r = service.reconcile(c.getId());
        assertTrue(r.isConsistent());
        assertEquals(7, r.getLedgerCount());
        assertEquals(Integer.valueOf(3), r.getReplayedStage());
        assertEquals(Integer.valueOf(3), r.getCurrentStage());
        // 截单关单独核：核查入档 2 回、责令 3 回（两进+收回时踩一脚）、复查收尾 2 回
        assertEquals(Integer.valueOf(2), r.getArrivals().get(1));
        assertEquals(Integer.valueOf(3), r.getArrivals().get(2));
        assertEquals(Integer.valueOf(2), r.getArrivals().get(3));

        // 追溯串：受件句、核查句（各带着原时刻），按原先后压着
        CaseTrace t = service.trace(c.getId());
        List<String> texts = new ArrayList<String>();
        for (int i = 0; i < t.size(); i++) {
            if (t.getEntries().get(i).getLogKind().intValue() == CaseFlowRules.LOG_NOTE) {
                texts.add(t.getEntries().get(i).getNote());
            }
        }
        assertTrue(texts.contains("受件句"));
        assertTrue(texts.contains("核查句"));
        assertEquals("受件句", texts.get(0));
    }

    @Test
    public void moveAndTraceAreOneTransactionShape() {
        // 推进与受理簿落笔成对出现：挪三关必有三笔，断一笔都对不上
        TNslpCaseFlow c = establish("PT0001");
        service.advance(c.getId());
        service.advance(c.getId());
        service.markOrderReceived(c.getId());
        service.advance(c.getId());
        assertEquals(3, ledgerStore.size());
        for (TNslpCaseLedger l : ledgerStore) {
            assertEquals(1, Math.abs(l.getToStage().intValue() - l.getFromStage().intValue()));
            assertNotNull(l.getMoveTime());
        }
        // 封门后受理簿不再添笔，封口只进按语串
        service.witness(c.getId(), 0, "甲");
        service.witness(c.getId(), 1, "乙");
        service.writeVoucher(c.getId(), "V-1");
        service.seal(c.getId());
        assertEquals(3, ledgerStore.size());
    }
}

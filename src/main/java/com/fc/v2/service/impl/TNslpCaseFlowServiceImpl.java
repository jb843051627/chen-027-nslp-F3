package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.fc.v2.service.ITNslpCaseFlowService;
import com.fc.v2.service.support.CaseFlowRules;

/**
 * 噪声投诉与超标处置办件流转 Service业务层处理（state-machine 形状：单据流转）。
 *
 * 只讲四件事：
 * 立——四样输入点名校验、报文关次不作数、一处点位只许一条没封住的件；
 * 挪——前后各只许邻档，跨关不生效，收回关的头句按语照原时刻转进追溯串；
 * 封——末关两名见证各一笔＋复测凭证写回才封，封后只能看不能动；
 * 捋——每关按语连同时刻压在本件身后，受理簿一笔一笔与当前档对账。
 * 判定口径全在 {@link CaseFlowRules}，本类只负责落库与装配，不另起算法。
 * 改档与留痕在同一事务里同成同败。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TNslpCaseFlowServiceImpl implements ITNslpCaseFlowService {

    @javax.annotation.Resource
    private TNslpCaseFlowMapper nslpCaseFlowMapper;
    @javax.annotation.Resource
    private TNslpCaseIntakeMapper nslpCaseIntakeMapper;
    @javax.annotation.Resource
    private TNslpCaseNoteMapper nslpCaseNoteMapper;
    @javax.annotation.Resource
    private TNslpCaseLedgerMapper nslpCaseLedgerMapper;
    @javax.annotation.Resource
    private TNslpPointMapper nslpPointMapper;
    @javax.annotation.Resource
    private TNslpZoneBookMapper nslpZoneBookMapper;

    // ---------------------------------------------------------------- 查

    @Override
    public TNslpCaseFlow selectTNslpCaseFlowById(Long id) {
        return this.nslpCaseFlowMapper.selectById(id);
    }

    @Override
    public List<TNslpCaseFlow> selectTNslpCaseFlowList(QueryWrapper<TNslpCaseFlow> queryWrapper) {
        return this.nslpCaseFlowMapper.selectList(queryWrapper);
    }

    // ---------------------------------------------------------------- 立

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseIntake intake(CaseIntakeSubmit submit) {
        if (submit == null) {
            throw new IllegalArgumentException("受件报文是空的，草稿落不了");
        }
        TNslpCaseIntake draft = new TNslpCaseIntake();
        draft.setPointNo(submit.getPointNo());
        draft.setZoneNo(submit.getZoneNo());
        draft.setSpotCount(submit.getSpotCount());
        draft.setCheckReading(submit.getCheckReading());
        // 报文自带的关次只当参考落个档；立件、读档都以后端数出来的为准
        draft.setRefStage(submit.getRefStage());
        draft.setContent(submit.getContent());
        draft.setStatus(CaseFlowRules.INTAKE_PENDING);
        draft.setDelFlag(0);
        this.nslpCaseIntakeMapper.insert(draft);
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseIntake cancelIntake(Long intakeId) {
        TNslpCaseIntake draft = mustFindDraft(intakeId);
        if (draft.getStatus() != null
                && draft.getStatus().intValue() == CaseFlowRules.INTAKE_ESTABLISHED) {
            throw new IllegalStateException("这一条已经立成正式件，撤不回了；要收口走末关作废");
        }
        if (draft.getStatus() != null
                && draft.getStatus().intValue() == CaseFlowRules.INTAKE_CANCELLED) {
            throw new IllegalStateException("这一条先前已经撤过，不撤第二回");
        }
        draft.setStatus(CaseFlowRules.INTAKE_CANCELLED);
        // 撤掉的条不进主档、不占「没封住」名额：只置草稿，不碰 t_nslp_case_flow
        this.nslpCaseIntakeMapper.updateById(draft);
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow establish(Long intakeId) {
        TNslpCaseIntake draft = mustFindDraft(intakeId);
        if (draft.getStatus() != null
                && draft.getStatus().intValue() == CaseFlowRules.INTAKE_CANCELLED) {
            throw new IllegalStateException("这一条已经撤回去了，立不起来");
        }
        if (draft.getStatus() != null
                && draft.getStatus().intValue() == CaseFlowRules.INTAKE_ESTABLISHED) {
            throw new IllegalStateException("这一条已经立过，不立第二回");
        }

        // 四样输入点名：少一样就起不起来，回话要说出少的是哪一样
        List<String> missing = CaseFlowRules.missingInputs(draft.getPointNo(), draft.getZoneNo(),
                draft.getSpotCount(), draft.getCheckReading());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("立不起来，缺这几样：" + joinNames(missing));
        }

        // 一处点位任何一刻只许有一条没封住的件（在办的、中途歇下的都算）
        List<TNslpCaseFlow> openOnPoint = this.nslpCaseFlowMapper.selectList(
                new QueryWrapper<TNslpCaseFlow>()
                        .eq("point_no", draft.getPointNo().trim())
                        .eq("status", CaseFlowRules.STATUS_OPEN)
                        .last("for update"));
        if (!openOnPoint.isEmpty()) {
            throw new IllegalStateException("这处点位已有一条没封住的件（件次 "
                    + openOnPoint.get(0).getBizNo() + "），第二条立不起来");
        }

        TNslpCaseFlow r = new TNslpCaseFlow();
        r.setBizNo(nextBizNo());
        r.setPointNo(draft.getPointNo().trim());
        r.setZoneNo(draft.getZoneNo().trim());
        r.setSpotCount(draft.getSpotCount());
        r.setCheckReading(draft.getCheckReading());
        // 走到哪一关一律后端数出来：一律从受件数起，报文那一个数不入算
        r.setStage(CaseFlowRules.STAGE_INTAKE);
        r.setStatus(CaseFlowRules.STATUS_OPEN);
        r.setOrderReceived(0);
        // 手机端敲进来的那句原话占受件关的按语栏；离开受件关时才带着原时刻转进追溯串
        r.setContent(draft.getContent());
        if (draft.getContent() != null && !draft.getContent().trim().isEmpty()) {
            r.setNoteTime(now());
        }
        r.setLastAction(CaseFlowRules.ACTION_ESTABLISH);
        r.setDelFlag(0);
        this.nslpCaseFlowMapper.insert(r);

        draft.setStatus(CaseFlowRules.INTAKE_ESTABLISHED);
        draft.setCaseId(r.getId());
        this.nslpCaseIntakeMapper.updateById(draft);
        return r;
    }

    // ---------------------------------------------------------------- 挪

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow advance(Long id, String remark) {
        TNslpCaseFlow r = mustFindOpen(id);
        int from = r.getStage().intValue();
        int to = from + 1;
        // 只许紧邻的下一关；末关之后没有再下一关
        if (!CaseFlowRules.isAdjacentForward(from, to)) {
            throw new IllegalStateException(
                    "已在「" + CaseFlowRules.stageName(from) + "」，前面没有紧邻的下一关可挪，整条留原地");
        }
        // 门槛摆在前头：进核查看底册挂名；进复查收尾看责令核收成「可」
        boolean listed = isPointListed(r.getPointNo());
        if (!CaseFlowRules.canEnter(to, listed,
                CaseFlowRules.orderAccepted(r.getOrderReceived()))) {
            if (to == CaseFlowRules.STAGE_CHECK) {
                throw new IllegalStateException("这处点位在底册上已撤名，进不去核查，整条留原地");
            }
            if (to == CaseFlowRules.STAGE_REVIEW) {
                throw new IllegalStateException("责令关核收没记成「可」，复查收尾开不了工，整条留原地");
            }
        }
        Date at = now();
        // 离开关先前那句头按语，带着它自己提交的那一刻转进追溯串；新关从头累计
        archiveStageNote(r, from, at);
        r.setStage(to);
        // 新关从头累计：头句按语空着等头一笔
        r.setContent(null);
        r.setNoteTime(null);
        resetStageAccumulators(r, to);
        r.setLastAction(CaseFlowRules.ACTION_FORWARD);
        this.nslpCaseFlowMapper.updateById(r);
        // 改档与留痕同一事务：受理簿这一笔与改档同成同败
        writeLedger(r.getId(), from, to, at, remark);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow rollback(Long id, String remark) {
        TNslpCaseFlow r = mustFindOpen(id);
        int from = r.getStage().intValue();
        int to = from - 1;
        // 一次只收回一关；站在第一关的，没什么可再收
        if (!CaseFlowRules.isAdjacentBack(from, to)) {
            throw new IllegalStateException("已在受件头一关，没什么可再收，整条留原地");
        }
        Date at = now();
        // 离开关先前那句头按语，照原先后（按它自己提交的那一刻）转进追溯串末尾
        archiveStageNote(r, from, at);
        r.setStage(to);
        r.setContent(null);
        r.setNoteTime(null);
        // 重新挪到哪一关，哪一关的累计从头计（收回到责令，核收即清零；回入复查须重新两笔见证与凭证）
        resetStageAccumulators(r, to);
        r.setLastAction(CaseFlowRules.ACTION_ROLLBACK);
        this.nslpCaseFlowMapper.updateById(r);
        writeLedger(r.getId(), from, to, at, remark);
        return r;
    }

    // ---------------------------------------------------------------- 关件上的动作

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean note(Long id, String text) {
        TNslpCaseFlow r = mustFindOpen(id);
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("按语是空的，记不进去");
        }
        // 同一关只留头一句：第二句盖不上、也不留副本
        if (!CaseFlowRules.acceptsStageNote(r.getContent(), text)) {
            return false;
        }
        Date at = now();
        // 按语此刻只进本关按语栏，连着提交时刻；离开本关时才转进身后那一串
        r.setContent(text.trim());
        r.setNoteTime(at);
        r.setLastAction(CaseFlowRules.ACTION_NOTE);
        this.nslpCaseFlowMapper.updateById(r);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow markOrderReceived(Long id) {
        TNslpCaseFlow r = mustFindOpen(id);
        if (r.getStage().intValue() != CaseFlowRules.STAGE_ORDER) {
            throw new IllegalStateException("责令关核收只在「责令」这一关记，当前停在「"
                    + CaseFlowRules.stageName(r.getStage()) + "」");
        }
        r.setOrderReceived(1);
        r.setLastAction(CaseFlowRules.ACTION_ORDER_RECEIVED);
        this.nslpCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow witness(Long id, int side, String who) {
        TNslpCaseFlow r = mustFindOpen(id);
        if (r.getStage().intValue() != CaseFlowRules.STAGE_REVIEW) {
            throw new IllegalStateException("见证落笔只在复查收尾关，当前停在「"
                    + CaseFlowRules.stageName(r.getStage()) + "」");
        }
        if (who == null || who.trim().isEmpty()) {
            throw new IllegalArgumentException("落笔者空着，这一笔落不下");
        }
        if (side == 0) {
            if (r.getWitnessA() != null) {
                throw new IllegalStateException("见证岗甲已经落过一笔，同一侧不接第二笔");
            }
            if (r.getWitnessB() != null && r.getWitnessB().trim().equals(who.trim())) {
                throw new IllegalStateException("两名见证岗须各是各的岗，同一个人落两笔不算两名");
            }
            r.setWitnessA(who.trim());
        } else if (side == 1) {
            if (r.getWitnessB() != null) {
                throw new IllegalStateException("见证岗乙已经落过一笔，同一侧不接第二笔");
            }
            if (r.getWitnessA() != null && r.getWitnessA().trim().equals(who.trim())) {
                throw new IllegalStateException("两名见证岗须各是各的岗，同一个人落两笔不算两名");
            }
            r.setWitnessB(who.trim());
        } else {
            throw new IllegalArgumentException("见证侧只认 0（甲）或 1（乙）");
        }
        r.setLastAction(CaseFlowRules.ACTION_WITNESS);
        this.nslpCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow writeVoucher(Long id, String voucher) {
        TNslpCaseFlow r = mustFindOpen(id);
        if (r.getStage().intValue() != CaseFlowRules.STAGE_REVIEW) {
            throw new IllegalStateException("复测凭证只在复查收尾关写回本行，当前停在「"
                    + CaseFlowRules.stageName(r.getStage()) + "」");
        }
        if (voucher == null || voucher.trim().isEmpty()) {
            throw new IllegalArgumentException("复测凭证那一串是空的，写不回本行");
        }
        if (r.getRetestVoucher() != null) {
            throw new IllegalStateException("复测凭证已经写回本行，写过一次盖不上");
        }
        r.setRetestVoucher(voucher.trim());
        r.setLastAction(CaseFlowRules.ACTION_VOUCHER);
        this.nslpCaseFlowMapper.updateById(r);
        return r;
    }

    // ---------------------------------------------------------------- 封

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow seal(Long id) {
        TNslpCaseFlow r = mustFindOpen(id);
        if (r.getStage().intValue() != CaseFlowRules.STAGE_REVIEW) {
            throw new IllegalStateException("走满末关才封得住，当前还停在「"
                    + CaseFlowRules.stageName(r.getStage()) + "」");
        }
        // 两名见证岗各落一笔 ＋ 复测凭证写回本行，两样齐了才算封上
        if (!CaseFlowRules.sealReady(r.getWitnessA(), r.getWitnessB(), r.getRetestVoucher())) {
            throw new IllegalStateException("封不上：两名见证岗各落一笔与复测凭证写回，两样须齐");
        }
        Date at = now();
        close(r, CaseFlowRules.CLOSE_SEAL, CaseFlowRules.LOG_SEAL,
                CaseFlowRules.ACTION_SEAL, "走满复查收尾，封住", at);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow withdraw(Long id) {
        TNslpCaseFlow r = mustFindOpen(id);
        // 受件之后递件人自己撤回去：视作收口，不再往前挪；停在哪档就封在哪档
        Date at = now();
        close(r, CaseFlowRules.CLOSE_WITHDRAW, CaseFlowRules.LOG_WITHDRAW,
                CaseFlowRules.ACTION_WITHDRAW, "递件人受件之后撤回反映，收口", at);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow closeByZoneMerged(Long id) {
        TNslpCaseFlow r = mustFindOpen(id);
        TNslpZoneBook zone = this.nslpZoneBookMapper.selectOne(new QueryWrapper<TNslpZoneBook>()
                .eq("zone_no", r.getZoneNo())
                .eq("del_flag", 0));
        if (zone == null || zone.getStatus() == null || zone.getStatus().intValue() != 1) {
            throw new IllegalStateException("所属区册「" + r.getZoneNo() + "」在区册底里还在册，没并走，不能按并走收口");
        }
        Date at = now();
        close(r, CaseFlowRules.CLOSE_ZONE_MERGED, CaseFlowRules.LOG_ZONE_MERGED,
                CaseFlowRules.ACTION_ZONE_MERGED, "所属区册从区册底里并走，收口", at);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpCaseFlow voidCase(Long id) {
        TNslpCaseFlow r = mustFindOpen(id);
        // 作废与撤销两条道儿：作废只从最后一关发起
        if (r.getStage().intValue() != CaseFlowRules.LAST_STAGE) {
            throw new IllegalStateException("作废只从最后一关（复查收尾）发起，当前停在「"
                    + CaseFlowRules.stageName(r.getStage()) + "」");
        }
        Date at = now();
        close(r, CaseFlowRules.CLOSE_VOID, CaseFlowRules.LOG_VOID,
                CaseFlowRules.ACTION_VOID, "末关发起作废，收口", at);
        return r;
    }

    // ---------------------------------------------------------------- 捋

    @Override
    public CaseTrace trace(Long id) {
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 按提交那一刻、再按落笔先后排：同一刻各记各的，不并成一条、不吞掉一条
        List<TNslpCaseNote> entries = new ArrayList<TNslpCaseNote>(
                this.nslpCaseNoteMapper.selectList(
                        new QueryWrapper<TNslpCaseNote>()
                                .eq("case_id", id)
                                .orderByAsc("note_time", "id")));
        // 此刻还压在本关按语栏里的头句，也算这一串上的一笔（按它自己的时刻插进去）
        if (r.getContent() != null && !r.getContent().trim().isEmpty()) {
            TNslpCaseNote onStage = new TNslpCaseNote();
            onStage.setCaseId(id);
            onStage.setStage(r.getStage());
            onStage.setNote(r.getContent().trim());
            onStage.setNoteTime(r.getNoteTime());
            onStage.setLogKind(CaseFlowRules.LOG_NOTE);
            insertByTime(entries, onStage);
        }
        return CaseTrace.of(r.getId(), r.getBizNo(), r.getPointNo(),
                r.getStage(), r.getStatus(), entries);
    }

    @Override
    public int openCount() {
        // 页面上头那个数只以后端这一次查询为准，不认前端缓存，也不另起算法
        Integer n = this.nslpCaseFlowMapper.selectCount(new QueryWrapper<TNslpCaseFlow>()
                .eq("status", CaseFlowRules.STATUS_OPEN));
        return n == null ? 0 : n.intValue();
    }

    @Override
    public CaseReconcile reconcile(Long id) {
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        List<TNslpCaseLedger> rows = this.nslpCaseLedgerMapper.selectList(
                new QueryWrapper<TNslpCaseLedger>()
                        .eq("case_id", id)
                        .orderByAsc("move_time", "id"));
        List<int[]> moves = new ArrayList<int[]>(rows.size());
        for (TNslpCaseLedger l : rows) {
            moves.add(new int[]{l.getFromStage().intValue(), l.getToStage().intValue()});
        }
        // 只认这一趟点出的步：从受件一笔一笔捋，末档要正正落在当前档上
        final CaseFlowRules.Replay replay = CaseFlowRules.replay(r.getStage(), moves);
        return CaseReconcile.of(r.getId(), r.getStage(), new CaseReconcile.CaseReplaySource() {
            @Override
            public int getFinalStage() {
                return replay.getFinalStage();
            }

            @Override
            public int getLedgerCount() {
                return rows.size();
            }

            @Override
            public boolean isConsistent() {
                return replay.isConsistent();
            }

            @Override
            public List<Integer> getOffenders() {
                return replay.getOffenders();
            }

            @Override
            public int getArrivals(int stage) {
                return replay.getArrivals(stage);
            }
        });
    }

    // ------------------------------------------------ 更正与抽回（封后三不接）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long id, String text) {
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 封住之后只能看不能动：更正内容一概不接
        if (r.getStatus() != null && r.getStatus().intValue() == CaseFlowRules.STATUS_CLOSED) {
            return false;
        }
        r.setRemark(text);
        return this.nslpCaseFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        // 正式件没有物理抽回这条道：立前那一小会儿走撤销，走完末关走作废
        return false;
    }

    // ---------------------------------------------------------------- 内务

    private TNslpCaseIntake mustFindDraft(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("受件草稿号是空的");
        }
        TNslpCaseIntake draft = this.nslpCaseIntakeMapper.selectById(id);
        if (draft == null) {
            throw new IllegalArgumentException("受件草稿查无此条");
        }
        return draft;
    }

    private TNslpCaseFlow mustFindOpen(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("办件号是空的");
        }
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            throw new IllegalArgumentException("办件查无此条");
        }
        if (r.getStatus() != null && r.getStatus().intValue() == CaseFlowRules.STATUS_CLOSED) {
            // 封住（含撤回／并走／作废）之后只能看不能动
            throw new IllegalStateException("这一条已收口（" + closeText(r.getCloseKind())
                    + "），只能看不能动");
        }
        if (!CaseFlowRules.validStage(r.getStage())) {
            throw new IllegalStateException("本件档数不明，先核档再动手");
        }
        return r;
    }

    private boolean isPointListed(String pointNo) {
        TNslpPoint p = this.nslpPointMapper.selectOne(new QueryWrapper<TNslpPoint>()
                .eq("point_no", pointNo)
                .eq("del_flag", 0));
        return CaseFlowRules.pointListed(p == null ? null : p.getDelFlag());
    }

    private void close(TNslpCaseFlow r, int closeKind, int logKind, String action,
                       String sentence, Date at) {
        // 收口时本关按语栏里若还压着头句，先带着原时刻转进追溯串，收口事件再压一笔
        archiveStageNote(r, r.getStage().intValue(), at);
        r.setStatus(CaseFlowRules.STATUS_CLOSED);
        r.setCloseKind(closeKind);
        r.setLastAction(action);
        this.nslpCaseFlowMapper.updateById(r);
        // 收口事件也压进同一串，各记各的一笔
        addNote(r.getId(), r.getStage(), sentence, at, logKind);
    }

    /** 入档那一刻把该关的累计从头计：责令核收清零；复查收尾两名见证与凭证清零 */
    private static void resetStageAccumulators(TNslpCaseFlow r, int enteringStage) {
        if (enteringStage == CaseFlowRules.STAGE_ORDER) {
            r.setOrderReceived(0);
        } else if (enteringStage == CaseFlowRules.STAGE_REVIEW) {
            r.setWitnessA(null);
            r.setWitnessB(null);
            r.setRetestVoucher(null);
        }
    }

    /** 离开某关时，把关上压着的头句按语带着它自己提交的那一刻转进追溯串末尾 */
    private void archiveStageNote(TNslpCaseFlow r, int fromStage, Date at) {
        if (r.getContent() != null && !r.getContent().trim().isEmpty()) {
            addNote(r.getId(), fromStage, r.getContent().trim(),
                    r.getNoteTime() == null ? at : r.getNoteTime(), CaseFlowRules.LOG_NOTE);
        }
    }

    /** 还压在关件按语栏的头句，按它自己提交的时刻插进追溯串（时刻不明放最前） */
    private static void insertByTime(List<TNslpCaseNote> entries, TNslpCaseNote onStage) {
        int pos = entries.size();
        if (onStage.getNoteTime() != null) {
            for (int i = 0; i < entries.size(); i++) {
                Date t = entries.get(i).getNoteTime();
                if (t != null && onStage.getNoteTime().before(t)) {
                    pos = i;
                    break;
                }
            }
        } else {
            pos = 0;
        }
        entries.add(pos, onStage);
    }

    private void addNote(Long caseId, int stage, String text, Date at, int logKind) {
        TNslpCaseNote n = new TNslpCaseNote();
        n.setCaseId(caseId);
        n.setStage(stage);
        n.setNote(text);
        n.setNoteTime(at);
        n.setLogKind(logKind);
        n.setDelFlag(0);
        this.nslpCaseNoteMapper.insert(n);
    }

    private void writeLedger(Long caseId, int from, int to, Date at, String remark) {
        TNslpCaseLedger l = new TNslpCaseLedger();
        l.setCaseId(caseId);
        l.setFromStage(from);
        l.setToStage(to);
        l.setMoveTime(at);
        // 旧走法捎来的话只作这笔的备注，不顶本关按语（按语只走 note，受第二句规矩管）
        l.setRemark(remark);
        l.setDelFlag(0);
        this.nslpCaseLedgerMapper.insert(l);
    }

    private static String joinNames(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private static String closeText(Integer closeKind) {
        if (closeKind == null) {
            return "封住";
        }
        switch (closeKind.intValue()) {
            case CaseFlowRules.CLOSE_SEAL:
                return "封住";
            case CaseFlowRules.CLOSE_WITHDRAW:
                return "反映撤回";
            case CaseFlowRules.CLOSE_ZONE_MERGED:
                return "区册并走";
            case CaseFlowRules.CLOSE_VOID:
                return "作废";
            default:
                return "封住";
        }
    }

    private static String nextBizNo() {
        return "NS" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%03d", new java.util.Random().nextInt(1000));
    }

    private static Date now() {
        return new Date();
    }
}

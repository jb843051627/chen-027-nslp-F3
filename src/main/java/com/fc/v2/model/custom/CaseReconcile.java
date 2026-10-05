package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 回头捋的回话：把一条件从末关一笔一笔收回、再从头捋，受理簿笔数要与
 * 本件当前档一笔一笔合得上；只截其中任意一关单独核（到关笔数）也得自洽。
 * 合不上的笔一一点名，不并笔、不吞笔，也不许另算一套去凑。
 *
 * @author fuce
 */
@ApiModel(value = "CaseReconcile", description = "噪声办件受理簿回头捋回话")
public class CaseReconcile implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "办件主键")
    private Long caseId;

    @ApiModelProperty(value = "本件此刻停着的档")
    private Integer currentStage;

    @ApiModelProperty(value = "受理簿一笔一笔捋出来的末档")
    private Integer replayedStage;

    @ApiModelProperty(value = "受理簿上的笔数")
    private int ledgerCount;

    @ApiModelProperty(value = "每一关的入档笔数（截单关单独核用，0..3）")
    private List<Integer> arrivals = new ArrayList<Integer>();

    @ApiModelProperty(value = "合得上与否")
    private boolean consistent;

    /** 点了名的坏笔：受理簿上的第几笔（1 起） */
    @ApiModelProperty(value = "点了名的坏笔序号")
    private List<Integer> offenders = new ArrayList<Integer>();

    public static CaseReconcile of(Long caseId, Integer currentStage, CaseReplaySource source) {
        CaseReconcile r = new CaseReconcile();
        r.caseId = caseId;
        r.currentStage = currentStage;
        r.replayedStage = source.getFinalStage();
        r.ledgerCount = source.getLedgerCount();
        r.consistent = source.isConsistent();
        r.offenders = source.getOffenders();
        for (int stage = 0; stage <= 3; stage++) {
            r.arrivals.add(Integer.valueOf(source.getArrivals(stage)));
        }
        return r;
    }

    /** 回放来源：由 support 口径算好，回话只转述 */
    public interface CaseReplaySource {
        int getFinalStage();

        int getLedgerCount();

        boolean isConsistent();

        List<Integer> getOffenders();

        int getArrivals(int stage);
    }

    public Long getCaseId() {
        return caseId;
    }

    public Integer getCurrentStage() {
        return currentStage;
    }

    public Integer getReplayedStage() {
        return replayedStage;
    }

    public int getLedgerCount() {
        return ledgerCount;
    }

    public List<Integer> getArrivals() {
        return arrivals;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public List<Integer> getOffenders() {
        return offenders;
    }
}

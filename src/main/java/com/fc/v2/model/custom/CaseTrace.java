package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TNslpCaseNote;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一条件身后那一串：每一关那一句按语连着提交那一刻，按时序排好；
 * 收口事件（封住／撤回／并走／作废）也压在同一串里，各记各的，不并笔。
 *
 * @author fuce
 */
@ApiModel(value = "CaseTrace", description = "噪声办件按语追溯串")
public class CaseTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "办件主键")
    private Long caseId;

    @ApiModelProperty(value = "件次")
    private String bizNo;

    @ApiModelProperty(value = "点位码")
    private String pointNo;

    @ApiModelProperty(value = "当前关次 0..3")
    private Integer stage;

    @ApiModelProperty(value = "1在办 2已收口")
    private Integer status;

    @ApiModelProperty(value = "按时序压着的按语与收口留痕")
    private List<TNslpCaseNote> entries = new ArrayList<TNslpCaseNote>();

    public static CaseTrace of(Long caseId, String bizNo, String pointNo,
                               Integer stage, Integer status, List<TNslpCaseNote> entries) {
        CaseTrace t = new CaseTrace();
        t.caseId = caseId;
        t.bizNo = bizNo;
        t.pointNo = pointNo;
        t.stage = stage;
        t.status = status;
        if (entries != null) {
            t.entries = entries;
        }
        return t;
    }

    /** 这一串共几笔 */
    public int size() {
        return entries.size();
    }

    /** 第 idx 笔的落痕时刻（0 起），供逐笔核时序 */
    public Date timeAt(int idx) {
        return entries.get(idx).getNoteTime();
    }

    public Long getCaseId() {
        return caseId;
    }

    public String getBizNo() {
        return bizNo;
    }

    public String getPointNo() {
        return pointNo;
    }

    public Integer getStage() {
        return stage;
    }

    public Integer getStatus() {
        return status;
    }

    public List<TNslpCaseNote> getEntries() {
        return entries;
    }
}

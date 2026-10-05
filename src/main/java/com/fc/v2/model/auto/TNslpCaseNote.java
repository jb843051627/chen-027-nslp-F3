package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 噪声办件按语追溯对象 t_nslp_case_note。
 *
 * 每一关那一句按语连着提交的那一刻，按时序压在本件身后；
 * 收回一关时，那关先前的按语照原先后转进本串末尾，重入此关从头累计。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_nslp_case_note")
@ApiModel(value = "TNslpCaseNote", description = "噪声办件按语追溯")
public class TNslpCaseNote implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属办件 */
    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属办件")
    private Long caseId;

    /** 落按语时所在关次 0..3 */
    @TableField("stage")
    @ApiModelProperty(value = "关次")
    private Integer stage;

    /** 按语原话（收口事件记事件那一句） */
    @TableField("note")
    @ApiModelProperty(value = "按语原话")
    private String note;

    /** 按语提交的那一刻（不是转进追溯记录的时刻） */
    @TableField("note_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "按语提交时刻")
    private Date noteTime;

    /** 留痕种类 0关次按语 1封住 2反映撤回 3区册并走 4末关作废 */
    @TableField("log_kind")
    @ApiModelProperty(value = "留痕种类")
    private Integer logKind;

    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记")
    private Integer delFlag;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "转进追溯记录的时刻")
    private Date createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getNoteTime() {
        return noteTime;
    }

    public void setNoteTime(Date noteTime) {
        this.noteTime = noteTime;
    }

    public Integer getLogKind() {
        return logKind;
    }

    public void setLogKind(Integer logKind) {
        this.logKind = logKind;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

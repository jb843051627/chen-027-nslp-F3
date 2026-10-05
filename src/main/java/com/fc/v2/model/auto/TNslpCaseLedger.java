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
 * 噪声办件受理簿对象 t_nslp_case_ledger。
 *
 * 挪一档记一笔：推进一笔、收回一笔。同一刻两条通道各送各的，
 * 各记各的先后来，不许并成一条，也不许吞掉一条。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_nslp_case_ledger")
@ApiModel(value = "TNslpCaseLedger", description = "噪声办件受理簿")
public class TNslpCaseLedger implements Serializable {
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

    /** 挪之前关次 */
    @TableField("from_stage")
    @ApiModelProperty(value = "挪之前关次")
    private Integer fromStage;

    /** 挪之后关次（必为邻档：+1 或 -1） */
    @TableField("to_stage")
    @ApiModelProperty(value = "挪之后关次")
    private Integer toStage;

    /** 这一笔落簿的那一刻 */
    @TableField("move_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落簿时刻")
    private Date moveTime;

    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记")
    private Integer delFlag;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
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

    public Integer getFromStage() {
        return fromStage;
    }

    public void setFromStage(Integer fromStage) {
        this.fromStage = fromStage;
    }

    public Integer getToStage() {
        return toStage;
    }

    public void setToStage(Integer toStage) {
        this.toStage = toStage;
    }

    public Date getMoveTime() {
        return moveTime;
    }

    public void setMoveTime(Date moveTime) {
        this.moveTime = moveTime;
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

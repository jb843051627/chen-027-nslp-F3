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
 * 噪声投诉与超标处置办件流转单对象 t_nslp_case_flow
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_nslp_case_flow")
@ApiModel(value = "TNslpCaseFlow", description = "噪声投诉与超标处置办件流转单")
public class TNslpCaseFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 噪声投诉与超标处置办件的件次 */
    @TableField("biz_no")
    @ApiModelProperty(value = "噪声投诉与超标处置办件的件次")
    private String bizNo;

    /** 点位码（立件四样之一；一处点位只许有一条没封住的件） */
    @TableField("point_no")
    @ApiModelProperty(value = "点位码")
    private String pointNo;

    /** 所属区册代号（立件四样之一） */
    @TableField("zone_no")
    @ApiModelProperty(value = "所属区册代号")
    private String zoneNo;

    /** 处数（立件四样之一） */
    @TableField("spot_count")
    @ApiModelProperty(value = "处数")
    private Integer spotCount;

    /** 准备折算的那一个核查读数（立件四样之一） */
    @TableField("check_reading")
    @ApiModelProperty(value = "准备折算的核查读数")
    private java.math.BigDecimal checkReading;

    /** 当前关次 0..3（受件/核查/责令/复查收尾），一律后端数出来落库 */
    @TableField("stage")
    @ApiModelProperty(value = "当前关次 0..3（受件/核查/责令/复查收尾）")
    private Integer stage;

    /** 单子走到哪一步 1在办（没封住） 2已收口（封住/撤回/并走/作废） */
    @TableField("status")
    @ApiModelProperty(value = "1在办 2已收口")
    private Integer status;

    /** 本关那一句按语（一关只留头一句，第二句盖不上也不留副本） */
    @TableField("content")
    @ApiModelProperty(value = "本关按语（只留头一句）")
    private String content;

    /** 头句按语提交的那一刻（收回转进追溯串时照原先后排） */
    @TableField("note_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "头句按语提交时刻")
    private Date noteTime;

    /** 责令关核收 0未记成 1记成「可」（进复查收尾的门槛；重入责令关从头计） */
    @TableField("order_received")
    @ApiModelProperty(value = "责令核收是否记成可")
    private Integer orderReceived;

    /** 复查收尾见证岗甲落笔者 */
    @TableField("witness_a")
    @ApiModelProperty(value = "见证岗甲落笔者")
    private String witnessA;

    /** 复查收尾见证岗乙落笔者（须与甲各落一笔） */
    @TableField("witness_b")
    @ApiModelProperty(value = "见证岗乙落笔者")
    private String witnessB;

    /** 复测凭证那一串（写回本行才许封） */
    @TableField("retest_voucher")
    @ApiModelProperty(value = "复测凭证串")
    private String retestVoucher;

    /** 收口种类 1走满末关封住 2反映撤回 3区册并走 4末关作废 */
    @TableField("close_kind")
    @ApiModelProperty(value = "收口种类 1封住 2撤回 3并走 4作废")
    private Integer closeKind;

    /** 最近一次挪关动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次挪关动作")
    private String lastAction;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public String getZoneNo() {
        return zoneNo;
    }

    public void setZoneNo(String zoneNo) {
        this.zoneNo = zoneNo;
    }

    public Integer getSpotCount() {
        return spotCount;
    }

    public void setSpotCount(Integer spotCount) {
        this.spotCount = spotCount;
    }

    public java.math.BigDecimal getCheckReading() {
        return checkReading;
    }

    public void setCheckReading(java.math.BigDecimal checkReading) {
        this.checkReading = checkReading;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getNoteTime() {
        return noteTime;
    }

    public void setNoteTime(Date noteTime) {
        this.noteTime = noteTime;
    }

    public Integer getOrderReceived() {
        return orderReceived;
    }

    public void setOrderReceived(Integer orderReceived) {
        this.orderReceived = orderReceived;
    }

    public String getWitnessA() {
        return witnessA;
    }

    public void setWitnessA(String witnessA) {
        this.witnessA = witnessA;
    }

    public String getWitnessB() {
        return witnessB;
    }

    public void setWitnessB(String witnessB) {
        this.witnessB = witnessB;
    }

    public String getRetestVoucher() {
        return retestVoucher;
    }

    public void setRetestVoucher(String retestVoucher) {
        this.retestVoucher = retestVoucher;
    }

    public Integer getCloseKind() {
        return closeKind;
    }

    public void setCloseKind(Integer closeKind) {
        this.closeKind = closeKind;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
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

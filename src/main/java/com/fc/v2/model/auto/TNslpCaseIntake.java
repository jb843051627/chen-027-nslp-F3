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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 噪声办件手机端受件草稿对象 t_nslp_case_intake
 *
 * 手机端只管把字敲进去、把数回显出来；走到哪一关一律后端数。
 * 报文自带的关次（refStage）只当参考，写库读档都不作数。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_nslp_case_intake")
@ApiModel(value = "TNslpCaseIntake", description = "噪声办件手机端受件草稿")
public class TNslpCaseIntake implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 点位码（手机端照敲） */
    @TableField("point_no")
    @ApiModelProperty(value = "点位码")
    private String pointNo;

    /** 所属区册代号（手机端照敲） */
    @TableField("zone_no")
    @ApiModelProperty(value = "所属区册代号")
    private String zoneNo;

    /** 处数（手机端回显） */
    @TableField("spot_count")
    @ApiModelProperty(value = "处数")
    private Integer spotCount;

    /** 准备折算的核查读数（手机端回显） */
    @TableField("check_reading")
    @ApiModelProperty(value = "准备折算的核查读数")
    private BigDecimal checkReading;

    /** 报文自带的关次：只当参考，立件、读档都以后端数出来的为准 */
    @TableField("ref_stage")
    @ApiModelProperty(value = "报文自带关次（只作参考）")
    private Integer refStage;

    /** 反映人敲进来的原话 */
    @TableField("content")
    @ApiModelProperty(value = "反映原话")
    private String content;

    /** 草稿情形 0待立 1已立（已转正式件） 2已撤 */
    @TableField("status")
    @ApiModelProperty(value = "0待立 1已立 2已撤")
    private Integer status;

    /** 立成正式件后那一条的主键 */
    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "立成正式件的主键")
    private Long caseId;

    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
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

    public BigDecimal getCheckReading() {
        return checkReading;
    }

    public void setCheckReading(BigDecimal checkReading) {
        this.checkReading = checkReading;
    }

    public Integer getRefStage() {
        return refStage;
    }

    public void setRefStage(Integer refStage) {
        this.refStage = refStage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
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

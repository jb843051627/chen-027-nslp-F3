package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 手机端递上来的受件草稿形状。
 *
 * 手机端只管把字敲进去、把数回显出来；走到哪一关一律由后端数。
 * refStage 是报文自带的关次，只当参考落个档，立件、读档都不作数。
 *
 * @author fuce
 */
@ApiModel(value = "CaseIntakeSubmit", description = "噪声办件手机端受件草稿")
public class CaseIntakeSubmit implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "点位码")
    private String pointNo;

    @ApiModelProperty(value = "所属区册代号")
    private String zoneNo;

    @ApiModelProperty(value = "处数")
    private Integer spotCount;

    @ApiModelProperty(value = "准备折算的核查读数")
    private BigDecimal checkReading;

    @ApiModelProperty(value = "报文自带的关次（只作参考）")
    private Integer refStage;

    @ApiModelProperty(value = "反映人敲进来的原话")
    private String content;

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
}

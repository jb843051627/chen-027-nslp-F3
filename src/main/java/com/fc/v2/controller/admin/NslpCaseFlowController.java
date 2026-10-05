package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TNslpCaseFlow;
import com.fc.v2.model.auto.TNslpCaseIntake;
import com.fc.v2.model.custom.CaseIntakeSubmit;
import com.fc.v2.service.ITNslpCaseFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 噪声投诉与超标处置办件流转单 Controller（state-machine 形状：流转入口）。
 *
 * 一处点位一条，从受件起到复查收尾止。四档先后钉死，走到哪一关一律后端
 * 数出来；手机端报文自带的关次只当参考。封后只能看不能动，名册照常露脸。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "噪声投诉与超标处置办件流转单")
@Controller
@RequestMapping("/nslpCaseFlow")
public class NslpCaseFlowController extends BaseController {

    private final String prefix = "admin/nslpCaseFlow";

    @Autowired
    private ITNslpCaseFlowService nslpCaseFlowService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("nslpCaseFlow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "噪声投诉与超标处置办件流转单流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "在办与已收口都在名册露脸，已收口的不给动手把手")
    @GetMapping("/list")
    @RequiresPermissions("nslpCaseFlow:list")
    @ResponseBody
    public ResultTable list(TNslpCaseFlow record) {
        QueryWrapper<TNslpCaseFlow> queryWrapper = new QueryWrapper<TNslpCaseFlow>()
                .orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<TNslpCaseFlow> page =
                new com.github.pagehelper.PageInfo<TNslpCaseFlow>(
                        nslpCaseFlowService.selectTNslpCaseFlowList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "噪声办件手机端受件", action = "intake")
    @ApiOperation(value = "手机端受件", notes = "只装敲进来的字与回显的数，报文关次只当参考")
    @PostMapping("/intake")
    @RequiresPermissions("nslpCaseFlow:intake")
    @ResponseBody
    public AjaxResult intake(@RequestBody CaseIntakeSubmit submit) {
        TNslpCaseIntake draft = nslpCaseFlowService.intake(submit);
        return AjaxResult.successData(200, draft);
    }

    @Log(title = "噪声办件立件前撤销", action = "cancel")
    @ApiOperation(value = "立件前撤销", notes = "只可能发生在正式立起来之前；不进主档、不占没封住名额")
    @PostMapping("/cancel")
    @RequiresPermissions("nslpCaseFlow:cancel")
    @ResponseBody
    public AjaxResult cancel(Long intakeId) {
        return toAjax(nslpCaseFlowService.cancelIntake(intakeId) != null ? 1 : 0);
    }

    @Log(title = "噪声办件立件", action = "establish")
    @ApiOperation(value = "立件", notes = "四样齐才立；关次一律从受件数起；同点位只许一条没封住的件")
    @PostMapping("/establish")
    @RequiresPermissions("nslpCaseFlow:establish")
    @ResponseBody
    public AjaxResult establish(Long intakeId) {
        return AjaxResult.successData(200, nslpCaseFlowService.establish(intakeId));
    }

    @Log(title = "噪声投诉与超标处置办件流转单推进", action = "advance")
    @ApiOperation(value = "往前挪紧邻下一关", notes = "一次只许 +1；跨关不生效，整条留原地。旧口原样留着，捎带的话只进该笔备注")
    @PostMapping("/advance")
    @RequiresPermissions("nslpCaseFlow:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String remark) {
        return AjaxResult.successData(200, nslpCaseFlowService.advance(id, remark));
    }

    @Log(title = "噪声投诉与超标处置办件流转单回退", action = "rollback")
    @ApiOperation(value = "往后收回一关", notes = "一次只许 -1；离开关的头句按语照原时刻转进追溯串。旧口原样留着")
    @PostMapping("/rollback")
    @RequiresPermissions("nslpCaseFlow:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return AjaxResult.successData(200, nslpCaseFlowService.rollback(id, remark));
    }

    @Log(title = "噪声办件关次按语", action = "note")
    @ApiOperation(value = "本关落一句按语", notes = "只留头一句，第二句盖不上也不留副本")
    @PostMapping("/note")
    @RequiresPermissions("nslpCaseFlow:note")
    @ResponseBody
    public AjaxResult note(Long id, String text) {
        return nslpCaseFlowService.note(id, text)
                ? success("按语已记") : error("本关头一句已经在了，第二句盖不上也不留副本");
    }

    @Log(title = "噪声办件责令核收", action = "orderReceived")
    @ApiOperation(value = "责令核收记成可", notes = "进复查收尾的门槛；只在责令关记，重入责令关从头计")
    @PostMapping("/orderReceived")
    @RequiresPermissions("nslpCaseFlow:order")
    @ResponseBody
    public AjaxResult orderReceived(Long id) {
        return AjaxResult.successData(200, nslpCaseFlowService.markOrderReceived(id));
    }

    @Log(title = "噪声办件见证落笔", action = "witness")
    @ApiOperation(value = "见证岗落笔", notes = "side=0甲 side=1乙，两名各落一笔")
    @PostMapping("/witness")
    @RequiresPermissions("nslpCaseFlow:witness")
    @ResponseBody
    public AjaxResult witness(Long id, Integer side, String who) {
        return AjaxResult.successData(200,
                nslpCaseFlowService.witness(id, side == null ? 0 : side.intValue(), who));
    }

    @Log(title = "噪声办件复测凭证写回", action = "voucher")
    @ApiOperation(value = "复测凭证写回本行", notes = "封门两样之一，只在复查收尾写")
    @PostMapping("/voucher")
    @RequiresPermissions("nslpCaseFlow:voucher")
    @ResponseBody
    public AjaxResult voucher(Long id, String voucher) {
        return AjaxResult.successData(200, nslpCaseFlowService.writeVoucher(id, voucher));
    }

    @Log(title = "噪声办件封门", action = "seal")
    @ApiOperation(value = "走满末关封住", notes = "两名见证各一笔＋凭证写回，两样齐才封")
    @PostMapping("/seal")
    @RequiresPermissions("nslpCaseFlow:seal")
    @ResponseBody
    public AjaxResult seal(Long id) {
        return AjaxResult.successData(200, nslpCaseFlowService.seal(id));
    }

    @Log(title = "噪声办件反映撤回", action = "withdraw")
    @ApiOperation(value = "递件人撤回反映", notes = "受件之后撤的，视作收口，不再往前挪")
    @PostMapping("/withdraw")
    @RequiresPermissions("nslpCaseFlow:withdraw")
    @ResponseBody
    public AjaxResult withdraw(Long id) {
        return AjaxResult.successData(200, nslpCaseFlowService.withdraw(id));
    }

    @Log(title = "噪声办件区册并走收口", action = "zoneMerged")
    @ApiOperation(value = "区册并走收口", notes = "所属区册从区册底里并走，视作收口")
    @PostMapping("/zoneMerged")
    @RequiresPermissions("nslpCaseFlow:merge")
    @ResponseBody
    public AjaxResult zoneMerged(Long id) {
        return AjaxResult.successData(200, nslpCaseFlowService.closeByZoneMerged(id));
    }

    @Log(title = "噪声办件末关作废", action = "void")
    @ApiOperation(value = "末关作废", notes = "作废只从最后一关发起；与立前撤销是两条道儿")
    @PostMapping("/void")
    @RequiresPermissions("nslpCaseFlow:void")
    @ResponseBody
    public AjaxResult voidCase(Long id) {
        return AjaxResult.successData(200, nslpCaseFlowService.voidCase(id));
    }

    @ApiOperation(value = "按语追溯串", notes = "每关按语连着提交时刻，按时序取")
    @GetMapping("/trace")
    @RequiresPermissions("nslpCaseFlow:trace")
    @ResponseBody
    public AjaxResult trace(Long id) {
        return AjaxResult.successData(200, nslpCaseFlowService.trace(id));
    }

    @ApiOperation(value = "没封住的条数", notes = "只以后端这一次查询为准，不认前端缓存，不另起算法")
    @GetMapping("/openCount")
    @RequiresPermissions("nslpCaseFlow:list")
    @ResponseBody
    public AjaxResult openCount() {
        return AjaxResult.successData(200, nslpCaseFlowService.openCount());
    }

    @ApiOperation(value = "回头捋", notes = "受理簿一笔一笔对当前档，截单关单独核也自洽，坏笔点名")
    @GetMapping("/reconcile")
    @RequiresPermissions("nslpCaseFlow:reconcile")
    @ResponseBody
    public AjaxResult reconcile(Long id) {
        return AjaxResult.successData(200, nslpCaseFlowService.reconcile(id));
    }
}

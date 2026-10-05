package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TNslpCaseFlow;
import com.fc.v2.model.auto.TNslpCaseIntake;
import com.fc.v2.model.custom.CaseIntakeSubmit;
import com.fc.v2.model.custom.CaseReconcile;
import com.fc.v2.model.custom.CaseTrace;

/**
 * 噪声投诉与超标处置办件流转 Service接口（state-machine 形状：单据流转）。
 *
 * 四档先后钉死：受件→核查→责令→复查收尾。走到哪一关一律后端数出来，
 * 报文自带关次只作参考。立、挪、封、捋四类动作各自的准入与留痕都只
 * 认这一处口径，旁处不下结论、不预设结果。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITNslpCaseFlowService {

    /** 按主键查询单据（已收口的也照常查得出，只是没了动手的把手） */
    TNslpCaseFlow selectTNslpCaseFlowById(Long id);

    /** 台账查询（在办与已收口都在名册里露脸） */
    List<TNslpCaseFlow> selectTNslpCaseFlowList(QueryWrapper<TNslpCaseFlow> queryWrapper);

    /** 手机端受件：只装敲进来的字与回显的数，落待立草稿；报文关次只当参考 */
    TNslpCaseIntake intake(CaseIntakeSubmit submit);

    /** 立件前那一小会儿把反映撤回去：草稿置撤，不进主档、不占没封住名额 */
    TNslpCaseIntake cancelIntake(Long intakeId);

    /** 把待立草稿立成正式件：四样齐、同点位没别的没封住件；关次一律从受件数起 */
    TNslpCaseFlow establish(Long intakeId);

    /** 往前挪紧邻的下一关（+1），跨关不生效；改档与留痕同一事务。旧名原样留着 */
    TNslpCaseFlow advance(Long id, String remark);

    /** 无按语的挪关旧走法：等价承接，不多一条道儿 */
    default TNslpCaseFlow advance(Long id) {
        return advance(id, null);
    }

    /** 往后收回一关（-1），离开关的头句按语照原时刻转进追溯串；头一关无可收。旧名原样留着 */
    TNslpCaseFlow rollback(Long id, String remark);

    /** 无按语的收回旧走法：等价承接 */
    default TNslpCaseFlow rollback(Long id) {
        return rollback(id, null);
    }

    /** 本关塞一句按语：只留头一句，第二句盖不上也不留副本（返 false）；已收口不接 */
    boolean note(Long id, String text);

    /** 责令关核收记成「可」：只在责令关受理，重入责令关从头计 */
    TNslpCaseFlow markOrderReceived(Long id);

    /** 复查收尾见证岗落笔：side=0 甲、side=1 乙，各落一笔，同一侧只落头一笔 */
    TNslpCaseFlow witness(Long id, int side, String who);

    /** 复测凭证那一串写回本行：只在复查收尾写，写过一次盖不上 */
    TNslpCaseFlow writeVoucher(Long id, String voucher);

    /** 走满末关封门：两名见证各一笔＋凭证写回，两样齐才封；封后只能看不能动 */
    TNslpCaseFlow seal(Long id);

    /** 递件人受件之后自己撤回去：视作收口，不再往前挪 */
    TNslpCaseFlow withdraw(Long id);

    /** 所属区册从区册底里并走：视作收口（区册须确已撤出） */
    TNslpCaseFlow closeByZoneMerged(Long id);

    /** 作废：只从最后一关发起 */
    TNslpCaseFlow voidCase(Long id);

    /** 本件身后那一串：每关按语连着提交时刻，按时序取（封件也取得出） */
    CaseTrace trace(Long id);

    /** 没封住的条数：起一条加一、封一条减一，只以后端这一次查询为准 */
    int openCount();

    /** 回头捋：受理簿一笔一笔对当前档，截单关单独核也自洽，坏笔点名 */
    CaseReconcile reconcile(Long id);

    /** 更正内容：已收口不接；封前只更备注，不碰按语栏 */
    boolean updateContent(Long id, String text);

    /** 整条抽回：正式件一概不接物理抽回（立前走撤销，走完走作废） */
    boolean remove(Long id);
}

package jp.co.sss.lms.form;

import java.util.Date;

import lombok.Data;

/**
 * 勤怠一括登録画面
 * 
 * @author 櫻井宝生 - Task.58
 */
@Data
public class AttendanceBulkForm {
	/** 期間(FROM) */
	private Date searchPeriodFrom;
	/** 期間(To) */
	private Date searchPeriodTo;
	/** 会場ID */
	private Integer placeId;
}

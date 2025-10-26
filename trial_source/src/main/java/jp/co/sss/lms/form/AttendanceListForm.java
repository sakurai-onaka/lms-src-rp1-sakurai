package jp.co.sss.lms.form;

import lombok.Data;

/**
 * 勤怠情報確認（受講生一覧）画面
 * 
 * @author 櫻井宝生 - Task.57
 */
@Data
public class AttendanceListForm {

	/** コース名 */
	String courseName;
	/** 企業名 */
	String companyName;
	/** ユーザー名 */
	String userName;
	
}

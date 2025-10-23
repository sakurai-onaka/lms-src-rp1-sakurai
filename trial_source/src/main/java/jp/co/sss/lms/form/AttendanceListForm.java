package jp.co.sss.lms.form;

import lombok.Data;

/**
 * 勤怠情報確認（受講生一覧）画面
 * 
 * @author 櫻井宝生 - Task.57
 */
@Data
public class AttendanceListForm {

	/** コースDTOリスト */
	String courseName;
	/** コースDTOリスト */
	String companyName;
	/** コースDTOリスト */
	String userName;
	
}

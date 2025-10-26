package jp.co.sss.lms.form;

import java.util.List;

import jp.co.sss.lms.dto.CompanyDto;
import jp.co.sss.lms.dto.CourseDto;
import jp.co.sss.lms.entity.MPlace;
import lombok.Data;

/**
 * 勤怠情報確認（受講生一覧）画面
 * 
 * @author 櫻井宝生 - Task.57
 */
@Data
public class AttendanceCheckForm {
	
	/** コースDTOリスト */
	List<CourseDto> courseDtoList;
	/** 会場リスト */
	MPlace mPlace;
	/** 企業DTOリスト */
	List<CompanyDto> companyDto;
	/** 勤怠情報確認（受講生一覧）画面入力情報 */
	AttendanceListForm attendanceListForm;
}

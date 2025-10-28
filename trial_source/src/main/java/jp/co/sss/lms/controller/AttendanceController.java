package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.form.AttendanceBulkForm;
import jp.co.sss.lms.form.AttendanceCheckForm;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.AttendanceListForm;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.LoginUserUtil;

/**
 * 勤怠管理コントローラ
 * 
 * @author 東京ITスクール
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	private StudentAttendanceService studentAttendanceService;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private LoginUserUtil loginUserUtil;

	/**
	 * 勤怠管理画面 初期表示
	 * 
	 * @param lmsUserId
	 * @param courseId
	 * @param model
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/detail", method = RequestMethod.GET)
	public String index(Model model, Integer lmsUserId, Integer courseId) {
		// 櫻井宝生 – Task.57
		if (loginUserUtil.isStudent()) {
			// 櫻井宝生 – Task.25　
			boolean notInputFlg = studentAttendanceService.pastDaysCheck();
			model.addAttribute("noInputPastDaysFlg", notInputFlg);
			courseId = loginUserDto.getCourseId();
			lmsUserId = loginUserDto.getLmsUserId();
		}
		// 勤怠一覧の取得
		// 櫻井宝生 – Task.57
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(courseId, lmsUserId);
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);
		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『出勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
	public String punchIn(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchIn();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『退勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
	public String punchOut(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchOut();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
	 * 
	 * @param model
	 * @return 勤怠情報直接変更画面
	 */
	@RequestMapping(path = "/update")
	public String update(Model model) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		// 勤怠フォームの生成
		AttendanceForm attendanceForm = studentAttendanceService
				.setAttendanceForm(attendanceManagementDtoList);
		model.addAttribute("attendanceForm", attendanceForm);

		return "attendance/update";
	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 * 
	 * @param attendanceForm
	 * @param model
	 * @param result
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
	public String complete(AttendanceForm attendanceForm, Model model, BindingResult result)
			throws ParseException {

		//櫻井宝生 - Task.27 更新前のチェック
		List<String> errors = studentAttendanceService.inputCheck(attendanceForm);
		model.addAttribute("errors", errors);
		if (errors.size() == 0) {
			// 更新
			String message = studentAttendanceService.update(attendanceForm);
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		if (errors.size() == 0) {
			model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

			return "attendance/detail";
		} else {
			// 勤怠フォームの生成
			attendanceForm = studentAttendanceService
					.setAttendanceForm(attendanceManagementDtoList);
			model.addAttribute("attendanceForm", attendanceForm);

			return "attendance/update";
		}
	}

	/**
	 * 勤怠管理画面 初期表示
	 * @author 櫻井宝生 - Task.57
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/list", method = RequestMethod.GET)
	public String list(Model model, AttendanceListForm attendanceListForm) {

		// 検索フォームの生成
		AttendanceCheckForm attendanceCheckForm = studentAttendanceService
				.setAttendanceCheckFormInput();
		attendanceCheckForm.setAttendanceListForm(attendanceListForm);
		model.addAttribute("attendanceCheckForm", attendanceCheckForm);
		return "attendance/list";
	}

	/**
	 * 勤怠管理画面 検索処理
	 * @author 櫻井宝生 - Task.57
	 * @param model
	 * @param attendanceListForm
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/list", method = RequestMethod.POST)
	public String searchAtt(Model model, AttendanceListForm attendanceListForm) {
		// 検索フォームの生成
		AttendanceCheckForm attendanceCheckForm = studentAttendanceService
				.setAttendanceCheckFormInput();
		//検索
		List<UserDetailDto> lmsUserDtoList = studentAttendanceService.getLmsUsers(attendanceListForm.getCourseName(),
				attendanceListForm.getCompanyName(), attendanceListForm.getUserName(),
				attendanceCheckForm.getMPlace().getPlaceId(), Constants.CODE_VAL_ROLL_STUDENT);

		attendanceCheckForm.setAttendanceListForm(attendanceListForm);
		model.addAttribute("attendanceCheckForm", attendanceCheckForm);
		model.addAttribute("lmsUserDtoList", lmsUserDtoList);
		return "attendance/list";
	}

	/**
	 * 勤怠管理画面 初期表示
	 * @author 櫻井宝生 - Task.58
	 * @param model
	 * @return 勤怠一括登録
	 */
	@RequestMapping(path = "/bulkRegist", method = RequestMethod.GET)
	public String bulkRegist(Model model) {
		// 検索フォームの生成
		String className = studentAttendanceService.getClassName();
		model.addAttribute("className", className);
		return "attendance/bulkRegist";
	}
	
	/**
	 * 勤怠管理画面 『検索』ボタン押下
	 * @author 櫻井宝生 - Task.58
	 * @param model
	 * @return 勤怠一括登録
	 */
	@RequestMapping(path = "/bulkRegist/search", method = RequestMethod.POST)
	public String bulkRegist(Model model,AttendanceBulkForm attendanceBulkForm) {
		//入力チェック
		String error = studentAttendanceService.searchValueCheck(attendanceBulkForm);
		if(error != null) {
			model.addAttribute(error);
			return "attendance/bulkRegist";
		}
		// 検索フォームの生成
		String className = studentAttendanceService.getClassName();
		model.addAttribute("className", className);
		return "attendance/bulkRegist";
	}

}
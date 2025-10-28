package jp.co.sss.lms.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.entity.MPlace;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceBulkForm;
import jp.co.sss.lms.form.AttendanceCheckForm;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.MCompanyMapper;
import jp.co.sss.lms.mapper.MCourseMapper;
import jp.co.sss.lms.mapper.MLmsUserMapper;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;
	@Autowired
	private MCourseMapper mCourseMapper;
	@Autowired
	private MPlaceMapper mPlaceMapper;
	@Autowired
	private MCompanyMapper mCompanyMapper;
	@Autowired
	private MLmsUserMapper mLmsUserMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		//櫻井宝生 - Task.26追加機能　開始
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		attendanceForm.setHourTimes(attendanceUtil.getHourMap());
		attendanceForm.setMinuteTimes(attendanceUtil.getMinuteMap());
		//櫻井宝生 - Task.26追加機能　終了

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			//櫻井宝生 - Task.26追加　開始
			dailyAttendanceForm.setTrainingStartTimeHour(
					attendanceUtil.getHour(attendanceManagementDto.getTrainingStartTime()));
			dailyAttendanceForm.setTrainingStartTimeMinute(
					attendanceUtil.getMinute(attendanceManagementDto.getTrainingStartTime()));
			dailyAttendanceForm
					.setTrainingEndTimeHour(attendanceUtil.getHour(attendanceManagementDto.getTrainingEndTime()));
			dailyAttendanceForm.setTrainingEndTimeMinute(
					attendanceUtil.getMinute(attendanceManagementDto.getTrainingEndTime()));
			//櫻井宝生 - Task.26　終了
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			//櫻井宝生 - Task.26開始　入力された「時」「分」を連結して出退勤時間に代入する
			if (dailyAttendanceForm.getTrainingStartTimeHour() != null
					&& dailyAttendanceForm.getTrainingStartTimeMinute() != null)
				dailyAttendanceForm.setTrainingStartTime(dailyAttendanceForm.getTrainingStartTimeHour() + ":"
						+ dailyAttendanceForm.getTrainingStartTimeMinute());
			;
			if (dailyAttendanceForm.getTrainingEndTimeHour() != null
					&& dailyAttendanceForm.getTrainingEndTimeMinute() != null)
				dailyAttendanceForm.setTrainingEndTime(dailyAttendanceForm.getTrainingEndTimeHour() + ":"
						+ dailyAttendanceForm.getTrainingEndTimeMinute());
			//櫻井宝生 - Task.26終了
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
			TrainingTime trainingStartTime = null;
			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			// 退勤時刻整形
			TrainingTime trainingEndTime = null;
			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 過去日の未入力チェック
	 * @author 櫻井宝生  – 櫻井宝生 - Task.25
	 * @return 過去日未入力判定結果
	 */
	public boolean pastDaysCheck() {
		Date trainingDate = attendanceUtil.getTrainingDate();
		Integer notInputAttDateConut = tStudentAttendanceMapper.notEnterCount(loginUserDto.getLmsUserId(),
				Constants.DB_FLG_FALSE, trainingDate);
		if (notInputAttDateConut > 0) {
			return true;
		}
		return false;
	}

	/**
	 * 入力チェック
	 * @author 櫻井宝生 - Task.27
	 * @return エラーメッセージ
	 */
	public List<String> inputCheck(AttendanceForm attendanceForm) {
		//該当の勤怠リスト[n]のnの部分
		int n = 0;
		List<String> errors = new ArrayList<>();
		//入力された情報分入力チェックを行う
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			n++;
			//備考が100文字を超過する場合
			if (dailyAttendanceForm.getNote().length() > 100) {
				errors.add(messageUtil.getMessage(Constants.VALID_KEY_MAXLENGTH, new String[] { "備考", "100" }));
			}

			//出勤時間（時）、出勤時間（分）の一方が入力有り　＆　もう一方が入力なしの場合
			if (dailyAttendanceForm.getTrainingStartTimeHour() != null
					|| dailyAttendanceForm.getTrainingStartTimeMinute() != null) {
				if (!(dailyAttendanceForm.getTrainingStartTimeHour() != null
						&& dailyAttendanceForm.getTrainingStartTimeMinute() != null)) {
					errors.add(messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "出勤時間" }));
				}
			}

			//退勤時間（時）、退勤時間（分）の一方が入力有り　＆　もう一方が入力なしの場合
			if (dailyAttendanceForm.getTrainingEndTimeHour() != null
					|| dailyAttendanceForm.getTrainingEndTimeMinute() != null) {
				if (!(dailyAttendanceForm.getTrainingEndTimeHour() != null
						&& dailyAttendanceForm.getTrainingEndTimeMinute() != null)) {
					errors.add(messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "退勤時間" }));
				}
			}

			//出勤時間に入力なし＆退勤時間に入力あり
			if (dailyAttendanceForm.getTrainingStartTimeHour() == null
					|| dailyAttendanceForm.getTrainingStartTimeMinute() == null) {
				if (dailyAttendanceForm.getTrainingEndTimeHour() != null
						|| dailyAttendanceForm.getTrainingEndTimeMinute() != null) {
					errors.add(messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY));
				}
			}

			//出勤時間 > 退勤時間の場合
			//単純に時間を比較をするのでyyyy/mm/ddは固定
			if (dailyAttendanceForm.getTrainingStartTimeHour() != null
					&& dailyAttendanceForm.getTrainingStartTimeMinute() != null
					&& dailyAttendanceForm.getTrainingEndTimeHour() != null
					&& dailyAttendanceForm.getTrainingEndTimeMinute() != null) {
				//出勤時間取得
				TrainingTime trainingTime = attendanceUtil.calcJukoTime(
						new TrainingTime(dailyAttendanceForm.getTrainingStartTimeHour(),
								dailyAttendanceForm.getTrainingStartTimeMinute()),
						new TrainingTime(dailyAttendanceForm.getTrainingEndTimeHour(),
								dailyAttendanceForm.getTrainingEndTimeMinute()));
				if (trainingTime == null) {
					errors.add(messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE,
							new String[] { String.valueOf(n) }));
				}
			}
			//中抜け時間が勤務時間を超えるかチェック
			if (dailyAttendanceForm.getTrainingEndTimeHour() != null
					&& dailyAttendanceForm.getTrainingEndTimeMinute() != null
					&& dailyAttendanceForm.getTrainingStartTimeHour() != null
					&& dailyAttendanceForm.getTrainingStartTimeMinute() != null
					&& dailyAttendanceForm.getBlankTime() != null) {
				//勤務時間算出
				TrainingTime trainingTime = attendanceUtil.calcJukoTime(
						new TrainingTime(dailyAttendanceForm.getTrainingStartTimeHour(),
								dailyAttendanceForm.getTrainingStartTimeMinute()),
						new TrainingTime(dailyAttendanceForm.getTrainingEndTimeHour(),
								dailyAttendanceForm.getTrainingEndTimeMinute()));
				//不正な入力値の場合勤務時間(分)を0にする
				Integer trainingTimeMinute;
				if (trainingTime != null) {
					trainingTimeMinute = trainingTime.getHour() * 60 + trainingTime.getMinute();
				} else {
					trainingTimeMinute = 0;
				}
				Integer blankMinute = dailyAttendanceForm.getBlankTime();

				//中抜け時間と比較
				if (trainingTimeMinute < blankMinute) {
					errors.add(messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_BLANKTIMEERROR));
				}
			}
		}
		return errors;
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @author 櫻井宝生 - Task.57
	 * @return 勤怠編集フォーム
	 */
	public AttendanceCheckForm setAttendanceCheckFormInput() {
		AttendanceCheckForm attendanceCheckForm = new AttendanceCheckForm();
		attendanceCheckForm.setCourseDtoList(
				mCourseMapper.getCourseDtoList(Constants.DB_HIDDEN_FLG_FALSE, Constants.DB_FLG_FALSE));
		attendanceCheckForm.setMPlace(mPlaceMapper.findByPlaceId(loginUserDto.getPlaceId(), Constants.DB_FLG_FALSE));
		attendanceCheckForm.setCompanyDto(mCompanyMapper.getCompanyDto(Constants.DB_FLG_FALSE));

		return attendanceCheckForm;
	}

	/**
	 * 勤怠情報確認（受講生一覧）画面検索
	 * 
	 * @author 櫻井宝生 - Task.57
	 * @param courseName
	 * @param companyName
	 * @param userName
	 * @param mPlace
	 * @param role
	 * @return ユーザー詳細DTOリスト
	 */
	public List<UserDetailDto> getLmsUsers(String courseName, String companyName, String userName, Integer placeId,
			String role) {
		// 勤怠管理リストの取得
		List<UserDetailDto> lmsUserDtoList = mLmsUserMapper.getUserDetailForSearch(courseName, companyName, userName,
				placeId, role, Constants.DB_FLG_FALSE);

		return lmsUserDtoList;
	}

	/**
	 * 会場DTOの取得
	 * 
	 * @author 櫻井宝生 - Task.58
	 * @return 勤怠編集フォーム
	 * 
	 */
	public String getClassName() {
		MPlace mPlace = mPlaceMapper.findByPlaceId(loginUserDto.getPlaceId(), Constants.DB_FLG_FALSE);
		//&が存在するか確認
		int startIndex = mPlace.getPlaceNote().indexOf("$");
		if (startIndex == -1) {
			startIndex = mPlace.getPlaceNote().indexOf("＄");
			if (startIndex == -1) {
				return null;
			}
		}
		String className = mPlace.getPlaceNote().substring(startIndex + 1);

		//)または）が存在するか確認
		int endIndex = mPlace.getPlaceNote().indexOf("$");
		if (endIndex == -1) {
			endIndex = mPlace.getPlaceNote().indexOf("＄");
			if (endIndex == -1) {
				return null;
			}
		}

		className = className.substring(0, endIndex);
		className = mPlace.getPlaceName() + "（" + className + "）";
		return className;
	}

	/**
	 * 勤怠管理画面 検索入力チェック
	 * @author 櫻井宝生 - Task.58
	 * @return エラーメッセージ
	 */
	public String searchValueCheck(AttendanceBulkForm attendanceBulkForm) {
		//本日の日付取得
		Date trainingDate = attendanceUtil.getTrainingDate();
		//期間(To)が期間(From)より過去日の場合
		if (attendanceBulkForm.getSearchPeriodTo().after(trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_SEARCHTORANGEERROR);
		}
		//期間(To)が現在日付より未来の場合
		if (attendanceBulkForm.getSearchPeriodFrom().after(attendanceBulkForm.getSearchPeriodTo())) {
			return messageUtil.getMessage(Constants.VALID_KEY_SEARCHPERIODCOMPAREERROR);
		}
		
		//期間(From)　～　期間(To)の日数　＞　30日の場合
		Integer difDays = dateUtil.differenceDays(attendanceBulkForm.getSearchPeriodTo(),attendanceBulkForm.getSearchPeriodFrom());
		Integer thirtieth = 1000 * 30 * 24 * 60 * 60;
		if(difDays >thirtieth) {
			return messageUtil.getMessage(Constants.VALID_KEY_SEARCHSETTINGOVER);
		}
		
		return null;
	}
}

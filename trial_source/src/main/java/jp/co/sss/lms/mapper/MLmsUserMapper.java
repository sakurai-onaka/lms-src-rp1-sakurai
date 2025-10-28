package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.UserDetailDto;

/**
 * LMSユーザーマスタマッパー
 * 
 * @author 東京ITスクール
 */
@Mapper
public interface MLmsUserMapper {

	/**
	 * ユーザー基本情報取得
	 * 
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return ユーザー基本情報DTO
	 */
	UserDetailDto getUserDetail(@Param("lmsUserId") Integer lmsUserId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * ユーザー基本情報取得(検索)
	 * 
	 * @author 櫻井宝生 - Task.57
	 * @param courseName
	 * @param companyName
	 * @param userName
	 * @param mPlace
	 * @param role
	 * @param deleteFlg
	 * @return ユーザー基本情報DTO
	 */
	List<UserDetailDto> getUserDetailForSearch(@Param("courseName") String courseName,
			@Param("companyName") String companyName, @Param("userName") String userName,
			@Param("placeId") Integer placeId, @Param("role") String role, @Param("deleteFlg") Short deleteFlg);

}

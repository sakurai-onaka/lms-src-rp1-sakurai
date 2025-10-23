package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.entity.MPlace;

@Mapper
public interface MPlaceMapper {
	
	/**
	 * 会場リスト
	 * @param hiddenFlg
	 * @param deleteFlg
	 * @return 会場リスト
	 */
	MPlace findByPlaceId(@Param("placeId") Integer placeId,
			@Param("deleteFlg") Short deleteFlg);
}

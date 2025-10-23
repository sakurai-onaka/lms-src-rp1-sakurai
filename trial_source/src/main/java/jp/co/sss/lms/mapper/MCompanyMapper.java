package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.CompanyDto;

/**
 * コースマスタマッパー
 * 
 * @author 東京ITスクール
 */
@Mapper
public interface MCompanyMapper {
	List<CompanyDto> getCompanyDto(@Param("deleteFlg") Short deleteFlg);
}

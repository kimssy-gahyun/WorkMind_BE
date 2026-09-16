package com.gh.workmind.common.template;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

public class FileRenamePolicy {

	/**
	 * 파일명 수정 후 업로드하는 공통코드
	 * @param upfile 요청 시 전달받은 파일 정보가 담긴 객체
	 * @param session 세션객체
	 * @return 문자열 형태의 수정파일명
	 */
	public static String saveFile(MultipartFile upfile, HttpSession session) {
		
		// 파일명 수정작업 후 서버 업로드 (webapp/resources/document_upfiles)

		// 1. 원본파일명 뽑아오기
		String originalFileName = upfile.getOriginalFilename();
		
		// 2. 시간 형식을 문자열로 뽑아내기
		String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		
		// 3. 뒤에 붙을 5자리 랜덤수 뽑기 (10000 ~ 99999)
		int ranNum = (int)(Math.random() * 90000 + 10000);
		
		// 4. 원본파일명으로부터 확장자명 뽑기
		String ext = originalFileName.substring(originalFileName.lastIndexOf("."));
		
		// 5. 2 + 3 + 4 모두 이어 붙이기
		String storedFileName = currentTime + ranNum + ext;
		
		// 6. 업로드 하고자 하는 서버 폴더의 물리적인 경로를 알아내기
		String savePath = session.getServletContext().getRealPath("/resources/document_upfiles/");
		
		// 7. 경로와 수정파일명 합체 후 파일 업로드
		try {
			upfile.transferTo(new File(savePath + storedFileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return storedFileName;

	}
	
}

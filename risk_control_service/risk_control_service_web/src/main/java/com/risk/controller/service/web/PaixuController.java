package com.risk.controller.service.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.handler.VerifyHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.impl.PaixuServiceImpl;

@RequestMapping("paixu")
@Controller
public class PaixuController {

    @Autowired
    private PaixuServiceImpl paixuServiceImpl;

	@Autowired
	private VerifyHandler verifyHandler;

    /**
     * 
     * @param
     * @param
     * @return
     */
    @RequestMapping(value = "api/apply", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity getRadarBehavior(@Validated DecisionHandleRequest request, BindingResult result) {
        return paixuServiceImpl.requestPaixu(request);
    }
    
    @RequestMapping(value = "/getContactCount")
    @ResponseBody
    public ResponseEntity getContactCount(DecisionHandleRequest req, String nids) throws Exception {
        return paixuServiceImpl.getContactCount(nids);
    }
    
    @RequestMapping(value = "/getContactTime")
    @ResponseBody
    public ResponseEntity getContactTime(DecisionHandleRequest req, String nids) throws Exception {
        return paixuServiceImpl.getContactTime(nids);
    }
    
    @RequestMapping(value = "/getExcel")
    public void getExcel(DecisionHandleRequest req, String nids, HttpServletResponse response, HttpServletRequest request) throws Exception {
    	
    	ResponseEntity responseEntity = paixuServiceImpl.getContactCount(nids);
    	
    	List<JSONArray> list = (List<JSONArray>) responseEntity.getData();
 
		HSSFWorkbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet("紧急联系人与运营商通话统计");
		
		Font columnHeadFont = wb.createFont();
		columnHeadFont.setFontName("宋体");
		columnHeadFont.setFontHeightInPoints((short) 12);
		columnHeadFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		
		Font boldFont = wb.createFont();
		boldFont.setFontName("宋体");
		boldFont.setFontHeightInPoints((short) 12);
		boldFont.setBoldweight((short) 300);
		
		CellStyle columnHeadStyle = wb.createCellStyle();
		columnHeadStyle.setFont(columnHeadFont);
		columnHeadStyle.setAlignment(CellStyle.ALIGN_CENTER);// 左右居中
		columnHeadStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);// 上下居中
		columnHeadStyle.setLocked(true);
		columnHeadStyle.setWrapText(true);
		
		CellStyle unitStyle = wb.createCellStyle();
		unitStyle.setFont(columnHeadFont);
		unitStyle.setAlignment(CellStyle.ALIGN_RIGHT);// 左右居又
		unitStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);// 上下居中
		unitStyle.setLocked(true);
		unitStyle.setWrapText(true);
		
		CellStyle boldStyle = wb.createCellStyle();
		boldStyle.setFont(boldFont);
		boldStyle.setAlignment(CellStyle.ALIGN_CENTER_SELECTION);// 左右居中
		boldStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);// 上下居中
		
		boldStyle.setLeftBorderColor(HSSFColor.BLACK.index);// 左边框的颜色
		boldStyle.setBorderLeft((short)1);// 边框的大小
	
		boldStyle.setRightBorderColor(HSSFColor.BLACK.index);// 右边框的颜色
		boldStyle.setBorderRight((short)1);// 边框的大小
		
		boldStyle.setTopBorderColor(HSSFColor.BLACK.index);
		boldStyle.setBorderTop((short)1);
	
		boldStyle.setBorderBottom((short)1); //
		boldStyle.setBottomBorderColor(HSSFColor.BLACK.index);
		boldStyle.setWrapText(true);
		
		int rowIdx = 0;
		Row row = sheet.createRow(rowIdx++);
		row.setRowStyle(boldStyle);
		int i = 0;
		Cell cellOne = row.createCell(i++);
		cellOne.setCellValue("用户编号");
		cellOne.setCellStyle(boldStyle);
		
		cellOne = row.createCell(i++);
		cellOne.setCellValue("用户手机号");
		cellOne.setCellStyle(boldStyle);
		
		cellOne = row.createCell(i++);
		cellOne.setCellValue("借款单号");
		cellOne.setCellStyle(boldStyle);

		sheet.setColumnWidth(i, 256 * 20);
		cellOne = row.createCell(i++);
		cellOne.setCellValue("联系人手机号");
		cellOne.setCellStyle(boldStyle);

		sheet.setColumnWidth(i, 256 * 20);
		cellOne = row.createCell(i++);
		cellOne.setCellValue("最后通话时间");
		cellOne.setCellStyle(boldStyle);

		sheet.setColumnWidth(i, 256 * 10);
		cellOne = row.createCell(i++);
		cellOne.setCellValue("最后时长");
		cellOne.setCellStyle(boldStyle);

//		cellOne = row.createCell(i++);
//		cellOne.setCellValue("总通话次数");
		
		cellOne = row.createCell(i++);
		cellOne.setCellValue("分通话次数");
		cellOne.setCellStyle(boldStyle);

		int ic = 0;
		for (JSONArray jsonArray : list) {
//			rowIdx = ic * 6 + rowIdx;

			int contactNo = 0;
    		for (Object object : jsonArray) {
    			contactNo ++;
    			
    			row = sheet.createRow(rowIdx++);
    			row.setRowStyle(boldStyle);
    			
    			i = 0;
    			i++;
    			int cellIdx = 0;
    			sheet.autoSizeColumn(cellIdx);

    			JSONObject jsonObject = (JSONObject) object;
    			
    			if (contactNo == 1) { // 联系人1
    				Cell cell = row.createCell(cellIdx++);
    				cell.setCellStyle(boldStyle);

        			cell.setCellValue(jsonObject.getString("clientId"));
        			sheet.autoSizeColumn(cellIdx);
        			cell.setCellStyle(boldStyle);
        			CellRangeAddress cra =new CellRangeAddress(rowIdx - 1, rowIdx + 4, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);
    			
        			cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);
        			
        			cell.setCellValue(jsonObject.getString("phone"));
        			sheet.autoSizeColumn(cellIdx);
        			cra =new CellRangeAddress(rowIdx - 1, rowIdx + 4, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);

        			cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);

        			cell.setCellValue(jsonObject.getString("nid"));
        			sheet.autoSizeColumn(cellIdx);
        			
        			cra =new CellRangeAddress(rowIdx - 1, rowIdx + 4, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);
        			
        			cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);

        			cell.setCellValue(jsonObject.getString("contactPhone"));
        			
        			cra =new CellRangeAddress(rowIdx - 1, rowIdx + 1, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);

        			cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);

        			String tem = jsonObject.getString("call_time").replace("1990-01-01 12:00:00", "无");
        			cell.setCellValue(tem);
        			
        			cra =new CellRangeAddress(rowIdx - 1, rowIdx + 1, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);

        			cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);

        			cell.setCellValue(jsonObject.getString("duration"));
        			sheet.autoSizeColumn(cellIdx);
        			
        			cra =new CellRangeAddress(rowIdx - 1, rowIdx + 1, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);
        			
    				rowIdx += 2; 
    			}
    			
    			if (contactNo == 2) {
    				cellIdx = 3;
    				Cell cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);

        			cell.setCellValue(jsonObject.getString("contactPhone"));
        			
        			CellRangeAddress cra =new CellRangeAddress(rowIdx - 1, rowIdx + 1, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);

        			cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);

        			String tem = jsonObject.getString("call_time").replace("1990-01-01 12:00:00", "无");
        			cell.setCellValue(tem);
        			
        			cra =new CellRangeAddress(rowIdx - 1, rowIdx + 1, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);

        			cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);

        			cell.setCellValue(jsonObject.getString("duration"));
        			sheet.autoSizeColumn(cellIdx);
        			
        			cra =new CellRangeAddress(rowIdx - 1, rowIdx + 1, cellIdx - 1, cellIdx - 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);
        			
    				rowIdx += 2; 

    			}
   			
    			jsonObject.remove("clientId");
    			jsonObject.remove("phone");
    			jsonObject.remove("nid");
    			jsonObject.remove("contactPhone");
    			jsonObject.remove("call_time");
    			jsonObject.remove("duration");
    			jsonObject.remove("total");
    			jsonObject.remove("record");
    			
    			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
    			
    			Row row2 = sheet.createRow(rowIdx - 2);
    			Row row3 = sheet.createRow(rowIdx - 1);
    			row2.setRowStyle(boldStyle);
    			row3.setRowStyle(boldStyle);
    			
    			String currMon = sdf.format(new Date());
    			for (int c = 0;c < 7;c++) {
    				
					sheet.setColumnWidth(cellIdx, 256 * 15);
					Cell cell = row.createCell(cellIdx++);
        			cell.setCellStyle(boldStyle);
        			String str = jsonObject.getString(currMon + "月") == null ? "本月无" : jsonObject.getString(currMon + "月");
        			cell.setCellValue(currMon + " ： " + str);
        			sheet.setColumnWidth(cellIdx, 256 * 17);
        			CellRangeAddress cra =new CellRangeAddress(rowIdx - 3, rowIdx - 3, cellIdx - 1, cellIdx + 1); // 起始行, 终止行, 起始列, 终止列
        			sheet.addMergedRegion(cra);

        			sheet.setColumnWidth(cellIdx, 256 * 15);

    				cell = row2.createCell(cellIdx - 1);
    				Cell cell3 = row3.createCell(cellIdx - 1);
        			cell.setCellStyle(boldStyle);
        			cell3.setCellStyle(boldStyle);

    				String month = currMon;
    				String zjValue = "主叫 ： " + (jsonObject.getString(month + "主叫") == null ? "0" : jsonObject.getString(month + "主叫"));
        			cell.setCellValue(zjValue);
        			sheet.setColumnWidth(cellIdx, 256 * 17);
        			
        			String zjscValue = "时长 ： " + (jsonObject.getString(month + "主叫时长") == null ? "0" : jsonObject.getString(month + "主叫时长"));
        			cell3.setCellValue(zjscValue);
        			sheet.setColumnWidth(cellIdx, 256 * 17);
        			
        			sheet.setColumnWidth(cellIdx, 256 * 15);
    				cell = row2.createCell(cellIdx);
    				cell3 = row3.createCell(cellIdx);
    				cell.setCellStyle(boldStyle);
        			cell3.setCellStyle(boldStyle);
        			
    				String bjValue = "被叫 ： " + (jsonObject.getString(month + "被叫") == null ?  "0" : jsonObject.getString(month + "被叫"));
        			cell.setCellValue(bjValue);
        			sheet.setColumnWidth(cellIdx, 256 * 17);
        			
        			String bjscValue = "时长 ： " + (jsonObject.getString(month + "被叫时长") == null ? "0" : jsonObject.getString(month + "被叫时长"));
        			cell3.setCellValue(bjscValue);
        			sheet.setColumnWidth(cellIdx, 256 * 17);
        			

    				sheet.setColumnWidth(cellIdx, 256 * 15);
    				cell = row2.createCell(cellIdx + 1);
    				cell3 = row3.createCell(cellIdx + 1);
    				cell.setCellStyle(boldStyle);
        			cell3.setCellStyle(boldStyle);

    				String wqfValue = "未知 ： " + (jsonObject.getString(month + "未知") == null ? "0" : jsonObject.getString(month + "未知"));
        			cell.setCellValue(wqfValue);
        			sheet.setColumnWidth(cellIdx + 1, 256 * 17);
        			
        			String wqfscValue = "时长 ： " + (jsonObject.getString(month + "未知时长") == null ? "0" : jsonObject.getString(month + "未知时长"));
        			cell3.setCellValue(wqfscValue);
        			sheet.setColumnWidth(cellIdx + 1, 256 * 17);
        			
        			cellIdx += 2;

    				currMon = paixuServiceImpl.getLastMonth(sdf.parse(currMon));
    			}
    			
//    			for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
//    				if (entry.getKey().contains("月")) {
//    					sheet.setColumnWidth(cellIdx, 256 * 15);
//    					Cell cell = row.createCell(cellIdx++);
//            			cell.setCellStyle(boldStyle);
//
//            			cell.setCellValue(entry.getKey() + " ： " + entry.getValue().toString() + "次");
//            			sheet.setColumnWidth(cellIdx, 256 * 17);
//            			CellRangeAddress cra =new CellRangeAddress(rowIdx - 3, rowIdx - 3, cellIdx - 1, cellIdx + 1); // 起始行, 终止行, 起始列, 终止列
//            			sheet.addMergedRegion(cra);
//
//            			sheet.setColumnWidth(cellIdx, 256 * 15);
//
//        				cell = row2.createCell(cellIdx - 1);
//        				Cell cell3 = row3.createCell(cellIdx - 1);
//            			cell.setCellStyle(boldStyle);
//            			cell3.setCellStyle(boldStyle);
//
//        				String month = entry.getKey().replace("月", "");
//        				String zjValue = "主叫 ： " + (jsonObject.getString(month + "主叫") == null ? "0" : jsonObject.getString(month + "主叫"));
//            			cell.setCellValue(zjValue);
//            			sheet.setColumnWidth(cellIdx, 256 * 17);
//            			
//            			String zjscValue = "时长 ： " + (jsonObject.getString(month + "主叫时长") == null ? "0" : jsonObject.getString(month + "主叫时长"));
//            			cell3.setCellValue(zjscValue);
//            			sheet.setColumnWidth(cellIdx, 256 * 17);
//            			
//            			sheet.setColumnWidth(cellIdx, 256 * 15);
//        				cell = row2.createCell(cellIdx);
//        				cell3 = row3.createCell(cellIdx);
//        				cell.setCellStyle(boldStyle);
//            			cell3.setCellStyle(boldStyle);
//            			
//        				String bjValue = "被叫 ： " + (jsonObject.getString(month + "被叫") == null ?  "0" : jsonObject.getString(month + "被叫"));
//            			cell.setCellValue(bjValue);
//            			sheet.setColumnWidth(cellIdx, 256 * 17);
//            			
//            			String bjscValue = "时长 ： " + (jsonObject.getString(month + "被叫时长") == null ? "0" : jsonObject.getString(month + "被叫时长"));
//            			cell3.setCellValue(bjscValue);
//            			sheet.setColumnWidth(cellIdx, 256 * 17);
//            			
//
//        				sheet.setColumnWidth(cellIdx, 256 * 15);
//        				cell = row2.createCell(cellIdx + 1);
//        				cell3 = row3.createCell(cellIdx + 1);
//        				cell.setCellStyle(boldStyle);
//            			cell3.setCellStyle(boldStyle);
//
//        				String wqfValue = "未知 ： " + (jsonObject.getString(month + "未知") == null ? "0" : jsonObject.getString(month + "未知"));
//            			cell.setCellValue(wqfValue);
//            			sheet.setColumnWidth(cellIdx + 1, 256 * 17);
//            			
//            			String wqfscValue = "时长 ： " + (jsonObject.getString(month + "未知时长") == null ? "0" : jsonObject.getString(month + "未知时长"));
//            			cell3.setCellValue(wqfscValue);
//            			sheet.setColumnWidth(cellIdx + 1, 256 * 17);
//            			
//            			cellIdx += 2;
//    				}
//        			
//    	        }
    		}
//    		rowIdx ++;

			ic ++;
    	}
		
		OutputStream fos = null;
		try {
			fos = response.getOutputStream();
			String userAgent = request.getHeader("USER-AGENT");
			String fileName = "紧急联系人与运营商通话统计";
			try {
				if(StringUtils.contains(userAgent, "Mozilla")){
					fileName = new String(fileName.getBytes(), "ISO8859-1");
				}else {
					fileName = URLEncoder.encode(fileName, "utf8");
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
 
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/vnd.ms-excel;charset=utf-8");// 设置contentType为excel格式
			response.setHeader("Content-Disposition", "Attachment;Filename="+ fileName+".xls");
			wb.write(fos);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

    }
    
    @RequestMapping(value = "/test")
    @ResponseBody
    public ResponseEntity sendPaixu(DecisionHandleRequest req, String nids) throws Exception {
        return paixuServiceImpl.sendPaixu(req, nids);
    }

	@RequestMapping(value = "/test3")
	@ResponseBody
	public void verifyXinyanData() {
		DecisionHandleRequest request = new DecisionHandleRequest();
		request.setNid("218102218100294838");

		AdmissionRuleDTO rule = new AdmissionRuleDTO();
		Map<String, String> set = new HashMap<>();
		set.put("repeatPerson", "3");
		set.put("repeatNum", "3");
		rule.setSetting(set);

		AdmissionResultDTO record = verifyHandler.verifyRepeatContactPhone(request, rule);

		System.out.println("===========================");
		System.out.println(JSONObject.toJSONString(record));
		System.out.println("===========================");
	}
}

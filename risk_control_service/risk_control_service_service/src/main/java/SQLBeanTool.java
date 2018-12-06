

public class SQLBeanTool {
	public static void main(String[] args) {

		String[] s = "nid, phone, avg_call_time_item_1m, avg_call_time_item_3m, avg_call_time_item_6m, avg_call_time_avg_item_3m, avg_call_time_avg_item_6m, no_dial_day_item_1m, no_dial_day_item_3m, no_dial_day_item_6m, no_dial_day_avg_item_3m, no_dial_day_avg_item_6m, no_dial_day_pct_item_1m, no_dial_day_pct_item_3m, no_dial_day_pct_item_6m, no_dial_day_pct_avg_item_3m, no_dial_day_pct_avg_item_6m, no_call_day_item_1m, no_call_day_item_3m, no_call_day_item_6m, no_call_day_avg_item_3m, no_call_day_avg_item_6m, no_call_day_pct_item_1m, no_call_day_pct_item_3m, no_call_day_pct_item_6m, no_call_day_pct_avg_item_3m, no_call_day_pct_avg_item_6m, max_power_on_day_item_1m, max_power_on_day_item_3m, max_power_on_day_item_6m, max_power_on_day_avg_item_3m, max_power_on_day_avg_item_6m, power_off_day_item_1m, power_off_day_item_3m, power_off_day_item_6m, power_off_day_avg_item_3m, power_off_day_avg_item_6m, power_off_day_pct_item_1m, power_off_day_pct_item_3m, power_off_day_pct_item_6m, power_off_day_pct_avg_item_3m, power_off_day_pct_avg_item_6m, continue_power_off_cnt_item_1m, continue_power_off_cnt_item_3m, continue_power_off_cnt_item_6m, continue_power_off_cnt_avg_item_3m, continue_power_off_cnt_avg_item_6m".toLowerCase().split(", ");
//		String[] s = "".toLowerCase().split(", ");
//		String[] s = "".toLowerCase().split(", ");
//		String[] s = "".toLowerCase().split(", ");
//		String[] s = "".toLowerCase().split(", ");
//		beanElement(s);
//		insertOne(s);
//		insertOne2(s); // 去链接符
		insertAll(s);

//		select(s);
//		where(s);
//		insertBatch(s,"t_batch_reg_detail");
//		fanyi(s);
//		System.out.println("COLLECT_SEQ, COLLECT_ID, OPR_ID, COLLECT_DATE, COLLECT_TIME, IDCARD, NAME, SEX, BIRTHDAY, MOBILE, NATION, HOUSEHOLDER_MARK, DOMICILE_TYPE, GBNAME, SGBCODE, GBCODE, ADDRESS, POSTCODE, PHONE, BLOOD_TYPE, RH, UNIT_NAME, UNIT_PHONE, EDUCATION, OCCUPATION, MARRIAGE, FIRST_GUARDIAN, FIRST_RELATION, GUARDIAN_PHONE1, SECOND_GUARDIAN, SECOND_RELATION, GUARDIAN_PHONE2, HOUSEHOLDER_NAME, RELATION, HOUSEHOLDER_IDCARD, PAYMENT_MODE, CITY, AREA, STREET, RXXP, ORG, CARD_TYPE, APPLY_FLAG".toLowerCase());
//		System.out.println("UPDATE_ORGANNAME, UPDATE_IDCARD, UPDATE_MAN, UPDATE_DATE, RXXP, ZTM, ID, HC_BATCH_REG, CITIZEN_IDCARD, COLLECT_SEQ, COLLECT_BATCH, COLLECT_ID, CITY_ID, AREA_ID, STREET_ID, STOCK_STATUS, INPUTNO, OUTPUTNO, RECEIVID, STATUS, POST_REGISTER_TIMES, CARD_TYPE, RSVD1, RSVD2, RSVD3, RSVD4, HY_ECC_PAN".toLowerCase());
//		System.out.println("".toLowerCase());
//		System.out.println("".toLowerCase());
//		System.out.println("123eA".matches("[A-Za-z0-9]+"));
//		System.out.println("123".matches("[0-9]+"));
	}
	
	public static void fanyi(String[] source){
		int i = 1;
		for(String s : source){
//			update t_all_hah_card_info t set t.nation='1' where nation='汉族';
			System.out.println("update t_all_hah_card_info t set t.nation='"+i+"' where nation='"+s.trim()+"';");
			i++;
		}
	}
	
	public static void beanElement(String[] source){
		for(String s : source){
			System.out.println("private String " + s +";");
			System.out.println();
		}
	}
	
	public static void select(String[] source){
		for(String s : source){
			System.out.println("t." + s + ",");
		}
	}
	
	public static void where(String[] source){
		//<if test="nid!= null and nid != ''" >AND t.nid = #{nid}</if>
		for(String s : source){
			System.out.println("<if test=\"" + s + " != null and " + s + " != ''\">AND t."  + s + " = #{" + s + "},</if>");
		}
	}
	
	public static void insertOne(String[] source){
		for(String s : source){
			//<if test="rsvd4!= null and rsvd4 != ''">rsvd4,</if>
			System.out.println("<if test=\"" + s + " != null and " + s + " != ''\">" + s + ",</if>");
		}
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		for(String s : source){
			//<if test="idcard != null  and idcard!= ''" > #{idcard},</if>
			System.out.println("<if test=\"" + s + " != null and " + s + " != ''\">#{" + s + "},</if>");
		}
	}
	
	public static void insertOne2(String[] source){
		for(String s : source){
			//<if test="rsvd4!= null and rsvd4 != ''">rsvd4,</if>
			String s2 = "";
			s2 = s;

			if (s.contains("_")) {
				
				String org = s.substring(s.indexOf("_"), s.indexOf("_") + 2);
				String tar = s.substring(s.indexOf("_"), s.indexOf("_") + 2).toUpperCase();
				s2 = s.replace(org, tar);
				s2 = s2.replaceAll("_", "");
			}
			System.out.println("<if test=\"" + s2 + " != null and " + s2 + " != ''\">" + s + ",</if>");
		}
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		String s2 = "";
		for(String s : source){
			s2 = s;
			if (s.contains("_")) {
				String org = s.substring(s.indexOf("_"), s.indexOf("_") + 2);
				String tar = s.substring(s.indexOf("_"), s.indexOf("_") + 2).toUpperCase();
				s2 = s.replace(org, tar);
				s2 = s2.replaceAll("_", "");
			}

			//<if test="idcard != null  and idcard!= ''" > #{idcard},</if>
			System.out.println("<if test=\"" + s2 + " != null and " + s2 + " != ''\">#{" + s2 + "},</if>");
		}
	}

	public static void insertAll(String[] source){

//		for(String s : source){
//			//<if test="idcard != null  and idcard!= ''" > #{idcard},</if>
//			System.out.println("#{" + s + "},");
//		}

		for(String s : source){
			//<if test="idcard != null  and idcard!= ''" > #{idcard},</if>
			System.out.print("#{" + s + "},");
		}
	}
	
	public static void insertBatch(String[] source,String type){
		
		
		for(String s : source){
			//#{t_info_sam.posid,jdbcType=VARCHAR},
			System.out.println("#{" + type + "." + s + "}" + ",");
		}
	}
}

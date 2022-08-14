package prj.shelter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import javafx.concurrent.Task;
import narl.itrc.UtilPhysical;
import prj.shelter.DevHustIO.Strength;
import prj.shelter.ManBooker.Mark;

public class TskExport extends Task<Void> {

	final File dest;
	final ManBooker book;

	public TskExport(final File fs, final ManBooker man) {
		dest = fs;
		book = man;
	}

	private CellStyle sty;
	private DataFormat fmt;
	//private final DateFormat fmt_day2 = new SimpleDateFormat("yyyyMMdd");
	private final DateFormat fmt_day1 = new SimpleDateFormat("yyyy/MM/dd");
	
	@Override
	protected Void call() throws Exception {

		try {
			final Workbook wb = WorkbookFactory.create(false);
			
			//eva = wb.getCreationHelper().createFormulaEvaluator();
			fmt = wb.createDataFormat();
			sty = wb.createCellStyle();
			sty.setDataFormat(fmt.getFormat("0.00"));

			dump_step_result(wb,Strength.V_3Ci);
			dump_step_result(wb,Strength.V_05Ci);
			dump_step_result(wb,Strength.V_005Ci);
			dump_metrix_vals(wb);

			FileOutputStream stm = new FileOutputStream(dest);
			wb.write(stm);
			stm.close();
		} catch (IOException e) {
			updateMessage("無法建立試算表：" + e.getMessage());
		}
		return null;
	}
	//-----------------------------------------------//
	
	private Mark[] select_crimp_mark(final Strength ss) {
		double[] goal = null;
		switch(ss) {
		case V_3Ci:
			goal = new double[] {				
				10000,8000,5000,4000,2000,
				 1000, 800, 500, 400, 
			};
			break;
		case V_05Ci:
			goal = new double[] {			
				1000,800,500,400,200,
				 100, 80, 50, 40,  
			};
			break;
		case V_005Ci:
			goal = new double[] {
				100,80,50,40,25,20,10,5,	
			};
			break;
		default: 
			return null;
		}
		
		final Mark[] dst = new Mark[goal.length*2];
		
		final List<Mark> lst = book.selectAll(ss);//list has been sorted~~~
		for(int i=0; i<goal.length; i++) {
			final double g0 = goal[i];
			//try to find bound~~~
			for(Mark m1:lst) {
				final double v0 = m1.stat.getMean();
				final double v1 = v0*0.977;
				if(g0<v0 && g0<v1) {					
					//try to find bound~~~
					int pos = lst.indexOf(m1) - 1;
					while(pos>=0) {
						Mark m2 = lst.get(pos);
						if(m2.stat.getMean()<g0) {
							dst[i*2+0] = m1;//up-bound
							dst[i*2+1] = m2;//low-bound
							break;//while(pos>=0)
						}
						pos-=1;
					}
					break;//for(Mark m1:lst)
				}
			}
		}
		return dst;
	}
	//-----------------------------------------------//
	
	private void dump_step_result(final Workbook wb, final Strength ss) {
		
		final Sheet sh = wb.createSheet(ss.toString());
		
		get_cell(sh, "A1").setCellValue("輻射偵檢儀校正實驗室輻射場強度標定紀錄表");

		get_cell(sh, "B2").setCellValue("電量計：");
		get_cell(sh, "C2").setCellValue(
			RadiateStep.at5350.Identify[0].get() + " " + 
			RadiateStep.at5350.Identify[1].get()
		);

		get_cell(sh, "F2").setCellValue("游離腔：");
		get_cell(sh, "G2").setCellValue("PTW TM32002(s/n: 0298)");

		get_cell(sh, "J2").setCellValue("校正報告：");
		get_cell(sh, "K2").setCellValue("（NRSL-104140，2015/05/15，INER）90");

		get_cell(sh, "B3").setCellValue(ss.toString() + "標定");
		get_cell(sh, "C3").setCellValue("匯出日期：");
		get_cell(sh, "D3").setCellValue(fmt_day1.format(Calendar.getInstance().getTime()));

		get_cell(sh, "A5").setCellValue("now (μSv/hr)");
		get_cell(sh, "A6").setCellValue("1年後(μSv/hr)");
		get_cell(sh, "A7").setCellValue("距離 (cm)");
		get_cell(sh, "A8").setCellValue("新距離(cm)");
		get_cell(sh, "A9").setCellValue("個數 (n)");
		get_cell(sh, "A10").setCellValue("平均 (μSv/hr)");
		get_cell(sh, "A11").setCellValue("Sigma");
		get_cell(sh, "A12").setCellValue("%Sigma");
		get_cell(sh, "A13").setCellValue("計讀值 (μSv/hr)");

		//skip two column~~~
		get_cell(sh, "B4").setCellValue("1");
		get_cell(sh, "C4").setCellValue("2");
		
		final Mark[] lst = select_crimp_mark(ss);
		if(lst==null) {
			return;
		}
		
		for(int i=0; i<lst.length; i++) {
			final int xx = 1+i;
			get_cell(sh, 3, xx).setCellValue(String.format("%d", xx));
			final Mark mm = lst[i];
			if(mm==null) {
				continue;
			}
			Cell aa;
			final String[] vals = mm.meas.split("[\n|,]");
			for (int j=vals.length-1; j>= 0; --j) {
				String txt = vals[j];
				final int pos = txt.indexOf("#");
				if (pos >= 0) {
					txt = txt.substring(0, pos);
				}
				txt = txt.replace('"', ' ').trim();
				txt = UtilPhysical.convertScale(txt, "uSv/hr");
				aa = get_cell(sh, 12+j, xx);
				aa.setCellStyle(sty);
				aa.setCellValue(Float.valueOf(txt));
			}
			
			char cc = (char)('B'+i);
			if(cc>='Z') {
				continue;
			}
			aa = get_cell(sh, 6, xx);
			aa.setCellStyle(sty);
			aa.setCellValue(mm.loca.replace("cm", "").trim());

			aa = get_cell(sh, 4, xx);
			aa.setCellStyle(sty);
			aa.setCellFormula(String.format("%C10", cc));

			aa = get_cell(sh, 5, xx);
			aa.setCellStyle(sty);
			aa.setCellFormula(String.format("%C5*0.977", cc));

			aa = get_cell(sh, 7, xx);
			aa.setCellStyle(sty);
			aa.setCellFormula(String.format("((%C7+90)*0.988)-90", cc));

			aa = get_cell(sh, 9, xx);
			aa.setCellStyle(sty);
			aa.setCellFormula(String.format("AVERAGE(%C13:%C32)", cc, cc));

			aa = get_cell(sh,10, xx);
			aa.setCellStyle(sty);
			aa.setCellFormula(String.format("STDEV(%C13:%C32)", cc, cc));

			aa = get_cell(sh,11, xx);
			aa.setCellStyle(sty);
			aa.setCellFormula(String.format("(%C11/%C10)*100", cc, cc));
		}
	}

	private void dump_metrix_vals(final Workbook wb) {
		
		Strength[] ss = DevHustIO.Strength.values();
		
		final Sheet sh = wb.createSheet("標定表");
		get_cell(sh, "A1").setCellValue(ss[0].toString());
		get_cell(sh, "A2").setCellValue("距離(cm)");
		get_cell(sh, "B2").setCellValue("劑量(uSv/min)");

		get_cell(sh, "D1").setCellValue(ss[1].toString());
		get_cell(sh, "D2").setCellValue("距離(cm)");
		get_cell(sh, "E2").setCellValue("劑量(uSv/min)");

		get_cell(sh, "G1").setCellValue(ss[2].toString());
		get_cell(sh, "G2").setCellValue("距離(cm)");
		get_cell(sh, "H2").setCellValue("劑量(uSv/min)");

		final char[][] cols = { { 'A', 'B' }, { 'D', 'E' }, { 'G', 'H' }, };

		for (int i=0; i<ss.length; i++) {			
			final Mark[] mm = select_crimp_mark(ss[i]);			
			for (int j=0; j<mm.length; j++) {
				if(mm[j]==null) {
					continue;
				}
				get_cell(sh, String.format("%C%d", cols[i][0], j + 5))
				.setCellValue(mm[j].loca.replace("cm", "").trim());
				get_cell(sh, String.format("%C%d", cols[i][1], j + 5))
				.setCellValue(String.format("%.4f",mm[j].stat.getMean()*60.));//unit is uSv/min
			}
		}
	}

	private static Cell get_cell(final Sheet sheet, final String address) {
		CellReference ref = new CellReference(address);
		final int yy = ref.getRow();
		final int xx = ref.getCol();
		return get_cell(sheet,yy,xx);
	}

	private static Cell get_cell(final Sheet sheet, final int yy, final int xx) {
		Row rr = sheet.getRow(yy);
		if (rr == null) {
			rr = sheet.createRow(yy);
		}
		Cell cc = rr.getCell(xx);
		if (cc == null) {
			cc = rr.createCell(xx);
		}
		return cc;
	}
}

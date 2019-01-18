package prj.economy;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.svg.SVGGlyph;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Calendar extends VBox {

	public static interface DayHook {
		void callback(Calendar.DayInfo info);
	};
	
	public static class DayInfo {
		public String memo = "";
		public int week;//0 is Sunday, 6 is Saturday, etc....
		public int day;//1 to 31, -1 is none~~~
		public int month;//1 to 12, -1 is none~~~
		public int year;
		public DayInfo(){
			reset();
		}
		public DayInfo(int _week){
			reset();
			week = _week;
		}
		private void reset(){
			week=-1; day=-1; month=-1;year=-1;
			memo = "";
		}
	};
	
	private static final DateTimeFormatter fmt_title = DateTimeFormatter.ofPattern("yyyy - MM月");
	
	private YearMonth yearmonth = YearMonth.now();
	
	private DateCell dayPick = null;
	
	private Label capText;
	private DateCell[] dayText, dayWeek;

	public Calendar() {
		getStyleClass().addAll("bg-white");
		getChildren().addAll(create_head(), create_body());
		update();
	}
	
	public DayHook eventUpdate = null;
	public DayHook eventPickup = null;
	
	public void update(){
		capText.setText(yearmonth.format(fmt_title));
		int end = yearmonth.atEndOfMonth().getDayOfMonth();
		int beg = 0;
		switch(yearmonth.atDay(1).getDayOfWeek()){
		case SUNDAY:   beg = 0; break;
		case MONDAY:   beg = 1; break;
		case TUESDAY:  beg = 2; break;
		case WEDNESDAY:beg = 3; break;
		case THURSDAY: beg = 4; break;
		case FRIDAY:   beg = 5; break;
		case SATURDAY: beg = 6; break;
		}
		int year = yearmonth.getYear();
		int month= yearmonth.getMonthValue();
		for(int i=0, day=1; i<dayWeek.length; i++){
			final DateCell cell = dayWeek[i];
			final DayInfo info = (DayInfo)cell.getUserData();
			info.year = year;
			info.month= month;
			if(beg<=i && day<=end){
				info.day = day;
				if(eventUpdate!=null){
					eventUpdate.callback(info);
				}
				cell.setText(String.format(" %2d\n%s",day,info.memo));
				cell.setItem(yearmonth.atDay(day));
				day = day + 1;
			}else{
				info.day = -1;
				info.memo = "";//reset this data again~~~
				cell.setText("");
				cell.setItem(null);
			}
		}
	}
	
	private static double FONE_SIZE = 18.;
	private static Color c_sunday  = Color.RED;
	private static Color c_saturday= Color.DARKCYAN;
	private static Color c_normal  = Color.BLACK;
	private static Font f_text = Font.font("", FontWeight.BOLD, FONE_SIZE);
	private static Font f_week = Font.font("", FontWeight.NORMAL, FONE_SIZE);

	private BorderPane create_head() {

		final SVGGlyph leftChevron = new SVGGlyph(
			0, "CHEVRON_LEFT",
			"M 742,-37 90,614 Q 53,651 53,704.5 53,758 90,795 l 652,651 q 37,37 90.5,37 53.5,0 90.5,-37 l 75,-75 q 37,-37 37,-90.5 0,-53.5 -37,-90.5 L 512,704 998,219 q 37,-38 37,-91 0,-53 -37,-90 L 923,-37 Q 886,-74 832.5,-74 779,-74 742,-37 z",
			Color.WHITE
		);
		final SVGGlyph rightChevron = new SVGGlyph(
			0, "CHEVRON_RIGHT",
			"m 1099,704 q 0,-52 -37,-91 L 410,-38 q -37,-37 -90,-37 -53,0 -90,37 l -76,75 q -37,39 -37,91 0,53 37,90 l 486,486 -486,485 q -37,39 -37,91 0,53 37,90 l 76,75 q 36,38 90,38 54,0 90,-38 l 652,-651 q 37,-37 37,-90 z",
			Color.WHITE
		);
		leftChevron.setSize(12, 22);
		rightChevron.setSize(12, 22);

		capText = new Label();
		capText.setStyle("-fx-text-fill: #EDEDED;");
		capText.setFont(f_week);
		capText.setAlignment(Pos.BASELINE_CENTER);
		
		final int _size = 64;
		
		final JFXButton prv = new JFXButton();
		prv.setMinSize(_size, _size);
		prv.setGraphic(leftChevron);
		prv.setOnAction(e->plus_month(-1));
		
		final JFXButton nxt = new JFXButton();
		nxt.setMinSize(_size, _size);
		nxt.setGraphic(rightChevron);
		nxt.setOnAction(e->plus_month(1));

		final BorderPane lay0 = new BorderPane();		
		lay0.getStyleClass().addAll("bg-title1");
		lay0.setLeft(prv);
		lay0.setCenter(capText);
		lay0.setRight(nxt);
		//lay0.setPickOnBounds(false);//??? Apply this, the scene will become transparent ???
		return lay0;
	}

	private void plus_month(long off){
		if(dayPick!=null){
			set_text_color(dayPick);
			dayPick.setBackground(b_week_exit);
			dayPick = null;
		}
		yearmonth = yearmonth.plusMonths(off);
		update();
	}
	
	private static final CornerRadii corner = new CornerRadii(40);
	
	private static final Background b_week_goin = new Background(
		new BackgroundFill(Color.valueOf("#EDEDED"), corner, Insets.EMPTY)
	);
	private static final Background b_week_exit = new Background(
		new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)
	);
	private static final Background b_week_pick = new Background(
		new BackgroundFill(Color.valueOf("#009688"), corner, Insets.EMPTY)
	);
	
	private GridPane create_body() {

		create_day_text();
		create_day_week();
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("grid-medium");
		lay0.addRow(0, dayText);
		lay0.addRow(1, get_row(dayWeek, 0));
		lay0.addRow(2, get_row(dayWeek, 7));
		lay0.addRow(3, get_row(dayWeek, 14));
		lay0.addRow(4, get_row(dayWeek, 21));
		lay0.addRow(5, get_row(dayWeek, 28));
		lay0.addRow(6, get_row(dayWeek, 35));
		return lay0;
	}

	private void create_day_text() {
		dayText = new DateCell[7];
		for (int i = 0; i < dayText.length; i++) {
			DateCell cell = new DateCell();
			cell.setAlignment(Pos.BASELINE_CENTER);
			cell.setFont(f_text);
			dayText[i] = cell;
		}
		dayText[0].setText("日");
		dayText[1].setText("一");
		dayText[2].setText("二");
		dayText[3].setText("三");
		dayText[4].setText("四");
		dayText[5].setText("五");
		dayText[6].setText("六");
		dayText[0].setTextFill(c_sunday);
		dayText[6].setTextFill(c_saturday);
	}

	private void create_day_week() {
		dayWeek = new DateCell[7*6];
		for (int i = 0; i < dayWeek.length; i++) {
			final DateCell cell = new DateCell();
			cell.setAlignment(Pos.BASELINE_CENTER);
			cell.setFont(f_week);
			cell.setPrefSize(48, 48);
			cell.setUserData(new DayInfo(i%7));
			set_text_color(cell);
			cell.setOnMouseEntered(event -> {
				if(dayPick==cell){
					return;
				}
				if(cell.getText().length()==0){
					return;
				}
				cell.setBackground(b_week_goin);
			});
			cell.setOnMouseExited(event -> {
				if(dayPick==cell){
					return;
				}
				if(cell.getText().length()==0){
					return;
				}
				cell.setBackground(b_week_exit);
			});
			cell.setOnMouseClicked(event->{
				if(cell.getText().length()==0){
					return;
				}
				if(dayPick==cell){
					dayPick = null;
					if(eventPickup!=null){
						eventPickup.callback(null);
					}
					set_text_color(cell);
					cell.setBackground(b_week_goin);					
				}else{
					//replace old date cell with new one....
					if(dayPick!=null){
						set_text_color(dayPick);
						dayPick.setBackground(b_week_exit);
					}
					dayPick = cell;
					cell.setTextFill(Color.WHITE);
					cell.setBackground(b_week_pick);
					if(eventPickup!=null){
						eventPickup.callback((DayInfo)cell.getUserData());
					}
				}
			});
			dayWeek[i] = cell;
		}
	}

	private void set_text_color(DateCell cell){
		final DayInfo info = (DayInfo)cell.getUserData();
		switch(info.week){
		case 0:   
			cell.setTextFill(c_sunday); 
			break;
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			cell.setTextFill(c_normal);
			break;
		case 6: 
			cell.setTextFill(c_saturday); 
			break;
		}
	}
	
	private DateCell[] get_row(final DateCell[] cells, int from) {
		final DateCell[] result = { 
			cells[from + 0], cells[from + 1], 
			cells[from + 2], cells[from + 3], 
			cells[from + 4], cells[from + 5], 
			cells[from + 6], 
		};
		return result;
	}

}

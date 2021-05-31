package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import narl.itrc.DrawDiagram;
import narl.itrc.Misc;

public class DrawVaccum extends DrawDiagram {

	private final ModCouple coup;
		
	public DrawVaccum(final ModCouple dev) {
		coup = dev;
		
		Chamber   oo1 = new Chamber()  .locate(300, 10);
		GateValve oo2 = new GateValve().locate(447,315);
		Booster   oo3 = new Booster()  .locate(447,358);		
		RoughPump oo4 = new RoughPump().locate( 10,380);
		CommValve gg1 = new CommValve().locate(253,380);
		CommValve gg2 = new CommValve().locate(253,500);
		
		PipeBlock pp1 = new PipeBlock('▟',90,113,11).locate(300,303);
		PipeBlock pp2 = new PipeBlock('▔',146,11).locate(110,405);
		
		PipeBlock pp3 = new PipeBlock('▔',196,11).locate(300,526);
		PipeBlock pp4 = new PipeBlock('▚',146,91,11).locate(110,447);
		
		oo3.setOnMouseClicked(e->{
			oo3.next_state();
		});
		oo4.setOnMouseClicked(e->{
			oo4.next_state();
		});
		pp2.state.bind(oo4.state.isEqualTo(1));
		pp4.state.bind(oo4.state.isEqualTo(1));
		
		gg1.setOnMouseClicked(e->{
			gg1.next_state();
		});
		pp1.state.bind(pp2.state.and(gg1.state.isEqualTo(1)));
		
		gg2.setOnMouseClicked(e->{
			gg2.next_state();
		});
		pp3.state.bind(pp4.state.and(gg2.state.isEqualTo(1)));
		
		//btn1.setOnAction(e->coup.asyncMotorPump(-1));
		//btn2.setOnAction(e->coup.asyncMotorPump( 1));
		//btn3.setOnAction(e->coup.asyncMotorPump( 0));		
		//modeEdit();
	}
	
	private static final Image img_work =  Misc.getResImage("/prj/sputter","work.png");

	private class Chamber extends AnchorPane 
	  implements DrawNode<Chamber> 
	{
		Chamber(){
			final int ww = 340;
			final int hh = 300;
			final int bw = 167;
			final int bh =  54;
			
			Rectangle rr;
			Line ll;
			
			rr = new Rectangle(0, 0, bw, bh);
			rr.setFill(Color.TRANSPARENT);
			rr.setStroke(Color.BLACK);
			rr.setStrokeWidth(1.5);
			rr.setArcWidth(27);
			rr.setArcHeight(27);
			put_in(rr,(ww-bw)/2,hh-bh-33);
			
			ll = new Line(0,0,45,45);
			ll.setStrokeWidth(3.1);
			ll.getStrokeDashArray().setAll(17.,13.,5.);
			//put_in(ll,0,0);
			
			//TranslateTransition tt = new TranslateTransition(Duration.millis(2000),rr);
			//tt.setByX(200f);
			//tt.setCycleCount(Animation.INDEFINITE);
			//tt.setAutoReverse(false);
		    //tt.play();
			
			ScaleTransition st = new ScaleTransition(Duration.millis(2000), ll);
			st.setByX(1.5f);
			st.setByY(1.5f);
			st.setCycleCount(Animation.INDEFINITE);
			st.setAutoReverse(false);
		    st.play();
		    
		    setPrefSize(ww, hh);
			final String css =
			  "-fx-pref-width : 340px;"+
			  "-fx-pref-height: 300px;"+
			  "-fx-background-radius: 70;"+
			  "-fx-border-radius: 70 70 0 0;"+
			  "-fx-border-width: 7px;"+
			  "-fx-border-color: black;"+
			  "-fx-border-style: solid";
			setStyle(css);
		}
		void put_in(final Node obj, final double xx, final double yy) {
			AnchorPane.setLeftAnchor(obj, xx);
			AnchorPane.setTopAnchor (obj, yy);
			getChildren().add(obj);
		}
		@Override
		public Chamber locate(int xx, int yy) {
			set_location(this,xx,yy);
			return this;
		}
	};
	
	private class Booster extends AnchorPane 
	  implements DrawNode<Booster>
	{
		final SimpleIntegerProperty state = new SimpleIntegerProperty(0);
		Booster(){
			//this is also called "cylindrical elbow"~~
			final int w1 = 170;
			final int w2 = 120;
			final int w3 =  70; 
			final int hh = 135;
			Rectangle rr;
			Line ll;
			//connector ring
			ll = new Line(0,0,w1,0);
			ll.setStrokeWidth(4.);
			put_in(ll,0,0);
			//console or panel
			ll = new Line(0,0,w3,0);
			ll.setStrokeWidth(2.);
			put_in(ll,w1/2-w3/2,hh*1/5+13);			
			rr = new Rectangle(w3,hh/3);
			rr.setFill(Color.TRANSPARENT);
			rr.setStroke(Color.BLACK);
			rr.setStrokeWidth(2.);
			put_in(rr,w1/2-w3/2,hh*1/5);
			//working indicator
			ImageView indr = new ImageView(img_work);
			indr.visibleProperty().bind(state.isEqualTo(1));
			RotateTransition ani = new RotateTransition(Duration.seconds(3), indr);
			ani.setByAngle(360);
			ani.setCycleCount(Animation.INDEFINITE);
			//ani.play();			
			put_in(indr,w1/2.-img_work.getWidth()/2.,hh*3/5);			
			//main body
			rr = new Rectangle(w2,hh);
			rr.setFill(Color.TRANSPARENT);
			rr.setStroke(Color.BLACK);
			rr.setStrokeWidth(4.);
			put_in(rr,(w1-w2)/2,0.);
			//support body
			rr = new Rectangle(70,70);
			rr.setFill(Color.TRANSPARENT);
			rr.setStroke(Color.BLACK);
			rr.setStrokeWidth(4.);
			put_in(rr,(w1-70)/2,hh);
			//define state action~~~
			state.addListener((obv,oldVal,newVal)->{
				switch(state.get()) {
				case 0: ani.stop(); break;
				case 1: ani.play(); break;
				}
			});
		}
		void put_in(final Node obj, final double xx, final double yy) {
			AnchorPane.setLeftAnchor(obj, xx);
			AnchorPane.setTopAnchor (obj, yy);
			getChildren().add(obj);
		}
		public void next_state() {
			int ss = state.get();
			ss = (ss+1)%2;
			state.set(ss);
		}
		@Override
		public Booster locate(int xx, int yy) {
			set_location(this,xx,yy);
			return this;
		}
	}
	
	private class RoughPump extends AnchorPane 
	  implements DrawNode<RoughPump>
	{
		final SimpleIntegerProperty state = new SimpleIntegerProperty(0);
		RoughPump(){
			final int ww = 100;

			ImageView img;
			img = new ImageView(img_work);
			put_in(img,
				(ww-img_work.getWidth())/2.,
				ww-img_work.getWidth()-7.
			);
			RotateTransition ani = new RotateTransition(Duration.seconds(2), img);
			ani.setByAngle(360);
			ani.setCycleCount(Animation.INDEFINITE);
			//ani.play();
			
			Rectangle rr;
			rr = new Rectangle(ww,ww);
			rr.setFill(Color.TRANSPARENT);
			rr.setStroke(Color.BLACK);
			rr.setStrokeWidth(3.3);
			put_in(rr,0,0);
			
			Line ll;
			ll = new Line(0,0,ww,0);
			ll.setStroke(Color.BLACK);
			ll.setStrokeWidth(1.7);
			put_in(ll,0,17);			
			ll = new Line(0,0,ww,0);
			ll.setStroke(Color.BLACK);
			ll.setStrokeWidth(1.7);
			put_in(ll,0,17*2);

			state.addListener((obv,oldVal,newVal)->{
				switch(state.get()) {
				case 0: ani.stop(); break;
				case 1: ani.play(); break;
				}
			});
			/*final String css =
			  "-fx-pref-width : 70px;"+
			  "-fx-pref-height: 70px;"+
			  "-fx-border-width: 1px;"+
			  "-fx-border-color: RED;"+
			  "-fx-border-style: solid";
			setStyle(css);*/
		}
		void put_in(final Node obj, final double xx, final double yy) {
			AnchorPane.setLeftAnchor(obj, xx);
			AnchorPane.setTopAnchor (obj, yy);
			getChildren().add(obj);
		}
		public void next_state() {
			int ss = state.get();
			ss = (ss+1)%2;
			state.set(ss);
		}
		@Override
		public RoughPump locate(int xx, int yy) {
			set_location(this,xx,yy);
			return this;
		}
	};
	
	private class GateValve extends StackPane
	  implements DrawNode<GateValve>
	{
		final SimpleIntegerProperty state = new SimpleIntegerProperty(1);
		GateValve(){
			final int ww =170;
			final int hh = 35;
			final int SPACE1 = 3;
			final int SPACE2 = 4;
			
			Line aa,bb;
			aa = new Line(SPACE1,SPACE1,ww-SPACE1,hh-SPACE1);	
			bb = new Line(0,hh-SPACE1,ww-SPACE1,0);
			aa.setStrokeWidth(2.);
			bb.setStrokeWidth(2.);
			Group cross = new Group(aa,bb);
			cross.visibleProperty().bind(state.isEqualTo(1));
			
			aa = new Line(ww/2-SPACE2,SPACE1,ww/2-SPACE2,hh-SPACE1); 
			bb = new Line(ww/2+SPACE2,SPACE1,ww/2+SPACE2,hh-SPACE1);
			aa.setStrokeWidth(2.);
			bb.setStrokeWidth(2.);
			Group pass = new Group(aa,bb);
			pass.visibleProperty().bind(state.isEqualTo(2));
			
			Rectangle body = new Rectangle(ww,hh);
			body.setFill(Color.TRANSPARENT);
			body.setStroke(Color.BLACK);
			body.setStrokeWidth(4.);
			
			getChildren().addAll(body,cross,pass);
		}
		@Override
		public GateValve locate(int xx, int yy) {
			set_location(this,xx,yy);
			return this;
		}
	};
	
	private class CommValve extends AnchorPane
	  implements DrawNode<CommValve>
	{
		final SimpleIntegerProperty state = new SimpleIntegerProperty(0);
		CommValve(){
			final int rr = 25;//半徑
			final int hw = 17;//手把寬
			final int hh = 11;//手把長
			final int sw = 13;//pipe line diagram~~~
			
			final float ra = (float) Math.sqrt(rr*rr-sw*sw);
			
			SVGPath body = new SVGPath();			
			body.setStrokeWidth(3.);
			body.setStroke(Color.BLACK);
			//svg.setFill(Color.CORNFLOWERBLUE);
			body.setFill(Color.TRANSPARENT);
			body.setContent(String.format(
			  "M%.1f,%.1f "+
			  "v%d a%d,%d 0 0 0 %.1f,%.1f "+
			  "v%d a%d,%d 0 0 0 %.1f,%.1f "+
			  "M%d,%.1f h%d,0 "+
			  "M0,%.1f v0,%d ",
			  -ra, -sw/2.,
			   sw, rr,rr, ra*2.,0.,
			  -sw, rr,rr,-ra*2.,0.,
			  -hw/2,-ra-hh, hw,
			  -ra,-hh
			));
			put_in(body,0,0);
			
			SVGPath idn1 = new SVGPath();
			idn1.setStrokeWidth(3.);
			idn1.setStroke(Color.BLACK);
			idn1.setFill(Color.TRANSPARENT);
			idn1.setContent(String.format(
			  "M%.1f,%.1f l%.1f,%.1f "+
			  "M%.1f,%.1f l%.1f,%.1f ",
			  -ra/2.,-ra/2., ra, ra,
			  -ra/2., ra/2., ra,-ra
			));
			idn1.visibleProperty().bind(state.isEqualTo(0));
			put_in(idn1,2+ra/2.,hh+ra/2.);
			
			SVGPath idn2 = new SVGPath();
			idn2.setStrokeWidth(3.);
			idn2.setStroke(Color.BLACK);
			idn2.setFill(Color.TRANSPARENT);
			idn2.setContent(String.format(
			  "M%.1f,%.1f h%.1f "+
			  "M%.1f,%.1f h%.1f ",
			  -ra/2.,-ra/2., ra,
			  -ra/2., ra/2., ra
			));
			idn2.visibleProperty().bind(state.isEqualTo(1));
			put_in(idn2,2+ra/2.,hh+ra/2.);
			
			/*final String css =
			  "-fx-border-width: 3px;"+
			  "-fx-border-color: RED;"+
			  "-fx-border-style: solid";
			setStyle(css);*/
		}
		void put_in(final Node obj, final double xx, final double yy) {
			AnchorPane.setLeftAnchor(obj, xx);
			AnchorPane.setTopAnchor (obj, yy);
			getChildren().add(obj);
		}
		public void next_state() {
			int ss = state.get();
			ss = (ss+1)%2;
			state.set(ss);
		}
		@Override
		public CommValve locate(int xx, int yy) {
			set_location(this,xx,yy);
			return this;
		}
	}
}

package prj.refuge;

import java.awt.image.BufferedImage;

public class UtilPerceive {

	/**
	 * check whether two pictures are same.<p>
	 * For Speed, we down-sample image.<p>
	 * @param aa
	 * @param bb
	 * @return
	 */
	public static int getPSNR(BufferedImage aa, BufferedImage bb){
		
		final int aw = aa.getWidth();
		final int ah = aa.getHeight();
		
		final int bw = bb.getWidth();
		final int bh = bb.getHeight();
		
		final int ww = (aw>bw)?(bw):(aw);
		final int hh = (ah>bh)?(bh):(ah);
		
		final int BLK_SIZE = 8;
		
		int cnt=0, max=0;
		
		max = (ww*hh) / (BLK_SIZE*BLK_SIZE);
		
		for(int y=0; y<hh; y+=BLK_SIZE){
			
			for(int x=0; x<ww; x+=BLK_SIZE){
				
				int i = x + BLK_SIZE/2;
				int j = y + BLK_SIZE/2;
				
				if(aa.getRGB(i,j)==bb.getRGB(i,j)){
					cnt++;
				}
			}
		}
		
		//Misc.logv("count=%d, max=%d, ratio=%d", cnt, max, (cnt*100)/max);
		return (cnt*100)/max;
	}	
}
